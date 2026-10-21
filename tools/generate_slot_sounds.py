"""Generate original, deterministic mechanical clicks; no external audio assets."""
import io
import math
from pathlib import Path
import random
import struct
import wave

ROOT = Path(__file__).resolve().parents[1]
DESTINATIONS = [
    ROOT / "app/shared/src/androidMain/res/raw",
    ROOT / "app/shared/src/commonMain/composeResources/files/audio",
]

for name, duration, frequency in [("slot_tick", 0.022, 1900), ("slot_stop", 0.055, 740)]:
    rng = random.Random(42)
    samples = []
    for i in range(int(44100 * duration)):
        t = i / 44100
        envelope = min(t / 0.0008, 1) * math.exp(-t / (duration / 6))
        value = (0.65 * math.sin(2 * math.pi * frequency * t) + 0.35 * rng.uniform(-1, 1))
        samples.append(struct.pack("<h", int(value * envelope * 22000)))
    data = io.BytesIO()
    with wave.open(data, "wb") as wav:
        wav.setparams((1, 2, 44100, 0, "NONE", "not compressed"))
        wav.writeframes(b"".join(samples))
    for destination in DESTINATIONS:
        destination.mkdir(parents=True, exist_ok=True)
        (destination / f"{name}.wav").write_bytes(data.getvalue())
