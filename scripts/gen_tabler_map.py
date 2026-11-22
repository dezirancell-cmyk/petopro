#!/usr/bin/env python3
"""نقشه‌ی «نام آیکون ← کدنقطه‌ی یونیکد» را از CSS رسمی Tabler (همان نسخه‌ی وب) می‌سازد.
استفاده: gen_tabler_map.py tabler-icons.min.css app/src/main/assets/tabler-map.json"""
import json, re, sys

css = open(sys.argv[1], encoding='utf-8').read()
found = re.findall(r'\.ti-([a-z0-9-]+):before\s*\{\s*content:\s*"\\([0-9a-fA-F]+)"', css)
mapping = {name: int(code, 16) for name, code in found}
if not mapping:
    sys.exit('هیچ آیکونی در CSS پیدا نشد')
with open(sys.argv[2], 'w', encoding='utf-8') as f:
    json.dump(mapping, f)
print('icons:', len(mapping))
