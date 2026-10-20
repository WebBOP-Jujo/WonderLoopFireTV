package org.wonderloop.app.platform

/**
 * Shared key sent in the X-WonderLoop-Key header. Injected at build time
 * (local.properties `wonderloop.appKey` or env `WONDERLOOP_APP_KEY`), never hardcoded.
 */
expect fun platformAppSecretKey(): String
