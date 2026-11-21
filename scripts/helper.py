# -*- coding: utf-8 -*-

def emit_kotlin_file(target_file, class_name, words):
    chunk_size = 60
    chunks = [words[i:i + chunk_size] for i in range(0, len(words), chunk_size)]
    
    lines = [
        "package com.example.data.vocabulary",
        "",
        "import com.example.data.model.WordEntity",
        "",
        f"object {class_name} {{",
    ]
    
    part_names = []
    for idx, chunk in enumerate(chunks):
        p_name = f"part{idx + 1}"
        part_names.append(p_name)
        lines.append(f"    private val {p_name} = listOf(")
        for w in chunk:
            if len(w) == 11:
                de, art, pl, tr, uk, en, ex_de, ex_uk, cat, sub, lvl = w
            else:
                de, art, pl, uk, en, ex_de, ex_uk, cat, sub, lvl = w
                tr = f"[{de.lower()}]"
            ex_de_esc = ex_de.replace('"', '\\"')
            ex_uk_esc = ex_uk.replace('"', '\\"')
            lines.append("        WordEntity(")
            lines.append(f'            german = "{de}",')
            lines.append(f'            article = "{art}",')
            lines.append(f'            plural = "{pl}",')
            lines.append(f'            transcription = "{tr}",')
            lines.append(f'            translationUk = "{uk}",')
            lines.append(f'            translationEn = "{en}",')
            lines.append(f'            exampleDe = "{ex_de_esc}",')
            lines.append(f'            exampleUk = "{ex_uk_esc}",')
            lines.append(f'            exampleEn = "",')
            lines.append(f'            categoryId = "{cat}",')
            lines.append(f'            subtopicId = "{sub}",')
            lines.append(f'            level = "{lvl}"')
            lines.append("        ),")
        lines.append("    )")
        lines.append("")
    
    parts_expr = " + ".join(part_names)
    lines.append(f"    val words: List<WordEntity> = {parts_expr}")
    lines.append("}")
    lines.append("")
    
    with open(target_file, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"Emitted {target_file} with {len(words)} words.")

