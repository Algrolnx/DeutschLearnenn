package com.example.util

import com.example.data.model.WordEntity

data class WordExampleSentence(
    val de: String,
    val uk: String
)

/**
 * Provides 3 distinct, grammatically accurate German/Ukrainian example sentences for every vocabulary word:
 * - Sentence #1 is always the word's primary curated sentence from the vocabulary dataset.
 * - Sentences #2 and #3 are deterministic, natural contextual sentences whose Ukrainian translations
 *   strictly use gender-invariant / nominative-compatible constructions (never producing broken gender
 *   agreement like «готель допомогло» or wrong case forms) while keeping the exact German target word
 *   cleanly matchable for Stage 6 fill-in-the-blank exercises.
 */
object WordExampleGenerator {

    fun buildExamplesForWord(word: WordEntity): List<WordExampleSentence> {
        val result = ArrayList<WordExampleSentence>(3)
        val seenDe = HashSet<String>()

        fun addIfUnique(de: String, uk: String) {
            val cleanDe = de.trim()
            val cleanUk = uk.trim()
            if (cleanDe.isEmpty() || cleanUk.isEmpty()) return
            val norm = cleanDe.lowercase()
            if (seenDe.add(norm)) {
                result.add(WordExampleSentence(de = cleanDe, uk = cleanUk))
            }
        }

        if (word.exampleDe.isNotBlank()) {
            addIfUnique(word.exampleDe, word.exampleUk.ifBlank { word.translationUk })
        }

        val extras = generateContextualExtras(word)
        for (extra in extras) {
            if (result.size >= 3) break
            addIfUnique(extra.de, extra.uk)
        }

        return result
    }

    private fun generateContextualExtras(word: WordEntity): List<WordExampleSentence> {
        val german = word.german.trim()
        val ukPrimary = primaryUkSense(word.translationUk)
        val art = word.article.lowercase().trim()
        val seed = (word.id * 31 + german.hashCode()) and 0x7fffffff

        if (art in listOf("der", "die", "das")) {
            return buildNounExamples(word, german, ukPrimary, art, seed)
        }

        val isAdjective = word.categoryId == "adjectives" ||
            word.subtopicId.startsWith("adj_")
        if (isAdjective) {
            return buildAdjectiveExamples(german, ukPrimary, seed)
        }

        val isVerb = word.categoryId == "verbs" ||
            word.subtopicId.startsWith("verbs_") ||
            word.translationEn.trim().startsWith("to ", ignoreCase = true) ||
            (art.isBlank() && german.firstOrNull()?.isLowerCase() == true && (german.endsWith("en") || german.endsWith("ern") || german.endsWith("eln")))
        if (isVerb) {
            return buildVerbExamples(german, ukPrimary, seed)
        }

        return buildGeneralPhraseExamples(german, ukPrimary)
    }

    private fun buildNounExamples(
        word: WordEntity,
        noun: String,
        uk: String,
        article: String,
        seed: Int
    ): List<WordExampleSentence> {
        val nomDef = article
        val akkDef = if (article == "der") "den" else article
        val akkIndef = when (article) {
            "der" -> "einen"
            "die" -> "eine"
            else -> "ein"
        }
        val datDef = when (article) {
            "der", "das" -> "dem"
            else -> "der"
        }
        val demNom = when (article) {
            "der" -> "Dieser"
            "die" -> "Diese"
            else -> "Dieses"
        }

        // Every Ukrainian template below keeps the dictionary nominative form ($uk) with
        // present/future 3rd-person verbs or invariant constructions (є / — / :), so gender
        // and case agreement are 100% natural for masculine, feminine, neuter, and plural nouns.
        val candidates: List<WordExampleSentence> = when {
            word.subtopicId == "food_drinks" -> listOf(
                WordExampleSentence("Auf der Karte steht heute auch $nomDef $noun.", "У меню сьогодні також є $uk."),
                WordExampleSentence("Auf dem Tisch steht noch $nomDef $noun für dich.", "На столі для тебе ще стоїть $uk."),
                WordExampleSentence("Im Café gibt es heute frisch: $nomDef $noun.", "У кав'ярні сьогодні є свіже замовлення: $uk.")
            )
            word.subtopicId in setOf("food_fruits", "food_vegetables", "food_meals", "food_meat_fish", "food_sweets") -> listOf(
                WordExampleSentence("Auf dem Markt gibt es heute ganz frisch $akkIndef $noun.", "На ринку сьогодні продається дуже свіжий продукт: $uk."),
                WordExampleSentence("$demNom $noun schmeckt wirklich ausgezeichnet!", "На смак $uk — просто чудово!"),
                WordExampleSentence("Auf meiner Einkaufsliste steht heute auch $nomDef $noun.", "У моєму списку покупок сьогодні є $uk.")
            )
            word.subtopicId == "food_restaurant" -> listOf(
                WordExampleSentence("Im Restaurant spielt $nomDef $noun eine wichtige Rolle.", "У ресторані важливу роль відіграє $uk."),
                WordExampleSentence("Auf dem Tisch im Lokal liegt schon $nomDef $noun.", "У закладі на столі вже є $uk."),
                WordExampleSentence("Der Kellner zeigt uns kurz $akkDef $noun.", "Офіціант показує нам у закладі: $uk.")
            )
            word.subtopicId == "food_cooking" -> listOf(
                WordExampleSentence("In der Küche hilft beim Kochen immer $nomDef $noun.", "На кухні під час готування завжди допомагає $uk."),
                WordExampleSentence("Wo liegt hier in der Küche $nomDef $noun?", "Де тут на кухні лежить $uk?"),
                WordExampleSentence("Für dieses Rezept fehlt uns nur noch $nomDef $noun.", "Для цього рецепта нам бракує лише одного: $uk.")
            )
            word.subtopicId == "travel_transport" -> listOf(
                WordExampleSentence("Wann kommt $nomDef $noun heute an?", "Коли сьогодні прибуває $uk?"),
                WordExampleSentence("Für diese Strecke ist $nomDef $noun am schnellsten.", "Для цього маршруту найшвидший варіант — $uk."),
                WordExampleSentence("Morgen früh fährt $nomDef $noun pünktlich ab.", "Завтра вранці вчасно вирушає $uk.")
            )
            word.subtopicId in setOf("travel_station", "travel_hotel", "travel_city_nav", "travel_vacation") -> listOf(
                WordExampleSentence("Entschuldigung, wo ist hier $nomDef $noun?", "Вибачте, де тут знаходиться $uk?"),
                WordExampleSentence("Auf unserer Reise hilft uns $nomDef $noun sehr.", "У нашій подорожі нам дуже допомагає $uk."),
                WordExampleSentence("Vor der Abreise steht auf unserer Liste: $nomDef $noun.", "Перед від'їздом у нашому списку є $uk.")
            )
            word.subtopicId in setOf("home_rooms", "home_furniture", "home_kitchen", "home_bathroom", "home_appliances") -> listOf(
                WordExampleSentence("In unserer neuen Wohnung gefällt mir $nomDef $noun am besten.", "У нашій новій квартирі мені найбільше подобається $uk."),
                WordExampleSentence("In diesem Zimmer steht jetzt $nomDef $noun.", "У цій кімнаті тепер стоїть $uk."),
                WordExampleSentence("Für zu Hause fehlt uns noch $nomDef $noun.", "Для дому нам іще потрібна покупка: $uk.")
            )
            word.subtopicId in setOf("city_places", "shopping_stores") -> listOf(
                WordExampleSentence("Hat $nomDef $noun heute noch geöffnet?", "Чи працює сьогодні $uk?"),
                WordExampleSentence("Gleich um die Ecke befindet sich $nomDef $noun.", "Одразу за рогом знаходиться $uk."),
                WordExampleSentence("In der Nähe vom Zentrum liegt $nomDef $noun.", "Неподалік від центру розташовується $uk.")
            )
            word.subtopicId in setOf("city_jobs", "people_family", "people_relationships", "work_career") -> listOf(
                WordExampleSentence("Heute kommt nach der Arbeit auch $nomDef $noun.", "Сьогодні після роботи також прийде $uk."),
                WordExampleSentence("$demNom $noun hilft uns im Alltag sehr oft.", "У повсякденні нам часто допомагає $uk."),
                WordExampleSentence("In unserem Team arbeitet auch $nomDef $noun.", "У нашій команді також працює $uk.")
            )
            word.categoryId == "body" -> listOf(
                WordExampleSentence("Für die Gesundheit hat $nomDef $noun eine große Bedeutung.", "Для здоров'я велике значення має $uk."),
                WordExampleSentence("Beim Arzt ist heute das Thema: $nomDef $noun.", "На прийомі в лікаря сьогодні тема — $uk."),
                WordExampleSentence("Im Alltag verdient $nomDef $noun besondere Aufmerksamkeit.", "У повсякденні особливої уваги потребує $uk.")
            )
            word.categoryId == "shopping" -> listOf(
                WordExampleSentence("Wie viel kostet $nomDef $noun hier im Geschäft?", "Скільки коштує $uk тут у магазині?"),
                WordExampleSentence("Im Schaufenster gefällt mir $nomDef $noun besonders gut.", "На вітрині мені найбільше подобається $uk."),
                WordExampleSentence("Im Angebot gibt es heute auch $akkIndef $noun.", "Сьогодні за акцією також є $uk.")
            )
            word.categoryId == "tech" -> listOf(
                WordExampleSentence("Funktioniert $nomDef $noun bei dir gerade problemlos?", "Чи працює в тебе зараз без проблем $uk?"),
                WordExampleSentence("Kannst du mir kurz erklären, wie $nomDef $noun funktioniert?", "Можеш коротко пояснити, як працює $uk?"),
                WordExampleSentence("Im Büro erleichtert $nomDef $noun unsere tägliche Arbeit.", "В офісі щоденну роботу спрощує $uk.")
            )
            word.categoryId == "education" -> listOf(
                WordExampleSentence("Für den Deutschkurs hilft mir $nomDef $noun sehr.", "На курсі німецької мені дуже допомагає $uk."),
                WordExampleSentence("Im Unterricht ist heute unser Thema: $nomDef $noun.", "Сьогодні на занятті наша тема — $uk."),
                WordExampleSentence("Beim Lernen spielt $nomDef $noun eine wichtige Rolle.", "У навчанні важливу роль відіграє $uk.")
            )
            word.categoryId == "nature" -> listOf(
                WordExampleSentence("In der Natur fasziniert mich $nomDef $noun besonders.", "На природі мене особливо вражає $uk."),
                WordExampleSentence("Auf diesem Foto sieht man deutlich: $nomDef $noun.", "На цьому фото чітко видно: $uk."),
                WordExampleSentence("Bei unserem Spaziergang begegnet uns oft $nomDef $noun.", "Під час прогулянки нам часто трапляється $uk.")
            )
            word.categoryId == "work" -> listOf(
                WordExampleSentence("Bei meiner Arbeit spielt $nomDef $noun eine große Rolle.", "На моїй роботі велику роль відіграє $uk."),
                WordExampleSentence("Für das neue Projekt hilft uns $nomDef $noun sehr.", "Для нового проєкту нам дуже допомагає $uk."),
                WordExampleSentence("Im Meeting steht heute auf dem Plan: $nomDef $noun.", "На зустрічі сьогодні в плані — $uk.")
            )
            word.categoryId == "hobbies" -> listOf(
                WordExampleSentence("In meiner Freizeit gefällt mir $nomDef $noun sehr gut.", "У вільний час мені дуже подобається $uk."),
                WordExampleSentence("Am Wochenende steht bei mir auf dem Plan: $nomDef $noun.", "На вихідних у моїх планах — $uk."),
                WordExampleSentence("Für gute Laune sorgt bei mir immer $nomDef $noun.", "Гарний настрій мені завжди дарує $uk.")
            )
            else -> listOf(
                WordExampleSentence("Im Alltag begegnet uns $nomDef $noun sehr oft.", "У повсякденному житті нам дуже часто трапляється $uk."),
                WordExampleSentence("In unserem Gespräch geht es heute um $akkDef $noun.", "У нашій розмові сьогодні тема — $uk."),
                WordExampleSentence("Im Deutschkurs spielt $nomDef $noun eine wichtige Rolle.", "На курсі німецької важливу роль відіграє $uk.")
            )
        }

        // Also add a natural plural sentence when the noun has a clean plural form
        val rawPlural = word.plural.trim()
        val withPlural = if (rawPlural.startsWith("die ", ignoreCase = true) && rawPlural.length > 4) {
            val pluralNoun = rawPlural.substring(4).trim()
            if (!pluralNoun.equals(noun, ignoreCase = true)) {
                candidates + WordExampleSentence(
                    de = "Im Gespräch kommen sowohl $nomDef $noun als auch viele $pluralNoun vor.",
                    uk = "У розмові трапляється і $uk, і множина ($rawPlural)."
                )
            } else {
                candidates
            }
        } else {
            candidates
        }

        // Rotate selection deterministically per word so adjacent cards don't share identical templates
        val start = seed % candidates.size
        return List(candidates.size) { offset -> candidates[(start + offset) % candidates.size] } +
            if (withPlural.size > candidates.size) listOf(withPlural.last()) else emptyList()
    }

    private fun buildVerbExamples(
        verb: String,
        uk: String,
        seed: Int
    ): List<WordExampleSentence> {
        val cleanVerb = verb.removePrefix("sich ").trim()
        val isReflexive = verb.startsWith("sich ", ignoreCase = true)
        val pool = if (isReflexive) {
            listOf(
                WordExampleSentence("Man sollte $verb jeden Tag ein wenig Zeit nehmen.", "Щодня варто спокійно $uk."),
                WordExampleSentence("Wir möchten heute in Ruhe $verb.", "Сьогодні ми хочемо спокійно $uk."),
                WordExampleSentence("Hier kann man ohne Stress $verb.", "Тут можна без стресу $uk.")
            )
        } else {
            listOf(
                WordExampleSentence("Ich möchte heute unbedingt $cleanVerb.", "Сьогодні я обов'язково хочу $uk."),
                WordExampleSentence("Kannst du mir zeigen, wie wir das richtig $cleanVerb?", "Можеш показати, як правильно $uk?"),
                WordExampleSentence("Morgen müssen wir rechtzeitig $cleanVerb.", "Завтра нам потрібно вчасно $uk."),
                WordExampleSentence("In diesem Kurs lernen wir, sicher zu $cleanVerb.", "На цьому курсі ми вчимося впевнено $uk.")
            )
        }
        val start = seed % pool.size
        return List(pool.size) { offset -> pool[(start + offset) % pool.size] }
    }

    private fun buildAdjectiveExamples(
        adj: String,
        uk: String,
        seed: Int
    ): List<WordExampleSentence> {
        // Pair with masculine nominative Ukrainian nouns (варіант / план / приклад / день)
        // so dictionary masculine adjectives (швидкий, важливий, новий, холодний) read 100% naturally.
        val pool = listOf(
            WordExampleSentence("Dieser Vorschlag ist heute wirklich $adj.", "Цей варіант сьогодні справді $uk."),
            WordExampleSentence("Der neue Plan ist für uns sehr $adj.", "Новий план для нас дуже $uk."),
            WordExampleSentence("Ist dieser Weg auch so $adj?", "Чи цей шлях теж такий $uk?"),
            WordExampleSentence("Im Vergleich zu gestern ist der heutige Tag sehr $adj.", "Порівняно зі вчора сьогоднішній день дуже $uk.")
        )
        val start = seed % pool.size
        return List(pool.size) { offset -> pool[(start + offset) % pool.size] }
    }

    private fun buildGeneralPhraseExamples(
        phrase: String,
        uk: String
    ): List<WordExampleSentence> = listOf(
        WordExampleSentence("In einem Gespräch auf Deutsch passt „$phrase“ sehr gut.", "У розмові німецькою чудово пасує вираз «$uk»."),
        WordExampleSentence("Hör genau hin, wann Muttersprachler „$phrase“ sagen.", "Уважно слухай, коли носії мови кажуть «$uk».")
    )

    private fun primaryUkSense(rawUk: String): String {
        val first = rawUk.split('/', ';').firstOrNull()?.trim().orEmpty()
        return first.replace(Regex("\\s*\\([^)]*\\)"), "").trim().ifBlank { rawUk.trim() }
    }
}
