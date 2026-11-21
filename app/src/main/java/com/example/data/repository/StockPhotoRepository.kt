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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

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
    data class Hits(val ranked: List<CachedStockPhoto>) : PixabayCallResult
    data object Empty : PixabayCallResult
    data object TransientFailure : PixabayCallResult
}

/**
 * Resolves relevant stock photographs for German vocabulary cards via Pixabay API.
 * - Uses Secrets Gradle Plugin (`BuildConfig.PIXABAY_API_KEY`).
 * - Inspects Pixabay's `tags` array across up to 20 candidates per single request,
 *   ranking candidates by primary-tag match, subtopic-preferred/avoided tags, and 4:3 framing.
 * - Permanently caches chosen URLs on-device (SharedPreferences) and image bytes (Coil 120 MB DiskCache).
 * - Supports one-tap cycling through alternative ranked photos (`cycleNextPhoto`).
 */
class StockPhotoRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(req)
        }
        .build()

    val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .okHttpClient(httpClient)
        .memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(appContext.cacheDir, "word_photos_coil"))
                .maxSizeBytes(120L * 1024L * 1024L)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memoryUrlCache = ConcurrentHashMap<String, CachedStockPhoto>()
    private val alternativesCache = ConcurrentHashMap<String, List<CachedStockPhoto>>()
    private val verifiedEmptyCache = ConcurrentHashMap<String, Boolean>()
    private val inFlight = ConcurrentHashMap<String, Deferred<CachedStockPhoto?>>()
    private val apiConcurrencyGuard = Semaphore(permits = 3)
    private val subtopicSelectionLock = Any()

    @Volatile
    private var pixabayCooldownUntilMs: Long = 0L

    private val pixabayApiKey: String
        get() = sanitizeApiKey(BuildConfig.PIXABAY_API_KEY, "YOUR_PIXABAY_API_KEY")

    fun hasConfiguredApiKey(): Boolean = pixabayApiKey.isNotEmpty()

    fun getCachedSync(word: WordEntity): CachedStockPhoto? {
        val key = cacheKey(word)
        memoryUrlCache[key]?.let { return it }
        return readFromDiskCache(key)?.also { memoryUrlCache[key] = it }
    }

    suspend fun resolvePhoto(word: WordEntity): CachedStockPhoto? {
        val key = cacheKey(word)
        memoryUrlCache[key]?.let { return it }

        readFromDiskCache(key)?.let { cached ->
            memoryUrlCache[key] = cached
            return cached
        }

        return fetchAndCache(word, key)
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
            val outcome = apiConcurrencyGuard.withPermit {
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
            writeToDiskCache(key, chosen)
            chosen
        }
        return next
    }

    /**
     * Fire-and-forget background prefetch on the repository scope so card navigation
     * never cancels an in-flight Pixabay lookup or poisons the next card's load.
     */
    fun prefetchWordsAsync(words: List<WordEntity>) {
        if (words.isEmpty() || !hasConfiguredApiKey() || System.currentTimeMillis() < pixabayCooldownUntilMs) {
            return
        }
        repoScope.launch {
            for (word in words) {
                if (System.currentTimeMillis() < pixabayCooldownUntilMs) break
                prefetchWord(word)
            }
        }
    }

    /**
     * Gently prefetches the photo URL and warms Coil's disk/memory cache for an upcoming card.
     */
    suspend fun prefetchWord(word: WordEntity) {
        if (!hasConfiguredApiKey() || System.currentTimeMillis() < pixabayCooldownUntilMs) return
        val resolved = resolvePhoto(word) ?: return
        val request = ImageRequest.Builder(appContext)
            .data(resolved.imageUrl)
            .build()
        imageLoader.enqueue(request)
    }

    /**
     * Invalidates a stale/expired cached Pixabay URL and fetches a fresh one.
     */
    suspend fun invalidateAndRefreshPhoto(word: WordEntity): CachedStockPhoto? {
        val key = cacheKey(word)
        memoryUrlCache.remove(key)
        alternativesCache.remove(key)
        verifiedEmptyCache.remove(key)
        prefs.edit().remove(key).apply()
        return fetchAndCache(word, key)
    }

    private suspend fun fetchAndCache(word: WordEntity, key: String): CachedStockPhoto? {
        if (!hasConfiguredApiKey() || verifiedEmptyCache.containsKey(key)) {
            return null
        }

        val deferred = inFlight.computeIfAbsent(key) {
            repoScope.async {
                try {
                    val outcome = apiConcurrencyGuard.withPermit {
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
        var fallbackDuplicateHits: PixabayCallResult.Hits? = null

        for (step in plan.steps) {
            if (System.currentTimeMillis() < pixabayCooldownUntilMs) {
                return fallbackDuplicateHits ?: PixabayCallResult.TransientFailure
            }
            when (val res = queryPixabay(step, plan.scoringProfile, variationSeed)) {
                is PixabayCallResult.Hits -> {
                    val hasFreshUrl = res.ranked.any { it.imageUrl !in usedUrlsInSubtopic }
                    if (hasFreshUrl) {
                        return res
                    }
                    if (fallbackDuplicateHits == null) {
                        fallbackDuplicateHits = res
                    }
                }
                PixabayCallResult.Empty -> continue
                PixabayCallResult.TransientFailure -> {
                    sawTransientError = true
                    if (System.currentTimeMillis() < pixabayCooldownUntilMs) {
                        return fallbackDuplicateHits ?: PixabayCallResult.TransientFailure
                    }
                }
            }
        }

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
            "&image_type=photo&safesearch=true&per_page=20"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
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

                val rankedCandidates = if (step.isSubtopicFallback) {
                    rankSubtopicFallbackCandidates(candidates, profile, variationSeed)
                } else {
                    rankCandidates(candidates, profile)
                }
                if (rankedCandidates.isEmpty()) return PixabayCallResult.Empty

                PixabayCallResult.Hits(
                    rankedCandidates.take(12).map { cand ->
                        CachedStockPhoto(
                            imageUrl = cand.url,
                            provider = StockPhotoProvider.PIXABAY,
                            photographer = cand.photographer
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pixabay request error for '${step.query}' (${step.lang}): ${e.message}")
            PixabayCallResult.TransientFailure
        }
    }

    private fun cacheKey(word: WordEntity): String =
        "${word.subtopicId.lowercase()}_${word.german.lowercase().trim()}"

    private fun getUsedUrlsInSubtopic(subtopicId: String, excludeKey: String): Set<String> {
        val prefix = "${subtopicId.lowercase()}_"
        val used = HashSet<String>()
        memoryUrlCache.forEach { (k, photo) ->
            if (k != excludeKey && k.startsWith(prefix)) {
                used.add(photo.imageUrl)
            }
        }
        prefs.all.forEach { (k, v) ->
            if (k != excludeKey && k.startsWith(prefix) && v is String) {
                val url = v.substringAfter('|', "").substringBefore('|')
                if (url.isNotBlank()) {
                    used.add(url)
                }
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
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        private val universalJunkTags = setOf(
            "clipart", "vector", "illustration", "cartoon", "logo", "icon",
            "text", "quote", "typography", "collage", "mockup", "frame",
            "background", "wallpaper", "abstract", "banner", "template", "drawing"
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
         * Ranks Pixabay candidates by semantic tag relevance (primary tag match, subtopic boost/avoid tags)
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
                val tagWords = item.tags.flatMap { it.split(' ') }.toSet()

                // 1. Primary tag position bonus: photos whose 1st/2nd tag matches the actual word are far more accurate
                if (profile.subjectTokens.isNotEmpty()) {
                    val firstTag = item.tags.getOrNull(0).orEmpty()
                    val secondTag = item.tags.getOrNull(1).orEmpty()
                    val topFive = item.tags.take(5)
                    semanticScore += when {
                        profile.subjectTokens.any { token -> firstTag.contains(token) } -> -2.4f
                        profile.subjectTokens.any { token -> secondTag.contains(token) } -> -1.6f
                        topFive.any { tag -> profile.subjectTokens.any { token -> tag.contains(token) } } -> -0.7f
                        else -> 1.2f
                    }
                }

                // 2. Subtopic preferred tags boost (e.g., building/interior/shop for city_places)
                if (profile.preferredTags.isNotEmpty()) {
                    val prefHits = tagWords.count { it in profile.preferredTags }
                    semanticScore -= (prefHits * 0.85f).coerceAtMost(2.55f)
                }

                // 3. Subtopic avoid tags penalty (e.g., pills/children/laptop for city_places)
                if (profile.avoidTags.isNotEmpty()) {
                    val avoidHits = tagWords.count { it in profile.avoidTags }
                    semanticScore += (avoidHits * 2.2f).coerceAtMost(6.6f)
                }

                // 4. Universal junk tag penalty (clipart, text quotes, logos, mockups)
                if (!profile.allowGraphicStyle) {
                    val junkHits = tagWords.count { it in universalJunkTags && it !in profile.subjectTokens }
                    semanticScore += (junkHits * 2.0f).coerceAtMost(4.0f)
                }
            }

            return ratioDiff + isPortraitPenalty + isUltraWidePenalty + semanticScore + (apiIndex * 0.05f)
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
