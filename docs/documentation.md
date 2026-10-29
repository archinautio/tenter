# Maintaining the documentation

## Where each explanation belongs

Keep each explanation in one place and link to it elsewhere instead of copying prose:

- How to use the library belongs in the relevant public guide under `docs/site/`.
- What a maintainer must preserve belongs in [architecture](architecture.md).
- `CLAUDE.md` holds only what every working session needs (commands and working
  conventions) and links to these pages for the rest. Do not repeat it here.
- Source comments state a declaration's contract briefly, or explain what the code cannot
  say for itself. Leave tutorials and examples to the guides.

Write every page in `docs/` for people to read: prefer a list or a table over a long paragraph
of clauses joined by semicolons.

The public site follows `main` and is built from `docs/site/` using Zensical.
Only the contents of generated `build/site/` are uploaded to Pages.

## Preview locally

From the repository root, using Python 3.14:

```sh
python3 -m venv .venv-docs
source .venv-docs/bin/activate
python -m pip install -r requirements-docs.txt
zensical serve
```

On Windows, use `py -m venv .venv-docs` and `.venv-docs\Scripts\Activate.ps1`
in PowerShell. Open the local URL printed by Zensical. Stop the preview with Ctrl-C.

## Validate changes

```sh
zensical build --clean --strict
```

- Strict validation checks internal links and anchors, and the snippets extension fails on
  missing source paths or named regions.
- Prefer named regions in compiled example sources over duplicated Kotlin.
- The greeting includes everything after its package line, so preserve its complete imports.
- When changing example sources, run the [project checks](../CLAUDE.md#commands) and the
  [packaged example check](site/examples.md#noninteractive-check).

When changing installation requirements, update the getting-started page from the
version catalog and Gradle wrapper. Verify Maven Local installation in a separate
consumer project.

Do not commit `build/site/`, `.cache/`, or the virtual environment.

## Publishing

Before publishing, check:

- Desktop and narrow layouts, keyboard navigation, search, copy buttons, and a directly opened
  nested page.
- The `/tenter/` project prefix for assets and links.

- Pull requests validate without deploying, and manual runs deploy only from `main`.
- A failed build leaves the previously deployed site in place.
- To recover from a bad content update, revert it through the normal review process; the next
  successful `main` build republishes the corrected site.
- No custom domain, deployment token, generated branch, or Sites hosting configuration is
  required.
