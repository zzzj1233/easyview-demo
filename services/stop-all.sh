#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
for app in gateway order inventory; do
  bash dev.sh $app stop
done
echo "all services stopped"
