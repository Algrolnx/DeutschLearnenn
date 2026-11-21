import os

target = "app/src/main/java/com/example/data/vocabulary/TestVocab.kt"
with open(target, "w", encoding="utf-8") as f:
    f.write("// test\n")
print("Target writable:", os.path.exists(target))
os.remove(target)
