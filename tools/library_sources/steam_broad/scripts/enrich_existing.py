import argparse
import json
from pathlib import Path

from steam_enrichment import enrich_steam_game, build_reels, write_coverage_report

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'data'


def main():
    parser = argparse.ArgumentParser(description='Enrich an already-built Steam games_1000.json without redownloading the ranking.')
    parser.add_argument('--input', default=str(DATA / 'games_1000.json'))
    parser.add_argument('--no-backup', action='store_true')
    args = parser.parse_args()

    path = Path(args.input)
    if not path.exists():
        raise SystemExit(f'Missing {path}')
    payload = json.loads(path.read_text(encoding='utf-8'))
    games = payload.get('games') or []
    if not games:
        raise SystemExit('No games found in JSON')

    if not args.no_backup:
        backup = path.with_suffix(path.suffix + '.pre_enrichment.bak')
        if not backup.exists():
            backup.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
            print('Backup:', backup)

    for g in games:
        tags = g.get('steam_tags') or []
        if isinstance(tags, str):
            tags = [x.strip() for x in tags.replace('|', ';').split(';') if x.strip()]
        genre = g.get('genre') or g.get('primary_genre') or ''
        enriched = enrich_steam_game(tags, genre=genre, title=g.get('title', ''))
        g.update(enriched)

    payload['annotation_version'] = 4
    payload['annotation_note'] = 'Re-enriched locally from existing Steam tags; uniform reel weights for better variety.'
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')

    library = games[0].get('library') or payload.get('library') or 'steam_library'
    (DATA / 'reels_from_1000.json').write_text(
        json.dumps(build_reels(games, library), ensure_ascii=False, indent=2), encoding='utf-8'
    )
    write_coverage_report(games, DATA / 'tag_coverage_report.json')
    print(f'Enriched {len(games)} games.')
    print('Updated:', path)
    print('Updated:', DATA / 'reels_from_1000.json')
    print('Report:', DATA / 'tag_coverage_report.json')


if __name__ == '__main__':
    main()
