"""Check generated documentation content, including the searchable root snippets."""
import json
import sys
from pathlib import Path

version = json.loads((Path(__file__).resolve().parents[1] / "mod_info.json").read_text())["version"]
site = Path(sys.argv[1] if len(sys.argv) > 1 else "site")
for relative, marker in {
    "index.html": f"Kayn Combat Diagnostics {version}",
    "build/index.html": "Build from source",
    "verification/index.html": "222",
    "documentation/index.html": "Documentation workflow",
}.items():
    if marker not in (site / relative).read_text(encoding="utf-8"):
        raise SystemExit(f"Missing content in {relative}: {marker}")
entries = json.loads((site / "search/search_index.json").read_text(encoding="utf-8"))["docs"]
searchable = " ".join(entry["text"] for entry in entries)
for marker in (version, "Ctrl+F8", "aiShouldFire", "222"):
    if marker not in searchable:
        raise SystemExit(f"Missing searchable content: {marker}")
print(f"PASS: four documentation pages and searchable content ({len(entries)} entries)")
