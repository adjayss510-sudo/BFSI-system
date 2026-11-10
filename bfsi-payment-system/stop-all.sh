#!/usr/bin/env bash
for f in logs/*.pid; do kill "$(cat "$f")" 2>/dev/null && echo "stopped $f"; rm -f "$f"; done
