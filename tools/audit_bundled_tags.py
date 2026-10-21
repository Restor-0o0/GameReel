"""Audit every bundled tag without fetching data or rewriting library snapshots."""
import json
import re
from collections import Counter
from pathlib import Path

from build_bundled_libraries import DESTINATION, LIST_FIELDS, ROOT, TEXT_FIELDS


def audit():
    enum = (ROOT / 'core/src/commonMain/kotlin/com/restor/gamereel/model/Category.kt').read_text(encoding='utf-8')
    categories = set(re.findall(r'^    ([A-Z_]+),', enum, re.MULTILINE))
    reports, errors = {}, []
    for path in sorted(DESTINATION.glob('*.json')):
        data = json.loads(path.read_text(encoding='utf-8'))
        canonical = 'library' in data
        rows = []
        standalone = data.get('library', {}).get('standaloneTags', [])
        rows.append(('editorial', [(t['category'], t['name']) for t in standalone]))
        for index, game in enumerate(data['games']):
            if canonical:
                tags = [(t['category'], t['name']) for t in game.get('tags', [])]
            else:
                tags = [(category, name) for field, category in LIST_FIELDS.items()
                        for name in game.get(field, [])]
                tags += [(category, game[field]) for field, category in TEXT_FIELDS.items()
                         if game.get(field)]
            rows.append((str(index), tags))
        counts, unique = Counter(), set()
        for owner, tags in rows:
            normalized = [(category, ' '.join(name.lower().split())) for category, name in tags]
            if canonical and len(normalized) != len(set(normalized)):
                errors.append(f'{path.stem}/{owner}: duplicate tag identity')
            for category, name in set(normalized):
                if category not in categories or not name or '\ufffd' in name or \
                        name.startswith('unspecified') or 'infer from' in name:
                    errors.append(f'{path.stem}/{owner}: invalid tag {category}/{name}')
                counts[category] += 1
                unique.add((category, name))
        reports[path.stem] = {
            'games': len(data['games']),
            'tag_instances': sum(counts.values()),
            'unique_category_names': len(unique),
            'category_counts': dict(sorted(counts.items())),
            'missing_categories': sorted(categories - counts.keys()),
        }
        if standalone:
            if any(not tag.get('description', '').strip() for tag in standalone):
                errors.append(f'{path.stem}: missing editorial description')
            if categories - {'OTHER'} - counts.keys():
                errors.append(f'{path.stem}: missing named editorial category')
    report = {'libraries': reports, 'errors': errors}
    destination = ROOT / 'build/preset-audit/tag-audit.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    for name, entry in reports.items():
        print(f'{name}: {entry["tag_instances"]} tags, '
              f'{entry["unique_category_names"]} unique category/name pairs, '
              f'{len(entry["category_counts"])} categories')
    print(f'Errors: {len(errors)}; report: {destination.relative_to(ROOT)}')
    for error in errors:
        print(error)
    return bool(errors)


if __name__ == '__main__':
    raise SystemExit(audit())
