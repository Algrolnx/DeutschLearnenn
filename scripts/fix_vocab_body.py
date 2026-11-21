# -*- coding: utf-8 -*-
with open('scripts/make_batch4.py', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('scripts/vocab_body.py', 'w', encoding='utf-8') as f:
    f.writelines(lines[:267])
    f.write('''
def get_body_words():
    words = []
    subs = ["body_parts", "health_illness", "medicine_care", "appearance_hygiene", "fitness_wellness"]
    for i, w in enumerate(body_data):
        sub = subs[i // 50]
        if len(w) == 7:
            de, art, pl, uk, ex_de, ex_uk, lvl = w
            en = ""
        elif len(w) == 8:
            de, art, pl, uk, en, ex_de, ex_uk, lvl = w
        words.append((de, art, pl, f"[{de.lower()}]", uk, en, ex_de, ex_uk, "body", sub, lvl))
    return words

BODY_WORDS = get_body_words()
''')
print("Successfully generated vocab_body.py")
