from __future__ import annotations
import argparse, json
from pathlib import Path
from collections import defaultdict

TAG_FIELDS = {
  'core':'core_mechanics','secondary':'extra_mechanics','structure':'structure_tags',
  'progression':'progression_tags','pressure':'pressure_tags','interaction':'interaction_tags',
  'setting':'setting_tags','theme':'theme_tags','graphics':'visual_tags','camera':'camera_tags',
  'players':'player_tags','other':'other_tags'
}

def load_games(path: Path):
    data=json.loads(path.read_text(encoding='utf-8'))
    if isinstance(data,list): return data
    for key in ('games','items','data'):
        if isinstance(data.get(key),list): return data[key]
    raise ValueError(f'Cannot find game list in {path}')

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('inputs', nargs='+', help='One or more games_1000.json files')
    ap.add_argument('--out', default='game_tag_dictionary.json')
    ap.add_argument('--reverse-out', default='tag_to_games.json')
    args=ap.parse_args()
    games_out=[]; reverse=defaultdict(list); seen_games=set()
    for inp in args.inputs:
        p=Path(inp); library=p.parent.parent.name if p.parent.name=='data' else p.stem
        for g in load_games(p):
            gid=str(g.get('appid') or g.get('app_id') or g.get('id') or '')
            title=str(g.get('title') or g.get('name') or '').strip()
            unique_key=(library,gid or title.lower())
            if unique_key in seen_games: continue
            seen_games.add(unique_key)
            by_reel={}
            flat=[]
            # source/store tags first
            source_tags=[]
            for key in ('tags','steam_tags','store_tags'):
                vals=g.get(key) or []
                if isinstance(vals,list):
                    source_tags.extend(str(x) for x in vals if x)
            for reel,field in TAG_FIELDS.items():
                vals=[]
                raw=g.get(field) or []
                if isinstance(raw,str): raw=[raw]
                for x in raw:
                    x=str(x).strip()
                    if x and x not in vals: vals.append(x)
                    if x and x not in flat: flat.append(x)
                if vals: by_reel[reel]=vals
                for x in vals:
                    reverse[x].append({'library':library,'id':gid,'title':title,'reel':reel})
            games_out.append({
                'library':library,'id':gid,'title':title,
                'genre':g.get('genre') or g.get('primary_genre') or g.get('category') or '',
                'source_tags':list(dict.fromkeys(source_tags)),
                'tags_by_reel':by_reel,
                'all_generator_tags':flat,
            })
    Path(args.out).write_text(json.dumps({'version':1,'games':games_out},ensure_ascii=False,indent=2),encoding='utf-8')
    reverse_payload={k:{'game_count':len({(x['library'],x['id'] or x['title']) for x in v}),'games':v} for k,v in sorted(reverse.items())}
    Path(args.reverse_out).write_text(json.dumps({'version':1,'tags':reverse_payload},ensure_ascii=False,indent=2),encoding='utf-8')
    print(f'games: {len(games_out)}')
    print(f'unique generator tags used by games: {len(reverse_payload)}')
    print(args.out); print(args.reverse_out)
if __name__=='__main__': main()
