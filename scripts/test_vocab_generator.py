# -*- coding: utf-8 -*-

def make_word(de, art, pl, uk, en, ex_de, ex_uk, cat, sub, lvl):
    tr = f"[{de.lower()}]"
    return (de, art, pl, tr, uk, en, ex_de, ex_uk, cat, sub, lvl)

test_words = [
    make_word("Brot", "das", "die Brote", "хліб", "bread", "Frisches Brot schmeckt lecker.", "Свіжий хліб смачний.", "food", "food_meals", "A1")
]
print("Test word ok:", test_words[0])
