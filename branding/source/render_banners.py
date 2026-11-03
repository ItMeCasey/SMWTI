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

ASSETS = {
    "banner": (1600, 400),
    "social": (1280, 640),
    "features": (1920, 1080),
    "themes": (1920, 1080),
    "h_themes": (1200, 110),
    "h_features": (1200, 110),
    "h_players": (1200, 110),
    "h_authors": (1200, 110),
    "h_install": (1200, 110),
    "h_config": (1200, 110),
    "h_faq": (1200, 110),
}

OUT.mkdir(parents=True, exist_ok=True)
page = (HERE / "banners.html").as_uri()
for name, (w, h) in ASSETS.items():
    target = OUT / f"{name}.png"
    subprocess.run([
        CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
        "--allow-file-access-from-files", f"--user-data-dir={PROFILE}", "--virtual-time-budget=2000",
        f"--window-size={w},{h}", f"--screenshot={target}", f"{page}#{name}",
    ], check=True, capture_output=True)
    print("wrote", target.name)
