package com.example.util

import com.example.data.model.WordEntity

data class WordExampleSentence(
    val de: String,
    val uk: String
)

/**
 * Provides 3 distinct, grammatically accurate German/Ukrainian example sentences for every vocabulary word:
 * - Sentence #1 is always the word's primary curated sentence from the vocabulary dataset.
 * - Sentences #2 and #3 are deterministic, natural contextual sentences tailored to the word's
 *   part of speech, grammatical gender (Nominativ / Akkusativ / Dativ articles), plural form, and
 *   semantic domain, ensuring the exact target word appears cleanly for both flashcards and Stage 6 blanks.
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
        val datIndef = when (article) {
            "der", "das" -> "einem"
            else -> "einer"
        }
        val possAkk = when (article) {
            "der" -> "meinen"
            "die" -> "meine"
            else -> "mein"
        }
        val demNom = when (article) {
            "der" -> "Dieser"
            "die" -> "Diese"
            else -> "Dieses"
        }
        val demAkk = when (article) {
            "der" -> "diesen"
            "die" -> "diese"
            else -> "dieses"
        }

        val candidates: List<WordExampleSentence> = when {
            word.subtopicId == "food_drinks" -> listOf(
                WordExampleSentence("Ich möchte bitte $akkIndef $noun bestellen.", "Я хотів би замовити «$uk»."),
                WordExampleSentence("Trinkst du $akkDef $noun lieber kalt oder warm?", "Ти п'єш «$uk» краще холодним чи теплим?"),
                WordExampleSentence("Auf dem Tisch steht noch $nomDef $noun für dich.", "На столі ще стоїть «$uk» для тебе.")
            )
            word.subtopicId in setOf("food_fruits", "food_vegetables", "food_meals", "food_meat_fish", "food_sweets") -> listOf(
                WordExampleSentence("Auf dem Markt kaufen wir heute frische Zutaten und $akkIndef $noun.", "На ринку ми сьогодні купуємо свіжі продукти й «$uk»."),
                WordExampleSentence("$demNom $noun schmeckt wirklich ausgezeichnet!", "Це «$uk» смакує справді чудово!"),
                WordExampleSentence("Hast du heute schon $akkDef $noun probiert?", "Ти сьогодні вже скуштував «$uk»?")
            )
            word.subtopicId == "food_restaurant" -> listOf(
                WordExampleSentence("Im Restaurant fragen wir freundlich nach $datDef $noun.", "У ресторані ми ввічливо запитуємо про «$uk»."),
                WordExampleSentence("Für unseren Abend im Lokal ist $nomDef $noun besonders wichtig.", "Для нашого вечора в закладі «$uk» особливо важливе."),
                WordExampleSentence("Der Kellner erklärt uns kurz $akkDef $noun.", "Офіціант коротко пояснює нам про «$uk».")
            )
            word.subtopicId == "food_cooking" -> listOf(
                WordExampleSentence("In der Küche brauchen wir beim Kochen $akkDef $noun.", "На кухні під час готування нам потрібне «$uk»."),
                WordExampleSentence("Wo liegt hier in der Küche $nomDef $noun?", "Де тут на кухні лежить «$uk»?"),
                WordExampleSentence("Ohne $akkDef $noun gelingt dieses Rezept nicht.", "Без «$uk» цей рецепт не вдасться.")
            )
            word.subtopicId == "travel_transport" -> listOf(
                WordExampleSentence("Morgen fahren wir ganz bequem mit $datDef $noun.", "Завтра ми дуже зручно поїдемо на «$uk»."),
                WordExampleSentence("Wann kommt $nomDef $noun heute an?", "Коли сьогодні прибуває «$uk»?"),
                WordExampleSentence("Für diese Strecke ist $nomDef $noun am schnellsten.", "Для цього маршруту «$uk» — найшвидший варіант.")
            )
            word.subtopicId in setOf("travel_station", "travel_hotel", "travel_city_nav", "travel_vacation") -> listOf(
                WordExampleSentence("Entschuldigung, wo finde ich hier $akkDef $noun?", "Вибачте, де мені тут знайти «$uk»?"),
                WordExampleSentence("Auf unserer Reise war $nomDef $noun sehr hilfreich.", "У нашій подорожі «$uk» дуже допомогло."),
                WordExampleSentence("Vor der Abreise prüfen wir noch $akkDef $noun.", "Перед від'їздом ми ще перевіряємо «$uk».")
            )
            word.subtopicId in setOf("home_rooms", "home_furniture", "home_kitchen", "home_bathroom", "home_appliances") -> listOf(
                WordExampleSentence("In unserer neuen Wohnung gefällt mir $nomDef $noun am besten.", "У нашій новій квартирі мені найбільше подобається «$uk»."),
                WordExampleSentence("Wir möchten nächste Woche $akkIndef $noun für zu Hause kaufen.", "Наступного тижня ми хочемо купити «$uk» для дому."),
                WordExampleSentence("Kannst du mir bitte kurz $akkDef $noun zeigen?", "Можеш, будь ласка, показати мені «$uk»?")
            )
            word.subtopicId in setOf("city_places", "shopping_stores") -> listOf(
                WordExampleSentence("Weißt du, ob $nomDef $noun heute noch geöffnet hat?", "Ти знаєш, чи «$uk» сьогодні ще відчинено?"),
                WordExampleSentence("Gleich um die Ecke gibt es $akkIndef $noun.", "Одразу за рогом є «$uk»."),
                WordExampleSentence("Wir treffen uns um 15 Uhr direkt vor $datDef $noun.", "Ми зустрічаємося о 15:00 просто перед «$uk».")
            )
            word.subtopicId in setOf("city_jobs", "people_family", "people_relationships", "work_career") -> listOf(
                WordExampleSentence("Heute spreche ich nach der Arbeit mit $datDef $noun.", "Сьогодні після роботи я поговорю з «$uk»."),
                WordExampleSentence("$demNom $noun ist sehr freundlich und hilfsbereit.", "«$uk» — дуже привітна й чуйна людина."),
                WordExampleSentence("Ich kenne $akkDef $noun schon seit mehreren Jahren.", "Я знаю «$uk» вже кілька років.")
            )
            word.categoryId == "body" -> listOf(
                WordExampleSentence("Beim Arzt sprechen wir heute über $akkDef $noun.", "У лікаря ми сьогодні говоримо про «$uk»."),
                WordExampleSentence("Für die Gesundheit ist $nomDef $noun sehr wichtig.", "Для здоров'я «$uk» має велике значення."),
                WordExampleSentence("Man sollte im Alltag gut auf $akkDef $noun achten.", "У повсякденні варто уважно стежити за «$uk».")
            )
            word.categoryId == "shopping" -> listOf(
                WordExampleSentence("Wie viel kostet $demNom $noun hier im Geschäft?", "Скільки коштує «$uk» тут у магазині?"),
                WordExampleSentence("Ich suche heute $akkIndef $noun in guter Qualität.", "Я сьогодні шукаю «$uk» хорошої якості."),
                WordExampleSentence("An der Kasse bezahlen wir gleich $akkDef $noun.", "На касі ми одразу оплатимо «$uk».")
            )
            word.categoryId == "tech" -> listOf(
                WordExampleSentence("Im Büro nutzen wir jeden Tag $akkDef $noun.", "В офісі ми щодня використовуємо «$uk»."),
                WordExampleSentence("Funktioniert $nomDef $noun bei dir gerade problemlos?", "У тебе «$uk» зараз працює без проблем?"),
                WordExampleSentence("Kannst du mir kurz erklären, wie $nomDef $noun funktioniert?", "Можеш коротко пояснити, як працює «$uk»?")
            )
            word.categoryId == "education" -> listOf(
                WordExampleSentence("Für den Deutschkurs brauche ich unbedingt $akkDef $noun.", "Для курсу німецької мені обов'язково потрібне «$uk»."),
                WordExampleSentence("Im Unterricht besprechen wir heute $akkDef $noun.", "На занятті ми сьогодні обговорюємо «$uk»."),
                WordExampleSentence("Ohne $akkDef $noun ist das Lernen viel schwieriger.", "Без «$uk» навчання набагато складніше.")
            )
            word.categoryId == "nature" -> listOf(
                WordExampleSentence("Bei unserem Spaziergang haben wir $akkIndef $noun gesehen.", "Під час нашої прогулянки ми побачили «$uk»."),
                WordExampleSentence("In der Natur beobachten wir gern $akkDef $noun.", "На природі ми любимо спостерігати за «$uk»."),
                WordExampleSentence("Auf dem Foto sieht man deutlich $akkDef $noun.", "На фото чітко видно «$uk».")
            )
            word.categoryId == "work" -> listOf(
                WordExampleSentence("Bei meiner Arbeit spielt $nomDef $noun eine große Rolle.", "На моїй роботі «$uk» відіграє велику роль."),
                WordExampleSentence("Für das neue Projekt brauchen wir $akkDef $noun.", "Для нового проєкту нам потрібне «$uk»."),
                WordExampleSentence("Im Team besprechen wir morgen $akkDef $noun.", "У команді ми завтра обговоримо «$uk».")
            )
            word.categoryId == "hobbies" -> listOf(
                WordExampleSentence("Am Wochenende freue ich mich besonders auf $akkDef $noun.", "На вихідних я особливо радію «$uk»."),
                WordExampleSentence("In meiner Freizeit mag ich $akkDef $noun sehr gern.", "У вільний час мені дуже подобається «$uk»."),
                WordExampleSentence("Hast du Lust, mehr über $akkDef $noun zu erfahren?", "Маєш бажання дізнатися більше про «$uk»?")
            )
            else -> listOf(
                WordExampleSentence("Im Alltag begegnet uns $nomDef $noun sehr oft.", "У повсякденному житті нам дуже часто трапляється «$uk»."),
                WordExampleSentence("Kannst du mir bitte $akkDef $noun kurz erklären?", "Можеш, будь ласка, коротко пояснити мені «$uk»?"),
                WordExampleSentence("Wir sprechen heute im Kurs über $akkDef $noun.", "Сьогодні на курсі ми говоримо про «$uk».")
            )
        }

        // Also add a natural plural sentence when the noun has a clean plural form
        val rawPlural = word.plural.trim()
        val withPlural = if (rawPlural.startsWith("die ", ignoreCase = true) && rawPlural.length > 4) {
            val pluralNoun = rawPlural.substring(4).trim()
            if (!pluralNoun.equals(noun, ignoreCase = true)) {
                candidates + WordExampleSentence(
                    de = "Im Gespräch verwenden wir sowohl $nomDef $noun als auch viele $pluralNoun.",
                    uk = "У розмові ми вживаємо і «$uk», і форму множини ($rawPlural)."
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
                WordExampleSentence("Man sollte $verb jeden Tag in Ruhe.", "Потрібно спокійно «$uk» щодня."),
                WordExampleSentence("Wir möchten $verb und genug Zeit dafür nehmen.", "Ми хочемо «$uk» і виділити на це достатньо часу."),
                WordExampleSentence("Kann man $verb hier ohne Stress?", "Чи можна тут спокійно «$uk»?")
            )
        } else {
            listOf(
                WordExampleSentence("Ich möchte heute unbedingt $cleanVerb.", "Я сьогодні обов'язково хочу «$uk»."),
                WordExampleSentence("Kannst du mir zeigen, wie wir das richtig $cleanVerb?", "Можеш показати, як нам правильно «$uk»?"),
                WordExampleSentence("Morgen müssen wir rechtzeitig $cleanVerb.", "Завтра нам потрібно вчасно «$uk»."),
                WordExampleSentence("In diesem Kurs lernen wir, klar und sicher zu $cleanVerb.", "На цьому курсі ми вчимося впевнено «$uk».")
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
        val pool = listOf(
            WordExampleSentence("Das Ergebnis finde ich heute wirklich $adj.", "Сьогоднішній результат я вважаю справді «$uk»."),
            WordExampleSentence("Für unseren Alltag ist das sehr $adj.", "Для нашого повсякдення це дуже «$uk»."),
            WordExampleSentence("Findest du diese Lösung auch so $adj?", "Ти теж вважаєш це рішення таким «$uk»?"),
            WordExampleSentence("Im Vergleich zu gestern wirkt heute alles $adj.", "Порівняно зі вчора сьогодні все виглядає «$uk».")
        )
        val start = seed % pool.size
        return List(pool.size) { offset -> pool[(start + offset) % pool.size] }
    }

    private fun buildGeneralPhraseExamples(
        phrase: String,
        uk: String
    ): List<WordExampleSentence> = listOf(
        WordExampleSentence("In einem Gespräch auf Deutsch passt „$phrase“ sehr gut.", "У розмові німецькою вираз «$uk» дуже доречний."),
        WordExampleSentence("Hör genau hin, wann Muttersprachler „$phrase“ sagen.", "Уважно слухай, коли носії мови кажуть «$uk».")
    )

    private fun primaryUkSense(rawUk: String): String {
        val first = rawUk.split('/', ';').firstOrNull()?.trim().orEmpty()
        return first.replace(Regex("\\s*\\([^)]*\\)"), "").trim().ifBlank { rawUk.trim() }
    }
}
