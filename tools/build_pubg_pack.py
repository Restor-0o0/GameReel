"""Build community/packs/pubg.json from the official PUBG api-assets: https://github.com/pubg/api-assets

usage: python tools/build_pubg_pack.py <dir with itemId.json, mapName.json and tree.json (git tree of api-assets)>
"""
from pathlib import Path
import json
import sys

source = Path(sys.argv[1])
load = lambda name: json.loads((source / name).read_text(encoding="utf-8"))
names, map_names, tree = load("itemId.json"), load("mapName.json"), load("tree.json")["tree"]
base = "https://raw.githubusercontent.com/pubg/api-assets/master/"

folders = {"Assets/Item/Weapon/Main/": "primary", "Assets/Item/Weapon/Handgun/": "sidearm",
           "Assets/Item/Equipment/Throwable/": "throwable"}
skipped = {"Item_Weapon_FlareGun_C", "Item_Weapon_M79_C", "Item_Weapon_Mortar_C", "Item_Weapon_PanzerFaust100M_C",
           "Item_Weapon_Apple_C", "Item_Weapon_Rock_C", "Item_Weapon_Snowball_C", "Item_Weapon_C4_C",
           "Item_Weapon_SpikeTrap_C", "Item_Weapon_BluezoneGrenade_C"}
extra = {"Item_Weapon_FAMASG2_C": "FAMAS"}

tags, seen = [], set()
for entry in sorted(tree, key=lambda e: e["path"]):
    path = entry["path"]
    folder = next((f for f in folders if path.startswith(f)), None)
    if not folder or not path.endswith("_C.png"):
        continue
    key = path[len(folder):-4]
    name = names.get(key) or extra.get(key)
    if not name or key in skipped or name in seen:
        continue
    seen.add(name)
    tags.append({"name": name, "slot": folders[folder], "image": base + path})
tags.sort(key=lambda t: (t["slot"], t["name"]))
map_names["Baltic_Main"] = "Erangel"
tags.extend({"name": name, "slot": "map"} for key, name in sorted(map_names.items(), key=lambda p: p[1])
            if key not in ("Range_Main", "Erangel_Main"))
tags.extend({"name": name, "slot": "drop"} for name in
            ["Hot drop", "Edge of the map", "Military base", "Small town", "Lone compound", "Follow the plane"])

pack = {"library": {
    "id": "pubg",
    "name": "PUBG: Battlegrounds",
    "version": 1,
    "slots": [
        {"id": "map", "name": "Map", "icon": "🗺️"},
        {"id": "drop", "name": "Drop", "icon": "🪂"},
        {"id": "primary", "name": "Primary", "icon": "🎯"},
        {"id": "sidearm", "name": "Sidearm", "icon": "🔫"},
        {"id": "throwable", "name": "Throwable", "icon": "💣"},
    ],
    "reels": ["map", "drop", "primary", "primary", "sidearm", "throwable"],
    "standaloneTags": tags,
}, "source_url": "https://github.com/pubg/api-assets"}
out = Path(__file__).resolve().parents[1] / "community/packs/pubg.json"
out.write_text(json.dumps(pack, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"{out}: {len(tags)} tags")
