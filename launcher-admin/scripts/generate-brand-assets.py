from pathlib import Path
from PIL import Image, ImageDraw


def render(size: int) -> Image.Image:
    scale = size / 256
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    def box(values):
        return tuple(round(value * scale) for value in values)

    draw.ellipse(box((12, 12, 244, 244)), fill="#14251f", outline="#79c56a", width=max(1, round(8 * scale)))
    draw.ellipse(box((46, 62, 190, 206)), fill="#f1b54d", outline="#fff0b3", width=max(1, round(7 * scale)))
    draw.polygon([box((155, 72)), box((178, 44)), box((216, 47)), box((199, 82)), box((161, 95))], fill="#67bd59")
    draw.line([box((161, 93)), box((207, 53))], fill="#e9ffd2", width=max(1, round(5 * scale)))

    dark = "#643f19"
    stroke = max(2, round(9 * scale))
    draw.line([box((82, 108)), box((96, 163)), box((118, 122)), box((140, 163)), box((154, 108))], fill=dark, width=stroke, joint="curve")
    draw.line([box((72, 130)), box((164, 130))], fill=dark, width=max(1, round(6 * scale)))
    draw.line([box((75, 146)), box((161, 146))], fill=dark, width=max(1, round(6 * scale)))
    return image


workspace = Path(__file__).resolve().parents[2]
launcher = workspace / "launcher"
images = launcher / "app" / "assets" / "images"
build = launcher / "build"

master = render(512)
master.save(images / "DonationMark.png")
master.save(build / "icon.png")
master.save(images / "DonationMark.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
print("브랜드 PNG/ICO 생성 완료")
