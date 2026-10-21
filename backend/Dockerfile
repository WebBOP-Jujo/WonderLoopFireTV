# ==========================================
# WonderLoop TV Backend Gateway — container image
# Amazon Developer Hackathon 2026 (deployed on Amazon EC2 behind Caddy, see deploy/)
# ==========================================

# Stage 1: Build stage
FROM node:22-alpine AS builder

WORKDIR /app

# Install build dependencies
COPY package*.json tsconfig.json ./
RUN npm ci

# Copy source code and build TypeScript
COPY src/ ./src/
RUN npm run build

# ==========================================
# Stage 2: Production runtime stage
# ==========================================
FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=8080

# Install only production dependencies (as root: /app is root-owned; the app only needs read access)
COPY package*.json ./
RUN npm ci --omit=dev && npm cache clean --force

# Copy compiled JavaScript from builder
COPY --from=builder /app/dist ./dist

# Run as the unprivileged user shipped with the official Node image
USER node

EXPOSE 8080

CMD ["node", "dist/server.js"]
