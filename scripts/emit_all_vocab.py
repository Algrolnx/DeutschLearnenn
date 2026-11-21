# -*- coding: utf-8 -*-
import sys
import os
sys.path.append('scripts')

import vocab_food, vocab_travel, data_home, cat_batch1, cat_batch2, data_batch3
import vocab_body, vocab_hobbies, vocab_work, vocab_tech, vocab_education, vocab_shopping
import vocab_society, vocab_emotions, vocab_verbs, vocab_adjectives
from helper import emit_kotlin_file

output_dir = "app/src/main/java/com/example/data/vocabulary"
os.makedirs(output_dir, exist_ok=True)

files_map = [
    (f"{output_dir}/FoodVocabulary.kt", "FoodVocabulary", vocab_food.get_words()),
    (f"{output_dir}/TravelVocabulary.kt", "TravelVocabulary", vocab_travel.TRAVEL_WORDS),
    (f"{output_dir}/HomeVocabulary.kt", "HomeVocabulary", data_home.get_home_words()),
    (f"{output_dir}/PeopleVocabulary.kt", "PeopleVocabulary", cat_batch1.get_people_words()),
    (f"{output_dir}/CityVocabulary.kt", "CityVocabulary", cat_batch2.get_city_words()),
    (f"{output_dir}/NatureVocabulary.kt", "NatureVocabulary", data_batch3.get_nature_words()),
    (f"{output_dir}/BodyVocabulary.kt", "BodyVocabulary", vocab_body.BODY_WORDS),
    (f"{output_dir}/HobbiesVocabulary.kt", "HobbiesVocabulary", vocab_hobbies.HOBBIES_WORDS),
    (f"{output_dir}/WorkVocabulary.kt", "WorkVocabulary", vocab_work.WORK_WORDS),
    (f"{output_dir}/TechVocabulary.kt", "TechVocabulary", vocab_tech.TECH_WORDS),
    (f"{output_dir}/EducationVocabulary.kt", "EducationVocabulary", vocab_education.EDUCATION_WORDS),
    (f"{output_dir}/ShoppingVocabulary.kt", "ShoppingVocabulary", vocab_shopping.SHOPPING_WORDS),
    (f"{output_dir}/SocietyVocabulary.kt", "SocietyVocabulary", vocab_society.SOCIETY_WORDS),
    (f"{output_dir}/EmotionsVocabulary.kt", "EmotionsVocabulary", vocab_emotions.EMOTIONS_WORDS),
    (f"{output_dir}/VerbsVocabulary.kt", "VerbsVocabulary", vocab_verbs.VERBS_WORDS),
    (f"{output_dir}/AdjectivesVocabulary.kt", "AdjectivesVocabulary", vocab_adjectives.ADJECTIVES_WORDS)
]

for target_file, class_name, words in files_map:
    emit_kotlin_file(target_file, class_name, words)

print("All 16 Vocabulary Kotlin files emitted successfully!")
