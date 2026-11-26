package com.example.util

import com.example.data.model.WordEntity

data class WordGrammarHint(
    val badge: String,
    val ruleTitle: String,
    val explanation: String,
    val topicId: String
)

data class GrammarAudioExample(
    val german: String,
    val highlight: String,
    val translationUk: String
)

data class GrammarRuleCardData(
    val badge: String,
    val title: String,
    val summary: String,
    val markers: List<String> = emptyList(),
    val examples: List<GrammarAudioExample> = emptyList(),
    val accentKind: GrammarAccentKind = GrammarAccentKind.PRIMARY
)

enum class GrammarAccentKind {
    PRIMARY,
    DER,
    DIE,
    DAS,
    AMBER,
    SUCCESS
}

data class GrammarTopicSection(
    val id: String,
    val shortTitle: String,
    val titleUk: String,
    val titleDe: String,
    val subtitleUk: String,
    val levelBadge: String,
    val accentKind: GrammarAccentKind,
    val introUk: String,
    val rules: List<GrammarRuleCardData>
)

object GrammarRuleExplainer {

    private fun countUmlauts(text: String): Int =
        text.count { it in setOf('ä', 'ö', 'ü', 'Ä', 'Ö', 'Ü') }

    private fun stripUmlauts(text: String): String = buildString(text.length) {
        for (ch in text.lowercase()) {
            when (ch) {
                'ä' -> append('a')
                'ö' -> append('o')
                'ü' -> append('u')
                else -> append(ch)
            }
        }
    }

    /**
     * Explains WHY a German noun forms its specific plural ending (and umlaut if present)
     * in concise, learner-friendly Ukrainian.
     */
    fun explainPlural(word: WordEntity): WordGrammarHint? {
        val article = word.article.lowercase().trim()
        if (article !in listOf("der", "die", "das")) return null

        val rawPlural = word.plural.trim()
        if (!rawPlural.startsWith("die ", ignoreCase = true) || rawPlural.length <= 4) return null

        val singular = word.german.trim()
        val pluralNoun = rawPlural.substring(4).trim()
        if (singular.isEmpty() || pluralNoun.isEmpty()) return null

        val singLow = singular.lowercase()
        val singPlain = stripUmlauts(singular)
        val plurPlain = stripUmlauts(pluralNoun)
        val gainedUmlaut = countUmlauts(pluralNoun) > countUmlauts(singular)
        val umlautNote = if (gainedUmlaut) " + умлаут (a→ä / o→ö / u→ü)" else ""

        // 1. Feminine -in -> -innen
        if (singLow.endsWith("in") && plurPlain == "${singPlain}nen") {
            return WordGrammarHint(
                badge = "-nen",
                ruleTitle = "Іменники на -in завжди отримують -nen",
                explanation = "Назви професій та осіб жіночого роду на -in у множині подвоюють n і додають -en: $singular → $rawPlural.",
                topicId = "plural"
            )
        }

        // 2. Zero ending (unchanged or umlaut only)
        if (plurPlain == singPlain) {
            val suffixMatch = listOf("chen", "lein", "el", "er", "en").firstOrNull { singLow.endsWith(it) }
            val reason = when {
                suffixMatch != null && gainedUmlaut ->
                    "Іменники на -$suffixMatch не додають закінчення у множині, але частина слів чоловічого роду отримує умлаут: $singular → $rawPlural."
                suffixMatch != null ->
                    "Іменники чоловічого й середнього роду на -$suffixMatch у множині зберігають основу без закінчення — змінюється лише артикль на die."
                gainedUmlaut ->
                    "Форма слова не додає закінчення, а множина позначається артиклем die та умлаутом у корені: $singular → $rawPlural."
                else ->
                    "Це слово не змінює форму в множині — множину показує артикль die."
            }
            return WordGrammarHint(
                badge = if (gainedUmlaut) "¨ (умлаут)" else "— (без зак.)",
                ruleTitle = if (gainedUmlaut) "Нульове закінчення + умлаут" else "Нульове закінчення (—)",
                explanation = reason,
                topicId = "plural"
            )
        }

        // 3. -s ending
        if (plurPlain == "${singPlain}s") {
            val endsWithVowel = singLow.lastOrNull() in setOf('a', 'i', 'o', 'u', 'y')
            val reason = if (endsWithVowel) {
                "Слова, що закінчуються на голосний (-a, -i, -o, -u, -y), майже завжди утворюють множину додаванням -s без умлаута."
            } else {
                "Запозичені слова, абревіатури та інтернаціоналізми зазвичай утворюють множину через закінчення -s."
            }
            return WordGrammarHint(
                badge = "-s",
                ruleTitle = "Множина на -s",
                explanation = reason,
                topicId = "plural"
            )
        }

        // 4. -er ending (+ frequent umlaut)
        if (plurPlain == "${singPlain}er") {
            val genderHint = when (article) {
                "das" -> "середнього роду (das)"
                "der" -> "чоловічого роду (der)"
                else -> "цієї групи"
            }
            return WordGrammarHint(
                badge = if (gainedUmlaut) "¨-er" else "-er",
                ruleTitle = "Закінчення -er$umlautNote",
                explanation = "Короткі односкладові іменники $genderHint часто утворюють множину за допомогою -er; якщо в корені є a, o, u або au, додається умлаут.",
                topicId = "plural"
            )
        }

        // 5. -n ending (mostly nouns ending in -e, -el, -er)
        if (plurPlain == "${singPlain}n") {
            val reason = when {
                singLow.endsWith("e") ->
                    "Майже всі іменники, що закінчуються на -e (особливо жіночого роду die), у множині просто додають -n без умлаута."
                singLow.endsWith("el") || singLow.endsWith("er") ->
                    "Іменники жіночого роду на -el та -er (на відміну від der/das) зазвичай додають у множині -n."
                else ->
                    "Цей іменник належить до моделі на -n (без умлаута): $singular → $rawPlural."
            }
            return WordGrammarHint(
                badge = "-n",
                ruleTitle = "Закінчення -n (без умлаута)",
                explanation = reason,
                topicId = "plural"
            )
        }

        // 6. -en ending
        if (plurPlain == "${singPlain}en") {
            val femSuffix = listOf("ung", "heit", "keit", "schaft", "tion", "sion", "tät", "ik", "ei", "ur", "enz", "anz")
                .firstOrNull { singLow.endsWith(it) }
            val reason = when {
                femSuffix != null ->
                    "Усі іменники жіночого роду із суфіксом -$femSuffix завжди мають закінчення -en і ніколи не беруть умлаут."
                article == "die" ->
                    "Більшість іменників жіночого роду (die), які закінчуються на приголосний, утворюють множину через -en без умлаута."
                article == "der" ->
                    "Частина іменників чоловічого роду (часто слабкої відміни або запозичення) утворює множину додаванням -en."
                else ->
                    "Цей іменник утворює множину через закінчення -en (без умлаута): $singular → $rawPlural."
            }
            return WordGrammarHint(
                badge = "-en",
                ruleTitle = "Закінчення -en",
                explanation = reason,
                topicId = "plural"
            )
        }

        // 7. -e ending (+ optional umlaut)
        if (plurPlain == "${singPlain}e") {
            val reason = when (article) {
                "der" -> "Близько 80% іменників чоловічого роду (der) утворюють множину за допомогою -e; односкладові часто додатково отримують умлаут."
                "das" -> "Багато іменників середнього роду (das) додають закінчення -e (зазвичай без умлаута)."
                "die" -> "Невелика група коротких іменників жіночого роду (die) утворює множину через -e з умлаутом (напр. die Hand → die Hände)."
                else -> "Типова модель множини з закінченням -e$umlautNote."
            }
            return WordGrammarHint(
                badge = if (gainedUmlaut) "¨-e" else "-e",
                ruleTitle = "Закінчення -e$umlautNote",
                explanation = reason,
                topicId = "plural"
            )
        }

        // 8. Latin/Greek/foreign stem swap (-um -> -en, -us -> -en, -a -> -en, -is -> -en)
        if (singLow.endsWith("um") && pluralNoun.lowercase().endsWith("en")) {
            return WordGrammarHint(
                badge = "-um → -en",
                ruleTitle = "Латинське закінчення -um змінюється на -en",
                explanation = "Запозичені іменники середнього роду на -um (як das Museum → die Museen, das Zentrum → die Zentren) замінюють -um на -en.",
                topicId = "plural"
            )
        }
        if (singLow.endsWith("a") && pluralNoun.lowercase().endsWith("en")) {
            return WordGrammarHint(
                badge = "-a → -en",
                ruleTitle = "Заміна закінчення -a на -en",
                explanation = "Частина інтернаціональних слів на -a (напр. die Firma → die Firmen, das Thema → die Themen) у множині змінює -a на -en.",
                topicId = "plural"
            )
        }
        if (singLow.endsWith("nis") && pluralNoun.lowercase().endsWith("nisse")) {
            return WordGrammarHint(
                badge = "-nis → -nisse",
                ruleTitle = "Подвоєння s перед -e",
                explanation = "Іменники на -nis у множині завжди подвоюють s і додають -e: $singular → $rawPlural.",
                topicId = "plural"
            )
        }

        // Fallback for compound / irregular plurals (e.g. -mann -> -leute)
        return WordGrammarHint(
            badge = "Особлива",
            ruleTitle = "Особлива модель множини",
            explanation = if (singLow.endsWith("mann") && pluralNoun.lowercase().endsWith("leute")) {
                "Складні слова на -mann (професії/групи людей) у множині часто замінюють -mann на -leute: $singular → $rawPlural."
            } else {
                "Це слово утворює множину за індивідуальною моделлю ($singular → $rawPlural) — запам'ятайте її разом із артиклем die."
            },
            topicId = "plural"
        )
    }

    /**
     * Explains WHY a German noun has its grammatical gender (der / die / das)
     * whenever a reliable suffix or semantic pattern applies.
     */
    fun explainArticle(word: WordEntity): WordGrammarHint? {
        val article = word.article.lowercase().trim()
        if (article !in listOf("der", "die", "das")) return null
        val german = word.german.trim()
        val low = german.lowercase()

        // Check 100% deterministic derivational suffixes first, then compound noun heads (Komposita),
        // then soft endings (-e, -er, Ge-, -en), and finally root-noun fallback.
        when (article) {
            "die" -> {
                val sureSuffix = listOf(
                    "ung" to "-ung (100% die)",
                    "heit" to "-heit (100% die)",
                    "keit" to "-keit (100% die)",
                    "schaft" to "-schaft (100% die)",
                    "tion" to "-tion (100% die)",
                    "sion" to "-sion (100% die)",
                    "tät" to "-tät (100% die)",
                    "ik" to "-ik (майже завжди die)",
                    "ei" to "-ei (100% die)",
                    "ie" to "-ie (зазвичай die)",
                    "enz" to "-enz (100% die)",
                    "anz" to "-anz (100% die)",
                    "age" to "-age (зазвичай die)",
                    "ur" to "-ur (зазвичай die)"
                ).firstOrNull { low.endsWith(it.first) && low.length > it.first.length + 1 }

                if (sureSuffix != null) {
                    return WordGrammarHint(
                        badge = "die · -${sureSuffix.first}",
                        ruleTitle = "Суфікс ${sureSuffix.second}",
                        explanation = "Іменники із закінченням -${sureSuffix.first} у німецькій мові належать до жіночого роду (die).",
                        topicId = "articles"
                    )
                }
                explainCompoundNounGender(german, article)?.let { return it }
                if (low.endsWith("in") && low.length > 4) {
                    return WordGrammarHint(
                        badge = "die · -in",
                        ruleTitle = "Жіночий суфікс -in",
                        explanation = "Назви професій та осіб жіночої статі на -in завжди мають артикль die (і множину на -innen).",
                        topicId = "articles"
                    )
                }
                if (low.endsWith("e") && low.length > 3) {
                    return WordGrammarHint(
                        badge = "die · -e",
                        ruleTitle = "Закінчення -e (~90% die)",
                        explanation = "Близько 90% двоскладових німецьких іменників, що закінчуються на -e, мають жіночий рід (die).",
                        topicId = "articles"
                    )
                }
                return WordGrammarHint(
                    badge = "die",
                    ruleTitle = "Жіночий рід (Feminin)",
                    explanation = "Кореневе слово без суфікса-маркера — запам'ятовуйте одразу парою «die $german» (рожевий колір для жіночого роду).",
                    topicId = "articles"
                )
            }

            "das" -> {
                val sureSuffix = listOf(
                    "chen" to "-chen (100% das)",
                    "lein" to "-lein (100% das)",
                    "ment" to "-ment (зазвичай das)",
                    "tum" to "-tum (майже завжди das)",
                    "um" to "-um (латинські слова — das)",
                    "nis" to "-nis (часто das)",
                    "ma" to "-ma (грецькі слова — das)"
                ).firstOrNull { low.endsWith(it.first) && low.length > it.first.length + 1 }

                if (sureSuffix != null) {
                    return WordGrammarHint(
                        badge = "das · -${sureSuffix.first}",
                        ruleTitle = "Суфікс ${sureSuffix.second}",
                        explanation = "Іменники із суфіксом -${sureSuffix.first} у німецькій мові мають середній рід (das).",
                        topicId = "articles"
                    )
                }
                explainCompoundNounGender(german, article)?.let { return it }
                if (low.startsWith("ge") && low.length > 4) {
                    return WordGrammarHint(
                        badge = "das · Ge-",
                        ruleTitle = "Префікс Ge- (часто das)",
                        explanation = "Велика частина збірних іменників із префіксом Ge- (das Gebäude, das Gemüse, das Gespräch) належить до середнього роду.",
                        topicId = "articles"
                    )
                }
                if (low.endsWith("en") && low.length > 4) {
                    return WordGrammarHint(
                        badge = "das · -en",
                        ruleTitle = "Субстантивований інфінітив або слово на -en",
                        explanation = "Усі дієслова, перетворені на іменники (das Essen, das Leben, das Schwimmen), завжди мають артикль das.",
                        topicId = "articles"
                    )
                }
                return WordGrammarHint(
                    badge = "das",
                    ruleTitle = "Середній рід (Neutrum)",
                    explanation = "Рід цього кореневого іменника не видно за закінченням — вчіть його разом із артиклем «das $german» (зелений колір).",
                    topicId = "articles"
                )
            }

            "der" -> {
                val sureSuffix = listOf(
                    "ismus" to "-ismus (100% der)",
                    "ling" to "-ling (100% der)",
                    "ig" to "-ig (100% der)",
                    "ich" to "-ich (100% der)",
                    "or" to "-or (майже завжди der)",
                    "ist" to "-ist (професії/особи — der)",
                    "ant" to "-ant (зазвичай der)",
                    "ent" to "-ent (зазвичай der)"
                ).firstOrNull { low.endsWith(it.first) && low.length > it.first.length + 1 }

                if (sureSuffix != null) {
                    return WordGrammarHint(
                        badge = "der · -${sureSuffix.first}",
                        ruleTitle = "Суфікс ${sureSuffix.second}",
                        explanation = "Іменники із закінченням -${sureSuffix.first} у німецькій мові зазвичай мають чоловічий рід (der).",
                        topicId = "articles"
                    )
                }
                explainCompoundNounGender(german, article)?.let { return it }
                if (low.endsWith("er") && low.length > 3) {
                    return WordGrammarHint(
                        badge = "der · -er",
                        ruleTitle = "Суфікс -er (~70% der)",
                        explanation = "Близько 70% іменників із закінченням -er (особливо професії та прилади) мають чоловічий рід (der).",
                        topicId = "articles"
                    )
                }
                return WordGrammarHint(
                    badge = "der",
                    ruleTitle = "Чоловічий рід (Maskulin)",
                    explanation = "Коротке кореневе слово без суфікса-маркера — вчіть його одразу з артиклем «der $german» (синій колір).",
                    topicId = "articles"
                )
            }
        }
        return null
    }

    private data class CompoundHeadInfo(
        val headLower: String,
        val headDisplay: String,
        val article: String,
        val meaningUk: String
    )

    private val inseparablePrefixes = setOf(
        "be", "ge", "er", "ver", "zer", "ent", "emp", "miss", "miß"
    )

    private val compoundNounHeads: List<CompoundHeadInfo> = listOf(
        // Neutral (das) heads
        CompoundHeadInfo("fahrrad", "Fahrrad", "das", "велосипед"),
        CompoundHeadInfo("krankenhaus", "Krankenhaus", "das", "лікарня"),
        CompoundHeadInfo("lebensmittel", "Lebensmittel", "das", "продукт харчування"),
        CompoundHeadInfo("flugzeug", "Flugzeug", "das", "літак"),
        CompoundHeadInfo("fahrzeug", "Fahrzeug", "das", "транспортний засіб"),
        CompoundHeadInfo("werkzeug", "Werkzeug", "das", "інструмент"),
        CompoundHeadInfo("spielzeug", "Spielzeug", "das", "іграшка"),
        CompoundHeadInfo("geschäft", "Geschäft", "das", "магазин / справа"),
        CompoundHeadInfo("gespräch", "Gespräch", "das", "розмова"),
        CompoundHeadInfo("getränk", "Getränk", "das", "напій"),
        CompoundHeadInfo("gericht", "Gericht", "das", "страва / суд"),
        CompoundHeadInfo("gebäude", "Gebäude", "das", "будівля"),
        CompoundHeadInfo("programm", "Programm", "das", "програма"),
        CompoundHeadInfo("telefon", "Telefon", "das", "телефон"),
        CompoundHeadInfo("zentrum", "Zentrum", "das", "центр"),
        CompoundHeadInfo("konzert", "Konzert", "das", "концерт"),
        CompoundHeadInfo("problem", "Problem", "das", "проблема"),
        CompoundHeadInfo("studium", "Studium", "das", "навчання у виші"),
        CompoundHeadInfo("system", "System", "das", "система"),
        CompoundHeadInfo("ticket", "Ticket", "das", "квиток"),
        CompoundHeadInfo("zimmer", "Zimmer", "das", "кімната"),
        CompoundHeadInfo("fenster", "Fenster", "das", "вікно"),
        CompoundHeadInfo("wasser", "Wasser", "das", "вода"),
        CompoundHeadInfo("wetter", "Wetter", "das", "погода"),
        CompoundHeadInfo("messer", "Messer", "das", "ніж"),
        CompoundHeadInfo("papier", "Papier", "das", "папір"),
        CompoundHeadInfo("mittel", "Mittel", "das", "засіб"),
        CompoundHeadInfo("muster", "Muster", "das", "зразок / візерунок"),
        CompoundHeadInfo("fleisch", "Fleisch", "das", "м'ясо"),
        CompoundHeadInfo("gericht", "Gericht", "das", "страва"),
        CompoundHeadInfo("schiff", "Schiff", "das", "корабель"),
        CompoundHeadInfo("schild", "Schild", "das", "вивіска / знак"),
        CompoundHeadInfo("schloss", "Schloss", "das", "замок"),
        CompoundHeadInfo("stück", "Stück", "das", "шматок / частина"),
        CompoundHeadInfo("gerät", "Gerät", "das", "прилад"),
        CompoundHeadInfo("gesetz", "Gesetz", "das", "закон"),
        CompoundHeadInfo("gebiet", "Gebiet", "das", "область / район"),
        CompoundHeadInfo("gewicht", "Gewicht", "das", "вага"),
        CompoundHeadInfo("gesicht", "Gesicht", "das", "обличчя"),
        CompoundHeadInfo("gepäck", "Gepäck", "das", "багаж"),
        CompoundHeadInfo("hotel", "Hotel", "das", "готель"),
        CompoundHeadInfo("konto", "Konto", "das", "рахунок"),
        CompoundHeadInfo("kleid", "Kleid", "das", "сукня"),
        CompoundHeadInfo("kreuz", "Kreuz", "das", "хрест"),
        CompoundHeadInfo("licht", "Licht", "das", "світло"),
        CompoundHeadInfo("recht", "Recht", "das", "право"),
        CompoundHeadInfo("spiel", "Spiel", "das", "гра"),
        CompoundHeadInfo("brett", "Brett", "das", "дошка"),
        CompoundHeadInfo("blatt", "Blatt", "das", "листок"),
        CompoundHeadInfo("leder", "Leder", "das", "шкіра"),
        CompoundHeadInfo("metall", "Metall", "das", "метал"),
        CompoundHeadInfo("eisen", "Eisen", "das", "залізо"),
        CompoundHeadInfo("haus", "Haus", "das", "будинок"),
        CompoundHeadInfo("buch", "Buch", "das", "книга"),
        CompoundHeadInfo("geld", "Geld", "das", "гроші"),
        CompoundHeadInfo("brot", "Brot", "das", "хліб"),
        CompoundHeadInfo("bild", "Bild", "das", "картина / зображення"),
        CompoundHeadInfo("bett", "Bett", "das", "ліжко"),
        CompoundHeadInfo("auto", "Auto", "das", "автомобіль"),
        CompoundHeadInfo("boot", "Boot", "das", "човен"),
        CompoundHeadInfo("büro", "Büro", "das", "офіс"),
        CompoundHeadInfo("glas", "Glas", "das", "скло / склянка"),
        CompoundHeadInfo("holz", "Holz", "das", "дерево (матеріал)"),
        CompoundHeadInfo("salz", "Salz", "das", "сіль"),
        CompoundHeadInfo("bier", "Bier", "das", "пиво"),
        CompoundHeadInfo("hemd", "Hemd", "das", "сорочка"),
        CompoundHeadInfo("heft", "Heft", "das", "зошит"),
        CompoundHeadInfo("tuch", "Tuch", "das", "хустка / рушник"),
        CompoundHeadInfo("kind", "Kind", "das", "дитина"),
        CompoundHeadInfo("tier", "Tier", "das", "тварина"),
        CompoundHeadInfo("wort", "Wort", "das", "слово"),
        CompoundHeadInfo("werk", "Werk", "das", "завод / твір"),
        CompoundHeadInfo("land", "Land", "das", "країна / земля"),
        CompoundHeadInfo("jahr", "Jahr", "das", "рік"),
        CompoundHeadInfo("fest", "Fest", "das", "свято"),
        CompoundHeadInfo("lied", "Lied", "das", "пісня"),
        CompoundHeadInfo("meer", "Meer", "das", "море"),
        CompoundHeadInfo("netz", "Netz", "das", "мережа / сітка"),
        CompoundHeadInfo("ziel", "Ziel", "das", "ціль / мета"),
        CompoundHeadInfo("ende", "Ende", "das", "кінець"),
        CompoundHeadInfo("auge", "Auge", "das", "око"),
        CompoundHeadInfo("herz", "Herz", "das", "серце"),
        CompoundHeadInfo("zeug", "Zeug", "das", "річ / спорядження"),
        CompoundHeadInfo("dach", "Dach", "das", "дах"),
        CompoundHeadInfo("fach", "Fach", "das", "предмет / відділення"),
        CompoundHeadInfo("feld", "Feld", "das", "поле"),
        CompoundHeadInfo("dorf", "Dorf", "das", "село"),
        CompoundHeadInfo("volk", "Volk", "das", "народ"),
        CompoundHeadInfo("band", "Band", "das", "стрічка"),
        CompoundHeadInfo("rad", "Rad", "das", "колесо"),
        CompoundHeadInfo("bad", "Bad", "das", "ванна / басейн"),
        CompoundHeadInfo("amt", "Amt", "das", "відомство / установа"),
        CompoundHeadInfo("tor", "Tor", "das", "ворота"),
        CompoundHeadInfo("eis", "Eis", "das", "морозиво / лід"),

        // Masculine (der) heads
        CompoundHeadInfo("computer", "Computer", "der", "комп'ютер"),
        CompoundHeadInfo("schlüssel", "Schlüssel", "der", "ключ"),
        CompoundHeadInfo("bahnhof", "Bahnhof", "der", "вокзал"),
        CompoundHeadInfo("flughafen", "Flughafen", "der", "аеропорт"),
        CompoundHeadInfo("automat", "Automat", "der", "автомат"),
        CompoundHeadInfo("ausweis", "Ausweis", "der", "посвідчення"),
        CompoundHeadInfo("vertrag", "Vertrag", "der", "договір"),
        CompoundHeadInfo("schmerz", "Schmerz", "der", "біль"),
        CompoundHeadInfo("schrank", "Schrank", "der", "шафа"),
        CompoundHeadInfo("betrieb", "Betrieb", "der", "підприємство"),
        CompoundHeadInfo("bericht", "Bericht", "der", "звіт"),
        CompoundHeadInfo("spiegel", "Spiegel", "der", "дзеркало"),
        CompoundHeadInfo("drucker", "Drucker", "der", "принтер"),
        CompoundHeadInfo("meister", "Meister", "der", "майстер"),
        CompoundHeadInfo("direktor", "Direktor", "der", "директор"),
        CompoundHeadInfo("lehrer", "Lehrer", "der", "вчитель"),
        CompoundHeadInfo("fahrer", "Fahrer", "der", "водій"),
        CompoundHeadInfo("freund", "Freund", "der", "друг"),
        CompoundHeadInfo("kuchen", "Kuchen", "der", "пиріг"),
        CompoundHeadInfo("kaffee", "Kaffee", "der", "кава"),
        CompoundHeadInfo("zucker", "Zucker", "der", "цукор"),
        CompoundHeadInfo("teller", "Teller", "der", "тарілка"),
        CompoundHeadInfo("löffel", "Löffel", "der", "ложка"),
        CompoundHeadInfo("becher", "Becher", "der", "кухоль / стаканчик"),
        CompoundHeadInfo("koffer", "Koffer", "der", "валіза"),
        CompoundHeadInfo("garten", "Garten", "der", "сад"),
        CompoundHeadInfo("strand", "Strand", "der", "пляж"),
        CompoundHeadInfo("schnee", "Schnee", "der", "сніг"),
        CompoundHeadInfo("schirm", "Schirm", "der", "екран / парасолька"),
        CompoundHeadInfo("fehler", "Fehler", "der", "помилка"),
        CompoundHeadInfo("schutz", "Schutz", "der", "захист"),
        CompoundHeadInfo("dienst", "Dienst", "der", "служба / послуга"),
        CompoundHeadInfo("schein", "Schein", "der", "довідка / квиток / купюра"),
        CompoundHeadInfo("urlaub", "Urlaub", "der", "відпустка"),
        CompoundHeadInfo("morgen", "Morgen", "der", "ранок"),
        CompoundHeadInfo("anfang", "Anfang", "der", "початок"),
        CompoundHeadInfo("verein", "Verein", "der", "клуб / об'єднання"),
        CompoundHeadInfo("mensch", "Mensch", "der", "людина"),
        CompoundHeadInfo("mantel", "Mantel", "der", "пальто"),
        CompoundHeadInfo("stiefel", "Stiefel", "der", "чобіт"),
        CompoundHeadInfo("finger", "Finger", "der", "палець"),
        CompoundHeadInfo("rücken", "Rücken", "der", "спина"),
        CompoundHeadInfo("boden", "Boden", "der", "підлога / ґрунт"),
        CompoundHeadInfo("wagen", "Wagen", "der", "візок / вагон / авто"),
        CompoundHeadInfo("hafen", "Hafen", "der", "порт / гавань"),
        CompoundHeadInfo("regen", "Regen", "der", "дощ"),
        CompoundHeadInfo("vogel", "Vogel", "der", "птах"),
        CompoundHeadInfo("apfel", "Apfel", "der", "яблуко"),
        CompoundHeadInfo("salat", "Salat", "der", "салат"),
        CompoundHeadInfo("fisch", "Fisch", "der", "риба"),
        CompoundHeadInfo("tisch", "Tisch", "der", "стіл"),
        CompoundHeadInfo("stuhl", "Stuhl", "der", "стілець"),
        CompoundHeadInfo("platz", "Platz", "der", "місце / площа"),
        CompoundHeadInfo("markt", "Markt", "der", "ринок"),
        CompoundHeadInfo("laden", "Laden", "der", "крамниця"),
        CompoundHeadInfo("sport", "Sport", "der", "спорт"),
        CompoundHeadInfo("schuh", "Schuh", "der", "черевик / взуття"),
        CompoundHeadInfo("stein", "Stein", "der", "камінь"),
        CompoundHeadInfo("strom", "Strom", "der", "струм / потік"),
        CompoundHeadInfo("stoff", "Stoff", "der", "тканина / речовина"),
        CompoundHeadInfo("stift", "Stift", "der", "олівець / ручка"),
        CompoundHeadInfo("stock", "Stock", "der", "поверх / палиця"),
        CompoundHeadInfo("steig", "Steig", "der", "платформа / стежка"),
        CompoundHeadInfo("preis", "Preis", "der", "ціна / приз"),
        CompoundHeadInfo("punkt", "Punkt", "der", "пункт / точка"),
        CompoundHeadInfo("grund", "Grund", "der", "причина / підстава"),
        CompoundHeadInfo("abend", "Abend", "der", "вечір"),
        CompoundHeadInfo("monat", "Monat", "der", "місяць"),
        CompoundHeadInfo("brief", "Brief", "der", "лист"),
        CompoundHeadInfo("fluss", "Fluss", "der", "річка"),
        CompoundHeadInfo("druck", "Druck", "der", "тиск / друк"),
        CompoundHeadInfo("kreis", "Kreis", "der", "коло / округ"),
        CompoundHeadInfo("saft", "Saft", "der", "сік"),
        CompoundHeadInfo("raum", "Raum", "der", "приміщення / простір"),
        CompoundHeadInfo("baum", "Baum", "der", "дерево"),
        CompoundHeadInfo("mann", "Mann", "der", "чоловік"),
        CompoundHeadInfo("arzt", "Arzt", "der", "лікар"),
        CompoundHeadInfo("pass", "Pass", "der", "паспорт"),
        CompoundHeadInfo("satz", "Satz", "der", "речення / набір"),
        CompoundHeadInfo("plan", "Plan", "der", "план"),
        CompoundHeadInfo("wein", "Wein", "der", "вино"),
        CompoundHeadInfo("käse", "Käse", "der", "сир"),
        CompoundHeadInfo("kohl", "Kohl", "der", "капуста"),
        CompoundHeadInfo("topf", "Topf", "der", "каструля / горщик"),
        CompoundHeadInfo("turm", "Turm", "der", "вежа"),
        CompoundHeadInfo("park", "Park", "der", "парк"),
        CompoundHeadInfo("wald", "Wald", "der", "ліс"),
        CompoundHeadInfo("berg", "Berg", "der", "гора"),
        CompoundHeadInfo("wind", "Wind", "der", "вітер"),
        CompoundHeadInfo("kurs", "Kurs", "der", "курс"),
        CompoundHeadInfo("film", "Film", "der", "фільм"),
        CompoundHeadInfo("kopf", "Kopf", "der", "голова"),
        CompoundHeadInfo("zahn", "Zahn", "der", "зуб"),
        CompoundHeadInfo("saal", "Saal", "der", "зал"),
        CompoundHeadInfo("club", "Club", "der", "клуб"),
        CompoundHeadInfo("gast", "Gast", "der", "гість"),
        CompoundHeadInfo("wert", "Wert", "der", "цінність / значення"),
        CompoundHeadInfo("grad", "Grad", "der", "градус / ступінь"),
        CompoundHeadInfo("flug", "Flug", "der", "політ / рейс"),
        CompoundHeadInfo("gang", "Gang", "der", "коридор / хід"),
        CompoundHeadInfo("ring", "Ring", "der", "кільце / обручка"),
        CompoundHeadInfo("fall", "Fall", "der", "випадок"),
        CompoundHeadInfo("teil", "Teil", "der", "частина"),
        CompoundHeadInfo("fuß", "Fuß", "der", "нога / стопа"),
        CompoundHeadInfo("tag", "Tag", "der", "день"),
        CompoundHeadInfo("weg", "Weg", "der", "шлях / дорога"),
        CompoundHeadInfo("hof", "Hof", "der", "двір"),
        CompoundHeadInfo("zug", "Zug", "der", "поїзд"),
        CompoundHeadInfo("bus", "Bus", "der", "автобус"),
        CompoundHeadInfo("tee", "Tee", "der", "чай"),
        CompoundHeadInfo("bau", "Bau", "der", "будівництво / споруда"),

        // Feminine (die) heads
        CompoundHeadInfo("kartoffel", "Kartoffel", "die", "картопля"),
        CompoundHeadInfo("kontrolle", "Kontrolle", "die", "контроль"),
        CompoundHeadInfo("maschine", "Maschine", "die", "машина / апарат"),
        CompoundHeadInfo("station", "Station", "die", "станція"),
        CompoundHeadInfo("sprache", "Sprache", "die", "мова"),
        CompoundHeadInfo("medizin", "Medizin", "die", "медицина / ліки"),
        CompoundHeadInfo("straße", "Straße", "die", "вулиця"),
        CompoundHeadInfo("schule", "Schule", "die", "школа"),
        CompoundHeadInfo("tasche", "Tasche", "die", "сумка / кишеня"),
        CompoundHeadInfo("brille", "Brille", "die", "окуляри"),
        CompoundHeadInfo("nummer", "Nummer", "die", "номер"),
        CompoundHeadInfo("stelle", "Stelle", "die", "місце / посада"),
        CompoundHeadInfo("speise", "Speise", "die", "страва"),
        CompoundHeadInfo("pfanne", "Pfanne", "die", "сковорода"),
        CompoundHeadInfo("brücke", "Brücke", "die", "міст"),
        CompoundHeadInfo("kirche", "Kirche", "die", "церква"),
        CompoundHeadInfo("stunde", "Stunde", "die", "година / урок"),
        CompoundHeadInfo("bürste", "Bürste", "die", "щітка"),
        CompoundHeadInfo("anlage", "Anlage", "die", "система / установка"),
        CompoundHeadInfo("praxis", "Praxis", "die", "практика / кабінет лікаря"),
        CompoundHeadInfo("klinik", "Klinik", "die", "клініка"),
        CompoundHeadInfo("person", "Person", "die", "особа"),
        CompoundHeadInfo("gruppe", "Gruppe", "die", "група"),
        CompoundHeadInfo("flasche", "Flasche", "die", "пляшка"),
        CompoundHeadInfo("pflanze", "Pflanze", "die", "рослина"),
        CompoundHeadInfo("stimme", "Stimme", "die", "голос"),
        CompoundHeadInfo("kasse", "Kasse", "die", "каса"),
        CompoundHeadInfo("karte", "Karte", "die", "картка / квиток / мапа"),
        CompoundHeadInfo("torte", "Torte", "die", "торт"),
        CompoundHeadInfo("blume", "Blume", "die", "квітка"),
        CompoundHeadInfo("jacke", "Jacke", "die", "куртка"),
        CompoundHeadInfo("liste", "Liste", "die", "список"),
        CompoundHeadInfo("milch", "Milch", "die", "молоко"),
        CompoundHeadInfo("suppe", "Suppe", "die", "суп"),
        CompoundHeadInfo("wurst", "Wurst", "die", "ковбаса / сосиска"),
        CompoundHeadInfo("beere", "Beere", "die", "ягода"),
        CompoundHeadInfo("bohne", "Bohne", "die", "квасоля / боб"),
        CompoundHeadInfo("tasse", "Tasse", "die", "чашка"),
        CompoundHeadInfo("gabel", "Gabel", "die", "виделка"),
        CompoundHeadInfo("lampe", "Lampe", "die", "лампа"),
        CompoundHeadInfo("decke", "Decke", "die", "ковдра / стеля"),
        CompoundHeadInfo("sonne", "Sonne", "die", "сонце"),
        CompoundHeadInfo("pause", "Pause", "die", "перерва / пауза"),
        CompoundHeadInfo("woche", "Woche", "die", "тиждень"),
        CompoundHeadInfo("nacht", "Nacht", "die", "ніч"),
        CompoundHeadInfo("musik", "Musik", "die", "музика"),
        CompoundHeadInfo("kunst", "Kunst", "die", "мистецтво"),
        CompoundHeadInfo("seife", "Seife", "die", "мило"),
        CompoundHeadInfo("creme", "Creme", "die", "крем"),
        CompoundHeadInfo("kraft", "Kraft", "die", "сила / енергія"),
        CompoundHeadInfo("halle", "Halle", "die", "зал / павільйон"),
        CompoundHeadInfo("seite", "Seite", "die", "сторона / сторінка"),
        CompoundHeadInfo("fahrt", "Fahrt", "die", "поїздка"),
        CompoundHeadInfo("reise", "Reise", "die", "подорож"),
        CompoundHeadInfo("kette", "Kette", "die", "ланцюжок / мережа"),
        CompoundHeadInfo("wolle", "Wolle", "die", "вовна"),
        CompoundHeadInfo("seide", "Seide", "die", "шовк"),
        CompoundHeadInfo("farbe", "Farbe", "die", "колір / фарба"),
        CompoundHeadInfo("stadt", "Stadt", "die", "місто"),
        CompoundHeadInfo("arbeit", "Arbeit", "die", "робота"),
        CompoundHeadInfo("bahn", "Bahn", "die", "залізниця / трамвай / колія"),
        CompoundHeadInfo("zeit", "Zeit", "die", "час"),
        CompoundHeadInfo("welt", "Welt", "die", "світ"),
        CompoundHeadInfo("hose", "Hose", "die", "штани"),
        CompoundHeadInfo("frau", "Frau", "die", "жінка"),
        CompoundHeadInfo("post", "Post", "die", "пошта"),
        CompoundHeadInfo("bank", "Bank", "die", "банк / лавка"),
        CompoundHeadInfo("nuss", "Nuss", "die", "горіх"),
        CompoundHeadInfo("wand", "Wand", "die", "стіна"),
        CompoundHeadInfo("note", "Note", "die", "оцінка / нота"),
        CompoundHeadInfo("hand", "Hand", "die", "рука / долоня"),
        CompoundHeadInfo("zahl", "Zahl", "die", "число / цифра"),
        CompoundHeadInfo("tür", "Tür", "die", "двері"),
        CompoundHeadInfo("uhr", "Uhr", "die", "годинник / година")
    ).sortedByDescending { it.headLower.length }

    private fun explainCompoundNounGender(german: String, article: String): WordGrammarHint? {
        val trimmed = german.trim()
        val low = trimmed.lowercase()
        val articleGenderUk = when (article) {
            "der" -> "чоловічий рід (der)"
            "die" -> "жіночий рід (die)"
            "das" -> "середній рід (das)"
            else -> return null
        }

        for (candidate in compoundNounHeads) {
            if (candidate.article != article) continue
            if (!low.endsWith(candidate.headLower)) continue
            val prefixLen = low.length - candidate.headLower.length
            val minPrefix = if (candidate.headLower.length <= 3) 3 else 2
            if (prefixLen < minPrefix) continue

            val prefixRaw = trimmed.substring(0, prefixLen)
            val prefixLow = prefixRaw.lowercase()
            if (prefixLow in inseparablePrefixes) continue
            // Ensure prefix contains at least one vowel so we don't split consonant clusters like "Schrank"
            if (prefixLow.none { it in "aeiouäöüy" }) continue

            return WordGrammarHint(
                badge = "$article · Kompositum",
                ruleTitle = "Складне слово: $prefixRaw- + ${candidate.article} ${candidate.headDisplay}",
                explanation = "У німецьких складних іменниках рід ЗАВЖДИ визначає останнє слово: «${candidate.article} ${candidate.headDisplay}» (${candidate.meaningUk}) → тому «$article $trimmed» має $articleGenderUk.",
                topicId = "articles"
            )
        }
        return null
    }
}

object GrammarGuideCatalog {
    val topics: List<GrammarTopicSection> = listOf(
        GrammarTopicSection(
            id = "plural",
            shortTitle = "Множина",
            titleUk = "Множина іменників",
            titleDe = "Pluralbildung",
            subtitleUk = "Чому закінчення -n, -en, -e, -er, -s або нульове + умлаут",
            levelBadge = "A1–B1",
            accentKind = GrammarAccentKind.DIE,
            introUk = "У німецькій немає одного універсального закінчення множини, але є 5 чітких моделей. Знаючи рід слова (der/die/das) і його закінчення, ви визначите правильну множину у 85% випадків. У множині артикль ЗАВЖДИ «die».",
            rules = listOf(
                GrammarRuleCardData(
                    badge = "1 · -n / -en",
                    title = "Жіночий рід (die) та слова на -e",
                    summary = "Найпередбачуваніше правило! Якщо слово закінчується на -e, просто додайте -n. Якщо це іменник жіночого роду на приголосний або із суфіксом -ung, -heit, -keit, -schaft, -tion, -tät — додайте -en. Слова на -in подвоюють n (-innen). Умлаут тут майже ніколи не з'являється.",
                    markers = listOf("-e → -n", "-ung / -heit / -keit → -en", "-in → -innen"),
                    examples = listOf(
                        GrammarAudioExample("die Lampe → die Lampen", "-n", "лампа → лампи"),
                        GrammarAudioExample("die Zeitung → die Zeitungen", "-en", "газета → газети"),
                        GrammarAudioExample("die Lehrerin → die Lehrerinnen", "-nen", "вчителька → вчительки")
                    ),
                    accentKind = GrammarAccentKind.DIE
                ),
                GrammarRuleCardData(
                    badge = "2 · -e (часто + ¨)",
                    title = "Чоловічий (der) і середній (das) рід",
                    summary = "Близько 80% іменників чоловічого роду і більшість односкладових слів середнього роду додають -e. У чоловічому роді часто з'являється умлаут (a→ä, o→ö, u→ü), а в середньому роді умлаут зазвичай відсутній.",
                    markers = listOf("der (односкл.) → ¨-e", "das → -e (без умлаута)"),
                    examples = listOf(
                        GrammarAudioExample("der Tisch → die Tische", "-e", "стіл → столи"),
                        GrammarAudioExample("der Stuhl → die Stühle", "¨-e", "стілець → стільці"),
                        GrammarAudioExample("das Brot → die Brote", "-e", "хліб → хліби")
                    ),
                    accentKind = GrammarAccentKind.DER
                ),
                GrammarRuleCardData(
                    badge = "3 · — (без зак.)",
                    title = "Слова на -el, -er, -en, -chen, -lein",
                    summary = "Іменники чоловічого та середнього роду на -el, -er, -en, а також усі зменшувальні на -chen і -lein НЕ додають жодного закінчення! Змінюється лише артикль на die, а частина слів чоловічого роду отримує умлаут.",
                    markers = listOf("der/das на -el, -er, -en → — / ¨", "-chen / -lein → завжди —"),
                    examples = listOf(
                        GrammarAudioExample("der Lehrer → die Lehrer", "—", "вчитель → вчителі"),
                        GrammarAudioExample("der Apfel → die Äpfel", "¨", "яблуко → яблука"),
                        GrammarAudioExample("das Mädchen → die Mädchen", "—", "дівчина → дівчата")
                    ),
                    accentKind = GrammarAccentKind.DAS
                ),
                GrammarRuleCardData(
                    badge = "4 · -er (майже завжди + ¨)",
                    title = "Короткі слова середнього роду (das)",
                    summary = "Односкладові іменники середнього роду (та кілька слів чоловічого роду) часто беруть закінчення -er. Якщо в корені є a, o, u або au, умлаут обов'язковий!",
                    markers = listOf("das (1 склад) → ¨-er", "a→ä, o→ö, u→ü, au→äu"),
                    examples = listOf(
                        GrammarAudioExample("das Kind → die Kinder", "-er", "дитина → діти"),
                        GrammarAudioExample("das Buch → die Bücher", "¨-er", "книга → книги"),
                        GrammarAudioExample("das Haus → die Häuser", "¨-er", "будинок → будинки")
                    ),
                    accentKind = GrammarAccentKind.AMBER
                ),
                GrammarRuleCardData(
                    badge = "5 · -s",
                    title = "Запозичення, абревіатури та голосні в кінці",
                    summary = "Слова, що закінчуються на -a, -i, -o, -u, -y, а також англіцизми й абревіатури утворюють множину просто через -s без умлаута.",
                    markers = listOf("-a / -i / -o / -u / -y → -s", "Інтернаціоналізми → -s"),
                    examples = listOf(
                        GrammarAudioExample("das Auto → die Autos", "-s", "авто → автомобілі"),
                        GrammarAudioExample("das Handy → die Handys", "-s", "телефон → телефони"),
                        GrammarAudioExample("das Büro → die Büros", "-s", "офіс → офіси")
                    ),
                    accentKind = GrammarAccentKind.SUCCESS
                ),
                GrammarRuleCardData(
                    badge = "Особливі",
                    title = "Латинські -um, суфікс -nis, -mann та слова без множини",
                    summary = "Запозичення на -um змінюють -um на -en (das Museum → die Museen). Слова на -nis подвоюють s (-nisse). Професії на -mann часто переходять у -leute. Речовини й абстрактні поняття (die Milch, das Geld, die Zeit) вживаються лише в однині.",
                    markers = listOf("-um → -en", "-nis → -nisse", "-mann → -leute", "Singular: Milch, Geld"),
                    examples = listOf(
                        GrammarAudioExample("das Museum → die Museen", "-um → -en", "музей → музеї"),
                        GrammarAudioExample("das Ergebnis → die Ergebnisse", "-nisse", "результат → результати"),
                        GrammarAudioExample("der Kaufmann → die Kaufleute", "-leute", "торговець → торговці")
                    ),
                    accentKind = GrammarAccentKind.PRIMARY
                )
            )
        ),
        GrammarTopicSection(
            id = "articles",
            shortTitle = "Артиклі",
            titleUk = "Роди та артиклі",
            titleDe = "der · die · das & ein · eine",
            subtitleUk = "Суфікси-маркери роду, означений та неозначений артикль",
            levelBadge = "A1–B1",
            accentKind = GrammarAccentKind.DER,
            introUk = "Граматичний рід у німецькій часто НЕ збігається з українським (напр. «das Mädchen» — середній рід!). Проте закінчення слова та його значення дають дуже точні підказки.",
            rules = listOf(
                GrammarRuleCardData(
                    badge = "der · Чоловічий",
                    title = "Maskulin: суфікси та групи значень",
                    summary = "Чоловічий рід мають дні тижня, місяці, пори року, погода, сторони світу, марки авто та більшість іменників із суфіксами -er, -ling, -ismus, -or, -ig, -ich.",
                    markers = listOf("-er, -ling, -ismus, -or, -ig", "Дні, місяці, погода, авто"),
                    examples = listOf(
                        GrammarAudioExample("der Montag", "день тижня", "понеділок"),
                        GrammarAudioExample("der Computer", "-er", "комп'ютер"),
                        GrammarAudioExample("der Frühling", "-ling", "весна")
                    ),
                    accentKind = GrammarAccentKind.DER
                ),
                GrammarRuleCardData(
                    badge = "die · Жіночий",
                    title = "Feminin: 100%-надійні суфікси",
                    summary = "Жіночий рід визначити найлегше! Усі слова на -ung, -heit, -keit, -schaft, -tion, -tät, -ei, -in завжди мають артикль die, як і ~90% слів на -e.",
                    markers = listOf("-ung, -heit, -keit, -schaft", "-tion, -tät, -ik, -ei, -in, -e"),
                    examples = listOf(
                        GrammarAudioExample("die Wohnung", "-ung", "квартира"),
                        GrammarAudioExample("die Gesundheit", "-heit", "здоров'я"),
                        GrammarAudioExample("die Universität", "-tät", "університет")
                    ),
                    accentKind = GrammarAccentKind.DIE
                ),
                GrammarRuleCardData(
                    badge = "das · Середній",
                    title = "Neutrum: зменшувальні та інфінітиви",
                    summary = "Будь-яке слово зі зменшувальним -chen або -lein (навіть про людей!) і будь-яке дієслово, вжите як іменник, ЗАВЖДИ середнього роду. Також це суфікси -ment, -um, -tum, префікс Ge- і назви кольорів.",
                    markers = listOf("-chen, -lein (100%)", "-ment, -um, -tum, Ge-", "Дієслово як іменник"),
                    examples = listOf(
                        GrammarAudioExample("das Brötchen", "-chen", "булочка"),
                        GrammarAudioExample("das Essen", "інфінітив", "їжа / прийом їжі"),
                        GrammarAudioExample("das Zentrum", "-um", "центр")
                    ),
                    accentKind = GrammarAccentKind.DAS
                ),
                GrammarRuleCardData(
                    badge = "der vs ein",
                    title = "Коли der/die/das, коли ein/eine, а коли без артикля?",
                    summary = "Неозначений артикль (ein / eine) вживається, коли згадуємо предмет уперше («якийсь»). Означений (der / die / das) — коли предмет уже відомий або єдиний у своєму роді. У множині «ein» не існує, а професії та незлічувані речовини часто стоять без артикля.",
                    markers = listOf("Уперше → ein / eine", "Конкретний → der / die / das", "Професії/рідини → без артикля"),
                    examples = listOf(
                        GrammarAudioExample("Das ist ein Buch. Das Buch ist neu.", "ein → das", "Це книга. Ця книга нова."),
                        GrammarAudioExample("Ich bin Arzt.", "професія → Ø", "Я лікар."),
                        GrammarAudioExample("Ich trinke gern Kaffee.", "речовина → Ø", "Я люблю пити каву.")
                    ),
                    accentKind = GrammarAccentKind.PRIMARY
                )
            )
        ),
        GrammarTopicSection(
            id = "cases",
            shortTitle = "Відмінки",
            titleUk = "Чотири відмінки (Kasus)",
            titleDe = "Nominativ · Akkusativ · Dativ · Genitiv",
            subtitleUk = "Як змінюються артиклі у всіх 4 відмінках німецької мови",
            levelBadge = "A1–B2",
            accentKind = GrammarAccentKind.AMBER,
            introUk = "У німецькій 4 відмінки, і майже всю роботу виконує зміна артикля — сам іменник змінюється рідко (лише у Dativ множини +n та Genitiv чол./сер. роду +s).",
            rules = listOf(
                GrammarRuleCardData(
                    badge = "Nominativ",
                    title = "Називний (Хто? Що?) — підмет",
                    summary = "Базові артиклі зі словника: der / ein (чол.), die / eine (жін.), das / ein (сер.), die (множина). Відповідає за того, хто виконує дію, і завжди стоїть після дієслів sein, werden, bleiben.",
                    markers = listOf("der / ein", "die / eine", "das / ein", "Pl: die"),
                    examples = listOf(
                        GrammarAudioExample("Der Kaffee ist heiß.", "Nominativ", "Кава гаряча."),
                        GrammarAudioExample("Das ist mein Freund.", "після sein", "Це мій друг.")
                    ),
                    accentKind = GrammarAccentKind.DER
                ),
                GrammarRuleCardData(
                    badge = "Akkusativ",
                    title = "Знахідний (Кого? Що?) — прямий додаток",
                    summary = "Гарна новина: змінюється ЛИШЕ чоловічий рід (der → den, ein → einen, mein → meinen)! Жіночий (die), середній (das) і множина (die) лишаються такими ж, як у Nominativ.",
                    markers = listOf("der → den / einen", "die → die", "das → das", "haben, sehen, kaufen, essen"),
                    examples = listOf(
                        GrammarAudioExample("Ich trinke einen Kaffee.", "der → einen", "Я п'ю каву."),
                        GrammarAudioExample("Ich sehe den Hund.", "der → den", "Я бачу собаку.")
                    ),
                    accentKind = GrammarAccentKind.DIE
                ),
                GrammarRuleCardData(
                    badge = "Dativ",
                    title = "Давальний (Кому? Де?) — адресат і місце",
                    summary = "У Dativ змінюються всі артиклі: der → dem, die → der, das → dem, множина → den + закінчення -n в іменника (якщо слово вже не закінчується на -n чи -s). Вживається з дієсловами helfen, danken, gehören, gefallen.",
                    markers = listOf("der/das → dem", "die → der", "Pl: den + -n", "helfen, danken, gefallen"),
                    examples = listOf(
                        GrammarAudioExample("Ich fahre mit dem Bus.", "der → dem", "Я їду автобусом."),
                        GrammarAudioExample("Ich helfe der Frau.", "die → der", "Я допомагаю жінці."),
                        GrammarAudioExample("Ich spiele mit den Kindern.", "Pl: den + -n", "Я граю з дітьми.")
                    ),
                    accentKind = GrammarAccentKind.DAS
                ),
                GrammarRuleCardData(
                    badge = "Genitiv",
                    title = "Родовий (Чий? Кого? Чого?) — належність",
                    summary = "Показує приналежність: der/das → des (+ закінчення -s або -es у самого іменника), die та множина → der. У розмовній мові часто замінюється на «von + Dativ», але на рівні B1–B2 Genitiv обов'язковий.",
                    markers = listOf("der/das → des + -(e)s", "die / Pl → der", "wegen, trotz, während"),
                    examples = listOf(
                        GrammarAudioExample("Das Auto des Lehrers ist neu.", "der → des + -s", "Авто вчителя нове."),
                        GrammarAudioExample("Die Farbe der Blume ist schön.", "die → der", "Колір квітки гарний."),
                        GrammarAudioExample("Wegen des Regens bleiben wir zu Hause.", "wegen + Gen.", "Через дощ ми залишаємося вдома.")
                    ),
                    accentKind = GrammarAccentKind.AMBER
                )
            )
        ),
        GrammarTopicSection(
            id = "verbs",
            shortTitle = "Дієслова",
            titleUk = "Дієслова та часи",
            titleDe = "Präsens · Modalverben · Perfekt",
            subtitleUk = "Відмінювання, зміна голосного, префікси, модальні та минулий час",
            levelBadge = "A1–B1",
            accentKind = GrammarAccentKind.PRIMARY,
            introUk = "Німецьке дієслово — це «двигун» речення. Опанувавши теперішній час (Präsens), модальні дієслова та розмовний минулий час (Perfekt), ви зможете вільно говорити про теперішнє, плани й минуле.",
            rules = listOf(
                GrammarRuleCardData(
                    badge = "Präsens",
                    title = "Особові закінчення (слабкі дієслова)",
                    summary = "Відкиньте -en від інфінітива (lernen → lern-) і додайте формулу «e-st-t-en-t-en». Якщо основа закінчується на -t або -d (arbeiten → arbeit-), для зручності вимови додається -e- (du arbeitest, er arbeitet).",
                    markers = listOf("ich -e", "du -st", "er/sie/es -t", "wir/sie -en", "ihr -t"),
                    examples = listOf(
                        GrammarAudioExample("Ich lerne Deutsch.", "lern + e", "Я вивчаю німецьку."),
                        GrammarAudioExample("Du arbeitest heute.", "arbeit + est", "Ти сьогодні працюєш."),
                        GrammarAudioExample("Wir wohnen in Berlin.", "wohn + en", "Ми живемо в Берліні.")
                    ),
                    accentKind = GrammarAccentKind.PRIMARY
                ),
                GrammarRuleCardData(
                    badge = "Сильні",
                    title = "Зміна кореневого голосного (лише du / er)",
                    summary = "У сильних дієсловах лише в формах du та er/sie/es змінюється голосний кореня: a→ä (fahren → fährt), e→i (sprechen → spricht), e→ie (lesen → liest). Форми ich, wir, ihr, sie залишаються без змін!",
                    markers = listOf("a → ä", "e → i", "e → ie", "Лише du та er/sie/es"),
                    examples = listOf(
                        GrammarAudioExample("Er spricht sehr gut Deutsch.", "e → i", "Він дуже добре говорить німецькою."),
                        GrammarAudioExample("Sie fährt mit dem Zug.", "a → ä", "Вона їде поїздом."),
                        GrammarAudioExample("Du liest ein Buch.", "e → ie", "Ти читаєш книгу.")
                    ),
                    accentKind = GrammarAccentKind.AMBER
                ),
                GrammarRuleCardData(
                    badge = "Префікси",
                    title = "Відокремлювані префікси (auf-, an-, ein-, mit-)",
                    summary = "Наголошені префікси ab-, an-, auf-, aus-, ein-, mit-, vor-, zu-, zurück- відриваються від дієслова в простому реченні та стають у САМИЙ КІНЕЦЬ. Невідокремлювані (be-, ge-, er-, ver-, zer-, ent-, emp-, miss-) завжди лишаються разом.",
                    markers = listOf("aufstehen → stehe ... auf", "einkaufen → kaufe ... ein", "verstehen → verstehe"),
                    examples = listOf(
                        GrammarAudioExample("Ich stehe um 7 Uhr auf.", "auf- в кінці", "Я встаю о 7-й годині."),
                        GrammarAudioExample("Wir kaufen im Supermarkt ein.", "ein- в кінці", "Ми робимо покупки в супермаркеті."),
                        GrammarAudioExample("Der Zug kommt pünktlich an.", "an- в кінці", "Поїзд прибуває вчасно.")
                    ),
                    accentKind = GrammarAccentKind.SUCCESS
                ),
                GrammarRuleCardData(
                    badge = "Модальні",
                    title = "Модальні дієслова (können, müssen, wollen...)",
                    summary = "Модальне дієслово стоїть на 2-му місці (причому форми ich і er/sie/es однакові й без закінчень!), а основне дієслово в інфінітиві йде в САМИЙ КІНЕЦЬ речення.",
                    markers = listOf("können (могти)", "müssen (мусити)", "wollen (хотіти)", "dürfen (мати дозвіл)", "sollen (слід)"),
                    examples = listOf(
                        GrammarAudioExample("Ich kann gut schwimmen.", "kann ... schwimmen", "Я вмію добре плавати."),
                        GrammarAudioExample("Wir müssen morgen früh arbeiten.", "müssen ... arbeiten", "Ми мусимо завтра рано працювати."),
                        GrammarAudioExample("Darf ich hier sitzen?", "darf ... sitzen", "Можна мені тут сісти?")
                    ),
                    accentKind = GrammarAccentKind.DER
                ),
                GrammarRuleCardData(
                    badge = "Perfekt",
                    title = "Розмовний минулий час (haben / sein + Partizip II)",
                    summary = "Основний минулий час у розмовній німецькій! На 2-му місці стоїть допоміжне дієслово haben (для більшості дій) або sein (рух із пункту А в Б і зміна стану: gehen, fahren, fliegen, aufstehen), а в кінці — Partizip II (ge- + основа + -t / -en).",
                    markers = listOf("haben + ge-...-t (слабкі)", "sein → рух / зміна стану", "-ieren → без ge- (studiert)"),
                    examples = listOf(
                        GrammarAudioExample("Ich habe gestern viel gelernt.", "haben + gelernt", "Я вчора багато вчився."),
                        GrammarAudioExample("Er ist nach München gefahren.", "sein + gefahren", "Він поїхав до Мюнхена."),
                        GrammarAudioExample("Wir haben einen Film gesehen.", "haben + gesehen", "Ми подивилися фільм.")
                    ),
                    accentKind = GrammarAccentKind.DIE
                )
            )
        ),
        GrammarTopicSection(
            id = "word_order",
            shortTitle = "Порядок слів",
            titleUk = "Порядок слів у реченні",
            titleDe = "Wortstellung · V2, TeKaMoLo, Nebensatz",
            subtitleUk = "Позиція дієслова, питання, обставини та підрядні речення з weil/dass",
            levelBadge = "A1–B1",
            accentKind = GrammarAccentKind.SUCCESS,
            introUk = "На відміну від української, порядок слів у німецькій чітко структурований. Якщо запам'ятати позицію дієслова у 3 типах речень, ваша німецька одразу звучатиме природно.",
            rules = listOf(
                GrammarRuleCardData(
                    badge = "Правило V2",
                    title = "Розповідне речення: дієслово ЗАВЖДИ на 2-й позиції",
                    summary = "Відмінюване дієслово стоїть на 2-му місці (не 2-ге слово, а 2-й член речення). Якщо на 1-е місце винести час або місце («Heute», «Im Sommer»), підмет одразу переходить на 3-тє місце після дієслова!",
                    markers = listOf("[1] Підмет · [2] Дієслово", "[1] Час/Місце · [2] Дієслово · [3] Підмет"),
                    examples = listOf(
                        GrammarAudioExample("Ich gehe heute ins Kino.", "Ich [1] gehe [2]", "Я йду сьогодні в кіно."),
                        GrammarAudioExample("Heute gehe ich ins Kino.", "Heute [1] gehe [2] ich [3]", "Сьогодні я йду в кіно.")
                    ),
                    accentKind = GrammarAccentKind.PRIMARY
                ),
                GrammarRuleCardData(
                    badge = "Питання",
                    title = "W-Fragen та запитання Так/Ні (Ja/Nein-Fragen)",
                    summary = "У питанні з питальним словом (Wer, Was, Wo, Wann, Warum, Wie) питальне слово стоїть на 1-му місці, а дієслово — на 2-му. У питанні без питального слова (відповідь «Так/Ні») дієслово виходить на 1-е місце!",
                    markers = listOf("W-Wort [1] + Verb [2]", "Ja/Nein → Verb [1] + Subjekt [2]"),
                    examples = listOf(
                        GrammarAudioExample("Wo wohnst du?", "Wo [1] wohnst [2]", "Де ти живеш?"),
                        GrammarAudioExample("Sprichst du Deutsch?", "Sprichst [1] du [2]", "Ти говориш німецькою?")
                    ),
                    accentKind = GrammarAccentKind.DER
                ),
                GrammarRuleCardData(
                    badge = "TeKaMoLo",
                    title = "Порядок обставин: Час → Причина → Спосіб → Місце",
                    summary = "Коли в реченні кілька обставин, ставте їх за формулою Te-Ka-Mo-Lo: Temporal (коли?) → Kausal (чому?) → Modal (як?) → Lokal (де / куди?). Найчастіше трапляється пара «Час перед Місцем»!",
                    markers = listOf("Te: Коли?", "Ka: Чому?", "Mo: Як?", "Lo: Де/Куди?"),
                    examples = listOf(
                        GrammarAudioExample("Ich fahre morgen mit dem Zug nach Wien.", "Te → Mo → Lo", "Я їду завтра поїздом до Відня."),
                        GrammarAudioExample("Wir treffen uns um 18 Uhr im Café.", "Час → Місце", "Ми зустрінемося о 18:00 у кафе.")
                    ),
                    accentKind = GrammarAccentKind.AMBER
                ),
                GrammarRuleCardData(
                    badge = "weil / dass",
                    title = "Підрядні речення: дієслово летить у САМИЙ КІНЕЦЬ",
                    summary = "Після сполучників weil (тому що), dass (що), wenn (якщо/коли), obwohl (хоча), ob (чи) відмінюване дієслово переноситься в самий кінець підрядного речення. Перед цими сполучниками завжди ставиться кома!",
                    markers = listOf("..., weil ... Verb.", "..., dass ... Verb.", "..., wenn ... Verb."),
                    examples = listOf(
                        GrammarAudioExample("Ich bleibe zu Hause, weil ich krank bin.", "bin у кінці", "Я залишаюся вдома, тому що я хворий."),
                        GrammarAudioExample("Ich weiß, dass Deutsch logisch ist.", "ist у кінці", "Я знаю, що німецька логічна.")
                    ),
                    accentKind = GrammarAccentKind.SUCCESS
                )
            )
        ),
        GrammarTopicSection(
            id = "negation",
            shortTitle = "nicht / kein",
            titleUk = "Заперечення: nicht чи kein?",
            titleDe = "Negation · Чітка різниця",
            subtitleUk = "Коли заперечувати іменник через kein, а коли ставити nicht",
            levelBadge = "A1–A2",
            accentKind = GrammarAccentKind.DIE,
            introUk = "В українській ми всюди кажемо «не», а в німецькій заперечення ділиться на два слова: kein (для іменників) і nicht (для всього іншого). Правило дуже просте!",
            rules = listOf(
                GrammarRuleCardData(
                    badge = "kein / keine",
                    title = "Заперечення іменників із ein/eine або без артикля",
                    summary = "Якщо перед іменником у стверджувальному реченні стоїть неозначений артикль (ein/eine) або взагалі немає артикля (множина, речовини, абстрактні поняття), заперечуйте його словом kein! Воно відмінюється так само, як ein (kein / keine / keinen).",
                    markers = listOf("ein Auto → kein Auto", "eine Katze → keine Katze", "Zeit → keine Zeit"),
                    examples = listOf(
                        GrammarAudioExample("Ich habe kein Auto.", "das Auto → kein", "У мене немає машини."),
                        GrammarAudioExample("Ich habe heute keine Zeit.", "die Zeit → keine", "У мене сьогодні немає часу."),
                        GrammarAudioExample("Er trinkt keinen Kaffee.", "Akk. der → keinen", "Він не п'є кави.")
                    ),
                    accentKind = GrammarAccentKind.DIE
                ),
                GrammarRuleCardData(
                    badge = "nicht",
                    title = "Заперечення дієслів, прикметників і визначених іменників",
                    summary = "Слово nicht використовується для заперечення дій (дієслів), ознак (прикметників і прислівників), власних назв, а також іменників, перед якими вже є означений артикль (der/die/das) або присвійний займенник (mein/dein).",
                    markers = listOf("Дієслова → nicht", "Прикметники → nicht", "der/mein + іменник → nicht"),
                    examples = listOf(
                        GrammarAudioExample("Das Wetter ist nicht kalt.", "прикметник", "Погода не холодна."),
                        GrammarAudioExample("Ich verstehe das Wort nicht.", "означений іменник", "Я не розумію цього слова."),
                        GrammarAudioExample("Das ist nicht mein Buch.", "присвійний mein", "Це не моя книга.")
                    ),
                    accentKind = GrammarAccentKind.DER
                ),
                GrammarRuleCardData(
                    badge = "Позиція",
                    title = "Де саме ставити nicht у реченні?",
                    summary = "Якщо заперечується конкретне слово (прикметник, місце з прийменником) — ставте nicht прямо ПЕРЕД ним. Якщо заперечується все речення (дієслово) — nicht іде якомога ближче до кінця, але ПЕРЕД другим дієсловом або відокремлюваним префіксом!",
                    markers = listOf("Перед прикметником/місцем", "В кінці (просте дієслово)", "Перед 2-м дієсловом / префіксом"),
                    examples = listOf(
                        GrammarAudioExample("Ich komme heute nicht.", "у кінці", "Я сьогодні не прийду."),
                        GrammarAudioExample("Ich wohne nicht in Berlin.", "перед in Berlin", "Я живу не в Берліні."),
                        GrammarAudioExample("Ich kann heute nicht kommen.", "перед інфінітивом", "Я не можу сьогодні прийти.")
                    ),
                    accentKind = GrammarAccentKind.PRIMARY
                )
            )
        ),
        GrammarTopicSection(
            id = "prepositions",
            shortTitle = "Прийменники",
            titleUk = "Прийменники та відмінки",
            titleDe = "Präpositionen & Wechselpräpositionen",
            subtitleUk = "Списки прийменників для Akkusativ, Dativ та подвійних (Wo? vs Wohin?)",
            levelBadge = "A2–B1",
            accentKind = GrammarAccentKind.DAS,
            introUk = "У німецькій кожен прийменник «керує» конкретним відмінком. Запам'ятавши три короткі групи, ви перестанете плутатися між «im Zimmer» та «ins Zimmer».",
            rules = listOf(
                GrammarRuleCardData(
                    badge = "Тільки Akkusativ",
                    title = "Завжди вимагають Akkusativ (der → den)",
                    summary = "Запам'ятайте акронім FUDGO (або короткий список): für (для), um (навколо / о годині), durch (крізь), gegen (проти / близько), ohne (без), bis (до). Після них завжди йде Akkusativ!",
                    markers = listOf("für", "um", "durch", "gegen", "ohne", "bis"),
                    examples = listOf(
                        GrammarAudioExample("Das Geschenk ist für meinen Vater.", "für + Akk.", "Подарунок для мого батька."),
                        GrammarAudioExample("Wir gehen durch den Park.", "durch + Akk.", "Ми йдемо крізь парк."),
                        GrammarAudioExample("Ich trinke Tee ohne Zucker.", "ohne + Akk.", "Я п'ю чай без цукру.")
                    ),
                    accentKind = GrammarAccentKind.DIE
                ),
                GrammarRuleCardData(
                    badge = "Тільки Dativ",
                    title = "Завжди вимагають Dativ (der/das → dem, die → der)",
                    summary = "Класична сімка: aus (з/із), bei (у/біля), mit (з), nach (після/до міста), seit (від/протягом), von (від), zu (до), плюс gegenüber (навпроти). Часто зливаються з артиклем: zum = zu dem, zur = zu der, beim = bei dem, vom = von dem.",
                    markers = listOf("aus, bei, mit, nach", "seit, von, zu, gegenüber", "zum / zur / beim / vom"),
                    examples = listOf(
                        GrammarAudioExample("Ich fahre mit dem Fahrrad zur Arbeit.", "mit dem · zur", "Я їду велосипедом на роботу."),
                        GrammarAudioExample("Er kommt aus der Ukraine.", "aus + Dat.", "Він родом з України."),
                        GrammarAudioExample("Ich wohne seit einem Jahr hier.", "seit + Dat.", "Я живу тут уже рік.")
                    ),
                    accentKind = GrammarAccentKind.DAS
                ),
                GrammarRuleCardData(
                    badge = "Wo? / Wohin?",
                    title = "Подвійні прийменники (Wechselpräpositionen)",
                    summary = "9 просторових прийменників: in, an, auf, über, unter, vor, hinter, neben, zwischen. Якщо питання «Куди?» (Wohin? — рух у напрямку) → ставимо Akkusativ. Якщо питання «Де?» (Wo? — стан на місці) → ставимо Dativ!",
                    markers = listOf("Wohin? (рух) → Akkusativ", "Wo? (місце) → Dativ", "ins = in das · im = in dem"),
                    examples = listOf(
                        GrammarAudioExample("Ich lege das Buch auf den Tisch.", "Wohin? → Akk.", "Я кладу книгу на стіл."),
                        GrammarAudioExample("Das Buch liegt auf dem Tisch.", "Wo? → Dat.", "Книга лежить на столі."),
                        GrammarAudioExample("Ich gehe ins Kino. Ich bin im Kino.", "ins (Akk.) / im (Dat.)", "Я йду в кіно. Я в кіно.")
                    ),
                    accentKind = GrammarAccentKind.AMBER
                )
            )
        )
    )
}
