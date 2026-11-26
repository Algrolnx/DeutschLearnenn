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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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
    private val offlinePhotoDir = File(appContext.filesDir, "offline_word_photos").apply { mkdirs() }
    private val tempPhotoSeq = AtomicLong(0L)
    private val memoryUrlCache = ConcurrentHashMap<String, CachedStockPhoto>()
    private val alternativesCache = ConcurrentHashMap<String, List<CachedStockPhoto>>()
    private val verifiedEmptyCache = ConcurrentHashMap<String, Boolean>()
    private val inFlight = ConcurrentHashMap<String, Deferred<CachedStockPhoto?>>()

    // Separate concurrency lanes: visible cards never wait behind background prefetching
    private val foregroundApiGuard = Semaphore(permits = 5)
    private val prefetchApiGuard = Semaphore(permits = 2)
    private val bulkApiGuard = Semaphore(permits = 3)

    private val subtopicSelectionLock = Any()
    private val subtopicUrlIndex = HashMap<String, MutableMap<String, String>>()
    @Volatile
    private var subtopicIndexHydrated = false

    @Volatile
    private var pixabayCooldownUntilMs: Long = 0L

    init {
        migrateAdjectivePhotoCacheOnce()
        migrateHomonymPhotoCacheOnce()
    }

    /**
     * One-time targeted cleanup of previously cached abstract-adjective photo URLs
     * (e.g., "schlecht" -> spa bridge, "schrecklich" -> Ivan the Terrible statue)
     * without touching any other subtopics or offline-downloaded noun/verb photos.
     */
    private fun migrateAdjectivePhotoCacheOnce() {
        if (prefs.getBoolean(KEY_ADJ_CACHE_MIGRATED_V1, false)) return
        val stalePrefixes = listOf(
            "adj_qualities_",
            "adj_size_time_",
            "adj_sensory_",
            "adj_personality_"
        )
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (stalePrefixes.any { prefix -> key.startsWith(prefix) }) {
                editor.remove(key)
                localPhotoFileForKey(key).delete()
            }
        }
        editor.putBoolean(KEY_ADJ_CACHE_MIGRATED_V1, true).apply()
    }

    /**
     * One-time targeted cleanup of previously cached homonym / uncategorized restaurant photos
     * (e.g., "Vorspeise" -> car starter motor, "Tablette" -> iPad, "Hobel" -> airplane)
     * so they immediately refresh with domain-locked Pixabay category & disambiguated queries
     * while preserving all valid offline photos.
     */
    private fun migrateHomonymPhotoCacheOnce() {
        if (prefs.getBoolean(KEY_HOMONYM_CACHE_MIGRATED_V2, false)) return
        val staleExactKeys = setOf(
            "food_cooking_öl", "food_cooking_pfeffer", "food_cooking_pfanne", "food_cooking_topf",
            "food_cooking_schneiden", "food_cooking_schälen", "food_cooking_rühren",
            "food_cooking_mischen", "food_cooking_würzen", "food_cooking_servieren",
            "food_cooking_backen", "food_cooking_braten",
            "food_sweets_bonbon", "food_sweets_torte", "food_sweets_sahne",
            "food_meals_marmelade", "food_meals_toast",
            "food_meat_fish_fisch", "food_meat_fish_hähnchen", "food_meat_fish_ente",
            "food_meat_fish_gans", "food_meat_fish_truthahn", "food_meat_fish_pute", "food_meat_fish_krabbe",
            "food_drinks_punsch", "food_fruits_limette", "food_fruits_dattel",
            "travel_station_haltestelle", "travel_station_schalter", "travel_station_sitzplatz",
            "travel_station_gleis", "travel_station_drehkreuz",
            "travel_hotel_aufzug", "travel_hotel_übernachtung", "travel_hotel_zimmer",
            "travel_hotel_schlüssel", "travel_hotel_liegeplatz", "travel_hotel_wlan",
            "travel_city_nav_platz", "travel_city_nav_links", "travel_city_nav_rechts",
            "travel_city_nav_geradeaus", "travel_city_nav_richtung", "travel_city_nav_norden",
            "travel_city_nav_osten", "travel_city_nav_süden",
            "travel_transport_strafe", "travel_transport_sattel", "travel_transport_kette",
            "work_tools_mutter", "work_tools_feile", "work_tools_hobel",
            "work_tools_schraubstock", "work_tools_mörtel", "work_tools_bolzen", "work_tools_nagel",
            "work_office_stempel", "work_office_schicht", "work_business_bestellung", "work_business_auftrag",
            "health_illness_tablette", "health_illness_pflaster", "health_illness_erkältung",
            "city_emergency_pflaster", "medicine_care_gips", "fitness_wellness_planke",
            "appearance_hygiene_taschentuch", "appearance_hygiene_creme", "home_bathroom_taschentuch",
            "body_parts_gelenk", "body_parts_brustkorb", "body_parts_rücken", "body_parts_ferse",
            "city_places_gericht", "city_places_bar", "city_places_diskothek",
            "society_politics_gericht", "society_religion_glocke", "society_religion_tempel",
            "city_services_wechselgeld", "city_services_filiale", "city_services_pin-code", "city_services_zinsen",
            "shopping_money_wechselgeld", "shopping_money_filiale", "shopping_money_pin", "shopping_money_zinsen",
            "shopping_materials_kapuze", "shopping_shoes_absatz", "home_furniture_hocker",
            "education_supplies_lineal", "education_languages_buchstabe", "education_languages_regel",
            "education_school_note", "education_school_fach",
            "music_instruments_band", "music_instruments_takt", "music_instruments_bühne",
            "gaming_entertainment_level", "gaming_entertainment_schauspiel", "gaming_entertainment_theaterstück",
            "nature_weather_schauer", "nature_weather_kälte", "nature_weather_eis",
            "nature_landscape_quelle", "nature_landscape_ebene", "nature_landscape_hafen",
            "nature_plants_zapfen", "nature_wildlife_fliege", "nature_animals_kalb",
            "tech_computers_taste", "tech_computers_laufwerk", "tech_computers_datei", "tech_computers_ordner",
            "tech_internet_cookie", "tech_internet_link", "tech_internet_spam",
            "tech_devices_schalter", "tech_devices_tablet",
            "tech_data_branch", "tech_data_port", "tech_data_log", "tech_data_bit", "tech_data_skript",
            "tech_science_tabelle", "tech_science_zelle",
            "people_relationships_vorstand", "people_relationships_verein", "people_relationships_brief",
            "adj_size_time_momentan", "adj_size_time_doppelt"
        )
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            val lower = key.lowercase()
            if (lower.startsWith("food_restaurant_") || lower in staleExactKeys) {
                editor.remove(key)
                localPhotoFileForKey(key).delete()
            }
        }
        staleExactKeys.forEach { key -> localPhotoFileForKey(key).delete() }
        editor.putBoolean(KEY_HOMONYM_CACHE_MIGRATED_V2, true).apply()
    }

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

    private fun localPhotoFileForKey(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        val hash = digest.joinToString("") { "%02x".format(it) }.take(32)
        return File(offlinePhotoDir, "$hash.jpg")
    }

    /**
     * Returns the permanent on-device photo file in filesDir if downloaded, or null if not yet saved.
     * Local files never expire (unlike Pixabay's 24h webformatURLs) and are never evicted by cacheDir pruning.
     */
    fun getLocalPhotoFile(word: WordEntity): File? {
        val file = localPhotoFileForKey(cacheKey(word))
        return if (file.exists() && file.length() > MIN_VALID_PHOTO_BYTES) file else null
    }

    fun deleteCorruptLocalPhoto(word: WordEntity) {
        try {
            localPhotoFileForKey(cacheKey(word)).delete()
        } catch (_: Exception) {
        }
    }

    fun savePhotoToPermanentCacheAsync(word: WordEntity, imageUrl: String) {
        if (imageUrl.isBlank()) return
        val targetFile = localPhotoFileForKey(cacheKey(word))
        if (targetFile.exists() && targetFile.length() > MIN_VALID_PHOTO_BYTES) return
        repoScope.launch {
            downloadUrlToLocalFile(imageUrl, targetFile)
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
        // Replace any previously persisted local file with the newly chosen photo
        localPhotoFileForKey(key).delete()
        savePhotoToPermanentCacheAsync(word, next.imageUrl)
        return next
    }

    /**
     * Fire-and-forget parallel background prefetch on the repository scope so card navigation
     * never cancels an in-flight lookup and upcoming cards are already decoded in Coil RAM cache.
     */
    fun prefetchWordsAsync(words: List<WordEntity>) {
        if (words.isEmpty()) return
        val uniqueWords = words.distinctBy { cacheKey(it) }.take(4)
        for (word in uniqueWords) {
            val localFile = getLocalPhotoFile(word)
            if (localFile != null) {
                val memKey = "file:${localFile.absolutePath}:${localFile.lastModified()}"
                if (!isInMemoryCache(memKey, memKey)) {
                    val request = ImageRequest.Builder(appContext)
                        .data(localFile)
                        .size(CARD_DECODE_WIDTH, CARD_DECODE_HEIGHT)
                        .memoryCacheKey(memKey)
                        .build()
                    imageLoader.enqueue(request)
                }
                continue
            }
            if (!hasConfiguredApiKey() || System.currentTimeMillis() < pixabayCooldownUntilMs) {
                continue
            }
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
     * Returns true if this word has its image bytes saved locally on device (permanent file
     * or legacy Coil disk cache) so it can render reliably without internet.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun isPhotoDiskCached(word: WordEntity): Boolean {
        if (getLocalPhotoFile(word) != null) return true
        val cached = getCachedSync(word) ?: return false
        return try {
            val diskCache = imageLoader.diskCache ?: return false
            diskCache.openSnapshot(cached.imageUrl)?.use { snapshot ->
                snapshot.data.toFile().length() > MIN_VALID_PHOTO_BYTES
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun executeImageCall(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = imageHttpClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) {
                        cont.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }

    private suspend fun downloadUrlToLocalFile(imageUrl: String, targetFile: File): Boolean =
        withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            if (targetFile.exists() && targetFile.length() > MIN_VALID_PHOTO_BYTES) {
                return@withContext true
            }
            val tmpFile = File(offlinePhotoDir, "${targetFile.name}.${tempPhotoSeq.incrementAndGet()}.tmp")
            try {
                val request = Request.Builder()
                    .url(imageUrl)
                    .build()
                executeImageCall(request).use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val body = response.body ?: return@withContext false
                    val contentType = response.header("Content-Type").orEmpty()
                    if (contentType.isNotBlank() && !contentType.startsWith("image/", ignoreCase = true)) {
                        return@withContext false
                    }
                    body.byteStream().use { input ->
                        tmpFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (tmpFile.length() <= MIN_VALID_PHOTO_BYTES) {
                    tmpFile.delete()
                    return@withContext false
                }
                if (targetFile.exists() && targetFile.length() > MIN_VALID_PHOTO_BYTES) {
                    tmpFile.delete()
                    return@withContext true
                }
                if (tmpFile.renameTo(targetFile)) {
                    true
                } else {
                    tmpFile.copyTo(targetFile, overwrite = true)
                    tmpFile.delete()
                    targetFile.exists() && targetFile.length() > MIN_VALID_PHOTO_BYTES
                }
            } catch (ce: CancellationException) {
                tmpFile.delete()
                throw ce
            } catch (e: Exception) {
                tmpFile.delete()
                Log.w(TAG, "Direct offline photo save failed for $imageUrl: ${e.message}")
                false
            }
        }

    /**
     * Resolves a photo URL directly in the caller's coroutine context for bulk offline downloads.
     * Supports immediate coroutine cancellation (including aborting the active HTTP call) and uses
     * single-call fast resolution so bulk preloading preserves Pixabay API quota.
     */
    private suspend fun resolvePhotoForBulk(word: WordEntity, forceFresh: Boolean = false): CachedStockPhoto? {
        coroutineContext.ensureActive()
        val key = cacheKey(word)
        if (!forceFresh) {
            memoryUrlCache[key]?.let { return it }
            readFromDiskCache(key)?.let { cached ->
                memoryUrlCache[key] = cached
                recordSubtopicUrl(word.subtopicId, key, cached.imageUrl)
                return cached
            }
        }
        if (!hasConfiguredApiKey() || (!forceFresh && verifiedEmptyCache.containsKey(key)) || remainingCooldownMs() > 0L) {
            return null
        }

        val outcome = bulkApiGuard.withPermit {
            coroutineContext.ensureActive()
            if (remainingCooldownMs() > 0L) return@withPermit PixabayCallResult.TransientFailure
            val usedSnapshot = synchronized(subtopicSelectionLock) {
                getUsedUrlsInSubtopic(word.subtopicId, excludeKey = key)
            }
            fetchFromPixabay(word, usedSnapshot, fastBulk = true)
        }

        return when (outcome) {
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
    }

    /**
     * Resolves (if needed) and saves the actual JPEG bytes permanently into `filesDir/offline_word_photos`
     * so every downloaded word displays reliably offline without depending on expiring URLs or `cacheDir`.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun preloadWordForOffline(word: WordEntity, fastBulk: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        val key = cacheKey(word)
        val targetFile = localPhotoFileForKey(key)
        if (targetFile.exists() && targetFile.length() > MIN_VALID_PHOTO_BYTES) {
            return@withContext true
        }

        val existingCached = getCachedSync(word)
        // If Coil already has this image in legacy cacheDir, copy it straight into permanent filesDir
        if (existingCached != null) {
            try {
                val migrated = imageLoader.diskCache?.openSnapshot(existingCached.imageUrl)?.use { snapshot ->
                    val src = snapshot.data.toFile()
                    if (src.exists() && src.length() > MIN_VALID_PHOTO_BYTES) {
                        src.copyTo(targetFile, overwrite = true)
                        targetFile.exists() && targetFile.length() > MIN_VALID_PHOTO_BYTES
                    } else {
                        false
                    }
                } ?: false
                if (migrated) return@withContext true
            } catch (_: Exception) {
            }
        }

        val resolved = existingCached ?: run {
            if (!hasConfiguredApiKey() || remainingCooldownMs() > 0L) return@withContext false
            if (fastBulk) resolvePhotoForBulk(word) else resolvePhoto(word, highPriority = false)
        } ?: return@withContext false

        coroutineContext.ensureActive()
        if (downloadUrlToLocalFile(resolved.imageUrl, targetFile)) {
            return@withContext true
        }

        // If an older cached Pixabay URL expired (403/404), fetch a fresh URL once and save it
        if (existingCached != null && hasConfiguredApiKey() && remainingCooldownMs() == 0L) {
            coroutineContext.ensureActive()
            val refreshed = resolvePhotoForBulk(word, forceFresh = true) ?: return@withContext false
            return@withContext downloadUrlToLocalFile(refreshed.imageUrl, targetFile)
        }

        false
    }

    /**
     * Refreshes an expired Pixabay URL without erasing an existing cached entry if offline.
     */
    suspend fun invalidateAndRefreshPhoto(word: WordEntity): CachedStockPhoto? {
        if (!hasConfiguredApiKey()) return getCachedSync(word)
        val key = cacheKey(word)
        val previous = getCachedSync(word)
        verifiedEmptyCache.remove(key)
        val outcome = foregroundApiGuard.withPermit {
            withContext(Dispatchers.IO) {
                val usedSnapshot = synchronized(subtopicSelectionLock) {
                    getUsedUrlsInSubtopic(word.subtopicId, excludeKey = key)
                }
                fetchFromPixabay(word, usedSnapshot)
            }
        }
        return when (outcome) {
            is PixabayCallResult.Hits -> {
                val chosen = synchronized(subtopicSelectionLock) {
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
                if (chosen != null) {
                    localPhotoFileForKey(key).delete()
                    savePhotoToPermanentCacheAsync(word, chosen.imageUrl)
                }
                chosen ?: previous
            }
            else -> previous
        }
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
        var createdTask: Deferred<CachedStockPhoto?>? = null
        val deferred = inFlight.computeIfAbsent(key) {
            val task = repoScope.async(start = CoroutineStart.LAZY) {
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
                } catch (t: Throwable) {
                    Log.w(TAG, "Stock photo lookup failed for '${word.german}': ${t.message}")
                    null
                }
            }
            createdTask = task
            task
        }
        // Attach completion cleanup and start strictly AFTER computeIfAbsent releases its bin lock
        // to avoid ConcurrentHashMap "IllegalStateException: Recursive update" crashes on Android.
        createdTask?.let { task ->
            task.invokeOnCompletion {
                inFlight.remove(key, task)
            }
            task.start()
        }

        return try {
            deferred.await()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Stock photo await failed for '${word.german}': ${t.message}")
            null
        }
    }

    private suspend fun fetchFromPixabay(
        word: WordEntity,
        usedUrlsInSubtopic: Set<String> = emptySet(),
        fastBulk: Boolean = false
    ): PixabayCallResult {
        coroutineContext.ensureActive()
        val plan = StockPhotoQueryBuilder.build(word)
        val now = System.currentTimeMillis()
        if (pixabayApiKey.isEmpty() || now < pixabayCooldownUntilMs) {
            return PixabayCallResult.TransientFailure
        }

        val variationSeed = "${word.subtopicId}_${word.german}".hashCode() and 0x7fffffff
        var sawTransientError = false
        var bestFreshHits: PixabayCallResult.Hits? = null
        var fallbackDuplicateHits: PixabayCallResult.Hits? = null
        var nonFallbackStepsTried = 0

        for (step in plan.steps) {
            coroutineContext.ensureActive()
            if (System.currentTimeMillis() < pixabayCooldownUntilMs) {
                return bestFreshHits ?: fallbackDuplicateHits ?: PixabayCallResult.TransientFailure
            }

            // Cap non-fallback probes once we already found a reasonable fresh hit so speed stays snappy
            val probeLimit = if (fastBulk) MAX_BULK_PROBE_STEPS else MAX_QUALITY_PROBE_STEPS
            if (!step.isSubtopicFallback && bestFreshHits != null && nonFallbackStepsTried >= probeLimit) {
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
                        // Fast 1-call exit whenever we hit a confident match (bulk mode also self-corrects noisy hits)
                        val bulkAcceptable = fastBulk && res.bestScore <= BULK_ACCEPTABLE_SCORE_THRESHOLD
                        if (bulkAcceptable || step.isSubtopicFallback || res.bestScore <= GOOD_MATCH_SCORE_THRESHOLD) {
                            return bestFreshHits
                        }
                    } else if (fallbackDuplicateHits == null || res.bestScore < fallbackDuplicateHits.bestScore) {
                        fallbackDuplicateHits = res
                        if (fastBulk && step.isSubtopicFallback) {
                            return fallbackDuplicateHits
                        }
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

    private suspend fun executeApiCall(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = apiHttpClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) {
                        cont.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }

    private suspend fun queryPixabay(
        step: PixabaySearchStep,
        profile: PhotoScoringProfile,
        variationSeed: Int
    ): PixabayCallResult {
        coroutineContext.ensureActive()
        if (step.query.isBlank() || System.currentTimeMillis() < pixabayCooldownUntilMs) {
            return PixabayCallResult.TransientFailure
        }
        val encodedQ = URLEncoder.encode(step.query, "UTF-8")
        val categoryParam = step.pixabayCategory
            ?.takeIf { it.isNotBlank() }
            ?.let { "&category=$it" }
            .orEmpty()
        val url = "https://pixabay.com/api/?key=$pixabayApiKey&q=$encodedQ&lang=${step.lang}" +
            "$categoryParam&image_type=photo&orientation=horizontal&safesearch=true&per_page=20"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        return try {
            executeApiCall(request).use { response ->
                val remainingHeader = response.header("X-RateLimit-Remaining")?.toIntOrNull()
                val resetSeconds = response.header("X-RateLimit-Reset")?.toLongOrNull()

                if (response.code == 429) {
                    val cooldownMs = if (resetSeconds != null && resetSeconds in 1L..65L) {
                        (resetSeconds + 1L) * 1000L
                    } else {
                        DEFAULT_RATE_LIMIT_COOLDOWN_MS
                    }
                    pixabayCooldownUntilMs = maxOf(pixabayCooldownUntilMs, System.currentTimeMillis() + cooldownMs)
                    Log.w(TAG, "Pixabay rate limit hit; cooling down for ${cooldownMs / 1000}s")
                    return PixabayCallResult.TransientFailure
                }

                // Proactively respect Pixabay's remaining quota before concurrent workers trigger 429s
                if (remainingHeader != null && remainingHeader <= 2 && resetSeconds != null && resetSeconds in 1L..65L) {
                    val cooldownMs = (resetSeconds + 1L) * 1000L
                    pixabayCooldownUntilMs = maxOf(pixabayCooldownUntilMs, System.currentTimeMillis() + cooldownMs)
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
        } catch (ce: CancellationException) {
            throw ce
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
        private const val KEY_ADJ_CACHE_MIGRATED_V1 = "__adj_photo_cache_migrated_v1"
        private const val KEY_HOMONYM_CACHE_MIGRATED_V2 = "__homonym_photo_cache_migrated_v2"
        private const val DEFAULT_RATE_LIMIT_COOLDOWN_MS = 15_000L
        private const val MIN_VALID_PHOTO_BYTES = 1024L
        private const val TARGET_ASPECT_RATIO = 4f / 3f
        internal const val CARD_DECODE_WIDTH = 640
        internal const val CARD_DECODE_HEIGHT = 480
        internal const val THUMB_DECODE_SIZE = 200
        internal const val GOOD_MATCH_SCORE_THRESHOLD = 0.35f
        private const val BULK_ACCEPTABLE_SCORE_THRESHOLD = 1.2f
        private const val MAX_QUALITY_PROBE_STEPS = 3
        private const val MAX_BULK_PROBE_STEPS = 2

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
            val start = (variationSeed and 0x7fffffff) % topPool.size
            return List(topPool.size) { offset -> topPool[(start + offset) % topPool.size] }
        }
    }
}
