from __future__ import annotations
from pathlib import Path
import json

CURATED_PATH = Path(__file__).resolve().parent.parent / "data" / "curated_reels_v5.json"

def load_curated():
    if not CURATED_PATH.exists():
        return {}
    return json.loads(CURATED_PATH.read_text(encoding="utf-8")).get("reels", {})

def merge_curated_items(items, reel_id):
    curated = load_curated().get(reel_id, [])
    by_label = {str(x.get("label", "")).strip().lower(): dict(x) for x in items if x.get("label")}
    for c in curated:
        c_obj = {"label": c} if isinstance(c, str) else dict(c)
        label = str(c_obj.get("label", "")).strip()
        if not label:
            continue
        key = label.lower()
        if key in by_label:
            # Keep source frequency/weight, but attach curated metadata such as descriptions.
            for meta_key, meta_value in c_obj.items():
                if meta_key != "label" and meta_value not in (None, ""):
                    by_label[key][meta_key] = meta_value
        else:
            item = {"label": label, "weight": 1, "frequency": 0, "source": "curated_v5"}
            item.update({k: v for k, v in c_obj.items() if k != "label"})
            by_label[key] = item
    return list(by_label.values())
