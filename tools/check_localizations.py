"""Check that all UI locales have the same resource keys and format arguments."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1] / "app/shared/src/commonMain/composeResources"
placeholder = re.compile(r"%(?:\d+\$)?[ds]")


def resources(path):
    return {item.attrib["name"]: "".join(item.itertext()) for item in ET.parse(path).getroot()}


base = resources(root / "values/strings.xml")
errors = []
for directory in sorted(root.glob("values-*")):
    translated = resources(directory / "strings.xml")
    missing = base.keys() - translated.keys()
    extra = translated.keys() - base.keys()
    if missing:
        errors.append(f"{directory.name}: missing {sorted(missing)}")
    if extra:
        errors.append(f"{directory.name}: extra {sorted(extra)}")
    for key in base.keys() & translated.keys():
        if set(placeholder.findall(base[key])) != set(placeholder.findall(translated[key])):
            errors.append(f"{directory.name}: format arguments differ in {key}")

if errors:
    print("\n".join(errors))
    sys.exit(1)
print(f"Localization keys and format arguments match: {len(base)} resources, 5 translations")
