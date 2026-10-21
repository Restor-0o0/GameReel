import csv
import json
import pathlib
import re
import urllib.request

from steam_enrichment import enrich_steam_game, build_reels, write_coverage_report

ROOT = pathlib.Path(__file__).resolve().parents[1]
DATA = ROOT / 'data'
DATA.mkdir(exist_ok=True)
URL = 'https://steamspy.com/api.php?request=all&page=0'
UA = 'GameReelDataset/2.0 (+local research tool)'


def fetch_json(url):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.load(r)


def owner_bounds(text):
    nums = [int(x.replace(',', '').strip()) for x in re.findall(r'[\d,]+', text or '')]
    if len(nums) >= 2:
        return nums[0], nums[1]
    if len(nums) == 1:
        return nums[0], nums[0]
    return 0, 0


def safe_name(s):
    s = re.sub(r'[^A-Za-z0-9_-]+', '_', s or '').strip('_')
    return s[:80] or 'game'


def main():
    raw = fetch_json(URL)
    rows = []
    for key, item in raw.items():
        appid = str(item.get('appid') or key)
        if appid == '999999':
            continue
        lo, hi = owner_bounds(item.get('owners', ''))
        tags_obj = item.get('tags') or {}
        if isinstance(tags_obj, dict):
            tags = [k for k, _ in sorted(tags_obj.items(), key=lambda kv: kv[1], reverse=True)]
        elif isinstance(tags_obj, list):
            tags = tags_obj
        else:
            tags = []
        rows.append((lo, hi, appid, item, tags))

    rows.sort(
        key=lambda r: (
            r[0], r[1], int(r[3].get('positive') or 0) + int(r[3].get('negative') or 0)
        ),
        reverse=True,
    )
    rows = rows[:1000]

    games = []
    for rank, (_, __, appid, item, tags) in enumerate(rows, 1):
        title = item.get('name') or f'App {appid}'
        genre = item.get('genre', '') or ''
        enriched = enrich_steam_game(tags, genre=genre, title=title)
        pos = int(item.get('positive') or 0)
        neg = int(item.get('negative') or 0)
        total = pos + neg
        game = {
            'library': 'steam_alltime_owners',
            'source_rank': rank,
            'appid': appid,
            'title': title,
            'developer': item.get('developer', ''),
            'publisher': item.get('publisher', ''),
            'genre': genre,
            'steam_tags': tags,
            **enriched,
            'owners': item.get('owners', ''),
            'average_playtime_forever_min': item.get('average_forever', 0),
            'median_playtime_forever_min': item.get('median_forever', 0),
            'ccu_yesterday': item.get('ccu', 0),
            'positive_reviews': pos,
            'negative_reviews': neg,
            'review_score_pct': round(pos * 100 / total, 2) if total else None,
            'price_cents': item.get('price', 0),
            'languages': item.get('languages', ''),
            'store_url': f'https://store.steampowered.com/app/{appid}/',
            'header_image_url': f'https://cdn.akamai.steamstatic.com/steam/apps/{appid}/header.jpg',
            'local_image': f'images/{rank:04d}_{safe_name(title)}.jpg',
        }
        games.append(game)

    payload = {
        'dataset': 'Steam all-time top 1000 by owners',
        'source_url': URL,
        'ranking_note': 'SteamSpy request=all&page=0; 1000 games sorted by owners.',
        'annotation_version': 4,
        'annotation_note': 'Rich tag enrichment. Unknown visual/camera values are omitted from reels instead of becoming repetitive placeholder atoms.',
        'games': games,
    }
    (DATA / 'games_1000.json').write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    _write_csv(games)
    (DATA / 'reels_from_1000.json').write_text(
        json.dumps(build_reels(games, 'steam_alltime_owners'), ensure_ascii=False, indent=2), encoding='utf-8'
    )
    write_coverage_report(games, DATA / 'tag_coverage_report.json')
    _write_images_manifest(games)
    print(f'Built {len(games)} Steam games with enriched tags.')
    print('Coverage report: data/tag_coverage_report.json')


def _write_csv(games):
    fields = [
        'library', 'source_rank', 'appid', 'title', 'developer', 'publisher', 'genre', 'steam_tags',
        'core_mechanics', 'extra_mechanics', 'structure_tags', 'progression_tags', 'pressure_tags',
        'interaction_tags', 'setting_tags', 'theme_tags', 'visual_tags', 'camera_tags', 'player_tags',
        'other_tags', 'visual_style', 'camera', 'player_mode', 'annotation_quality',
        'owners', 'average_playtime_forever_min', 'median_playtime_forever_min', 'ccu_yesterday',
        'positive_reviews', 'negative_reviews', 'review_score_pct', 'price_cents', 'languages',
        'store_url', 'header_image_url', 'local_image',
    ]
    list_fields = {
        'steam_tags', 'core_mechanics', 'extra_mechanics', 'structure_tags', 'progression_tags',
        'pressure_tags', 'interaction_tags', 'setting_tags', 'theme_tags', 'visual_tags', 'camera_tags',
        'player_tags', 'other_tags',
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


def _write_images_manifest(games):
    with (DATA / 'images_manifest.csv').open('w', encoding='utf-8-sig', newline='') as f:
        w = csv.writer(f)
        w.writerow(['rank', 'appid', 'title', 'url', 'local_file'])
        for g in games:
            w.writerow([g['source_rank'], g['appid'], g['title'], g['header_image_url'], g['local_image']])


if __name__ == '__main__':
    main()
