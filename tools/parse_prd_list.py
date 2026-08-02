import json
import re

path = r"C:\Users\EDY\AppData\Local\Temp\trae\toolcall-output\7cd4f903-6466-4186-a7d9-f56a35ff228c.txt"

with open(path, "r", encoding="utf-8", errors="ignore") as f:
    raw = f.read()

# Extract JSON array/object from