import importlib.util
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCES = ROOT / 'tools/library_sources'
DESTINATION = ROOT / 'app/shared/src/commonMain/composeResources/files/libraries'


def load_rules(folder, filename):
    sys.path.insert(0, str(folder / 'scripts'))
    sys.modules.pop('curated_reels', None)
    spec = importlib.util.spec_from_file_location(filename, folder / 'scripts' / f'{filename}.py')
    module = importlib.util.module_from_spec(spec)
    try:
        spec.loader.exec_module(module)
    finally:
        sys.path.pop(0)
    return module


SUFFIXES = dict(setting='мир и обстановка, в которых разворачивается действие.',
    world='эпоха и устройство мира игры.', location='место действия, вокруг которого строятся уровни, задания и исследование.',
    environment='природная среда и условия, в которых проходит игра.', theme='настроение и тема, которые задают тон всей игре.',
    graphics='визуальный стиль игры.', camera='то, как игрок видит происходящее.', players='формат игры по числу и роли игроков.')


def load_review(categories):
    descriptions, references = {}, {}
    for path in sorted((SOURCES / 'review').glob('*.tsv')):
        reel = path.stem
        for line in path.read_text(encoding='utf-8').splitlines():
            if not line.strip():
                continue
            label, description, game = line.split('	')
            if ':' not in description and reel in SUFFIXES:
                description = description.rstrip('.') + ': ' + SUFFIXES[reel]
            key = categories[reel] + ':' + label.strip().lower()
            descriptions[key] = description
            references[key] = game.strip()
    return descriptions, references


LIST_FIELDS = dict(core_mechanics='CORE', extra_mechanics='SECONDARY_MECHANIC', visual_tags='VISUAL', camera_tags='CAMERA',
    player_tags='MULTIPLAYER', mobile_business_tags='MONETIZATION', other_tags='OTHER', structure_tags='STRUCTURE',
    progression_tags='PROGRESSION', pressure_tags='PRESSURE', interaction_tags='INTERACTION', setting_tags='SETTING',
    theme_tags='THEME', world_tags='WORLD', location_tags='LOCATION', environment_tags='ENVIRONMENT', twist_tags='TWIST')
TEXT_FIELDS = dict(visual_style='VISUAL', camera='CAMERA', player_mode='MULTIPLAYER', production_scope_estimate='SCOPE')
LIBRARY_ORDER = ('steam_alltime', 'steam_broad', 'google_play')
ADDON_WORDS = ('season pass', ' pack', 'dlc', 'workshop', 'expansion', 'bundle', 'soundtrack', 'creations', 'bonus content', ' × ')


def load_library_review():
    actions, fixes = {}, {}
    for line in (SOURCES / 'review/libraries/tags.tsv').read_text(encoding='utf-8').splitlines():
        if line.strip():
            category, tag, action = line.split('\t')
            actions[category + ':' + tag.strip().lower()] = action.strip()
    for line in (SOURCES / 'review/libraries/fixes.tsv').read_text(encoding='utf-8').splitlines():
        if line.strip():
            title, category, tags = line.split('\t')
            fixes.setdefault(title, []).append((category, {tag.strip().lower() for tag in tags.split(';')}))
    return actions, fixes


def review_game(game, actions, fixes, unused):
    def clean(category, tag):
        tag = tag.strip()
        if category == 'OTHER':
            tag = tag.lower()
        for fix_category, names in fixes.get(game['title'], []):
            if fix_category in ('*', category) and tag.lower() in names:
                unused.discard((game['title'], tag.lower()))
                return None
        action = actions.get(category + ':' + tag.lower(), '')
        if action == '-':
            return None
        return action[1:] if action.startswith('=') else tag
    for field, category in LIST_FIELDS.items():
        if isinstance(game.get(field), list):
            game[field] = list(dict.fromkeys(tag for tag in (clean(category, value) for value in game[field] if value.strip()) if tag))
    for field, category in TEXT_FIELDS.items():
        if game.get(field):
            game[field] = clean(category, game[field]) or ''


def game_tags(game):
    for field, category in LIST_FIELDS.items():
        for tag in game.get(field) or []:
            yield category, tag
    for field, category in TEXT_FIELDS.items():
        if game.get(field):
            yield category, game[field]


def title_key(title):
    title = re.sub(r'\([^)]*\)|\b(19|20)\d\d edition\b', '', title.lower())
    return ''.join(ch for ch in title if ch.isalnum() and ch.isascii())


def same_game(first, second):
    shorter, longer = sorted((title_key(first), title_key(second)), key=len)
    if not shorter or not longer.startswith(shorter):
        return False
    return not re.fullmatch(r'[0-9ivx]{1,3}', longer[len(shorter):])


def main():
    DESTINATION.mkdir(parents=True, exist_ok=True)
    broad = json.loads((SOURCES / 'steam_broad/data/games_1000.json').read_text(encoding='utf-8-sig'))
    steam_metadata = {str(game['appid']): game for game in broad['games']}
    actions, fixes = load_library_review()
    unused = {(title, tag) for title, entries in fixes.items() for _, tags in entries for tag in tags}
    library_games = {}
    for name in ('steam_broad', 'steam_alltime', 'google_play'):
        if '--curated-only' in sys.argv:
            library_games[name] = json.loads((DESTINATION / f'{name}.json').read_text(encoding='utf-8'))['games']
            continue
        folder = SOURCES / name
        payload = json.loads((folder / 'data/games_1000.json').read_text(encoding='utf-8-sig'))
        mobile = name == 'google_play'
        rules = load_rules(folder, 'google_play_enrichment' if mobile else 'steam_enrichment')
        games = payload['games']
        cache_path = folder / 'data/google_play_metadata_cache.json'
        cache = json.loads(cache_path.read_text(encoding='utf-8')) if mobile and cache_path.exists() else {}
        for game in games:
            if mobile:
                enriched = rules.enrich_google_play_game(game, (cache.get(game['app_id']) or {}).get('meta') or {})
            else:
                tags = game.get('steam_tags') or []
                reference = steam_metadata.get(str(game.get('appid')), {})
                if not tags and reference.get('steam_tags'):
                    tags = reference['steam_tags']
                    game['steam_tags'] = tags
                    game['tag_metadata_source'] = 'steam_broad:matching_appid'
                if isinstance(tags, str):
                    tags = [tag.strip() for tag in tags.replace('|', ';').split(';') if tag.strip()]
                enriched = rules.enrich_steam_game(tags, genre=game.get('genre') or game.get('primary_genre') or reference.get('genre') or '', title=game.get('title', ''))
            game.update(enriched)
            review_game(game, actions, fixes, unused)
        library_games[name] = games
        payload['annotation_version'] = 6
        payload['annotation_note'] = 'v5 rules applied to existing snapshots with manual tag review; curated atoms stored separately; no new network metadata fetched.'
        target = DESTINATION / f'{name}.json'
        target.write_text(json.dumps(payload, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
        rules.write_coverage_report(games, folder / 'data/tag_coverage_report.json')
        print(f'{name}: {len(games)} games, {target.stat().st_size} bytes')
    categories = dict(core='CORE', secondary='SECONDARY_MECHANIC', structure='STRUCTURE',
        progression='PROGRESSION', pressure='PRESSURE', interaction='INTERACTION', setting='SETTING',
        world='WORLD', location='LOCATION', environment='ENVIRONMENT', theme='THEME', graphics='VISUAL',
        camera='CAMERA', players='MULTIPLAYER', twist='TWIST')
    review_descriptions, review_references = load_review(categories)
    curated = json.loads((SOURCES / 'v8/curated_reels_v5.json').read_text(encoding='utf-8'))
    previous = json.loads((SOURCES / 'v5/curated_reels_v5.json').read_text(encoding='utf-8'))
    tags = []
    descriptions = {}
    for reel, atoms in curated['reels'].items():
        assert [atom['label'] for atom in atoms] == previous['reels'][reel], f'Tag identities changed: {reel}'
        for atom in atoms:
            category = categories[reel]
            description = review_descriptions.get(category + ':' + atom['label'].strip().lower(), atom.get('description_ru', '').strip())
            tags.append({'name': atom['label'], 'category': category, 'description': description})
            if description:
                descriptions[category + ':' + atom['label'].strip().lower()] = description
    additions = json.loads((SOURCES / 'review/curated_additions.json').read_text(encoding='utf-8'))
    keys = {(tag['category'], tag['name'].strip().lower()) for tag in tags}
    for tag in additions:
        key = (tag['category'], tag['name'].strip().lower())
        assert key not in keys, f'Duplicate editorial tag: {key}'
        assert tag['description'].strip(), f'Missing description: {key}'
        keys.add(key)
        tags.append(tag)
        descriptions[':'.join(key)] = tag['description']
    lookup = json.loads((SOURCES / 'v8/TAG_DESCRIPTIONS_v8.json').read_text(encoding='utf-8'))
    for reel, entries in lookup['categories'].items():
        for label, entry in entries.items():
            key = categories[reel] + ':' + label.strip().lower()
            description = review_descriptions.get(key, entry.get('description_ru', '').strip())
            if description:
                descriptions.setdefault(categories[reel] + ':' + label.strip().lower(), description)
    for key, action in actions.items():
        if action not in ('-',) and not action.startswith('='):
            descriptions.setdefault(key, action)
    quote = lambda value: json.dumps(value, ensure_ascii=False).replace('$', '\\$')
    lines = [f'    {quote(key)} to {quote(value)},' for key, value in descriptions.items()]
    presentation = ROOT / 'app/shared/src/commonMain/kotlin/com/restor/gamereel/ui/presentation'
    header = 'package com.restor.gamereel.ui.presentation\n\n// generated by tools/build_bundled_libraries.py, dont edit\n'
    (presentation / 'CuratedDescriptions.kt').write_text(header + 'internal val curatedDescriptions = mapOf(\n' + '\n'.join(lines) + '\n)\n', encoding='utf-8')
    references = json.loads((SOURCES / 'v9/TAG_REFERENCE_GAMES_v9.json').read_text(encoding='utf-8'))['references']
    games = {categories[reel] + ':' + label.strip().lower(): [entry['reference_game']]
        for reel, entries in references.items() for label, entry in entries.items()}
    games.update({key: [game.strip() for game in value.split(';') if game.strip() != '-'] for key, value in review_references.items()})
    popular = {}
    for name in LIBRARY_ORDER:
        for game in library_games[name]:
            title = game['title']
            if any(word in title.lower() for word in ADDON_WORDS) or ' - ' in title:
                continue
            for category, tag in game_tags(game):
                if category == 'SCOPE':
                    continue
                popular.setdefault(category + ':' + tag.strip().lower(), []).append(title)
    reference_file = presentation / 'CuratedReferences.kt'
    titles_now = {game['title'] for name in LIBRARY_ORDER for game in library_games[name]}
    # keep old picks while the games are still there
    unquote = lambda value: json.loads(value.replace('\\$', '$'))
    previous = {unquote(key): unquote(value).split('\n')
        for key, value in re.findall(r'^    ("(?:[^"\\]|\\.)*") to ("(?:[^"\\]|\\.)*"),$', reference_file.read_text(encoding='utf-8'), re.M)} \
        if reference_file.exists() else {}
    renamed = {}
    play_backup = SOURCES / 'google_play/data/games_1000.2021.json'
    if play_backup.exists():
        current = {game['app_id']: game['title'] for game in library_games['google_play']}
        renamed = {game['title']: current[game['app_id']]
            for game in json.loads(play_backup.read_text(encoding='utf-8'))['games'] if game['app_id'] in current}
    for key, titles in popular.items():
        chosen = games.setdefault(key, [])
        if key in previous:
            for title in previous[key]:
                title = title if title in titles_now else renamed.get(title)
                if title in titles_now and len(chosen) < 3 and not any(same_game(title, existing) for existing in chosen):
                    chosen.append(title)
            continue
        for title in titles:
            if len(chosen) >= 3:
                break
            if not any(same_game(title, existing) for existing in chosen):
                chosen.append(title)
    reference_lines = [f'    {quote(key)} to {quote(chr(10).join(titles))},' for key, titles in games.items() if titles]
    reference_file.write_text(header + 'internal val tagReferences = mapOf(\n' + '\n'.join(reference_lines) + '\n)\n', encoding='utf-8')
    for title, tag in sorted(unused if '--curated-only' not in sys.argv else []):
        print(f'unused fix: {title} / {tag}')
    pack = {'library': {'id': 'curated_v5', 'name': 'Теги GameReel', 'version': 10, 'standaloneTags': tags},
        'games': [], 'source': 'GameReel_editorial_v10_category_coverage'}
    (DESTINATION / 'curated.json').write_text(json.dumps(pack, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
    print(f'curated: {len(tags)} tags, {len({tag["category"] for tag in tags})} categories')
    print(f'descriptions: {len(descriptions)}')
    print(f'references: {len(reference_lines)}')


if __name__ == '__main__':
    main()
