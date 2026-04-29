#!/bin/bash
# =============================================================================
# Andromeda OKE Bring-Up Script
# Run this from Cloud Shell after recreating the OKE cluster.
# Prerequisites: OCI CLI configured, kubectl configured for andromedaoke.
# =============================================================================

set -e

# -----------------------------------------------------------------------------
# CONFIGURATION — update these if your cluster changes
# -----------------------------------------------------------------------------
CLUSTER_ID="${CLUSTER_ID:-}"           # Set via env or will prompt
REGION="mx-queretaro-1"
NAMESPACE="andromeda"
IMAGE="mx-queretaro-1.ocir.io/axieboiigznv/andromeda-backend:0.1"
OCIR_SERVER="mx-queretaro-1.ocir.io"
MANIFEST="k8s/andromeda-deployment.yaml"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WAIT]${NC} $1"; }
fail() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

echo ""
echo "========================================"
echo "  Andromeda OKE Bring-Up"
echo "========================================"
echo ""

# -----------------------------------------------------------------------------
# STEP 1 — Configure kubectl
# -----------------------------------------------------------------------------
echo "--- Step 1: Configure kubectl ---"

if [ -z "$CLUSTER_ID" ]; then
  echo -n "Paste your cluster OCID: "
  read -r CLUSTER_ID
fi

oci ce cluster create-kubeconfig \
  --cluster-id "$CLUSTER_ID" \
  --region "$REGION" \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT \
  --overwrite

kubectl cluster-info --request-timeout=10s > /dev/null 2>&1 \
  && ok "kubectl connected to cluster" \
  || fail "kubectl cannot reach cluster — is the node pool running and Ready?"

# -----------------------------------------------------------------------------
# STEP 2 — Wait for at least 1 node to be Ready
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 2: Wait for node Ready ---"
warn "Waiting for node to become Ready (up to 5 min)..."

for i in $(seq 1 30); do
  READY=$(kubectl get nodes --no-headers 2>/dev/null | grep -c " Ready" || true)
  if [ "$READY" -ge 1 ]; then
    ok "Node is Ready"
    break
  fi
  if [ "$i" -eq 30 ]; then
    fail "No nodes Ready after 5 min. Check OCI Console → Node Pools → pool1 → Details."
  fi
  sleep 10
done

# -----------------------------------------------------------------------------
# STEP 3 — Create namespace
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 3: Create namespace ---"

kubectl get namespace "$NAMESPACE" > /dev/null 2>&1 \
  && ok "Namespace '$NAMESPACE' already exists" \
  || { kubectl create namespace "$NAMESPACE" && ok "Namespace '$NAMESPACE' created"; }

# -----------------------------------------------------------------------------
# STEP 4 — OCIR image pull secret
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 4: OCIR image pull secret ---"

if kubectl get secret ocir-secret -n "$NAMESPACE" > /dev/null 2>&1; then
  ok "ocir-secret already exists — skipping"
else
  echo -n "OCI username (format: <tenancy>/<username>, e.g. axieboiigznv/a01571222@tec.mx): "
  read -r OCIR_USER
  echo -n "OCI Auth Token (from OCI Console → Profile → Auth Tokens): "
  read -rs OCIR_TOKEN
  echo ""

  kubectl create secret docker-registry ocir-secret \
    --docker-server="$OCIR_SERVER" \
    --docker-username="$OCIR_USER" \
    --docker-password="$OCIR_TOKEN" \
    --docker-email="a01571222@tec.mx" \
    -n "$NAMESPACE"
  ok "ocir-secret created"
fi

# -----------------------------------------------------------------------------
# STEP 5 — App secrets (andromeda-secrets + db-wallet-secret)
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 5: App secrets ---"

if kubectl get secret andromeda-secrets -n "$NAMESPACE" > /dev/null 2>&1; then
  ok "andromeda-secrets already exists — skipping"
else
  echo "Enter secret values for andromeda-secrets:"
  echo -n "  DB_USERNAME: "; read -r DB_USERNAME
  echo -n "  DB_PASSWORD: "; read -rs DB_PASSWORD; echo ""
  echo -n "  WALLET_TRUSTSTORE_PASSWORD: "; read -rs WALLET_TS_PASS; echo ""
  echo -n "  WALLET_KEYSTORE_PASSWORD: "; read -rs WALLET_KS_PASS; echo ""
  echo -n "  TELEGRAM_BOT_TOKEN: "; read -rs TG_TOKEN; echo ""
  echo -n "  TELEGRAM_BOT_USERNAME: "; read -r TG_USER

  kubectl create secret generic andromeda-secrets \
    --from-literal=DB_USERNAME="$DB_USERNAME" \
    --from-literal=DB_PASSWORD="$DB_PASSWORD" \
    --from-literal=WALLET_TRUSTSTORE_PASSWORD="$WALLET_TS_PASS" \
    --from-literal=WALLET_KEYSTORE_PASSWORD="$WALLET_KS_PASS" \
    --from-literal=TELEGRAM_BOT_TOKEN="$TG_TOKEN" \
    --from-literal=TELEGRAM_BOT_USERNAME="$TG_USER" \
    -n "$NAMESPACE"
  ok "andromeda-secrets created"
fi

if kubectl get secret db-wallet-secret -n "$NAMESPACE" > /dev/null 2>&1; then
  ok "db-wallet-secret already exists — skipping"
else
  echo -n "Path to wallet directory (e.g. /home/a01571222/wallet): "
  read -r WALLET_DIR

  [ -d "$WALLET_DIR" ] || fail "Wallet directory '$WALLET_DIR' not found."

  kubectl create secret generic db-wallet-secret \
    --from-file="$WALLET_DIR" \
    -n "$NAMESPACE"
  ok "db-wallet-secret created"
fi

# -----------------------------------------------------------------------------
# STEP 6 — Apply deployment manifest
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 6: Apply manifest ---"

[ -f "$MANIFEST" ] || fail "Manifest not found at '$MANIFEST'. Run this script from your repo root."

kubectl apply -f "$MANIFEST"
ok "Manifest applied"

# -----------------------------------------------------------------------------
# STEP 7 — Wait for pod Running
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 7: Wait for pod Running ---"
warn "Waiting for pod to start (up to 5 min — image pull takes time)..."

for i in $(seq 1 30); do
  STATUS=$(kubectl get pods -n "$NAMESPACE" --no-headers 2>/dev/null | grep "andromeda" | awk '{print $3}' | head -1)
  if [ "$STATUS" = "Running" ]; then
    ok "Pod is Running"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo ""
    echo "Pod status after 5 min:"
    kubectl get pods -n "$NAMESPACE"
    echo ""
    echo "Pod events:"
    kubectl describe pod -n "$NAMESPACE" -l app=andromeda | tail -20
    fail "Pod did not reach Running state. Check logs above."
  fi
  echo "  Status: ${STATUS:-Pending} (attempt $i/30)..."
  sleep 10
done

# -----------------------------------------------------------------------------
# STEP 8 — Get external IP
# -----------------------------------------------------------------------------
echo ""
echo "--- Step 8: Get external IP ---"
warn "Waiting for Load Balancer external IP (up to 4 min)..."

for i in $(seq 1 24); do
  EXTERNAL_IP=$(kubectl get svc andromeda-service -n "$NAMESPACE" --no-headers 2>/dev/null | awk '{print $4}')
  if [ -n "$EXTERNAL_IP" ] && [ "$EXTERNAL_IP" != "<pending>" ]; then
    ok "Load Balancer ready"
    echo ""
    echo "========================================"
    echo -e "  ${GREEN}Andromeda is UP${NC}"
    echo "  External IP : $EXTERNAL_IP"
    echo "  API base URL: http://$EXTERNAL_IP/api"
    echo "  Health check: http://$EXTERNAL_IP/actuator/health"
    echo "========================================"
    echo ""
    echo "Post this IP in your team channel now."
    exit 0
  fi
  echo "  Still pending (attempt $i/24)..."
  sleep 10
done

fail "Load Balancer IP still pending after 4 min. Check OCI Console → Networking → Load Balancers."