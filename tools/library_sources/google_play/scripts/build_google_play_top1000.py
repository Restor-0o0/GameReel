import csv
import json
import pathlib
import re

from google_play_enrichment import enrich_google_play_game, build_reels, norm_category, write_coverage_report

ROOT = pathlib.Path(__file__).resolve().parents[1]
DATA = ROOT / 'data'
DATA.mkdir(exist_ok=True)
RAW = DATA / 'raw' / 'Google-Playstore.csv'
GAME_CATEGORIES = {
    'Action','Adventure','Arcade','Board','Card','Casino','Casual','Educational','Music','Puzzle','Racing','Role Playing','Simulation','Sports','Strategy','Trivia','Word',
    'GAME_ACTION','GAME_ADVENTURE','GAME_ARCADE','GAME_BOARD','GAME_CARD','GAME_CASINO','GAME_CASUAL','GAME_EDUCATIONAL','GAME_MUSIC','GAME_PUZZLE','GAME_RACING','GAME_ROLE_PLAYING','GAME_SIMULATION','GAME_SPORTS','GAME_STRATEGY','GAME_TRIVIA','GAME_WORD','Game'
}


def intval(v):
    if v is None:
        return 0
    s = str(v).replace(',', '').replace('+', '').strip()
    try:
        return int(float(s))
    except Exception:
        return 0


def floatval(v):
    try:
        return float(v)
    except Exception:
        return 0.0


def boolval(v):
    return str(v).strip().lower() in {'true', '1', 'yes'}


def safe_name(s):
    return (re.sub(r'[^A-Za-z0-9_-]+', '_', s or '').strip('_')[:80] or 'game')


def is_game(row):
    c = (row.get('Category') or '').strip()
    return c in GAME_CATEGORIES or c.startswith('GAME_')


def main():
    if not RAW.exists():
        raise SystemExit('Missing data/raw/Google-Playstore.csv. Run scripts/fetch_dataset.ps1 first, or place the Kaggle CSV there manually.')

    best = {}
    with RAW.open(encoding='utf-8', errors='replace', newline='') as f:
        for row in csv.DictReader(f):
            if not is_game(row):
                continue
            appid = (row.get('App Id') or '').strip()
            if not appid:
                continue
            installs = intval(row.get('Maximum Installs') or row.get('Minimum Installs') or row.get('Installs'))
            ratings = intval(row.get('Rating Count'))
            rating = floatval(row.get('Rating'))
            score = (installs, ratings, rating)
            if appid not in best or score > best[appid][0]:
                best[appid] = (score, row)

    ranked = sorted(best.values(), key=lambda x: x[0], reverse=True)[:1000]
    games = []
    for rank, (_, row) in enumerate(ranked, 1):
        category = norm_category(row.get('Category'))
        appid = (row.get('App Id') or '').strip()
        title = (row.get('App Name') or appid).strip()
        free = boolval(row.get('Free'))
        ads = boolval(row.get('Ad Supported'))
        iap = boolval(row.get('In App Purchases'))
        game = {
            'library': 'google_play_alltime_like',
            'source_rank': rank,
            'app_id': appid,
            'title': title,
            'category': category,
            'rating': floatval(row.get('Rating')),
            'rating_count': intval(row.get('Rating Count')),
            'minimum_installs': intval(row.get('Minimum Installs') or row.get('Installs')),
            'maximum_installs': intval(row.get('Maximum Installs') or row.get('Installs')),
            'free': free,
            'price': row.get('Price', ''),
            'currency': row.get('Currency', ''),
            'content_rating': row.get('Content Rating', ''),
            'ad_supported': ads,
            'in_app_purchases': iap,
            'editors_choice': boolval(row.get('Editors Choice')),
            'developer_id': row.get('Developer Id', ''),
            'released': row.get('Released', ''),
            'last_updated': row.get('Last Updated', ''),
            'store_url': f'https://play.google.com/store/apps/details?id={appid}',
            'local_image': f'images/{rank:04d}_{safe_name(title)}.jpg',
        }
        game.update(enrich_google_play_game(game))
        games.append(game)

    payload = {
        'dataset': 'Google Play top 1000 games by cumulative installs (historical snapshot)',
        'source': 'gauthamp10/google-playstore-apps; June 2021 snapshot',
        'ranking_note': 'Game categories only; Maximum Installs desc, then Rating Count, then Rating.',
        'annotation_version': 4,
        'annotation_note': 'Offline pass uses title/category. build_all.ps1 then enriches from live Play Store descriptions where available.',
        'games': games,
    }
    (DATA / 'games_1000.json').write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    _write_csv(games)
    (DATA / 'reels_from_1000.json').write_text(json.dumps(build_reels(games), ensure_ascii=False, indent=2), encoding='utf-8')
    write_coverage_report(games, DATA / 'tag_coverage_report.json')
    _write_manifest(games)
    print('Built', len(games), 'Google Play games (offline enrichment pass).')


def _write_csv(games):
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


def _write_manifest(games):
    with (DATA / 'images_manifest.csv').open('w', encoding='utf-8-sig', newline='') as f:
        w = csv.writer(f)
        w.writerow(['rank','app_id','title','store_url','image_url','local_file'])
        for g in games:
            w.writerow([g['source_rank'], g['app_id'], g['title'], g['store_url'], g.get('image_url',''), g['local_image']])


if __name__ == '__main__':
    main()
