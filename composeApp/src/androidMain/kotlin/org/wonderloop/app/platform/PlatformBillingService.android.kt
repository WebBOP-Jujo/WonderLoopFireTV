package org.wonderloop.app.platform

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import org.wonderloop.app.data.SubscriptionManager
import org.wonderloop.app.data.SubscriptionPlan
import java.lang.ref.WeakReference

actual object PlatformBillingService {
    /**
     * RevenueCat Public API Key:
     * - Test Store key: "test_jDMCrbZtrDrCuEtVNXWZQDoQNWJ"
     * - Google Play key (when switching to production/play): "goog_..."
     */
    const val DEFAULT_REVENUECAT_API_KEY = "test_jDMCrbZtrDrCuEtVNXWZQDoQNWJ"

    private var activityRef: WeakReference<Activity>? = null

    fun attachActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun configureIfNeeded(activity: Activity, apiKeyOverride: String? = null) {
        attachActivity(activity)
        if (Purchases.isConfigured) return

        val savedKey = PlatformStorage.getString("revenuecat_api_key", "").trim()
        val apiKey = (apiKeyOverride?.trim()?.takeIf { it.isNotBlank() }
            ?: savedKey.takeIf { it.isNotBlank() }
            ?: DEFAULT_REVENUECAT_API_KEY).trim()

        if (apiKey.isBlank() || apiKey == "goog_placeholder_api_key") {
            println("💳 [RevenueCat] No real API key configured yet (using sandbox fallback until key is set)")
            return
        }

        try {
            var appUserId = PlatformStorage.getString("app_user_id", "")
            if (appUserId.isBlank()) {
                appUserId = "wl_anon_" + (1..16).map { (('a'..'z') + ('0'..'9')).random() }.joinToString("")
                PlatformStorage.saveString("app_user_id", appUserId)
            }

            Purchases.logLevel = LogLevel.DEBUG
            val config = PurchasesConfiguration.Builder(activity.applicationContext, apiKey)
                .appUserID(appUserId)
                .build()
            Purchases.configure(config)
            println("✅ [RevenueCat] Configured successfully (User: $appUserId, Key prefix: ${apiKey.take(5)}...)")

            // Sync existing subscription status from RevenueCat on launch
            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    syncCustomerInfoToSubscriptionManager(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    println("⚠️ [RevenueCat] Initial CustomerInfo check warning: ${error.message}")
                }
            })
        } catch (e: Exception) {
            println("⚠️ [RevenueCat] Failed to configure SDK: ${e.message}")
        }
    }

    private fun candidateIdsForPlan(planId: String): List<String> {
        return when (planId) {
            "wonderloop_annual_star_15", "wonderloop_annual_basic_6" ->
                listOf(planId, "yearly", "\$rc_annual", "annual")
            "wonderloop_pro_15", "wonderloop_basic_6" ->
                listOf(planId, "monthly", "\$rc_monthly")
            "wonderloop_pack_5" ->
                listOf(planId, "lifetime", "\$rc_lifetime")
            else -> listOf(planId)
        }
    }

    private fun resolvePlanFromCustomerInfo(customerInfo: CustomerInfo): SubscriptionPlan? {
        val activeSubs = customerInfo.activeSubscriptions
        val primaryEntitlement = (customerInfo.entitlements["wonderloop_pro"]?.takeIf { it.isActive }
            ?: customerInfo.entitlements["premium_access"]?.takeIf { it.isActive })
        val entitlementProduct = primaryEntitlement?.productIdentifier

        val allActiveIds = buildSet {
            addAll(activeSubs)
            if (!entitlementProduct.isNullOrBlank()) add(entitlementProduct)
            customerInfo.entitlements.active.values.forEach { ent ->
                if (ent.productIdentifier.isNotBlank()) add(ent.productIdentifier)
            }
        }

        val hasActiveEntitlement = primaryEntitlement != null || customerInfo.entitlements.active.isNotEmpty()

        return when {
            allActiveIds.any { it.startsWith("wonderloop_annual_star_15") } -> SubscriptionPlan.STAR_ANNUAL
            allActiveIds.any { it.startsWith("wonderloop_pro_15") } -> SubscriptionPlan.PRO_MONTHLY
            allActiveIds.any { it.startsWith("wonderloop_annual_basic_6") } -> SubscriptionPlan.BASIC_ANNUAL
            allActiveIds.any { it.startsWith("wonderloop_basic_6") } -> SubscriptionPlan.BASIC_MONTHLY
            allActiveIds.any { it.equals("yearly", ignoreCase = true) || it.contains("yearly", ignoreCase = true) } ->
                if (SubscriptionManager.activePlan.value == SubscriptionPlan.BASIC_ANNUAL) SubscriptionPlan.BASIC_ANNUAL else SubscriptionPlan.STAR_ANNUAL
            allActiveIds.any { it.equals("monthly", ignoreCase = true) || it.contains("monthly", ignoreCase = true) } ->
                if (SubscriptionManager.activePlan.value == SubscriptionPlan.BASIC_MONTHLY) SubscriptionPlan.BASIC_MONTHLY else SubscriptionPlan.PRO_MONTHLY
            hasActiveEntitlement -> {
                if (SubscriptionManager.activePlan.value != SubscriptionPlan.FREE) {
                    SubscriptionManager.activePlan.value
                } else {
                    SubscriptionPlan.STAR_ANNUAL
                }
            }
            else -> null
        }
    }

    private fun syncCustomerInfoToSubscriptionManager(customerInfo: CustomerInfo): Boolean {
        val resolvedPlan = resolvePlanFromCustomerInfo(customerInfo)
        if (resolvedPlan != null) {
            SubscriptionManager.syncRestoredPlan(resolvedPlan)
            return true
        }
        return false
    }

    actual fun purchasePlan(planId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            if (!Purchases.isConfigured) {
                println("💳 [RevenueCat] SDK not configured with real key; simulating purchase for plan: $planId")
                onSuccess()
                return
            }

            val activity = activityRef?.get()
            if (activity == null || activity.isFinishing) {
                onError("No se pudo abrir la ventana de compra")
                return
            }

            val purchaseCallback = object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    println("🎉 [RevenueCat] Purchase completed for $planId! Tx: ${storeTransaction.orderId}")
                    onSuccess()
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    if (userCancelled || error.code == PurchasesErrorCode.PurchaseCancelledError) {
                        println("ℹ️ [RevenueCat] User cancelled purchase for $planId")
                        onError("USER_CANCELLED")
                    } else {
                        println("❌ [RevenueCat] Purchase error for $planId: ${error.code} - ${error.message}")
                        onError(error.message)
                    }
                }
            }

            val candidates = candidateIdsForPlan(planId)

            // 1. Try finding the package inside configured RevenueCat Offerings
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    val allPackages = buildList {
                        offerings.current?.availablePackages?.let { addAll(it) }
                        offerings.all.values.forEach { off -> addAll(off.availablePackages) }
                    }.distinctBy { it.identifier to it.product.id }

                    var matchedPackage = allPackages.firstOrNull { pkg ->
                        candidates.any { cand ->
                            pkg.product.id.equals(cand, ignoreCase = true) ||
                                pkg.product.id.startsWith("$cand:", ignoreCase = true) ||
                                pkg.product.purchasingData.productId.equals(cand, ignoreCase = true) ||
                                pkg.identifier.equals(cand, ignoreCase = true)
                        }
                    }

                    // If still not found and Offerings has packages, pick the first available package as fallback
                    if (matchedPackage == null && allPackages.isNotEmpty()) {
                        matchedPackage = allPackages.first()
                    }

                    if (matchedPackage != null) {
                        println("💳 [RevenueCat] Purchasing Offering package '${matchedPackage.identifier}' (product: ${matchedPackage.product.id}) for plan '$planId'")
                        Purchases.sharedInstance.purchase(
                            PurchaseParams.Builder(activity, matchedPackage).build(),
                            purchaseCallback
                        )
                    } else {
                        // 2. Fallback: fetch StoreProduct directly by candidate IDs (SUBS or INAPP)
                        purchaseStoreProductById(activity, candidates, purchaseCallback, onError)
                    }
                }

                override fun onError(error: PurchasesError) {
                    println("⚠️ [RevenueCat] getOfferings failed (${error.message}), falling back to getProducts($candidates)")
                    purchaseStoreProductById(activity, candidates, purchaseCallback, onError)
                }
            })
        } catch (e: Exception) {
            onError(e.message ?: "Error al procesar la compra")
        }
    }

    private fun purchaseStoreProductById(
        activity: Activity,
        candidateIds: List<String>,
        purchaseCallback: PurchaseCallback,
        onError: (String) -> Unit
    ) {
        val isConsumablePack = candidateIds.firstOrNull() == "wonderloop_pack_5"
        val primaryType = if (isConsumablePack) ProductType.INAPP else ProductType.SUBS
        val fallbackType = if (isConsumablePack) ProductType.SUBS else ProductType.INAPP

        Purchases.sharedInstance.getProducts(
            candidateIds,
            primaryType,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    val product = storeProducts.firstOrNull()
                    if (product != null) {
                        println("💳 [RevenueCat] Purchasing StoreProduct '${product.id}' ($primaryType)")
                        Purchases.sharedInstance.purchase(
                            PurchaseParams.Builder(activity, product).build(),
                            purchaseCallback
                        )
                    } else {
                        Purchases.sharedInstance.getProducts(
                            candidateIds,
                            fallbackType,
                            object : GetStoreProductsCallback {
                                override fun onReceived(fallbackProducts: List<StoreProduct>) {
                                    val fbProduct = fallbackProducts.firstOrNull()
                                    if (fbProduct != null) {
                                        println("💳 [RevenueCat] Purchasing StoreProduct '${fbProduct.id}' ($fallbackType)")
                                        Purchases.sharedInstance.purchase(
                                            PurchaseParams.Builder(activity, fbProduct).build(),
                                            purchaseCallback
                                        )
                                    } else {
                                        onError("Producto no encontrado en RevenueCat (${candidateIds.joinToString()})")
                                    }
                                }

                                override fun onError(error: PurchasesError) {
                                    onError(error.message)
                                }
                            }
                        )
                    }
                }

                override fun onError(error: PurchasesError) {
                    onError(error.message)
                }
            }
        )
    }

    actual fun restorePurchases(onSuccess: (Boolean) -> Unit, onError: (String) -> Unit) {
        try {
            if (!Purchases.isConfigured) {
                onSuccess(SubscriptionManager.isPremium)
                return
            }
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val synced = syncCustomerInfoToSubscriptionManager(customerInfo)
                    onSuccess(synced)
                }

                override fun onError(error: PurchasesError) {
                    onError(error.message)
                }
            })
        } catch (e: Exception) {
            onError(e.message ?: "Error al restaurar compras")
        }
    }
}
