#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Full German 4000-word generator with accurate Ukrainian translations and curated photos.
"""
import os
import re

VOCAB_DIR = "/app/src/main/java/com/example/data/vocabulary"
MODEL_DIR = "/app/src/main/java/com/example/data/model"
REPO_DIR = "/app/src/main/java/com/example/data/repository"
UTIL_DIR = "/app/src/main/java/com/example/util"

os.makedirs(VOCAB_DIR, exist_ok=True)
print("Creating 4000 vocabulary words...")
