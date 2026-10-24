"""Validate community presets, tag sets, libraries and game packs and rebuild community/index.json.

Usage: python tools/validate_community.py [--check]
--check fails when index.json is out of date instead of rewriting it.
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "community"
CATEGORIES = {
    "CORE", "SECONDARY_MECHANIC", "STRUCTURE", "PRESSURE", "CAMERA", "VISUAL", "SETTING",
    "TWIST", "PROGRESSION", "MULTIPLAYER", "INTERACTION", "SCOPE", "MONETIZATION",
    "PLATFORM_TRAIT", "OTHER", "THEME", "WORLD", "LOCATION", "ENVIRONMENT",
}
RESERVED_LIBRARIES = {
    "fixture", "steam_broad", "steam_alltime_owners", "google_play_alltime_like", "curated_v5",
    "itch_selected", "steam_selected", "personal_games",
}
SLUG = re.compile(r"[a-z0-9][a-z0-9-]{0,60}\.json")
LIBRARY_ID = re.compile(r"[A-Za-z0-9_.-]{1,100}")
MAX_BYTES = 5_000_000
MAX_REELS = 100
MAX_ITEMS = 10_000


def text(value, limit):
    return isinstance(value, str) and value.strip() != "" and len(value) <= limit


def check_preset(data, errors):
    if not text(data.get("id"), 200) or not text(data.get("name"), 80):
        errors.append("id and name (up to 80 characters) are required")
    reels = data.get("reels")
    if not isinstance(reels, list) or not 1 <= len(reels) <= MAX_REELS:
        errors.append(f"reels must hold 1..{MAX_REELS} entries")
        return {}
    if data.get("tags") or data.get("games"):
        errors.append("a reel preset must not contain tags or games")
    for reel in reels:
        if not isinstance(reel, dict) or not text(reel.get("id"), 200) or reel.get("category") not in CATEGORIES:
            errors.append(f"bad reel {reel!r}")
    return {"reels": len(reels)}


def check_tag_set(data, errors):
    if not text(data.get("id"), 200) or not text(data.get("name"), 80):
        errors.append("id and name (up to 80 characters) are required")
    tags = data.get("tags")
    if not isinstance(tags, list) or not 1 <= len(tags) <= MAX_ITEMS:
        errors.append(f"tags must hold 1..{MAX_ITEMS} entries")
        return {}
    if data.get("reels"):
        errors.append("a tag set must not contain reels")
    ids = set()
    for tag in tags:
        if not isinstance(tag, dict) or not text(tag.get("id"), 300) or not text(tag.get("name"), 120) \
                or tag.get("category") not in CATEGORIES:
            errors.append(f"bad tag {tag!r}")
        elif tag["id"] in ids:
            errors.append(f"duplicate tag id {tag['id']}")
        else:
            ids.add(tag["id"])
    return {"tags": len(tags), "categories": sorted({t.get("category") for t in tags if isinstance(t, dict)})}


def check_library(data, errors):
    library = data.get("library")
    games = data.get("games")
    if not isinstance(library, dict) or not isinstance(games, list):
        errors.append("library object and games array are required")
        return {}
    library_id = library.get("id")
    if not isinstance(library_id, str) or not LIBRARY_ID.fullmatch(library_id) or library_id in RESERVED_LIBRARIES:
        errors.append(f"library id {library_id!r} is invalid or reserved")
    if not text(library.get("name"), 80):
        errors.append("library name is required")
    if not isinstance(library.get("version"), int) or library["version"] <= 0:
        errors.append("library version must be a positive integer")
    standalone = library.get("standaloneTags", [])
    for tag in standalone:
        if not isinstance(tag, dict) or not text(tag.get("name"), 120) or tag.get("category") not in CATEGORIES:
            errors.append(f"bad standalone tag {tag!r}")
    if not games and not standalone:
        errors.append("library has no games and no standalone tags")
    if len(games) > MAX_ITEMS:
        errors.append(f"more than {MAX_ITEMS} games")
    seen = set()
    for index, game in enumerate(games, 1):
        if not isinstance(game, dict):
            errors.append(f"game #{index} is not an object")
            continue
        external = game.get("externalId")
        if not text(external, 200) or external in seen:
            errors.append(f"game #{index} has a missing or duplicate externalId")
        seen.add(external)
        if not text(game.get("title"), 200):
            errors.append(f"game #{index} has no title")
        url = game.get("storeUrl")
        if url is not None and not (isinstance(url, str) and url.startswith("https://")):
            errors.append(f"game #{index} storeUrl must be https")
        tags = game.get("tags")
        if not isinstance(tags, list) or not tags:
            errors.append(f"game #{index} has no tags")
            continue
        for tag in tags:
            if not isinstance(tag, dict) or not text(tag.get("name"), 120) or tag.get("category") not in CATEGORIES:
                errors.append(f"game #{index} has bad tag {tag!r}")
    return {"id": library_id, "version": library.get("version"), "games": len(games), "standaloneTags": len(standalone)}


SLOT_ID = re.compile(r"[A-Za-z0-9_.-]{1,60}")
# keep in sync with imageSources in RemoteImages.kt
IMAGE_SOURCES = (
    "https://cdn.cloudflare.steamstatic.com/", "https://cdn.akamai.steamstatic.com/",
    "https://community.cloudflare.steamstatic.com/economy/image/",
    "https://community.akamai.steamstatic.com/economy/image/",
    "https://ddragon.leagueoflegends.com/cdn/", "https://media.valorant-api.com/",
    "https://raw.githubusercontent.com/pubg/api-assets/",
    "https://raw.githubusercontent.com/ByMykel/counter-strike-image-tracker/",
)


def image(value):
    return isinstance(value, str) and len(value) <= 600 and value.startswith(IMAGE_SOURCES)


def check_pack(data, errors):
    library = data.get("library")
    if not isinstance(library, dict):
        errors.append("library object is required")
        return {}
    library_id = library.get("id")
    if not isinstance(library_id, str) or not LIBRARY_ID.fullmatch(library_id) or library_id in RESERVED_LIBRARIES:
        errors.append(f"library id {library_id!r} is invalid or reserved")
    if not text(library.get("name"), 80):
        errors.append("library name is required")
    slots = library.get("slots")
    if not isinstance(slots, list) or not 1 <= len(slots) <= 50:
        errors.append("slots must hold 1..50 entries")
        return {}
    parents = {}
    for slot in slots:
        if not isinstance(slot, dict) or not isinstance(slot.get("id"), str) or not SLOT_ID.fullmatch(slot["id"])                 or not text(slot.get("name"), 60) or slot["id"] in parents:
            errors.append(f"bad slot {slot!r}")
            return {}
        parents[slot["id"]] = slot.get("parent")
    for slot_id in parents:
        seen, current = set(), parents[slot_id]
        while current:
            if current not in parents or current in seen:
                errors.append(f"slot {slot_id} has a missing or cyclic parent")
                break
            seen.add(current)
            current = parents[current]
    reels = library.get("reels")
    if not isinstance(reels, list) or not 1 <= len(reels) <= MAX_REELS or any(r not in parents for r in reels):
        errors.append(f"reels must hold 1..{MAX_REELS} declared slot ids")
    tags = library.get("standaloneTags", [])
    if not isinstance(tags, list) or len(tags) > MAX_ITEMS:
        errors.append(f"standaloneTags must hold up to {MAX_ITEMS} entries")
        return {}
    counts = {}
    for tag in tags:
        if not isinstance(tag, dict) or not text(tag.get("name"), 120) or tag.get("slot") not in parents:
            errors.append(f"bad pack tag {tag!r}")
        else:
            counts[tag["slot"]] = counts.get(tag["slot"], 0) + 1
            if "image" in tag and not image(tag["image"]):
                errors.append(f"tag {tag['name']} has an image outside the allowed servers")
    for slot_id in reels or []:
        if not counts.get(slot_id):
            errors.append(f"slot {slot_id} has no tags")
    summary = {"id": library_id, "version": library.get("version"), "reels": len(reels), "tags": len(tags)}
    cover = data.get("cover") or next((t["image"] for t in tags if isinstance(t, dict) and image(t.get("image"))), None)
    if cover is not None:
        if not image(cover):
            errors.append("cover must point at an allowed image server")
        summary["cover"] = cover
    return summary


KINDS = {"presets": check_preset, "tag-sets": check_tag_set, "libraries": check_library, "packs": check_pack}


def main():
    failures = []
    index = {}
    for kind, check in KINDS.items():
        entries = []
        for path in sorted((ROOT / kind).glob("*")):
            name = f"{kind}/{path.name}"
            errors = []
            if not SLUG.fullmatch(path.name):
                errors.append("file name must be lowercase-kebab-case.json")
            if path.stat().st_size > MAX_BYTES:
                errors.append(f"file is larger than {MAX_BYTES} bytes")
            try:
                data = json.loads(path.read_text(encoding="utf-8-sig"))
            except (ValueError, UnicodeDecodeError) as error:
                failures.append(f"{name}: invalid JSON ({error})")
                continue
            if not isinstance(data, dict):
                failures.append(f"{name}: root must be an object")
                continue
            summary = check(data, errors)
            failures += [f"{name}: {error}" for error in errors]
            title = data.get("library", {}).get("name") if kind in ("libraries", "packs") else data.get("name")
            entries.append({"file": name, "name": title, **summary})
        index[kind] = entries

    if failures:
        print("\n".join(failures))
        return 1

    rendered = json.dumps(index, ensure_ascii=False, indent=2) + "\n"
    target = ROOT / "index.json"
    if "--check" in sys.argv:
        if not target.exists() or target.read_text(encoding="utf-8") != rendered:
            print("community/index.json is out of date: run python tools/validate_community.py")
            return 1
    else:
        target.write_text(rendered, encoding="utf-8")
    print(f"OK: {sum(len(v) for v in index.values())} community files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
