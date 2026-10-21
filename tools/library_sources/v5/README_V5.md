# GameReel Tag Libraries v5

This is a v5 patch over the uploaded v4 tag-enrichment package.

## What changed

- Curated baseline pools were expanded heavily so reels do not depend only on a few common store tags.
- `SETTING` was expanded for backward compatibility.
- New optional reels were added: `WORLD`, `LOCATION`, `ENVIRONMENT`, `TWIST`.
- Source frequency remains analytics-only; curated and source atoms default to weight 1.
- Added `MASTER_TAG_DICTIONARY_v5.json`: all current generator atoms in one place.
- Added `build_global_game_tag_dictionary.py`: when pointed at real `games_1000.json` files it creates:
  - `game_tag_dictionary.json` — every game with all tags grouped by reel;
  - `tag_to_games.json` — reverse index from tag to games.
- Added a strict description policy: category prompts must never be used as atom descriptions.

## Build the per-game dictionary

```bash
python build_global_game_tag_dictionary.py \
  steam_top1000_idea_generator/data/games_1000.json \
  steam_alltime_top1000_generator/data/games_1000.json \
  google_play_top1000_generator/data/games_1000.json
```

The uploaded v4 package does not contain the actual `games_1000.json` datasets, so the per-game dictionary cannot be pre-populated inside this archive. The builder is included and works once those dataset files are present.
