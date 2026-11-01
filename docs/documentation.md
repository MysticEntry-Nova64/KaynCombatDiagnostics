# Documentation workflow

Run these PowerShell commands from the repository root. Python 3.11 or newer is required.
The documentation environment is independent of the Java mod toolchain.

```powershell
python -m venv .venv-docs
.\.venv-docs\Scripts\python.exe -m pip install -r requirements-docs.txt
.\.venv-docs\Scripts\python.exe -m mkdocs build --strict
.\.venv-docs\Scripts\python.exe -m mkdocs serve --dev-addr 127.0.0.1:8000
```

Open <http://127.0.0.1:8000> while the preview runs; Ctrl+C stops it.
Generated output goes into `site/`. The environment and generated site are ignored by Git.
No publication or GitHub Pages deployment is configured.

## Editing

Edit `README.md`, `BUILD.md`, and `TEST-RESULTS.md` at the repository root.
The corresponding documentation pages include those files using PyMdown Snippets;
they do not maintain duplicate copies. Edit this page for documentation tooling guidance.
Run the strict build before sharing changes. The site reflects the local checkout,
including uncommitted changes; historical test results describe the revisions identified there.

## DeepWiki MCP in Codex

The supported user-level connection is:

```powershell
codex mcp add deepwiki --url https://mcp.deepwiki.com/mcp
codex mcp get deepwiki --json
```

It is already configured on the setup machine. Restart Codex or start a fresh session
if the tools are not available in an existing chat. The hosted public service requires
no local server package or API key. It reads indexed public GitHub content, not this
working directory or uncommitted changes.

Use `read_wiki_structure` with `repoName` set to
`MysticEntry-Nova64/KaynCombatDiagnostics`. Discover the current question tool name
from the server tool list; the setup check advertised `ask_wiki_question`.
Treat generated answers as navigation aids and verify claims against source code.

On 2026-11-01 a subsequent MCP read successfully returned documentation topics for
`MysticEntry-Nova64/KaynCombatDiagnostics`, confirming indexed availability at that
check. Indexing can lag behind newly pushed commits; verify current behavior against
the repository source rather than assuming generated documentation is up to date.

References: [Material installation](https://squidfunk.github.io/mkdocs-material/getting-started/),
[DeepWiki MCP](https://docs.devin.ai/work-with-devin/deepwiki-mcp),
and [Codex MCP configuration](https://developers.openai.com/learn/docs-mcp).
