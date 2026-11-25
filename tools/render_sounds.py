"""Render TypeUI's felt cue (Soft thud) to small, deterministic mono PCM files."""
from math import cos, sin, pi
from pathlib import Path
import struct
import wave

RATE = 44100
COUNT = round(RATE * .05)
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"


def envelope(t, attack, peak):
    floor = .0001
    if t < attack:
        return floor + (peak - floor) * t / attack
    return peak * (floor / peak) ** ((t - attack) / (.05 - attack))


def render():
    omega = 2 * pi * 500 / RATE
    alpha = sin(omega) / (2 * 10 ** (.7 / 20))
    b = (1 - cos(omega)) / (2 * (1 + alpha))
    a1, a2 = -2 * cos(omega) / (1 + alpha), (1 - alpha) / (1 + alpha)
    x1 = x2 = y1 = y2 = 0
    seed = 1337
    samples = []
    for i in range(COUNT):
        t = i / RATE
        seed = int(float(seed) * 1103515245 + 12345) & 0x7fffffff
        x = (seed / 0x40000000 - 1) * (1 - i / COUNT)
        y = b * (x + 2 * x1 + x2) - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, x, y1, y
        samples.append(y * envelope(t, .008, .0125) +
                       sin(2 * pi * 160 * t) * envelope(t, .002, .025))
    return samples + [0.0]


samples = render()
level = .42 * 32767 / max(map(abs, samples))
for name, ratio in (("soft_thud", 1), ("backspace", 2 ** (-2 / 12))):
    pcm = bytearray()
    for i in range(int(COUNT / ratio)):
        pos = i * ratio
        j = int(pos)
        value = samples[j] + (samples[j + 1] - samples[j]) * (pos - j)
        pcm.extend(struct.pack("<h", round(value * level)))
    with wave.open(str(OUTPUT / (name + ".wav")), "wb") as out:
        out.setparams((1, 2, RATE, 0, "NONE", ""))
        out.writeframes(pcm)

