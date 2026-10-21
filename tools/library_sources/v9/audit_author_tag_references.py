import json
from pathlib import Path
d=json.loads(Path(__file__).with_name("MASTER_TAG_DICTIONARY_v5.json").read_text(encoding="utf-8"))
x=[(c,i.get("label")) for c,v in d["category_items"].items() for i in v if i.get("source")=="curated_v5" and not i.get("reference_game")]
print("missing references:",len(x)); print(*x,sep="\n"); raise SystemExit(bool(x))
