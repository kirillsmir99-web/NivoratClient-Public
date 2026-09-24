"""Generate antialiased GUI textures; Pillow is needed only to regenerate assets."""

from math import cos, radians, sin
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/nivoratclient/textures"
SCALE = 4
WHEEL_SIZE = 320


def box(cx, cy, radius):
    return tuple(round(v * SCALE) for v in (cx - radius, cy - radius, cx + radius, cy + radius))


def wheel_base(count):
    image = Image.new("RGBA", (WHEEL_SIZE * SCALE, WHEEL_SIZE * SCALE))
    draw = ImageDraw.Draw(image)
    draw.ellipse(box(160, 160, 148), fill=(14, 16, 21, 208))
    draw.ellipse(box(160, 160, 56), fill=(0, 0, 0, 0))
    draw.ellipse(box(160, 160, 148), outline=(80, 88, 108, 115), width=2 * SCALE)
    draw.ellipse(box(160, 160, 56), outline=(80, 88, 108, 115), width=2 * SCALE)
    if count > 1:
        for index in range(count):
            angle = radians(-90 + index * 360 / count)
            points = [(round((160 + radius * cos(angle)) * SCALE),
                       round((160 + radius * sin(angle)) * SCALE)) for radius in (57, 147)]
            draw.line(points, fill=(80, 88, 108, 115), width=SCALE)
    draw.ellipse(box(160, 160, 46), fill=(10, 12, 16, 242),
                 outline=(80, 88, 108, 140), width=2 * SCALE)
    return image.resize((WHEEL_SIZE, WHEEL_SIZE), Image.Resampling.LANCZOS)


def wheel_highlight(count, index):
    image = Image.new("RGBA", (WHEEL_SIZE * SCALE, WHEEL_SIZE * SCALE))
    draw = ImageDraw.Draw(image)
    start = -90 + index * 360 / count
    end = -90 + (index + 1) * 360 / count
    if count == 1:
        draw.ellipse(box(160, 160, 147), fill=(18, 172, 244, 68))
    else:
        draw.pieslice(box(160, 160, 147), start, end, fill=(18, 172, 244, 68))
    draw.ellipse(box(160, 160, 57), fill=(0, 0, 0, 0))
    return image.resize((WHEEL_SIZE, WHEEL_SIZE), Image.Resampling.LANCZOS)


def hub_highlight():
    image = Image.new("RGBA", (WHEEL_SIZE * SCALE, WHEEL_SIZE * SCALE))
    draw = ImageDraw.Draw(image)
    draw.ellipse(box(160, 160, 46), fill=(19, 40, 53, 170),
                 outline=(0, 210, 255, 180), width=2 * SCALE)
    return image.resize((WHEEL_SIZE, WHEEL_SIZE), Image.Resampling.LANCZOS)


def badge():
    scale = 4
    image = Image.new("RGBA", (32 * scale, 32 * scale))
    draw = ImageDraw.Draw(image)
    points = [(16, 2), (26, 8), (26, 23), (16, 30), (6, 23), (6, 8)]
    points = [(x * scale, y * scale) for x, y in points]
    glow = Image.new("RGBA", image.size)
    ImageDraw.Draw(glow).polygon(points, fill=(170, 52, 255, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(2.3 * scale))
    image.alpha_composite(glow)
    draw = ImageDraw.Draw(image)
    draw.polygon(points, fill=(43, 17, 88, 245), outline=(226, 177, 255, 235), width=scale)
    draw.polygon([(16*scale, 4*scale), (24*scale, 9*scale), (16*scale, 16*scale), (8*scale, 9*scale)],
                 fill=(170, 91, 248, 225))
    draw.polygon([(8*scale, 11*scale), (16*scale, 18*scale), (24*scale, 11*scale),
                  (24*scale, 22*scale), (16*scale, 28*scale), (8*scale, 22*scale)],
                 fill=(70, 28, 138, 225))
    draw.line([(10*scale, 22*scale), (10*scale, 11*scale), (22*scale, 22*scale), (22*scale, 11*scale)],
              fill=(238, 240, 255, 255), width=2*scale, joint="curve")
    draw.line([(20*scale, 10*scale), (23*scale, 7*scale)], fill=(98, 239, 255, 255), width=2*scale)
    return image.resize((32, 32), Image.Resampling.LANCZOS)


def atlas_icon(kind):
    image = Image.new("RGBA", (24*SCALE, 24*SCALE))
    draw = ImageDraw.Draw(image)
    def line(points, width=2):
        draw.line([(int(x*SCALE), int(y*SCALE)) for x, y in points],
                  fill=(255, 255, 255, 255), width=width*SCALE, joint="curve")
    if kind == "edit":
        line([(5, 18), (16, 7), (19, 10), (8, 21), (4, 21), (5, 18)])
        line([(14, 9), (17, 12)])
    elif kind == "combat":
        line([(5, 5), (18, 18), (20, 21)])
        line([(19, 5), (6, 18), (4, 21)])
        line([(4, 5), (7, 5), (5, 8)])
        line([(20, 5), (17, 5), (19, 8)])
    elif kind == "defense":
        draw.polygon([(12*SCALE, 3*SCALE), (20*SCALE, 6*SCALE), (19*SCALE, 14*SCALE),
                      (16*SCALE, 19*SCALE), (12*SCALE, 22*SCALE), (8*SCALE, 19*SCALE),
                      (5*SCALE, 14*SCALE), (4*SCALE, 6*SCALE)],
                     outline=(255, 255, 255, 255), width=2*SCALE)
        line([(12, 6), (12, 19)], 1)
    elif kind == "utility":
        draw.arc((4*SCALE, 3*SCALE, 16*SCALE, 15*SCALE), 30, 300,
                 fill=(255, 255, 255, 255), width=2*SCALE)
        line([(13, 13), (20, 20)], 3)
        draw.ellipse((18*SCALE, 18*SCALE, 21*SCALE, 21*SCALE), fill=(255, 255, 255, 255))
    elif kind == "chevron_up":
        line([(5, 15), (12, 8), (19, 15)], 2)
    return image.resize((24, 24), Image.Resampling.LANCZOS)


def generate():
    radial = TEXTURES / "gui/radial"
    radial.mkdir(parents=True, exist_ok=True)
    for count in range(1, 9):
        wheel_base(count).save(radial / f"base_{count}.png", optimize=True)
        for index in range(count):
            wheel_highlight(count, index).save(radial / f"highlight_{count}_{index}.png", optimize=True)
    hub_highlight().save(radial / "hub_hover.png", optimize=True)

    font = TEXTURES / "font"
    font.mkdir(parents=True, exist_ok=True)
    badge().save(font / "dev_badge.png", optimize=True)

    atlas_path = TEXTURES / "gui/nivorat_icons_atlas.png"
    original = Image.open(atlas_path).convert("RGBA")
    atlas = Image.new("RGBA", (192, 120))
    atlas.paste(original.crop((0, 0, 192, 96)))
    for kind, x, y in (("edit", 96, 72), ("combat", 120, 72),
                       ("defense", 144, 72), ("utility", 168, 72),
                       ("chevron_up", 0, 96)):
        atlas.paste(atlas_icon(kind), (x, y))
    atlas.save(atlas_path, optimize=True)


if __name__ == "__main__":
    generate()
