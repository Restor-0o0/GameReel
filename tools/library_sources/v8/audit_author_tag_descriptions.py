from __future__ import annotations
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def main():
    failures=[]
    for name,key in [("ALL_TAGS_BY_REEL_v5.json","reel_items"),("MASTER_TAG_DICTIONARY_v5.json","category_items")]:
        data=json.loads((ROOT/name).read_text(encoding="utf-8"))
        for category,items in data.get(key,{}).items():
            for item in items:
                if isinstance(item,dict) and item.get("source")=="curated_v5":
                    if not str(item.get("description","")).strip() or not str(item.get("description_ru","")).strip():
                        failures.append((name,category,item.get("label")))
    if failures:
        for x in failures: print("MISSING", *x)
        raise SystemExit(1)
    print("OK: every curated_v5 author tag has EN + RU descriptions in both main indexes")

if __name__ == "__main__": main()
