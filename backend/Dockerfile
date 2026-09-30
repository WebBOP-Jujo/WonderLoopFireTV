# ==========================================
# WonderLoop Gateway — Google Cloud Run Dockerfile
# Optimized for RevenueCat Shipaton 2026
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

# Create non-root app user for container security
USER node

# Copy production package manifests and install only production dependencies
COPY --chown=node:node package*.json ./
RUN npm ci --only=production

# Copy compiled JavaScript from builder
COPY --chown=node:node --from=builder /app/dist ./dist

# Expose standard Cloud Run port
EXPOSE 8080

# Run the production server
CMD ["node", "dist/server.js"]
