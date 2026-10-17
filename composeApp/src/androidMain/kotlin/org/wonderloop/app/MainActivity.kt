package org.wonderloop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 0. Initialize Local Platform Storage & Native Audio handles (non-blocking)
        org.wonderloop.app.platform.PlatformStorage.init(this)
        org.wonderloop.app.platform.PlatformAudioService.init(this)
        org.wonderloop.app.platform.PlatformBillingService.attachActivity(this)

        setContent {
            App(onExitApp = { finish() })
        }

        // Initialize billing off the critical first-frame path
        window.decorView.post {
            org.wonderloop.app.platform.PlatformBillingService.configureIfNeeded(this)
        }
    }

    override fun onResume() {
        super.onResume()
        org.wonderloop.app.platform.PlatformBillingService.attachActivity(this)
        AppLifecycleObserver.onAppResumed()
    }

    override fun onPause() {
        super.onPause()
        org.wonderloop.app.platform.PlatformAudioService.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        org.wonderloop.app.platform.PlatformAudioService.shutdown()
    }
}
