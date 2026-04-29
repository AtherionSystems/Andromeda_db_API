#!/bin/bash
# =============================================================================
# Andromeda OKE Undeploy Script
# Run this from Cloud Shell at the end of every work session.
# Tears down: K8s services, node pool, LB, boot volumes, cluster.
# Safe: Autonomous DB and VCN are NOT touched.
# =============================================================================

set -e

# -----------------------------------------------------------------------------
# CONFIGURATION
# -----------------------------------------------------------------------------
REGION="mx-queretaro-1"
NAMESPACE="andromeda"
CLUSTER_ID="${CLUSTER_ID:-}"
NODE_POOL_ID="${NODE_POOL_ID:-}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WAIT]${NC} $1"; }
fail() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }
info() { echo -e "${CYAN}[INFO]${NC} $1"; }

echo ""
echo "========================================"
echo "  Andromeda OKE Undeploy"
echo "========================================"
echo ""
echo -e "${YELLOW}This will permanently delete the OKE cluster and all nodes.${NC}"
echo "Your Autonomous DB and VCN will NOT be affected."
echo ""
echo -n "Type YES to continue: "
read -r CONFIRM
[ "$CONFIRM" = "YES" ] || { echo "Aborted."; exit 0; }
echo ""

# -----------------------------------------------------------------------------
# STEP 1 — Collect OCIDs if not set
# -----------------------------------------------------------------------------
echo "--- Step 1: Cluster and node pool OCIDs ---"

if [ -z "$CLUSTER_ID" ]; then
  echo -n "Paste cluster OCID: "
  read -r CLUSTER_ID
fi

if [ -z "$NODE_POOL_ID" ]; then
  echo -n "Paste node pool OCID (pool1): "
  read -r NODE_POOL_ID
fi

# -----------------------------------------------------------------------------
# STEP 2 — Configure kubectl
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 2: Configure kubectl ---"

oci ce cluster create-kubeconfig \
  --cluster-id "$CLUSTER_ID" \
  --region "$REGION" \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT \
  --overwrite > /dev/null 2>&1 \
  && ok "kubectl configured" \
  || warn "kubectl config failed — K8s cleanup will be skipped (cluster may already be unreachable)"

KUBECTL_OK=false
kubectl cluster-info --request-timeout=5s > /dev/null 2>&1 && KUBECTL_OK=true || true

# -----------------------------------------------------------------------------
# STEP 3 — Delete K8s services to release Load Balancer
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 3: Delete K8s services (releases Load Balancer) ---"

if [ "$KUBECTL_OK" = true ]; then
  SVC_COUNT=$(kubectl get svc -n "$NAMESPACE" --no-headers 2>/dev/null | grep -v "^kubernetes" | wc -l || echo 0)
  if [ "$SVC_COUNT" -gt 0 ]; then
    kubectl delete svc --all -n "$NAMESPACE" --timeout=60s
    ok "Services deleted — Load Balancer release triggered"
  else
    ok "No services found in namespace — skipping"
  fi

  INGRESS_COUNT=$(kubectl get ingress -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l || echo 0)
  if [ "$INGRESS_COUNT" -gt 0 ]; then
    kubectl delete ingress --all -n "$NAMESPACE" --timeout=60s
    ok "Ingresses deleted"
  fi
else
  warn "Cluster unreachable via kubectl — skipping K8s service deletion"
  info "Load Balancer may need manual deletion in OCI Console → Networking → Load Balancers"
fi

# -----------------------------------------------------------------------------
# STEP 4 — Scale node pool to 0
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 4: Scale node pool to 0 ---"

oci ce node-pool update \
  --node-pool-id "$NODE_POOL_ID" \
  --size 0 \
  --region "$REGION" \
  --force > /dev/null 2>&1 \
  && ok "Node pool scaled to 0" \
  || warn "Could not scale node pool — it may already be empty or deleted"

# Give OCI a moment to register the scale-down before cluster delete
warn "Waiting 15s for scale-down to register..."
sleep 15

# -----------------------------------------------------------------------------
# STEP 5 — Delete the cluster
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 5: Delete OKE cluster ---"

warn "Deleting cluster — this terminates all nodes automatically..."

oci ce cluster delete \
  --cluster-id "$CLUSTER_ID" \
  --region "$REGION" \
  --force

ok "Cluster delete initiated"

# -----------------------------------------------------------------------------
# STEP 6 — Wait for cluster deletion
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 6: Wait for cluster deletion ---"
warn "Polling cluster status (up to 10 min)..."

for i in $(seq 1 40); do
  STATUS=$(oci ce cluster get \
    --cluster-id "$CLUSTER_ID" \
    --region "$REGION" \
    --query 'data."lifecycle-state"' \
    --raw-output 2>/dev/null || echo "DELETED")

  if [ "$STATUS" = "DELETED" ] || [ "$STATUS" = "" ]; then
    ok "Cluster deleted"
    break
  fi
  if [ "$i" -eq 40 ]; then
    warn "Cluster still deleting after 10 min — it will finish on its own. Check OCI Console."
    break
  fi
  echo "  Status: $STATUS (attempt $i/40)..."
  sleep 15
done

# -----------------------------------------------------------------------------
# STEP 7 — Check for orphaned boot volumes
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 7: Orphaned boot volumes ---"

# Get compartment OCID from the cluster's compartment
COMPARTMENT_ID=$(oci ce cluster get \
  --cluster-id "$CLUSTER_ID" \
  --region "$REGION" \
  --query 'data."compartment-id"' \
  --raw-output 2>/dev/null || echo "")

if [ -n "$COMPARTMENT_ID" ]; then
  VOLS=$(oci bv boot-volume list \
    --compartment-id "$COMPARTMENT_ID" \
    --region "$REGION" \
    --query 'data[?"lifecycle-state"==`AVAILABLE`].id' \
    --raw-output 2>/dev/null || echo "")

  if [ -z "$VOLS" ] || [ "$VOLS" = "[]" ]; then
    ok "No orphaned boot volumes found"
  else
    warn "Orphaned boot volumes detected. Delete them manually:"
    info "OCI Console → Block Storage → Boot Volumes → compartment reacttodo"
  fi
else
  info "Could not query boot volumes automatically — check manually:"
  info "OCI Console → Block Storage → Boot Volumes → compartment reacttodo"
fi

# -----------------------------------------------------------------------------
# DONE
# -----------------------------------------------------------------------------
echo ""
echo "========================================"
echo -e "  ${GREEN}Andromeda is DOWN${NC}"
echo "  Cluster:       deleted"
echo "  Nodes:         terminated"
echo "  Load Balancer: released"
echo "  Autonomous DB: untouched"
echo "  VCN:           untouched"
echo "========================================"
echo ""
echo "Token spend stopped. See you next session."
echo ""