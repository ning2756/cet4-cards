# -*- coding: utf-8 -*-
"""从 docx 抽取卡片数据 -> cards.json（en/zh/ex/cat）"""
import json, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from docx import Document

SRC = r"D:\DeepSeekharness工作区\004-通用\四级真题高频搭配与句式精析.docx"
OUT = r"D:\DeepSeekharness工作区\004-通用\cards.json"

d = Document(SRC)

def clean(s):
    s = (s or "").replace("\u00a0", " ").replace("\n", " ")
    s = re.sub(r"\s+", " ", s).strip()
    return s

def rows(i, skip_header=True):
    tb = d.tables[i]
    out = []
    for r in tb.rows:
        cells = [clean(c.text) for c in r.cells]
        if not any(cells):
            continue
        out.append(cells)
    return out[1:] if skip_header else out

cards = []
seen = set()

def add(en, zh, ex, cat):
    en, zh, ex = clean(en), clean(zh), clean(ex)
    if not en or not zh:
        return
    key = (en.lower(), zh.lower())
    if key in seen:
        return
    seen.add(key)
    cards.append({"en": en, "zh": zh, "ex": ex, "cat": cat})

# ---- 第一章 160 条搭配 ----
for ti, cat in [(3, "动词+介词"), (4, "动宾搭配"), (5, "形容词+介词"), (6, "名词+介词")]:
    for c in rows(ti):
        if len(c) >= 3:
            add(c[0], c[1], c[2], cat)

# ---- 第二章 介词短语 48 ----
for c in rows(7):
    if len(c) >= 3:
        add(c[0], c[1], c[2], "介词短语")

# ---- 2.3 易混介词短语 6 ----
for c in rows(8):
    if len(c) >= 2:
        add(c[0], c[1], "", "易混辨析")

# ---- 第三章 非谓语 ----
for c in rows(9):          # 3.0 三种形式分工
    if len(c) >= 3:
        add(c[0], c[1] + "；功能：" + c[2], "", "非谓语")
for c in rows(10):         # 高频动名词句型
    if len(c) >= 3:
        add(c[0], c[1], c[2], "非谓语")
for c in rows(11):         # doing vs to do
    if len(c) >= 3:
        add(c[0], c[1], c[2], "非谓语")
for c in rows(12):         # 分词化插入语
    if len(c) >= 3:
        add(c[0], c[1], "用法：" + c[2], "非谓语")
for c in rows(13):         # 非谓语高频固定结构
    if len(c) >= 3:
        add(c[0], c[1], c[2], "非谓语")

# ---- 第四章 从句与特殊句式 ----
for c in rows(15):         # 状语从句九类
    if len(c) >= 3:
        add(c[1], "引导" + c[0] + "状语从句", c[2], "从句句式")
for c in rows(16):         # 比较结构
    if len(c) >= 3:
        add(c[0], c[1], c[2], "从句句式")
for c in rows(17):         # rather than 三兄弟
    if len(c) >= 3:
        add(c[0], c[1] + "；" + c[2], "", "从句句式")
for c in rows(18):         # 虚拟语气
    if len(c) >= 3:
        add(c[1], c[0] + "的虚拟语气", c[2], "从句句式")

# ---- 第五章 写作模板 24 ----
for ti in (19, 20, 21, 22):
    for c in rows(ti):
        if len(c) >= 3:
            add(c[1], "用途：" + c[2], "", "写作模板")

# ---- 第六章 翻译 ----
for c in rows(23):         # 37 核心词块（中文 / 英文 / 提示）
    if len(c) >= 3:
        add(c[1], c[0], c[2], "翻译词块")
for c in rows(24):         # 14 母句型（中文句式 / 英文母句 / 真题应用）
    if len(c) >= 3:
        zh = re.sub(r"^\d+\.\s*", "", c[0])
        add(c[1], zh, c[2], "翻译母句")

# ---- 附录B 易混辨析 15 ----
for c in rows(27):
    if len(c) >= 3:
        add(c[0], c[1], c[2], "易混辨析")

# 过滤明显异常
cards = [c for c in cards if len(c["en"]) >= 2 and len(c["zh"]) >= 2]

# 分类排序：按上面出现顺序
order = ["动词+介词", "动宾搭配", "形容词+介词", "名词+介词", "介词短语", "非谓语",
         "从句句式", "写作模板", "翻译词块", "翻译母句", "易混辨析"]
cards.sort(key=lambda c: order.index(c["cat"]) if c["cat"] in order else 99)

with open(OUT, "w", encoding="utf-8") as f:
    json.dump(cards, f, ensure_ascii=False, separators=(",", ":"))

from collections import Counter
cnt = Counter(c["cat"] for c in cards)
print("TOTAL", len(cards))
for k, v in cnt.items():
    print(f"  {k}\t{v}")
print("with ex:", sum(1 for c in cards if c["ex"]))
print("sample:", json.dumps(cards[0], ensure_ascii=False)[:160])