#!/bin/bash
# =============================================================================
# blue-green-swap.sh
# Switches production traffic from the current active slot to TARGET_SLOT
# by patching the andromeda-service selector, then updates bluegreen-state.
#
# This script runs AFTER Manual Approval in the OCI DevOps pipeline.
# It should never run unless blue-green-deploy.sh completed successfully.
#
# Usage:
#   bash scripts/blue-green-swap.sh <target-slot>
#
# Examples:
#   bash scripts/blue-green-swap.sh green
#   bash scripts/blue-green-swap.sh blue   # rollback
# =============================================================================
set -euo pipefail

# --- helpers -----------------------------------------------------------------
ok()   { echo "  [OK] $*"; }
fail() { echo "  [FAIL] $*" >&2; exit 1; }
info() { echo ""; echo "--- $* ---"; }

# --- args --------------------------------------------------------------------
TARGET_SLOT="${1:-}"

if [[ -z "$TARGET_SLOT" ]]; then
  fail "Usage: $0 <target-slot>   (slot: blue | green)"
fi

if [[ "$TARGET_SLOT" != "blue" && "$TARGET_SLOT" != "green" ]]; then
  fail "Invalid slot '$TARGET_SLOT'. Must be 'blue' or 'green'."
fi

# --- config ------------------------------------------------------------------
NAMESPACE="andromeda"
SERVICE="andromeda-service"

# --- read current state ------------------------------------------------------
info "Pre-flight check"

CURRENT_SLOT=$(kubectl get configmap bluegreen-state \
  -n "$NAMESPACE" \
  -o jsonpath='{.data.active-slot}' 2>/dev/null || echo "unknown")

ok "Current active slot: $CURRENT_SLOT"
ok "Swapping to: $TARGET_SLOT"

if [[ "$TARGET_SLOT" == "$CURRENT_SLOT" ]]; then
  fail "Slot '$TARGET_SLOT' is already active. Nothing to swap."
fi

# --- step 1: patch service selector ------------------------------------------
info "Step 1: Patching $SERVICE selector → slot=$TARGET_SLOT"

kubectl patch service "$SERVICE" \
  -n "$NAMESPACE" \
  --type='json' \
  -p="[{\"op\":\"replace\",\"path\":\"/spec/selector/slot\",\"value\":\"$TARGET_SLOT\"}]"

ok "Service selector updated."

# --- step 2: verify selector took effect -------------------------------------
info "Step 2: Verifying selector"

ACTIVE_SELECTOR=$(kubectl get service "$SERVICE" \
  -n "$NAMESPACE" \
  -o jsonpath='{.spec.selector.slot}')

if [[ "$ACTIVE_SELECTOR" != "$TARGET_SLOT" ]]; then
  fail "Selector verification failed. Expected '$TARGET_SLOT', got '$ACTIVE_SELECTOR'."
fi

ok "Selector confirmed: slot=$ACTIVE_SELECTOR"

# --- step 3: update ConfigMap state ------------------------------------------
info "Step 3: Updating bluegreen-state ConfigMap"

kubectl patch configmap bluegreen-state \
  -n "$NAMESPACE" \
  --type='merge' \
  -p="{\"data\":{\"active-slot\":\"$TARGET_SLOT\"}}"

ok "ConfigMap updated — active-slot=$TARGET_SLOT"

# --- step 4: save state locally (for bringup.sh on next cluster recreation) --
info "Step 4: Saving local state file"

echo "$TARGET_SLOT" > .bluegreen-state
ok "Wrote '$TARGET_SLOT' to .bluegreen-state"

# --- done --------------------------------------------------------------------
echo ""
echo "================================================================"
echo "  Traffic swap complete."
echo "  Active slot: $TARGET_SLOT"
echo "  Previous slot '$CURRENT_SLOT' remains running for instant rollback."
echo ""
echo "  To rollback immediately:"
echo "    bash scripts/blue-green-swap.sh $CURRENT_SLOT"
echo "================================================================"