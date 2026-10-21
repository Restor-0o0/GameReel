from __future__ import annotations

import collections
import json
import re
from pathlib import Path
from typing import Iterable
from curated_reels import merge_curated_items

CATEGORY_BASE = {
    'Action': {'core': ['action combat']},
    'Adventure': {'core': ['exploration/adventure']},
    'Arcade': {'core': ['arcade action'], 'structure': ['short score-based sessions']},
    'Board': {'core': ['board-game rules']},
    'Card': {'core': ['card play']},
    'Casino': {'core': ['chance-based game loop']},
    'Casual': {'core': ['casual short-session loop']},
    'Educational': {'core': ['learning mechanics']},
    'Music': {'core': ['rhythm/music interaction']},
    'Puzzle': {'core': ['puzzles']},
    'Racing': {'core': ['racing'], 'interaction': ['vehicle control']},
    'Role Playing': {'core': ['role-playing systems'], 'progression': ['rpg progression']},
    'Simulation': {'core': ['simulation']},
    'Sports': {'core': ['sports mechanics']},
    'Strategy': {'core': ['strategy']},
    'Trivia': {'core': ['quiz/knowledge']},
    'Word': {'core': ['word puzzles']},
}

# High-signal textual rules. They are intentionally phrase-oriented: better to miss a tag
# than to turn generic marketing copy into a wrong mechanic.
TEXT_RULES = [
    # core mechanics
    (r'\b(first[- ]person shooter|fps)\b', {'core': ['shooting'], 'camera': ['first-person']}),
    (r'\b(third[- ]person shooter)\b', {'core': ['shooting'], 'camera': ['third-person']}),
    (r'\b(shoot(?:er|ing)|gunfight|gun game)\b', {'core': ['shooting']}),
    (r'\b(match[ -]?3|three in a row)\b', {'core': ['match-3']}),
    (r'\b(merge game|merge puzzle|merge two|merge items)\b', {'core': ['merging']}),
    (r'\b(hidden object)\b', {'core': ['hidden-object searching']}),
    (r'\b(escape room|room escape)\b', {'core': ['escape-room puzzles']}),
    (r'\b(word search|crossword|word puzzle|word game)\b', {'core': ['word puzzles']}),
    (r'\b(sudoku)\b', {'core': ['sudoku']}),
    (r'\b(solitaire)\b', {'core': ['solitaire/card play']}),
    (r'\b(deck[- ]?build|build your deck)\b', {'core': ['deckbuilding'], 'progression': ['deck growth']}),
    (r'\b(card battle|card battler|collectible card)\b', {'core': ['card combat'], 'progression': ['card collection']}),
    (r'\b(tower defense|defend your tower|defend your base from waves)\b', {'core': ['tower defense'], 'pressure': ['wave defense']}),
    (r'\b(idle game|idle rpg|idle tycoon|afk)\b', {'core': ['idle progression'], 'structure': ['idle sessions'], 'progression': ['passive growth']}),
    (r'\b(incremental|clicker game|tap to earn|tap game)\b', {'core': ['incremental/clicker'], 'progression': ['incremental growth']}),
    (r'\b(endless runner|running game|runner game)\b', {'core': ['endless running'], 'structure': ['endless score run']}),
    (r'\b(platformer|platform game)\b', {'core': ['platforming']}),
    (r'\b(farm game|farming game|grow crops|harvest crops)\b', {'core': ['farming'], 'progression': ['farm growth']}),
    (r'\b(fishing game|go fishing|catch fish)\b', {'core': ['fishing']}),
    (r'\b(cooking game|cook food|restaurant game|cooking simulator)\b', {'core': ['cooking'], 'secondary': ['order fulfillment']}),
    (r'\b(restaurant tycoon|restaurant manager)\b', {'core': ['restaurant management']}),
    (r'\b(city builder|build a city|build your city|city building)\b', {'core': ['city building'], 'progression': ['city growth']}),
    (r'\b(base building|build your base|build a base)\b', {'core': ['base building'], 'progression': ['base growth']}),
    (r'\b(crafting game|craft weapons|craft items|crafting system)\b', {'core': ['crafting']}),
    (r'\b(survival game|survive the|survival adventure)\b', {'core': ['survival'], 'pressure': ['survival needs']}),
    (r'\b(battle royale)\b', {'core': ['combat'], 'structure': ['battle-royale matches'], 'pressure': ['last-player pressure'], 'players': ['competitive multiplayer']}),
    (r'\b(auto battler|autobattler)\b', {'core': ['auto battler'], 'progression': ['team composition progression']}),
    (r'\b(tycoon game|business tycoon|idle tycoon)\b', {'core': ['management/tycoon'], 'progression': ['business growth']}),
    (r'\b(management game|manage your|manager game)\b', {'core': ['management']}),
    (r'\b(driving game|car driving|drive cars|driving simulator)\b', {'core': ['driving'], 'interaction': ['vehicle control']}),
    (r'\b(racing game|race cars|street racing|car racing)\b', {'core': ['racing'], 'interaction': ['vehicle control']}),
    (r'\b(flight simulator|flying game|fly planes|airplane simulator)\b', {'core': ['flight']}),
    (r'\b(chess)\b', {'core': ['chess/board strategy']}),
    (r'\b(football|soccer)\b', {'core': ['football/soccer']}),
    (r'\b(basketball)\b', {'core': ['basketball']}),
    (r'\b(baseball)\b', {'core': ['baseball']}),
    (r'\b(golf)\b', {'core': ['golf']}),
    (r'\b(tennis)\b', {'core': ['tennis']}),
    (r'\b(bowling)\b', {'core': ['bowling']}),
    (r'\b(rhythm game|music game|tap to the beat)\b', {'core': ['rhythm']}),
    (r'\b(dress up|dress-up|fashion game)\b', {'core': ['dress-up/customization'], 'progression': ['cosmetic collection']}),
    (r'\b(coloring game|colouring game|color by number)\b', {'core': ['coloring']}),
    (r'\b(pet game|virtual pet|take care of your pet)\b', {'core': ['pet care'], 'progression': ['pet progression']}),

    # secondary / interaction
    (r'\b(build and craft|mine and craft|craft and build)\b', {'secondary': ['building and crafting']}),
    (r'\b(collect heroes|collect characters|collect monsters|collect creatures)\b', {'secondary': ['collection'], 'progression': ['collection progression']}),
    (r'\b(physics puzzle|realistic physics|physics based|physics-based)\b', {'secondary': ['physics interaction'], 'interaction': ['physics manipulation']}),
    (r'\b(destroy|destruction|destructible)\b', {'secondary': ['destruction'], 'interaction': ['destructible objects']}),
    (r'\b(customize your|character customization|customise your)\b', {'secondary': ['customization']}),
    (r'\b(upgrade your|upgrade weapons|upgrade cars|upgrade heroes)\b', {'progression': ['upgrade progression']}),
    (r'\b(unlock new|unlock characters|unlock cars|unlock levels)\b', {'progression': ['unlock progression']}),
    (r'\b(level up|level-up)\b', {'progression': ['level-based progression']}),
    (r'\b(resource management|manage resources)\b', {'secondary': ['resource management'], 'pressure': ['resource scarcity']}),
    (r'\b(inventory management|manage your inventory)\b', {'secondary': ['inventory management']}),

    # structure
    (r'\b(open world|open-world)\b', {'structure': ['open world']}),
    (r'\b(sandbox game|sandbox world)\b', {'structure': ['sandbox']}),
    (r'\b(roguelike|rogue-like)\b', {'structure': ['roguelike runs'], 'progression': ['run reset/progression']}),
    (r'\b(roguelite|rogue-lite)\b', {'structure': ['roguelite runs'], 'progression': ['meta progression']}),
    (r'\b(story mode|campaign mode|single player campaign)\b', {'structure': ['campaign']}),
    (r'\b(hundreds of levels|thousands of levels|challenging levels)\b', {'structure': ['level-based progression']}),
    (r'\b(waves of enemies|enemy waves|survive waves)\b', {'structure': ['wave-based sessions'], 'pressure': ['escalating waves']}),

    # pressure
    (r'\b(time limit|time-limit|countdown|before time runs out)\b', {'pressure': ['time pressure']}),
    (r'\b(limited moves|moves are limited)\b', {'pressure': ['limited moves']}),
    (r'\b(permadeath|permanent death)\b', {'pressure': ['permadeath']}),
    (r'\b(stealth game|stay hidden|avoid detection)\b', {'core': ['stealth'], 'pressure': ['detection avoidance']}),

    # multiplayer
    (r'\b(online multiplayer|multiplayer online|play online with)\b', {'players': ['online multiplayer']}),
    (r'\b(co-op|coop|cooperative multiplayer)\b', {'players': ['co-op']}),
    (r'\b(pvp|player versus player|player vs player)\b', {'players': ['competitive multiplayer'], 'pressure': ['player competition']}),
    (r'\b(single player|single-player)\b', {'players': ['singleplayer']}),
    (r'\b(offline game|play offline)\b', {'players': ['offline/singleplayer-friendly']}),

    # cameras
    (r'\b(first[- ]person)\b', {'camera': ['first-person']}),
    (r'\b(third[- ]person)\b', {'camera': ['third-person']}),
    (r'\b(top[- ]down)\b', {'camera': ['top-down']}),
    (r'\b(isometric)\b', {'camera': ['isometric']}),
    (r'\b(side[- ]scrolling|side scroller)\b', {'camera': ['side-view']}),

    # visuals
    (r'\b(pixel art|pixel graphics|8-bit|16-bit)\b', {'visual': ['pixel art']}),
    (r'\b(low[- ]poly|low poly)\b', {'visual': ['low-poly 3D']}),
    (r'\b(voxel|blocky 3d)\b', {'visual': ['voxel/blocky 3D']}),
    (r'\b(anime style|anime-style|anime rpg)\b', {'visual': ['anime stylized']}),
    (r'\b(cartoon graphics|cartoon style|cartoony)\b', {'visual': ['cartoon stylized']}),
    (r'\b(realistic graphics|realistic 3d|photorealistic)\b', {'visual': ['realistic 3D']}),
    (r'\b(3d game|3d graphics)\b', {'visual': ['3D']}),
    (r'\b(2d game|2d graphics)\b', {'visual': ['2D']}),

    # settings/themes
    (r'\b(zombie|zombies)\b', {'setting': ['zombie apocalypse']}),
    (r'\b(space|galaxy|galactic|spaceship)\b', {'setting': ['space']}),
    (r'\b(sci[- ]?fi|science fiction)\b', {'setting': ['science fiction']}),
    (r'\b(medieval)\b', {'setting': ['medieval']}),
    (r'\b(fantasy world|fantasy rpg|epic fantasy)\b', {'setting': ['fantasy']}),
    (r'\b(post[- ]apocalyptic|apocalypse)\b', {'setting': ['post-apocalyptic']}),
    (r'\b(cyberpunk)\b', {'setting': ['cyberpunk']}),
    (r'\b(pirate|pirates)\b', {'setting': ['pirates']}),
    (r'\b(underwater|under the sea|ocean world)\b', {'setting': ['underwater/ocean']}),
    (r'\b(military|modern warfare|world war)\b', {'setting': ['military/war']}),
    (r'\b(horror game|scary game|terrifying)\b', {'theme': ['horror'], 'pressure': ['horror threat']}),
    (r'\b(cozy|cosy game|relaxing game)\b', {'theme': ['cozy/relaxing']}),
    (r'\b(funny game|comedy game|hilarious)\b', {'theme': ['comedy']}),
]

FIELD_TO_OUTPUT = {
    'core': 'core_mechanics',
    'secondary': 'extra_mechanics',
    'structure': 'structure_tags',
    'progression': 'progression_tags',
    'pressure': 'pressure_tags',
    'interaction': 'interaction_tags',
    'setting': 'setting_tags',
    'theme': 'theme_tags',
    'visual': 'visual_tags',
    'camera': 'camera_tags',
    'players': 'player_tags',
}


def norm_category(value: str) -> str:
    c = (value or '').strip()
    if c.startswith('GAME_'):
        c = c[5:].replace('_', ' ').title()
    return c


def _add_unique(target: list[str], values: Iterable[str]) -> None:
    for value in values:
        if value and value not in target:
            target.append(value)


def enrich_google_play_game(game: dict, store_meta: dict | None = None) -> dict:
    store_meta = store_meta or {}
    category = norm_category(game.get('category') or store_meta.get('genre') or '')
    genre = (store_meta.get('genre') or '').strip()
    title = store_meta.get('title') or game.get('title') or ''
    summary = store_meta.get('summary') or ''
    description = store_meta.get('description') or ''
    text = ' '.join([title, category, genre, summary, description]).lower()

    buckets = {k: [] for k in FIELD_TO_OUTPUT}
    matched_rules = 0
    for pattern, rule in TEXT_RULES:
        if re.search(pattern, text, flags=re.IGNORECASE):
            matched_rules += 1
            for category_key, values in rule.items():
                _add_unique(buckets[category_key], values)

    fallback_sources = []
    category_rule = CATEGORY_BASE.get(category) or CATEGORY_BASE.get(genre)
    if category_rule:
        for category_key, values in category_rule.items():
            if not buckets[category_key]:
                _add_unique(buckets[category_key], values)
                fallback_sources.append(f'category:{category}')

    # Monetization atoms are intentionally separate from mechanics.
    free = bool(game.get('free'))
    ads = bool(game.get('ad_supported')) or bool(store_meta.get('containsAds'))
    iap = bool(game.get('in_app_purchases')) or bool(store_meta.get('offersIAP'))
    monetization = ['free-to-play' if free else 'premium']
    if ads:
        monetization.append('ads')
    if iap:
        monetization.append('in-app purchases')

    out = {FIELD_TO_OUTPUT[k]: v for k, v in buckets.items()}
    out['mobile_business_tags'] = monetization
    out['store_genre'] = genre
    out['store_summary'] = summary[:500] if summary else ''
    out['visual_style'] = buckets['visual'][0] if buckets['visual'] else ''
    out['camera'] = buckets['camera'][0] if buckets['camera'] else ''
    out['player_mode'] = buckets['players'][0] if buckets['players'] else ''
    out['other_tags'] = [x for x in [category, genre] if x]

    atom_count = sum(len(v) for v in buckets.values())
    if atom_count >= 9:
        quality = 'rich'
    elif atom_count >= 4:
        quality = 'medium'
    else:
        quality = 'sparse'
    out['annotation_quality'] = {
        'text_rules_matched': matched_rules,
        'generator_atom_count': atom_count,
        'fallback_used': bool(fallback_sources),
        'fallback_sources': sorted(set(fallback_sources)),
        'store_metadata_used': bool(store_meta),
        'quality': quality,
    }
    return out


def _counter_items(counter: collections.Counter) -> list[dict]:
    return [
        {'label': label, 'weight': 1, 'frequency': count}
        for label, count in counter.most_common() if label
    ]


def build_reels(games: list[dict], library: str = 'google_play_alltime_like', version: int = 4) -> dict:
    fields = [
        ('core', 'КОР', 'core_mechanics'),
        ('secondary', 'ДОП. МЕХАНИКА', 'extra_mechanics'),
        ('structure', 'СТРУКТУРА', 'structure_tags'),
        ('progression', 'ПРОГРЕССИЯ', 'progression_tags'),
        ('pressure', 'ДАВЛЕНИЕ / ОГРАНИЧЕНИЕ', 'pressure_tags'),
        ('interaction', 'ВЗАИМОДЕЙСТВИЕ', 'interaction_tags'),
        ('setting', 'СЕТТИНГ', 'setting_tags'),
        ('theme', 'ТОН / ТЕМА', 'theme_tags'),
        ('graphics', 'ГРАФИКА', 'visual_tags'),
        ('camera', 'КАМЕРА', 'camera_tags'),
        ('players', 'ИГРОКИ', 'player_tags'),
        ('monetization', 'МОНЕТИЗАЦИЯ', 'mobile_business_tags'),
    ]
    counters = {reel_id: collections.Counter() for reel_id, _, _ in fields}
    categories = collections.Counter()
    for g in games:
        for reel_id, _, field in fields:
            counters[reel_id].update(x for x in (g.get(field) or []) if x)
        if g.get('category'):
            categories[g['category']] += 1
    reels = [
        {'id': reel_id, 'label_ru': label, 'items': merge_curated_items(_counter_items(counters[reel_id]), reel_id)}
        for reel_id, label, _ in fields
    ]
    for extra_id, extra_label in [('world','МИР / ЭПОХА'),('location','ЛОКАЦИЯ'),('environment','ОКРУЖЕНИЕ / БИОМ'),('twist','ТВИСТ')]:
        reels.append({'id': extra_id, 'label_ru': extra_label, 'items': merge_curated_items([], extra_id)})
    reels.append({'id': 'genre', 'label_ru': 'МОБИЛЬНЫЙ ЖАНР', 'items': _counter_items(categories)})
    reels.append({
        'id': 'reference_game',
        'label_ru': 'ИГРА-РЕФЕРЕНС',
        'items': [
            {'label': g.get('title', ''), 'app_id': g.get('app_id', ''), 'image': g.get('local_image', ''), 'weight': 1}
            for g in games if g.get('title')
        ],
    })
    return {
        'version': 5,
        'library': library,
        'source_games': len(games),
        'weighting': 'uniform_by_atom_plus_curated_v5',
        'note': 'frequency is informational; weight=1 by default to reduce repeated common atoms',
        'reels': reels,
    }


def write_coverage_report(games: list[dict], output_path: str | Path) -> None:
    fields = [
        'core_mechanics', 'extra_mechanics', 'structure_tags', 'progression_tags', 'pressure_tags',
        'interaction_tags', 'setting_tags', 'theme_tags', 'visual_tags', 'camera_tags', 'player_tags',
    ]
    qualities = collections.Counter((g.get('annotation_quality') or {}).get('quality', 'unknown') for g in games)
    coverage = {}
    for field in fields:
        nonempty = sum(1 for g in games if g.get(field))
        unique = len({x for g in games for x in (g.get(field) or []) if x})
        coverage[field] = {
            'games_with_value': nonempty,
            'coverage_pct': round(nonempty * 100 / max(1, len(games)), 1),
            'unique_atoms': unique,
        }
    sparse = [
        {'rank': g.get('source_rank'), 'title': g.get('title'), 'app_id': g.get('app_id'), 'category': g.get('category')}
        for g in games if (g.get('annotation_quality') or {}).get('quality') == 'sparse'
    ]
    Path(output_path).write_text(json.dumps({
        'games': len(games), 'quality_counts': dict(qualities), 'coverage': coverage, 'sparse_games': sparse
    }, ensure_ascii=False, indent=2), encoding='utf-8')
