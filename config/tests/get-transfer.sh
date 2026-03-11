#!/bin/bash
set -euo pipefail
set -x

KC="http://localhost:8180"
REALM="banking"
CLIENT_ID="banking-gateway"
CLIENT_SECRET="$1"
TRANSFER_ID="$2"
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

curl -s -i -X GET "$GATEWAY_BASE/banking/transfer/$TRANSFER_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"