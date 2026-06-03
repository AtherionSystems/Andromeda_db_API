#!/bin/bash
# =============================================================================
# blue-green-deploy.sh
# Deploys a new image to the INACTIVE slot and validates it with a smoke test.
# Does NOT swap production traffic — that is handled by blue-green-swap.sh
# after a Manual Approval stage in the OCI DevOps pipeline.
#
# Usage:
#   bash scripts/blue-green-deploy.sh <target-slot> <image-tag>
#
# Examples:
#   bash scripts/blue-green-deploy.sh green 20260602-abc1234
#   bash scripts/blue-green-deploy.sh blue  20260602-def5678
#
# The OCI DevOps pipeline passes TARGET_SLOT and IMAGE_TAG as pipeline
# parameters. When running manually from Cloud Shell, pass them as arguments.
# =============================================================================
set -euo pipefail

# --- helpers -----------------------------------------------------------------
ok()   { echo "  [OK] $*"; }
fail() { echo "  [FAIL] $*" >&2; exit 1; }
info() { echo ""; echo "--- $* ---"; }

# --- args --------------------------------------------------------------------
TARGET_SLOT="${1:-}"
IMAGE_TAG="${2:-}"

if [[ -z "$TARGET_SLOT" || -z "$IMAGE_TAG" ]]; then
  fail "Usage: $0 <target-slot> <image-tag>   (slot: blue | green)"
fi

if [[ "$TARGET_SLOT" != "blue" && "$TARGET_SLOT" != "green" ]]; then
  fail "Invalid slot '$TARGET_SLOT'. Must be 'blue' or 'green'."
fi

# --- config ------------------------------------------------------------------
NAMESPACE="andromeda"
REGISTRY="mx-queretaro-1.ocir.io/axieboiigznv"
IMAGE="$REGISTRY/andromeda-backend:$IMAGE_TAG"
DEPLOYMENT="andromeda-$TARGET_SLOT"
INTERNAL_SVC="andromeda-$TARGET_SLOT-internal"
ROLLOUT_TIMEOUT="180s"
SMOKE_TIMEOUT=10   # seconds per curl attempt
SMOKE_RETRIES=6    # retry up to 6 times (~1 min total)

# --- guard: don't deploy to the active slot ----------------------------------
info "Pre-flight check"

CURRENT_SLOT=$(kubectl get configmap bluegreen-state \
  -n "$NAMESPACE" \
  -o jsonpath='{.data.active-slot}' 2>/dev/null || echo "blue")

ok "Current active slot: $CURRENT_SLOT"

if [[ "$TARGET_SLOT" == "$CURRENT_SLOT" ]]; then
  fail "Slot '$TARGET_SLOT' is currently ACTIVE. Deploy to the opposite slot ('$([ "$TARGET_SLOT" = "blue" ] && echo green || echo blue)')."
fi

ok "Target slot '$TARGET_SLOT' is inactive — safe to deploy."

# --- step 1: update image ----------------------------------------------------
info "Step 1: Updating image on $DEPLOYMENT"

kubectl set image deployment/"$DEPLOYMENT" \
  andromeda="$IMAGE" \
  -n "$NAMESPACE"

ok "Image updated to: $IMAGE"

# --- step 2: wait for rollout ------------------------------------------------
info "Step 2: Waiting for rollout (timeout: $ROLLOUT_TIMEOUT)"

kubectl rollout status deployment/"$DEPLOYMENT" \
  -n "$NAMESPACE" \
  --timeout="$ROLLOUT_TIMEOUT"

ok "Rollout complete."

# --- step 3: smoke test via internal ClusterIP -------------------------------
info "Step 3: Smoke test against $INTERNAL_SVC (not public)"

CLUSTER_IP=$(kubectl get svc "$INTERNAL_SVC" \
  -n "$NAMESPACE" \
  -o jsonpath='{.spec.clusterIP}')

if [[ -z "$CLUSTER_IP" ]]; then
  fail "Could not resolve ClusterIP for $INTERNAL_SVC. Is the service applied?"
fi

ok "ClusterIP: $CLUSTER_IP"

HTTP_STATUS=""
for i in $(seq 1 $SMOKE_RETRIES); do
  echo "  Attempt $i/$SMOKE_RETRIES — GET http://$CLUSTER_IP:8080/actuator/health"
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://$CLUSTER_IP:8080/actuator/health" \
    --max-time "$SMOKE_TIMEOUT" 2>/dev/null || echo "000")

  if [[ "$HTTP_STATUS" == "200" ]]; then
    break
  fi

  echo "  Response: $HTTP_STATUS — waiting 10s before retry..."
  sleep 10
done

if [[ "$HTTP_STATUS" != "200" ]]; then
  fail "Smoke test failed after $SMOKE_RETRIES attempts (last HTTP status: $HTTP_STATUS). Pipeline will NOT proceed to swap."
fi

ok "Smoke test passed (HTTP 200)."

# --- done --------------------------------------------------------------------
echo ""
echo "================================================================"
echo "  Slot '$TARGET_SLOT' is healthy and ready for traffic."
echo "  Image: $IMAGE"
echo "  Next step: Manual Approval in OCI DevOps, then blue-green-swap.sh"
echo "================================================================"