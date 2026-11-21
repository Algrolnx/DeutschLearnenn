# -*- coding: utf-8 -*-
"""Generates Food, Travel, Home, People (1000 words)"""
import os

def render_kotlin_file(class_name, words):
    chunk_size = 80
    chunks = [words[i:i + chunk_size] for i in range(0, len(words), chunk_size)]
    
    code = [
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
        code.append(f"    private val {p_name} = listOf(")
        for w in chunk:
            de, art, pl, tr, uk, en, ex_de, ex_uk, cat, sub, lvl = w
            # escape quotes
            ex_de_esc = ex_de.replace('"', '\\"')
            ex_uk_esc = ex_uk.replace('"', '\\"')
            code.append("        WordEntity(")
            code.append(f'            german = "{de}",')
            code.append(f'            article = "{art}",')
            code.append(f'            plural = "{pl}",')
            code.append(f'            transcription = "{tr}",')
            code.append(f'            translationUk = "{uk}",')
            code.append(f'            translationEn = "{en}",')
            code.append(f'            exampleDe = "{ex_de_esc}",')
            code.append(f'            exampleUk = "{ex_uk_esc}",')
            code.append(f'            exampleEn = "",')
            code.append(f'            categoryId = "{cat}",')
            code.append(f'            subtopicId = "{sub}",')
            code.append(f'            level = "{lvl}"')
            code.append("        ),")
        code.append("    )")
        code.append("")
    
    parts_expr = " + ".join(part_names)
    code.append(f"    val words: List<WordEntity> = {parts_expr}")
    code.append("}")
    code.append("")
    return "\n".join(code)

print("gen_part1 loaded")
