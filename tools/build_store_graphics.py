"""Draw original geometric GameReel store assets. Requires Pillow; no external artwork."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "release/store"
OUT.mkdir(parents=True, exist_ok=True)
BG, GOLD, PANEL = "#101318", "#EBCB7B", "#1D2130"


def mark(image, x, y, size):
    draw = ImageDraw.Draw(image)
    scale = size / 108
    def points(values):
        return [(x + px * scale, y + py * scale) for px, py in values]
    def box(values):
        a, b, c, d = values
        return (x + a * scale, y + b * scale, x + c * scale, y + d * scale)
    draw.rounded_rectangle(box((22, 28, 86, 80)), radius=5 * scale, fill="#C9B5FF")
    draw.rectangle(box((28, 43, 80, 74)), fill=PANEL)
    draw.polygon(points([(37, 31), (31, 35.5), (37, 40), (39, 37.5), (36, 35.5), (39, 33.5)]), fill=PANEL)
    draw.polygon(points([(46, 31), (44, 33.5), (47, 35.5), (44, 37.5), (46, 40), (52, 35.5)]), fill=PANEL)
    draw.rectangle(box((68, 34, 76, 37)), fill=PANEL)

    # Sample the same cubic curves as the Android vector gamepad.
    controller = [(42, 48), (66, 48)]

    def curve(control1, control2, end):
        start = controller[-1]
        for step in range(1, 25):
            t = step / 24
            controller.append(tuple(
                (1 - t) ** 3 * start[i] + 3 * (1 - t) ** 2 * t * control1[i]
                + 3 * (1 - t) * t ** 2 * control2[i] + t ** 3 * end[i]
                for i in range(2)
            ))

    curve((72, 48), (75, 55), (77, 65))
    curve((78, 70), (73, 73), (69, 70))
    controller.extend([(63, 65), (45, 65), (39, 70)])
    curve((35, 73), (30, 70), (31, 65))
    curve((33, 55), (36, 48), (42, 48))
    draw.polygon(points(controller), fill="#B6E76F")
    draw.polygon(points([(40, 53), (44, 53), (44, 57), (48, 57), (48, 61), (44, 61),
                         (44, 65), (40, 65), (40, 61), (36, 61), (36, 57), (40, 57)]), fill=PANEL)
    draw.ellipse(box((62, 52, 68, 58)), fill="#594294")
    draw.ellipse(box((68, 58, 74, 64)), fill="#A32C54")


icon = Image.new("RGB", (1024, 1024), BG)
mark(icon, 0, 0, 1024)
icon.resize((512, 512), Image.Resampling.LANCZOS).save(OUT / "icon-512.png")

for density, pixels in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    destination = ROOT / f"app/androidApp/src/main/res/mipmap-{density}"
    destination.mkdir(parents=True, exist_ok=True)
    fallback = icon.resize((pixels, pixels), Image.Resampling.LANCZOS)
    fallback.save(destination / "ic_launcher.png")
    fallback.save(destination / "ic_launcher_round.png")

feature = Image.new("RGB", (2048, 1000), BG)
mark(feature, 0, 80, 840)
draw = ImageDraw.Draw(feature)
font_dir = Path("C:/Windows/Fonts")
title = ImageFont.truetype(str(font_dir / "segoeuib.ttf"), 140)
subtitle = ImageFont.truetype(str(font_dir / "segoeui.ttf"), 42)
draw.text((820, 330), "GameReel", font=title, fill="#B6E76F")
draw.text((832, 525), "IDEAS FOR YOUR NEXT GAME", font=subtitle, fill="#D9D4E4")
feature.resize((1024, 500), Image.Resampling.LANCZOS).save(OUT / "feature-1024x500.png")
print(OUT)
