from __future__ import annotations

import collections
import json
import re
from pathlib import Path
from typing import Iterable
from curated_reels import merge_curated_items

# Rich, conservative mapping from common Steam user tags to generator atoms.
# A single Steam tag may contribute to several independent reels.
TAG_RULES: dict[str, dict[str, list[str]]] = {
    # Core actions / loops
    'FPS': {'core': ['shooting'], 'camera': ['first-person']},
    'Shooter': {'core': ['shooting']},
    'Third-Person Shooter': {'core': ['shooting'], 'camera': ['third-person']},
    'Looter Shooter': {'core': ['shooting'], 'progression': ['loot-driven progression']},
    'Hero Shooter': {'core': ['shooting'], 'progression': ['hero/ability progression']},
    'Extraction Shooter': {'core': ['shooting'], 'structure': ['extraction runs'], 'pressure': ['risk/reward extraction']},
    'Arena Shooter': {'core': ['shooting'], 'structure': ['arena matches']},
    'Twin Stick Shooter': {'core': ['shooting'], 'camera': ['top-down']},
    'Shoot Em Up': {'core': ['shooting'], 'pressure': ['projectile avoidance']},
    "Shoot 'Em Up": {'core': ['shooting'], 'pressure': ['projectile avoidance']},
    'Bullet Hell': {'core': ['projectile dodging'], 'pressure': ['dense projectile pressure']},
    'Bullet Heaven': {'core': ['auto-combat survival'], 'structure': ['run-based sessions']},
    'Hack and Slash': {'core': ['melee combat']},
    "Beat 'em up": {'core': ['beat-em-up combat']},
    'Souls-like': {'core': ['deliberate melee combat'], 'progression': ['checkpoint progression'], 'pressure': ['high punishment']},
    'Swordplay': {'core': ['melee combat']},
    'Combat': {'core': ['combat']},
    'Action RPG': {'core': ['action combat'], 'progression': ['rpg progression']},
    'RPG': {'progression': ['rpg progression']},
    'JRPG': {'core': ['party rpg combat'], 'progression': ['rpg progression']},
    'CRPG': {'core': ['party rpg systems'], 'progression': ['rpg progression']},
    'Turn-Based Combat': {'core': ['turn-based combat']},
    'Turn-Based Strategy': {'core': ['turn-based strategy']},
    'Turn-Based Tactics': {'core': ['turn-based tactics']},
    'Strategy': {'core': ['strategy']},
    'RTS': {'core': ['real-time strategy']},
    '4X': {'core': ['4X strategy'], 'progression': ['empire progression']},
    'Grand Strategy': {'core': ['grand strategy'], 'progression': ['long-form empire progression']},
    'Tower Defense': {'core': ['tower defense'], 'pressure': ['wave defense']},
    'City Builder': {'core': ['city building'], 'progression': ['settlement growth']},
    'Colony Sim': {'core': ['colony management'], 'progression': ['colony growth']},
    'Management': {'core': ['management']},
    'Life Sim': {'core': ['life simulation'], 'structure': ['open-ended life loop']},
    'Farming Sim': {'core': ['farming'], 'progression': ['farm growth']},
    'Cooking': {'core': ['cooking']},
    'Fishing': {'core': ['fishing']},
    'Hunting': {'core': ['hunting']},
    'Driving': {'core': ['driving']},
    'Racing': {'core': ['racing']},
    'Flight': {'core': ['flight']},
    'Platformer': {'core': ['platforming']},
    '3D Platformer': {'core': ['3D platforming'], 'camera': ['third-person']},
    'Precision Platformer': {'core': ['precision platforming'], 'pressure': ['precision execution']},
    'Puzzle': {'core': ['puzzles']},
    'Puzzle Platformer': {'core': ['puzzle platforming']},
    'Match 3': {'core': ['match-3']},
    'Hidden Object': {'core': ['hidden-object searching']},
    'Investigation': {'core': ['investigation']},
    'Detective': {'core': ['investigation']},
    'Stealth': {'core': ['stealth'], 'pressure': ['detection avoidance']},
    'Rhythm': {'core': ['rhythm']},
    'Music': {'core': ['music/rhythm interaction']},
    'Typing': {'core': ['typing']},
    'Trivia': {'core': ['quiz/knowledge']},
    'Word Game': {'core': ['word puzzles']},
    'Card Game': {'core': ['card play']},
    'Card Battler': {'core': ['card combat']},
    'Deckbuilding': {'core': ['deckbuilding'], 'progression': ['deck growth']},
    'Roguelike Deckbuilder': {'core': ['deckbuilding'], 'structure': ['roguelike runs'], 'progression': ['run deck growth']},
    'Creature Collector': {'core': ['creature collection'], 'progression': ['collection progression']},
    'Auto Battler': {'core': ['auto battler'], 'progression': ['team composition progression']},
    'Idler': {'core': ['idle progression'], 'structure': ['idle sessions'], 'progression': ['passive growth']},
    'Incremental': {'core': ['incremental progression'], 'progression': ['exponential growth']},
    'Clicker': {'core': ['clicker/incremental']},
    'Automation': {'core': ['automation'], 'progression': ['factory/system scaling']},
    'Base Building': {'core': ['base building'], 'progression': ['base growth'], 'interaction': ['construction']},
    'Building': {'core': ['building'], 'interaction': ['construction']},
    'Crafting': {'core': ['crafting'], 'interaction': ['crafting']},
    'Trading': {'core': ['trading'], 'interaction': ['buy/sell decisions']},
    'Economy': {'core': ['economy management']},
    'Resource Management': {'core': ['resource management'], 'pressure': ['resource scarcity']},
    'Transportation': {'core': ['transport/logistics']},
    'Logistics': {'core': ['logistics']},
    'Exploration': {'core': ['exploration']},
    'Dungeon Crawler': {'core': ['dungeon crawling'], 'structure': ['dungeon runs']},
    'Survival': {'core': ['survival'], 'pressure': ['survival needs']},
    'Survival Horror': {'core': ['survival'], 'pressure': ['scarce resources', 'horror threat'], 'theme': ['horror']},
    'Social Deduction': {'core': ['social deduction'], 'interaction': ['hidden-role communication']},
    'Visual Novel': {'core': ['choice/narrative reading'], 'structure': ['branching narrative']},
    'Interactive Fiction': {'core': ['choice/narrative reading'], 'structure': ['branching narrative']},
    'Dating Sim': {'core': ['relationship choices'], 'progression': ['relationship progression']},
    'Immersive Sim': {'core': ['systemic problem solving'], 'interaction': ['multiple-solution interactions']},

    # Secondary / interaction systems
    'Physics': {'core': ['physics interaction'], 'interaction': ['physics manipulation']},
    'Destruction': {'core': ['destruction'], 'interaction': ['destructible environment']},
    'Inventory Management': {'secondary': ['inventory management'], 'interaction': ['inventory decisions']},
    'Procedural Generation': {'secondary': ['procedural generation'], 'structure': ['procedurally varied content']},
    'Character Customization': {'secondary': ['character customization'], 'progression': ['build customization']},
    'Moddable': {'secondary': ['modding support']},
    'Time Management': {'secondary': ['time management'], 'pressure': ['time pressure']},
    'Parkour': {'secondary': ['parkour traversal'], 'interaction': ['movement mastery']},
    'Climbing': {'secondary': ['climbing'], 'interaction': ['vertical traversal']},
    'Grappling Hook': {'secondary': ['grappling-hook traversal'], 'interaction': ['momentum traversal']},
    'Loot': {'secondary': ['loot collection'], 'progression': ['loot-driven progression']},
    'Mining': {'secondary': ['resource extraction'], 'interaction': ['mining']},
    'Farming': {'secondary': ['farming']},
    'Collectathon': {'secondary': ['collecting'], 'progression': ['collection completion']},
    'Choices Matter': {'secondary': ['meaningful choices'], 'structure': ['branching outcomes']},
    'Multiple Endings': {'secondary': ['multiple endings'], 'structure': ['branching outcomes']},
    'Dialogue': {'secondary': ['dialogue interaction'], 'interaction': ['conversation choices']},
    'Level Editor': {'secondary': ['level creation'], 'interaction': ['user-generated content']},
    'Character Action Game': {'secondary': ['combo combat']},
    'Quick-Time Events': {'secondary': ['quick-time events']},
    'Vehicular Combat': {'core': ['vehicular combat'], 'interaction': ['vehicle control']},

    # Structure
    'Roguelike': {'structure': ['roguelike runs'], 'progression': ['run reset/progression']},
    'Roguelite': {'structure': ['roguelite runs'], 'progression': ['meta progression']},
    'Action Roguelike': {'structure': ['roguelite runs'], 'progression': ['meta progression']},
    'Open World': {'structure': ['open world']},
    'Sandbox': {'structure': ['sandbox']},
    'Metroidvania': {'structure': ['interconnected gated world'], 'progression': ['ability-gated progression']},
    'Battle Royale': {'structure': ['battle-royale matches'], 'pressure': ['shrinking/last-player pressure'], 'players': ['competitive multiplayer']},
    'Wave Based': {'structure': ['wave-based sessions'], 'pressure': ['escalating waves']},
    'Score Attack': {'structure': ['score-attack sessions']},
    'Linear': {'structure': ['linear campaign']},
    'Story Rich': {'structure': ['narrative campaign']},
    'Campaign': {'structure': ['campaign']},
    'MMORPG': {'structure': ['persistent online world'], 'players': ['massively multiplayer']},
    'Massively Multiplayer': {'structure': ['persistent multiplayer'], 'players': ['massively multiplayer']},

    # Progression
    'Skill Tree': {'progression': ['skill-tree progression']},
    'Leveling': {'progression': ['level-based progression']},
    'Class-Based': {'progression': ['class/build progression']},
    'Perma Death': {'pressure': ['permadeath']},
    'Permadeath': {'pressure': ['permadeath']},
    'Difficult': {'pressure': ['high difficulty']},
    'Hardcore': {'pressure': ['hardcore punishment']},

    # Camera
    'First-Person': {'camera': ['first-person']},
    'Third Person': {'camera': ['third-person']},
    'Top-Down': {'camera': ['top-down']},
    'Isometric': {'camera': ['isometric']},
    'Side Scroller': {'camera': ['side-view']},
    'Side-Scrolling': {'camera': ['side-view']},
    'Fixed Camera': {'camera': ['fixed camera']},

    # Visuals
    'Pixel Graphics': {'visual': ['pixel art']},
    'Voxel': {'visual': ['voxel 3D']},
    '2D': {'visual': ['2D']},
    '3D': {'visual': ['3D']},
    '2.5D': {'visual': ['2.5D']},
    'Hand-drawn': {'visual': ['hand-drawn']},
    'Anime': {'visual': ['anime stylized']},
    'Cartoony': {'visual': ['cartoon stylized']},
    'Cartoon': {'visual': ['cartoon stylized']},
    'Realistic': {'visual': ['realistic']},
    'Low-Poly': {'visual': ['low-poly 3D']},
    'Minimalist': {'visual': ['minimalist']},
    'Psychedelic': {'visual': ['psychedelic']},
    'Surreal': {'visual': ['surreal']},
    'Retro': {'visual': ['retro']},
    'Stylized': {'visual': ['stylized']},
    'FMV': {'visual': ['full-motion video']},
    'Text-Based': {'visual': ['text-based']},
    'Noir': {'visual': ['noir']},

    # Players
    'Singleplayer': {'players': ['singleplayer']},
    'Multiplayer': {'players': ['multiplayer']},
    'Co-op': {'players': ['co-op']},
    'Online Co-Op': {'players': ['online co-op']},
    'Local Co-Op': {'players': ['local co-op']},
    'Local Multiplayer': {'players': ['local multiplayer']},
    'PvP': {'players': ['competitive multiplayer'], 'pressure': ['player competition']},
    'PvE': {'players': ['cooperative/AI opposition']},
    'Split Screen': {'players': ['split-screen multiplayer']},
    'Party Game': {'players': ['party multiplayer'], 'structure': ['short party rounds']},
    '4 Player Local': {'players': ['4-player local']},

    # Settings
    'Space': {'setting': ['space']},
    'Sci-fi': {'setting': ['science fiction']},
    'Science': {'setting': ['science/technology']},
    'Cyberpunk': {'setting': ['cyberpunk']},
    'Medieval': {'setting': ['medieval']},
    'Fantasy': {'setting': ['fantasy']},
    'Dark Fantasy': {'setting': ['dark fantasy'], 'theme': ['dark']},
    'Post-apocalyptic': {'setting': ['post-apocalyptic']},
    'Zombies': {'setting': ['zombie apocalypse']},
    'Military': {'setting': ['military']},
    'War': {'setting': ['war']},
    'World War II': {'setting': ['World War II']},
    'World War I': {'setting': ['World War I']},
    'Western': {'setting': ['western']},
    'Pirates': {'setting': ['pirates']},
    'Underwater': {'setting': ['underwater']},
    'Naval': {'setting': ['naval']},
    'Historical': {'setting': ['historical']},
    'Futuristic': {'setting': ['futuristic']},
    'Modern': {'setting': ['modern']},
    'Steampunk': {'setting': ['steampunk']},
    'Dystopian': {'setting': ['dystopian']},
    'Lovecraftian': {'setting': ['lovecraftian'], 'theme': ['cosmic horror']},
    'Supernatural': {'setting': ['supernatural']},
    'Vikings': {'setting': ['vikings']},
    'Dinosaurs': {'setting': ['dinosaurs']},


    'Crime': {'setting': ['crime/underworld']},
    'Aliens': {'setting': ['aliens']},
    'Dragons': {'setting': ['dragons/fantasy']},
    'America': {'setting': ['modern America']},
    'Nature': {'setting': ['wilderness/nature']},
    'Superhero': {'setting': ['superheroes']},

    # Themes / mood

    'Funny': {'theme': ['comedy']},
    'Dark': {'theme': ['dark']},
    'Cats': {'theme': ['cats']},
    'Cinematic': {'visual': ['cinematic presentation']},

    'Horror': {'theme': ['horror'], 'pressure': ['horror threat']},
    'Psychological Horror': {'theme': ['psychological horror']},
    'Atmospheric': {'theme': ['atmospheric']},
    'Relaxing': {'theme': ['relaxing']},
    'Cozy': {'theme': ['cozy']},
    'Cute': {'theme': ['cute']},
    'Wholesome': {'theme': ['wholesome']},
    'Comedy': {'theme': ['comedy']},
    'Dark Humor': {'theme': ['dark comedy']},
    'Mystery': {'theme': ['mystery']},
    'Emotional': {'theme': ['emotional']},
    'Colorful': {'theme': ['colorful']},
}

GENRE_FALLBACK: dict[str, dict[str, list[str]]] = {
    'Action': {'core': ['action combat']},
    'Adventure': {'core': ['exploration/adventure']},
    'Strategy': {'core': ['strategy']},
    'Simulation': {'core': ['simulation']},
    'RPG': {'core': ['role-playing systems'], 'progression': ['rpg progression']},
    'Racing': {'core': ['racing']},
    'Sports': {'core': ['sports mechanics']},
    'Casual': {'core': ['casual short-session loop']},
    'Massively Multiplayer': {'structure': ['persistent multiplayer'], 'players': ['massively multiplayer']},
}

IGNORE_AS_ATOM = {
    'Indie', 'Free to Play', 'Early Access', 'Steam Achievements', 'Steam Trading Cards',
    'Great Soundtrack', 'Soundtrack', 'Female Protagonist', 'Male Protagonist', 'Family Friendly',
    'Controller', 'Mouse only', 'Keyboard Only', 'VR', 'VR Only', 'VR Supported',
    'Singleplayer', 'Multiplayer', 'Co-op', 'Online Co-Op', 'Local Co-Op', 'PvP', 'PvE',
    '2D', '3D', '2.5D', 'First-Person', 'Third Person', 'Top-Down', 'Isometric',
}

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


def _add_unique(target: list[str], values: Iterable[str]) -> None:
    for value in values:
        if value and value not in target:
            target.append(value)


def _genre_tokens(genre: str) -> list[str]:
    return [x.strip() for x in re.split(r'[,;/|]', genre or '') if x.strip()]


def normalize_raw_atom(tag: str) -> str:
    s = re.sub(r'\s+', ' ', (tag or '').strip())
    return s.lower()


def enrich_steam_game(tags: Iterable[str], genre: str = '', title: str = '') -> dict:
    tags = [str(t).strip() for t in tags if str(t).strip()]
    buckets: dict[str, list[str]] = {k: [] for k in FIELD_TO_OUTPUT}
    recognized: set[str] = set()

    for tag in tags:
        rule = TAG_RULES.get(tag)
        if not rule:
            continue
        recognized.add(tag)
        for category, values in rule.items():
            _add_unique(buckets[category], values)

    fallback_sources: list[str] = []
    # Only use genre as a fallback for missing core/progression/etc.; do not overwrite richer Steam tags.
    for token in _genre_tokens(genre):
        rule = GENRE_FALLBACK.get(token)
        if not rule:
            continue
        used = False
        for category, values in rule.items():
            if not buckets[category]:
                _add_unique(buckets[category], values)
                used = True
        if used:
            fallback_sources.append(f'genre:{token}')

    # Conservative title fallbacks for very sparse records. These are intentionally high-signal only.
    title_l = (title or '').lower()
    title_rules = [
        (r'\b(racing|race|rally|drift|kart)\b', 'core', ['racing']),
        (r'\b(farm|farming|harvest)\b', 'core', ['farming']),
        (r'\b(fishing|fish)\b', 'core', ['fishing']),
        (r'\b(chess)\b', 'core', ['board-game strategy']),
        (r'\b(poker|solitaire|cards?)\b', 'core', ['card play']),
        (r'\b(tycoon|manager|management)\b', 'core', ['management']),
        (r'\b(simulator|simulation)\b', 'core', ['simulation']),
        (r'\b(zombie|zombies)\b', 'setting', ['zombie apocalypse']),
        (r'\b(space|galaxy|galactic)\b', 'setting', ['space']),
    ]
    for pattern, category, values in title_rules:
        if not buckets[category] and re.search(pattern, title_l):
            _add_unique(buckets[category], values)
            fallback_sources.append(f'title:{pattern}')

    # Keep useful unmapped Steam tags so the app can search/pin them, without polluting core reels.
    other_tags = []
    for tag in tags:
        if tag in recognized or tag in IGNORE_AS_ATOM:
            continue
        atom = normalize_raw_atom(tag)
        if atom and atom not in other_tags:
            other_tags.append(atom)

    out = {FIELD_TO_OUTPUT[k]: v for k, v in buckets.items()}
    out['other_tags'] = other_tags
    atom_count = sum(len(v) for v in buckets.values())
    if atom_count >= 10:
        quality = 'rich'
    elif atom_count >= 5:
        quality = 'medium'
    else:
        quality = 'sparse'
    out['annotation_quality'] = {
        'source_tag_count': len(tags),
        'recognized_source_tag_count': len(recognized),
        'generator_atom_count': atom_count,
        'other_tag_count': len(other_tags),
        'fallback_used': bool(fallback_sources),
        'fallback_sources': fallback_sources,
        'quality': quality,
    }
    # Backward-compatible single-value fields. Empty means unknown and is intentionally not a reel item.
    out['visual_style'] = buckets['visual'][0] if buckets['visual'] else ''
    out['camera'] = buckets['camera'][0] if buckets['camera'] else ''
    out['player_mode'] = buckets['players'][0] if buckets['players'] else ''
    return out


def _counter_items(counter: collections.Counter, *, diverse: bool = True) -> list[dict]:
    # Frequency is retained for analytics, but default random weight is flat to prevent common tags
    # from appearing 10–20x more often than rare, useful atoms.
    return [
        {'label': label, 'weight': 1 if diverse else max(1, min(5, count)), 'frequency': count}
        for label, count in counter.most_common()
        if label
    ]


def build_reels(games: list[dict], library: str, version: int = 4) -> dict:
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
    ]
    counters = {reel_id: collections.Counter() for reel_id, _, _ in fields}
    genre_counter = collections.Counter()
    other_counter = collections.Counter()

    for g in games:
        for reel_id, _, field in fields:
            counters[reel_id].update(x for x in (g.get(field) or []) if x)
        for token in _genre_tokens(g.get('genre') or g.get('primary_genre') or ''):
            genre_counter[token] += 1
        other_counter.update(x for x in (g.get('other_tags') or []) if x)

    reels = []
    for reel_id, label, _ in fields:
        reels.append({'id': reel_id, 'label_ru': label, 'items': merge_curated_items(_counter_items(counters[reel_id]), reel_id)})
    for extra_id, extra_label in [('world','МИР / ЭПОХА'),('location','ЛОКАЦИЯ'),('environment','ОКРУЖЕНИЕ / БИОМ'),('twist','ТВИСТ')]:
        reels.append({'id': extra_id, 'label_ru': extra_label, 'items': merge_curated_items([], extra_id)})
    reels.append({'id': 'genre', 'label_ru': 'ЖАНР', 'items': _counter_items(genre_counter)})
    # Raw-but-useful tags are optional: searchable/pinnable and usable as a wild-card reel.
    reels.append({'id': 'other', 'label_ru': 'ПРОЧИЙ ТЕГ', 'items': _counter_items(other_counter)})
    reels.append({
        'id': 'reference_game',
        'label_ru': 'ИГРА-РЕФЕРЕНС',
        'items': [
            {
                'label': g.get('title', ''),
                'appid': g.get('appid', ''),
                'image': g.get('local_image', ''),
                'weight': 1,
            }
            for g in games if g.get('title')
        ],
    })
    return {
        'version': 5,
        'library': library,
        'source_games': len(games),
        'weighting': 'uniform_by_atom_plus_curated_v5',
        'note': 'frequency is informational; weight defaults to 1 to improve variety',
        'reels': reels,
    }


def write_coverage_report(games: list[dict], output_path: str | Path) -> None:
    path = Path(output_path)
    qualities = collections.Counter((g.get('annotation_quality') or {}).get('quality', 'unknown') for g in games)
    fields = [
        'core_mechanics', 'extra_mechanics', 'structure_tags', 'progression_tags', 'pressure_tags',
        'interaction_tags', 'setting_tags', 'theme_tags', 'visual_tags', 'camera_tags', 'player_tags',
    ]
    coverage = {}
    for field in fields:
        nonempty = sum(1 for g in games if g.get(field))
        unique = len({x for g in games for x in (g.get(field) or []) if x})
        coverage[field] = {'games_with_value': nonempty, 'coverage_pct': round(nonempty * 100 / max(1, len(games)), 1), 'unique_atoms': unique}
    sparse = []
    for g in games:
        q = g.get('annotation_quality') or {}
        if q.get('quality') == 'sparse':
            sparse.append({
                'rank': g.get('source_rank'), 'title': g.get('title'), 'appid': g.get('appid'),
                'source_tags': q.get('source_tag_count', 0), 'atoms': q.get('generator_atom_count', 0),
            })
    payload = {
        'games': len(games),
        'quality_counts': dict(qualities),
        'coverage': coverage,
        'sparse_games': sparse,
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
