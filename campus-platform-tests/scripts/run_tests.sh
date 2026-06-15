#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PROFILE=""
TEST_TARGETS=(tests)

while [[ $# -gt 0 ]]; do
  case "$1" in
    --target)
      export TARGET_BASE_URL="$2"
      shift 2
      ;;
    --api)
      export API_BASE_URL="$2"
      shift 2
      ;;
    --profile)
      PROFILE="$2"
      shift 2
      ;;
    --strict)
      export STRICT_MODE=true
      shift
      ;;
    *)
      export TARGET_BASE_URL="$1"
      shift
      ;;
  esac
done

case "$PROFILE" in
  smoke) TEST_TARGETS=(tests/test_00_smoke.py tests/test_smoke_matrix.py) ;;
  blackbox) TEST_TARGETS=(tests/test_blackbox_deployment.py) ;;
  contract) TEST_TARGETS=(tests/test_contract_quality.py) ;;
  deployment) TEST_TARGETS=(tests/test_http_quality.py) ;;
  negative) TEST_TARGETS=(tests/test_negative_inputs.py) ;;
  frontend) TEST_TARGETS=(tests/test_frontend_routes.py) ;;
  business) TEST_TARGETS=(tests/test_business_modules.py) ;;
  api) TEST_TARGETS=(tests/test_api_public_contract.py tests/test_api_authenticated.py) ;;
  rbac) TEST_TARGETS=(tests/test_rbac_permissions.py) ;;
  security) TEST_TARGETS=(tests/test_security_baseline.py) ;;
  performance) TEST_TARGETS=(tests/test_performance_baseline.py tests/test_resilience_baseline.py) ;;
  full|"") TEST_TARGETS=(tests) ;;
  *) echo "Unknown profile: $PROFILE"; exit 2 ;;
esac

python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt

PROFILE_DIR="${PROFILE:-full}"
if [[ "$PROFILE_DIR" == "" ]]; then
  PROFILE_DIR="full"
fi
REPORT_DIR="reports/$PROFILE_DIR"
mkdir -p "$REPORT_DIR"
REPORT_PATH="$REPORT_DIR/campus-test-report-$(date +%Y%m%d-%H%M%S).html"

ARGS=("${TEST_TARGETS[@]}" -p campus_tests.pytest_dashboard --html="$REPORT_PATH" --self-contained-html)

python -m pytest "${ARGS[@]}"
echo "HTML report: $ROOT/$REPORT_PATH"
echo "Visual dashboard: $ROOT/$REPORT_DIR/latest-dashboard.html"
echo "Live progress page: $ROOT/$REPORT_DIR/latest-live.html"
echo "Reports index: $ROOT/reports/index.html"
