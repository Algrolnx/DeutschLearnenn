package com.example.data.repository

import com.example.data.model.WordEntity

data class PixabaySearchStep(
    val query: String,
    val lang: String = "en",
    val isSubtopicFallback: Boolean = false,
    val pixabayCategory: String? = null
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
        "vorspeise" to listOf("appetizer dish", "starter salad food"),
        "hauptgericht" to listOf("main course dish", "restaurant dinner plate"),
        "hauptspeise" to listOf("main course dish", "restaurant dinner plate"),
        "nachtisch" to listOf("dessert cake plate", "sweet dessert"),
        "nachspeise" to listOf("dessert cake plate", "sweet dessert"),
        "tagesgericht" to listOf("restaurant daily dish", "chalkboard menu food"),
        "guten appetit" to listOf("friends dining dinner", "delicious meal table"),
        "torte" to listOf("cream layer cake", "birthday cake"),
        "bonbon" to listOf("colorful candy sweets", "hard candy"),
        "marmelade" to listOf("fruit jam jar", "strawberry jam bread"),
        "punsch" to listOf("mulled wine mug", "hot fruit punch"),
        "limette" to listOf("green lime citrus", "fresh lime fruit"),
        "fahrkarte" to listOf("train ticket"),

        // Travel & abstract station/service terms
        "haltestelle" to listOf("bus stop shelter", "city bus stop"),
        "sitzplatz" to listOf("train passenger seat", "airplane seat"),
        "übernachtung" to listOf("hotel bedroom bed", "cozy hotel room"),
        "wlan" to listOf("wifi router signal", "laptop coffee wifi"),
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
        "links" to listOf("left arrow sign", "road sign left"),
        "rechts" to listOf("right arrow sign", "road sign right"),
        "geradeaus" to listOf("straight highway road", "road ahead"),
        "richtung" to listOf("direction signpost", "wooden signpost arrows"),
        "norden" to listOf("compass north needle", "compass map"),
        "osten" to listOf("sunrise compass east", "compass map"),
        "süden" to listOf("compass south map", "sunny coast south"),

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
        "wachsen" to listOf("plant sprouting", "seedling growing"),

        // Wichtige Adjektive — Qualität & Wert (concrete visual scenes instead of ambiguous single words like "bad"/"terrible")
        "gut" to listOf("thumbs up", "happy smile"),
        "schlecht" to listOf("thumbs down", "rotten apple"),
        "toll" to listOf("fireworks celebration", "happy cheering"),
        "wunderbar" to listOf("rainbow mountains", "aurora night sky"),
        "ausgezeichnet" to listOf("gold trophy", "gold medal"),
        "prima" to listOf("ok hand gesture", "thumbs up smile"),
        "schrecklich" to listOf("scared shocked face", "lightning storm"),
        "furchtbar" to listOf("dark storm clouds", "frightened face"),
        "wichtig" to listOf("exclamation mark", "sticky note reminder"),
        "unwichtig" to listOf("crumpled paper trash", "waste paper bin"),
        "nützlich" to listOf("swiss army knife", "handyman tools"),
        "nutzlos" to listOf("broken umbrella", "rusty scrap junk"),
        "einfach" to listOf("minimalist white desk", "simple puzzle"),
        "schwierig" to listOf("rubik cube", "maze labyrinth"),
        "kompliziert" to listOf("tangled cables", "complex clockwork gears"),
        "leicht" to listOf("floating feather", "dandelion seeds wind"),
        "schwer" to listOf("heavy dumbbell", "gym barbell weights"),
        "richtig" to listOf("green checkmark", "checked exam paper"),
        "falsch" to listOf("red x cross", "wrong answer red"),
        "wahr" to listOf("brass compass", "honest handshake"),
        "echt" to listOf("sparkling diamond", "wax seal letter"),
        "künstlich" to listOf("plastic artificial flowers", "robotic hand"),
        "sauber" to listOf("clean white towels", "sparkling clean kitchen"),
        "schmutzig" to listOf("muddy boots", "dirty muddy hands"),
        "ordentlich" to listOf("organized clean desk", "neat folded clothes"),
        "unordentlich" to listOf("messy desk", "cluttered room"),
        "neu" to listOf("shiny new car", "wrapped gift box"),
        "alt" to listOf("vintage antique clock", "old leather books"),
        "modern" to listOf("modern glass building", "contemporary interior"),
        "antik" to listOf("antique pocket watch", "vintage furniture"),
        "billig" to listOf("sale discount tag", "coins piggy bank"),
        "teuer" to listOf("luxury gold watch", "diamond necklace"),
        "günstig" to listOf("discount price tag", "market bargain"),
        "kostbar" to listOf("precious gemstones", "gold jewelry box"),
        "wertvoll" to listOf("gold bars", "antique treasure"),
        "wertlos" to listOf("empty wallet", "torn paper"),
        "sicher" to listOf("metal padlock lock", "safety helmet"),
        "gefährlich" to listOf("danger warning sign", "steep rocky cliff"),
        "heil" to listOf("fresh whole egg", "healthy red apple"),
        "kaputt" to listOf("broken shattered glass", "cracked phone screen"),
        "ganz" to listOf("whole round pizza", "complete jigsaw puzzle"),
        "perfekt" to listOf("bullseye dart target", "perfect snowflake"),
        "mangelhaft" to listOf("cracked ceramic mug", "broken wall crack"),
        "bequem" to listOf("cozy plush armchair", "soft sofa cushions"),
        "unbequem" to listOf("hard wooden bench", "back pain office"),
        "praktisch" to listOf("pocket multitool", "organized travel backpack"),
        "unpraktisch" to listOf("overstuffed suitcase", "heavy boxes stairs"),
        "nötig" to listOf("first aid kit", "drinking water bottle"),
        "möglich" to listOf("open door sunlight", "mountain summit climber"),
        "makellos" to listOf("white pearl shell", "flawless diamond"),
        "makellose" to listOf("white pearl shell", "flawless diamond"),
        "haltbar" to listOf("glass mason jars", "preserved food jars"),
        "zerbrechlich" to listOf("fragile wine glass", "delicate eggshell"),
        "überflüssig" to listOf("overflowing wardrobe", "pile old boxes"),
        "gemütlich" to listOf("cozy fireplace blanket", "warm tea armchair"),
        "preiswert" to listOf("price tag sale", "fresh market basket"),
        "wasserdicht" to listOf("water droplets leaf", "raincoat umbrella rain"),
        "nachhaltig" to listOf("solar panels wind turbine", "hands holding sapling"),
        "zuverlässigst" to listOf("mechanical watch gears", "coastal lighthouse"),
        "robust" to listOf("heavy steel chain", "hiking boots rock"),
        "langlebig" to listOf("ancient oak tree", "old stone castle"),
        "handlich" to listOf("compact pocket notebook", "mini camera hand"),
        "exzellent" to listOf("five stars gold", "golden trophy cup"),
        "sparsam" to listOf("piggy bank coins", "glass jar savings"),
        "authentisch" to listOf("pottery wheel artisan", "handmade bread bakery"),
        "kinderleicht" to listOf("wooden toy blocks", "child simple puzzle"),
        "klassisch" to listOf("classical violin bow", "grand piano concert"),
        "komisch" to listOf("funny dog face", "smiling clown"),
        "komplett" to listOf("completed jigsaw puzzle", "full chess set"),
        "konkret" to listOf("architect blueprint", "magnifying glass map"),
        "korrekt" to listOf("green checkmark", "checked checklist"),
        "kulinarisch" to listOf("gourmet restaurant plate", "chef plating dish"),
        "lächerlich" to listOf("silly sunglasses dog", "funny costume"),
        "ländlich" to listOf("countryside farm meadow", "rural village hills"),
        "lebensnah" to listOf("family dinner table", "everyday city cafe"),
        "leer" to listOf("empty glass cup", "empty sunlit room"),
        "legendär" to listOf("medieval hilltop castle", "ancient ruins sunset"),
        "lesbar" to listOf("open book pages", "reading glasses book"),
        "logisch" to listOf("chess game strategy", "wooden logic puzzle"),
        "lohnenswert" to listOf("mountain peak sunrise", "scenic lookout view"),
        "malerisch" to listOf("picturesque lake village", "colorful coastal town"),
        "markant" to listOf("dramatic mountain peak", "red coastal lighthouse"),
        "merkwürdig" to listOf("mysterious wooden door", "curious owl eyes"),
        "mühelos" to listOf("eagle gliding sky", "calm floating swan"),
        "mündlich" to listOf("people talking coffee", "interview microphone"),
        "natürlich" to listOf("green forest sunlight", "fresh dew leaves"),
        "niedlich" to listOf("cute fluffy kitten", "cute puppy grass"),
        "oberflächlich" to listOf("water surface ripples", "pond reflection"),
        "offensichtlich" to listOf("magnifying glass clue", "bright arrow sign"),
        "öffentlich" to listOf("city bus public", "public square fountain"),
        "örtlich" to listOf("local farmers market", "town square bakery"),
        "positiv" to listOf("smiling happy person", "bright yellow sunflower"),
        "prächtig" to listOf("palace flower garden", "peacock colorful feathers"),
        "präzise" to listOf("watchmaker precision tools", "metal caliper measure"),
        "privat" to listOf("diary notebook lock", "quiet garden gate"),
        "provisorisch" to listOf("camping tent forest", "wooden scaffolding"),

        // Other abstract adjectives (size/time, sensory, personality)
        "groß" to listOf("tall skyscraper", "giant elephant"),
        "klein" to listOf("tiny ladybug leaf", "small espresso cup"),
        "riesig" to listOf("giant sequoia tree", "huge whale ocean"),
        "winzig" to listOf("tiny ant leaf", "miniature sprout"),
        "lang" to listOf("long straight road", "long bridge"),
        "kurz" to listOf("short pencil", "short path"),
        "breit" to listOf("wide highway road", "wide river valley"),
        "schmal" to listOf("narrow cobblestone alley", "narrow footpath"),
        "eng" to listOf("narrow canyon alley", "tight packed books"),
        "weit" to listOf("vast open horizon", "wide green meadow"),
        "langsam" to listOf("garden snail leaf", "tortoise walking"),
        "schnell" to listOf("high speed train", "cheetah running"),
        "plötzlich" to listOf("lightning bolt sky", "water splash drop"),
        "direkt" to listOf("straight desert road", "arrow target"),
        "warm" to listOf("warm tea cup", "cozy fireplace"),
        "kalt" to listOf("snowy winter ice", "frozen icicles"),
        "heiß" to listOf("steaming hot coffee", "glowing campfire"),
        "kühl" to listOf("iced water mint", "morning mist forest"),
        "lauwarm" to listOf("warm water hands", "bath tub water"),
        "eiskalt" to listOf("ice cubes glass", "frozen glacier"),
        "feucht" to listOf("rain drops window", "morning dew grass"),
        "nass" to listOf("rain umbrella wet", "wet cobblestone rain"),
        "schwül" to listOf("tropical rainforest mist", "humid summer sky"),
        "windig" to listOf("wind turbines grass", "blowing wind trees"),
        "freundlich" to listOf("friendly smiling greeting", "warm handshake smile"),
        "unfreundlich" to listOf("grumpy crossed arms", "frowning angry face"),
        "nett" to listOf("kind smiling friend", "giving flowers smile"),
        "lieb" to listOf("mother hugging child", "affectionate puppy"),
        "böse" to listOf("angry frowning face", "angry storm dark"),
        "brav" to listOf("obedient sitting dog", "calm studying child"),
        "gemein" to listOf("arguing angry person", "bullying conflict"),
        "grausam" to listOf("dark barbed wire", "harsh winter storm"),
        "hilfsbereit" to listOf("helping hand climbing", "volunteers helping"),
        "egoistisch" to listOf("grabbing money hands", "alone turning back"),
        "ehrlich" to listOf("open handshake trust", "looking eyes honest"),
        "besonders" to listOf("single red flower", "shining star night"),
        "zufrieden" to listOf("relaxed happy coffee", "peaceful smiling person"),
        "offen" to listOf("open wooden door", "open window sunlight")
    )

    // True homonyms & domain-sensitive words where subtopic determines the exact visual meaning
    private val homonymDisambiguation: Map<Pair<String, String>, String> = mapOf(
        // Food & Restaurant
        ("orange" to "food_fruits") to "orange fruit",
        ("orange" to "adj_colors") to "orange color",
        ("date" to "food_fruits") to "date fruit",
        ("lime" to "food_fruits") to "green lime fruit",
        ("pepper" to "food_vegetables") to "bell pepper",
        ("pepper" to "food_cooking") to "black pepper spice",
        ("oil" to "food_cooking") to "olive oil cooking",
        ("pot" to "food_cooking") to "cooking pot kitchen",
        ("pan" to "food_cooking") to "frying pan kitchen",
        ("cut" to "food_cooking") to "chopping vegetables knife",
        ("peel" to "food_cooking") to "peeling potato kitchen",
        ("stir" to "food_cooking") to "stirring soup pot",
        ("mix" to "food_cooking") to "mixing dough bowl",
        ("season" to "food_cooking") to "seasoning food spices",
        ("serve" to "food_cooking") to "serving dinner plate",
        ("bake" to "food_cooking") to "baking bread oven",
        ("fry" to "food_cooking") to "frying pan cooking",
        ("jam" to "food_sweets") to "fruit jam jar",
        ("jam" to "food_meals") to "fruit jam jar",
        ("sweet" to "food_sweets") to "colorful candy sweets",
        ("gateau" to "food_sweets") to "cream layer cake",
        ("cream" to "food_sweets") to "whipped cream dessert",
        ("chips" to "food_sweets") to "potato chips",
        ("toast" to "food_meals") to "toast bread",
        ("roll" to "food_meals") to "bread roll bakery",
        ("bun" to "food_meals") to "bread roll bakery",
        ("turkey" to "food_meat_fish") to "roast turkey dinner",
        ("chicken" to "food_meat_fish") to "roast chicken dish",
        ("duck" to "food_meat_fish") to "roast duck dinner",
        ("goose" to "food_meat_fish") to "roast goose dinner",
        ("fish" to "food_meat_fish") to "grilled fish dish",
        ("crab" to "food_meat_fish") to "cooked crab seafood",
        ("punch" to "food_drinks") to "mulled wine hot punch",
        ("glass" to "food_drinks") to "water glass drink",
        ("starter" to "food_restaurant") to "appetizer food dish",
        ("order" to "food_restaurant") to "waiter restaurant order",
        ("table" to "food_restaurant") to "restaurant dining table",
        ("bill" to "food_restaurant") to "restaurant bill check",
        ("tip" to "food_restaurant") to "restaurant tip coins",
        ("check" to "food_restaurant") to "restaurant bill check",
        ("service" to "food_restaurant") to "waiter restaurant service",
        ("guest" to "food_restaurant") to "restaurant guests dining",
        ("reservation" to "food_restaurant") to "reserved restaurant table",
        ("cup" to "food_restaurant") to "coffee cup cafe",

        // Travel & Navigation
        ("plane" to "travel_transport") to "airplane flying",
        ("train" to "travel_transport") to "passenger train",
        ("ship" to "travel_transport") to "cruise ship sea",
        ("boat" to "travel_transport") to "boat water sailing",
        ("fine" to "travel_transport") to "parking ticket fine",
        ("saddle" to "travel_transport") to "bicycle saddle seat",
        ("chain" to "travel_transport") to "bicycle chain gear",
        ("gate" to "travel_station") to "airport gate",
        ("stop" to "travel_station") to "bus stop shelter",
        ("counter" to "travel_station") to "airport ticket counter",
        ("seat" to "travel_station") to "train passenger seat",
        ("track" to "travel_station") to "railway train track",
        ("hub" to "travel_station") to "airport terminal hub",
        ("lift" to "travel_hotel") to "hotel elevator doors",
        ("night" to "travel_hotel") to "hotel bedroom bed",
        ("key" to "travel_hotel") to "hotel room key",
        ("room" to "travel_hotel") to "hotel bedroom interior",
        ("berth" to "travel_hotel") to "marina harbor boat",
        ("square" to "travel_city_nav") to "european town square",
        ("map" to "travel_city_nav") to "city street map",
        ("corner" to "travel_city_nav") to "city street corner",
        ("stamp" to "travel_vacation") to "passport postage stamp",

        // Health, Body & Fitness
        ("cold" to "health_illness") to "sick person sneezing tissue",
        ("plaster" to "health_illness") to "adhesive bandage bandaid",
        ("plaster" to "city_emergency") to "adhesive bandage bandaid",
        ("tablet" to "health_illness") to "medicine pills capsule",
        ("cast" to "medicine_care") to "arm plaster cast medical",
        ("tissue" to "appearance_hygiene") to "paper tissue box",
        ("tissue" to "home_bathroom") to "paper tissue box",
        ("cream" to "appearance_hygiene") to "skin cream jar cosmetics",
        ("plank" to "fitness_wellness") to "plank exercise fitness",
        ("coach" to "fitness_wellness") to "fitness trainer gym",
        ("goal" to "fitness_wellness") to "running finish line",
        ("nail" to "body_parts") to "fingernail hand closeup",
        ("palm" to "body_parts") to "open hand palm",
        ("joint" to "body_parts") to "knee joint anatomy sport",
        ("chest" to "body_parts") to "fitness torso chest",
        ("back" to "body_parts") to "human back posture",
        ("heel" to "body_parts") to "foot heel ankle",

        // City, Services, Shopping & Money
        ("bank" to "city_places") to "bank building exterior",
        ("bank" to "city_services") to "bank building exterior",
        ("bank" to "shopping_money") to "bank building exterior",
        ("court" to "city_places") to "courthouse justice building",
        ("court" to "society_politics") to "courtroom justice gavel",
        ("bar" to "city_places") to "cocktail bar interior",
        ("club" to "city_places") to "nightclub disco lights",
        ("police" to "city_places") to "police station building",
        ("police" to "city_emergency") to "police patrol car",
        ("stamp" to "city_services") to "postage stamp envelope",
        ("letter" to "city_services") to "envelope mail letter",
        ("change" to "city_services") to "euro coins change money",
        ("change" to "shopping_money") to "euro coins change money",
        ("branch" to "city_services") to "bank branch storefront",
        ("branch" to "shopping_money") to "bank branch storefront",
        ("pin" to "city_services") to "atm keypad pin terminal",
        ("pin" to "shopping_money") to "atm keypad pin terminal",
        ("interest" to "city_services") to "bank finance percent chart",
        ("interest" to "shopping_money") to "bank finance percent chart",
        ("bill" to "shopping_money") to "euro banknotes money",
        ("tip" to "shopping_money") to "coins tip jar",
        ("glasses" to "shopping_clothing") to "eyeglasses fashion",
        ("glasses" to "shopping_shoes") to "eyeglasses fashion",
        ("ring" to "shopping_shoes") to "gold ring jewelry",
        ("watch" to "shopping_shoes") to "wristwatch leather",
        ("heel" to "shopping_shoes") to "high heel shoe fashion",
        ("boot" to "shopping_shoes") to "leather boots fashion",
        ("tie" to "shopping_clothing") to "necktie business suit",
        ("suit" to "shopping_clothing") to "business suit blazer",
        ("suit" to "people_appearance") to "business suit blazer",
        ("coat" to "shopping_clothing") to "winter coat fashion",
        ("vest" to "shopping_clothing") to "clothing vest waistcoat",
        ("cap" to "shopping_clothing") to "baseball cap hat",
        ("hood" to "shopping_materials") to "hoodie jacket hood",

        // Home & Appliances
        ("glass" to "home_kitchen") to "drinking glass kitchen",
        ("fan" to "home_appliances") to "electric fan room",
        ("iron" to "home_appliances") to "clothes steam iron",
        ("sink" to "home_bathroom") to "bathroom sink faucet",
        ("sink" to "home_kitchen") to "kitchen sink faucet",
        ("stool" to "home_furniture") to "wooden bar stool chair",

        // Nature, Animals & Weather
        ("mouse" to "nature_animals") to "mouse rodent animal",
        ("turkey" to "nature_animals") to "turkey bird farm",
        ("duck" to "nature_animals") to "duck pond water",
        ("calf" to "nature_animals") to "baby cow calf farm",
        ("seal" to "nature_animals") to "seal marine animal",
        ("seal" to "nature_wildlife") to "seal marine animal",
        ("bat" to "nature_animals") to "bat flying animal",
        ("bat" to "nature_wildlife") to "bat flying animal",
        ("crane" to "nature_animals") to "crane bird wildlife",
        ("crane" to "nature_wildlife") to "crane bird wildlife",
        ("bear" to "nature_wildlife") to "brown bear forest",
        ("mole" to "nature_wildlife") to "mole animal garden",
        ("fly" to "nature_wildlife") to "housefly insect macro",
        ("palm" to "nature_plants") to "palm tree tropical",
        ("bark" to "nature_plants") to "tree bark wood",
        ("trunk" to "nature_plants") to "tree trunk forest",
        ("cone" to "nature_plants") to "pine cone conifer",
        ("spring" to "nature_weather") to "spring blossom flowers",
        ("spring" to "nature_landscape") to "natural water spring stream",
        ("fall" to "nature_weather") to "autumn colorful leaves",
        ("shower" to "nature_weather") to "rain shower clouds",
        ("cloud" to "nature_weather") to "sky white clouds",
        ("cold" to "nature_weather") to "winter snow frost ice",
        ("ice" to "nature_weather") to "frozen ice winter",
        ("bank" to "nature_landscape") to "river bank nature",
        ("plain" to "nature_landscape") to "green meadow plain valley",
        ("port" to "nature_landscape") to "sea harbor port boats",

        // Tech, Science & Data
        ("mouse" to "tech_devices") to "computer mouse desk",
        ("mouse" to "tech_computers") to "computer mouse desk",
        ("tablet" to "tech_devices") to "tablet computer screen",
        ("switch" to "tech_devices") to "wall light switch",
        ("key" to "tech_computers") to "computer keyboard key",
        ("drive" to "tech_computers") to "computer hard drive ssd",
        ("file" to "tech_computers") to "computer digital files screen",
        ("folder" to "tech_computers") to "computer folders screen",
        ("chip" to "tech_computers") to "microchip processor board",
        ("cloud" to "tech_internet") to "cloud computing server",
        ("web" to "tech_internet") to "website browser laptop",
        ("link" to "tech_internet") to "website browser address",
        ("cookie" to "tech_internet") to "internet privacy computer",
        ("spam" to "tech_internet") to "email inbox laptop",
        ("bug" to "tech_data") to "programming code screen",
        ("file" to "tech_data") to "computer data files screen",
        ("branch" to "tech_data") to "software programming code",
        ("port" to "tech_data") to "network ethernet server",
        ("log" to "tech_data") to "computer terminal code",
        ("bit" to "tech_data") to "binary code digital data",
        ("script" to "tech_data") to "coding script laptop",
        ("cell" to "tech_science") to "microscope biology cell",
        ("table" to "tech_science") to "spreadsheet data chart",

        // Work, Tools & Office
        ("file" to "work_office") to "office folder binder",
        ("stamp" to "work_office") to "rubber stamp office desk",
        ("shift" to "work_office") to "factory shift work clock",
        ("order" to "work_business") to "business contract order",
        ("nail" to "work_tools") to "iron nails hammer wood",
        ("nut" to "work_tools") to "metal hex nut bolt",
        ("bolt" to "work_tools") to "metal bolt screw hardware",
        ("file" to "work_tools") to "metal rasp woodworking file",
        ("plane" to "work_tools") to "carpenter hand plane wood",
        ("vice" to "work_tools") to "workshop bench vise clamp",
        ("mortar" to "work_tools") to "bricklayer cement mortar trowel",

        // Education, Hobbies, Music, Society, People & Adjectives
        ("ruler" to "education_supplies") to "school ruler measuring",
        ("letter" to "education_languages") to "alphabet letters wooden",
        ("rule" to "education_languages") to "grammar textbook study",
        ("grade" to "education_school") to "school exam paper grade",
        ("subject" to "education_school") to "school books classroom",
        ("note" to "education_exams") to "exam grade paper",
        ("note" to "music_instruments") to "musical notes sheet",
        ("band" to "music_instruments") to "music band concert stage",
        ("beat" to "music_instruments") to "music metronome drums",
        ("stage" to "music_instruments") to "concert music stage",
        ("match" to "sports_games") to "football sports match",
        ("pool" to "sports_games") to "swimming pool lanes",
        ("track" to "sports_games") to "running track stadium",
        ("court" to "sports_games") to "tennis court net",
        ("coach" to "sports_games") to "sports coach team",
        ("goal" to "sports_games") to "soccer goal net stadium",
        ("level" to "gaming_entertainment") to "video game controller screen",
        ("play" to "gaming_entertainment") to "theater stage drama performance",
        ("club" to "people_relationships") to "community club group friends",
        ("board" to "people_relationships") to "business meeting board table",
        ("letter" to "people_relationships") to "handwritten letter envelope",
        ("bell" to "society_religion") to "church bell tower",
        ("temple" to "society_religion") to "ancient temple architecture",
        ("host" to "society_festivals") to "party host dinner guests",
        ("host" to "society_media") to "tv studio presenter microphone",
        ("tip" to "emotions_communication") to "lightbulb idea advice",
        ("light" to "adj_qualities") to "floating feather",
        ("light" to "adj_colors") to "sunlight bright sky",
        ("current" to "adj_size_time") to "clock watch time",
        ("double" to "adj_size_time") to "pair twin dice"
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

    /**
     * Maps subtopics to official Pixabay API `category` values so homonyms (e.g. food "starter" vs car starter,
     * work_tools "plane" vs airplane, health "tablet" vs iPad) stay strictly locked to the right domain.
     */
    internal fun pixabayCategoryFor(word: WordEntity, genderHint: PersonGenderHint): String? {
        if (genderHint != PersonGenderHint.NONE) return "people"
        return when (word.subtopicId) {
            "food_fruits", "food_vegetables", "food_drinks", "food_meals",
            "food_meat_fish", "food_sweets", "food_restaurant", "food_cooking" -> "food"
            "nature_animals", "nature_wildlife" -> "animals"
            "nature_plants", "nature_landscape", "nature_weather" -> "nature"
            "travel_transport" -> "transportation"
            "travel_station", "travel_hotel", "travel_city_nav", "travel_vacation" -> "travel"
            "city_places", "home_rooms" -> "buildings"
            "body_parts", "health_illness", "medicine_care", "appearance_hygiene", "fitness_wellness" -> "health"
            "education_school", "education_university", "education_supplies", "education_exams", "education_languages" -> "education"
            "tech_computers", "tech_internet", "tech_devices", "tech_data" -> "computer"
            "tech_science" -> "science"
            "sports_games" -> "sports"
            "music_instruments" -> "music"
            "shopping_clothing", "shopping_shoes", "people_appearance", "shopping_materials" -> "fashion"
            "work_office", "work_business", "work_career", "shopping_money", "city_services" -> "business"
            "work_tools", "work_trades" -> "industry"
            "people_family", "people_relationships", "city_jobs" -> "people"
            "emotions_positive", "emotions_negative", "emotions_mind", "emotions_traits",
            "emotions_communication", "people_emotions", "people_character" -> "feelings"
            "society_religion" -> "religion"
            else -> null
        }
    }

    fun build(word: WordEntity): StockPhotoQueryPlan {
        val steps = ArrayList<PixabaySearchStep>(10)
        val seen = HashSet<String>()

        fun addStep(
            query: String,
            lang: String = "en",
            isFallback: Boolean = false,
            pixabayCategory: String? = null
        ) {
            val normalized = query.trim().take(60)
            if (normalized.length < 2) return
            val key = "$lang:${pixabayCategory.orEmpty()}:${normalized.lowercase()}"
            if (seen.add(key)) {
                steps.add(PixabaySearchStep(normalized, lang, isFallback, pixabayCategory))
            }
        }

        val cleanDe = cleanGermanTerm(word.german)
        val enSenses = extractEnglishSenses(word.translationEn)
        val primaryEn = enSenses.firstOrNull().orEmpty()
        val genderHint = detectPersonGenderHint(word, cleanDe, enSenses)
        val domainCategory = pixabayCategoryFor(word, genderHint)
        val disambiguatedPrimary = if (primaryEn.isNotBlank()) {
            homonymDisambiguation[primaryEn to word.subtopicId]
        } else {
            null
        }

        // 1. Homonym disambiguation (subtopic-specific beats generic override)
        if (disambiguatedPrimary != null) {
            addStep(disambiguatedPrimary, "en", pixabayCategory = domainCategory)
            if (domainCategory != null) {
                addStep(disambiguatedPrimary, "en", pixabayCategory = null)
            }
        }

        // 2. Gender-aware person/profession queries (e.g., Lehrer vs Lehrerin, Feuerwehrmann vs Feuerwehrfrau)
        if (genderHint != PersonGenderHint.NONE) {
            addGenderedPersonSteps(
                genderHint = genderHint,
                enSenses = enSenses,
                cleanDe = cleanDe,
                addStep = { q, lang, isFallback ->
                    addStep(q, lang, isFallback, if (isFallback) null else domainCategory)
                }
            )
            // Uncategorized fallback for rare professions if Pixabay "people" category returns empty
            if (domainCategory != null && steps.isNotEmpty()) {
                val lead = steps.first()
                addStep(lead.query, lead.lang, isFallback = false, pixabayCategory = null)
            }
        } else {
            // 3. Curated exact override(s) for places, rooms, verbs, and tricky non-person words
            val curatedList = if (disambiguatedPrimary == null) specialGermanQueries[cleanDe] else null
            curatedList?.forEach { curated ->
                addStep(curated, "en", pixabayCategory = domainCategory)
            }

            // 4. Primary English sense (concise 1-2 words) — skip naked ambiguous primary when disambiguated
            val concisePrimary = if (primaryEn.isNotBlank()) conciseQuery(primaryEn) else ""
            if (disambiguatedPrimary == null && concisePrimary.isNotBlank()) {
                addStep(concisePrimary, "en", pixabayCategory = domainCategory)
            }

            // 5. Secondary English sense after '/' or ',' (e.g., "untidy / messy" -> "messy")
            if (enSenses.size > 1) {
                addStep(conciseQuery(enSenses[1]), "en", pixabayCategory = domainCategory)
            }

            // 6. Native German word search BEFORE uncategorized or partial splits
            // (Pixabay is German-founded and indexes compound German nouns accurately!)
            if (cleanDe.length >= 3 && !cleanDe.equals(primaryEn, ignoreCase = true)) {
                addStep(cleanDe, "de", pixabayCategory = domainCategory)
            }

            // 6b. If domain category yielded zero hits for a niche word, retry top query without category filter
            if (domainCategory != null) {
                val bestUncat = curatedList?.firstOrNull()
                    ?: if (disambiguatedPrimary == null) concisePrimary.takeIf { it.isNotBlank() } else null
                if (bestUncat != null) {
                    addStep(bestUncat, "en", pixabayCategory = null)
                }
                if (cleanDe.length >= 3 && !cleanDe.equals(primaryEn, ignoreCase = true)) {
                    addStep(cleanDe, "de", pixabayCategory = null)
                }
            }

            // 7. Single-word head fallback ONLY if not a generic/ambiguous head noun or verb particle
            if (disambiguatedPrimary == null && concisePrimary.isNotBlank()) {
                val parts = concisePrimary.split(' ').filter { it.length >= 3 }
                if (parts.size >= 2) {
                    val lastWord = parts.last().lowercase()
                    if (lastWord !in ambiguousSingleFallbackWords) {
                        addStep(lastWord, "en", pixabayCategory = domainCategory)
                    }
                }
            }

            // 8. Concrete capitalized German noun from exampleDe for abstract words
            extractExampleNoun(word.exampleDe, cleanDe)?.let { exampleNoun ->
                addStep(exampleNoun, "de", pixabayCategory = domainCategory)
            }
        }

        // 9. Guaranteed visual fallback for subtopic
        addStep(subtopicFallbackQuery(word.subtopicId, word.categoryId), "en", isFallback = true, pixabayCategory = domainCategory)
        if (domainCategory != null) {
            addStep(subtopicFallbackQuery(word.subtopicId, word.categoryId), "en", isFallback = true, pixabayCategory = null)
        }

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
            word.subtopicId == "food_restaurant" -> Triple(
                setOf("restaurant", "food", "dining", "meal", "dish", "plate", "table", "menu", "cafe", "bistro", "appetizer", "dinner", "lunch", "cuisine", "waiter", "service"),
                setOf("car", "engine", "motor", "auto", "automotive", "vehicle", "machine", "mechanical", "industrial", "factory", "tool", "computer", "weapon", "animal", "pet", "selfie"),
                false
            )
            word.subtopicId == "food_meat_fish" -> Triple(
                setOf("food", "meat", "fish", "dish", "meal", "grilled", "roasted", "cooked", "plate", "dinner", "kitchen", "culinary", "seafood", "steak", "fillet"),
                setOf("portrait", "woman", "man", "girl", "boy", "child", "children", "people", "person", "face", "selfie", "aquarium", "underwater", "swimming", "pond", "farm", "live", "pet", "bird", "wings", "feathers", "car", "engine", "motor", "machine"),
                false
            )
            word.categoryId == "food" -> Triple(
                setOf("food", "fruit", "vegetable", "fresh", "dish", "meal", "drink", "eat", "healthy", "organic", "ripe", "sweet", "bakery", "cooking", "ingredient", "kitchen"),
                setOf("portrait", "woman", "man", "girl", "boy", "child", "children", "people", "person", "face", "selfie", "car", "engine", "motor", "auto", "machine", "mechanical", "industrial", "factory", "weapon", "computer"),
                false
            )
            word.subtopicId in setOf("nature_animals", "nature_wildlife") -> Triple(
                setOf("animal", "wildlife", "mammal", "bird", "pet", "nature", "fauna", "wild", "insect"),
                setOf("portrait", "woman", "man", "girl", "boy", "child", "people", "toy", "plush", "statue", "sculpture", "tattoo", "cooked", "meat", "plate", "food", "car", "airplane", "machine"),
                false
            )
            word.subtopicId in setOf("nature_plants", "nature_landscape", "nature_weather") -> Triple(
                setOf("nature", "plant", "flower", "tree", "landscape", "outdoor", "sky", "forest", "mountain", "water", "weather"),
                setOf("portrait", "woman", "man", "girl", "boy", "child", "selfie", "bathroom", "car", "machine", "computer"),
                false
            )
            word.subtopicId in setOf("travel_transport", "travel_city_nav", "travel_vacation") -> Triple(
                setOf("transport", "vehicle", "travel", "road", "railway", "aviation", "street", "bridge", "vacation", "luggage", "journey", "tourism"),
                setOf("selfie", "baby", "cat", "dog", "food"),
                false
            )
            word.subtopicId in setOf("shopping_clothing", "shopping_shoes", "people_appearance", "shopping_materials") -> Triple(
                setOf("clothing", "fashion", "apparel", "wear", "garment", "shoes", "footwear", "fabric", "textile", "style", "outfit", "accessory"),
                setOf("landscape", "animal", "food", "car", "engine", "motor"),
                false
            )
            word.subtopicId in setOf("shopping_money", "city_services", "work_business") -> Triple(
                setOf("money", "euro", "finance", "bank", "payment", "cash", "card", "receipt", "economy", "mail", "post", "service", "business", "contract"),
                setOf("animal", "pet", "food", "flower", "tree", "forest"),
                false
            )
            word.categoryId == "tech" -> Triple(
                setOf("technology", "computer", "digital", "device", "electronics", "hardware", "screen", "internet", "science", "laboratory", "code", "modern", "server", "network"),
                setOf("flower", "food", "baking", "chocolate", "cookie", "pet", "animal", "tree", "forest", "wood", "harbor", "ship", "vintage"),
                false
            )
            word.categoryId == "education" -> Triple(
                setOf("education", "school", "study", "university", "book", "books", "classroom", "stationery", "desk", "learning", "exam", "writing"),
                setOf("beach", "party", "alcohol", "animal", "king", "crown", "monarch"),
                false
            )
            word.categoryId == "body" -> Triple(
                setOf("health", "medical", "medicine", "doctor", "care", "body", "fitness", "wellness", "hygiene", "sport", "clean", "hospital"),
                setOf("horror", "zombie", "skull", "blood", "weapon", "ipad", "computer", "laptop", "snow", "winter", "lumber", "wood", "cannabis", "marijuana", "smoke"),
                false
            )
            word.subtopicId in setOf("sports_games", "music_instruments", "outdoor_activities", "gaming_entertainment") -> Triple(
                setOf("sport", "sports", "game", "music", "instrument", "concert", "outdoor", "hiking", "camping", "adventure", "gaming", "cinema", "ball", "stage", "theater"),
                setOf("office", "medical", "pills", "carpenter", "tool"),
                false
            )
            word.subtopicId in setOf("city_jobs", "people_family", "people_relationships", "work_career", "work_trades") -> Triple(
                setOf("person", "people", "portrait", "worker", "professional", "uniform", "doctor", "teacher", "chef", "family", "friends", "team", "group"),
                setOf("empty", "building", "landscape", "abstract", "golf", "wood", "surfboard"),
                false
            )
            word.categoryId == "emotions" || word.subtopicId in setOf("people_emotions", "people_character") -> Triple(
                setOf("person", "people", "face", "emotion", "expression", "feeling", "human", "smile", "hands", "portrait", "lifestyle"),
                setOf("car", "engine", "machine", "factory", "logo", "icon"),
                false
            )
            word.categoryId == "society" -> Triple(
                setOf("society", "culture", "city", "people", "history", "building", "architecture", "media", "festival", "celebration", "church", "temple"),
                setOf("selfie", "car", "engine", "food"),
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
            word.subtopicId == "adj_qualities" -> Triple(
                setOf("hand", "gesture", "object", "closeup", "concept", "symbol", "lifestyle", "clean", "modern", "daily"),
                setOf("statue", "monument", "memorial", "horse", "spa", "bridge", "flag", "church", "cathedral", "war"),
                false
            )
            word.subtopicId == "adj_personality" -> Triple(
                setOf("person", "people", "face", "expression", "smile", "emotion", "friends", "human", "portrait"),
                setOf("statue", "monument", "building", "landscape", "car", "logo"),
                false
            )
            word.subtopicId in setOf("adj_size_time", "adj_sensory") -> Triple(
                setOf("nature", "weather", "closeup", "texture", "macro", "outdoor", "water", "light", "road", "clock"),
                setOf("statue", "monument", "logo", "selfie", "flag"),
                false
            )
            else -> Triple(
                emptySet(),
                setOf("car", "engine", "motor", "logo", "icon"),
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
