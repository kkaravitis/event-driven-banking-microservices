#!/bin/bash
set -euo pipefail

KC="http://localhost:8180"
REALM="banking"
CLIENT_ID="banking-gateway"
CLIENT_SECRET="$1"
CANCEL_DELAY_IN_SECONDS="$2"
USER="alice"
PASS="alice-password"


GATEWAY_BASE="http://localhost:8079"

get_token () {
  curl -s -X POST "$KC/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=$CLIENT_ID" \
    -d "client_secret=$CLIENT_SECRET" \
    -d "username=$USER" \
    -d "password=$PASS" \
  | jq -r '.access_token'
}

TOKEN="$(get_token)"

echo "Calling initiate transfer..."
INITIATE_RESPONSE="$(curl -s -i -X POST "$GATEWAY_BASE/banking/transfer" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId":"ACC-101",
    "toAccountId":"ACC-201",
    "amount":1,
    "currency":"EUR"
  }')"

# Extract body (everything after the blank line) then parse transferId
INITIATE_BODY="$(printf '%s' "$INITIATE_RESPONSE" | sed -n '/^\r\{0,1\}$/,$p' | tail -n +2)"
TRANSFER_ID="$(printf '%s' "$INITIATE_BODY" | jq -r '.transferId // empty')"

if [[ -z "$TRANSFER_ID" || "$TRANSFER_ID" == "null" ]]; then
  echo "ERROR: Could not parse transferId from response body:"
  echo "$INITIATE_BODY"
  exit 1
fi

echo "transferId: $TRANSFER_ID"

sleep $CANCEL_DELAY_IN_SECONDS

curl -s -i -X POST "$GATEWAY_BASE/banking/transfer/$TRANSFER_ID/cancel" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \

sleep 5

TOKEN="$(get_token)"

echo "Calling get transfer..."
curl -i -s "$GATEWAY_BASE/banking/transfer/$TRANSFER_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"