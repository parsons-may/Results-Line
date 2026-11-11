#!/usr/bin/env bash
# Runs the evaluation and writes a results file under docs/results/.
#   ./run-eval.sh        real Claude. Needs ANTHROPIC_API_KEY in .env. Makes paid API calls.
#   ./run-eval.sh demo   the demo keyword script (free). Results are labeled DEMO and git-ignored.
set -euo pipefail
cd "$(dirname "$0")"

if [ "${1:-}" = "demo" ]; then
  DEMO=true
else
  DEMO=false
  if [ -f .env ]; then
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
  fi
  if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
    echo "ANTHROPIC_API_KEY is not set. Put it in .env, or run './run-eval.sh demo' for the free demo client." >&2
    exit 1
  fi
fi

export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
cd backend
mvn -q spring-boot:run -Dspring-boot.run.arguments="--results-line.eval.run=true --results-line.claude.demo-mode=${DEMO} --spring.main.web-application-type=none --spring.main.banner-mode=off --logging.level.root=WARN"
