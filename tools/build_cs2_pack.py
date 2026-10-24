"""Build community/packs/cs2.json from ByMykel CSGO-API (MIT): https://github.com/ByMykel/CSGO-API
images come from the steam cdn or the counter-strike-image-tracker mirror of the game files

usage: python tools/build_cs2_pack.py <dir with base_weapons.json>
"""
from pathlib import Path
import json
import sys

weapons = json.loads((Path(sys.argv[1]) / "base_weapons.json").read_text(encoding="utf-8"))

t_only = {"Glock-18", "Tec-9", "Galil AR", "AK-47", "SG 553", "G3SG1", "MAC-10", "Sawed-Off", "Molotov"}
ct_only = {"USP-S", "P2000", "Five-SeveN", "FAMAS", "M4A4", "M4A1-S", "AUG", "SCAR-20", "MP9", "MAG-7",
           "Incendiary Grenade"}
slots = {"Pistols": "pistol", "Rifles": "primary", "SMGs": "primary", "Heavy": "primary", "Grenades": "grenade"}

tags = [{"name": "Terrorists", "slot": "side"}, {"name": "Counter-Terrorists", "slot": "side"}]
seen = set()
for weapon in sorted(weapons, key=lambda w: w["name"]):
    slot = slots.get(weapon["category"]["name"])
    if not slot or weapon["name"] in seen:
        continue
    seen.add(weapon["name"])
    tag = {"name": weapon["name"], "slot": slot, "image": weapon["image"]}
    if weapon["name"] in t_only:
        tag["requires"] = ["Terrorists"]
    elif weapon["name"] in ct_only:
        tag["requires"] = ["Counter-Terrorists"]
    tags.append(tag)
tags.extend({"name": name, "slot": "map"} for name in
            ["Ancient", "Anubis", "Dust II", "Inferno", "Mirage", "Nuke", "Overpass", "Train", "Vertigo"])

pack = {"library": {
    "id": "cs2",
    "name": "Counter-Strike 2",
    "version": 1,
    "slots": [
        {"id": "side", "name": "Side", "icon": "🛡️"},
        {"id": "map", "name": "Map", "icon": "🗺️"},
        {"id": "pistol", "name": "Pistol", "icon": "🔫", "parent": "side"},
        {"id": "primary", "name": "Primary", "icon": "🎯", "parent": "side"},
        {"id": "grenade", "name": "Grenade", "icon": "💣", "parent": "side"},
    ],
    "reels": ["side", "map", "pistol", "primary", "grenade", "grenade"],
    "standaloneTags": tags,
}, "source_url": "https://github.com/ByMykel/CSGO-API"}
out = Path(__file__).resolve().parents[1] / "community/packs/cs2.json"
out.write_text(json.dumps(pack, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"{out}: {len(seen)} weapons, {len(tags)} tags")
