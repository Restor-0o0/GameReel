"""Refreshes store artwork in the bundled libraries (steam header, google play icon) and ships it with the app."""
import io
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
BUNDLED = ROOT.parent / 'app/shared/src/commonMain/composeResources/files/libraries'
CACHE = ROOT / 'library_sources/store_images.json'
ART = ROOT.parent / 'app/shared/src/commonMain/composeResources/files/art'
PRESENTATION = ROOT.parent / 'app/shared/src/commonMain/kotlin/com/restor/gamereel/ui/presentation'
VERSIONS = {'steam_broad': 7, 'steam_alltime': 7, 'google_play': 8}
SIZES = {'steam': (320, 150), 'ref': (320, 150), 'play': (128, 128)}
EDITIONS = r'(directorscut|definitiveedition|remastered|remake|goty|gameoftheyearedition|completeedition|enhancededition|' \
    r'deluxeedition|specialedition|legendaryedition|anniversaryedition|royal|hd|redux|thefinalcut|finalcut|ultimateedition|' \
    r'enhanced|legacy|goldedition|gotyedition|landmarkedition|connected|treasuretrove|\d{4})?'
AGENT ={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Accept-Language': 'en-US,en;q=0.9'}


def request(url, method='GET'):
    with urllib.request.urlopen(urllib.request.Request(url, headers=AGENT, method=method), timeout=20) as response:
        return response.status, response.read() if method == 'GET' else b''


def alive(url):
    try:
        return request(url, 'HEAD')[0] == 200
    except Exception:
        return False


def steam_header(appid):
    legacy = f'https://cdn.akamai.steamstatic.com/steam/apps/{appid}/header.jpg'
    if alive(legacy):
        return legacy
    for attempt in range(5):
        try:
            data = json.loads(request(f'https://store.steampowered.com/api/appdetails?appids={appid}&filters=basic')[1])
            # steam sometimes answers under a different id
            entry = data.get(str(appid)) or next(iter(data.values()), {})
            return (entry.get('data') or {}).get('header_image') if entry.get('success') else None
        except urllib.error.HTTPError as error:
            if error.code != 429:
                return None
            time.sleep(30 * (attempt + 1))
        except Exception:
            return None
    return None


def play_icon(app_id):
    try:
        html = request(f'https://play.google.com/store/apps/details?id={app_id}&hl=en&gl=US')[1].decode('utf-8', 'replace')
    except Exception:
        return None
    match = re.search(r'<meta property="og:image" content="([^"]+)"', html)
    # og:image is the icon for apps; strip the size suffix and ask for a fixed one
    return re.sub(r'=[swh]\d+.*$', '', match.group(1)) + '=s256' if match else None


def art_file(key):
    return ART / (key.replace(':', '_') + '.webp')


def bundle(key, url):
    target = art_file(key)
    if target.exists():
        return True
    try:
        image = Image.open(io.BytesIO(request(url)[1])).convert('RGBA')
    except Exception:
        return False
    image.thumbnail(SIZES[key.split(':')[0]], Image.LANCZOS)
    image.save(target, 'WEBP', quality=72, method=6)
    return True


def title_key(title):
    # keep in sync with GameArt.titleKey
    return ''.join(ch for ch in re.sub(r'\([^)]*\)', '', title).lower() if ch.isalnum())


def reference_titles():
    source = (PRESENTATION / 'CuratedReferences.kt').read_text(encoding='utf-8')
    titles = []
    for value in re.findall(r'^\s+"[^"]*" to "((?:[^"\\]|\\.)*)",$', source, re.M):
        value = value.replace('\\"', '"').replace('\\$', '$').replace('\\\\', '\\')
        titles += value.split('\\n')
    return titles


def steam_search(title):
    key = title_key(title)
    for attempt in range(5):
        try:
            term = re.sub(r'\([^)]*\)', '', title).strip()
            data = json.loads(request('https://store.steampowered.com/api/storesearch/?cc=us&l=en&term=' + urllib.parse.quote(term))[1])
            break
        except urllib.error.HTTPError as error:
            if error.code != 429:
                return None
            time.sleep(30 * (attempt + 1))
        except Exception:
            return None
    else:
        return None
    items = [(title_key(item['name']), item) for item in data.get('items', [])]
    match = next((item for other, item in items if other == key), None) or next((item for other, item in items
        # same game in a re-release: "death stranding director's cut", "prey (2017)"
        if other.startswith(key) and re.fullmatch(EDITIONS, other[len(key):])), None) or next((item for other, item in items
        # publisher prefix: "tom clancy's rainbow six siege"
        if len(key) >= 8 and other.endswith(key)), None)
    if not match:
        return None
    # the search capsule usually sits next to the header
    header = re.sub(r'capsule_231x87\.jpg.*$', 'header.jpg', match['tiny_image'])
    return header if alive(header) else steam_header(match['id'])


def play_search(title):
    key = title_key(title)
    try:
        term = re.sub(r'\([^)]*\)', '', title).strip()
        html = request('https://play.google.com/store/search?c=apps&hl=en&gl=US&q=' + urllib.parse.quote(term))[1].decode('utf-8', 'replace')
    except Exception:
        return None
    found = []
    for app_id in list(dict.fromkeys(re.findall(r'/store/apps/details\?id=([\w.]+)', html)))[:3]:
        try:
            page = request(f'https://play.google.com/store/apps/details?id={app_id}&hl=en&gl=US')[1].decode('utf-8', 'replace')
        except Exception:
            continue
        name = re.search(r'<meta property="og:title" content="([^"]+?)(?: - Apps on Google Play)?"', page)
        icon = re.search(r'<meta property="og:image" content="([^"]+)"', page)
        if name and icon:
            found.append((title_key(name.group(1)), re.sub(r'=[swh]\d+.*$', '', icon.group(1)) + '=s256'))
    exact = next((url for other, url in found if other == key), None)
    if exact or not found:
        return exact
    # only trust the top hit for "angry birds 2", "pokemon go"; short names pull in clones and utilities
    other, url = found[0]
    suffix = other[len(key):]
    ok = other.startswith(key) and (re.fullmatch(EDITIONS, suffix) or re.fullmatch(r'\d+|go|mobile|\d+\w*anniversary', suffix) or len(key) >= 8)
    return url if ok else None


def reference_art(title):
    return steam_search(title) or play_search(title)


def archived_play_icon(app_id):
    # delisted apps: the last wayback snapshot still points at a live play-lh icon
    try:
        meta = json.loads(request(f'https://archive.org/wayback/available?url=play.google.com/store/apps/details?id={app_id}')[1])
        snapshot = (meta.get('archived_snapshots') or {}).get('closest')
        if not snapshot:
            return None
        html = request(snapshot['url'])[1].decode('utf-8', 'replace')
    except Exception:
        return None
    match = re.search(r'<meta (?:property|itemprop)="(?:og:)?image" content="(?:https?://web\.archive\.org/web/\d+(?:im_)?/)?([^"]+)"', html)
    if not match:
        return None
    icon = re.sub(r'=[swh]\d+.*$', '', match.group(1)) + '=s256'
    return icon if alive(icon) else None


def mobile_art(game):
    return archived_play_icon(game['app_id']) or play_search(game['title']) or steam_search(game['title'])


def fetch_references(cache, library_keys, refresh):
    titles = {}
    for title in reference_titles():
        key = title_key(title)
        if key and key not in library_keys:
            titles.setdefault(key, title)
    jobs = {f'ref:{key}': title for key, title in titles.items() if refresh or not cache.get(f'ref:{key}', '') and '--retry-missing' in sys.argv or f'ref:{key}' not in cache}
    print(f'searching {len(jobs)} reference games')
    with ThreadPoolExecutor(4) as pool:
        for done, (key, url) in enumerate(zip(jobs, pool.map(reference_art, jobs.values())), 1):
            cache[key] = url
            if done % 50 == 0:
                print(done, flush=True)
                CACHE.write_text(json.dumps(cache, indent=0, sort_keys=True), encoding='utf-8')
    return {f'ref:{key}' for key in titles}


def main():
    cache = json.loads(CACHE.read_text(encoding='utf-8')) if CACHE.exists() else {}
    refresh = '--refresh' in sys.argv
    files = {name: json.loads((BUNDLED / f'{name}.json').read_text(encoding='utf-8')) for name in ('steam_broad', 'steam_alltime', 'google_play')}
    jobs = {}
    for name, payload in files.items():
        for game in payload['games']:
            key = f"play:{game['app_id']}" if name == 'google_play' else f"steam:{game['appid']}"
            if refresh or key not in cache:
                jobs[key] = (play_icon, game['app_id']) if name == 'google_play' else (steam_header, game['appid'])
    print(f'fetching {len(jobs)} urls')
    with ThreadPoolExecutor(8) as pool:
        for done, (key, url) in enumerate(zip(jobs, pool.map(lambda job: job[0](job[1]), jobs.values())), 1):
            cache[key] = url
            if done % 100 == 0:
                print(done)
                CACHE.write_text(json.dumps(cache, indent=0, sort_keys=True), encoding='utf-8')
    # delisted store pages: look the game up by title instead
    lost = {f"play:{game['app_id']}" if name == 'google_play' else f"steam:{game['appid']}": (name, game)
        for name, payload in files.items() for game in payload['games']}
    lost = {key: value for key, value in lost.items() if not cache.get(key) and (key in jobs or '--retry-missing' in sys.argv)}
    print(f'searching {len(lost)} games by title')
    with ThreadPoolExecutor(4) as pool:
        for key, url in zip(lost, pool.map(lambda job: mobile_art(job[1]) if job[0] == 'google_play' else reference_art(job[1]['title']), lost.values())):
            cache[key] = url
    library_keys = {title_key(game['title']) for name, payload in files.items() for game in payload['games']
        if cache.get(f"play:{game['app_id']}" if name == 'google_play' else f"steam:{game['appid']}")}
    references = fetch_references(cache, library_keys, refresh)
    CACHE.write_text(json.dumps(cache, indent=0, sort_keys=True), encoding='utf-8')
    ART.mkdir(parents=True, exist_ok=True)
    current = {f"play:{game['app_id']}" if name == 'google_play' else f"steam:{game['appid']}"
        for name, payload in files.items() for game in payload['games']}
    wanted = {key for key, url in cache.items() if url and (key in references if key.startswith('ref:') else key in current)}
    with ThreadPoolExecutor(8) as pool:
        bundled = dict(zip(wanted, pool.map(lambda key: bundle(key, cache[key]), wanted)))
    for stale in ART.glob('*.webp'):
        if stale.stem.replace('_', ':', 1) not in wanted:
            stale.unlink()
    print(f'art: {sum(bundled.values())} files, {sum(f.stat().st_size for f in ART.glob("*.webp")) // 1024} KB')
    found = sorted(key.split(':', 1)[1] for key in wanted if key.startswith('ref:') and art_file(key).exists())
    print(f'reference art: {len(found)}/{len(references)}')
    (PRESENTATION / 'ReferenceArt.kt').write_text('package com.restor.gamereel.ui.presentation\n\n'
        '// generated by tools/fetch_store_images.py, dont edit\n'
        'internal val referenceArt = """\n' + '\n'.join(found) + '\n""".lineSequence().filter { it.isNotBlank() }.toHashSet()\n', encoding='utf-8')
    for name, payload in files.items():
        payload['annotation_version'] = VERSIONS[name]
        for game in payload['games']:
            key = f"play:{game['app_id']}" if name == 'google_play' else f"steam:{game['appid']}"
            if cache.get(key) and art_file(key).exists():
                game['header_image_url'] = cache[key]
            else:
                game.pop('header_image_url', None)
                game.pop('image_url', None)
        (BUNDLED / f'{name}.json').write_text(json.dumps(payload, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
        found = sum(1 for game in payload['games'] if game.get('header_image_url'))
        print(f'{name}: {found}/{len(payload["games"])} images')


if __name__ == '__main__':
    main()
