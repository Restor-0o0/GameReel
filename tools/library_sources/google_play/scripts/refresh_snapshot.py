import argparse
import csv
import datetime
import json
import pathlib
import shutil
import socket
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed

from build_google_play_top1000 import RAW, DATA, intval, is_game, safe_name, _write_csv, _write_manifest
from enrich_existing import CACHE, load_cache, save_cache, minimal_meta, choose_image
from google_play_enrichment import enrich_google_play_game, build_reels, norm_category, write_coverage_report

LIVE = DATA / 'play_live_details.json'
QUERIES = [
    'action games', 'adventure games', 'arcade games', 'board games', 'card games', 'casino games',
    'casual games', 'educational games', 'music games', 'puzzle games', 'racing games', 'rpg games',
    'simulation games', 'sports games', 'strategy games', 'trivia games', 'word games', 'battle royale',
    'shooter', 'moba', 'idle games', 'merge games', 'match 3', 'hyper casual', 'open world', 'survival',
    'tower defense', 'football', 'cricket', 'chess', 'ludo', 'solitaire', 'gacha', 'anime games',
    'multiplayer games', 'offline games', 'horror games', 'sandbox', 'runner games', 'kids games',
]


def details(app_id):
    from google_play_scraper import app
    for country in ('us', 'gb'):
        try:
            return app_id, app(app_id, lang='en', country=country)
        except Exception:
            pass
    return app_id, None


def candidates(limit):
    ids = {}
    with RAW.open(encoding='utf-8', errors='replace', newline='') as f:
        for row in csv.DictReader(f):
            appid = (row.get('App Id') or '').strip()
            if appid and is_game(row):
                ids[appid] = max(ids.get(appid, 0), intval(row.get('Maximum Installs')))
    found = sorted(ids, key=ids.get, reverse=True)[:limit]
    from google_play_scraper import search
    for query in QUERIES:
        try:
            found += [hit['appId'] for hit in search(query, lang='en', country='us', n_hits=30) if hit.get('appId')]
        except Exception as error:
            print('search failed:', query, error, file=sys.stderr)
    return list(dict.fromkeys(found))


def main():
    parser = argparse.ArgumentParser(description='Re-rank the Google Play library by live install counts.')
    parser.add_argument('--candidates', type=int, default=2500)
    parser.add_argument('--workers', type=int, default=16)
    parser.add_argument('--refresh', action='store_true', help='Ignore cached live details.')
    args = parser.parse_args()

    live = {} if args.refresh or not LIVE.exists() else json.loads(LIVE.read_text(encoding='utf-8'))
    ids = candidates(args.candidates)
    missing = [i for i in ids if i not in live]
    print(f'{len(ids)} candidates, {len(missing)} to fetch', flush=True)
    socket.setdefaulttimeout(20)
    with ThreadPoolExecutor(args.workers) as pool:
        jobs = [pool.submit(details, app_id) for app_id in missing]
        for done, job in enumerate(as_completed(jobs), 1):
            app_id, info = job.result()
            live[app_id] = info and {k: v for k, v in info.items() if k not in ('comments', 'histogram')}
            if done % 100 == 0:
                LIVE.write_text(json.dumps(live, ensure_ascii=False), encoding='utf-8')
                print(f'{done}/{len(missing)}', flush=True)
    LIVE.write_text(json.dumps(live, ensure_ascii=False), encoding='utf-8')

    ranked = sorted(
        (info for info in live.values()
         if info and str(info.get('genreId', '')).startswith('GAME_') and info.get('realInstalls')),
        key=lambda info: (info['realInstalls'], info.get('ratings') or 0, info.get('score') or 0),
        reverse=True,
    )[:1000]

    source = DATA / 'games_1000.json'
    backup = DATA / 'games_1000.2021.json'
    if source.exists() and not backup.exists():
        shutil.copy(source, backup)

    today = datetime.date.today().isoformat()
    cache = load_cache()
    games = []
    for rank, info in enumerate(ranked, 1):
        app_id = info['appId']
        meta = minimal_meta(info)
        cache[app_id] = {'meta': meta, 'error': None}
        updated = info.get('updated')
        game = {
            'library': 'google_play_alltime_like',
            'source_rank': rank,
            'app_id': app_id,
            'title': info.get('title') or app_id,
            'category': norm_category(info.get('genre')),
            'rating': round(info.get('score') or 0.0, 2),
            'rating_count': info.get('ratings') or 0,
            'minimum_installs': info.get('minInstalls') or 0,
            'maximum_installs': info['realInstalls'],
            'free': bool(info.get('free')),
            'price': str(info.get('price') or 0),
            'currency': info.get('currency') or '',
            'content_rating': info.get('contentRating') or '',
            'ad_supported': bool(info.get('containsAds')),
            'in_app_purchases': bool(info.get('offersIAP')),
            'editors_choice': False,
            'developer_id': info.get('developer') or '',
            'released': info.get('released') or '',
            'last_updated': datetime.date.fromtimestamp(updated).strftime('%b %d, %Y') if updated else '',
            'store_url': f'https://play.google.com/store/apps/details?id={app_id}',
            'local_image': f'images/{rank:04d}_{safe_name(info.get("title"))}.jpg',
            'image_url': choose_image(meta),
            'metadata_scrape_status': 'ok',
        }
        game.update(enrich_google_play_game(game, meta))
        games.append(game)
    save_cache(cache)

    payload = {
        'dataset': f'Google Play top 1000 games by cumulative installs ({today} snapshot)',
        'source': f'Live Google Play metadata fetched {today}; candidates from the 2021 gauthamp10 dataset plus Play search',
        'snapshot_date': today,
        'ranking_note': 'Game genres only; live realInstalls desc, then rating count, then rating.',
        'annotation_version': 4,
        'games': games,
    }
    source.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    _write_csv(games)
    (DATA / 'reels_from_1000.json').write_text(json.dumps(build_reels(games), ensure_ascii=False, indent=2), encoding='utf-8')
    write_coverage_report(games, DATA / 'tag_coverage_report.json')
    _write_manifest(games)
    print(f'Built {len(games)} games from live data.')


if __name__ == '__main__':
    main()
