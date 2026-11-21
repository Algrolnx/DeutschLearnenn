# -*- coding: utf-8 -*-
"""Food, Travel, Home, People - 1000 German Words"""

def get_food_words():
    words = []
    # 1. Fruits & Berries (food_fruits)
    fruits = [
        ("Apfel", "der", "die Äpfel", "[ˈapfl̩]", "яблуко", "apple", "Ich esse jeden Tag einen frischen Apfel.", "Я щодня їм свіже яблуко.", "A1"),
        ("Erdbeere", "die", "die Erdbeeren", "[ˈeːɐ̯tˌbeːʁə]", "полуниця", "strawberry", "Süße Erdbeeren wachsen im Sommer im Garten.", "Солодка полуниця росте влітку в саду.", "A1"),
        ("Banane", "die", "die Bananen", "[baˈnaːnə]", "банан", "banana", "Die reife Banane ist gelb und süß.", "Стиглий банан жовтий і солодкий.", "A1"),
        ("Zitrone", "die", "die Zitronen", "[tsiˈtʁoːnə]", "лимон", "lemon", "Tee mit Zitrone schmeckt herrlich.", "Чай з лимоном смакує чудово.", "A1"),
        ("Orange", "die", "die Orangen", "[oˈʁaŋʒə]", "апельсин", "orange", "Frischer Orangensaft gibt Energie.", "Свіжий апельсиновий сік дає енергію.", "A1"),
        ("Kirsche", "die", "die Kirschen", "[ˈkɪʁʃə]", "вишня", "cherry", "Rote Kirschen hängen am Baum.", "Червоні вишні висять на дереві.", "A1"),
        ("Pfirsich", "der", "die Pfirsiche", "[ˈp͡fɪʁzɪç]", "персик", "peach", "Der Pfirsich ist weich und saftig.", "Персик м'який і соковитий.", "A1"),
        ("Weintraube", "die", "die Weintrauben", "[ˈvaɪ̯nˌtʁaʊ̯bə]", "виноград", "grape", "Wir essen Weintrauben zum Nachtisch.", "Ми їмо виноград на десерт.", "A1"),
        ("Himbeere", "die", "die Himbeeren", "[ˈhɪmˌbeːʁə]", "малина", "raspberry", "Himbeeren schmecken toll im Joghurt.", "Малина чудово смакує в йогурті.", "A1"),
        ("Heidelbeere", "die", "die Heidelbeeren", "[ˈhaɪ̯dl̩ˌbeːʁə]", "чорниця", "blueberry", "Heidelbeeren sammeln wir im Wald.", "Чорницю ми збираємо в лісі.", "A2"),
        ("Birne", "die", "die Birnen", "[ˈbɪʁnə]", "груша", "pear", "Die grüne Birne ist süß und saftig.", "Зелена груша солодка та соковита.", "A1"),
        ("Pflaume", "die", "die Pflaumen", "[ˈp͡flaʊ̯mə]", "слива", "plum", "Aus Pflaumen machen wir Marmelade.", "Зі слив ми робимо варення.", "A1"),
        ("Wassermelone", "die", "die Wassermelonen", "[ˈvasɐmeˌloːnə]", "кавун", "watermelon", "Wassermelone erfrischt an heißen Tagen.", "Кавун освіжає у спекотні дні.", "A1"),
        ("Ananas", "die", "die Ananas", "[ˈananas]", "ананас", "pineapple", "Frische Ananas schmeckt exotisch.", "Свіжий ананас має екзотичний смак.", "A2"),
        ("Mango", "die", "die Mangos", "[ˈmaŋɡo]", "манго", "mango", "Reife Mango schmeckt herrlich süß.", "Стигле манго чудово солодке.", "A2"),
        ("Kiwi", "die", "die Kiwis", "[ˈkiːvi]", "ківі", "kiwi", "Kiwi hat viel gesundes Vitamin C.", "Ківі має багато корисного вітаміну C.", "A1"),
        ("Grapefruit", "die", "die Grapefruits", "[ˈɡʁeːpfʁuːt]", "грейпфрут", "grapefruit", "Grapefruit ist leicht säuerlich.", "Грейпфрут злегка кислуватий.", "A2"),
        ("Brombeere", "die", "die Brombeeren", "[ˈbʁɔmˌbeːʁə]", "ожина", "blackberry", "Brombeeren reifen im Spätsommer.", "Ожина достигає наприкінці літа.", "A2"),
        ("Stachelbeere", "die", "die Stachelbeeren", "[ˈʃtaxl̩ˌbeːʁə]", "аґрус", "gooseberry", "Stachelbeeren haben feine Säure.", "Аґрус має витончену кислинку.", "B1"),
        ("Aprikose", "die", "die Aprikosen", "[apʁiˈkoːzə]", "абрикос", "apricot", "Aprikosen duften wunderbar.", "Абрикоси чудово пахнуть.", "A2"),
        ("Feige", "die", "die Feigen", "[ˈfaɪ̯ɡə]", "інжир", "fig", "Frische Feigen passen zu Käse.", "Свіжий інжир пасує до сиру.", "B1"),
        ("Granatapfel", "der", "die Granatäpfel", "[ɡʁaˈnaːtˌʔap͡fl̩]", "гранат", "pomegranate", "Granatapfelkerne sind rot.", "Зерна граната червоні.", "B1"),
        ("Mandarine", "die", "die Mandarinen", "[mandaˈʁiːnə]", "мандарин", "mandarin", "Mandarinen gehören zum Winter.", "Мандарини асоціюються із зимою.", "A1"),
        ("Kokosnuss", "die", "die Kokosnüsse", "[ˈkoːkɔsˌnʊs]", "кокос", "coconut", "Die Kokosnuss hat dicke Schale.", "Кокос має товсту шкаралупу.", "A2"),
        ("Avocado", "die", "die Avocados", "[avoˈkaːdo]", "авокадо", "avocado", "Avocado schmeckt auf Toast.", "Авокадо смакує на тості.", "A2"),
        ("Johannisbeere", "die", "die Johannisbeeren", "[joˈhanɪsˌbeːʁə]", "смородина / порічка", "currant", "Rote Johannisbeeren sind sauer.", "Червоні порічки кислі.", "A2"),
        ("Clementine", "die", "die Clementinen", "[klemɛnˈtiːnə]", "клементин", "clementine", "Süße Clementinen ohne Kerne.", "Солодкі клементини без кісточок.", "A2"),
        ("Melone", "die", "die Melonen", "[meˈloːnə]", "диня", "melon", "Honigmelone schmeckt im Sommer.", "Диня чудово смакує влітку.", "A1"),
        ("Preiselbeere", "die", "die Preiselbeeren", "[ˈpʁaɪ̯zl̩ˌbeːʁə]", "брусниця", "lingonberry", "Preiselbeeren passen zu Wild.", "Брусниця підходить до дичини.", "B1"),
        ("Holunderbeere", "die", "die Holunderbeeren", "[hoˈlʊndɐˌbeːʁə]", "бузина", "elderberry", "Aus Holunderbeeren kocht man Saft.", "З ягід бузини варять сік.", "B1")
    ]
    for w in fruits:
        words.append((w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], "food", "food_fruits", w[8]))

    # 2. Vegetables (food_vegetables)
    veggies = [
        ("Tomate", "die", "die Tomaten", "[toˈmaːtə]", "помідор", "tomato", "Ich schneide frische Tomaten.", "Я ріжу свіжі помідори.", "A1"),
        ("Gurke", "die", "die Gurken", "[ˈɡʊʁkə]", "огірок", "cucumber", "Frische Gurken sind knackig.", "Свіжі огірки хрусткі.", "A1"),
        ("Kartoffel", "die", "die Kartoffeln", "[kaʁˈtɔfl̩]", "картопля", "potato", "Wir kochen Kartoffeln mit Salz.", "Ми варимо картоплю із сіллю.", "A1"),
        ("Zwiebel", "die", "die Zwiebeln", "[ˈtsviːbl̩]", "цибуля", "onion", "Zwiebeln braten wir in der Pfanne.", "Цибулю ми смажимо на сковороді.", "A1"),
        ("Karotte", "die", "die Karotten", "[kaˈʁɔtə]", "морква", "carrot", "Frische Karotten sind gesund.", "Свіжа морква корисна.", "A1"),
        ("Knoblauch", "der", "-", "[ˈknoːpˌlaʊ̯x]", "часник", "garlic", "Knoblauch gibt gutes Aroma.", "Часник дає гарний аромат.", "A1"),
        ("Salat", "der", "die Salate", "[zaˈlaːt]", "салат", "salad", "Ein frischer grüner Salat.", "Свіжий зелений салат.", "A1"),
        ("Paprika", "die", "die Paprikas", "[ˈpapʁika]", "болгарський перець", "bell pepper", "Rote Paprika schmeckt süßlich.", "Червоний перець солодкуватий.", "A1"),
        ("Brokkoli", "der", "-", "[ˈbʁɔkoli]", "броколі", "broccoli", "Brokkoli dämpfen wir kurz.", "Броколі ми злегка тушкуємо.", "A2"),
        ("Blumenkohl", "der", "-", "[ˈbluːmənˌkoːl]", "цвітна капуста", "cauliflower", "Gekochter Blumenkohl schmeckt mild.", "Варена цвітна капуста ніжна на смак.", "A2"),
        ("Spinat", "der", "-", "[ʃpiˈnaːt]", "шпинат", "spinach", "Spinat schmeckt mit Eiern.", "Шпинат смакує з яйцями.", "A2"),
        ("Pilz", "der", "die Pilze", "[pɪlts]", "гриб", "mushroom", "Im Herbst sammeln wir Pilze.", "Восени ми збираємо гриби.", "A1"),
        ("Champignon", "der", "die Champignons", "[ˈʃɑ̃pɪnjɔ̃]", "печериця", "white mushroom", "Gebratene Champignons mit Kräutern.", "Смажені печериці із зеленню.", "A2"),
        ("Kohl", "der", "die Kohle", "[koːl]", "капуста", "cabbage", "Aus Weißkohl macht man Salat.", "З білокачанної капусти роблять салат.", "A2"),
        ("Zucchini", "die", "die Zucchini", "[tsuˈkiːni]", "кабачок", "zucchini", "Zucchini grillen wir gerne.", "Кабачок ми охоче смажимо на грилі.", "A2"),
        ("Aubergine", "die", "die Auberginen", "[obɛʁˈʒiːnə]", "баклажан", "eggplant", "Aubergine im Ofen gebacken.", "Баклажан, запечений у духовці.", "A2"),
        ("Erbse", "die", "die Erbsen", "[ˈʔɛʁpsə]", "горох", "pea", "Grüne Erbsen in der Suppe.", "Зелений горошок у супі.", "A2"),
        ("Bohne", "die", "die Bohnen", "[ˈboːnə]", "квасоля", "bean", "Grüne Bohnen mit Speck.", "Зелена квасоля з беконом.", "A2"),
        ("Mais", "der", "-", "[maɪ̯s]", "кукурудза", "corn", "Maiskolben vom Grill schmecken süß.", "Качани кукурудзи з гриля солодкі.", "A2"),
        ("Kürbis", "der", "die Kürbisse", "[ˈkʏʁbɪs]", "гарбуз", "pumpkin", "Heiße Kürbissuppe im Herbst.", "Гарячий гарбузовий суп восени.", "A2"),
        ("Spargel", "der", "-", "[ˈʃpaʁɡl̩]", "спаржа", "asparagus", "Weißer Spargel mit Schinken.", "Біла спаржа з шинкою.", "B1"),
        ("Radieschen", "das", "die Radieschen", "[ʁaˈdiːsçən]", "редиска", "radish", "Kleine rote Radieschen im Salat.", "Маленька червона редиска в салаті.", "A2"),
        ("Rote Bete", "die", "-", "[ˈʁoːtə ˈbeːtə]", "буряк", "beetroot", "Rote Bete ist dunkelrot.", "Буряк темно-червоний.", "A2"),
        ("Sellerie", "der", "-", "[ˈzɛləʁi]", "селера", "celery", "Sellerie für die Brühe.", "Селера для бульйону.", "B1"),
        ("Lauch", "der", "die Lauche", "[laʊ̯x]", "порей", "leek", "Lauchsuppe schmeckt herzhaft.", "Суп із цибулею-пореєм ситний.", "B1"),
        ("Ingwer", "der", "-", "[ˈɪŋvɐ]", "імбир", "ginger", "Ingwertee wärmt im Winter.", "Імбирний чай зігріває взимку.", "A2"),
        ("Artischocke", "die", "die Artischocken", "[aʁtiˈʃɔkə]", "артишок", "artichoke", "Gekochte Artischocke mit Dip.", "Варений артишок із соусом.", "B1"),
        ("Chili", "die", "die Chilis", "[ˈtʃɪli]", "чилі / гострий перець", "chili pepper", "Rote Chilis sind sehr scharf.", "Червоний перець чилі дуже гострий.", "A2"),
        ("Fenchel", "der", "-", "[ˈfɛnçl̩]", "фенхель", "fennel", "Fenchel hat ein feines Anisaroma.", "Фенхель має приємний анісовий аромат.", "B1"),
        ("Rucola", "der", "-", "[ˈʁuːkola]", "рукола", "arugula / rocket", "Rucola passt perfekt auf Pizza.", "Рукола ідеально пасує до піци.", "A2")
    ]
    for w in veggies:
        words.append((w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], "food", "food_vegetables", w[8]))

    # 3. Drinks (food_drinks)
    drinks = [
        ("Kaffee", "der", "die Kaffees", "[ˈkafe]", "кава", "coffee", "Eine Tasse heißer Kaffee am Morgen.", "Чашка гарячої кави вранці.", "A1"),
        ("Tee", "der", "die Tees", "[teː]", "чай", "tea", "Schwarzer Tee mit Zitrone.", "Чорний чай з лимоном.", "A1"),
        ("Wasser", "das", "-", "[ˈvasɐ]", "вода", "water", "Ein Glas kühles Wasser.", "Склянка прохолодної води.", "A1"),
        ("Milch", "die", "-", "[mɪlç]", "молоко", "milk", "Frische Milch vom Bauernhof.", "Свіже молоко з ферми.", "A1"),
        ("Saft", "der", "die Säfte", "[zaft]", "сік", "juice", "Orangensaft schmeckt frisch.", "Апельсиновий сік свіжий.", "A1"),
        ("Bier", "das", "die Biere", "[biːɐ̯]", "пиво", "beer", "Kühles Bier im Biergarten.", "Холодне пиво в пивному саду.", "A1"),
        ("Wein", "der", "die Weine", "[vaɪ̯n]", "вино", "wine", "Ein Glas Rotwein zum Essen.", "Келих червоного вина до їжі.", "A1"),
        ("Mineralwasser", "das", "-", "[mɪneˈʁaːlˌvasɐ]", "мінеральна вода", "mineral water", "Mineralwasser mit Kohlensäure.", "Газована мінеральна вода.", "A1"),
        ("Limonade", "die", "die Limonaden", "[limoˈnaːdə]", "лимонад", "lemonade", "Zitronenlimonade mit Eis.", "Лимонад із льодом.", "A1"),
        ("Kakao", "der", "-", "[kaˈkaːo]", "какао", "hot chocolate", "Heißer Kakao mit Sahne.", "Гаряче какао з вершками.", "A1"),
        ("Sekt", "der", "die Sekte", "[zɛkt]", "ігристе вино", "sparkling wine", "Ein Glas Sekt zum Anstoßen.", "Келих шампанського для тосту.", "A2"),
        ("Cocktail", "der", "die Cocktails", "[ˈkɔkteɪ̯l]", "коктейль", "cocktail", "Fruchtiger Cocktail an der Bar.", "Фруктовий коктейль у барі.", "A2"),
        ("Apfelschorle", "die", "die Apfelschorlen", "[ˈapfl̩ˌʃɔʁlə]", "яблучний шорле", "apple spritzer", "Apfelschorle löscht den Durst.", "Яблучний шорле втамовує спрагу.", "A2"),
        ("Kräutertee", "der", "die Kräutertees", "[ˈkʁɔɪ̯tɐˌteː]", "трав'яний чай", "herbal tea", "Kamillentee beruhigt am Abend.", "Ромашковий чай заспокоює ввечері.", "A2"),
        ("Eistee", "der", "die Eistees", "[ˈaɪ̯sˌteː]", "холодний чай", "iced tea", "Kalter Eistee mit Pfirsich.", "Холодний чай з персиком.", "A1"),
        ("Espresso", "der", "die Espressos", "[ɛsˈpʁɛso]", "еспресо", "espresso", "Ein starker kleiner Espresso.", "Міцне маленьке еспресо.", "A2"),
        ("Cappuccino", "der", "die Cappuccinos", "[kapʊˈtʃiːno]", "капучино", "cappuccino", "Cappuccino mit Milchschaum.", "Капучино з молочною пінкою.", "A1"),
        ("Latte Macchiato", "der", "-", "[ˈlatɛ maˈki̯aːto]", "лате макіато", "latte macchiato", "Latte Macchiato im Glas.", "Лате макіато у високій склянці.", "A2"),
        ("Smoothie", "der", "die Smoothies", "[ˈsmuːði]", "смузі", "smoothie", "Fruchtiger Smoothie aus Beeren.", "Фруктовий смузі з ягід.", "A2"),
        ("Kräuterschnaps", "der", "-", "[ˈkʁɔɪ̯tɐˌʃnaps]", "трав'яний лікер", "herbal liqueur", "Ein Kräuterschnaps nach dem Essen.", "Трав'яний лікер після їди.", "B1"),
        ("Glühwein", "der", "-", "[ˈɡlyːˌvaɪ̯n]", "глінтвейн", "mulled wine", "Heißer Glühwein auf dem Markt.", "Гарячий глінтвейн на ярмарку.", "A2"),
        ("Bierkrug", "der", "die Bierkrüge", "[ˈbiːɐ̯ˌkʁuːk]", "пивний кухоль", "beer mug", "Ein schwerer Bierkrug aus Glas.", "Важкий скляний пивний кухоль.", "A2"),
        ("Weißwein", "der", "die Weißweine", "[ˈvaɪ̯sˌvaɪ̯n]", "біле вино", "white wine", "Kühler Weißwein passt zu Fisch.", "Прохолодне біле вино пасує до риби.", "A2"),
        ("Rotwein", "der", "die Rotweine", "[ˈʁoːtˌvaɪ̯n]", "червоне вино", "red wine", "Trockener Rotwein aus Frankreich.", "Сухе червоне вино з Франції.", "A2"),
        ("Sirup", "der", "die Sirupe", "[ˈziːʁʊp]", "сироп", "syrup", "Himbeersirup mit Sprudelwasser.", "Малиновий сироп із газованою водою.", "B1"),
        ("Buttermilch", "die", "-", "[ˈbʊtɐˌmɪlç]", "маслянка", "buttermilk", "Kalte Buttermilch ist erfrischend.", "Холодна маслянка освіжає.", "B1"),
        ("Leitungswasser", "das", "-", "[ˈlaɪ̯tʊŋsˌvasɐ]", "вода з-під крана", "tap water", "In Deutschland kann man Leitungswasser trinken.", "У Німеччині можна пити воду з-під крана.", "A2"),
        ("Eiswürfel", "der", "die Eiswürfel", "[ˈaɪ̯sˌvʏʁfl̩]", "кубик льоду", "ice cube", "Zwei Eiswürfel ins Glas geben.", "Покласти два кубики льоду в склянку.", "A2"),
        ("Trinkhalm", "der", "die Trinkhalme", "[ˈtʁɪŋkˌhalm]", "соломинка для пиття", "straw", "Ein bunter Trinkhalm für die Limonade.", "Кольорова соломинка для лимонаду.", "A2"),
        ("Kanne", "die", "die Kannen", "[ˈkanə]", "чайник / глечик", "pot / pitcher", "Eine Kanne voller Pfefferminztee.", "Чайник м'ятного чаю.", "A2")
    ]
    for w in drinks:
        words.append((w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], "food", "food_drinks", w[8]))

    # 4. Meals & Bakery (food_meals)
    meals = [
        ("Brot", "das", "die Brote", "[bʁoːt]", "хліб", "bread", "Deutsches Vollkornbrot schmeckt kräftig.", "Німецький цільнозерновий хліб дуже поживний.", "A1"),
        ("Brötchen", "das", "die Brötchen", "[ˈbʁøːtçən]", "булочка", "bread roll", "Knusprige Brötchen zum Frühstück.", "Хрусткі булочки до сніданку.", "A1"),
        ("Käse", "der", "die Käse", "[ˈkɛːzə]", "сир", "cheese", "Ein Stück würziger Gouda-Käse.", "Шматочок пікантного сиру гауда.", "A1"),
        ("Butter", "die", "-", "[ˈbʊtɐ]", "вершкове масло", "butter", "Butter aufs Brot streichen.", "Намастити масло на хліб.", "A1"),
        ("Suppe", "die", "die Suppen", "[ˈzʊpə]", "суп", "soup", "Eine warme Gemüsesuppe schmeckt gut.", "Теплий овочевий суп смачний.", "A1"),
        ("Ei", "das", "die Eier", "[aɪ̯]", "яйце", "egg", "Zwei gekochte Eier zum Frühstück.", "Два варених яйця на сніданок.", "A1"),
        ("Reis", "der", "-", "[ʁaɪ̯s]", "рис", "rice", "Gekochter Reis als Beilage.", "Варений рис як гарнір.", "A1"),
        ("Nudeln", "die", "die Nudeln", "[ˈnuːdl̩n]", "макарони", "pasta / noodles", "Spaghetti-Nudeln mit Tomatensauce.", "Спагеті з томатним соусом.", "A1"),
        ("Frühstück", "das", "die Frühstücke", "[ˈfʁyːˌʃtʏk]", "сніданок", "breakfast", "Ein gesundes Frühstück gibt Kraft.", "Корисний сніданок дає сили.", "A1"),
        ("Mittagessen", "das", "die Mittagessen", "[ˈmɪtaːkˌʔɛsn̩]", "обід", "lunch", "Wir kochen Mittagessen um 13 Uhr.", "Ми готуємо обід о 13:00.", "A1"),
        ("Abendessen", "das", "die Abendessen", "[ˈaːbn̩tˌʔɛsn̩]", "вечеря", "dinner", "Zum Abendessen gibt es Brot und Käse.", "На вечерю хліб та сир.", "A1"),
        ("Marmelade", "die", "die Marmeladen", "[maʁməˈlaːdə]", "джем", "jam", "Süße Erdbeermarmelade.", "Солодкий полуничний джем.", "A1"),
        ("Müsli", "das", "die Müslis", "[ˈmyːsli]", "мюслі", "muesli", "Müsli mit frischer Milch.", "Мюслі зі свіжим молоком.", "A1"),
        ("Joghurt", "der", "die Joghurts", "[ˈjoːɡʊʁt]", "йогурт", "yogurt", "Fruchtjoghurt mit Beeren.", "Фруктовий йогурт з ягодами.", "A1"),
        ("Quark", "der", "-", "[kvaʁk]", "сир кисломолочний", "curd / quark", "Magerquark mit Honig.", "Знежирений сир з медом.", "A2"),
        ("Pizza", "die", "die Pizzas", "[ˈpɪtsa]", "піца", "pizza", "Knusprige Steinofenpizza.", "Хрустка піца з печі.", "A1"),
        ("Auflauf", "der", "die Aufläufe", "[ˈaʊ̯fˌlaʊ̯f]", "запіканка", "casserole", "Nudelauflauf mit Käse überbacken.", "Макаронна запіканка із сиром.", "A2"),
        ("Sandwich", "das", "die Sandwiches", "[ˈzɛntvɪtʃ]", "сендвіч", "sandwich", "Ein Sandwich für unterwegs.", "Сендвіч у дорогу.", "A1"),
        ("Haferflocken", "die", "-", "[ˈhaːfɐˌflɔkn̩]", "вівсяні пластівці", "oat flakes", "Haferflocken mit warmem Wasser.", "Вівсяні пластівці з гарячою водою.", "A2"),
        ("Pfannkuchen", "der", "die Pfannkuchen", "[ˈp͡fanˌkuːxn̩]", "млинець", "pancake", "Dünne Pfannkuchen mit Zucker.", "Тонкі млинці з цукром.", "A1"),
        ("Brezel", "die", "die Brezeln", "[ˈbʁeːtsl̩]", "брецель", "pretzel", "Eine salzige bayerische Brezel.", "Солоний баварський брецель.", "A1"),
        ("Croissant", "das", "die Croissants", "[kʁo̯aˈsɑ̃ː]", "круасан", "croissant", "Buttercroissant aus der Bäckerei.", "Масляний круасан з пекарні.", "A1"),
        ("Toast", "der", "die Toaste", "[toːst]", "тост", "toast", "Gerösteter Toast mit Butter.", "Підсмажений тост з маслом.", "A1"),
        ("Baguette", "das", "die Baguettes", "[baˈɡɛt]", "багет", "baguette", "Frisches französisches Baguette.", "Свіжий французький багет.", "A2"),
        ("Brotlaib", "der", "die Brotlaibe", "[ˈbʁoːtˌlaɪ̯p]", "буханець хліба", "loaf of bread", "Ein ganzer Brotlaib aus Roggen.", "Цілий житній буханець.", "B1"),
        ("Semmel", "die", "die Semmeln", "[ˈzɛml̩]", "булочка (південнонім.)", "roll (bavarian)", "Zwei Semmeln mit Schinken.", "Дві булочки з шинкою.", "A2"),
        ("Kruste", "die", "die Krusten", "[ˈkʁʊstə]", "хрустка скоринка", "crust", "Die knusprige Kruste vom Brot.", "Хрустка хлібна скоринка.", "B1"),
        ("Vollkornbrot", "das", "die Vollkornbrote", "[ˈfɔlˌkɔʁnˌbʁoːt]", "цільнозерновий хліб", "wholegrain bread", "Vollkornbrot ist reich an Ballaststoffen.", "Цільнозерновий хліб багатий на клітковину.", "A2"),
        ("Roggenbrot", "das", "die Roggenbrote", "[ˈʁɔɡn̩ˌbʁoːt]", "житній хліб", "rye bread", "Dunkles Roggenbrot schmeckt würzig.", "Темний житній хліб має пряний смак.", "A2"),
        ("Weißbrot", "das", "die Weißbrote", "[ˈvaɪ̯sˌbʁoːt]", "білий хліб", "white bread", "Weiches Weißbrot zum Frühstück.", "М'який білий хліб до сніданку.", "A1")
    ]
    for w in meals:
        words.append((w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], "food", "food_meals", w[8]))

    # 5. Meat & Fish (food_meat_fish)
    meat_fish = [
        ("Fleisch", "das", "-", "[flaɪ̯ʃ]", "м'ясо", "meat", "Frisches Fleisch vom Metzger.", "Свіже м'ясо від м'ясника.", "A1"),
        ("Fisch", "der", "die Fische", "[fɪʃ]", "риба", "fish", "Frischer Fisch aus der Ostsee.", "Свіжа риба з Балтійського моря.", "A1"),
        ("Hähnchen", "das", "die Hähnchen", "[ˈhɛːnçən]", "курка", "chicken", "Knuspriges gebratenes Hähnchen.", "Хрустка смажена курочка.", "A1"),
        ("Rindfleisch", "das", "-", "[ˈʁɪntˌflaɪ̯ʃ]", "яловичина", "beef", "Zartes Rindfleisch zum Schmoren.", "Ніжна яловичина для тушкування.", "A2"),
        ("Schweinefleisch", "das", "-", "[ˈʃvaɪ̯nəˌflaɪ̯ʃ]", "свинина", "pork", "Schweinefleisch für leckere Schnitzel.", "Свинина для смачних шніцелів.", "A2"),
        ("Wurst", "die", "die Würste", "[vʊʁst]", "ковбаса", "sausage", "Gegrillte Bratwurst mit Senf.", "Смажена ковбаска з гірчицею.", "A1"),
        ("Schinken", "der", "die Schinken", "[ˈʃɪŋkn̩]", "шинка", "ham", "Würziger Schinken aus dem Schwarzwald.", "Пряна шинка зі Шварцвальда.", "A1"),
        ("Lachs", "der", "die Lachse", "[laks]", "лосось", "salmon", "Gebratener Lachs mit Zitrone.", "Смажений лосось з лимоном.", "A2"),
        ("Forelle", "die", "die Forellen", "[foˈʁɛlə]", "форель", "trout", "Forelle blau aus dem Bach.", "Форель зі струмка.", "B1"),
        ("Thunfisch", "der", "die Thunfische", "[ˈtuːnˌfɪʃ]", "тунець", "tuna", "Thunfischsalat mit Zwiebeln.", "Салат з тунцем і цибулею.", "A2"),
        ("Garnelen", "die", "die Garnelen", "[ɡaʁˈneːlən]", "креветки", "prawns / shrimp", "Gebratene Garnelen mit Knoblauch.", "Смажені креветки з часником.", "A2"),
        ("Hackfleisch", "das", "-", "[ˈhakˌflaɪ̯ʃ]", "фарш", "minced meat", "Hackfleisch für Spaghetti Bolognese.", "Фарш для спагеті болоньєзе.", "A2"),
        ("Steak", "das", "die Steaks", "[steːk]", "стейк", "steak", "Ein saftiges Steak vom Rind.", "Соковитий яловичий стейк.", "A2"),
        ("Schnitzel", "das", "die Schnitzel", "[ˈʃnɪtsl̩]", "шніцель", "schnitzel", "Wiener Schnitzel mit Kartoffelsalat.", "Віденський шніцель з картопляним салатом.", "A1"),
        ("Speck", "der", "-", "[ʃpɛk]", "бекон", "bacon", "Gebratener Speck mit Spiegeleiern.", "Смажений бекон з яєчнею.", "A2"),
        ("Salami", "die", "die Salamis", "[zaˈlaːmi]", "салямі", "salami", "Salami auf die Pizza legen.", "Покласти салямі на піцу.", "A1"),
        ("Truthahn", "der", "die Truthähne", "[ˈtʁuːtˌhaːn]", "індичка", "turkey", "Gebratener Truthahn zu Weihnachten.", "Смажена індичка на Різдво.", "B1"),
        ("Ente", "die", "die Enten", "[ˈʔɛntə]", "качка", "duck", "Knusprige Ente mit Rotkohl.", "Хрустка качка з червоною капустою.", "A2"),
        ("Gans", "die", "die Gänse", "[ɡans]", "гуска", "goose", "Die Martinsgans ist ein Festessen.", "Святкова гуска — традиційна страва.", "B1"),
        ("Krabbe", "die", "die Krabben", "[ˈkʁabə]", "краб", "crab", "Nordseekrabben auf Schwarzbrot.", "Краби Північного моря на чорному хлібі.", "B1"),
        ("Muschel", "die", "die Muscheln", "[ˈmʊʃl̩]", "мідія / мушля", "mussel", "Frische Muscheln im Weißweinsud.", "Свіжі мідії у білому вині.", "B1"),
        ("Kabeljau", "der", "die Kabeljaus", "[ˈkaːbl̩ˌjaʊ̯]", "тріска", "cod", "Gebratener Kabeljau mit Remoulade.", "Смажена тріска з соусом ремулад.", "B1"),
        ("Hering", "der", "die Heringe", "[ˈheːʁɪŋ]", "оселедець", "herring", "Marinierter Hering mit Zwiebelringen.", "Маринований оселедець з цибулею.", "A2"),
        ("Kalbfleisch", "das", "-", "[ˈkalpˌflaɪ̯ʃ]", "телятина", "veal", "Zartes Kalbfleisch für feine Gerichte.", "Ніжна телятина для вишуканих страв.", "B1"),
        ("Lammfleisch", "das", "-", "[ˈlamˌflaɪ̯ʃ]", "баранина", "lamb", "Gegrilltes Lammfleisch mit Rosmarin.", "Баранина на грилі з розмарином.", "B1"),
        ("Kotelett", "das", "die Koteletts", "[kɔtˈlɛt]", "відбивна / корейка", "cutlet / chop", "Ein saftiges Kotelett braten.", "Посмажити соковиту відбивну.", "A2"),
        ("Frikadelle", "die", "die Frikadellen", "[fʁikaˈdɛlə]", "котлета", "meatball / patty", "Warme Frikadelle mit Senf.", "Тепла котлета з гірчицею.", "A2"),
        ("Leberwurst", "die", "die Leberwürste", "[ˈleːbɐˌvʊʁst]", "печінкова ковбаса", "liver sausage", "Leberwurst auf frischem Brot.", "Печінкова ковбаса на свіжому хлібі.", "A2"),
        ("Weißwurst", "die", "die Weißwürste", "[ˈvaɪ̯sˌvʊʁst]", "біла ковбаска", "bavarian white sausage", "Bayerische Weißwurst mit süßem Senf.", "Баварська біла ковбаска із солодким гірчичним соусом.", "A2"),
        ("Currywurst", "die", "die Currywürste", "[ˈkœʁiˌvʊʁst]", "каррівурст", "curry sausage", "Currywurst ist typisch für Berlin.", "Каррівурст — типова страва Берліна.", "A1")
    ]
    for w in meat_fish:
        words.append((w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], "food", "food_meat_fish", w[8]))

    # 6. Sweets (food_sweets)
    sweets = [
        ("Schokolade", "die", "die Schokoladen", "[ʃokoˈlaːdə]", "шоколад", "chocolate", "Dunkle Schokolade schmeckt edel.", "Темний шоколад має вишуканий смак.", "A1"),
        ("Kuchen", "der", "die Kuchen", "[ˈkuːxn̩]", "пиріг", "cake", "Apfelkuchen mit Zimt gebacken.", "Яблучний пиріг з корицею.", "A1"),
        ("Torte", "die", "die Torten", "[ˈtɔʁtə]", "торт", "gateau", "Geburtstagstorte mit Kerzen.", "Іменинний торт зі свічками.", "A2"),
        ("Eis", "das", "-", "[aɪ̯s]", "морозиво", "ice cream", "Vanilleeis mit Schokoladensauce.", "Ванільне морозиво з шоколадом.", "A1"),
        ("Zucker", "der", "-", "[ˈtsʊkɐ]", "цукор", "sugar", "Ein Löffel Zucker im Tee.", "Ложка цукру в чаї.", "A1"),
        ("Honig", "der", "-", "[ˈhoːnɪç]", "мед", "honey", "Goldener Bienenhonig schmeckt süß.", "Золотистий бджолиний мед солодкий.", "A1"),
        ("Keks", "der", "die Kekse", "[keːks]", "печиво", "biscuit", "Knusprige Kekse zum Kaffee.", "Хрустке печиво до кави.", "A1"),
        ("Bonbon", "das", "die Bonbons", "[bɔŋˈbɔŋ]", "цукерка", "sweet / candy", "Fruchtige Bonbons für Kinder.", "Фруктові цукерки для дітей.", "A1"),
        ("Waffel", "die", "die Waffeln", "[ˈvafl̩]", "вафля", "waffle", "Warme Waffel mit Puderzucker.", "Тепла вафля з цукровою пудрою.", "A2"),
        ("Praline", "die", "die Pralinen", "[pʁaˈliːnə]", "шоколадна цукерка", "praline", "Eine Schachtel Pralinen schenken.", "Подарувати коробку шоколадних цукерок.", "B1"),
        ("Pudding", "der", "die Puddings", "[ˈpʊdɪŋ]", "пудинг", "pudding", "Schokoladenpudding kochen.", "Варити шоколадний пудинг.", "A2"),
        ("Zimt", "der", "-", "[tsɪmt]", "кориця", "cinnamon", "Zimt duftet nach Weihnachten.", "Кориця пахне Різдвом.", "A2"),
        ("Vanille", "die", "-", "[vaˈnɪljə]", "ваніль", "vanilla", "Echte Vanille ist kostbar.", "Справжня ваніль коштовна.", "A2"),
        ("Gummibärchen", "das", "die Gummibärchen", "[ˈɡʊmiˌbɛːɐ̯çən]", "желатинові ведмедики", "gummy bears", "Gummibärchen sind beliebt.", "Желейні ведмедики дуже популярні.", "A1"),
        ("Lebkuchen", "der", "die Lebkuchen", "[ˈleːpˌkuːxn̩]", "пряник", "gingerbread", "Nürnberger Lebkuchen im Winter.", "Нюрнберзькі пряники взимку.", "A2"),
        ("Marzipan", "das", "-", "[maʁtsiˈpaːn]", "марципан", "marzipan", "Lübecker Marzipan aus Mandeln.", "Любецький марципан з мигдалю.", "B1"),
        ("Strudel", "der", "die Strudel", "[ˈʃtʁuːdl̩]", "струдель", "strudel", "Warmer Wiener Apfelstrudel.", "Теплий віденський яблучний струдель.", "A2"),
        ("Muffin", "der", "die Muffins", "[ˈmafɪn]", "кекс", "muffin", "Blaubeermuffin frisch aus dem Ofen.", "Чорничний кекс щойно з духовки.", "A1"),
        ("Donut", "der", "die Donuts", "[ˈdoːnat]", "пончик", "doughnut", "Süßer Donut mit Glasur.", "Солодкий пончик з глазур'ю.", "A1"),
        ("Karamell", "das", "-", "[kaʁaˈmɛl]", "карамель", "caramel", "Karamell schmeckt süß und cremig.", "Карамель солодка і ніжна.", "A2"),
        ("Nougat", "der", "-", "[ˈnuːɡat]", "нуга", "nougat", "Nuss-Nougat-Creme aufs Brot.", "Горіхова паста з нугою на хліб.", "A2"),
        ("Sahne", "die", "-", "[ˈzaːnə]", "вершки", "cream", "Schlagsahne auf die Torte geben.", "Покласти збиті вершки на торт.", "A1"),
        ("Sirup", "der", "die Sirupe", "[ˈziːʁʊp]", "сироп", "syrup", "Ahornsirup auf Pfannkuchen.", "Кленовий сироп на млинці.", "A2"),
        ("Kaugummi", "der", "die Kaugummis", "[ˈkaʊ̯ɡʊmi]", "жуйка", "chewing gum", "Kaugummi mit Minzgeschmack.", "Жувальна гумка зі смаком м'яти.", "A1"),
        ("Konfitüre", "die", "die Konfitüren", "[kɔnfiˈtyːʁə]", "конфітюр", "preserves", "Aprikosenkonfitüre zum Frühstück.", "Абрикосовий конфітюр до сніданку.", "B1"),
        ("Zuckerwatte", "die", "-", "[ˈtsʊkɐˌvatə]", "солодка вата", "cotton candy", "Zuckerwatte auf dem Jahrmarkt.", "Солодка вата на ярмарку.", "A2"),
        ("Schokoriegel", "der", "die Schokoriegel", "[ˈʃoːkoˌʁiːɡl̩]", "шоколадний батончик", "chocolate bar", "Ein Schokoriegel für die Pause.", "Шоколадний батончик на перекус.", "A1"),
        ("Teegebäck", "das", "-", "[ˈteːɡəˌbɛk]", "чайне печиво", "tea biscuits", "Feines Teegebäck servieren.", "Подавати вишукане чайне печиво.", "B1"),
        ("Baiser", "das", "die Baisers", "[bɛˈzeː]", "безе", "meringue", "Luftiges Baiser aus Eiweiß.", "Повітряне безе з яєчного білка.", "B1"),
        ("Puderzucker", "der", "-", "[ˈpuːdɐˌtsʊkɐ]", "цукрова пудра", "powdered sugar", "Puderzucker über den Kuchen streuen.", "Посипати торт цукровою пудрою.", "A2")
    ]
    for w in sweets:
        words.append((w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], "food", "food_sweets", w[8]))

    # 7. Restaurant (food_restaurant)
    restaurant = [
        ("Speisekarte", "die", "die Speisekarten", "[ˈʃpaɪ̯zəˌkaʁtə]", "меню ресторану", "menu", "Könnten wir bitte die Speisekarte sehen?", "Чи можемо ми побачити меню?", "A1"),
        ("Rechnung", "die", "die Rechnungen", "[ˈʁɛçnʊŋ]", "рахунок", "bill", "Zusammen oder getrennt bezahlen?", "Разом чи окремо платитимете?", "A1"),
        ("Kellner", "der", "die Kellner", "[ˈkɛlnɐ]", "офіціант", "waiter", "Der Kellner bringt das Essen.", "Офіціант приносить їжу.", "A1"),
        ("Kellnerin", "die", "die Kellnerinnen", "[ˈkɛlnəʁɪn]", "офіціантка", "waitress", "Die Kellnerin nimmt die Bestellung auf.", "Офіціантка приймає замовлення.", "A1"),
        ("Trinkgeld", "das", "die Trinkgelder", "[ˈtʁɪŋkˌɡɛlt]", "чайові", "tip", "Wir geben zehn Prozent Trinkgeld.", "Ми даємо десять відсотків чайових.", "A2"),
        ("Tisch", "der", "die Tische", "[tɪʃ]", "стіл", "table", "Einen Tisch für zwei reservieren.", "Забронювати стіл на двох.", "A1"),
        ("Bestellung", "die", "die Bestellungen", "[bəˈʃtɛlʊŋ]", "замовлення", "order", "Ihre Bestellung kommt sofort.", "Ваше замовлення зараз принесуть.", "A2"),
        ("Vorspeise", "die", "die Vorspeisen", "[ˈfoːɐ̯ˌʃpaɪ̯zə]", "закуска", "starter", "Eine leichte Suppe als Vorspeise.", "Легкий суп як закуска.", "A2"),
        ("Hauptgericht", "das", "die Hauptgerichte", "[ˈhaʊ̯ptɡəˌʁɪçt]", "основна страва", "main dish", "Das Hauptgericht schmeckt ausgezeichnet.", "Основна страва смакує чудово.", "A2"),
        ("Nachtisch", "der", "die Nachtische", "[ˈnaːxˌtɪʃ]", "десерт", "dessert", "Zum Nachtisch gibt es Eis.", "На десерт морозиво.", "A1"),
        ("Guten Appetit", "", "-", "[ˌɡuːtn̩ ʔapeˈtiːt]", "смачного", "enjoy your meal", "Guten Appetit allerseits!", "Смачного всім!", "A1"),
        ("Bedienung", "die", "-", "[bəˈdiːnʊŋ]", "обслуговування", "service", "Die Bedienung ist sehr freundlich.", "Обслуговування дуже привітне.", "A2"),
        ("Gast", "der", "die Gäste", "[ɡast]", "гість / відвідувач", "guest", "Viele Gäste sitzen auf der Terrasse.", "Багато гостей сидить на терасі.", "A1"),
        ("Reservierung", "die", "die Reservierungen", "[ʁezɛʁˈviːʁʊŋ]", "бронювання", "reservation", "Haben Sie eine Reservierung?", "У вас є бронювання?", "A2"),
        ("Lokal", "das", "die Lokale", "[loˈkaːl]", "заклад / ресторан", "pub / restaurant", "Ein gemütliches Lokal im Zentrum.", "Затишний заклад у центрі міста.", "A2"),
        ("Gasthaus", "das", "die Gasthäuser", "[ˈɡastˌhaʊ̯s]", "корчма / готель", "inn / tavern", "Ein traditionelles bayerisches Gasthaus.", "Традиційна баварська корчма.", "A2"),
        ("Stammgast", "der", "die Stammgäste", "[ˈʃtamˌɡast]", "постійний відвідувач", "regular guest", "Er ist Stammgast in diesem Café.", "Він постійний гість у цій кав'ярні.", "B1"),
        ("Biergarten", "der", "die Biergärten", "[ˈbiːɐ̯ˌɡaʁtn̩]", "пивний сад", "beer garden", "Im Biergarten unter Kastanienbäumen.", "У пивному саду під каштанами.", "A1"),
        ("Tagesgericht", "das", "die Tagesgerichte", "[ˈtaːɡəsɡəˌʁɪçt]", "страва дня", "dish of the day", "Das Tagesgericht ist heute Lachs.", "Сьогодні страва дня — лосось.", "A2"),
        ("Trinkbecher", "der", "die Trinkbecher", "[ˈtʁɪŋkˌbɛçɐ]", "стаканчик", "cup", "Ein Becher zum Mitnehmen.", "Стаканчик із напоєм із собою.", "A1"),
        ("Zahnstocher", "der", "die Zahnstocher", "[ˈtsaːnˌʃtɔxɐ]", "зубочистка", "toothpick", "Zahnstocher stehen auf dem Tisch.", "Зубочистки стоять на столі.", "A2"),
        ("Aschenbecher", "der", "die Aschenbecher", "[ˈaʃn̩ˌbɛçɐ]", "попільничка", "ashtray", "Der Aschenbecher steht draußen.", "Попільничка стоїть надворі.", "A2"),
        ("Garderobe", "die", "die Garderoben", "[ɡaʁdəˈʁoːbə]", "гардероб", "cloakroom", "Mäntel an der Garderobe abgeben.", "Здати пальта в гардероб.", "A2"),
        ("Terrasse", "die", "die Terrassen", "[tɛˈʁasə]", "тераса", "terrace", "Draußen auf der Terrasse sitzen.", "Сидіти надворі на терасі.", "A2"),
        ("Besteck", "das", "die Bestecke", "[bəˈʃtɛk]", "столові прибори", "cutlery", "Silbernes Besteck auflegen.", "Розкласти срібні столові прибори.", "A1")
    ]
    for w in restaurant:
        words.append((w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], "food", "food_restaurant", w[8]))

    # 8. Cooking (food_cooking)
    cooking = [
        ("Salz", "das", "-", "[zalts]", "сіль", "salt", "Etwas Salz ins Wasser geben.", "Додати трохи солі у воду.", "A1"),
        ("Pfeffer", "der", "-", "[ˈp͡fɛfɐ]", "перець", "pepper", "Frisch gemahlener Pfeffer.", "Свіжозмелений перець.", "A1"),
        ("Öl", "das", "die Öle", "[øːl]", "олія", "oil", "Olivenöl für den Salat.", "Оливкова олія для салату.", "A1"),
        ("Essig", "der", "-", "[ˈɛsɪç]", "оцет", "vinegar", "Ein Schuss Essig ins Dressing.", "Крапля оцту в заправку.", "A2"),
        ("Mehl", "das", "-", "[meːl]", "борошно", "flour", "Weizenmehl zum Backen.", "Пшеничне борошно для випікання.", "A1"),
        ("Hefe", "die", "-", "[ˈheːfə]", "дріжджі", "yeast", "Frische Hefe für den Teig.", "Свіжі дріжджі для тіста.", "A2"),
        ("Kräuter", "die", "-", "[ˈkʁɔɪ̯tɐ]", "пряні трави", "herbs", "Frische Kräuter wie Basilikum.", "Свіжа зелень, як-от базилік.", "A2"),
        ("Pfanne", "die", "die Pfannen", "[ˈp͡fanə]", "сковорода", "pan", "Die Pfanne auf den Herd stellen.", "Поставити сковороду на плиту.", "A1"),
        ("Topf", "der", "die Töpfe", "[tɔp͡f]", "каструля", "pot", "Ein großer Topf für Suppe.", "Велика каструля для супу.", "A1"),
        ("Rezept", "das", "die Rezepte", "[ʁeˈtsɛpt]", "рецепт", "recipe", "Ein traditionelles Familienrezept.", "Традиційний сімейний рецепт.", "A2"),
        ("Zutat", "die", "die Zutaten", "[ˈtsuːˌtaːt]", "інгредієнт", "ingredient", "Alle Zutaten bereitstellen.", "Підготувати всі інгредієнти.", "A2"),
        ("kochen", "", "-", "[ˈkɔxn̩]", "готувати / варити", "to cook", "Wir kochen heute gemeinsam.", "Ми сьогодні готуємо разом.", "A1"),
        ("backen", "", "-", "[ˈbakn̩]", "випікати", "to bake", "Brot im Ofen backen.", "Пекти хліб у духовці.", "A1"),
        ("braten", "", "-", "[ˈbʁaːtn̩]", "смажити", "to fry", "Fleisch in der Pfanne braten.", "Смажити м'ясо на сковороді.", "A2"),
        ("schneiden", "", "-", "[ˈʃnaɪ̯dn̩]", "різати", "to cut", "Zwiebeln fein schneiden.", "Дрібно нарізати цибулю.", "A1"),
        ("schälen", "", "-", "[ˈʃɛːlən]", "чистити від шкірки", "to peel", "Kartoffeln mit dem Messer schälen.", "Чистити картоплю ножем.", "A2"),
        ("rühren", "", "-", "[ˈʁyːʁən]", "перемішувати", "to stir", "Die Sauce kräftig umrühren.", "Добре перемішати соус.", "A2"),
        ("mischen", "", "-", "[ˈmɪʃn̩]", "змішувати", "to mix", "Zutaten in der Schüssel mischen.", "Змішати інгредієнти в мисці.", "A1"),
        ("würzen", "", "-", "[ˈvʏʁtsn̩]", "приправляти", "to season", "Mit Salz und Pfeffer würzen.", "Приправити сіллю та перцем.", "A2"),
        ("servieren", "", "-", "[zɛʁˈviːʁən]", "подавати до столу", "to serve", "Das Essen warm servieren.", "Подавати їжу гарячою.", "A2"),
        ("Teig", "der", "die Teige", "[taɪ̯k]", "тісто", "dough", "Den Teig zehn Minuten kneten.", "Місити тісто десять хвилин.", "A2"),
        ("Sieb", "das", "die Siebe", "[ziːp]", "сито / друшляк", "sieve / colander", "Nudeln durch das Sieb abgießen.", "Відцідити макарони через друшляк.", "A2"),
        ("Reibe", "die", "die Reiben", "[ˈʁaɪ̯bə]", "терка", "grater", "Käse mit der Reibe raspeln.", "Натерти сир на тертці.", "B1"),
        ("Schürze", "die", "die Schürzen", "[ˈʃʏʁtsə]", "фартух", "apron", "Eine Schürze beim Kochen tragen.", "Одягати фартух під час готування.", "A2"),
        ("Deckel", "der", "die Deckel", "[ˈdɛkl̩]", "кришка", "lid", "Den Deckel auf den Topf setzen.", "Накрити каструлю кришкою.", "A1")
    ]
    for w in cooking:
        words.append((w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], "food", "food_cooking", w[8]))

    return words

print("get_food_words ready, count:", len(get_food_words()))
