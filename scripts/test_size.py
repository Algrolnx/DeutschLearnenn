# Verify python capacity
words = []
for i in range(4000):
    words.append(("Wort" + str(i), "das", "die Wörter", "[vɔʁt]", "слово", "word", "Beispielsatz", "Приклад", "cat", "sub", "A1"))
print(f"Generated {len(words)} words successfully")
