#!/bin/bash
# =============================================================================
# andromeda-bringup.sh  (Blue-Green edition)
# Brings up a fresh OKE cluster for Andromeda after Custom Create in Console.
#
# What this script does:
#   1. Prompts for the new cluster OCID and configures kubectl
#   2. Waits for the node to be Ready
#   3. Creates namespace, OCIR secret, andromeda-secrets, db-wallet-secret
#   4. Applies all Blue-Green manifests (deployments + services + configmap)
#   5. Restores the active slot from .bluegreen-state (defaults to "blue")
#   6. Waits for the Load Balancer external IP
#
# Prerequisites:
#   - OKE cluster created via Custom Create using VCN oke-vcn-quick-andromedaoke-1b67f3489
#   - OCI CLI configured in Cloud Shell
#   - Wallet directory available locally
#   - Run from repo root: bash scripts/andromeda-bringup.sh
# =============================================================================
set -euo pipefail

# --- helpers -----------------------------------------------------------------
ok()   { echo "  [OK] $*"; }
fail() { echo "  [FAIL] $*" >&2; exit 1; }
info() { echo ""; echo "--- $* ---"; }

# --- config ------------------------------------------------------------------
NAMESPACE="andromeda"
REGION="mx-queretaro-1"
OCIR_SERVER="$REGION.ocir.io"
K8S_DIR="k8s"

# =============================================================================
# STEP 1 — kubectl config
# =============================================================================
info "Step 1: Configure kubectl"

echo -n "Paste the cluster OCID (from OCI Console → Cluster Details): "
read -r CLUSTER_OCID

if [[ -z "$CLUSTER_OCID" ]]; then
  fail "Cluster OCID cannot be empty."
fi

oci ce cluster create-kubeconfig \
  --cluster-id "$CLUSTER_OCID" \
  --file "$HOME/.kube/config" \
  --region "$REGION" \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT

ok "kubeconfig written."
kubectl cluster-info

# =============================================================================
# STEP 2 — Wait for node Ready
# =============================================================================
info "Step 2: Wait for node Ready"

ATTEMPTS=0
MAX_ATTEMPTS=30
until kubectl get nodes 2>/dev/null | grep -q " Ready"; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [[ $ATTEMPTS -ge $MAX_ATTEMPTS ]]; then
    fail "Node not Ready after $((MAX_ATTEMPTS * 10))s. Check OCI Console → Node Pools → pool1 → Details."
  fi
  echo "  Waiting for node... (${ATTEMPTS}/${MAX_ATTEMPTS})"
  sleep 10
done

ok "Node is Ready."
kubectl get nodes

# =============================================================================
# STEP 3 — Namespace
# =============================================================================
info "Step 3: Create namespace"

kubectl get namespace "$NAMESPACE" > /dev/null 2>&1 \
  && ok "Namespace '$NAMESPACE' already exists." \
  || { kubectl create namespace "$NAMESPACE" && ok "Namespace '$NAMESPACE' created."; }

# =============================================================================
# STEP 4 — OCIR image pull secret
# =============================================================================
info "Step 4: OCIR image pull secret"

if kubectl get secret ocir-secret -n "$NAMESPACE" > /dev/null 2>&1; then
  ok "ocir-secret already exists — skipping."
else
  echo -n "OCI username (format: axieboiigznv/<your-email>): "
  read -r OCIR_USER
  echo -n "OCI Auth Token (OCI Console → Profile → Auth Tokens): "
  read -rs OCIR_TOKEN
  echo ""

  kubectl create secret docker-registry ocir-secret \
    --docker-server="$OCIR_SERVER" \
    --docker-username="$OCIR_USER" \
    --docker-password="$OCIR_TOKEN" \
    --docker-email="a01571222@tec.mx" \
    -n "$NAMESPACE"
  ok "ocir-secret created."
fi

# =============================================================================
# STEP 5 — App secrets
# =============================================================================
info "Step 5: App secrets (andromeda-secrets)"

if kubectl get secret andromeda-secrets -n "$NAMESPACE" > /dev/null 2>&1; then
  ok "andromeda-secrets already exists — skipping."
else
  echo "Enter values for andromeda-secrets:"
  echo -n "  DB_USERNAME: ";                  read -r  DB_USERNAME
  echo -n "  DB_PASSWORD: ";                  read -rs DB_PASSWORD;   echo ""
  echo -n "  WALLET_TRUSTSTORE_PASSWORD: ";   read -rs WALLET_TS;     echo ""
  echo -n "  WALLET_KEYSTORE_PASSWORD: ";     read -rs WALLET_KS;     echo ""
  echo -n "  TELEGRAM_BOT_TOKEN: ";           read -rs TG_TOKEN;      echo ""
  echo -n "  TELEGRAM_BOT_USERNAME: ";        read -r  TG_USER
  echo -n "  JWT_SECRET: ";                   read -rs JWT_SECRET;    echo ""
  echo -n "  OAUTH2_ISSUER_URI: ";            read -r  OAUTH2_URI

  kubectl create secret generic andromeda-secrets \
    --from-literal=DB_USERNAME="$DB_USERNAME" \
    --from-literal=DB_PASSWORD="$DB_PASSWORD" \
    --from-literal=WALLET_TRUSTSTORE_PASSWORD="$WALLET_TS" \
    --from-literal=WALLET_KEYSTORE_PASSWORD="$WALLET_KS" \
    --from-literal=TELEGRAM_BOT_TOKEN="$TG_TOKEN" \
    --from-literal=TELEGRAM_BOT_USERNAME="$TG_USER" \
    --from-literal=JWT_SECRET="$JWT_SECRET" \
    --from-literal=OAUTH2_ISSUER_URI="$OAUTH2_URI" \
    -n "$NAMESPACE"
  ok "andromeda-secrets created."
fi

# =============================================================================
# STEP 6 — Wallet secret
# =============================================================================
info "Step 6: DB wallet secret (db-wallet-secret)"

if kubectl get secret db-wallet-secret -n "$NAMESPACE" > /dev/null 2>&1; then
  ok "db-wallet-secret already exists — skipping."
else
  echo -n "Path to wallet directory (e.g. /home/a01571222/wallet): "
  read -r WALLET_DIR

  [[ -d "$WALLET_DIR" ]] || fail "Wallet directory '$WALLET_DIR' not found."

  kubectl create secret generic db-wallet-secret \
    --from-file="$WALLET_DIR" \
    -n "$NAMESPACE"
  ok "db-wallet-secret created."
fi

# =============================================================================
# STEP 7 — Apply Blue-Green manifests
# =============================================================================
info "Step 7: Apply Blue-Green Kubernetes manifests"

kubectl apply -f "$K8S_DIR/andromeda-deployment-blue.yaml"
kubectl apply -f "$K8S_DIR/andromeda-deployment-green.yaml"
ok "Deployments applied (blue + green)."

kubectl apply -f "$K8S_DIR/andromeda-service-blue-internal.yaml"
kubectl apply -f "$K8S_DIR/andromeda-service-green-internal.yaml"
ok "Internal services applied."

kubectl apply -f "$K8S_DIR/andromeda-service-active.yaml"
ok "Active LoadBalancer service applied."

# =============================================================================
# STEP 8 — Restore active slot
# =============================================================================
info "Step 8: Restore active slot"

if [[ -f ".bluegreen-state" ]]; then
  ACTIVE_SLOT=$(cat .bluegreen-state)
  ok "Read active slot from .bluegreen-state: $ACTIVE_SLOT"
else
  ACTIVE_SLOT="blue"
  ok "No .bluegreen-state found — defaulting to: $ACTIVE_SLOT"
  echo "$ACTIVE_SLOT" > .bluegreen-state
fi

if [[ "$ACTIVE_SLOT" != "blue" && "$ACTIVE_SLOT" != "green" ]]; then
  echo "  WARNING: .bluegreen-state contains invalid value '$ACTIVE_SLOT'. Defaulting to 'blue'."
  ACTIVE_SLOT="blue"
  echo "$ACTIVE_SLOT" > .bluegreen-state
fi

kubectl patch service andromeda-service \
  -n "$NAMESPACE" \
  --type='json' \
  -p="[{\"op\":\"replace\",\"path\":\"/spec/selector/slot\",\"value\":\"$ACTIVE_SLOT\"}]"
ok "Service selector patched → slot=$ACTIVE_SLOT"

kubectl create configmap bluegreen-state \
  --from-literal=active-slot="$ACTIVE_SLOT" \
  -n "$NAMESPACE" \
  --dry-run=client -o yaml | kubectl apply -f -
ok "bluegreen-state ConfigMap restored — active-slot=$ACTIVE_SLOT"

# =============================================================================
# STEP 9 — Wait for Load Balancer external IP
# =============================================================================
info "Step 9: Waiting for Load Balancer external IP"

echo "  (This can take 2–4 minutes on a fresh cluster)"

ATTEMPTS=0
MAX_ATTEMPTS=30
EXTERNAL_IP=""

until [[ -n "$EXTERNAL_IP" ]]; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [[ $ATTEMPTS -ge $MAX_ATTEMPTS ]]; then
    fail "Load Balancer IP not assigned after $((MAX_ATTEMPTS * 10))s.
  Debug: kubectl describe svc andromeda-service -n andromeda
  Common cause: missing port 80 ingress rule on oke-svclbseclist-* security list."
  fi

  EXTERNAL_IP=$(kubectl get svc andromeda-service \
    -n "$NAMESPACE" \
    -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)

  if [[ -z "$EXTERNAL_IP" ]]; then
    echo "  Waiting for IP... (${ATTEMPTS}/${MAX_ATTEMPTS})"
    sleep 10
  fi
done

ok "External IP assigned: $EXTERNAL_IP"

# =============================================================================
# DONE
# =============================================================================
echo ""
echo "================================================================"
echo "  Andromeda is up."
echo ""
echo "  External IP:  $EXTERNAL_IP"
echo "  API base URL: http://$EXTERNAL_IP/api"
echo "  Health check: http://$EXTERNAL_IP/actuator/health"
echo "  Active slot:  $ACTIVE_SLOT"
echo ""
echo "  Post this IP in the team channel immediately."
echo ""
echo "  Reminder: add port 80 ingress rule to oke-svclbseclist-*"
echo "  if not already present (OCI Console → VCN → Security Lists)."
echo "================================================================"