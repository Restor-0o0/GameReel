"""Read-only inspection of a review APK/AAB. This does not certify legal or Play compliance."""
import argparse
import json
from pathlib import Path
import struct
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("artifact", type=Path)
args = parser.parse_args()
errors = []
native = []
with zipfile.ZipFile(args.artifact) as archive:
    names = archive.namelist()
    image_extensions = (".webp", ".png", ".jpg", ".jpeg", ".avif")
    art = [
        name for name in names
        if "/files/art/" in f"/{name}"
        or ("/files/" in f"/{name}" and "/files/tag_icons/" not in f"/{name}"
            and name.lower().endswith(image_extensions))
    ]
    if art:
        errors.append(f"Contains {len(art)} store artwork files")
    libraries = [name for name in names if "/files/libraries/" in name and name.endswith(".json")]
    allowed = {"curated.json", "itch_selected.json", "steam_selected.json"}
    if len(libraries) != len(allowed) or {Path(name).name for name in libraries} != allowed:
        errors.append("Public artifact must contain exactly the three selected libraries")
    for name in libraries:
        pack = json.loads(archive.read(name))
        with_images = sum(
            any(game.get(key) for key in (
                "image", "image_url", "header_image", "header_image_url",
                "local_image", "cover_url", "icon",
            ))
            for game in pack.get("games", [])
        )
        if with_images:
            errors.append(f"Game artwork metadata remains in {name}: {with_images} games")
    policies = [name for name in names if name.endswith("/files/art-policy.txt")]
    if len(policies) != 1 or archive.read(policies[0]).decode().strip() != "no-store-art":
        errors.append("Missing or incorrect no-store-art policy")
    for name in names:
        if not name.endswith(".so"):
            continue
        data = archive.read(name)
        if data[:4] != b"\x7fELF":
            errors.append(f"Not ELF: {name}")
            continue
        wide = data[4] == 2
        endian = "<" if data[5] == 1 else ">"
        phoff = struct.unpack_from(endian + ("Q" if wide else "I"), data, 32 if wide else 28)[0]
        size, count = struct.unpack_from(endian + "HH", data, 54 if wide else 42)
        alignments = []
        for index in range(count):
            offset = phoff + index * size
            if struct.unpack_from(endian + "I", data, offset)[0] == 1:
                alignments.append(struct.unpack_from(endian + ("Q" if wide else "I"), data, offset + (48 if wide else 28))[0])
        native.append({"file": name, "load_alignment": alignments})
        if not alignments or min(alignments) < 16384:
            errors.append(f"ELF load alignment below 16 KB: {name}")
print(json.dumps({"artifact": str(args.artifact), "artwork_files": len(art), "libraries": libraries, "native": native, "errors": errors,
                  "not_checked": ["rights to datasets", "signature", "Play Console declarations", "runtime on 16 KB device", "APK ZIP alignment"]}, indent=2))
raise SystemExit(bool(errors))
