#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==========================================="
echo "  Build + Install + Run"
echo "==========================================="

cd "$PROJECT_ROOT"

# Step 1: Build
echo ""
echo "Step 1: Building..."
"$PROJECT_ROOT/scripts/build_debug.sh"

# Step 2: Install
echo ""
echo "Step 2: Installing..."
"$PROJECT_ROOT/scripts/install_debug.sh"

# Step 3: Run
echo ""
echo "Step 3: Running..."
"$PROJECT_ROOT/scripts/run_app.sh"

echo ""
echo "==========================================="
echo "✅ All done!"
echo "==========================================="
