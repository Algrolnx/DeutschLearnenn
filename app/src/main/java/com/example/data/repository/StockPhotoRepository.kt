package com.example.data.repository

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.data.model.WordEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ln

enum class StockPhotoProvider(val displayName: String) {
    PIXABAY("Pixabay")
}

data class CachedStockPhoto(
    val imageUrl: String,
    val provider: StockPhotoProvider,
    val photographer: String = ""
)

internal data class PhotoCandidate(
    val url: String,
    val width: Int,
    val height: Int,
    val photographer: String,
    val tags: List<String> = emptyList(),
    val likes: Int = 0
)

private sealed interface PixabayCallResult {
    data class Hits(
        val ranked: List<CachedStockPhoto>,
        val bestScore: Float
    ) : PixabayCallResult
    data object Empty : PixabayCallResult
    data object TransientFailure : PixabayCallResult
}

/**
 * Resolves relevant stock photographs for German vocabulary cards via Pixabay API.
 * - Dedicated HTTP clients with tuned connection pools & host concurrency so JSON lookups
 *   never block image byte downloads.
 * - Priority-separated semaphores so currently visible cards never queue behind background prefetch.
 * - O(1) in-memory subtopic URL index (no repeated SharedPreferences full scans).
 * - Whole-word tag matching, multi-word phrase synergy, landscape-only orientation, AI/text junk filtering,
 *   and quality-gated multi-step search that self-corrects noisy hits while keeping 1-call speed for clean hits.
 */
class StockPhotoRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Fast, dedicated client for Pixabay JSON metadata calls
    private val apiHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply {
            maxRequests = 24
            maxRequestsPerHost = 12
        })
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(req)
        }
        .build()

    // Separate high-concurrency client for Coil CDN image downloads
    private val imageHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 16
        })
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(req)
        }
        .build()

    val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .okHttpClient(imageHttpClient)
        .memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(appContext.cacheDir, "word_photos_coil"))
                .maxSizeBytes(300L * 1024L * 1024L)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(100)
        .build()

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memoryUrlCache = ConcurrentHashMap<String, CachedStockPhoto>()
    private val alternativesCache = ConcurrentHashMap<String, List<CachedStockPhoto>>()
    private val verifiedEmptyCache = ConcurrentHashMap<String, Boolean>()
    private val inFlight = ConcurrentHashMap<String, Deferred<CachedStockPhoto?>>()

    // Separate concurrency lanes: visible cards never wait behind background prefetching
    private val foregroundApiGuard = Semaphore(permits = 5)
    private val prefetchApiGuard = Semaphore(permits = 2)

    private val subtopicSelectionLock = Any()
    private val subtopicUrlIndex = HashMap<String, MutableMap<String, String>>()
    @Volatile
    private var subtopicIndexHydrated = false

    @Volatile
    private var pixabayCooldownUntilMs: Long = 0L

    private val pixabayApiKey: String
        get() = sanitizeApiKey(BuildConfig.PIXABAY_API_KEY, "YOUR_PIXABAY_API_KEY")

    fun hasConfiguredApiKey(): Boolean = pixabayApiKey.isNotEmpty()

    fun getCachedSync(word: WordEntity): CachedStockPhoto? {
        val key = cacheKey(word)
        memoryUrlCache[key]?.let { return it }
        return readFromDiskCache(key)?.also { cached ->
            memoryUrlCache[key] = cached
            recordSubtopicUrl(word.subtopicId, key, cached.imageUrl)
        }
    }

    /**
     * Checks whether a full-card bitmap is already decoded in Coil's RAM cache for instant 0ms display.
     */
    fun isInMemoryCache(imageUrl: String, memoryKey: String = imageUrl): Boolean {
        val cache = imageLoader.memoryCache ?: return false
        return cache[MemoryCache.Key(memoryKey)] != null
    }

    suspend fun resolvePhoto(word: WordEntity, highPriority: Boolean = true): CachedStockPhoto? {
        val key = cacheKey(word)
        memoryUrlCache[key]?.let { return it }

        readFromDiskCache(key)?.let { cached ->
            memoryUrlCache[key] = cached
            recordSubtopicUrl(word.subtopicId, key, cached.imageUrl)
            return cached
        }

        return fetchAndCache(word, key, highPriority)
    }

    /**
     * Cycles to the next ranked Pixabay alternative for this word and saves the user's pick permanently.
     */
    suspend fun cycleNextPhoto(word: WordEntity): CachedStockPhoto? {
        if (!hasConfiguredApiKey()) return null
        val key = cacheKey(word)
        val currentUrl = memoryUrlCache[key]?.imageUrl ?: readFromDiskCache(key)?.imageUrl

        var pool = alternativesCache[key]
        if (pool == null || pool.size <= 1) {
            val outcome = foregroundApiGuard.withPermit {
                withContext(Dispatchers.IO) {
                    fetchFromPixabay(word)
                }
            }
            if (outcome is PixabayCallResult.Hits && outcome.ranked.isNotEmpty()) {
                pool = outcome.ranked
                alternativesCache[key] = pool
            }
        }

        val candidates = pool ?: return null
        if (candidates.isEmpty()) return null
        val next = synchronized(subtopicSelectionLock) {
            val usedByOthers = getUsedUrlsInSubtopic(word.subtopicId, excludeKey = key)
            val chosen = selectNextCycleCandidate(candidates, currentUrl, usedByOthers)
            memoryUrlCache[key] = chosen
            recordSubtopicUrlLocked(word.subtopicId, key, chosen.imageUrl)
            writeToDiskCache(key, chosen)
            chosen
        }
        return next
    }

    /**
     * Fire-and-forget parallel background prefetch on the repository scope so card navigation
     * never cancels an in-flight lookup and upcoming cards are already decoded in Coil RAM cache.
     */
    fun prefetchWordsAsync(words: List<WordEntity>) {
        if (words.isEmpty() || !hasConfiguredApiKey() || System.currentTimeMillis() < pixabayCooldownUntilMs) {
            return
        }
        val uniqueWords = words.distinctBy { cacheKey(it) }.take(4)
        for (word in uniqueWords) {
            val cached = getCachedSync(word)
            if (cached != null) {
                if (!isInMemoryCache(cached.imageUrl)) {
                    enqueueCardBitmapWarmup(cached.imageUrl)
                }
                continue
            }
            repoScope.launch {
                if (System.currentTimeMillis() < pixabayCooldownUntilMs) return@launch
                prefetchWord(word)
            }
        }
    }

    /**
     * Gently prefetches the photo URL (via background lane) and warms Coil's disk + RAM cache.
     */
    suspend fun prefetchWord(word: WordEntity) {
        if (!hasConfiguredApiKey() || System.currentTimeMillis() < pixabayCooldownUntilMs) return
        val resolved = resolvePhoto(word, highPriority = false) ?: return
        if (!isInMemoryCache(resolved.imageUrl)) {
            enqueueCardBitmapWarmup(resolved.imageUrl)
        }
    }

    private fun enqueueCardBitmapWarmup(imageUrl: String) {
        val request = ImageRequest.Builder(appContext)
            .data(imageUrl)
            .size(CARD_DECODE_WIDTH, CARD_DECODE_HEIGHT)
            .memoryCacheKey(imageUrl)
            .build()
        imageLoader.enqueue(request)
    }

    fun remainingCooldownMs(): Long {
        return (pixabayCooldownUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * Returns true if this word already has both a cached photo URL and its image bytes
     * stored inside Coil's on-device disk cache for offline viewing.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun isPhotoDiskCached(word: WordEntity): Boolean {
        val cached = getCachedSync(word) ?: return false
        return try {
            val diskCache = imageLoader.diskCache ?: return false
            diskCache.openSnapshot(cached.imageUrl)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Resolves (if needed) and downloads the image bytes into Coil's disk cache synchronously
     * on Dispatchers.IO so the card displays seamlessly without internet.
     */
    suspend fun preloadWordForOffline(word: WordEntity): Boolean = withContext(Dispatchers.IO) {
        if (isPhotoDiskCached(word)) return@withContext true
        val resolved = getCachedSync(word) ?: run {
            if (!hasConfiguredApiKey() || remainingCooldownMs() > 0L) return@withContext false
            resolvePhoto(word, highPriority = false)
        } ?: return@withContext false

        try {
            val request = ImageRequest.Builder(appContext)
                .data(resolved.imageUrl)
                .size(CARD_DECODE_WIDTH, CARD_DECODE_HEIGHT)
                .memoryCacheKey(resolved.imageUrl)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            result.drawable != null || isPhotoDiskCached(word)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "Offline image download failed for '${word.german}': ${e.message}")
            false
        }
    }

    /**
     * Invalidates a stale/expired cached Pixabay URL and fetches a fresh one.
     */
    suspend fun invalidateAndRefreshPhoto(word: WordEntity): CachedStockPhoto? {
        val key = cacheKey(word)
        memoryUrlCache.remove(key)
        alternativesCache.remove(key)
        verifiedEmptyCache.remove(key)
        synchronized(subtopicSelectionLock) {
            subtopicUrlIndex[word.subtopicId.lowercase()]?.remove(key)
        }
        prefs.edit().remove(key).apply()
        return fetchAndCache(word, key, highPriority = true)
    }

    private suspend fun fetchAndCache(
        word: WordEntity,
        key: String,
        highPriority: Boolean
    ): CachedStockPhoto? {
        if (!hasConfiguredApiKey() || verifiedEmptyCache.containsKey(key)) {
            return null
        }

        val guard = if (highPriority) foregroundApiGuard else prefetchApiGuard
        val deferred = inFlight.computeIfAbsent(key) {
            repoScope.async {
                try {
                    val outcome = guard.withPermit {
                        val usedSnapshot = synchronized(subtopicSelectionLock) {
                            getUsedUrlsInSubtopic(word.subtopicId, excludeKey = key)
                        }
                        fetchFromPixabay(word, usedSnapshot)
                    }
                    when (outcome) {
                        is PixabayCallResult.Hits -> {
                            synchronized(subtopicSelectionLock) {
                                val usedNow = getUsedUrlsInSubtopic(word.subtopicId, excludeKey = key)
                                val best = selectDistinctCandidate(outcome.ranked, usedNow)
                                if (best != null) {
                                    alternativesCache[key] = outcome.ranked
                                    memoryUrlCache[key] = best
                                    recordSubtopicUrlLocked(word.subtopicId, key, best.imageUrl)
                                    writeToDiskCache(key, best)
                                }
                                best
                            }
                        }
                        PixabayCallResult.Empty -> {
                            verifiedEmptyCache[key] = true
                            null
                        }
                        PixabayCallResult.TransientFailure -> null
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    Log.w(TAG, "Stock photo lookup failed for '${word.german}': ${e.message}")
                    null
                }
            }.also { task ->
                task.invokeOnCompletion {
                    inFlight.remove(key, task)
                }
            }
        }

        return deferred.await()
    }

    private fun fetchFromPixabay(
        word: WordEntity,
        usedUrlsInSubtopic: Set<String> = emptySet()
    ): PixabayCallResult {
        val plan = StockPhotoQueryBuilder.build(word)
        val now = System.currentTimeMillis()
        if (pixabayApiKey.isEmpty() || now < pixabayCooldownUntilMs) {
            return PixabayCallResult.TransientFailure
        }

        val variationSeed = abs("${word.subtopicId}_${word.german}".hashCode())
        var sawTransientError = false
        var bestFreshHits: PixabayCallResult.Hits? = null
        var fallbackDuplicateHits: PixabayCallResult.Hits? = null
        var nonFallbackStepsTried = 0

        for (step in plan.steps) {
            if (System.currentTimeMillis() < pixabayCooldownUntilMs) {
                return bestFreshHits ?: fallbackDuplicateHits ?: PixabayCallResult.TransientFailure
            }

            // Cap non-fallback probes once we already found a reasonable fresh hit so speed stays snappy
            if (!step.isSubtopicFallback && bestFreshHits != null && nonFallbackStepsTried >= MAX_QUALITY_PROBE_STEPS) {
                return bestFreshHits
            }
            if (step.isSubtopicFallback && bestFreshHits != null) {
                return bestFreshHits
            }

            if (!step.isSubtopicFallback) {
                nonFallbackStepsTried++
            }

            when (val res = queryPixabay(step, plan.scoringProfile, variationSeed)) {
                is PixabayCallResult.Hits -> {
                    val hasFreshUrl = res.ranked.any { it.imageUrl !in usedUrlsInSubtopic }
                    if (hasFreshUrl) {
                        if (bestFreshHits == null || res.bestScore < bestFreshHits.bestScore) {
                            bestFreshHits = res
                        }
                        // Fast 1-call exit whenever we hit a crisp, high-confidence visual match
                        if (step.isSubtopicFallback || res.bestScore <= GOOD_MATCH_SCORE_THRESHOLD) {
                            return bestFreshHits
                        }
                    } else if (fallbackDuplicateHits == null || res.bestScore < fallbackDuplicateHits.bestScore) {
                        fallbackDuplicateHits = res
                    }
                }
                PixabayCallResult.Empty -> continue
                PixabayCallResult.TransientFailure -> {
                    sawTransientError = true
                    if (System.currentTimeMillis() < pixabayCooldownUntilMs) {
                        return bestFreshHits ?: fallbackDuplicateHits ?: PixabayCallResult.TransientFailure
                    }
                }
            }
        }

        if (bestFreshHits != null) return bestFreshHits
        if (fallbackDuplicateHits != null) return fallbackDuplicateHits
        return if (sawTransientError) PixabayCallResult.TransientFailure else PixabayCallResult.Empty
    }

    private fun queryPixabay(
        step: PixabaySearchStep,
        profile: PhotoScoringProfile,
        variationSeed: Int
    ): PixabayCallResult {
        if (step.query.isBlank() || System.currentTimeMillis() < pixabayCooldownUntilMs) {
            return PixabayCallResult.TransientFailure
        }
        val encodedQ = URLEncoder.encode(step.query, "UTF-8")
        val url = "https://pixabay.com/api/?key=$pixabayApiKey&q=$encodedQ&lang=${step.lang}" +
            "&image_type=photo&orientation=horizontal&safesearch=true&per_page=20"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        return try {
            apiHttpClient.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    val resetSeconds = response.header("X-RateLimit-Reset")?.toLongOrNull()
                    val cooldownMs = if (resetSeconds != null && resetSeconds in 1L..60L) {
                        (resetSeconds + 1L) * 1000L
                    } else {
                        DEFAULT_RATE_LIMIT_COOLDOWN_MS
                    }
                    pixabayCooldownUntilMs = System.currentTimeMillis() + cooldownMs
                    Log.w(TAG, "Pixabay rate limit hit; cooling down for ${cooldownMs / 1000}s")
                    return PixabayCallResult.TransientFailure
                }
                if (!response.isSuccessful) {
                    Log.w(TAG, "Pixabay HTTP ${response.code} for '${step.query}' (${step.lang})")
                    return if (response.code >= 500 || response.code == 408) {
                        PixabayCallResult.TransientFailure
                    } else {
                        PixabayCallResult.Empty
                    }
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return PixabayCallResult.Empty
                val hits = JSONObject(body).optJSONArray("hits") ?: return PixabayCallResult.Empty
                val candidates = ArrayList<PhotoCandidate>(hits.length())
                for (i in 0 until hits.length()) {
                    val item = hits.optJSONObject(i) ?: continue
                    val imageUrl = item.optString("webformatURL", "").trim()
                    if (imageUrl.isEmpty()) continue
                    val w = item.optInt("webformatWidth", item.optInt("imageWidth", 640))
                    val h = item.optInt("webformatHeight", item.optInt("imageHeight", 480))
                    val user = item.optString("user", "").trim()
                    val rawTags = item.optString("tags", "")
                    val tags = rawTags.split(',')
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() }
                    val likes = item.optInt("likes", 0)
                    candidates.add(PhotoCandidate(imageUrl, w, h, user, tags, likes))
                }
                if (candidates.isEmpty()) return PixabayCallResult.Empty

                val scored = candidates.mapIndexed { idx, cand ->
                    cand to scoreCandidate(cand, idx, profile)
                }.sortedBy { it.second }

                val bestScore = scored.firstOrNull()?.second ?: Float.MAX_VALUE
                val rankedCandidates = if (step.isSubtopicFallback) {
                    rankSubtopicFallbackCandidates(candidates, profile, variationSeed)
                } else {
                    scored.map { it.first }
                }
                if (rankedCandidates.isEmpty()) return PixabayCallResult.Empty

                PixabayCallResult.Hits(
                    ranked = rankedCandidates.take(12).map { cand ->
                        CachedStockPhoto(
                            imageUrl = cand.url,
                            provider = StockPhotoProvider.PIXABAY,
                            photographer = cand.photographer
                        )
                    },
                    bestScore = bestScore
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pixabay request error for '${step.query}' (${step.lang}): ${e.message}")
            PixabayCallResult.TransientFailure
        }
    }

    private fun cacheKey(word: WordEntity): String =
        "${word.subtopicId.lowercase()}_${word.german.lowercase().trim()}"

    private fun ensureSubtopicIndexHydratedLocked() {
        if (subtopicIndexHydrated) return
        prefs.all.forEach { (k, v) ->
            if (v is String) {
                val url = v.substringAfter('|', "").substringBefore('|')
                val subtopicId = extractSubtopicPrefix(k)
                if (url.isNotBlank() && subtopicId != null) {
                    subtopicUrlIndex.getOrPut(subtopicId) { HashMap() }[k] = url
                }
            }
        }
        subtopicIndexHydrated = true
    }

    private fun extractSubtopicPrefix(cacheKey: String): String? {
        val lastSep = cacheKey.lastIndexOf('_')
        return if (lastSep > 0) cacheKey.substring(0, lastSep) else null
    }

    private fun recordSubtopicUrl(subtopicId: String, key: String, url: String) {
        synchronized(subtopicSelectionLock) {
            recordSubtopicUrlLocked(subtopicId, key, url)
        }
    }

    private fun recordSubtopicUrlLocked(subtopicId: String, key: String, url: String) {
        ensureSubtopicIndexHydratedLocked()
        subtopicUrlIndex.getOrPut(subtopicId.lowercase()) { HashMap() }[key] = url
    }

    private fun getUsedUrlsInSubtopic(subtopicId: String, excludeKey: String): Set<String> {
        ensureSubtopicIndexHydratedLocked()
        val entries = subtopicUrlIndex[subtopicId.lowercase()] ?: return emptySet()
        val used = HashSet<String>(entries.size)
        entries.forEach { (k, url) ->
            if (k != excludeKey && url.isNotBlank()) {
                used.add(url)
            }
        }
        return used
    }

    private fun readFromDiskCache(key: String): CachedStockPhoto? {
        val raw = prefs.getString(key, null) ?: return null
        val parts = raw.split('|')
        if (parts.size < 2) return null
        val provider = StockPhotoProvider.entries.firstOrNull { it.name == parts[0] } ?: return null
        val url = parts[1]
        if (url.isBlank()) return null
        val photographer = parts.getOrNull(2).orEmpty()
        return CachedStockPhoto(url, provider, photographer)
    }

    private fun writeToDiskCache(key: String, photo: CachedStockPhoto) {
        val safeAuthor = photo.photographer.replace('|', ' ')
        prefs.edit().putString(key, "${photo.provider.name}|${photo.imageUrl}|$safeAuthor").apply()
    }

    companion object {
        private const val TAG = "StockPhotoRepository"
        private const val PREFS_NAME = "deutschlern_stock_photo_cache_v4"
        private const val DEFAULT_RATE_LIMIT_COOLDOWN_MS = 15_000L
        private const val TARGET_ASPECT_RATIO = 4f / 3f
        internal const val CARD_DECODE_WIDTH = 640
        internal const val CARD_DECODE_HEIGHT = 480
        internal const val THUMB_DECODE_SIZE = 200
        internal const val GOOD_MATCH_SCORE_THRESHOLD = 0.35f
        private const val MAX_QUALITY_PROBE_STEPS = 3

        private val tagWordSplitRegex = Regex("[\\s\\-/]+")

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        private val universalJunkTags = setOf(
            "clipart", "vector", "illustration", "cartoon", "logo", "icon",
            "text", "quote", "typography", "collage", "mockup", "frame",
            "background", "wallpaper", "abstract", "banner", "template", "drawing",
            "ai", "digital", "3d", "render", "cgi", "surreal", "fantasy",
            "silhouette", "scrabble", "letters", "alphabet", "word", "words",
            "chalkboard", "blackboard", "infographic", "diagram", "chart",
            "sticker", "emoji", "emoticon", "meme"
        )

        private val universalJunkPhrases = setOf(
            "ai generated", "digital art", "word cloud", "text message"
        )

        @Volatile
        private var INSTANCE: StockPhotoRepository? = null

        fun getInstance(context: Context): StockPhotoRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StockPhotoRepository(context).also { INSTANCE = it }
            }
        }

        internal fun sanitizeApiKey(raw: String?, placeholder: String): String {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty() || trimmed.equals(placeholder, ignoreCase = true)) {
                return ""
            }
            if (trimmed.startsWith("YOUR_", ignoreCase = true) || trimmed.startsWith("MY_", ignoreCase = true)) {
                return ""
            }
            return trimmed
        }

        /**
         * Whole-word / clean-plural tag matching so short words never produce false substring hits
         * (e.g., "car" must NOT match "carrot"/"scarf"; "ice" must NOT match "office"/"police").
         */
        internal fun tagMatchesToken(tag: String, token: String): Boolean {
            if (tag.isBlank() || token.isBlank()) return false
            val cleanToken = token.lowercase().trim()
            val cleanTag = tag.lowercase().trim()
            if (cleanTag == cleanToken) return true
            val words = cleanTag.split(tagWordSplitRegex)
            return words.any { w ->
                when {
                    w == cleanToken -> true
                    w == "${cleanToken}s" || w == "${cleanToken}es" -> true
                    cleanToken.length >= 5 && w.startsWith(cleanToken) -> true
                    else -> false
                }
            }
        }

        /**
         * Ranks Pixabay candidates by semantic tag relevance (whole-word primary tag match,
         * multi-word phrase synergy, subtopic boost/avoid tags, AI/text junk filtering, likes quality)
         * combined with 4:3 horizontal framing fitness. Lower score = better match.
         */
        internal fun rankCandidates(
            candidates: List<PhotoCandidate>,
            profile: PhotoScoringProfile
        ): List<PhotoCandidate> {
            if (candidates.isEmpty()) return emptyList()
            return candidates.withIndex()
                .sortedBy { (index, item) -> scoreCandidate(item, index, profile) }
                .map { it.value }
        }

        internal fun scoreCandidate(
            item: PhotoCandidate,
            apiIndex: Int,
            profile: PhotoScoringProfile
        ): Float {
            val ratio = if (item.height > 0) item.width.toFloat() / item.height.toFloat() else TARGET_ASPECT_RATIO
            val isPortraitPenalty = if (ratio < 1.05f) 3.0f else 0f
            val isUltraWidePenalty = if (ratio > 2.1f) 1.5f else 0f
            val ratioDiff = abs(ratio - TARGET_ASPECT_RATIO)

            var semanticScore = 0f
            if (item.tags.isNotEmpty()) {
                val tagWords = item.tags.flatMap { it.split(tagWordSplitRegex) }
                    .filter { it.isNotEmpty() }
                    .toSet()

                // 1. Multi-word query phrase synergy (e.g., "bell pepper", "orange juice", "school building")
                if (profile.primaryQueryTokens.size >= 2) {
                    val matchedPrimaryCount = profile.primaryQueryTokens.count { token ->
                        item.tags.any { tag -> tagMatchesToken(tag, token) }
                    }
                    semanticScore += when {
                        matchedPrimaryCount == profile.primaryQueryTokens.size -> -2.2f
                        matchedPrimaryCount == 1 -> 0.5f
                        else -> 1.8f
                    }
                }

                // 2. Primary tag position bonus using whole-word matching (no substring false positives!)
                if (profile.subjectTokens.isNotEmpty()) {
                    val firstTag = item.tags.getOrNull(0).orEmpty()
                    val secondTag = item.tags.getOrNull(1).orEmpty()
                    val topFive = item.tags.take(5)
                    semanticScore += when {
                        profile.subjectTokens.any { token -> tagMatchesToken(firstTag, token) } -> -2.4f
                        profile.subjectTokens.any { token -> tagMatchesToken(secondTag, token) } -> -1.6f
                        topFive.any { tag -> profile.subjectTokens.any { token -> tagMatchesToken(tag, token) } } -> -0.7f
                        else -> 1.4f
                    }
                }

                // 3. Subtopic preferred tags boost (e.g., building/interior/shop for city_places)
                if (profile.preferredTags.isNotEmpty()) {
                    val prefHits = tagWords.count { it in profile.preferredTags }
                    semanticScore -= (prefHits * 0.85f).coerceAtMost(2.55f)
                }

                // 4. Subtopic avoid tags penalty (e.g., pills/children/laptop for city_places)
                if (profile.avoidTags.isNotEmpty()) {
                    val avoidHits = tagWords.count { it in profile.avoidTags }
                    semanticScore += (avoidHits * 2.2f).coerceAtMost(6.6f)
                }

                // 5. Universal junk & AI/text-board tag penalty
                if (!profile.allowGraphicStyle) {
                    val junkWordHits = tagWords.count { it in universalJunkTags && it !in profile.subjectTokens }
                    val junkPhraseHits = item.tags.count { it in universalJunkPhrases }
                    semanticScore += ((junkWordHits + junkPhraseHits) * 2.0f).coerceAtMost(4.5f)
                }
            }

            // 6. Gentle popularity quality tie-breaker (iconic crisp photos vs 0-like random uploads)
            val likesBonus = if (item.likes > 0) {
                (ln((item.likes + 1).toDouble()).toFloat() * 0.09f).coerceAtMost(0.55f)
            } else {
                0f
            }

            return ratioDiff + isPortraitPenalty + isUltraWidePenalty + semanticScore - likesBonus + (apiIndex * 0.05f)
        }

        internal fun pickBestLandscapeCandidate(
            candidates: List<PhotoCandidate>,
            profile: PhotoScoringProfile = PhotoScoringProfile(emptySet(), emptySet(), emptySet())
        ): PhotoCandidate? = rankCandidates(candidates, profile).firstOrNull()

        internal fun selectDistinctCandidate(
            ranked: List<CachedStockPhoto>,
            usedUrlsInSubtopic: Set<String>
        ): CachedStockPhoto? {
            if (ranked.isEmpty()) return null
            return ranked.firstOrNull { it.imageUrl !in usedUrlsInSubtopic } ?: ranked.first()
        }

        internal fun selectNextCycleCandidate(
            candidates: List<CachedStockPhoto>,
            currentUrl: String?,
            usedUrlsByOtherWords: Set<String>
        ): CachedStockPhoto {
            val currentIdx = candidates.indexOfFirst { it.imageUrl == currentUrl }
            val rotated = List(candidates.size) { offset ->
                candidates[(currentIdx + 1 + offset) % candidates.size]
            }
            return rotated.firstOrNull { it.imageUrl != currentUrl && it.imageUrl !in usedUrlsByOtherWords }
                ?: rotated.first()
        }

        internal fun rankSubtopicFallbackCandidates(
            candidates: List<PhotoCandidate>,
            profile: PhotoScoringProfile,
            variationSeed: Int
        ): List<PhotoCandidate> {
            val ranked = rankCandidates(candidates, profile)
            if (ranked.size <= 1) return ranked
            val topPool = ranked.take(12)
            val start = abs(variationSeed) % topPool.size
            return List(topPool.size) { offset -> topPool[(start + offset) % topPool.size] }
        }
    }
}
