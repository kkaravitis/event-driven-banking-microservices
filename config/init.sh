#!/bin/bash
set -e

cd ../
docker network create banking-net
docker compose up -d
