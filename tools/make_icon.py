"""Рисует иконку мода 128x128: тёмный квадрат в стиле Steam с голубым кубом."""
import sys
from PIL import Image, ImageDraw

S = 128
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rounded_rectangle([0, 0, S - 1, S - 1], radius=24, fill=(23, 26, 33, 255), outline=(42, 71, 94, 255), width=4)

# изометрический куб
cx, cy, r = 64, 66, 36
top = [(cx, cy - r), (cx + r, cy - r // 2), (cx, cy), (cx - r, cy - r // 2)]
left = [(cx - r, cy - r // 2), (cx, cy), (cx, cy + r), (cx - r, cy + r // 2)]
right = [(cx + r, cy - r // 2), (cx, cy), (cx, cy + r), (cx + r, cy + r // 2)]
d.polygon(top, fill=(102, 192, 244, 255))
d.polygon(left, fill=(61, 108, 141, 255))
d.polygon(right, fill=(42, 71, 94, 255))
for face in (top, left, right):
    d.line(face + [face[0]], fill=(200, 235, 255, 255), width=2)

img.save(sys.argv[1])
