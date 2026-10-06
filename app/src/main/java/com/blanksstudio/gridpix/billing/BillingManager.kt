package com.blanksstudio.gridpix.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.blanksstudio.gridpix.BuildConfig
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed interface BillingAvailability {
    data object Connecting : BillingAvailability
    data object Available : BillingAvailability
    data class Unavailable(val reason: String) : BillingAvailability
}

sealed interface BillingEvent {
    data object Purchased : BillingEvent
    data object Pending : BillingEvent
    data object Cancelled : BillingEvent
    data class Restored(val anyOwned: Boolean) : BillingEvent
    data object StoreUnavailable : BillingEvent
    data class Error(val message: String) : BillingEvent
}

/**
 * Play Billing for the products in SPEC section 6.
 * - Started once from [com.blanksstudio.gridpix.GridPixApp]; Play is re-queried on every launch.
 * - Consumables (`hints_10`, `hints_50`) credit the hint balance once per purchase token, then are consumed.
 * - Non-consumables are acknowledged and mirrored into `owned_products`; `everything` also grants 100 hints once.
 * - When Play cannot be reached the stored state is left alone; a successful query is the only thing
 *   that grants or revokes ownership.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val settings: SettingsRepository,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val _availability = MutableStateFlow<BillingAvailability>(BillingAvailability.Connecting)
    val availability: StateFlow<BillingAvailability> = _availability.asStateFlow()

    /** Formatted local prices from Play keyed by product id (SPEC section 6: never hard-coded). */
    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private val productDetails = mutableMapOf<String, ProductDetails>()
    private var started = false

    fun start() {
        if (started) return
        started = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    _availability.value = BillingAvailability.Available
                    scope.launch {
                        loadProductDetails()
                        restorePurchases(silent = true)
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                    _availability.value = BillingAvailability.Unavailable(result.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                _availability.value = BillingAvailability.Connecting
            }
        })
    }

    private suspend fun loadProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                Products.ALL.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()
        val (result, details) = suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { billingResult, queryResult ->
                cont.resume(billingResult to queryResult.productDetailsList)
            }
        }
        if (result.responseCode == BillingResponseCode.OK) {
            details.forEach { productDetails[it.productId] = it }
            _prices.value = details.mapNotNull { d ->
                d.oneTimePurchaseOfferDetails?.formattedPrice?.let { d.productId to it }
            }.toMap()
        } else {
            Log.w(TAG, "Product details unavailable: ${result.responseCode} ${result.debugMessage}")
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        val details = productDetails[productId]
        if (details == null || _availability.value !is BillingAvailability.Available) {
            emit(BillingEvent.StoreUnavailable)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            emit(BillingEvent.Error(result.debugMessage.ifBlank { "code ${result.responseCode}" }))
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch {
                val owned = settings.snapshotOwned().toMutableSet()
                purchases.orEmpty().forEach { handlePurchase(it, owned, notify = true) }
                settings.setOwnedProducts(owned)
            }
            BillingResponseCode.USER_CANCELED -> emit(BillingEvent.Cancelled)
            BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restorePurchases(silent = false) }
            else -> emit(BillingEvent.Error(result.debugMessage.ifBlank { "code ${result.responseCode}" }))
        }
    }

    /**
     * Re-reads owned purchases from Play. With [silent] the result is applied without an event
     * (app start); otherwise the user asked for a restore and gets told what happened.
     */
    suspend fun restorePurchases(silent: Boolean) {
        if (_availability.value !is BillingAvailability.Available) {
            if (!silent) emit(BillingEvent.StoreUnavailable)
            return
        }
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val (result, purchases) = suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { billingResult, list -> cont.resume(billingResult to list) }
        }
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "queryPurchases failed: ${result.responseCode} ${result.debugMessage}")
            if (!silent) emit(BillingEvent.Error(result.debugMessage.ifBlank { "code ${result.responseCode}" }))
            return
        }
        val owned = mutableSetOf<String>()
        for (purchase in purchases) handlePurchase(purchase, owned, notify = false)
        settings.setOwnedProducts(owned)
        if (!silent) emit(BillingEvent.Restored(owned.isNotEmpty()))
    }

    /** Applies one Play purchase: credits consumables, acknowledges non-consumables, adds them to [owned]. */
    private suspend fun handlePurchase(purchase: Purchase, owned: MutableSet<String>, notify: Boolean) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> Unit
            Purchase.PurchaseState.PENDING -> {
                if (notify) emit(BillingEvent.Pending)
                return
            }
            else -> return
        }
        if (!signatureAccepted(purchase)) {
            Log.w(TAG, "Purchase signature rejected for order ${purchase.orderId}")
            if (notify) emit(BillingEvent.Error("Purchase could not be verified"))
            return
        }
        val knownProducts = purchase.products.filter { it in Products.ALL }
        if (knownProducts.isEmpty()) return

        val consumables = knownProducts.filter { it in Products.CONSUMABLES }
        if (consumables.isNotEmpty()) {
            if (settings.markHintPurchaseCredited(purchase.purchaseToken)) {
                settings.creditHints(consumables.sumOf { Products.hintsGranted(it) })
            }
            val consumeResult = suspendCancellableCoroutine { cont ->
                client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { r, _ -> cont.resume(r) }
            }
            if (consumeResult.responseCode != BillingResponseCode.OK) {
                Log.w(TAG, "Consume failed: ${consumeResult.responseCode} ${consumeResult.debugMessage}")
            }
        }

        val nonConsumables = knownProducts.filter { it in Products.NON_CONSUMABLES }
        if (nonConsumables.isNotEmpty()) {
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                val ackResult = suspendCancellableCoroutine { cont -> client.acknowledgePurchase(ackParams) { cont.resume(it) } }
                if (ackResult.responseCode != BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${ackResult.responseCode} ${ackResult.debugMessage}")
                    if (notify) emit(BillingEvent.Error(ackResult.debugMessage.ifBlank { "acknowledge failed" }))
                    return
                }
            }
            owned.addAll(nonConsumables)
            if (Products.EVERYTHING in nonConsumables && !settings.everythingHintsGranted.first()) {
                settings.creditHints(Products.hintsGranted(Products.EVERYTHING))
                settings.setEverythingHintsGranted()
            }
        }
        if (notify) emit(BillingEvent.Purchased)
    }

    private fun signatureAccepted(purchase: Purchase): Boolean {
        val key = BuildConfig.PLAY_LICENSE_KEY
        if (key.isBlank()) {
            if (!BuildConfig.DEBUG) Log.w(TAG, "PLAY_LICENSE_KEY missing in release build; signature not verified")
            return true
        }
        return PurchaseVerifier.verify(key, purchase.originalJson, purchase.signature)
    }

    private fun emit(event: BillingEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val TAG = "BillingManager"
    }
}
