package org.wonderloop.app.platform

import org.wonderloop.app.BuildConfig

actual fun platformAppSecretKey(): String = BuildConfig.WONDERLOOP_APP_KEY
