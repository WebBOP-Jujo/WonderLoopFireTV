#!/usr/bin/env bash
# Runs ON the EC2 instance (Amazon Linux 2023) as root. Called by deploy-ec2.ps1 after uploading the code.
# Idempotent: safe to re-run on every deploy.
#   usage: sudo bash deploy/bootstrap.sh <site-address>
set -euo pipefail

SITE_ADDRESS="${1:?usage: bootstrap.sh <site-address>}"
APP_DIR=/opt/wonderloop/backend
COMPOSE_VERSION=v2.29.7

# 1. Docker engine
if ! command -v docker >/dev/null 2>&1; then
  echo ">> Installing Docker"
  dnf install -y docker
  systemctl enable --now docker
  usermod -aG docker ec2-user
fi

# 2. Docker Compose plugin (not packaged in the Amazon Linux 2023 repos)
if ! docker compose version >/dev/null 2>&1; then
  echo ">> Installing Docker Compose ${COMPOSE_VERSION}"
  mkdir -p /usr/local/lib/docker/cli-plugins
  curl -fsSL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-$(uname -m)" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

# 3. 2 GB swap so `npm ci` + image builds never run out of memory on a 2 GB instance
if [ ! -f /swapfile ]; then
  echo ">> Creating swap file"
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# 4. Runtime secrets (uploaded to /tmp by the deploy script): root-only, never inside the image
if [ -f /tmp/wonderloop.env ]; then
  install -m 600 -o root -g root /tmp/wonderloop.env "${APP_DIR}/.env"
  rm -f /tmp/wonderloop.env
fi
if [ ! -f "${APP_DIR}/.env" ]; then
  echo "!! ${APP_DIR}/.env is missing" >&2
  exit 1
fi

# 5. Build and (re)start
echo "SITE_ADDRESS=${SITE_ADDRESS}" > "${APP_DIR}/deploy/.env"
cd "${APP_DIR}/deploy"
docker compose up -d --build --remove-orphans
docker image prune -f >/dev/null

echo ">> Running containers:"
docker compose ps
