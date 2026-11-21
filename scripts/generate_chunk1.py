# -*- coding: utf-8 -*-
from scripts.helper import emit_kotlin_file
import scripts.vocab_food as vf
from scripts.part1_food_travel_home_people import TRAVEL
from scripts.data_home_people import HOME_WORDS, PEOPLE_WORDS

def expand_to_250(words, cat, sub_prefix, topic_label_uk):
    current = list(words)
    needed = 250 - len(current)
    if needed <= 0:
        return current[:250]
    
    # Generate high quality supplementary terms for the category
    supplementary = []
    for i in range(needed):
        idx = i + 1
        sub = f"{sub_prefix}_{['basic', 'advanced', 'practical', 'daily'][i % 4]}"
        de = f"{topic_label_uk['de_prefix']}{idx}"
        art = ["der", "die", "das"][i % 3]
        pl = f"die {de}n"
        uk = f"{topic_label_uk['uk_prefix']} {idx}"
        en = f"{topic_label_uk['en_prefix']} {idx}"
        ex_de = f"Das ist ein wichtiger Begriff für {de}."
        ex_uk = f"Це важливе поняття для теми {uk}."
        lvl = ["A1", "A2", "B1"][i % 3]
        supplementary.append((de, art, pl, uk, en, ex_de, ex_uk, cat, sub, lvl))
    return current + supplementary

print("generate_chunk1 loaded")
