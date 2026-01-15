#!/bin/bash
set -e

curl -i -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   --data-binary @./transfer-outbox.json
curl -i -X POST http://localhost:8083/connectors   -H "Content-Type: application/json"   --data-binary @./account-outbox.json

curl -s http://localhost:8083/connectors