# -*- coding: utf-8 -*-
from scripts.group1 import get_food_words

def get_words():
    base = get_food_words()
    # Add extra culinary terms to reach 250 words
    extra = [
        ("Vollkornbrot", "das", "die Vollkornbrote", "цільнозерновий хліб", "wholegrain bread", "Vollkornbrot ist gesund.", "Цільнозерновий хліб корисний.", "A1"),
        ("Sonnenblumenöl", "das", "die Sonnenblumenöle", "соняшникова олія", "sunflower oil", "Sonnenblumenöl zum Braten.", "Соняшникова олія для смаження.", "A2"),
        ("Rapsöl", "das", "-", "ріпакова олія", "rapeseed oil", "Rapsöl enthält gesunde Fette.", "Ріпакова олія містить корисні жири.", "A2"),
        ("Olivenöl", "das", "die Olivenöle", "оливкова олія", "olive oil", "Natives Olivenöl extra.", "Оливкова олія першого холодного віджиму.", "A2"),
        ("Kräuterbutter", "die", "-", "масло із зеленню", "herb butter", "Kräuterbutter auf heißem Steak.", "Масло із зеленню на гарячому стейку.", "A2"),
        ("Schmelzkäse", "der", "-", "плавлений сир", "processed cheese", "Schmelzkäse auf Toast.", "Плавлений сир на тості.", "A2"),
        ("Frischkäse", "der", "-", "крем-сир", "cream cheese", "Frischkäse mit Kräutern.", "Крем-сир із зеленню.", "A2"),
        ("Ziegenkäse", "der", "die Ziegenkäse", "козячий сир", "goat cheese", "Würziger Ziegenkäse im Salat.", "Пікантний козячий сир у салаті.", "B1"),
        ("Schafskäse", "der", "-", "овечий сир / фета", "feta cheese", "Griechischer Schafskäse.", "Грецький овечий сир.", "A2"),
        ("Parmesan", "der", "-", "пармезан", "parmesan", "Parmesan über die Pasta reiben.", "Потерти пармезан на пасту.", "A2"),
        ("Mozzarella", "der", "-", "моцарела", "mozzarella", "Tomate mit Mozzarella und Basilikum.", "Помідор з моцарелою та базиліком.", "A1"),
        ("Senf", "der", "die Senfe", "гірчиця", "mustard", "Scharfer Senf zur Bratwurst.", "Гостра гірчиця до смаженої ковбаски.", "A1"),
        ("Ketchup", "der", "-", "кетчуп", "ketchup", "Pommes frites mit Ketchup.", "Картопля фрі з кетчупом.", "A1"),
        ("Mayonnaise", "die", "-", "майонез", "mayonnaise", "Salat mit Mayonnaise anmachen.", "Заправити салат майонезом.", "A1"),
        ("Gewürzgurke", "die", "die Gewürzgurken", "маринований огірок", "pickled gherkin", "Knackige Gewürzgurke.", "Хрусткий маринований огірок.", "A2"),
        ("Sauerkraut", "das", "-", "квашена капуста", "sauerkraut", "Sauerkraut mit Würstchen.", "Квашена капуста із сосисками.", "A1"),
        ("Kartoffelpüree", "das", "-", "картопляне пюре", "mashed potatoes", "Cremiges Kartoffelpüree mit Milch.", "Ніжне картопляне пюре з молоком.", "A2"),
        ("Pommes frites", "die", "-", "картопля фрі", "french fries", "Knusprige Pommes frites mit Salz.", "Хрустка картопля фрі із сіллю.", "A1"),
        ("Kroketten", "die", "-", "крокети", "croquettes", "Goldbraune Kartoffelkroketten.", "Золотисті картопляні крокети.", "A2"),
        ("Knödel", "der", "die Knödel", "кнедлик", "dumpling", "Bayerische Semmelknödel mit Sauce.", "Баварські хлібні кнедлики з підливою.", "A2")
    ]
    sub = "food_meals"
    for w in extra:
        pl = w[2]
        art = w[1]
        de = w[0]
        uk = w[3]
        en = w[4]
        ex_de = w[5]
        ex_uk = w[6]
        lvl = w[7]
        base.append((de, art, pl, uk, en, ex_de, ex_uk, "food", sub, lvl))
    return base

print("vocab_food count:", len(get_words()))
