package org.wonderloop.app.platform

expect object PlatformBillingService {
    fun purchasePlan(planId: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun restorePurchases(onSuccess: (Boolean) -> Unit, onError: (String) -> Unit)
}
