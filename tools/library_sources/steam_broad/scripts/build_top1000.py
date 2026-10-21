import csv
import json
import pathlib
import re
import sys
import urllib.request

from steam_enrichment import enrich_steam_game, build_reels, write_coverage_report

ROOT = pathlib.Path(__file__).resolve().parents[1]
DATA = ROOT / 'data'
DATA.mkdir(exist_ok=True)
SOURCE_URL = 'https://raw.githubusercontent.com/preethichelli/GameSense/main/steam_games_2026.csv'
RAW = DATA / 'steam_top1000_raw.csv'


def safe_name(s: str) -> str:
    return (re.sub(r'[^A-Za-z0-9_-]+', '_', s or '').strip('_')[:80] or 'game')


def download():
    print('Downloading source dataset...')
    req = urllib.request.Request(SOURCE_URL, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=60) as r:
        RAW.write_bytes(r.read())


def main():
    if '--offline' not in sys.argv:
        download()
    if not RAW.exists():
        raise SystemExit('Raw CSV missing. Run once with internet or place it at data/steam_top1000_raw.csv')

    games = []
    with RAW.open(encoding='utf-8-sig', newline='') as f:
        for rank, row in enumerate(csv.DictReader(f), start=1):
            tags = [x.strip() for x in (row.get('All_Tags') or '').split(';') if x.strip()]
            appid = str(row.get('AppID') or '').strip()
            title = (row.get('Name') or '').strip()
            genre = row.get('Primary_Genre', '') or ''
            enriched = enrich_steam_game(tags, genre=genre, title=title)
            game = {
                'library': 'steam_broad_top1000',
                'source_rank': rank,
                'appid': appid,
                'title': title,
                'primary_genre': genre,
                'steam_tags': tags,
                **enriched,
                'production_scope_estimate': _scope_estimate(tags),
                'review_score_pct': row.get('Review_Score_Pct', ''),
                'total_reviews': row.get('Total_Reviews', ''),
                'estimated_owners': row.get('Estimated_Owners', ''),
                'steam_url': f'https://store.steampowered.com/app/{appid}/' if appid else '',
                'header_image_url': f'https://cdn.akamai.steamstatic.com/steam/apps/{appid}/header.jpg' if appid else '',
                'local_image': f'images/{rank:04d}_{safe_name(title)}.jpg',
            }
            games.append(game)

    if len(games) != 1000:
        print(f'WARNING: expected 1000 games, got {len(games)}')

    payload = {
        'dataset': 'Steam broad top 1000',
        'source_url': SOURCE_URL,
        'annotation_version': 4,
        'annotation_note': 'Rich tag enrichment with independent mechanic/structure/progression/pressure/visual/etc. atoms. Reel weights are uniform for variety.',
        'games': games,
    }
    (DATA / 'games_1000.json').write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    _write_csv(games)
    (DATA / 'reels_from_1000.json').write_text(
        json.dumps(build_reels(games, 'steam_broad_top1000'), ensure_ascii=False, indent=2), encoding='utf-8'
    )
    write_coverage_report(games, DATA / 'tag_coverage_report.json')
    _write_images_manifest(games)
    print('Built', len(games), 'games with enriched tags.')
    print('Coverage report: data/tag_coverage_report.json')


def _scope_estimate(tags):
    expensive = {'Open World', 'MMORPG', 'Massively Multiplayer', 'Realistic', 'Cinematic', 'City Builder', 'Colony Sim', 'Base Building', 'Online Co-Op', 'Multiplayer', 'VR'}
    score = sum(1 for x in tags if x in expensive)
    if score >= 5:
        return 'very_large'
    if score >= 3:
        return 'large'
    if score >= 1:
        return 'medium'
    return 'small'


def _write_csv(games):
    fields = [
        'library', 'source_rank', 'appid', 'title', 'primary_genre', 'steam_tags',
        'core_mechanics', 'extra_mechanics', 'structure_tags', 'progression_tags', 'pressure_tags',
        'interaction_tags', 'setting_tags', 'theme_tags', 'visual_tags', 'camera_tags', 'player_tags',
        'other_tags', 'visual_style', 'camera', 'player_mode', 'annotation_quality',
        'production_scope_estimate', 'review_score_pct', 'total_reviews', 'estimated_owners',
        'steam_url', 'header_image_url', 'local_image',
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
