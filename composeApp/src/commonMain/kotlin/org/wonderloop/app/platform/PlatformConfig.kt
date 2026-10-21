package org.wonderloop.app.platform

/**
 * Shared key sent in the X-WonderLoop-Key header. Injected at build time
 * (local.properties `wonderloop.appKey` or env `WONDERLOOP_APP_KEY`), never hardcoded.
 */
expect fun platformAppSecretKey(): String

/**
 * Default backend URL injected at build time (local.properties `wonderloop.backendUrl` or env
 * `WONDERLOOP_BACKEND_URL`); defaults to the public HTTPS backend deployed on Amazon EC2.
 */
expect fun platformDefaultBackendUrl(): String
