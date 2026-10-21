"""Build a small, source-checked itch.io collection, without copying artwork or store prose."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LIBRARY = "itch_selected"
# Keep editorial data separate from the reproducible resource builder.
SOURCE = ROOT / "tools/library_sources/itch_selected"
ENTRIES = json.loads((SOURCE / "entries.json").read_text(encoding="utf-8"))

games = []
evidence = []
for entry in ENTRIES:
    key, title, url = entry["id"], entry["title"], entry["source"]
    games.append({"externalId": key, "title": title, "store_url": url, "tags": entry["tags"]})
    evidence.append({"id": key, "title": title, "author": entry["author"], "source": url,
                     "checked": "2026-09-27", "basis": "creator description and page metadata",
                     "artwork": "not included; no redistribution license assumed",
                     "annotations": "Original GameReel editorial summaries; not a claim of playtesting"})

assert len({game["externalId"] for game in games}) == len(games), "duplicate game IDs"
assert all(game["tags"] for game in games), "game without annotations"

pack = {"library": {"id": LIBRARY, "name": "itch.io · Selected", "version": 2},
        "source": "https://itch.io/ — creator pages checked 2026-09-27; editorial selection, not a ranking",
        "games": games}
output = ROOT / "app/shared/src/commonMain/composeResources/files/libraries/itch_selected.json"
output.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
source = ROOT / "tools/library_sources/itch_selected"
source.mkdir(parents=True, exist_ok=True)
(source / "provenance.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Built {len(games)} games and {sum(len(g['tags']) for g in games)} tags, without artwork")
