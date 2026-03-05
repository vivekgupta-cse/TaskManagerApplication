#!/usr/bin/env bash
# Minimal wrapper: run docker compose with multiple compose files for the app stack.
# Usage examples:
#   ./docker_scripts/run_app.sh up -d    -> runs: docker compose -f ./docker_scripts/docker-compose-postgres.yml -f ./docker_scripts/docker-compose-postgres-test.yml -f ./docker_scripts/docker-compose-app.yml up -d
#   ./docker_scripts/run_app.sh down     -> runs: docker compose -f ./docker_scripts/docker-compose-postgres.yml -f ./docker_scripts/docker-compose-postgres-test.yml -f ./docker_scripts/docker-compose-app.yml down

set -euo pipefail

COMPOSE_FILES=(
  "./docker_scripts/docker-compose-postgres.yml"
  "./docker_scripts/docker-compose-postgres-test.yml"
  "./docker_scripts/docker-compose-app.yml"
)

# Build -f args
COMPOSE_ARGS=()
for f in "${COMPOSE_FILES[@]}"; do
  COMPOSE_ARGS+=( -f "$f" )
done

# If no positional args provided, default to up -d
if [ "$#" -eq 0 ]; then
  set -- up -d
fi

# Execute docker compose with all files
exec docker compose "${COMPOSE_ARGS[@]}" "$@"
