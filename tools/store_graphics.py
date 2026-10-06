#!/usr/bin/env python3
"""Render the Play Store icon (512x512) and feature graphic (1024x500) for GridPix.

Usage: python tools/store_graphics.py <output-dir>
Needs Pillow (pip install pillow). Uses the Starter pack's pixel heart so the store art
matches the in-app adaptive icon (res/drawable/ic_launcher_foreground.xml).
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HEART = [
    "..........",
    "..##..##..",
    ".########.",
    "##########",
    "##########",
    ".########.",
    "..######..",
    "...####...",
    "....##....",
    "..........",
]
BG = (30, 58, 95)        # #1E3A5F, same as ic_launcher_background
FG = (255, 255, 255)
ACCENT = (247, 189, 72)  # tertiary from the dark palette, used for a few picture pixels


def draw_heart(img, x, y, px, color=FG, grid=None):
    d = ImageDraw.Draw(img)
    for r, row in enumerate(HEART):
        for c, ch in enumerate(row):
            if ch == "#":
                d.rectangle([x + c * px, y + r * px, x + (c + 1) * px - 1, y + (r + 1) * px - 1], fill=color)
    if grid:
        for i in range(11):
            d.line([x + i * px, y, x + i * px, y + 10 * px], fill=grid, width=1)
            d.line([x, y + i * px, x + 10 * px, y + i * px], fill=grid, width=1)


def font(size, bold=True):
    for name in (["segoeuib.ttf", "arialbd.ttf"] if bold else ["segoeui.ttf", "arial.ttf"]):
        path = os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts", name)
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def icon(out):
    img = Image.new("RGB", (512, 512), BG)
    draw_heart(img, 64, 64, 38.4 and 38)  # 10 px * 38 = 380, centred with 66 margin
    # Recentre exactly: heart block is 380 wide/high
    img = Image.new("RGB", (512, 512), BG)
    draw_heart(img, (512 - 380) // 2, (512 - 380) // 2, 38)
    img.save(os.path.join(out, "play_icon_512.png"))


def feature(out):
    img = Image.new("RGB", (1024, 500), BG)
    d = ImageDraw.Draw(img)
    # Heart with faint grid on the left
    draw_heart(img, 70, 70, 36, grid=(255, 255, 255, 40) and (60, 88, 125))
    # Title and tagline
    d.text((470, 150), "GridPix", font=font(96), fill=FG)
    d.text((474, 270), "Nonogram puzzles", font=font(44, bold=False), fill=(214, 227, 255))
    d.text((474, 330), "Offline  •  No ads  •  Daily puzzle", font=font(30, bold=False), fill=(169, 199, 255))
    img.save(os.path.join(out, "feature_graphic_1024x500.png"))


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    icon(out)
    feature(out)
    print("wrote play_icon_512.png and feature_graphic_1024x500.png to", out)


if __name__ == "__main__":
    main()
