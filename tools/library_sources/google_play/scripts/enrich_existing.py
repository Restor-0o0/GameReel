import argparse
import csv
import json
import time
from pathlib import Path

from google_play_enrichment import enrich_google_play_game, build_reels, write_coverage_report

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'data'
CACHE = DATA / 'google_play_metadata_cache.json'


def load_cache():
    if CACHE.exists():
        try:
            return json.loads(CACHE.read_text(encoding='utf-8'))
        except Exception:
            pass
    return {}


def save_cache(cache):
    CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding='utf-8')


def minimal_meta(info):
    if not info:
        return {}
    return {
        'title': info.get('title') or '',
        'summary': info.get('summary') or '',
        'description': info.get('description') or '',
        'genre': info.get('genre') or '',
        'genreId': info.get('genreId') or '',
        'containsAds': bool(info.get('containsAds')),
        'offersIAP': bool(info.get('offersIAP')),
        'headerImage': info.get('headerImage') or '',
        'icon': info.get('icon') or '',
        'screenshots': (info.get('screenshots') or [])[:3],
    }


def fetch_meta(app_id, app_func, countries=('us','gb')):
    last_error = None
    for country in countries:
        try:
            return minimal_meta(app_func(app_id, lang='en', country=country)), None
        except Exception as exc:
            last_error = str(exc)
    return {}, last_error


def choose_image(meta):
    return meta.get('headerImage') or ((meta.get('screenshots') or [None])[0]) or meta.get('icon') or ''


def write_csv(games):
    fields = [
        'library','source_rank','app_id','title','category','store_genre','store_summary',
        'core_mechanics','extra_mechanics','structure_tags','progression_tags','pressure_tags',
        'interaction_tags','setting_tags','theme_tags','visual_tags','camera_tags','player_tags','other_tags',
        'mobile_business_tags','visual_style','camera','player_mode','annotation_quality',
        'rating','rating_count','minimum_installs','maximum_installs','free','price','currency','content_rating',
        'ad_supported','in_app_purchases','editors_choice','developer_id','released','last_updated','store_url','local_image','image_url'
    ]
    list_fields = {
        'core_mechanics','extra_mechanics','structure_tags','progression_tags','pressure_tags','interaction_tags',
        'setting_tags','theme_tags','visual_tags','camera_tags','player_tags','other_tags','mobile_business_tags'
    }
    with (DATA / 'games_1000.csv').open('w', encoding='utf-8-sig', newline='') as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        for game in games:
            row = {k: game.get(k, '') for k in fields}
            for k in list_fields:
                row[k] = ' | '.join(row[k] or [])
            row['annotation_quality'] = json.dumps(row['annotation_quality'], ensure_ascii=False)
            w.writerow(row)


def write_manifest(games):
    with (DATA / 'images_manifest.csv').open('w', encoding='utf-8-sig', newline='') as f:
        w = csv.writer(f)
        w.writerow(['rank','app_id','title','store_url','image_url','local_file'])
        for g in games:
            w.writerow([g['source_rank'], g['app_id'], g['title'], g['store_url'], g.get('image_url',''), g['local_image']])


def main():
    p = argparse.ArgumentParser(description='Enrich existing Google Play games_1000.json. Online mode fetches Play Store description/genre and caches it.')
    p.add_argument('--input', default=str(DATA / 'games_1000.json'))
    p.add_argument('--online', action='store_true', help='Fetch Play Store metadata using google-play-scraper.')
    p.add_argument('--limit', type=int, default=0, help='For testing: only fetch this many uncached apps online; still re-enrich all games.')
    p.add_argument('--no-backup', action='store_true')
    p.add_argument('--sleep', type=float, default=0.08)
    args = p.parse_args()

    path = Path(args.input)
    if not path.exists():
        raise SystemExit(f'Missing {path}')
    payload = json.loads(path.read_text(encoding='utf-8'))
    games = payload.get('games') or []
    if not games:
        raise SystemExit('No games in JSON')

    if not args.no_backup:
        backup = path.with_suffix(path.suffix + '.pre_enrichment.bak')
        if not backup.exists():
            backup.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
            print('Backup:', backup)

    app_func = None
    if args.online:
        try:
            from google_play_scraper import app as app_func
        except ImportError:
            raise SystemExit('Online enrichment needs: python -m pip install google-play-scraper')

    cache = load_cache()
    fetched = 0
    failed = 0
    online_budget = args.limit if args.limit > 0 else None

    for i, g in enumerate(games, 1):
        app_id = g.get('app_id') or ''
        cached = cache.get(app_id) or {}
        meta = cached.get('meta') or {}
        if args.online and app_id and not meta and (online_budget is None or fetched < online_budget):
            meta, error = fetch_meta(app_id, app_func)
            if meta:
                cache[app_id] = {'meta': meta, 'error': None}
                fetched += 1
            else:
                cache[app_id] = {'meta': {}, 'error': error or 'unknown'}
                failed += 1
            if (fetched + failed) % 25 == 0:
                save_cache(cache)
                print(f'[{i}/{len(games)}] fetched={fetched} failed={failed} cached={len(cache)}')
            time.sleep(max(0.0, args.sleep))
        elif app_id in cache:
            meta = (cache[app_id] or {}).get('meta') or {}

        enriched = enrich_google_play_game(g, meta)
        g.update(enriched)
        if meta:
            if meta.get('title'):
                g['store_title'] = meta.get('title')
            g['image_url'] = choose_image(meta)
            g['metadata_scrape_status'] = 'ok'
        elif app_id in cache and (cache[app_id] or {}).get('error'):
            g['metadata_scrape_status'] = 'failed'
        else:
            g['metadata_scrape_status'] = 'not_fetched'

    if args.online:
        save_cache(cache)

    payload['annotation_version'] = 4
    payload['annotation_note'] = 'Rich enrichment from category/title plus cached Play Store descriptions where available. Reel weights are uniform for variety.'
    payload['metadata_enrichment'] = {
        'online_requested': args.online,
        'cache_entries': len(cache),
        'newly_fetched': fetched,
        'failed': failed,
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    write_csv(games)
    (DATA / 'reels_from_1000.json').write_text(json.dumps(build_reels(games), ensure_ascii=False, indent=2), encoding='utf-8')
    write_coverage_report(games, DATA / 'tag_coverage_report.json')
    write_manifest(games)
    print(f'Enriched {len(games)} games. New metadata fetched: {fetched}; failed: {failed}.')
    print('Coverage report:', DATA / 'tag_coverage_report.json')


if __name__ == '__main__':
    main()
