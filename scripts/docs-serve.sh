#!/bin/bash
# Serve the Zensical documentation site locally with live reload.
# Creates .venv-docs and installs requirements-docs.txt on first run.
set -euo pipefail

cd "$(dirname "$0")/.."

VENV=.venv-docs
STAMP="$VENV/.requirements-stamp"

if [ ! -d "$VENV" ]; then
  python3 -m venv "$VENV"
fi

if [ ! -f "$STAMP" ] || [ requirements-docs.txt -nt "$STAMP" ]; then
  "$VENV/bin/python" -m pip install -r requirements-docs.txt
  touch "$STAMP"
fi

exec "$VENV/bin/zensical" serve "$@"
