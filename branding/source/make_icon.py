"""Draws the SMWTI icon as 32x32 pixel art and writes it at several sizes.

Run from the repository root: python branding/source/make_icon.py
Writes branding/icon/icon_<size>.png, branding/icon/icon.svg and smwti_logo.png (for the jar).
"""
import math
import random
from pathlib import Path

from PIL import Image

N = 32
OUT = Path(__file__).resolve().parent.parent / "icon"

# Palette
BG = (24, 22, 38)
BG_EDGE = (46, 42, 70)
BG_TOP = (58, 54, 88)
GOLD = (242, 179, 61)
GOLD_HI = (255, 226, 140)
GOLD_LO = (178, 116, 32)
GRASS = [(109, 187, 69), (94, 168, 56), (124, 201, 82)]
GRASS_DARK = (70, 132, 42)
DIRT = [(139, 94, 52), (122, 82, 45), (150, 104, 60)]
DIRT_DARK = (96, 64, 36)
GLARE = (235, 248, 255)
HANDLE = (92, 60, 38)
HANDLE_HI = (138, 95, 60)
HANDLE_LO = (58, 38, 26)
BUBBLE = (250, 250, 250)
BUBBLE_LO = (196, 200, 214)
INK = (24, 22, 38)

random.seed(7)
px = [[None] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


# Background tile with stepped rounded corners.
R = 4
for y in range(N):
    for x in range(N):
        cx = min(max(x, R - 0.5), N - R - 0.5)
        cy = min(max(y, R - 0.5), N - R - 0.5)
        if math.hypot(x - cx, y - cy) <= R:
            px[y][x] = BG
for y in range(N):
    for x in range(N):
        if px[y][x] is None:
            continue
        edge = any(
            not (0 <= x + dx < N and 0 <= y + dy < N) or px[y + dy][x + dx] is None
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
        )
        if edge:
            px[y][x] = BG_TOP if y < N // 2 else BG_EDGE

# Lens: glass with an isometric grass block inside.
LX, LY = 12.5, 12.5
R_IN, R_OUT = 7.7, 10.3
GLASS = [(150, 214, 240), (128, 198, 232), (108, 180, 222), (92, 160, 206)]


def in_poly(x, y, pts):
    inside = False
    for i in range(len(pts)):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % len(pts)]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


CX0, CY0 = 6.5, 6.0  # cube's top left in pixels
T, L, Rr, C = (6, 0), (0, 3), (12, 3), (6, 6)
BL, BR, B = (0, 10), (12, 10), (6, 13)


def off(pts):
    return [(a + CX0, b + CY0) for a, b in pts]


TOP, LEFT, RIGHT = off([T, Rr, C, L]), off([L, C, B, BL]), off([C, Rr, BR, B])

for y in range(N):
    for x in range(N):
        d = math.hypot(x + 0.5 - LX, y + 0.5 - LY)
        fx, fy = x + 0.5, y + 0.5
        if d <= R_IN:
            band = min(3, max(0, int((fy - (LY - R_IN)) / (2 * R_IN) * 4)))
            c = GLASS[band]
            if in_poly(fx, fy, TOP):
                c = random.choice(GRASS)
            elif in_poly(fx, fy, LEFT):
                # Depth below the top edge of the left face.
                depth = fy - (CY0 + 3 + (fx - CX0) * 0.5)
                c = random.choice(GRASS[:2]) if depth < 2 else random.choice(DIRT)
                if depth >= 2 and random.random() < 0.15:
                    c = DIRT_DARK
            elif in_poly(fx, fy, RIGHT):
                depth = fy - (CY0 + 6 - (fx - CX0 - 6) * 0.5)
                c = GRASS_DARK if depth < 2 else random.choice([DIRT_DARK, DIRT[1]])
            put(x, y, c)
        elif d <= R_OUT:
            nx, ny = (fx - LX) / d, (fy - LY) / d
            light = -(nx + ny) * 0.7071
            if light > 0.5:
                c = GOLD_HI
            elif light < -0.4:
                c = GOLD_LO
            else:
                c = GOLD
            put(x, y, c)

# Glare on the glass, upper left.
for x, y in ((6, 10), (6, 9), (7, 8), (7, 7), (8, 6), (9, 6)):
    put(x, y, GLARE)

# Handle down to the lower right.
HX0, HY0, HX1, HY1 = 19.6, 19.6, 28.2, 28.2
for y in range(N):
    for x in range(N):
        fx, fy = x + 0.5, y + 0.5
        t = ((fx - HX0) * (HX1 - HX0) + (fy - HY0) * (HY1 - HY0)) / ((HX1 - HX0) ** 2 + (HY1 - HY0) ** 2)
        if not 0 <= t <= 1:
            continue
        px_, py_ = HX0 + t * (HX1 - HX0), HY0 + t * (HY1 - HY0)
        side = ((fx - px_) - (fy - py_)) * 0.7071  # + is upper right, - is lower left
        dist = math.hypot(fx - px_, fy - py_)
        if dist <= 1.75:
            if t < 0.18:
                c = GOLD_LO if side < 0 else GOLD
            elif side > 0.6:
                c = HANDLE_HI
            elif side < -0.6:
                c = HANDLE_LO
            else:
                c = HANDLE
            put(x, y, c)

# Question bubble at the top right.
bubble = [
    ".XXXXXXX.",
    "XXXXXXXXX",
    "XXXXXXXXX",
    "XXXXXXXXX",
    "XXXXXXXXX",
    "XXXXXXXXX",
    "XXXXXXXXX",
    "XXXXXXXXX",
    ".XXXXXXX.",
    ".XX......",
    ".X.......",
]
qmark = [
    ".........",
    "...###...",
    "..#...#..",
    "......#..",
    ".....#...",
    "....#....",
    ".........",
    "....#....",
    ".........",
]
BX, BY = 21, 2
for j, row in enumerate(bubble):
    for i, ch in enumerate(row):
        if ch == "X":
            put(BX + i, BY + j, BUBBLE_LO if j >= 8 or i == 8 else BUBBLE)
for j, row in enumerate(qmark):
    for i, ch in enumerate(row):
        if ch == "#":
            put(BX + i, BY + j, INK)

# Write the base image.
img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
for y in range(N):
    for x in range(N):
        if px[y][x] is not None:
            img.putpixel((x, y), px[y][x] + (255,))

OUT.mkdir(parents=True, exist_ok=True)
for size in (32, 64, 128, 256, 400, 512, 1024):
    img.resize((size, size), Image.NEAREST).save(OUT / f"icon_{size}.png", optimize=True)
img.resize((512, 512), Image.NEAREST).save(OUT / "smwti_logo.png", optimize=True)

# SVG version: one rect per pixel, crisp at any size.
rects = []
for y in range(N):
    for x in range(N):
        c = px[y][x]
        if c is not None:
            rects.append(f'<rect x="{x}" y="{y}" width="1.02" height="1.02" fill="#{c[0]:02x}{c[1]:02x}{c[2]:02x}"/>')
(OUT / "icon.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {N} {N}" shape-rendering="crispEdges">'
    + "".join(rects) + "</svg>\n",
    encoding="utf-8",
)
print("wrote", OUT)
