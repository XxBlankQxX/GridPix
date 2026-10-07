package com.blanksstudio.gridpix.ui.shop

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.billing.BillingAvailability
import com.blanksstudio.gridpix.billing.BillingEvent
import com.blanksstudio.gridpix.billing.BillingManager
import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.billing.Products
import com.blanksstudio.gridpix.data.rules.HintRules
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.ui.common.ScreenScaffold
import com.blanksstudio.gridpix.util.LocalDates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopItem(val productId: Int, val titleRes: Int, val descRes: Int, val id: String, val price: String?, val owned: Boolean)

data class ShopUiState(
    val items: List<ShopItem> = emptyList(),
    val available: Boolean = false,
    val hints: Int = 0,
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    val billing: BillingManager,
    settings: SettingsRepository,
) : ViewModel() {

    private val catalogue = listOf(
        Triple(Products.HINTS_10, R.string.shop_hints_10, R.string.shop_hints_desc),
        Triple(Products.HINTS_50, R.string.shop_hints_50, R.string.shop_hints_desc),
        Triple(Products.PACK_ANIME, R.string.shop_pack_anime, R.string.shop_pack_anime_desc),
        Triple(Products.PACK_ANIMALS, R.string.shop_pack_animals, R.string.shop_pack_desc),
        Triple(Products.PACK_VEHICLES, R.string.shop_pack_vehicles, R.string.shop_pack_desc),
        Triple(Products.PACK_FOOD, R.string.shop_pack_food, R.string.shop_pack_desc),
        Triple(Products.PACK_NATURE, R.string.shop_pack_nature, R.string.shop_pack_desc),
        Triple(Products.LARGE_GRIDS, R.string.shop_large_grids, R.string.shop_large_grids_desc),
        Triple(Products.EVERYTHING, R.string.shop_everything, R.string.shop_everything_desc),
    )

    val uiState: StateFlow<ShopUiState> = combine(
        billing.prices,
        billing.availability,
        settings.ownedProducts,
        settings.hintWallet,
    ) { prices, availability, owned, wallet ->
        val effective = Entitlements.effectivelyOwned(owned)
        ShopUiState(
            items = catalogue.mapIndexed { i, (id, title, desc) ->
                ShopItem(i, title, desc, id, prices[id], owned = id in effective)
            },
            available = availability is BillingAvailability.Available,
            hints = HintRules.available(wallet, LocalDates.today()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopUiState())

    init {
        billing.refreshProducts()
    }

    fun buy(activity: Activity, productId: String) = billing.launchPurchase(activity, productId)

    fun restore() = viewModelScope.launch { billing.restorePurchases(silent = false) }
}

@Composable
fun ShopScreen(onBack: () -> Unit, viewModel: ShopViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.billing.events.collect { event ->
            val message = when (event) {
                BillingEvent.Purchased -> context.getString(R.string.shop_event_purchased)
                BillingEvent.Pending -> context.getString(R.string.shop_event_pending)
                BillingEvent.Cancelled -> context.getString(R.string.shop_event_cancelled)
                is BillingEvent.Restored -> if (event.anyOwned) context.getString(R.string.shop_event_restored) else context.getString(R.string.shop_event_restored_none)
                BillingEvent.StoreUnavailable -> context.getString(R.string.shop_unavailable)
                is BillingEvent.Error -> context.getString(R.string.shop_event_error, event.message)
            }
            snackbar.showSnackbar(message)
        }
    }

    ScreenScaffold(title = stringResource(R.string.shop_title), onBack = onBack, snackbarHostState = snackbar) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.shop_intro), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.shop_hints_balance, state.hints), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!state.available) {
                    Text(stringResource(R.string.shop_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
            items(state.items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(item.titleRes), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(item.descRes), style = MaterialTheme.typography.bodySmall)
                        }
                        if (item.owned) {
                            Text(stringResource(R.string.owned), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        } else {
                            val activity = context as? Activity
                            Button(
                                onClick = { activity?.let { viewModel.buy(it, item.id) } },
                                enabled = state.available && item.price != null && activity != null,
                            ) { Text(item.price ?: stringResource(R.string.shop_price_loading)) }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = viewModel::restore, modifier = Modifier.fillMaxWidth(), enabled = state.available) {
                    Text(stringResource(R.string.shop_restore))
                }
            }
        }
    }
}
