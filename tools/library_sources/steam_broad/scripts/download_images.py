import csv, pathlib, urllib.request, time
ROOT=pathlib.Path(__file__).resolve().parents[1]
manifest=ROOT/'data'/'images_manifest.csv'; out=ROOT/'images'; out.mkdir(exist_ok=True)
with manifest.open(encoding='utf-8-sig') as f:
    rows=list(csv.DictReader(f))
for i,r in enumerate(rows,1):
    target=ROOT/r['local_file']
    if target.exists() and target.stat().st_size>5000: continue
    try:
        req=urllib.request.Request(r['url'],headers={'User-Agent':'Mozilla/5.0'})
        with urllib.request.urlopen(req,timeout=30) as resp: target.write_bytes(resp.read())
        print(f'{i}/{len(rows)} OK {r["title"]}')
    except Exception as e: print(f'{i}/{len(rows)} FAIL {r["title"]}: {e}')
    time.sleep(0.05)
