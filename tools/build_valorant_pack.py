"""Build community/packs/valorant.json from valorant-api.com: https://valorant-api.com

usage: python tools/build_valorant_pack.py <dir with agents.json, weapons.json, maps.json>
"""
from pathlib import Path
import json
import sys

source = Path(sys.argv[1])
load = lambda name: json.loads((source / name).read_text(encoding="utf-8"))["data"]
agents, weapons, maps = (load(f"{n}.json") for n in ("agents", "weapons", "maps"))

tags = [{"name": a["displayName"], "slot": "agent", "image": a["displayIcon"]}
        for a in sorted(agents, key=lambda a: a["displayName"]) if a["isPlayableCharacter"]]
seen = set()
for m in sorted(maps, key=lambda m: m["displayName"]):
    if m.get("tacticalDescription") and m["displayName"] not in seen:
        seen.add(m["displayName"])
        tags.append({"name": m["displayName"], "slot": "map", "image": m["listViewIcon"]})
for w in sorted(weapons, key=lambda w: w["displayName"]):
    category = (w.get("shopData") or {}).get("category")
    if category:
        tags.append({"name": w["displayName"], "slot": "sidearm" if category == "Pistols" else "primary",
                     "image": w["displayIcon"]})

pack = {"library": {
    "id": "valorant",
    "name": "Valorant",
    "version": 1,
    "slots": [
        {"id": "agent", "name": "Agent", "icon": "🦸"},
        {"id": "map", "name": "Map", "icon": "🗺️"},
        {"id": "sidearm", "name": "Sidearm", "icon": "🔫"},
        {"id": "primary", "name": "Primary", "icon": "🎯"},
    ],
    "reels": ["agent", "map", "sidearm", "primary"],
    "standaloneTags": tags,
}, "source_url": "https://valorant-api.com"}
out = Path(__file__).resolve().parents[1] / "community/packs/valorant.json"
out.write_text(json.dumps(pack, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"{out}: {len(tags)} tags")
