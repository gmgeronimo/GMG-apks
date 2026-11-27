"""Converte as fontes do @fontsource (woff2) para TTF na pasta res/font do app.
Uso: python3 tools/fontes.py <pasta node_modules/@fontsource> <pasta res/font>"""
import sys, os
from fontTools.ttLib import TTFont

src, dst = sys.argv[1], sys.argv[2]
os.makedirs(dst, exist_ok=True)
WEIGHTS = {"mona-sans": (300, 500, 600, 700), "nunito": (500, 600, 700, 800)}
for fam, weights in WEIGHTS.items():
    for w in weights:
        f = TTFont(f"{src}/{fam}/files/{fam}-latin-{w}-normal.woff2")
        f.flavor = None
        f.save(f"{dst}/{fam.replace('-', '_')}_{w}.ttf")
        print("ok", fam, w)
