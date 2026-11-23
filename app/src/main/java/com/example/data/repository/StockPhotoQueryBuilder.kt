package com.example.data.repository

import com.example.data.model.WordEntity

data class PixabaySearchStep(
    val query: String,
    val lang: String = "en",
    val isSubtopicFallback: Boolean = false
)

data class PhotoScoringProfile(
    val subjectTokens: Set<String>,
    val preferredTags: Set<String>,
    val avoidTags: Set<String>,
    val allowGraphicStyle: Boolean = false,
    val primaryQueryTokens: Set<String> = emptySet()
)

enum class PersonGenderHint {
    NONE,
    MALE,
    FEMALE
}

data class StockPhotoQueryPlan(
    val steps: List<PixabaySearchStep>,
    val scoringProfile: PhotoScoringProfile,
    val genderHint: PersonGenderHint = PersonGenderHint.NONE
)

/**
 * Builds concise 1-2 word Pixabay search queries and subtopic-aware tag scoring profiles
 * so Pixabay hits match the actual meaning (e.g., pharmacy storefront instead of loose pills,
 * office room instead of a laptop close-up, school building/classroom instead of random portraits,
 * distinct female/male portraits for gendered profession pairs, and expressive scenes for verbs).
 */
object StockPhotoQueryBuilder {

    private val parentheticalRegex = Regex("\\([^)]*\\)")
    private val nonEnWordRegex = Regex("[^a-zA-Z0-9\\s-]")
    private val nonDeWordRegex = Regex("[^\\p{L}0-9\\s-]")
    private val multiSpaceRegex = Regex("\\s+")
    private val germanTokenRegex = Regex("[\\p{L}-]+")

    private val explicitFemaleRegex = Regex(
        "\\b(female|woman|women|girl|waitress|actress|policewoman|saleswoman|postwoman|businesswoman|wife|mother|sister|daughter|grandmother|aunt|niece|girlfriend|bride|queen|princess)\\b"
    )
    private val explicitMaleRegex = Regex(
        "\\b(male|man|men|boy|waiter|actor|policeman|salesman|postman|businessman|craftsman|husband|father|brother|son|grandfather|uncle|nephew|boyfriend|groom|king|prince)\\b"
    )
    private val personRoleSubtopics = setOf(
        "city_jobs", "people_family", "people_relationships", "work_career", "work_trades",
        "education_school", "education_university", "food_restaurant", "city_shopping",
        "city_services", "shopping_stores", "society_politics", "society_media"
    )
    private val masculinePersonRoles = setOf(
        "teacher", "doctor", "dentist", "vet", "veterinarian", "firefighter", "cook", "chef",
        "engineer", "baker", "butcher", "hairdresser", "driver", "pharmacist", "architect",
        "painter", "carpenter", "electrician", "plumber", "gardener", "mechanic", "journalist",
        "photographer", "lawyer", "cashier", "customer", "student", "professor", "friend",
        "neighbour", "neighbor", "colleague", "partner", "flatmate", "classmate", "boss",
        "employee", "citizen", "singer", "artist", "chancellor", "minister"
    )
    private val roleQueryAliases = mapOf(
        "vet" to "veterinarian",
        "flatmate" to "roommate",
        "neighbour" to "neighbor",
        "student peer" to "college student"
    )

    // Generic head nouns & particles that produce wildly off-topic photos if searched alone
    private val ambiguousSingleFallbackWords = setOf(
        "machine", "room", "card", "board", "box", "bar", "shop", "store", "house", "home",
        "day", "time", "line", "sign", "light", "paper", "water", "oil", "juice", "cream",
        "soup", "sauce", "cake", "bread", "test", "game", "work", "book", "bag", "case",
        "ring", "band", "way", "side", "part", "place", "point", "set", "system", "service",
        "center", "centre", "station", "control", "check", "pass", "list", "plan", "rate",
        "level", "power", "energy", "sense", "state", "form", "type", "kind", "sort", "mode",
        "style", "pool", "party", "ticket", "table", "chair", "glass", "cup", "plate", "dish",
        "man", "woman", "person", "people", "thing", "stuff", "object", "item", "area", "zone",
        "up", "off", "out", "down", "over", "away", "back", "in", "on", "for", "with", "into",
        "onto", "from", "through", "around", "about", "after", "before", "under", "again"
    )

    private val germanNonNounCapitalized = setOf(
        "ich", "du", "er", "sie", "es", "wir", "ihr", "man", "ein", "eine", "einen", "einem", "einer",
        "der", "die", "das", "den", "dem", "des", "dieser", "diese", "dieses", "mein", "meine",
        "dein", "deine", "sein", "seine", "unser", "unsere", "alle", "alles", "viele", "sehr",
        "nicht", "kein", "keine", "sich", "hier", "dort", "heute", "morgen", "gestern", "immer",
        "oft", "bitte", "danke", "vorsicht", "achtung", "leider", "natürlich", "vielleicht",
        "wenn", "weil", "dass", "aber", "oder", "und", "mit", "für", "auf", "bei", "nach", "von",
        "aus", "zum", "zur", "im", "am", "um", "über", "unter", "vor", "hinter", "zwischen"
    )

    // Curated concise 1-2 word Pixabay queries for places, rooms, objects, verbs, and abstract words
    private val specialGermanQueries: Map<String, List<String>> = mapOf(
        // City places & buildings (prefer actual buildings/interiors over tangential objects/portraits)
        "apotheke" to listOf("pharmacy shop", "pharmacy store", "apothecary"),
        "büro" to listOf("office room", "office interior", "workspace office"),
        "schule" to listOf("school building", "classroom", "schoolhouse"),
        "krankenhaus" to listOf("hospital building", "hospital exterior", "hospital"),
        "universität" to listOf("university building", "university campus", "university"),
        "bibliothek" to listOf("library interior", "library shelves", "library"),
        "supermarkt" to listOf("supermarket aisle", "grocery store", "supermarket"),
        "bäckerei" to listOf("bakery shop", "bakery bread", "bakery"),
        "metzgerei" to listOf("butcher shop", "meat market"),
        "buchhandlung" to listOf("bookstore", "bookshop"),
        "blumenladen" to listOf("flower shop", "florist shop"),
        "friseur" to listOf("hair salon", "barber shop"),
        "geschäft" to listOf("storefront shop", "retail store", "shop"),
        "laden" to listOf("shop storefront", "boutique shop"),
        "kaufhaus" to listOf("department store", "shopping mall"),
        "post" to listOf("post office", "mailbox"),
        "rathaus" to listOf("city hall", "town hall"),
        "kirche" to listOf("church building", "church"),
        "dom" to listOf("cathedral building", "cathedral"),
        "museum" to listOf("museum gallery", "museum building", "museum"),
        "theater" to listOf("theater stage", "theater hall"),
        "kino" to listOf("cinema theater", "movie theater"),
        "restaurant" to listOf("restaurant interior", "restaurant dining"),
        "café" to listOf("coffee shop", "cafe interior"),
        "hotel" to listOf("hotel building", "hotel lobby", "hotel"),
        "bahnhof" to listOf("train station", "railway station"),
        "flughafen" to listOf("airport terminal", "airport"),
        "tankstelle" to listOf("gas station", "petrol station"),
        "parkplatz" to listOf("parking lot", "car park"),
        "werkstatt" to listOf("car workshop", "garage workshop"),
        "fabrik" to listOf("factory building", "industrial factory"),
        "kindergarten" to listOf("kindergarten classroom", "preschool room"),
        "schwimmbad" to listOf("swimming pool"),
        "stadion" to listOf("football stadium", "stadium"),
        "park" to listOf("city park", "green park"),
        "klassenzimmer" to listOf("classroom desk", "school classroom"),
        "hörsaal" to listOf("lecture hall", "university auditorium"),
        "schaufenster" to listOf("shop window", "storefront display"),

        // Home rooms & everyday objects
        "wohnung" to listOf("apartment interior", "modern apartment"),
        "haus" to listOf("modern house", "family house"),
        "küche" to listOf("kitchen interior", "modern kitchen"),
        "wohnzimmer" to listOf("living room", "sofa room"),
        "schlafzimmer" to listOf("bedroom interior", "bedroom"),
        "badezimmer" to listOf("bathroom interior", "modern bathroom"),
        "balkon" to listOf("apartment balcony", "balcony"),
        "flur" to listOf("hallway interior", "corridor"),
        "schreibtisch" to listOf("office desk", "wooden desk"),
        "bürostuhl" to listOf("office chair"),
        "drucker" to listOf("office printer"),
        "ordner" to listOf("ring binder", "office folders"),
        "tastatur" to listOf("computer keyboard"),
        "kopfhörer" to listOf("headphones"),
        "zahnbürste" to listOf("toothbrush"),
        "handtuch" to listOf("bath towel", "folded towels"),
        "sonnenbrille" to listOf("sunglasses"),
        "koffer" to listOf("travel suitcase", "luggage suitcase"),
        "radiergummi" to listOf("pencil eraser"),
        "wörterbuch" to listOf("dictionary book"),
        "kreditkarte" to listOf("credit card"),
        "bargeld" to listOf("euro banknotes", "cash money"),
        "kassenbon" to listOf("shopping receipt", "paper receipt"),
        "einkaufswagen" to listOf("shopping cart"),
        "reißverschluss" to listOf("zipper closeup"),
        "speisekarte" to listOf("restaurant menu"),
        "rechnung" to listOf("restaurant bill", "invoice paper"),
        "trinkgeld" to listOf("tip coins", "money jar"),
        "besteck" to listOf("cutlery fork knife", "silverware"),
        "fahrkarte" to listOf("train ticket"),

        // Travel & abstract station/service terms
        "verspätung" to listOf("train clock", "train station"),
        "abfahrt" to listOf("train departure", "train station"),
        "ankunft" to listOf("airport arrival", "airport terminal"),
        "fahrplan" to listOf("train timetable", "train schedule"),
        "gleis" to listOf("railway tracks", "train track"),
        "bahnsteig" to listOf("train platform", "railway platform"),
        "durchsage" to listOf("megaphone", "loudspeaker"),
        "sicherheitskontrolle" to listOf("airport security", "airport checkpoint"),
        "zoll" to listOf("passport control", "customs"),
        "umsteigen" to listOf("train platform", "train station"),
        "auskunft" to listOf("information desk", "reception desk"),
        "kontoauszug" to listOf("bank receipt", "financial document"),
        "geldautomat" to listOf("atm machine", "cash machine"),
        "führerschein" to listOf("driver license"),
        "personalausweis" to listOf("identity card"),
        "reisepass" to listOf("passport"),
        "krankenwagen" to listOf("ambulance"),
        "feuerwehr" to listOf("fire truck"),

        // Vegetables, fruits & concrete objects
        "aubergine" to listOf("eggplant"),
        "blumenkohl" to listOf("cauliflower"),
        "zucchini" to listOf("zucchini"),
        "spargel" to listOf("asparagus"),
        "radieschen" to listOf("radish"),
        "paprika" to listOf("bell pepper"),
        "pilz" to listOf("mushroom"),
        "champignon" to listOf("mushroom"),
        "kohl" to listOf("cabbage"),
        "erbse" to listOf("green peas"),
        "bohne" to listOf("green beans"),
        "lauch" to listOf("leek"),
        "sellerie" to listOf("celery"),
        "fenchel" to listOf("fennel"),
        "ingwer" to listOf("ginger"),
        "artischocke" to listOf("artichoke"),
        "rote bete" to listOf("beetroot"),
        "handy" to listOf("smartphone"),
        "kühlschrank" to listOf("refrigerator"),
        "staubsauger" to listOf("vacuum cleaner"),
        "waschmaschine" to listOf("washing machine"),
        "trocken" to listOf("dry desert"),
        "silbergrau" to listOf("silver metal"),
        "passend" to listOf("puzzle"),
        "mitgliedschaft" to listOf("membership card"),

        // High-frequency German verbs -> clear visual scenes
        "schweigen" to listOf("quiet finger lips", "silence gesture"),
        "aufstehen" to listOf("waking up morning", "alarm clock bed"),
        "schlafen" to listOf("sleeping bed", "peaceful sleep"),
        "laufen" to listOf("person running", "jogging park"),
        "schwimmen" to listOf("swimming pool person", "swimmer water"),
        "kochen" to listOf("cooking kitchen", "chef cooking"),
        "putzen" to listOf("cleaning house", "home cleaning"),
        "waschen" to listOf("washing hands", "laundry washing"),
        "vergessen" to listOf("confused thinking", "forgetting hand head"),
        "versprechen" to listOf("handshake agreement", "pinky promise"),
        "erklären" to listOf("teacher explaining", "colleagues discussing"),
        "entscheiden" to listOf("crossroads choice", "signpost road"),
        "lachen" to listOf("happy laughing", "laughing friends"),
        "weinen" to listOf("sad crying", "person crying"),
        "singen" to listOf("singer microphone", "person singing"),
        "tanzen" to listOf("couple dancing", "dancing joy"),
        "wandern" to listOf("mountain hiking", "hiker trail"),
        "klettern" to listOf("rock climbing", "climber mountain"),
        "lesen" to listOf("reading book", "person reading"),
        "schreiben" to listOf("writing notebook", "hand writing pen"),
        "sprechen" to listOf("people talking", "conversation friends"),
        "fragen" to listOf("raising hand", "asking question"),
        "helfen" to listOf("helping hand", "volunteers helping"),
        "kaufen" to listOf("shopping bags", "paying store"),
        "bezahlen" to listOf("paying card", "cashier payment"),
        "reparieren" to listOf("mechanic repairing", "fixing tools"),
        "bauen" to listOf("construction building", "builder bricks"),
        "wachsen" to listOf("plant sprouting", "seedling growing")
    )

    // True homonyms where subtopic determines meaning
    private val homonymDisambiguation: Map<Pair<String, String>, String> = mapOf(
        ("orange" to "food_fruits") to "orange fruit",
        ("orange" to "adj_colors") to "orange color",
        ("date" to "food_fruits") to "date fruit",
        ("mouse" to "tech_devices") to "computer mouse",
        ("mouse" to "tech_computers") to "computer mouse",
        ("mouse" to "nature_animals") to "mouse animal",
        ("bank" to "city_places") to "bank building",
        ("bank" to "city_services") to "bank building",
        ("bank" to "nature_landscape") to "river bank",
        ("police" to "city_places") to "police station",
        ("police" to "city_emergency") to "police car",
        ("spring" to "nature_weather") to "spring flowers",
        ("fall" to "nature_weather") to "autumn leaves",
        ("glasses" to "shopping_clothing") to "eyeglasses",
        ("glasses" to "shopping_shoes") to "eyeglasses",
        ("light" to "adj_qualities") to "feather",
        ("light" to "adj_colors") to "sunlight bright",
        ("glass" to "food_drinks") to "water glass",
        ("glass" to "home_kitchen") to "drinking glass",
        ("turkey" to "food_meat_fish") to "roast turkey",
        ("turkey" to "nature_animals") to "turkey bird",
        ("seal" to "nature_animals") to "seal animal",
        ("seal" to "nature_wildlife") to "seal animal",
        ("bat" to "nature_animals") to "bat animal",
        ("bat" to "nature_wildlife") to "bat animal",
        ("crane" to "nature_animals") to "crane bird",
        ("crane" to "nature_wildlife") to "crane bird",
        ("jam" to "food_sweets") to "fruit jam",
        ("toast" to "food_meals") to "toast bread",
        ("chips" to "food_sweets") to "potato chips",
        ("match" to "sports_games") to "sports match",
        ("ring" to "shopping_shoes") to "gold ring jewelry",
        ("watch" to "shopping_shoes") to "wristwatch",
        ("tie" to "shopping_clothing") to "necktie suit",
        ("boot" to "shopping_shoes") to "leather boots",
        ("suit" to "shopping_clothing") to "business suit",
        ("coat" to "shopping_clothing") to "winter coat",
        ("nail" to "work_tools") to "iron nails hammer",
        ("nail" to "body_parts") to "fingernail hand",
        ("palm" to "nature_plants") to "palm tree",
        ("palm" to "body_parts") to "open hand palm",
        ("bark" to "nature_plants") to "tree bark",
        ("trunk" to "nature_plants") to "tree trunk",
        ("bill" to "food_restaurant") to "restaurant bill",
        ("bill" to "shopping_money") to "banknote money",
        ("tip" to "food_restaurant") to "restaurant tip coins",
        ("check" to "food_restaurant") to "restaurant bill",
        ("fan" to "home_appliances") to "electric fan",
        ("chip" to "tech_computers") to "microchip processor",
        ("bug" to "tech_data") to "computer code screen",
        ("cloud" to "tech_internet") to "cloud computing server",
        ("cloud" to "nature_weather") to "sky clouds",
        ("web" to "tech_internet") to "website browser",
        ("file" to "tech_data") to "computer files folder",
        ("file" to "work_office") to "office folder binder",
        ("stamp" to "city_services") to "postage stamp",
        ("letter" to "city_services") to "envelope mail",
        ("note" to "music_instruments") to "musical notes sheet",
        ("note" to "education_exams") to "exam grade paper",
        ("pepper" to "food_vegetables") to "bell pepper",
        ("pepper" to "food_cooking") to "black pepper spice",
        ("roll" to "food_meals") to "bread roll bakery",
        ("bun" to "food_meals") to "bread roll bakery",
        ("pot" to "food_cooking") to "cooking pot kitchen",
        ("pan" to "food_cooking") to "frying pan kitchen",
        ("sink" to "home_bathroom") to "bathroom sink",
        ("sink" to "home_kitchen") to "kitchen sink",
        ("iron" to "home_appliances") to "clothes iron",
        ("pool" to "sports_games") to "swimming pool",
        ("track" to "sports_games") to "running track stadium",
        ("court" to "sports_games") to "tennis court",
        ("coach" to "sports_games") to "sports coach",
        ("plane" to "travel_transport") to "airplane flying",
        ("train" to "travel_transport") to "passenger train",
        ("ship" to "travel_transport") to "cruise ship sea",
        ("gate" to "travel_station") to "airport gate",
        ("bear" to "nature_wildlife") to "brown bear forest",
        ("duck" to "nature_animals") to "duck pond",
        ("mole" to "nature_wildlife") to "mole animal"
    )

    internal fun detectPersonGenderHint(
        word: WordEntity,
        cleanDe: String,
        enSenses: List<String>
    ): PersonGenderHint {
        val enLower = word.translationEn.lowercase()
        val ukLower = word.translationUk.lowercase()
        val articleLower = word.article.lowercase().trim()

        // Explicit (f)/(m) markers take highest priority
        if (enLower.contains("(f)") || ukLower.contains("(жінка)")) {
            return PersonGenderHint.FEMALE
        }
        if (enLower.contains("(m)")) {
            return PersonGenderHint.MALE
        }

        // Check male before female for compounds like "male nurse"
        if (enLower.contains("male nurse") || explicitMaleRegex.containsMatchIn(enLower)) {
            return PersonGenderHint.MALE
        }
        if (explicitFemaleRegex.containsMatchIn(enLower) ||
            (enLower.contains("nurse") && !enLower.contains("male"))
        ) {
            return PersonGenderHint.FEMALE
        }

        // German feminine profession/role endings (e.g., Feuerwehrfrau, Lehrerin, Ärztin)
        if (articleLower == "die") {
            if (cleanDe.endsWith("frau")) {
                return PersonGenderHint.FEMALE
            }
            if (cleanDe.length >= 5 && cleanDe.endsWith("in") && word.subtopicId in personRoleSubtopics) {
                return PersonGenderHint.FEMALE
            }
        }

        // German masculine profession/role nouns (e.g., Feuerwehrmann, der Lehrer in education_school)
        if (articleLower == "der") {
            if (cleanDe.endsWith("mann")) {
                return PersonGenderHint.MALE
            }
            if (word.subtopicId in personRoleSubtopics &&
                enSenses.any { it.lowercase() in masculinePersonRoles }
            ) {
                return PersonGenderHint.MALE
            }
        }

        return PersonGenderHint.NONE
    }

    fun build(word: WordEntity): StockPhotoQueryPlan {
        val steps = ArrayList<PixabaySearchStep>(8)
        val seen = HashSet<String>()

        fun addStep(query: String, lang: String = "en", isFallback: Boolean = false) {
            val normalized = query.trim().take(60)
            if (normalized.length < 2) return
            val key = "$lang:${normalized.lowercase()}"
            if (seen.add(key)) {
                steps.add(PixabaySearchStep(normalized, lang, isFallback))
            }
        }

        val cleanDe = cleanGermanTerm(word.german)
        val enSenses = extractEnglishSenses(word.translationEn)
        val primaryEn = enSenses.firstOrNull().orEmpty()
        val genderHint = detectPersonGenderHint(word, cleanDe, enSenses)

        // 1. Homonym disambiguation (subtopic-specific beats generic override)
        if (primaryEn.isNotBlank()) {
            homonymDisambiguation[primaryEn to word.subtopicId]?.let { disambiguated ->
                addStep(disambiguated, "en")
            }
        }

        // 2. Gender-aware person/profession queries (e.g., Lehrer vs Lehrerin, Feuerwehrmann vs Feuerwehrfrau)
        if (genderHint != PersonGenderHint.NONE) {
            addGenderedPersonSteps(
                genderHint = genderHint,
                enSenses = enSenses,
                cleanDe = cleanDe,
                addStep = ::addStep
            )
        } else {
            // 3. Curated exact override(s) for places, rooms, verbs, and tricky non-person words
            specialGermanQueries[cleanDe]?.forEach { curated ->
                addStep(curated, "en")
            }

            // 4. Primary English sense (concise 1-2 words)
            val concisePrimary = if (primaryEn.isNotBlank()) conciseQuery(primaryEn) else ""
            if (concisePrimary.isNotBlank()) {
                addStep(concisePrimary, "en")
            }

            // 5. Secondary English sense after '/' or ',' (e.g., "untidy / messy" -> "messy")
            if (enSenses.size > 1) {
                addStep(conciseQuery(enSenses[1]), "en")
            }

            // 6. Native German word search BEFORE partial English splits
            // (Pixabay is German-founded and indexes compound German nouns accurately!)
            if (cleanDe.length >= 3 && !cleanDe.equals(primaryEn, ignoreCase = true)) {
                addStep(cleanDe, "de")
            }

            // 7. Single-word head fallback ONLY if not a generic/ambiguous head noun or verb particle
            if (concisePrimary.isNotBlank()) {
                val parts = concisePrimary.split(' ').filter { it.length >= 3 }
                if (parts.size >= 2) {
                    val lastWord = parts.last().lowercase()
                    if (lastWord !in ambiguousSingleFallbackWords) {
                        addStep(lastWord, "en")
                    }
                }
            }

            // 8. Concrete capitalized German noun from exampleDe for abstract words
            extractExampleNoun(word.exampleDe, cleanDe)?.let { exampleNoun ->
                addStep(exampleNoun, "de")
            }
        }

        // 9. Guaranteed visual fallback for subtopic
        addStep(subtopicFallbackQuery(word.subtopicId, word.categoryId), "en", isFallback = true)

        val primaryStepTokens = steps.firstOrNull { !it.isSubtopicFallback }
            ?.query
            ?.lowercase()
            ?.split(' ')
            ?.filter { it.length >= 3 }
            ?.toSet()
            ?: emptySet()

        val profile = buildScoringProfile(word, cleanDe, enSenses, genderHint, primaryStepTokens)
        return StockPhotoQueryPlan(steps = steps, scoringProfile = profile, genderHint = genderHint)
    }

    private fun addGenderedPersonSteps(
        genderHint: PersonGenderHint,
        enSenses: List<String>,
        cleanDe: String,
        addStep: (query: String, lang: String, isFallback: Boolean) -> Unit
    ) {
        val rawPrimary = enSenses.firstOrNull().orEmpty()
        val rawSecondary = enSenses.getOrNull(1).orEmpty()

        val basePrimary = stripGenderPrefix(rawPrimary).let { roleQueryAliases[it] ?: it }
        val baseSecondary = stripGenderPrefix(rawSecondary).let { roleQueryAliases[it] ?: it }
        // Prefer "chef" over "cook" when paired as "cook / chef" since Pixabay chef tags are cleaner
        val leadRole = if (basePrimary == "cook" && baseSecondary == "chef") "chef" else basePrimary

        when (genderHint) {
            PersonGenderHint.FEMALE -> {
                if (leadRole.isNotBlank()) {
                    if (leadRole.endsWith("woman")) {
                        val stem = leadRole.removeSuffix("woman").trim()
                        addStep(leadRole, "en", false)
                        if (stem.length >= 3) {
                            addStep("$stem woman", "en", false)
                            addStep("female $stem", "en", false)
                        }
                    } else if (leadRole in setOf("waitress", "actress", "wife", "mother", "sister", "daughter", "grandmother", "aunt", "niece", "girlfriend", "bride", "queen", "princess", "woman", "girl")) {
                        addStep(leadRole, "en", false)
                        addStep("$leadRole woman", "en", false)
                    } else {
                        val conciseRole = conciseQuery(leadRole)
                        addStep("$conciseRole woman", "en", false)
                        addStep("female $conciseRole", "en", false)
                    }
                }
                if (baseSecondary.isNotBlank() && baseSecondary != leadRole) {
                    val conciseSecondary = conciseQuery(baseSecondary)
                    addStep("$conciseSecondary woman", "en", false)
                }
                if (cleanDe.length >= 3) {
                    addStep(cleanDe, "de", false)
                }
                if (leadRole.isNotBlank()) {
                    addStep(conciseQuery(leadRole), "en", false)
                }
            }

            PersonGenderHint.MALE -> {
                if (leadRole.isNotBlank()) {
                    if (leadRole.endsWith("man") && leadRole != "woman") {
                        val stem = leadRole.removeSuffix("man").trim()
                        addStep(leadRole, "en", false)
                        if (stem.length >= 3) {
                            addStep("$stem man", "en", false)
                        }
                    } else if (leadRole in setOf("waiter", "actor", "husband", "father", "brother", "son", "grandfather", "uncle", "nephew", "boyfriend", "groom", "king", "prince", "man", "boy")) {
                        addStep(leadRole, "en", false)
                        addStep("$leadRole man", "en", false)
                    } else {
                        val conciseRole = conciseQuery(leadRole)
                        addStep("$conciseRole man", "en", false)
                        addStep("male $conciseRole", "en", false)
                    }
                }
                if (baseSecondary.isNotBlank() && baseSecondary != leadRole) {
                    val conciseSecondary = conciseQuery(baseSecondary)
                    addStep("$conciseSecondary man", "en", false)
                }
                if (cleanDe.length >= 3) {
                    addStep(cleanDe, "de", false)
                }
                if (leadRole.isNotBlank()) {
                    addStep(conciseQuery(leadRole), "en", false)
                }
            }

            PersonGenderHint.NONE -> Unit
        }
    }

    private fun stripGenderPrefix(sense: String): String {
        return sense.lowercase()
            .trim()
            .removePrefix("female ")
            .removePrefix("male ")
            .trim()
    }

    internal fun buildScoringProfile(
        word: WordEntity,
        cleanDe: String,
        enSenses: List<String>,
        genderHint: PersonGenderHint = detectPersonGenderHint(word, cleanDe, enSenses),
        primaryQueryTokens: Set<String> = emptySet()
    ): PhotoScoringProfile {
        val subjectTokens = LinkedHashSet<String>()
        enSenses.take(2).forEach { sense ->
            sense.split(' ').filter { it.length >= 3 }.forEach { token ->
                val lower = token.lowercase()
                subjectTokens.add(lower)
                roleQueryAliases[lower]?.let { subjectTokens.add(it) }
            }
        }
        cleanDe.split(' ').filter { it.length >= 3 }.forEach { subjectTokens.add(it.lowercase()) }
        if (genderHint == PersonGenderHint.NONE) {
            specialGermanQueries[cleanDe]?.firstOrNull()?.split(' ')
                ?.filter { it.length >= 3 }
                ?.forEach { subjectTokens.add(it.lowercase()) }
        }
        primaryQueryTokens.forEach { subjectTokens.add(it) }

        val (preferred, avoid, allowGraphic) = when {
            genderHint == PersonGenderHint.FEMALE -> Triple(
                setOf(
                    "woman", "female", "lady", "girl", "person", "people",
                    "portrait", "worker", "professional", "uniform"
                ),
                setOf(
                    "man", "men", "male", "gentleman", "boy", "boys", "beard", "mustache",
                    "empty", "building", "landscape", "abstract"
                ),
                false
            )
            genderHint == PersonGenderHint.MALE -> Triple(
                setOf(
                    "man", "male", "gentleman", "person", "people",
                    "portrait", "worker", "professional", "uniform"
                ),
                setOf(
                    "woman", "women", "female", "lady", "girl", "girls",
                    "empty", "building", "landscape", "abstract"
                ),
                false
            )
            word.subtopicId in setOf("city_places", "shopping_stores", "travel_station", "travel_hotel") -> Triple(
                setOf(
                    "building", "architecture", "facade", "exterior", "interior", "store", "shop",
                    "storefront", "classroom", "hall", "room", "workspace", "entrance", "street",
                    "city", "urban", "shelves", "counter", "lobby", "terminal", "station", "campus",
                    "pharmacy", "hospital", "school", "office", "library", "bakery", "supermarket"
                ),
                setOf(
                    "pills", "pill", "tablet", "tablets", "capsule", "capsules", "medication",
                    "children", "child", "kids", "kid", "girl", "girls", "boy", "boys", "baby",
                    "portrait", "smiling", "smile", "face", "woman", "man", "selfie", "people",
                    "laptop", "notebook", "pen", "pencil", "smartphone", "phone", "keyboard",
                    "stethoscope", "syringe", "hands", "hand"
                ),
                false
            )
            word.subtopicId == "home_rooms" -> Triple(
                setOf("room", "interior", "home", "house", "apartment", "furniture", "modern", "design", "living", "architecture"),
                setOf("portrait", "face", "woman", "man", "child", "children", "girl", "boy", "cat", "dog", "pet"),
                false
            )
            word.subtopicId in setOf("home_furniture", "home_kitchen", "home_bathroom", "home_appliances") -> Triple(
                setOf("home", "interior", "furniture", "kitchen", "bathroom", "appliance", "tableware", "modern", "clean", "household", "wood", "ceramic"),
                setOf("portrait", "selfie", "face", "crowd", "concert", "landscape"),
                false
            )
            word.subtopicId == "work_office" -> Triple(
                setOf("office", "workspace", "desk", "room", "interior", "chair", "business", "corporate", "meeting"),
                setOf("child", "children", "baby", "animal", "pet", "flower"),
                false
            )
            word.subtopicId == "work_tools" -> Triple(
                setOf("tool", "tools", "workshop", "craft", "carpenter", "woodworking", "repair", "metal", "hardware", "construction", "diy"),
                setOf("portrait", "selfie", "flower", "food", "animal"),
                false
            )
            word.categoryId == "food" && word.subtopicId != "food_restaurant" -> Triple(
                setOf("food", "fruit", "vegetable", "fresh", "dish", "meal", "drink", "eat", "healthy", "organic", "ripe", "sweet", "bakery", "cooking", "ingredient"),
                setOf("portrait", "woman", "man", "girl", "boy", "child", "children", "people", "person", "face", "selfie"),
                false
            )
            word.subtopicId in setOf("nature_animals", "nature_wildlife") -> Triple(
                setOf("animal", "wildlife", "mammal", "bird", "pet", "nature", "fauna", "wild"),
                setOf("portrait", "woman", "man", "girl", "boy", "child", "people", "toy", "plush", "statue", "sculpture", "tattoo"),
                false
            )
            word.subtopicId in setOf("nature_plants", "nature_landscape", "nature_weather") -> Triple(
                setOf("nature", "plant", "flower", "tree", "landscape", "outdoor", "sky", "forest", "mountain", "water", "weather"),
                setOf("portrait", "woman", "man", "girl", "boy", "child", "selfie"),
                false
            )
            word.subtopicId in setOf("travel_transport", "travel_city_nav", "travel_vacation") -> Triple(
                setOf("transport", "vehicle", "travel", "road", "railway", "aviation", "street", "bridge", "vacation", "luggage", "journey", "tourism"),
                setOf("selfie", "baby", "cat", "dog", "food"),
                false
            )
            word.subtopicId in setOf("shopping_clothing", "shopping_shoes", "people_appearance", "shopping_materials") -> Triple(
                setOf("clothing", "fashion", "apparel", "wear", "garment", "shoes", "footwear", "fabric", "textile", "style", "outfit", "accessory"),
                setOf("landscape", "animal", "food", "car"),
                false
            )
            word.subtopicId in setOf("shopping_money", "city_services") -> Triple(
                setOf("money", "euro", "finance", "bank", "payment", "cash", "card", "receipt", "economy", "mail", "post", "service"),
                setOf("animal", "pet", "food", "flower"),
                false
            )
            word.categoryId == "tech" -> Triple(
                setOf("technology", "computer", "digital", "device", "electronics", "hardware", "screen", "internet", "science", "laboratory", "code", "modern"),
                setOf("flower", "food", "pet", "vintage"),
                false
            )
            word.categoryId == "education" -> Triple(
                setOf("education", "school", "study", "university", "book", "books", "classroom", "stationery", "desk", "learning", "exam", "writing"),
                setOf("beach", "party", "alcohol", "animal"),
                false
            )
            word.categoryId == "body" -> Triple(
                setOf("health", "medical", "medicine", "doctor", "care", "body", "fitness", "wellness", "hygiene", "sport", "clean", "hospital"),
                setOf("horror", "zombie", "skull", "blood", "weapon"),
                false
            )
            word.subtopicId in setOf("sports_games", "music_instruments", "outdoor_activities", "gaming_entertainment") -> Triple(
                setOf("sport", "sports", "game", "music", "instrument", "concert", "outdoor", "hiking", "camping", "adventure", "gaming", "cinema", "ball"),
                setOf("office", "medical", "pills"),
                false
            )
            word.subtopicId in setOf("city_jobs", "people_family", "people_relationships", "work_career", "work_trades") -> Triple(
                setOf("person", "people", "portrait", "worker", "professional", "uniform", "doctor", "teacher", "chef", "family", "friends", "team"),
                setOf("empty", "building", "landscape", "abstract"),
                false
            )
            word.categoryId == "verbs" -> Triple(
                setOf("person", "people", "action", "activity", "lifestyle", "hand", "hands", "motion", "daily", "work", "movement"),
                setOf("logo", "icon", "diagram", "chart", "blank"),
                false
            )
            word.subtopicId in setOf("art_creativity", "adj_colors") -> Triple(
                setOf("color", "colorful", "art", "paint", "painting", "palette", "texture", "canvas"),
                setOf("portrait", "selfie"),
                true
            )
            else -> Triple(
                emptySet(),
                emptySet(),
                false
            )
        }

        // Never penalize a tag that is literally one of the word's own subject tokens!
        val safeAvoid = avoid.filterNot { it in subjectTokens }.toSet()

        return PhotoScoringProfile(
            subjectTokens = subjectTokens,
            preferredTags = preferred,
            avoidTags = safeAvoid,
            allowGraphicStyle = allowGraphic,
            primaryQueryTokens = primaryQueryTokens
        )
    }

    internal fun extractEnglishSenses(raw: String): List<String> {
        return raw.split('/', ';', ',')
            .mapNotNull { part ->
                val cleaned = cleanEnglishTerm(part)
                cleaned.takeIf { it.isNotBlank() }
            }
            .distinct()
    }

    internal fun cleanEnglishTerm(raw: String): String {
        val stripped = parentheticalRegex.replace(raw, " ")
            .lowercase()
            .trim()
            .removePrefix("to be ")
            .removePrefix("to ")
            .removePrefix("the ")
            .removePrefix("a ")
            .removePrefix("an ")
            .removePrefix("one's ")
            .removePrefix("someone ")
            .removePrefix("something ")
        return multiSpaceRegex
            .replace(nonEnWordRegex.replace(stripped, " "), " ")
            .trim()
    }

    internal fun cleanGermanTerm(raw: String): String {
        val firstSense = raw
            .split('/', ';', ',')
            .firstOrNull()
            .orEmpty()
        val stripped = parentheticalRegex.replace(firstSense, " ")
            .lowercase()
            .trim()
            .removePrefix("der ")
            .removePrefix("die ")
            .removePrefix("das ")
            .removePrefix("sich ")
            .trim()
        return multiSpaceRegex
            .replace(nonDeWordRegex.replace(stripped, " "), " ")
            .trim()
    }

    internal fun extractExampleNoun(exampleDe: String, excludeLower: String): String? {
        if (exampleDe.isBlank()) return null
        val tokens = germanTokenRegex.findAll(exampleDe).map { it.value }.toList()
        if (tokens.isEmpty()) return null

        val midSentenceNoun = tokens.drop(1).firstOrNull { token ->
            token.length >= 4 &&
                token[0].isUpperCase() &&
                token.lowercase() != excludeLower &&
                token.lowercase() !in germanNonNounCapitalized
        }
        if (midSentenceNoun != null) return midSentenceNoun

        val first = tokens.first()
        if (first.length >= 4 &&
            first[0].isUpperCase() &&
            first.lowercase() != excludeLower &&
            first.lowercase() !in germanNonNounCapitalized
        ) {
            return first
        }
        return null
    }

    private fun conciseQuery(cleaned: String): String {
        val words = cleaned.split(' ').filter { it.isNotBlank() }
        return if (words.size <= 2) cleaned else words.take(2).joinToString(" ")
    }

    internal fun subtopicFallbackQuery(subtopicId: String, categoryId: String): String = when {
        subtopicId == "food_fruits" -> "fruit"
        subtopicId == "food_vegetables" -> "vegetables"
        subtopicId == "food_drinks" -> "drink"
        subtopicId == "food_sweets" -> "dessert"
        subtopicId == "food_meals" -> "food"
        subtopicId == "food_meat_fish" -> "cooking"
        subtopicId == "nature_animals" || subtopicId == "nature_wildlife" -> "animal"
        subtopicId == "nature_plants" -> "plant"
        subtopicId == "nature_weather" -> "weather"
        subtopicId == "travel_station" -> "train station"
        subtopicId == "travel_transport" -> "transport"
        subtopicId == "city_places" -> "city building"
        subtopicId == "home_rooms" -> "room interior"
        subtopicId == "adj_colors" -> "colorful"
        subtopicId == "music_instruments" -> "music"
        subtopicId == "sports_games" -> "sport"
        categoryId == "food" -> "food"
        categoryId == "nature" -> "nature"
        categoryId == "travel" -> "travel"
        categoryId == "body" -> "health"
        categoryId == "city" -> "city"
        categoryId == "home" -> "interior"
        categoryId == "work" -> "office"
        categoryId == "tech" -> "technology"
        categoryId == "education" -> "books"
        categoryId == "shopping" -> "shopping"
        categoryId == "emotions" -> "people"
        categoryId == "family" -> "family"
        categoryId == "time" -> "clock"
        else -> "lifestyle"
    }
}
