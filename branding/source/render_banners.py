"""Screenshots every asset in banners.html with headless Chrome into branding/banners/.

Run from the repository root after make_icon.py: python branding/source/render_banners.py
"""
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
OUT = HERE.parent / "banners"
# Chrome, not Edge: Edge is redirected by another program on this PC.
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
PROFILE = tempfile.mkdtemp(prefix="smwti-chrome-")

# name: (width, height, device scale). Description images are small and rendered at 2x so they
# stay sharp; gallery images are full size.
ASSETS = {
    "banner": (800, 150, 2),
    "themes_strip": (800, 150, 2),
    "social": (1280, 640, 1),
    "features": (1920, 1080, 1),
    "themes": (1920, 1080, 1),
    **{f"h_{k}": (800, 46, 2) for k in ("features", "themes", "players", "authors", "install", "config", "faq")},
}

OUT.mkdir(parents=True, exist_ok=True)
page = (HERE / "banners.html").as_uri()
for name, (w, h, scale) in ASSETS.items():
    target = OUT / f"{name}.png"
    subprocess.run([
        CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--force-device-scale-factor={scale}",
        "--allow-file-access-from-files", f"--user-data-dir={PROFILE}", "--virtual-time-budget=2000",
        f"--window-size={w},{h}", f"--screenshot={target}", f"{page}#{name}",
    ], check=True, capture_output=True, timeout=90)
    print("wrote", target.name)
