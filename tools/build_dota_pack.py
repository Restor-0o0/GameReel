"""Build community/packs/dota2.json from OpenDota dotaconstants (MIT): https://github.com/odota/dotaconstants

usage: python tools/build_dota_pack.py <dir with heroes.json, items.json, hero_abilities.json, abilities.json>
"""
from pathlib import Path
import json
import sys

source = Path(sys.argv[1])
load = lambda name: json.loads((source / name).read_text(encoding="utf-8"))
heroes, items, hero_abilities, abilities = (load(f"{n}.json") for n in ("heroes", "items", "hero_abilities", "abilities"))

boots = {"Power Treads", "Phase Boots", "Arcane Boots", "Tranquil Boots", "Boots of Travel",
         "Guardian Greaves", "Boots of Bearing"}
skipped = {"Aghanim's Blessing", "Boots of Travel 2", "Crystalys", "Oblivion Staff", "Perseverance",
           "Soul Booster", "Trident"}
levels = {1: "talent10", 2: "talent15", 3: "talent20", 4: "talent25"}

tags = []
names = {}
for hero in sorted(heroes.values(), key=lambda h: h["localized_name"]):
    names[hero["name"]] = hero["localized_name"]
    tags.append({"name": hero["localized_name"], "slot": "hero"})
for key, entry in sorted(hero_abilities.items()):
    if key not in names:
        continue
    for talent in entry.get("talents", []):
        label = abilities.get(talent["name"], {}).get("dname", "").strip()
        if label and "{" not in label and talent["level"] in levels:
            tags.append({"name": label, "slot": levels[talent["level"]], "requires": [names[key]]})

item_names = set()
for key, item in items.items():
    name = item.get("dname", "").strip()
    if not name or key.startswith("recipe") or name in skipped:
        continue
    if name in boots or name == "Blink Dagger":
        item_names.add(name)
    elif item.get("created") and item.get("qual") != "consumable" and (item.get("cost") or 0) >= 1000:
        item_names.add(name)
for name in sorted(item_names):
    tags.append({"name": name, "slot": "boots" if name in boots else "item"})
tags.extend({"name": name, "slot": "position"} for name in
            ["Position 1 · Carry", "Position 2 · Mid", "Position 3 · Offlane",
             "Position 4 · Soft support", "Position 5 · Hard support"])

pack = {"library": {
    "id": "dota2",
    "name": "Dota 2",
    "version": 1,
    "slots": [
        {"id": "hero", "name": "Hero", "icon": "🦸"},
        {"id": "position", "name": "Position", "icon": "🧭"},
        {"id": "boots", "name": "Boots", "icon": "👢"},
        {"id": "item", "name": "Item", "icon": "🗡️"},
        {"id": "talent10", "name": "Talent 10", "icon": "⭐", "parent": "hero"},
        {"id": "talent15", "name": "Talent 15", "icon": "⭐", "parent": "hero"},
        {"id": "talent20", "name": "Talent 20", "icon": "⭐", "parent": "hero"},
        {"id": "talent25", "name": "Talent 25", "icon": "⭐", "parent": "hero"},
    ],
    "reels": ["hero", "boots", "item", "item", "item", "item", "item",
              "talent10", "talent15", "talent20", "talent25"],
    "standaloneTags": tags,
}, "source_url": "https://github.com/odota/dotaconstants"}
out = Path(__file__).resolve().parents[1] / "community/packs/dota2.json"
out.write_text(json.dumps(pack, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"{out}: {len(heroes)} heroes, {len(item_names)} items, {len(tags)} tags")
