import csv
import pathlib
import urllib.request
import time
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
manifest = ROOT / 'data' / 'images_manifest.csv'
outdir = ROOT / 'images'
outdir.mkdir(exist_ok=True)

try:
    from google_play_scraper import app
except ImportError:
    app = None

with manifest.open(encoding='utf-8-sig') as f:
    rows = list(csv.DictReader(f))

for i, r in enumerate(rows, 1):
    out = ROOT / r['local_file']
    if out.exists() and out.stat().st_size > 1000:
        continue
    try:
        url = (r.get('image_url') or '').strip()
        if not url:
            if app is None:
                raise RuntimeError('no image_url in manifest and google-play-scraper is not installed')
            info = app(r['app_id'], lang='en', country='us')
            url = info.get('headerImage') or (info.get('screenshots') or [None])[0] or info.get('icon')
        if not url:
            raise RuntimeError('no image url')
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=30) as resp:
            out.write_bytes(resp.read())
        print(f'[{i}/{len(rows)}] {r["title"]}')
    except Exception as e:
        print('ERR', r['title'], e)
    time.sleep(0.08)
