#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generates 16 vocabulary files (4,000 words), TopicHierarchy.kt, InitialVocabulary.kt, and WordImageProvider.kt
"""
import os

VOCAB_DIR = "app/src/main/java/com/example/data/vocabulary"
MODEL_DIR = "app/src/main/java/com/example/data/model"
REPO_DIR = "app/src/main/java/com/example/data/repository"
UTIL_DIR = "app/src/main/java/com/example/util"

print("Builder initialized")
