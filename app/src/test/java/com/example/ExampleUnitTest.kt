package com.example

import com.example.data.model.TopicCatalog
import com.example.data.model.WordEntity
import com.example.data.repository.CachedStockPhoto
import com.example.data.repository.InitialVocabulary
import com.example.data.repository.PersonGenderHint
import com.example.data.repository.PhotoCandidate
import com.example.data.repository.StockPhotoProvider
import com.example.data.repository.StockPhotoQueryBuilder
import com.example.data.repository.StockPhotoRepository
import com.example.data.repository.VocabularyRepository
import com.example.ui.LearningViewModel
import com.example.ui.ReviewDashboardState
import com.example.ui.components.illustrations.WordArtworkKey
import com.example.ui.components.illustrations.WordArtworkResolver
import com.example.util.SpacedRepetition
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun allSubtopicsHave6000Vocabulary() {
        val allWords = InitialVocabulary.getInitialWords()
        val allSubtopics = TopicCatalog.mainTopics.flatMap { it.subtopics }

        assertEquals("Should contain exactly 6000 curated words", 6000, allWords.size)

        for (subtopic in allSubtopics) {
            val wordsForSubtopic = allWords.filter { it.subtopicId == subtopic.id }
            assertTrue(
                "Subtopic ${subtopic.id} (${subtopic.titleDe}) must have at least 10 words, found ${wordsForSubtopic.size}",
                wordsForSubtopic.size >= 10
            )
            for (word in wordsForSubtopic) {
                assertEquals("Word categoryId must match mainTopicId", subtopic.mainTopicId, word.categoryId)
                assertTrue("German word must not be blank", word.german.isNotBlank())
                assertTrue("Ukrainian translation must not be blank", word.translationUk.isNotBlank())
                assertTrue("English translation must not be blank", word.translationEn.isNotBlank())
            }
        }
    }

    @Test
    fun all6000WordsResolveToVectorArtwork() {
        val allWords = InitialVocabulary.getInitialWords()
        assertEquals(6000, allWords.size)

        val usedKeys = mutableSetOf<WordArtworkKey>()
        for (word in allWords) {
            val resolved = WordArtworkResolver.resolveWordArtworkKey(word)
            assertNotNull("Word '${word.german}' must resolve to a valid WordArtworkKey", resolved)
            usedKeys.add(resolved)
        }
        assertTrue(
            "All 6000 words should exercise broad visual variety (used ${usedKeys.size} keys)",
            usedKeys.size >= 140
        )
    }

    @Test
    fun concreteAndAbstractWordsResolveToExpectedIllustrations() {
        assertEquals(
            WordArtworkKey.APPLE,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Apfel", article = "der", translationUk = "яблуко", translationEn = "apple", categoryId = "food", subtopicId = "food_fruits")
            )
        )
        assertEquals(
            WordArtworkKey.BANANA,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Banane", article = "die", translationUk = "банан", translationEn = "banana", categoryId = "food", subtopicId = "food_fruits")
            )
        )
        assertEquals(
            WordArtworkKey.PEACH,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Pfirsich", article = "der", translationUk = "персик", translationEn = "peach", categoryId = "food", subtopicId = "food_fruits")
            )
        )
        assertEquals(
            WordArtworkKey.CHERRY,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Kirsche", article = "die", translationUk = "вишня", translationEn = "cherry", categoryId = "food", subtopicId = "food_fruits")
            )
        )
        assertEquals(
            WordArtworkKey.FRUIT_SALAD,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Obstsalat", article = "der", translationUk = "фруктовий салат", translationEn = "fruit salad", categoryId = "food", subtopicId = "food_fruits")
            )
        )
        assertEquals(
            WordArtworkKey.POMEGRANATE,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Granatapfel", article = "der", translationUk = "гранат", translationEn = "pomegranate", categoryId = "food", subtopicId = "food_fruits")
            )
        )
        assertEquals(
            WordArtworkKey.PINEAPPLE,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Ananas", article = "die", translationUk = "ананас", translationEn = "pineapple", categoryId = "food", subtopicId = "food_fruits")
            )
        )
        assertEquals(
            WordArtworkKey.EGGPLANT,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Aubergine", article = "die", translationUk = "баклажан", translationEn = "eggplant", categoryId = "food", subtopicId = "food_vegetables")
            )
        )
        assertEquals(
            WordArtworkKey.CAULIFLOWER,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Blumenkohl", article = "der", translationUk = "цвітна капуста", translationEn = "cauliflower", categoryId = "food", subtopicId = "food_vegetables")
            )
        )
        assertEquals(
            WordArtworkKey.DELAY_HOURGLASS,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Verspätung", article = "die", translationUk = "запізнення", translationEn = "delay", categoryId = "travel", subtopicId = "travel_station")
            )
        )
        assertEquals(
            WordArtworkKey.RAILWAY_TRACK,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Gleis", article = "das", translationUk = "колія", translationEn = "track", categoryId = "travel", subtopicId = "travel_station")
            )
        )
        assertEquals(
            WordArtworkKey.AIRPORT_TERMINAL,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Flughafen", article = "der", translationUk = "аеропорт", translationEn = "airport", categoryId = "travel", subtopicId = "travel_station")
            )
        )
        assertEquals(
            WordArtworkKey.SUN,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "trocken", article = "", translationUk = "сухий", translationEn = "dry", categoryId = "adjectives", subtopicId = "adj_sensory")
            )
        )
        assertEquals(
            WordArtworkKey.COLOR_DROP,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "silbergrau", article = "", translationUk = "сріблясто-сірий", translationEn = "silver gray", categoryId = "adjectives", subtopicId = "adj_colors")
            )
        )
        assertEquals(
            WordArtworkKey.DOCUMENT_PAGE,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Kontoauszug", article = "der", translationUk = "виписка з рахунку", translationEn = "bank statement", categoryId = "city", subtopicId = "city_services")
            )
        )
        assertEquals(
            WordArtworkKey.MONEY,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Geldautomat", article = "der", translationUk = "банкомат", translationEn = "ATM", categoryId = "city", subtopicId = "city_services")
            )
        )
        assertEquals(
            WordArtworkKey.CAR,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Auto", article = "das", translationUk = "автомобіль", translationEn = "car", categoryId = "travel", subtopicId = "travel_transport")
            )
        )
        assertEquals(
            WordArtworkKey.PHONE,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Handy", article = "das", translationUk = "мобільний телефон", translationEn = "mobile phone", categoryId = "tech", subtopicId = "tech_devices")
            )
        )
        assertEquals(
            WordArtworkKey.IDEA_SPARK,
            WordArtworkResolver.resolveWordArtworkKey(
                WordEntity(german = "Gedanke", article = "der", translationUk = "думка", translationEn = "thought / idea", categoryId = "emotions", subtopicId = "emotions_mind")
            )
        )
    }

    @Test
    fun stockPhotoQueryBuilderProducesCleanObjectQueries() {
        val eggplant = StockPhotoQueryBuilder.build(
            WordEntity(german = "Aubergine", article = "die", translationUk = "баклажан", translationEn = "eggplant", categoryId = "food", subtopicId = "food_vegetables")
        )
        assertEquals("eggplant", eggplant.steps.first().query)
        assertTrue(eggplant.steps.any { it.lang == "de" && it.query == "aubergine" })

        val delay = StockPhotoQueryBuilder.build(
            WordEntity(german = "Verspätung", article = "die", translationUk = "запізнення", translationEn = "delay", categoryId = "travel", subtopicId = "travel_station")
        )
        assertEquals("train clock", delay.steps.first().query)

        val orange = StockPhotoQueryBuilder.build(
            WordEntity(german = "Orange", article = "die", translationUk = "апельсин", translationEn = "orange", categoryId = "food", subtopicId = "food_fruits")
        )
        assertEquals("orange fruit", orange.steps.first().query)

        val messy = StockPhotoQueryBuilder.build(
            WordEntity(
                german = "unordentlich",
                article = "",
                translationUk = "безладний",
                translationEn = "untidy / messy",
                exampleDe = "Ein unordentlicher Schreibtisch voller Zettel.",
                categoryId = "adjectives",
                subtopicId = "adj_qualities"
            )
        )
        assertTrue(messy.steps.any { it.query == "untidy" && it.lang == "en" })
        assertTrue(messy.steps.any { it.query == "messy" && it.lang == "en" })
        assertTrue(messy.steps.any { it.query == "Schreibtisch" && it.lang == "de" })
    }

    @Test
    fun stockPhotoRepositorySanitizesPlaceholdersAndPrefersSemanticPlaceMatches() {
        assertEquals("", StockPhotoRepository.sanitizeApiKey("YOUR_PIXABAY_API_KEY", "YOUR_PIXABAY_API_KEY"))
        assertEquals("", StockPhotoRepository.sanitizeApiKey("   ", "YOUR_PIXABAY_API_KEY"))
        assertEquals("abc123", StockPhotoRepository.sanitizeApiKey(" abc123 ", "YOUR_PIXABAY_API_KEY"))

        val best = StockPhotoRepository.pickBestLandscapeCandidate(
            listOf(
                PhotoCandidate("https://example.com/portrait.jpg", 600, 1200, "a"),
                PhotoCandidate("https://example.com/landscape43.jpg", 800, 600, "b"),
                PhotoCandidate("https://example.com/panorama.jpg", 2400, 600, "c")
            )
        )
        assertEquals("https://example.com/landscape43.jpg", best?.url)

        // Verify Apotheke (city_places) picks pharmacy storefront over blister-pack pills
        val apothekePlan = StockPhotoQueryBuilder.build(
            WordEntity(german = "Apotheke", article = "die", translationUk = "аптека", translationEn = "pharmacy", categoryId = "city", subtopicId = "city_places")
        )
        assertEquals("pharmacy shop", apothekePlan.steps.first().query)
        val apothekePick = StockPhotoRepository.pickBestLandscapeCandidate(
            listOf(
                PhotoCandidate("https://example.com/pills.jpg", 800, 600, "a", listOf("tablets", "pills", "medicine", "pharmacy")),
                PhotoCandidate("https://example.com/pharmacy_store.jpg", 800, 600, "b", listOf("pharmacy", "shop", "store", "interior"))
            ),
            apothekePlan.scoringProfile
        )
        assertEquals("https://example.com/pharmacy_store.jpg", apothekePick?.url)

        // Verify Schule (city_places) picks school building/classroom over portrait of kids
        val schulePlan = StockPhotoQueryBuilder.build(
            WordEntity(german = "Schule", article = "die", translationUk = "школа", translationEn = "school", categoryId = "city", subtopicId = "city_places")
        )
        assertEquals("school building", schulePlan.steps.first().query)
        val schulePick = StockPhotoRepository.pickBestLandscapeCandidate(
            listOf(
                PhotoCandidate("https://example.com/kids.jpg", 800, 600, "a", listOf("children", "girls", "smiling", "school")),
                PhotoCandidate("https://example.com/school_building.jpg", 800, 600, "b", listOf("school", "building", "classroom", "architecture"))
            ),
            schulePlan.scoringProfile
        )
        assertEquals("https://example.com/school_building.jpg", schulePick?.url)

        // Verify Büro (city_places) picks office interior over laptop close-up
        val bueroPlan = StockPhotoQueryBuilder.build(
            WordEntity(german = "Büro", article = "das", translationUk = "офіс", translationEn = "office", categoryId = "city", subtopicId = "city_places")
        )
        assertEquals("office room", bueroPlan.steps.first().query)
        val bueroPick = StockPhotoRepository.pickBestLandscapeCandidate(
            listOf(
                PhotoCandidate("https://example.com/laptop.jpg", 800, 600, "a", listOf("laptop", "notebook", "pen", "office")),
                PhotoCandidate("https://example.com/office_room.jpg", 800, 600, "b", listOf("office", "interior", "room", "workspace"))
            ),
            bueroPlan.scoringProfile
        )
        assertEquals("https://example.com/office_room.jpg", bueroPick?.url)
    }

    @Test
    fun genderedProfessionPairsProduceDistinctQueriesAndOppositeTagScoring() {
        val lehrer = WordEntity(
            german = "Lehrer",
            article = "der",
            translationUk = "вчитель",
            translationEn = "teacher (m)",
            categoryId = "city",
            subtopicId = "city_jobs"
        )
        val lehrerin = WordEntity(
            german = "Lehrerin",
            article = "die",
            translationUk = "вчителька",
            translationEn = "teacher (f)",
            categoryId = "city",
            subtopicId = "city_jobs"
        )

        val lehrerPlan = StockPhotoQueryBuilder.build(lehrer)
        val lehrerinPlan = StockPhotoQueryBuilder.build(lehrerin)

        assertEquals(PersonGenderHint.MALE, lehrerPlan.genderHint)
        assertEquals(PersonGenderHint.FEMALE, lehrerinPlan.genderHint)
        assertEquals("teacher man", lehrerPlan.steps.first().query)
        assertEquals("teacher woman", lehrerinPlan.steps.first().query)

        val teacherCandidates = listOf(
            PhotoCandidate("https://example.com/male_teacher.jpg", 800, 600, "a", listOf("teacher", "man", "male", "classroom")),
            PhotoCandidate("https://example.com/female_teacher.jpg", 800, 600, "b", listOf("teacher", "woman", "female", "classroom"))
        )
        assertEquals(
            "https://example.com/male_teacher.jpg",
            StockPhotoRepository.pickBestLandscapeCandidate(teacherCandidates, lehrerPlan.scoringProfile)?.url
        )
        assertEquals(
            "https://example.com/female_teacher.jpg",
            StockPhotoRepository.pickBestLandscapeCandidate(teacherCandidates, lehrerinPlan.scoringProfile)?.url
        )

        val fireman = WordEntity(
            german = "Feuerwehrmann",
            article = "der",
            translationUk = "пожежник",
            translationEn = "firefighter (m)",
            categoryId = "city",
            subtopicId = "city_jobs"
        )
        val firewoman = WordEntity(
            german = "Feuerwehrfrau",
            article = "die",
            translationUk = "пожежниця",
            translationEn = "firefighter (f)",
            categoryId = "city",
            subtopicId = "city_jobs"
        )
        val firemanPlan = StockPhotoQueryBuilder.build(fireman)
        val firewomanPlan = StockPhotoQueryBuilder.build(firewoman)

        assertEquals("firefighter man", firemanPlan.steps.first().query)
        assertEquals("firefighter woman", firewomanPlan.steps.first().query)
        assertTrue(firewomanPlan.steps.any { it.query == "female firefighter" && it.lang == "en" })
        assertTrue(firewomanPlan.steps.any { it.query == "feuerwehrfrau" && it.lang == "de" })

        val firefighterCandidates = listOf(
            PhotoCandidate("https://example.com/fire_m.jpg", 800, 600, "a", listOf("firefighter", "man", "male", "helmet")),
            PhotoCandidate("https://example.com/fire_f.jpg", 800, 600, "b", listOf("firefighter", "woman", "female", "helmet"))
        )
        assertEquals(
            "https://example.com/fire_m.jpg",
            StockPhotoRepository.pickBestLandscapeCandidate(firefighterCandidates, firemanPlan.scoringProfile)?.url
        )
        assertEquals(
            "https://example.com/fire_f.jpg",
            StockPhotoRepository.pickBestLandscapeCandidate(firefighterCandidates, firewomanPlan.scoringProfile)?.url
        )

        // Male nurse vs female nurse, and hairdresser in city_jobs
        val maleNurse = StockPhotoQueryBuilder.build(
            WordEntity(german = "Krankenpfleger", article = "der", translationUk = "медбрат", translationEn = "male nurse", categoryId = "city", subtopicId = "city_jobs")
        )
        val femaleNurse = StockPhotoQueryBuilder.build(
            WordEntity(german = "Krankenschwester", article = "die", translationUk = "медсестра", translationEn = "nurse", categoryId = "city", subtopicId = "city_jobs")
        )
        assertEquals(PersonGenderHint.MALE, maleNurse.genderHint)
        assertEquals(PersonGenderHint.FEMALE, femaleNurse.genderHint)

        val friseur = StockPhotoQueryBuilder.build(
            WordEntity(german = "Friseur", article = "der", translationUk = "перукар", translationEn = "hairdresser (m)", categoryId = "city", subtopicId = "city_jobs")
        )
        assertEquals("hairdresser man", friseur.steps.first().query)
    }

    @Test
    fun subtopicDeduplicationPreventsDuplicateUrlsAcrossCards() {
        val photo1 = CachedStockPhoto("https://example.com/photo1.jpg", StockPhotoProvider.PIXABAY, "u1")
        val photo2 = CachedStockPhoto("https://example.com/photo2.jpg", StockPhotoProvider.PIXABAY, "u2")
        val photo3 = CachedStockPhoto("https://example.com/photo3.jpg", StockPhotoProvider.PIXABAY, "u3")

        val picked = StockPhotoRepository.selectDistinctCandidate(
            ranked = listOf(photo1, photo2, photo3),
            usedUrlsInSubtopic = setOf("https://example.com/photo1.jpg")
        )
        assertEquals(photo2, picked)

        val cycled = StockPhotoRepository.selectNextCycleCandidate(
            candidates = listOf(photo1, photo2, photo3),
            currentUrl = photo1.imageUrl,
            usedUrlsByOtherWords = setOf(photo2.imageUrl)
        )
        assertEquals(photo3, cycled)
    }

    @Test
    fun stage6SpellingAutoSpaceAndOneLetterHintWorkCleanly() {
        // Auto-space insertion after word boundaries
        assertEquals(
            "Rote ",
            LearningViewModel.autoAdvanceSeparators("Rote", "Rote Bete")
        )
        assertEquals(
            "Tag der ",
            LearningViewModel.autoAdvanceSeparators("Tag der", "Tag der Deutschen Einheit")
        )

        // Backspace skips trailing auto-inserted space and deletes preceding letter
        assertEquals(
            "Rot",
            LearningViewModel.backspaceOverSeparators("Rote ")
        )
        assertEquals(
            "Rote",
            LearningViewModel.backspaceOverSeparators("Rote B")
        )

        // Interactive 1-letter hint progression & wrong-suffix repair
        assertEquals("T", LearningViewModel.computeNextHintInput("", "Tomate"))
        assertEquals("To", LearningViewModel.computeNextHintInput("T", "Tomate"))
        assertEquals("Tom", LearningViewModel.computeNextHintInput("Ton", "Tomate"))
        assertEquals("Rote B", LearningViewModel.computeNextHintInput("Rote", "Rote Bete"))
        assertEquals("Rote B", LearningViewModel.computeNextHintInput("Rote ", "Rote Bete"))
        assertEquals("Rote ", LearningViewModel.computeNextHintInput("Rot", "Rote Bete"))
    }

    @Test
    fun spacedRepetition16StepCycleAndDailyLimitWorkCorrectly() {
        val t0 = 1_700_000_000_000L

        // Initial enrollment schedules first review in 6 hours (same day)
        assertEquals(t0 + TimeUnit.HOURS.toMillis(6L), SpacedRepetition.initialEnrollmentTimestamp(t0))

        // Step 0 -> 1 schedules next review in 1 day (24h)
        val r1 = SpacedRepetition.calculate16StepReview(currentStep = 0, isCorrect = true, now = t0)
        assertEquals(1, r1.newStep)
        assertEquals(24L, r1.intervalHours)
        assertFalse(r1.isMastered)

        // Step 1 -> 2 schedules next review in 2 days (48h)
        val r2 = SpacedRepetition.calculate16StepReview(currentStep = 1, isCorrect = true, now = t0)
        assertEquals(2, r2.newStep)
        assertEquals(48L, r2.intervalHours)

        // Complete all 16 repetitions -> reaches mastered state and box 5
        var step = 0
        var lastResult = r1
        repeat(16) {
            lastResult = SpacedRepetition.calculate16StepReview(currentStep = step, isCorrect = true, now = t0)
            step = lastResult.newStep
        }
        assertEquals(16, step)
        assertTrue(lastResult.isMastered)
        assertEquals(5, lastResult.newBox)

        // Mistake gently steps back by 2 (never wipes progress to 0 from mid-progress)
        val mistake = SpacedRepetition.calculate16StepReview(currentStep = 6, isCorrect = false, now = t0)
        assertEquals(4, mistake.newStep)
        assertEquals(12L, mistake.intervalHours)
        assertFalse(mistake.isMastered)

        // Daily anti-overload quota calculation
        val dashboard = ReviewDashboardState(
            dueCount = 135,
            enrolledCount = 200,
            mastered16Count = 12,
            reviewsDoneToday = 15,
            dailyLimit = 60
        )
        assertEquals(45, dashboard.remainingDailyQuota)
        assertEquals(45, dashboard.availableTodayCount)
        assertEquals(90, dashboard.deferredToTomorrowCount)

        // Enabled stage skipping (8-stage flow)
        val customStages = setOf(1, 3, 5, 8)
        assertEquals(3, LearningViewModel.nextEnabledStage(customStages, 1))
        assertEquals(5, LearningViewModel.nextEnabledStage(customStages, 3))
        assertEquals(8, LearningViewModel.nextEnabledStage(customStages, 5))
        assertNull(LearningViewModel.nextEnabledStage(customStages, 8))
    }

    @Test
    fun stage6SmartPrefixRetentionAndArticleStrippingWorkCorrectly() {
        // Keeps correct prefix when mistake happens mid-word
        assertEquals("Apfe", LearningViewModel.keepCorrectPrefix("Apfex", "Apfel"))

        // Clears entire input if the very first character is wrong
        assertEquals("", LearningViewModel.keepCorrectPrefix("Xpfel", "Apfel"))

        // Case-insensitive matching preserves target canonical casing & auto-advances space
        assertEquals("guten Mo", LearningViewModel.keepCorrectPrefix("guten moxgen", "guten Morgen"))
        assertEquals("Rote ", LearningViewModel.keepCorrectPrefix("Rote X", "Rote Bete"))

        // Keyboard input gracefully strips optional leading article matching word's article
        assertEquals("Tomate", LearningViewModel.stripOptionalArticlePrefix("die Tomate", "die"))
        assertEquals("Tomate", LearningViewModel.stripOptionalArticlePrefix("DIE  Tomate", "die"))
        assertEquals("Tomate", LearningViewModel.stripOptionalArticlePrefix("Tomate", "die"))
        assertEquals("Auto", LearningViewModel.stripOptionalArticlePrefix("Auto", ""))
    }

    @Test
    fun spacedRepetitionEnrollmentRequiresAllEightStagesCompleted() {
        assertTrue(VocabularyRepository.isAllStagesCompleted(setOf(1, 2, 3, 4, 5, 6, 7, 8)))
        assertFalse(VocabularyRepository.isAllStagesCompleted(setOf(1, 2, 3, 4, 5, 6, 7)))
        assertFalse(VocabularyRepository.isAllStagesCompleted(setOf(1, 2, 3, 4, 6, 7, 8)))
        assertFalse(VocabularyRepository.isAllStagesCompleted(setOf(1, 2, 3, 4, 5, 6)))
        assertFalse(VocabularyRepository.isAllStagesCompleted(setOf(8)))
        assertFalse(VocabularyRepository.isAllStagesCompleted(emptySet()))
    }

    @Test
    fun keyboardModeLenientUmlautMatchingAcceptsMissingDiacriticsAndRejectsErrors() {
        // Plain base letters a/o/u for ä/ö/ü and ss/s for ß
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Buro", "Büro"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Buero", "Büro"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Madchen", "Mädchen"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Maedchen", "Mädchen"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Apfel", "Äpfel"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Aepfel", "Äpfel"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Ol", "Öl"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Oel", "Öl"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Strasse", "Straße"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Strase", "Straße"))
        assertTrue(LearningViewModel.matchesWithLenientUmlauts("Kuhlschranke", "Kühlschränke"))

        // Rejects real typos and spurious umlauts where target has plain letters
        assertFalse(LearningViewModel.matchesWithLenientUmlauts("Kätze", "Katze"))
        assertFalse(LearningViewModel.matchesWithLenientUmlauts("Bura", "Büro"))
        assertFalse(LearningViewModel.matchesWithLenientUmlauts("Strasze", "Straße"))

        // Lenient prefix retention in keyboard mode
        assertEquals("Küch", LearningViewModel.keepCorrectPrefix("Kuchxx", "Küche", allowLenientUmlauts = true))
        assertEquals("Mädch", LearningViewModel.keepCorrectPrefix("Maedchxx", "Mädchen", allowLenientUmlauts = true))
        assertEquals("Straß", LearningViewModel.keepCorrectPrefix("Strassx", "Straße", allowLenientUmlauts = true))
    }

    @Test
    fun stage5PluralTrainerFiltersNounsAndBuildsPlausibleOptions() {
        val apple = WordEntity(
            german = "Apfel",
            article = "der",
            plural = "die Äpfel",
            translationUk = "яблуко",
            translationEn = "apple",
            categoryId = "food",
            subtopicId = "food_fruits"
        )
        val milk = WordEntity(
            german = "Milch",
            article = "die",
            plural = "-",
            translationUk = "молоко",
            translationEn = "milk",
            categoryId = "food",
            subtopicId = "food_drinks"
        )
        val schweigen = WordEntity(
            german = "schweigen",
            article = "",
            plural = "schwieg, geschwiegen",
            translationUk = "мовчати",
            translationEn = "to be silent",
            categoryId = "verbs",
            subtopicId = "verbs_communication"
        )
        val adj = WordEntity(
            german = "trocken",
            article = "",
            plural = "",
            translationUk = "сухий",
            translationEn = "dry",
            categoryId = "adjectives",
            subtopicId = "adj_sensory"
        )

        assertTrue(LearningViewModel.isPluralizableNoun(apple))
        assertFalse(LearningViewModel.isPluralizableNoun(milk))
        assertFalse(LearningViewModel.isPluralizableNoun(schweigen))
        assertFalse(LearningViewModel.isPluralizableNoun(adj))

        val options = LearningViewModel.generatePluralOptions(apple)
        assertEquals(4, options.size)
        assertEquals(4, options.distinct().size)
        assertTrue(options.contains("die Äpfel"))
        assertTrue(options.all { it.startsWith("die ") })
    }

    @Test
    fun stage8MissingLettersIndicesLenientUmlautsAndHintsWorkCorrectly() {
        val word = "Kühlschrank"
        val blanks = LearningViewModel.buildMissingLettersIndices(word)
        assertTrue("Should mask at least 2 letters", blanks.size >= 2)
        assertTrue("Should leave visible context letters", blanks.size < word.length)
        assertEquals("Blank indices must be sorted", blanks.sorted(), blanks)
        assertFalse("First letter should remain visible as an anchor", blanks.contains(0))

        val pool = LearningViewModel.buildMissingLettersPool(word, blanks)
        val expectedMissing = buildString { blanks.forEach { append(word[it]) } }
        assertTrue("Pool must be at least as large as missing letters", pool.size >= expectedMissing.length)

        // Lenient umlaut matching on missing-letter sequence and full word
        assertTrue(LearningViewModel.matchesMissingLettersLenient("u", "ü", "Büro"))
        assertTrue(LearningViewModel.matchesMissingLettersLenient("Buro", "ü", "Büro"))
        assertTrue(LearningViewModel.matchesMissingLettersLenient("Strasse", "ß", "Straße"))
        assertFalse(LearningViewModel.matchesMissingLettersLenient("x", "ü", "Büro"))

        // Prefix retention and single-letter hints for missing slots
        assertEquals("ü", LearningViewModel.keepCorrectMissingPrefix("ux", "üa", allowLenientUmlauts = true))
        assertEquals("a", LearningViewModel.computeNextMissingHint("", "ae"))
        assertEquals("ae", LearningViewModel.computeNextMissingHint("a", "ae"))
        assertEquals("ae", LearningViewModel.computeNextMissingHint("ax", "ae"))
    }

    @Test
    fun stockPhotoRelevanceWholeWordPhraseSynergyAndJunkFiltering() {
        // 1. Whole-word matching prevents short-word substring false positives
        assertTrue(StockPhotoRepository.tagMatchesToken("car", "car"))
        assertTrue(StockPhotoRepository.tagMatchesToken("sports cars", "car"))
        assertFalse(StockPhotoRepository.tagMatchesToken("carrot", "car"))
        assertFalse(StockPhotoRepository.tagMatchesToken("scarf", "car"))
        assertFalse(StockPhotoRepository.tagMatchesToken("office", "ice"))
        assertFalse(StockPhotoRepository.tagMatchesToken("teacher", "tea"))
        assertFalse(StockPhotoRepository.tagMatchesToken("bread", "red"))

        // 2. Multi-word phrase synergy picks actual "bell pepper" over a church "bell"
        val paprika = WordEntity(
            german = "Paprika",
            article = "die",
            translationUk = "перець",
            translationEn = "bell pepper",
            categoryId = "food",
            subtopicId = "food_vegetables"
        )
        val paprikaPlan = StockPhotoQueryBuilder.build(paprika)
        val paprikaCandidates = listOf(
            PhotoCandidate("https://example.com/church_bell.jpg", 800, 600, "a", listOf("bell", "church", "metal", "bronze"), likes = 90),
            PhotoCandidate("https://example.com/bell_pepper.jpg", 800, 600, "b", listOf("bell pepper", "pepper", "vegetable", "fresh"), likes = 45)
        )
        assertEquals(
            "https://example.com/bell_pepper.jpg",
            StockPhotoRepository.pickBestLandscapeCandidate(paprikaCandidates, paprikaPlan.scoringProfile)?.url
        )

        // 3. Ambiguous single-word fallback guard stops "ice cream" -> "cream" and "washing machine" -> "machine"
        val iceCreamPlan = StockPhotoQueryBuilder.build(
            WordEntity(german = "Eis", article = "das", translationUk = "морозиво", translationEn = "ice cream", categoryId = "food", subtopicId = "food_sweets")
        )
        assertFalse("Should never fall back to ambiguous 'cream'", iceCreamPlan.steps.any { it.query == "cream" })

        val washerPlan = StockPhotoQueryBuilder.build(
            WordEntity(german = "Waschmaschine", article = "die", translationUk = "пральна машина", translationEn = "washing machine", categoryId = "home", subtopicId = "home_appliances")
        )
        assertFalse("Should never fall back to ambiguous 'machine'", washerPlan.steps.any { it.query == "machine" })
        assertTrue("Native German query must be included", washerPlan.steps.any { it.lang == "de" && it.query == "waschmaschine" })

        // 4. Curated visual query for German verbs like schweigen
        val schweigenPlan = StockPhotoQueryBuilder.build(
            WordEntity(german = "schweigen", article = "", translationUk = "мовчати", translationEn = "to be silent", categoryId = "verbs", subtopicId = "verbs_communication")
        )
        assertEquals("quiet finger lips", schweigenPlan.steps.first().query)

        // 5. Real photograph beats AI-generated / chalkboard / scrabble junk images
        val book = WordEntity(german = "Buch", article = "das", translationUk = "книга", translationEn = "book", categoryId = "education", subtopicId = "education_supplies")
        val bookPlan = StockPhotoQueryBuilder.build(book)
        val bookCandidates = listOf(
            PhotoCandidate("https://example.com/ai_book.jpg", 800, 600, "a", listOf("book", "ai generated", "fantasy", "3d", "render"), likes = 10),
            PhotoCandidate("https://example.com/real_book.jpg", 800, 600, "b", listOf("book", "reading", "library", "education", "desk"), likes = 120)
        )
        assertEquals(
            "https://example.com/real_book.jpg",
            StockPhotoRepository.pickBestLandscapeCandidate(bookCandidates, bookPlan.scoringProfile)?.url
        )
    }
}
