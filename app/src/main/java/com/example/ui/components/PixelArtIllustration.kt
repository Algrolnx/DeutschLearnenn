package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.WordEntity
import com.example.ui.theme.LocalAppExtendedColors
import kotlin.math.abs

enum class PixelSpriteId {
    APPLE,
    BREAD,
    COFFEE,
    CAKE,
    MEAT,
    CARROT,
    DRINK,
    CAR,
    TRAIN,
    PLANE,
    KEY,
    SUITCASE,
    MAP,
    HOUSE,
    BED,
    CHAIR,
    HEART,
    PERSON,
    DOCTOR,
    TREE,
    SUN,
    CAT,
    MOUNTAIN,
    CITY,
    COIN,
    SHIRT,
    BRIEFCASE,
    LAPTOP,
    PHONE,
    BOOK,
    MUSIC,
    SPORT,
    GLOBE,
    CLOCK,
    LIGHTNING,
    PALETTE,
    SHOPPING_BAG
}

private data class PixelSpriteSpec(
    val rows: List<String>,
    val palette: Map<Char, Color>,
    val bgTint: Color
)

/**
 * Crisp retro-craft Canvas Pixel-Art Sprite Engine.
 * Draws bespoke integer-aligned pixel illustrations matched to German words & subtopics
 * with zero network calls and instant 60 FPS Compose rendering.
 */
@Composable
fun PixelArtIllustration(
    word: WordEntity,
    modifier: Modifier = Modifier,
    innerPadding: Dp = 8.dp
) {
    val extended = LocalAppExtendedColors.current
    val spriteId = remember(word.german, word.subtopicId, word.categoryId) {
        resolveSpriteForWord(word)
    }
    val spec = remember(spriteId) { buildSpriteSpec(spriteId) }
    val seed = remember(word.german) { abs(word.german.hashCode()) }

    val stageBg = remember(spec.bgTint, extended.isDark, extended.pixelStageBg) {
        if (extended.isDark) {
            extended.pixelStageBg
        } else {
            spec.bgTint.copy(alpha = 0.28f)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(stageBg)
            .border(1.dp, extended.pixelStageBorder, RoundedCornerShape(14.dp))
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridH = spec.rows.size
            val gridW = spec.rows.maxOfOrNull { it.length } ?: 12
            val cellSize = minOf(size.width / gridW, size.height / gridH)
            val totalW = cellSize * gridW
            val totalH = cellSize * gridH
            val originX = (size.width - totalW) * 0.5f
            val originY = (size.height - totalH) * 0.5f

            // Subtle pixel corner framing dots matching the word hash (tactile retro detail)
            val dotColor = if (extended.isDark) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color.Black.copy(alpha = 0.06f)
            }
            val dotSize = Size(cellSize * 0.7f, cellSize * 0.7f)
            if (seed % 2 == 0) {
                drawRect(dotColor, Offset(4f, 4f), dotSize)
                drawRect(dotColor, Offset(size.width - dotSize.width - 4f, size.height - dotSize.height - 4f), dotSize)
            } else {
                drawRect(dotColor, Offset(size.width - dotSize.width - 4f, 4f), dotSize)
                drawRect(dotColor, Offset(4f, size.height - dotSize.height - 4f), dotSize)
            }

            // Grounded pixel shadow under sprite
            drawRect(
                color = Color.Black.copy(alpha = if (extended.isDark) 0.35f else 0.10f),
                topLeft = Offset(originX + cellSize * 2f, originY + totalH - cellSize * 0.85f),
                size = Size(totalW - cellSize * 4f, cellSize * 0.85f)
            )

            // Draw pixel art matrix with tiny +0.4f overlap to avoid hairline seams
            val pixelSize = Size(cellSize + 0.4f, cellSize + 0.4f)
            for (y in 0 until gridH) {
                val row = spec.rows[y]
                for (x in row.indices) {
                    val ch = row[x]
                    if (ch == '.') continue
                    val color = spec.palette[ch] ?: continue
                    drawRect(
                        color = color,
                        topLeft = Offset(originX + x * cellSize, originY + y * cellSize),
                        size = pixelSize
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Canvas Pixel-Art Sprite Icon for Main Topics & Subtopics.
 * Replaces generic Material Icons on topic rows (zero emojis).
 */
@Composable
fun TopicPixelIcon(
    iconKey: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    innerPadding: Dp = 8.dp
) {
    val spriteId = remember(iconKey) { resolveSpriteForTopic(iconKey) }
    val spec = remember(spriteId) { buildSpriteSpec(spriteId) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridH = spec.rows.size
            val gridW = spec.rows.maxOfOrNull { it.length } ?: 12
            val cellSize = minOf(size.width / gridW, size.height / gridH)
            val totalW = cellSize * gridW
            val totalH = cellSize * gridH
            val originX = (size.width - totalW) * 0.5f
            val originY = (size.height - totalH) * 0.5f
            val pixelSize = Size(cellSize + 0.4f, cellSize + 0.4f)

            for (y in 0 until gridH) {
                val row = spec.rows[y]
                for (x in row.indices) {
                    val ch = row[x]
                    if (ch == '.') continue
                    val color = spec.palette[ch] ?: continue
                    drawRect(
                        color = color,
                        topLeft = Offset(originX + x * cellSize, originY + y * cellSize),
                        size = pixelSize
                    )
                }
            }
        }
    }
}

fun resolveSpriteForTopic(iconKey: String): PixelSpriteId {
    return when (iconKey.lowercase()) {
        // 16 Main Topics
        "food" -> PixelSpriteId.APPLE
        "travel" -> PixelSpriteId.TRAIN
        "home" -> PixelSpriteId.HOUSE
        "people" -> PixelSpriteId.PERSON
        "city" -> PixelSpriteId.CITY
        "nature" -> PixelSpriteId.TREE
        "body" -> PixelSpriteId.DOCTOR
        "hobbies" -> PixelSpriteId.SPORT
        "work" -> PixelSpriteId.BRIEFCASE
        "tech" -> PixelSpriteId.LAPTOP
        "education" -> PixelSpriteId.BOOK
        "shopping_main", "shopping" -> PixelSpriteId.SHOPPING_BAG
        "society" -> PixelSpriteId.GLOBE
        "emotions" -> PixelSpriteId.HEART
        "verbs" -> PixelSpriteId.LIGHTNING
        "adjectives" -> PixelSpriteId.PALETTE

        // Subtopic iconKeys
        "fruit" -> PixelSpriteId.APPLE
        "vegetable" -> PixelSpriteId.CARROT
        "drink" -> PixelSpriteId.COFFEE
        "meal" -> PixelSpriteId.BREAD
        "meat" -> PixelSpriteId.MEAT
        "sweets" -> PixelSpriteId.CAKE
        "restaurant" -> PixelSpriteId.DRINK
        "cooking" -> PixelSpriteId.BREAD
        "train" -> PixelSpriteId.TRAIN
        "airplane" -> PixelSpriteId.PLANE
        "hotel" -> PixelSpriteId.KEY
        "navigation" -> PixelSpriteId.MAP
        "vacation" -> PixelSpriteId.SUITCASE
        "rooms" -> PixelSpriteId.HOUSE
        "furniture" -> PixelSpriteId.CHAIR
        "kitchen" -> PixelSpriteId.COFFEE
        "bathroom" -> PixelSpriteId.DRINK
        "appliances" -> PixelSpriteId.HOUSE
        "family", "personality", "social" -> PixelSpriteId.PERSON
        "clothes", "clothing", "shoes" -> PixelSpriteId.SHIRT
        "heart", "positive", "negative", "traits", "mind", "communication" -> PixelSpriteId.HEART
        "building" -> PixelSpriteId.CITY
        "briefcase", "office", "business", "trades", "career" -> PixelSpriteId.BRIEFCASE
        "stores", "materials" -> PixelSpriteId.SHOPPING_BAG
        "services", "money" -> PixelSpriteId.COIN
        "emergency", "health", "doctor", "body_parts", "hygiene" -> PixelSpriteId.DOCTOR
        "weather" -> PixelSpriteId.SUN
        "animal" -> PixelSpriteId.CAT
        "plant" -> PixelSpriteId.TREE
        "landscape", "nature" -> PixelSpriteId.MOUNTAIN
        "fitness", "sport" -> PixelSpriteId.SPORT
        "music" -> PixelSpriteId.MUSIC
        "art", "colors" -> PixelSpriteId.PALETTE
        "outdoor" -> PixelSpriteId.MOUNTAIN
        "games" -> PixelSpriteId.LAPTOP
        "tools" -> PixelSpriteId.KEY
        "computer", "internet", "data" -> PixelSpriteId.LAPTOP
        "devices" -> PixelSpriteId.PHONE
        "science" -> PixelSpriteId.GLOBE
        "school", "university", "supplies", "languages", "exams" -> PixelSpriteId.BOOK
        "festivals", "politics", "media", "religion", "history" -> PixelSpriteId.GLOBE
        "movement", "speaking", "daily", "thinking", "actions" -> PixelSpriteId.LIGHTNING
        "qualities", "size", "sensory", "mood" -> PixelSpriteId.PALETTE
        else -> PixelSpriteId.BOOK
    }
}

fun resolveSpriteForWord(word: WordEntity): PixelSpriteId {
    val g = word.german.lowercase()
    val en = word.translationEn.lowercase()
    val sub = word.subtopicId.lowercase()
    val cat = word.categoryId.lowercase()

    // Exact German/English keyword matches first for maximum visual relevance
    return when {
        g.contains("apfel") || g.contains("obst") || g.contains("kirsch") || g.contains("erdbeer") || g.contains("birne") || g.contains("orange") -> PixelSpriteId.APPLE
        g.contains("brot") || g.contains("brötchen") || g.contains("toast") || g.contains("bäck") -> PixelSpriteId.BREAD
        g.contains("kaffee") || g.contains("tee") || g.contains("tasse") || g.contains("espresso") -> PixelSpriteId.COFFEE
        g.contains("kuchen") || g.contains("torte") || g.contains("eis") || g.contains("schoko") || g.contains("zucker") -> PixelSpriteId.CAKE
        g.contains("fleisch") || g.contains("wurst") || g.contains("hähnchen") || g.contains("fisch") || g.contains("steak") -> PixelSpriteId.MEAT
        g.contains("karotte") || g.contains("tomate") || g.contains("gurke") || g.contains("salat") || g.contains("kartoffel") || g.contains("gemüse") -> PixelSpriteId.CARROT
        g.contains("wasser") || g.contains("saft") || g.contains("milch") || g.contains("wein") || g.contains("bier") -> PixelSpriteId.DRINK

        g.contains("schlüssel") || g.contains("schloss") -> PixelSpriteId.KEY
        g.contains("flug") || g.contains("flughafen") || g.contains("pilot") -> PixelSpriteId.PLANE
        g.contains("zug") || g.contains("bahn") || g.contains("tram") || g.contains("gleis") -> PixelSpriteId.TRAIN
        g.contains("auto") || g.contains("bus") || g.contains("taxi") || g.contains("wagen") || g.contains("rad") -> PixelSpriteId.CAR
        g.contains("koffer") || g.contains("gepäck") || g.contains("tasche") || g.contains("rucksack") -> PixelSpriteId.SUITCASE
        g.contains("karte") || g.contains("pass") || g.contains("weg") || g.contains("reise") -> PixelSpriteId.MAP

        g.contains("bett") || g.contains("schlaf") || g.contains("kissen") || g.contains("decke") -> PixelSpriteId.BED
        g.contains("stuhl") || g.contains("tisch") || g.contains("sofa") || g.contains("schrank") || g.contains("möbel") -> PixelSpriteId.CHAIR
        g.contains("haus") || g.contains("wohnung") || g.contains("dach") || g.contains("tür") || g.contains("zimmer") -> PixelSpriteId.HOUSE

        g.contains("herz") || g.contains("liebe") || g.contains("glück") || g.contains("freude") -> PixelSpriteId.HEART
        g.contains("arzt") || g.contains("medizin") || g.contains("krank") || g.contains("fieber") || g.contains("apotheke") -> PixelSpriteId.DOCTOR
        g.contains("hund") || g.contains("katze") || g.contains("tier") || g.contains("vogel") || g.contains("pferd") -> PixelSpriteId.CAT

        g.contains("sonne") || g.contains("wetter") || g.contains("sommer") || g.contains("licht") || g.contains("regen") -> PixelSpriteId.SUN
        g.contains("berg") || g.contains("meer") || g.contains("fluss") || g.contains("see") || g.contains("strand") -> PixelSpriteId.MOUNTAIN
        g.contains("baum") || g.contains("wald") || g.contains("blume") || g.contains("pflanze") || g.contains("garten") -> PixelSpriteId.TREE

        g.contains("geld") || g.contains("euro") || g.contains("preis") || g.contains("bank") || g.contains("kauf") -> PixelSpriteId.COIN
        g.contains("hemd") || g.contains("hose") || g.contains("jacke") || g.contains("schuh") || g.contains("kleid") -> PixelSpriteId.SHIRT

        g.contains("laptop") || g.contains("computer") || g.contains("netz") || g.contains("daten") || g.contains("software") -> PixelSpriteId.LAPTOP
        g.contains("handy") || g.contains("telefon") || g.contains("anruf") -> PixelSpriteId.PHONE
        g.contains("buch") || g.contains("schule") || g.contains("uni") || g.contains("stud") || g.contains("les") -> PixelSpriteId.BOOK
        g.contains("musik") || g.contains("lied") || g.contains("gitarre") || g.contains("konzert") -> PixelSpriteId.MUSIC
        g.contains("sport") || g.contains("ball") || g.contains("fußball") || g.contains("spiel") -> PixelSpriteId.SPORT
        g.contains("zeit") || g.contains("uhr") || g.contains("stunde") || g.contains("tag") || g.contains("woche") -> PixelSpriteId.CLOCK
        g.contains("farbe") || g.contains("kunst") || g.contains("mal") || g.contains("bild") -> PixelSpriteId.PALETTE
        g.contains("welt") || g.contains("land") || g.contains("politik") || g.contains("gesetz") -> PixelSpriteId.GLOBE

        // Subtopic fallbacks
        sub.contains("fruit") -> PixelSpriteId.APPLE
        sub.contains("veg") -> PixelSpriteId.CARROT
        sub.contains("drink") -> PixelSpriteId.COFFEE
        sub.contains("meat") -> PixelSpriteId.MEAT
        sub.contains("bakery") || sub.contains("sweet") -> PixelSpriteId.CAKE
        sub.contains("airport") -> PixelSpriteId.PLANE
        sub.contains("transport") || sub.contains("drive") -> PixelSpriteId.TRAIN
        sub.contains("hotel") || sub.contains("luggage") -> PixelSpriteId.SUITCASE
        sub.contains("furniture") -> PixelSpriteId.CHAIR
        sub.contains("room") || sub.contains("appliance") || sub.contains("tool") -> PixelSpriteId.HOUSE
        sub.contains("health") || sub.contains("body") -> PixelSpriteId.DOCTOR
        sub.contains("animal") -> PixelSpriteId.CAT
        sub.contains("weather") -> PixelSpriteId.SUN
        sub.contains("plant") || sub.contains("eco") -> PixelSpriteId.TREE
        sub.contains("geo") -> PixelSpriteId.MOUNTAIN
        sub.contains("cloth") -> PixelSpriteId.SHIRT
        sub.contains("money") || sub.contains("pay") || sub.contains("shop") -> PixelSpriteId.COIN
        sub.contains("music") || sub.contains("art") -> PixelSpriteId.MUSIC
        sub.contains("sport") || sub.contains("game") -> PixelSpriteId.SPORT
        sub.contains("internet") || sub.contains("software") || sub.contains("device") -> PixelSpriteId.LAPTOP
        sub.contains("mobile") || sub.contains("media") -> PixelSpriteId.PHONE
        sub.contains("positive") || sub.contains("negative") || sub.contains("mind") -> PixelSpriteId.HEART
        sub.contains("move") || sub.contains("action") -> PixelSpriteId.LIGHTNING

        // Category fallbacks
        cat == "food" -> PixelSpriteId.BREAD
        cat == "travel" -> PixelSpriteId.MAP
        cat == "home" -> PixelSpriteId.HOUSE
        cat == "people" -> PixelSpriteId.PERSON
        cat == "city" -> PixelSpriteId.CITY
        cat == "nature" -> PixelSpriteId.TREE
        cat == "body" -> PixelSpriteId.DOCTOR
        cat == "hobbies" -> PixelSpriteId.MUSIC
        cat == "work" -> PixelSpriteId.BRIEFCASE
        cat == "tech" -> PixelSpriteId.LAPTOP
        cat == "education" -> PixelSpriteId.BOOK
        cat == "shopping" -> PixelSpriteId.COIN
        cat == "society" -> PixelSpriteId.GLOBE
        cat == "emotions" -> PixelSpriteId.HEART
        cat == "verbs" -> PixelSpriteId.LIGHTNING
        cat == "adjectives" -> PixelSpriteId.PALETTE
        en.contains("time") -> PixelSpriteId.CLOCK
        else -> PixelSpriteId.BOOK
    }
}

private fun buildSpriteSpec(id: PixelSpriteId): PixelSpriteSpec = when (id) {
    PixelSpriteId.APPLE -> PixelSpriteSpec(
        rows = listOf(
            "......GG....",
            ".....GGb....",
            "...RRRRRR...",
            "..RWWRRRRR..",
            ".RWRRRRRRRR.",
            ".RRRRRRRRRD.",
            ".RRRRRRRRDD.",
            ".RRRRRRRDDD.",
            "..RRRRRDDD..",
            "...RRRDDD...",
            "....RRDD...."
        ),
        palette = mapOf(
            'R' to Color(0xFFD9383A),
            'D' to Color(0xFF9E1F24),
            'W' to Color(0xFFFFB5A7),
            'G' to Color(0xFF3E8E41),
            'b' to Color(0xFF6B4226)
        ),
        bgTint = Color(0xFFEF9A9A)
    )

    PixelSpriteId.BREAD -> PixelSpriteSpec(
        rows = listOf(
            "...BBBBBB...",
            "..BTTTTTTB..",
            ".BTWWTTWWTT.",
            ".BTTTTTTTTD.",
            "BTTTTTTTTDDD",
            "BCCCCCCCDDDD",
            "BCCCCCCCDDDD",
            ".BCCCCCCCDD.",
            "..BBBBBBBB.."
        ),
        palette = mapOf(
            'B' to Color(0xFF6E3B16),
            'T' to Color(0xFFD98A36),
            'W' to Color(0xFFF3C677),
            'D' to Color(0xFF9C541C),
            'C' to Color(0xFFE7B872)
        ),
        bgTint = Color(0xFFE6BE8A)
    )

    PixelSpriteId.COFFEE -> PixelSpriteSpec(
        rows = listOf(
            "....S...S...",
            "...S...S....",
            "....S...S...",
            "..KKKKKKKK..",
            "..KCCCCCCC..",
            "..KMMMMMMKBB",
            "..KMMMMMMK.B",
            "..KMMMMMMKBB",
            "..KMMMMMMK..",
            "...KKKKKK...",
            ".DDDDDDDDDD."
        ),
        palette = mapOf(
            'S' to Color(0xFFD6CCC2),
            'K' to Color(0xFFEDE0D4),
            'C' to Color(0xFF4A2C18),
            'M' to Color(0xFFB4532A),
            'B' to Color(0xFFDDB892),
            'D' to Color(0xFF7F5539)
        ),
        bgTint = Color(0xFFDDB892)
    )

    PixelSpriteId.CAKE -> PixelSpriteSpec(
        rows = listOf(
            ".....RR.....",
            "....RRRR....",
            "..WWWWWWWW..",
            ".WWWWWWWWWW.",
            ".PPPPPPPPPP.",
            ".BBBBBBBBBB.",
            ".PPPPPPPPPP.",
            ".BBBBBBBBBB.",
            "DDDDDDDDDDDD"
        ),
        palette = mapOf(
            'R' to Color(0xFFD9383A),
            'W' to Color(0xFFFFF8E7),
            'P' to Color(0xFFE58A9B),
            'B' to Color(0xFFD4A373),
            'D' to Color(0xFF7F5539)
        ),
        bgTint = Color(0xFFF4ACB7)
    )

    PixelSpriteId.MEAT -> PixelSpriteSpec(
        rows = listOf(
            "...BBBBBB...",
            "..BRRRRRRB..",
            ".BRWWWWWRRB.",
            ".BRRWRRRRDB.",
            ".BRRRRRRDDB.",
            "..BRRRRDDB..",
            "...BBBBBB..."
        ),
        palette = mapOf(
            'B' to Color(0xFF5C2B1B),
            'R' to Color(0xFFC84B31),
            'W' to Color(0xFFF5DDD3),
            'D' to Color(0xFF8E2B1C)
        ),
        bgTint = Color(0xFFE69F8A)
    )

    PixelSpriteId.CARROT -> PixelSpriteSpec(
        rows = listOf(
            "....G..G....",
            "...GGGGG....",
            "....OOO.....",
            "...OOOWO....",
            "...OOOOO....",
            "....OODO....",
            "....OOD.....",
            ".....OO.....",
            ".....D......"
        ),
        palette = mapOf(
            'G' to Color(0xFF3E8E41),
            'O' to Color(0xFFE76F28),
            'W' to Color(0xFFF8B26A),
            'D' to Color(0xFFB34410)
        ),
        bgTint = Color(0xFFA8D5BA)
    )

    PixelSpriteId.DRINK -> PixelSpriteSpec(
        rows = listOf(
            ".......SS...",
            "......S.....",
            "..GGGGGGGG..",
            "..GWWWWWWG..",
            "..GBBBBBBG..",
            "..GBBWWBBG..",
            "..GBBBBBBG..",
            "..GDDBBDDG..",
            "...GGGGGG..."
        ),
        palette = mapOf(
            'S' to Color(0xFFD9383A),
            'G' to Color(0xFF90B8C8),
            'W' to Color(0xFFEAF6FA),
            'B' to Color(0xFF4287A8),
            'D' to Color(0xFF285B75)
        ),
        bgTint = Color(0xFF90C2E7)
    )

    PixelSpriteId.CAR -> PixelSpriteSpec(
        rows = listOf(
            "....BBBB....",
            "...BWWWWB...",
            ".BBCCCCCCBB.",
            "YCCCCCCCCCCY",
            "CCCCCCCCCCCC",
            ".KDDKKKKDDK.",
            "..KK....KK.."
        ),
        palette = mapOf(
            'B' to Color(0xFF2B3A42),
            'W' to Color(0xFFBDE0FE),
            'C' to Color(0xFFD1493B),
            'Y' to Color(0xFFF4C430),
            'K' to Color(0xFF222222),
            'D' to Color(0xFF9E9E9E)
        ),
        bgTint = Color(0xFFA9C8E2)
    )

    PixelSpriteId.TRAIN -> PixelSpriteSpec(
        rows = listOf(
            "..SSSSSSSS..",
            ".STTTTTTTTS.",
            ".SWWWSSWWWS.",
            ".SWWWSSWWWS.",
            ".STTTTTTTTS.",
            ".SRRRRRRRRS.",
            ".SYSSSSSSYS.",
            "..SS....SS..",
            ".GG......GG."
        ),
        palette = mapOf(
            'S' to Color(0xFF3D405B),
            'T' to Color(0xFFF4F1DE),
            'W' to Color(0xFF81B29A),
            'R' to Color(0xFFE07A5F),
            'Y' to Color(0xFFF2CC8F),
            'G' to Color(0xFF6C757D)
        ),
        bgTint = Color(0xFFB0C4B1)
    )

    PixelSpriteId.PLANE -> PixelSpriteSpec(
        rows = listOf(
            ".....SS.....",
            ".....SW.....",
            "....SSWW....",
            "SSSSSSWWSSSS",
            ".SSSSWWWWSS.",
            "....SSWW....",
            "....SSWW....",
            "...SS.WW.SS.",
            "..SS......SS"
        ),
        palette = mapOf(
            'S' to Color(0xFF4A6FA5),
            'W' to Color(0xFFF1F5F9)
        ),
        bgTint = Color(0xFF9BBCE3)
    )

    PixelSpriteId.KEY -> PixelSpriteSpec(
        rows = listOf(
            "..YYYY......",
            ".YWWWWY.....",
            ".YW..WY.....",
            "..YYYYYYYYYY",
            ".......Y.Y.Y",
            ".......Y...Y"
        ),
        palette = mapOf(
            'Y' to Color(0xFFD49A2A),
            'W' to Color(0xFFF4D06F)
        ),
        bgTint = Color(0xFFE9D8A6)
    )

    PixelSpriteId.SUITCASE -> PixelSpriteSpec(
        rows = listOf(
            "....KKKK....",
            "....K..K....",
            ".BBBBBBBBBB.",
            "BTTBTTTTBTTB",
            "BTYBTYYBTYTB",
            "BTTBTTTTBTTB",
            "BDDDDDDDDDDB",
            ".BB......BB."
        ),
        palette = mapOf(
            'K' to Color(0xFF3A2D24),
            'B' to Color(0xFF6B4226),
            'T' to Color(0xFFA47148),
            'D' to Color(0xFF8B5A2B),
            'Y' to Color(0xFFE2B158)
        ),
        bgTint = Color(0xFFD7B899)
    )

    PixelSpriteId.MAP -> PixelSpriteSpec(
        rows = listOf(
            ".....RR.....",
            "....RWRR....",
            "....RRRR....",
            ".PP..RR.DD..",
            "PPPPPPPPDDDD",
            "PPGGPPPPGGDD",
            "PPGGPPBBGGDD",
            "PPPPBBPPDDDD",
            ".PP.....DD.."
        ),
        palette = mapOf(
            'R' to Color(0xFFC83E3B),
            'W' to Color(0xFFFFFFFF),
            'P' to Color(0xFFF3E9D2),
            'D' to Color(0xFFD5C6A8),
            'G' to Color(0xFF6B9080),
            'B' to Color(0xFF457B9D)
        ),
        bgTint = Color(0xFFCDEAC0)
    )

    PixelSpriteId.HOUSE -> PixelSpriteSpec(
        rows = listOf(
            ".....RR.....",
            "....RRRR....",
            "...RRRRRR...",
            "..RRRRRRRR..",
            ".RRRRRRRRRR.",
            "..WWWWWWWW..",
            "..WBWWWWBW..",
            "..WWDDDDWW..",
            "..WWDWWDWW..",
            "..WWWWWWWW.."
        ),
        palette = mapOf(
            'R' to Color(0xFFB4532A),
            'W' to Color(0xFFF4EFE6),
            'B' to Color(0xFF7CA5B8),
            'D' to Color(0xFF7F5539)
        ),
        bgTint = Color(0xFFE6CCB2)
    )

    PixelSpriteId.BED -> PixelSpriteSpec(
        rows = listOf(
            "B...........",
            "B.WWW.......",
            "BWWWWBBBBBB.",
            "BCCCCCCCCCCB",
            "BBBBBBBBBBBB",
            "B..........B"
        ),
        palette = mapOf(
            'B' to Color(0xFF6D4C3D),
            'W' to Color(0xFFF7F4EE),
            'C' to Color(0xFF3E6B89)
        ),
        bgTint = Color(0xFFAEC6CF)
    )

    PixelSpriteId.CHAIR -> PixelSpriteSpec(
        rows = listOf(
            "...BBBB.....",
            "...BTTB.....",
            "...BTTB.....",
            "...BBBBBB...",
            "...BTTTTB...",
            "...B....B...",
            "...B....B..."
        ),
        palette = mapOf(
            'B' to Color(0xFF5C3A21),
            'T' to Color(0xFFA47148)
        ),
        bgTint = Color(0xFFDDB892)
    )

    PixelSpriteId.HEART -> PixelSpriteSpec(
        rows = listOf(
            "..RRR..RRR..",
            ".RWWWRRRRDR.",
            "RRWWRRRRRDDR",
            "RRRRRRRRDDDR",
            ".RRRRRRDDDR.",
            "..RRRRDDDR..",
            "...RRRDDR...",
            "....RRDR....",
            ".....RR....."
        ),
        palette = mapOf(
            'R' to Color(0xFFD1495B),
            'W' to Color(0xFFF8B3BC),
            'D' to Color(0xFF9B2235)
        ),
        bgTint = Color(0xFFF5B7B1)
    )

    PixelSpriteId.PERSON -> PixelSpriteSpec(
        rows = listOf(
            "....HHHH....",
            "...HHHHHH...",
            "...HSSSSS...",
            "...SKSKSS...",
            "....SSSS....",
            "...BBBBBB...",
            "..BTTTTTTB..",
            "..BTTTTTTB..",
            "...BBBBBB..."
        ),
        palette = mapOf(
            'H' to Color(0xFF5C4033),
            'S' to Color(0xFFE8B89A),
            'K' to Color(0xFF2B2D42),
            'B' to Color(0xFF264653),
            'T' to Color(0xFF457B9D)
        ),
        bgTint = Color(0xFFB5CDE0)
    )

    PixelSpriteId.DOCTOR -> PixelSpriteSpec(
        rows = listOf(
            "..WWWWWWWW..",
            ".WWWWWWWWWW.",
            ".WWWW..WWWW.",
            ".WWWWRRWWWW.",
            ".WWWRRRRWWW.",
            ".WWWWRRWWWW.",
            ".WWWW..WWWW.",
            ".WWWWWWWWWW.",
            "..DDDDDDDD.."
        ),
        palette = mapOf(
            'W' to Color(0xFFF8F9FA),
            'R' to Color(0xFFC53B3B),
            'D' to Color(0xFFCED4DA)
        ),
        bgTint = Color(0xFFF1C0C0)
    )

    PixelSpriteId.TREE -> PixelSpriteSpec(
        rows = listOf(
            ".....GG.....",
            "....GGGG....",
            "...GGGGGG...",
            "....GGGG....",
            "..GGGGGGGG..",
            ".GGGGGGGGDD.",
            "...GGGGGG...",
            ".....BB.....",
            ".....BB.....",
            "....BBBB...."
        ),
        palette = mapOf(
            'G' to Color(0xFF2D6A4F),
            'D' to Color(0xFF1B4332),
            'B' to Color(0xFF6F4E37)
        ),
        bgTint = Color(0xFFB7E4C7)
    )

    PixelSpriteId.SUN -> PixelSpriteSpec(
        rows = listOf(
            ".....YY.....",
            ".Y..YYYY..Y.",
            "..YYWWWWYY..",
            "..YWWWWWWY..",
            "YYYWWYYYYYYY",
            "YYYYYYYYYYYY",
            "..YYYYYYYY..",
            ".Y..YYYY..Y.",
            ".....YY....."
        ),
        palette = mapOf(
            'Y' to Color(0xFFE9A825),
            'W' to Color(0xFFFBE38E)
        ),
        bgTint = Color(0xFFFDF0B5)
    )

    PixelSpriteId.CAT -> PixelSpriteSpec(
        rows = listOf(
            ".OO......OO.",
            ".OOO....OOO.",
            ".OOOOOOOOOO.",
            ".OOKOOOOOKOO",
            ".OOOOOPOOOO.",
            "..OOOWWWOO..",
            "...OOOOOO..."
        ),
        palette = mapOf(
            'O' to Color(0xFFD9822B),
            'K' to Color(0xFF1D242B),
            'P' to Color(0xFFE58A9B),
            'W' to Color(0xFFFDF5E6)
        ),
        bgTint = Color(0xFFF5D0A9)
    )

    PixelSpriteId.MOUNTAIN -> PixelSpriteSpec(
        rows = listOf(
            ".....WW.....",
            "....WWWW....",
            "...WWGGWW...",
            "..GGGGGGGD..",
            ".GGGGGGGGDD.",
            "GGGGGGDDDDDD",
            "BBBBBBBBBBBB"
        ),
        palette = mapOf(
            'W' to Color(0xFFF4F6F9),
            'G' to Color(0xFF52796F),
            'D' to Color(0xFF354F52),
            'B' to Color(0xFF457B9D)
        ),
        bgTint = Color(0xFFA8DADC)
    )

    PixelSpriteId.CITY -> PixelSpriteSpec(
        rows = listOf(
            "....BBBB....",
            "....BYYB....",
            ".GG.BBBB.SS.",
            ".GY.BYYB.SY.",
            ".GG.BBBB.SS.",
            ".GY.BYYB.SY.",
            ".GG.BWWB.SS.",
            "DDDDDDDDDDDD"
        ),
        palette = mapOf(
            'B' to Color(0xFF3D5A80),
            'G' to Color(0xFF6D6875),
            'S' to Color(0xFF5C6B73),
            'Y' to Color(0xFFF4D35E),
            'W' to Color(0xFF98C1D9),
            'D' to Color(0xFF293241)
        ),
        bgTint = Color(0xFFC2CEDB)
    )

    PixelSpriteId.COIN -> PixelSpriteSpec(
        rows = listOf(
            "...YYYYYY...",
            "..YYWWWWYY..",
            ".YYWYYYYDYY.",
            ".YYWYYDYYDY.",
            ".YYWYYYYDYY.",
            "..YYDDDDYY..",
            "...YYYYYY..."
        ),
        palette = mapOf(
            'Y' to Color(0xFFD99B26),
            'W' to Color(0xFFFAE084),
            'D' to Color(0xFFA46B13)
        ),
        bgTint = Color(0xFFF7E5B7)
    )

    PixelSpriteId.SHIRT -> PixelSpriteSpec(
        rows = listOf(
            "..BB....BB..",
            ".BBBBWWBBBB.",
            "BBBBBBBBBBBB",
            "BBB.BBBB.BBB",
            "....BBBB....",
            "....BWWB....",
            "....BBBB....",
            "....BBBB...."
        ),
        palette = mapOf(
            'B' to Color(0xFF3A6073),
            'W' to Color(0xFFA8C6D6)
        ),
        bgTint = Color(0xFFBACCD6)
    )

    PixelSpriteId.BRIEFCASE -> PixelSpriteSpec(
        rows = listOf(
            "....KKKK....",
            "....K..K....",
            ".BBBBBBBBBB.",
            "BTTTTTTTTTTB",
            "BTTTTYYTTTTB",
            "BDDDDDDDDDDB",
            "BDDDDDDDDDDB",
            ".BBBBBBBBBB."
        ),
        palette = mapOf(
            'K' to Color(0xFF2B2118),
            'B' to Color(0xFF4E342E),
            'T' to Color(0xFF795548),
            'D' to Color(0xFF5D4037),
            'Y' to Color(0xFFE0A943)
        ),
        bgTint = Color(0xFFD6C3B3)
    )

    PixelSpriteId.LAPTOP -> PixelSpriteSpec(
        rows = listOf(
            "..KKKKKKKK..",
            "..KBBBBBBK..",
            "..KBWWWWBK..",
            "..KBWWWWBK..",
            "..KBBBBBBK..",
            ".KKKKKKKKKK.",
            "KSSSSSSSSSSK",
            "KKKKKKKKKKKK"
        ),
        palette = mapOf(
            'K' to Color(0xFF2E3440),
            'B' to Color(0xFF4C566A),
            'W' to Color(0xFF88C0D0),
            'S' to Color(0xFFD8DEE9)
        ),
        bgTint = Color(0xFFC5D5E0)
    )

    PixelSpriteId.PHONE -> PixelSpriteSpec(
        rows = listOf(
            "...KKKKKK...",
            "...KWWWWK...",
            "...KWBBWK...",
            "...KWWWWK...",
            "...KWYYWK...",
            "...KWWWWK...",
            "...KKKKKK..."
        ),
        palette = mapOf(
            'K' to Color(0xFF2B2D42),
            'W' to Color(0xFFEDF2F4),
            'B' to Color(0xFF457B9D),
            'Y' to Color(0xFF2A9D8F)
        ),
        bgTint = Color(0xFFC6DCE4)
    )

    PixelSpriteId.BOOK -> PixelSpriteSpec(
        rows = listOf(
            "..BBBB.BBBB.",
            ".BWWWWBWWWWB",
            ".BWKKWBWKKWB",
            ".BWWWWBWWWWB",
            ".BWKKWBWKKWB",
            ".BWWWWBWWWWB",
            "..BBBB.BBBB.",
            ".....RRR...."
        ),
        palette = mapOf(
            'B' to Color(0xFF7F4F24),
            'W' to Color(0xFFFAF3E3),
            'K' to Color(0xFF8C8275),
            'R' to Color(0xFFB4532A)
        ),
        bgTint = Color(0xFFE6D5B8)
    )

    PixelSpriteId.MUSIC -> PixelSpriteSpec(
        rows = listOf(
            ".....PPPP...",
            "....PPPPPP..",
            "....PP..PP..",
            "....PP..PP..",
            "..PPPP..PP..",
            ".PPPPP.PPP..",
            ".PPPPP.PPP..",
            "..PPP...P..."
        ),
        palette = mapOf(
            'P' to Color(0xFF6C4A7E)
        ),
        bgTint = Color(0xFFD7C9E2)
    )

    PixelSpriteId.SPORT -> PixelSpriteSpec(
        rows = listOf(
            "...WWWWWW...",
            "..WWKKWWWW..",
            ".WKKWWKKWWW.",
            ".WWWWWWWWKW.",
            ".WWKKWWKKWW.",
            "..WWKKWWWW..",
            "...WWWWWW..."
        ),
        palette = mapOf(
            'W' to Color(0xFFF4F4F4),
            'K' to Color(0xFF264653)
        ),
        bgTint = Color(0xFFC4DFCB)
    )

    PixelSpriteId.GLOBE -> PixelSpriteSpec(
        rows = listOf(
            "...BBBBBB...",
            "..BBGGBBBB..",
            ".BGGGGBBBGB.",
            ".BGGGBBGGGB.",
            ".BBBBGGGGBB.",
            "..BBBGGGBB..",
            "...BBBBBB..."
        ),
        palette = mapOf(
            'B' to Color(0xFF3A7CA5),
            'G' to Color(0xFF52B788)
        ),
        bgTint = Color(0xFFB8DBE5)
    )

    PixelSpriteId.CLOCK -> PixelSpriteSpec(
        rows = listOf(
            "...BBBBBB...",
            "..BWWWWWWB..",
            ".BWWWKWWWWB.",
            ".BWWWKWWKKB.",
            ".BWWWWWWWWB.",
            "..BWWWWWWB..",
            "...BBBBBB..."
        ),
        palette = mapOf(
            'B' to Color(0xFFB4532A),
            'W' to Color(0xFFFAF4E6),
            'K' to Color(0xFF2D2218)
        ),
        bgTint = Color(0xFFEAD7BE)
    )

    PixelSpriteId.LIGHTNING -> PixelSpriteSpec(
        rows = listOf(
            ".....YYYY...",
            "....YYYY....",
            "...YYYY.....",
            "..YYYYYYYY..",
            ".....YYYY...",
            "....YYYY....",
            "...YYY......",
            "..YY........"
        ),
        palette = mapOf(
            'Y' to Color(0xFFD9822B)
        ),
        bgTint = Color(0xFFF6DFB9)
    )

    PixelSpriteId.PALETTE -> PixelSpriteSpec(
        rows = listOf(
            "...BBBBBB...",
            "..BWRBBGBB..",
            ".BWRRBBGGBB.",
            ".BBBBBBBBYY.",
            ".BBPPBB..YY.",
            "..BBBBBB....",
            "....BBBB...."
        ),
        palette = mapOf(
            'B' to Color(0xFFD7B899),
            'W' to Color(0xFFFAF4E6),
            'R' to Color(0xFFC53B3B),
            'G' to Color(0xFF287D56),
            'Y' to Color(0xFFE9A825),
            'P' to Color(0xFF457B9D)
        ),
        bgTint = Color(0xFFE4D5C7)
    )

    PixelSpriteId.SHOPPING_BAG -> PixelSpriteSpec(
        rows = listOf(
            "....KKKK....",
            "...K....K...",
            "..PPPPPPPP..",
            ".PPPPWWPPPP.",
            ".PPPPPPPPPP.",
            ".PPPDDDDPPP.",
            ".PPPDDDDPPP.",
            "..PPPPPPPP.."
        ),
        palette = mapOf(
            'K' to Color(0xFF8C5A3C),
            'P' to Color(0xFFD95375),
            'W' to Color(0xFFFAD2DC),
            'D' to Color(0xFFAA3654)
        ),
        bgTint = Color(0xFFF5C6D2)
    )
}
