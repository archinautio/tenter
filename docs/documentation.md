# Maintaining the documentation

The public site follows `main` and is built from `docs/site/` using Zensical.
Architecture and contributor documentation stay outside that directory. Only the
contents of generated `build/site/` are uploaded to Pages.

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

Strict validation checks internal links and anchors. The snippets extension fails
on missing source paths or named regions. Prefer named regions in compiled example
sources over duplicated Kotlin. The greeting includes everything after its package
line; preserve its complete imports. Run the [project checks](../CLAUDE.md#commands)
and the [packaged example check](site/examples.md#noninteractive-check) when changing example sources.

When changing installation requirements, update the getting-started page from the
version catalog and Gradle wrapper. Verify Maven Local installation in a separate
consumer project. Keep each explanation in one place: caller-facing behavior belongs
in the relevant public guide; implementation invariants belong in [architecture](architecture.md).
Use links elsewhere, including the README and agent instructions, instead of copying prose.

Before publishing, inspect desktop and narrow layouts, keyboard navigation, search,
copy buttons, and a directly opened nested page. Verify the `/tenter/` project prefix
for assets and links. Do not commit `build/site/`, `.cache/`, or the virtual environment.

Pull requests validate without deploying. Manual runs deploy only from `main`.
Failed builds leave the previously deployed site in place. To recover from a bad
content update, revert that update through the normal review process; the next
successful `main` build republishes the corrected site. No custom domain, deployment
token, generated branch, or Sites hosting configuration is required.
