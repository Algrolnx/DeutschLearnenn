package com.example.data.model

import androidx.compose.ui.graphics.Color

data class Subtopic(
    val id: String,
    val mainTopicId: String,
    val titleDe: String,
    val titleUk: String,
    val titleEn: String,
    val description: String,
    val iconKey: String,
    val level: String = "A1"
)

data class MainTopic(
    val id: String,
    val titleDe: String,
    val titleUk: String,
    val titleEn: String,
    val iconKey: String,
    val color: Color,
    val containerColor: Color,
    val subtopics: List<Subtopic>
)

object TopicCatalog {

    val mainTopics = listOf(
        // === 1. FOOD & DRINKS ===
        MainTopic(
            id = "food",
            titleDe = "Essen & Trinken",
            titleUk = "Їжа та напої",
            titleEn = "Food & Drinks",
            iconKey = "food",
            color = Color(0xFFEA580C),
            containerColor = Color(0xFFFFEDD5),
            subtopics = listOf(
                Subtopic("food_fruits", "food", "Früchte & Beeren", "Фрукти та ягоди", "Fruits & Berries", "Apfel, Banane, Erdbeere, Zitrone, Kirsche, Pfirsich", "fruit", "A1"),
                Subtopic("food_vegetables", "food", "Gemüse & Salate", "Овочі та салати", "Vegetables", "Tomate, Gurke, Kartoffel, Zwiebel, Karotte, Knoblauch", "vegetable", "A1"),
                Subtopic("food_drinks", "food", "Getränke", "Напої", "Beverages", "Kaffee, Tee, Wasser, Milch, Saft, Bier, Wein", "drink", "A1"),
                Subtopic("food_meals", "food", "Mahlzeiten & Brot", "Страви та хліб", "Meals & Bread", "Brot, Käse, Suppe, Butter, Ei, Reis, Nudeln", "meal", "A1"),
                Subtopic("food_meat_fish", "food", "Fleisch & Fisch", "М'ясо та риба", "Meat & Fish", "Steak, Wurst, Schinken, Lachs, Forelle, Hähnchen", "meat", "A2"),
                Subtopic("food_sweets", "food", "Süßigkeiten & Desserts", "Солодощі та десерти", "Sweets & Desserts", "Schokolade, Kuchen, Torte, Eis, Honig, Keks", "sweets", "A2"),
                Subtopic("food_restaurant", "food", "Im Restaurant", "У ресторані", "At the Restaurant", "Speisekarte, Kellner, Rechnung, Trinkgeld, Besteck", "restaurant", "A2"),
                Subtopic("food_cooking", "food", "Kochen & Backen", "Кулінарія та випічка", "Cooking & Baking", "Topf, Pfanne, Ofen, Rezept, Gewürz, Öl, Salz", "cooking", "A2")
            )
        ),

        // === 2. TRAVEL & TRANSPORT ===
        MainTopic(
            id = "travel",
            titleDe = "Reisen & Verkehr",
            titleUk = "Подорожі та транспорт",
            titleEn = "Travel & Transport",
            iconKey = "travel",
            color = Color(0xFF0284C7),
            containerColor = Color(0xFFE0F2FE),
            subtopics = listOf(
                Subtopic("travel_transport", "travel", "Verkehrsmittel", "Види транспорту", "Means of Transport", "Zug, Flugzeug, Bus, Straßenbahn, Fahrrad, Schiff, U-Bahn", "train", "A1"),
                Subtopic("travel_station", "travel", "Bahnhof & Flughafen", "Вокзал та аеропорт", "Station & Airport", "Bahnsteig, Fahrkarte, Gleis, Abflug, Gate, Gepäckausgabe", "airplane", "A1"),
                Subtopic("travel_hotel", "travel", "Hotel & Unterkunft", "Готель та проживання", "Hotel & Stay", "Hotelzimmer, Schlüssel, Rezeption, Reservierung, Balkon", "hotel", "A2"),
                Subtopic("travel_city_nav", "travel", "Orientierung & Weg", "Орієнтація та напрямки", "Directions & Way", "Stadtplan, Kreuzung, Ampel, Abbiegen, Norden, Brücke", "navigation", "A2"),
                Subtopic("travel_vacation", "travel", "Urlaub & Ferien", "Відпустка та відпочинок", "Vacation & Holiday", "Koffer, Strand, Reisepass, Souvenir, Kamera, Sonnenbrille", "vacation", "A1")
            )
        ),

        // === 3. HOME & LIVING ===
        MainTopic(
            id = "home",
            titleDe = "Wohnen & Haushalt",
            titleUk = "Дім та побут",
            titleEn = "Home & Living",
            iconKey = "home",
            color = Color(0xFF16A34A),
            containerColor = Color(0xFFDCFCE7),
            subtopics = listOf(
                Subtopic("home_rooms", "home", "Zimmer & Räume", "Кімнати та приміщення", "Rooms & Spaces", "Wohnzimmer, Schlafzimmer, Küche, Bad, Flur, Balkon, Keller", "rooms", "A1"),
                Subtopic("home_furniture", "home", "Möbel & Einrichtung", "Меблі та інтер'єр", "Furniture", "Tisch, Stuhl, Bett, Schrank, Sofa, Teppich, Regal", "furniture", "A1"),
                Subtopic("home_kitchen", "home", "Küche & Geschirr", "Кухня та посуд", "Kitchen & Dishes", "Teller, Tasse, Glas, Gabel, Löffel, Messer, Schüssel", "kitchen", "A1"),
                Subtopic("home_bathroom", "home", "Bad & Hygiene", "Ванна кімната", "Bathroom & Hygiene", "Dusche, Badewanne, Seife, Handtuch, Zahnbürste, Spiegel", "bathroom", "A1"),
                Subtopic("home_appliances", "home", "Haushaltsgeräte", "Побутова техніка", "Appliances", "Kühlschrank, Waschmaschine, Herd, Staubsauger, Mikrowelle", "appliances", "A2")
            )
        ),

        // === 4. PEOPLE & FAMILY ===
        MainTopic(
            id = "people",
            titleDe = "Menschen & Familie",
            titleUk = "Люди та родина",
            titleEn = "People & Family",
            iconKey = "people",
            color = Color(0xFF9333EA),
            containerColor = Color(0xFFF3E8FF),
            subtopics = listOf(
                Subtopic("people_family", "people", "Familie & Verwandte", "Сім'я та родичі", "Family Members", "Mutter, Vater, Schwester, Bruder, Kind, Oma, Opa, Tante", "family", "A1"),
                Subtopic("people_appearance", "people", "Aussehen & Stil", "Зовнішність та стиль", "Appearance & Style", "Hemd, Hose, Kleid, Augenfarbe, Haarfarbe, Größe, Stil", "clothes", "A1"),
                Subtopic("people_character", "people", "Charakter & Wesen", "Характер та риси", "Character Traits", "Charakter, Ehrlichkeit, Höflichkeit, Humor, Freundlichkeit, Mut", "personality", "A2"),
                Subtopic("people_relationships", "people", "Freunde & Beziehungen", "Друзі та стосунки", "Relationships", "Bester Freund, Partner, Kollege, Nachbar, Vertrauen", "social", "A2"),
                Subtopic("people_emotions", "people", "Gefühle & Bindung", "Почуття та емоції", "Feelings & Bonding", "Liebe, Zuneigung, Freude, Treue, Mitgefühl, Herzlichkeit", "heart", "A2")
            )
        ),

        // === 5. CITY & PLACES ===
        MainTopic(
            id = "city",
            titleDe = "Stadt & Infrastruktur",
            titleUk = "Місто та інфраструктура",
            titleEn = "City & Places",
            iconKey = "city",
            color = Color(0xFF2563EB),
            containerColor = Color(0xFFDBEAFE),
            subtopics = listOf(
                Subtopic("city_places", "city", "Orte & Gebäude", "Місця та споруди", "City Places", "Rathaus, Museum, Theater, Park, Bibliothek, Kirche, Turm", "building", "A1"),
                Subtopic("city_jobs", "city", "Berufe in der Stadt", "Міські професії", "Urban Professions", "Polizist, Arzt, Lehrer, Busfahrer, Verkäufer, Feuerwehrmann", "briefcase", "A2"),
                Subtopic("city_shopping", "city", "Einkaufen & Kasse", "Покупки та каса", "Shopping & Checkout", "Geschäft, Supermarkt, Kasse, Preis, Rabatt, Einkaufswagen, Kassenbon", "shopping", "A1"),
                Subtopic("city_services", "city", "Dienstleistungen & Bank", "Служби та банк", "Public Services", "Bank, Post, Geldautomat, Bürgeramt, Haltestelle, Paket", "services", "A2"),
                Subtopic("city_emergency", "city", "Notdienste & Sicherheit", "Екстрені служби", "Emergency Services", "Krankenhaus, Feuerwehr, Polizei, Notruf, Rettungswagen", "emergency", "A2")
            )
        ),

        // === 6. NATURE & ENVIRONMENT ===
        MainTopic(
            id = "nature",
            titleDe = "Natur & Umwelt",
            titleUk = "Природа та довкілля",
            titleEn = "Nature & Environment",
            iconKey = "nature",
            color = Color(0xFF0D9488),
            containerColor = Color(0xFFCCFBF1),
            subtopics = listOf(
                Subtopic("nature_weather", "nature", "Wetter & Klima", "Погода та клімат", "Weather & Climate", "Sonne, Regen, Schnee, Wind, Wolke, Gewitter, Temperatur", "weather", "A1"),
                Subtopic("nature_animals", "nature", "Haustiere & Hof", "Домашні та свійські тварини", "Pets & Farm Animals", "Hund, Katze, Vogel, Pferd, Kuh, Schaf, Ziege, Kaninchen", "animal", "A1"),
                Subtopic("nature_plants", "nature", "Pflanzen & Blumen", "Рослини та квіти", "Plants & Flowers", "Baum, Blume, Rose, Wald, Blatt, Gras, Tulpe, Sonnenblume", "plant", "A1"),
                Subtopic("nature_landscape", "nature", "Landschaften & Gewässer", "Ландшафти та водойми", "Landscapes & Waters", "Berg, Fluss, See, Wald, Wiese, Tal, Insel, Wasserfall", "landscape", "A2"),
                Subtopic("nature_wildlife", "nature", "Wildtiere & Waldtiere", "Дикі тварини та птахи", "Wild Animals & Wildlife", "Bär, Wolf, Fuchs, Reh, Hirsch, Eichhörnchen, Igel, Adler", "animal", "A2")
            )
        ),

        // === 7. BODY & HEALTH ===
        MainTopic(
            id = "body",
            titleDe = "Körper & Gesundheit",
            titleUk = "Тіло та здоров'я",
            titleEn = "Body & Health",
            iconKey = "body",
            color = Color(0xFFE11D48),
            containerColor = Color(0xFFFFE4E6),
            subtopics = listOf(
                Subtopic("body_parts", "body", "Körperteile", "Частини тіла", "Body Parts", "Kopf, Auge, Ohr, Nase, Mund, Hand, Arm, Bein, Fuß, Herz", "body_parts", "A1"),
                Subtopic("health_illness", "body", "Krankheiten & Symptome", "Хвороби та симптоми", "Health & Illness", "Fieber, Husten, Schnupfen, Schmerz, Grippe, Allergie, Wunde", "health", "A2"),
                Subtopic("medicine_care", "body", "Medizin & Ärzte", "Медицина та лікування", "Medicine & Care", "Arzt, Apotheke, Medikament, Tablette, Rezept, Verband", "doctor", "A2"),
                Subtopic("appearance_hygiene", "body", "Pflege & Hygiene", "Догляд та гігієна", "Care & Hygiene", "Seife, Shampoo, Zahnpasta, Bürste, Handtuch, Creme, Rasur", "hygiene", "A1"),
                Subtopic("fitness_wellness", "body", "Fitness & Wohlbefinden", "Фітнес та велнес", "Fitness & Wellness", "Training, Yoga, Ausdauer, Laufen, Massage, Entspannung", "fitness", "A2")
            )
        ),

        // === 8. HOBBIES & LEISURE ===
        MainTopic(
            id = "hobbies",
            titleDe = "Freizeit & Hobbys",
            titleUk = "Дозвілля та хобі",
            titleEn = "Leisure & Hobbies",
            iconKey = "hobbies",
            color = Color(0xFFD97706),
            containerColor = Color(0xFFFEF3C7),
            subtopics = listOf(
                Subtopic("sports_games", "hobbies", "Sport & Wettkampf", "Спорт та змагання", "Sports & Games", "Fußball, Basketball, Tennis, Schwimmen, Laufen, Pokal", "sport", "A1"),
                Subtopic("music_instruments", "hobbies", "Musik & Instrumente", "Музика та інструменти", "Music & Instruments", "Gitarre, Klavier, Violine, Schlagzeug, Trompete, Lied", "music", "A1"),
                Subtopic("art_creativity", "hobbies", "Kunst & Kreativität", "Мистецтво та творчість", "Art & Creativity", "Malerei, Zeichnung, Pinsel, Leinwand, Skulptur, Galerie", "art", "A2"),
                Subtopic("outdoor_activities", "hobbies", "Outdoor & Abenteuer", "Активний відпочинок", "Outdoor Activities", "Wandern, Zelten, Klettern, Angeln, Lagerfeuer, Picknick", "outdoor", "A2"),
                Subtopic("gaming_entertainment", "hobbies", "Gaming & Kino", "Геймінг та розваги", "Gaming & Entertainment", "Computerspiel, Konsole, Kino, Theater, Film, Popcorn", "games", "A1")
            )
        ),

        // === 9. WORK & PROFESSION ===
        MainTopic(
            id = "work",
            titleDe = "Arbeit & Beruf",
            titleUk = "Робота та кар'єра",
            titleEn = "Work & Career",
            iconKey = "work",
            color = Color(0xFF475569),
            containerColor = Color(0xFFF1F5F9),
            subtopics = listOf(
                Subtopic("work_office", "work", "Büro & Arbeitsplatz", "Офіс та робоче місце", "Office & Workplace", "Schreibtisch, Bürostuhl, Drucker, Ordner, Kollege, Pause", "office", "A1"),
                Subtopic("work_business", "work", "Geschäft & Verhandlung", "Бізнес та переговори", "Business & Commerce", "Meeting, Vertrag, Kunde, Projekt, Budget, Rechnung, Erfolg", "business", "A2"),
                Subtopic("work_trades", "work", "Handwerk & Berufe", "Ремесла та фахівці", "Trades & Crafts", "Meister, Schreiner, Elektriker, Bäcker, Koch, Mechaniker", "trades", "A2"),
                Subtopic("work_career", "work", "Karriere & Bewerbung", "Кар'єра та пошук роботи", "Career & Employment", "Bewerbung, Lebenslauf, Gehalt, Beförderung, Vorstellungsgespräch", "career", "A2"),
                Subtopic("work_tools", "work", "Werkzeuge & Bau", "Інструменти та будівництво", "Tools & Construction", "Hammer, Schraubenzieher, Bohrmaschine, Säge, Leiter, Zange", "tools", "A2")
            )
        ),

        // === 10. TECHNOLOGY & DIGITAL ===
        MainTopic(
            id = "tech",
            titleDe = "Technik & Digitales",
            titleUk = "Технології та IT",
            titleEn = "Tech & Digital",
            iconKey = "tech",
            color = Color(0xFF6366F1),
            containerColor = Color(0xFFEEF2FF),
            subtopics = listOf(
                Subtopic("tech_computers", "tech", "Computer & Hardware", "Комп'ютери та залізо", "Computers & Hardware", "Laptop, Monitor, Tastatur, Maus, Festplatte, Prozessor, Akku", "computer", "A1"),
                Subtopic("tech_internet", "tech", "Internet & Netzwerk", "Інтернет та мережі", "Internet & Network", "WLAN, Browser, Website, E-Mail, Passwort, Router, Download", "internet", "A1"),
                Subtopic("tech_devices", "tech", "Smartphones & Gadgets", "Смартфони та гаджети", "Smartphones & Devices", "Smartphone, Tablet, Smartwatch, Kopfhörer, Kamera, Drohne", "devices", "A1"),
                Subtopic("tech_science", "tech", "Wissenschaft & Forschung", "Наука та дослідження", "Science & Research", "Labor, Experiment, Entdeckung, Teleskop, Physik, Chemie", "science", "A2"),
                Subtopic("tech_data", "tech", "Software & Daten", "Програмування та дані", "Software & Code", "Künstliche Intelligenz, Algorithmus, Code, Datenbank, App", "data", "A2")
            )
        ),

        // === 11. EDUCATION & STUDY ===
        MainTopic(
            id = "education",
            titleDe = "Bildung & Sprachen",
            titleUk = "Освіта та мови",
            titleEn = "Education & Study",
            iconKey = "education",
            color = Color(0xFF8B5CF6),
            containerColor = Color(0xFFF5F3FF),
            subtopics = listOf(
                Subtopic("education_school", "education", "Schule & Fächer", "Школа та предмети", "School & Subjects", "Klassenzimmer, Lehrer, Schüler, Mathematik, Deutsch, Pause", "school", "A1"),
                Subtopic("education_university", "education", "Universität & Studium", "Університет та наука", "University & Higher Ed", "Hörsaal, Professor, Vorlesung, Semester, Campus, Mensa", "university", "A2"),
                Subtopic("education_supplies", "education", "Schreibwaren & Bücher", "Канцтовари та книги", "Supplies & Stationery", "Heft, Stift, Füller, Lineal, Radiergummi, Mäppchen, Buch", "supplies", "A1"),
                Subtopic("education_languages", "education", "Sprachen & Grammatik", "Мови та граматика", "Languages & Grammar", "Wortschatz, Aussprache, Vokabel, Satz, Verb, Artikel, Regel", "languages", "A1"),
                Subtopic("education_exams", "education", "Prüfungen & Abschlüsse", "Іспити та оцінки", "Exams & Degrees", "Klausur, Note, Zeugnis, Diplom, Vorbereitung, Lösung", "exams", "A2")
            )
        ),

        // === 12. SHOPPING & FASHION ===
        MainTopic(
            id = "shopping",
            titleDe = "Einkaufen & Mode",
            titleUk = "Покупки та мода",
            titleEn = "Shopping & Fashion",
            iconKey = "shopping_main",
            color = Color(0xFFEC4899),
            containerColor = Color(0xFFFCE7F3),
            subtopics = listOf(
                Subtopic("shopping_clothing", "shopping", "Kleidung & Mode", "Одяг та вбрання", "Clothing & Apparel", "Hemd, Hose, Pullover, Jacke, Kleid, Mantel, T-Shirt, Rock", "clothing", "A1"),
                Subtopic("shopping_shoes", "shopping", "Schuhe & Accessoires", "Взуття та аксесуари", "Shoes & Accessories", "Sneaker, Stiefel, Sandale, Handtasche, Gürtel, Sonnenbrille", "shoes", "A1"),
                Subtopic("shopping_stores", "shopping", "Geschäfte & Shopping", "Магазини та центри", "Stores & Malls", "Einkaufszentrum, Boutique, Kasse, Regal, Schaufenster", "stores", "A1"),
                Subtopic("shopping_money", "shopping", "Geld & Bezahlung", "Гроші та оплата", "Money & Payment", "Euro, Bargeld, Kreditkarte, Münze, Schein, Rabatt, Preis", "money", "A1"),
                Subtopic("shopping_materials", "shopping", "Stoffe & Materialien", "Тканини та матеріали", "Fabrics & Materials", "Baumwolle, Wolle, Seide, Leder, Leinen, Reißverschluss", "materials", "A2")
            )
        ),

        // === 13. SOCIETY & CULTURE ===
        MainTopic(
            id = "society",
            titleDe = "Gesellschaft & Kultur",
            titleUk = "Суспільство та культура",
            titleEn = "Society & Culture",
            iconKey = "society",
            color = Color(0xFF0F766E),
            containerColor = Color(0xFFCCFBF1),
            subtopics = listOf(
                Subtopic("society_festivals", "society", "Feste & Bräuche", "Свята та традиції", "Festivals & Traditions", "Weihnachten, Ostern, Silvester, Oktoberfest, Geburtstag", "festivals", "A1"),
                Subtopic("society_politics", "society", "Demokratie & Staat", "Демократія та держава", "Politics & Law", "Bundestag, Kanzler, Wahl, Gesetz, Bürger, Recht, Freiheit", "politics", "A2"),
                Subtopic("society_media", "society", "Medien & Nachrichten", "Медіа та новини", "Media & News", "Zeitung, Fernsehen, Radio, Interview, Artikel, Internet", "media", "A1"),
                Subtopic("society_religion", "society", "Glaube & Kirchen", "Віра та храми", "Faith & Beliefs", "Kirche, Dom, Gebet, Bibel, Glocke, Frieden, Hoffnung", "religion", "A2"),
                Subtopic("society_history", "society", "Geschichte & Epochen", "Історія та епохи", "History & Heritage", "Schloss, Burg, Ritter, Mauer, Denkmal, Jahrhundert, Museum", "history", "A2")
            )
        ),

        // === 14. EMOTIONS & PERSONALITY ===
        MainTopic(
            id = "emotions",
            titleDe = "Gefühle & Charakter",
            titleUk = "Емоції та характер",
            titleEn = "Emotions & Personality",
            iconKey = "emotions",
            color = Color(0xFFF43F5E),
            containerColor = Color(0xFFFFE4E6),
            subtopics = listOf(
                Subtopic("emotions_positive", "emotions", "Freude & Glück", "Радість та щастя", "Positive Emotions", "Freude, Glück, Liebe, Lachen, Hoffnung, Begeisterung, Stolz", "positive", "A1"),
                Subtopic("emotions_negative", "emotions", "Sorgen & Ängste", "Смуток та тривоги", "Difficult Emotions", "Angst, Wut, Ärger, Trauer, Sorge, Enttäuschung, Stress", "negative", "A1"),
                Subtopic("emotions_traits", "emotions", "Charakterzüge", "Риси особистості", "Character Traits", "Freundlichkeit, Ehrlichkeit, Geduld, Mut, Fleiß, Humor", "traits", "A2"),
                Subtopic("emotions_mind", "emotions", "Geist & Gedanken", "Розум та мислення", "Mind & Thoughts", "Gedanke, Idee, Verstand, Traum, Erinnerung, Phantasie", "mind", "A2"),
                Subtopic("emotions_communication", "emotions", "Dialog & Austausch", "Спілкування та діалог", "Social Interaction", "Gespräch, Lob, Ratschlag, Kompliment, Einigung, Respekt", "communication", "A2")
            )
        ),

        // === 15. KEY VERBS ===
        MainTopic(
            id = "verbs",
            titleDe = "Wichtige Verben",
            titleUk = "Ключові дієслова",
            titleEn = "Key Verbs",
            iconKey = "verbs",
            color = Color(0xFF059669),
            containerColor = Color(0xFFD1FAE5),
            subtopics = listOf(
                Subtopic("verbs_movement", "verbs", "Bewegung & Reisen", "Рух та переміщення", "Movement & Travel", "gehen, laufen, fahren, fliegen, schwimmen, springen, reisen", "movement", "A1"),
                Subtopic("verbs_communication", "verbs", "Sprechen & Austausch", "Мовлення та контакт", "Speaking & Dialogue", "sprechen, sagen, fragen, antworten, rufen, erklären, danken", "speaking", "A1"),
                Subtopic("verbs_daily", "verbs", "Alltag & Haushalt", "Щоденні справи", "Daily Routines", "aufstehen, schlafen, essen, trinken, kochen, waschen, putzen", "daily", "A1"),
                Subtopic("verbs_mind", "verbs", "Denken & Fühlen", "Мислення та почуття", "Thinking & Feeling", "denken, wissen, verstehen, erinnern, hoffen, lieben, freuen", "thinking", "A1"),
                Subtopic("verbs_change", "verbs", "Veränderung & Tun", "Зміни та процеси", "Change & Actions", "werden, beginnen, enden, bauen, reparieren, finden, wachsen", "actions", "A2")
            )
        ),

        // === 16. ESSENTIAL ADJECTIVES ===
        MainTopic(
            id = "adjectives",
            titleDe = "Wichtige Adjektive",
            titleUk = "Основні прикметники",
            titleEn = "Essential Adjectives",
            iconKey = "adjectives",
            color = Color(0xFF7C3AED),
            containerColor = Color(0xFFEDE9FE),
            subtopics = listOf(
                Subtopic("adj_qualities", "adjectives", "Qualität & Wert", "Якість та властивості", "Qualities & Value", "gut, schlecht, toll, wichtig, einfach, schwer, neu, alt, sauber", "qualities", "A1"),
                Subtopic("adj_colors", "adjectives", "Farben & Licht", "Кольори та відтінки", "Colors & Light", "rot, blau, grün, gelb, schwarz, weiß, grau, bunt, hell, dunkel", "colors", "A1"),
                Subtopic("adj_size_time", "adjectives", "Größe & Zeit", "Розмір та час", "Size, Space & Time", "groß, klein, lang, kurz, breit, hoch, nah, fern, früh, spät", "size", "A1"),
                Subtopic("adj_sensory", "adjectives", "Sinne & Geschmack", "Відчуття та смаки", "Sensory & Taste", "warm, kalt, heiß, süß, sauer, salzig, lecker, frisch, weich", "sensory", "A1"),
                Subtopic("adj_personality", "adjectives", "Mensch & Charakter", "Характер та риси", "Character & Mood", "freundlich, nett, lieb, ehrlich, geduldig, mutig, klug, ruhig", "mood", "A1")
            )
        )
    )

    fun findSubtopicById(subtopicId: String): Subtopic? {
        return mainTopics.flatMap { it.subtopics }.firstOrNull { it.id == subtopicId }
    }

    fun findMainTopicById(mainTopicId: String): MainTopic? {
        return mainTopics.firstOrNull { it.id == mainTopicId }
    }
}
