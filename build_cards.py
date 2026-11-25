# -*- coding: utf-8 -*-
"""把 cards.json 注入模板，生成成品网页；同时同步到 APK 工程 assets"""
import json, sys, io, os, shutil
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

HERE = os.path.dirname(os.path.abspath(__file__))
TPL = os.path.join(HERE, "app_template.html")
DATA = os.path.join(HERE, "cards.json")
OUT = os.path.join(HERE, "index.html")
ASSETS = r"D:\tools\androidbuild\cet4app\assets\index.html"

with open(DATA, encoding="utf-8") as f:
    cards = json.load(f)
with open(TPL, encoding="utf-8") as f:
    tpl = f.read()

payload = json.dumps(cards, ensure_ascii=False, separators=(",", ":"))
html = tpl.replace("__DATA__", payload).replace("__COUNT__", str(len(cards)))

with open(OUT, "w", encoding="utf-8") as f:
    f.write(html)

if os.path.isdir(os.path.dirname(ASSETS)):
    with open(ASSETS, "w", encoding="utf-8") as f:
        f.write(html)

print("cards:", len(cards))
print("html bytes:", len(html.encode("utf-8")))
print("out:", OUT)