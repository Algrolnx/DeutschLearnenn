package com.example.data.model

import androidx.compose.ui.graphics.Color

data class Category(
    val id: String,
    val titleDe: String,
    val titleUk: String,
    val titleEn: String,
    val description: String,
    val iconKey: String,
    val color: Color,
    val level: String = "A1"
)

object CategoryData {
    val categories = listOf(
        Category(
            id = "food",
            titleDe = "Essen & Trinken",
            titleUk = "Їжа та напої",
            titleEn = "Food & Drinks",
            description = "Früchte, Gemüse, Mahlzeiten und Getränke",
            iconKey = "food",
            color = Color(0xFFF97316), // Orange
            level = "A1"
        ),
        Category(
            id = "travel",
            titleDe = "Reisen & Verkehr",
            titleUk = "Подорожі та транспорт",
            titleEn = "Travel & Transport",
            description = "Flughafen, Zug, Hotel und Orientierung",
            iconKey = "travel",
            color = Color(0xFF0284C7), // Sky Blue
            level = "A1"
        ),
        Category(
            id = "home",
            titleDe = "Alltag & Zuhause",
            titleUk = "Дім та побут",
            titleEn = "Home & Daily Life",
            description = "Möbel, Zimmer und Haushaltsgegenstände",
            iconKey = "home",
            color = Color(0xFF10B981), // Emerald Green
            level = "A1"
        ),
        Category(
            id = "people",
            titleDe = "Menschen & Familie",
            titleUk = "Люди та сім'я",
            titleEn = "People & Family",
            description = "Verwandte, Gefühle und Beziehungen",
            iconKey = "people",
            color = Color(0xFF8B5CF6), // Purple
            level = "A1"
        ),
        Category(
            id = "city",
            titleDe = "Stadt & Berufe",
            titleUk = "Місто та професії",
            titleEn = "City & Occupations",
            description = "Arbeit, Gebäude, Geschäfte und Orte",
            iconKey = "city",
            color = Color(0xFF2563EB), // Indigo Blue
            level = "A2"
        ),
        Category(
            id = "nature",
            titleDe = "Natur & Wetter",
            titleUk = "Природа та погода",
            titleEn = "Nature & Weather",
            description = "Tiere, Pflanzen, Jahreszeiten und Klima",
            iconKey = "nature",
            color = Color(0xFF059669), // Green
            level = "A2"
        ),
        Category(
            id = "body",
            titleDe = "Körper & Gesundheit",
            titleUk = "Тіло та здоров'я",
            titleEn = "Body & Health",
            description = "Körperteile, Medizin und Wohlbefinden",
            iconKey = "body",
            color = Color(0xFFE11D48), // Rose
            level = "B1"
        ),
        Category(
            id = "hobbies",
            titleDe = "Freizeit & Hobbys",
            titleUk = "Дозвілля та хобі",
            titleEn = "Leisure & Hobbies",
            description = "Sport, Musik, Kunst und Unterhaltung",
            iconKey = "hobbies",
            color = Color(0xFFD97706), // Amber
            level = "B1"
        )
    )
}
