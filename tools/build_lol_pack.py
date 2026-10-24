"""Build community/packs/league-of-legends.json from Riot Data Dragon: https://developer.riotgames.com/docs/lol

usage: python tools/build_lol_pack.py <dir with champion.json, item.json, summoner.json, runesReforged.json>
"""
from pathlib import Path
import json
import sys

source = Path(sys.argv[1])
load = lambda name: json.loads((source / name).read_text(encoding="utf-8"))
champions, items, summoners, runes = (load(f"{n}.json") for n in ("champion", "item", "summoner", "runesReforged"))
version = champions["version"]
cdn = f"https://ddragon.leagueoflegends.com/cdn/{version}/img"

tags = [{"name": c["name"], "slot": "champion", "image": f"{cdn}/champion/{c['image']['full']}"}
        for c in sorted(champions["data"].values(), key=lambda c: c["name"])]
tags.extend({"name": name, "slot": "role"} for name in ["Top", "Jungle", "Mid", "Bot", "Support"])
for tree in runes:
    tags.extend({"name": rune["name"], "slot": "keystone", "image": f"https://ddragon.leagueoflegends.com/cdn/img/{rune['icon']}"}
                for rune in tree["slots"][0]["runes"])
for spell in sorted(summoners["data"].values(), key=lambda s: s["name"]):
    if "CLASSIC" in spell["modes"]:
        tags.append({"name": spell["name"], "slot": "summoner", "image": f"{cdn}/spell/{spell['image']['full']}"})


def rift(item):
    return item.get("maps", {}).get("11") and item["gold"]["purchasable"] and not item.get("requiredChampion") \
        and not item.get("requiredAlly") and "Consumable" not in item.get("tags", []) \
        and "Trinket" not in item.get("tags", [])


names = set()
for key, item in sorted(items["data"].items(), key=lambda pair: pair[1]["name"]):
    if not rift(item) or item["name"] in names:
        continue
    boots = "Boots" in item.get("tags", [])
    if boots and item.get("depth") == 2:
        slot = "boots"
    elif not boots and not item.get("into") and item["gold"]["total"] >= 2200:
        slot = "item"
    else:
        continue
    names.add(item["name"])
    tags.append({"name": item["name"], "slot": slot, "image": f"{cdn}/item/{key}.png"})

pack = {"library": {
    "id": "league-of-legends",
    "name": "League of Legends",
    "version": 1,
    "slots": [
        {"id": "champion", "name": "Champion", "icon": "🦸"},
        {"id": "role", "name": "Role", "icon": "🧭"},
        {"id": "keystone", "name": "Keystone", "icon": "💠"},
        {"id": "summoner", "name": "Summoner spell", "icon": "✨"},
        {"id": "boots", "name": "Boots", "icon": "👢"},
        {"id": "item", "name": "Item", "icon": "🗡️"},
    ],
    "reels": ["champion", "role", "keystone", "summoner", "summoner", "boots", "item", "item", "item", "item", "item"],
    "standaloneTags": tags,
}, "source_url": "https://developer.riotgames.com/docs/lol"}
out = Path(__file__).resolve().parents[1] / "community/packs/league-of-legends.json"
out.write_text(json.dumps(pack, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"{out}: patch {version}, {len(champions['data'])} champions, {len(names)} items, {len(tags)} tags")
