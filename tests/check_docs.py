"""Check generated documentation content, including the searchable root snippets."""
import json
from html.parser import HTMLParser
from urllib.parse import unquote, urlsplit
import sys
from pathlib import Path

version = json.loads((Path(__file__).resolve().parents[1] / "mod_info.json").read_text())["version"]
site = Path(sys.argv[1] if len(sys.argv) > 1 else "site")
for relative, marker in {
    "index.html": f"Kayn Combat Diagnostics {version}",
    "build/index.html": "Build from source",
    "verification/index.html": "333",
    "documentation/index.html": "Documentation workflow",
}.items():
    if marker not in (site / relative).read_text(encoding="utf-8"):
        raise SystemExit(f"Missing content in {relative}: {marker}")
entries = json.loads((site / "search/search_index.json").read_text(encoding="utf-8"))["docs"]
searchable = " ".join(entry["text"] for entry in entries)
for marker in (version, "Ctrl+F8", "aiShouldFire", "333"):
    if marker not in searchable:
        raise SystemExit(f"Missing searchable content: {marker}")
print(f"PASS: four documentation pages and searchable content ({len(entries)} entries)")


class Images(HTMLParser):
    def __init__(self):
        super().__init__()
        self.sources = []

    def handle_starttag(self, tag, attrs):
        if tag == "img":
            source = dict(attrs).get("src")
            if source:
                self.sources.append(source)


# README is also included in the MkDocs homepage. Check both Markdown images
# and the HTML images inside its collapsible gallery after rendering.
images = Images()
images.feed((site / "index.html").read_text(encoding="utf-8"))
repo = Path(__file__).resolve().parents[1]
media_prefix = "https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/"
gifs = set()
for source in images.sources:
    if source.startswith(media_prefix):
        target = repo / unquote(source[len(media_prefix):])
    elif not urlsplit(source).scheme and not source.startswith("//"):
        target = site / unquote(urlsplit(source).path.lstrip("/"))
    else:
        continue
    if not target.is_file():
        raise SystemExit(f"Missing homepage image: {source}")
    if target.suffix.lower() == ".gif":
        if target.read_bytes()[:6] not in (b"GIF87a", b"GIF89a"):
            raise SystemExit(f"Invalid GIF header: {target}")
        gifs.add(target.resolve())
expected = {path.resolve() for path in (repo / "docs/media").glob("*.gif")}
if not expected or gifs != expected:
    raise SystemExit(f"Homepage GIF coverage differs: {len(gifs)} shown, {len(expected)} available")
print(f"PASS: homepage image paths and all {len(gifs)} GIFs")
