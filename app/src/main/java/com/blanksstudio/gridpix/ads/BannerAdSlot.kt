package com.blanksstudio.gridpix.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.blanksstudio.gridpix.BuildConfig
import com.blanksstudio.gridpix.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Home-only banner (decision D29) with a small "Remove ads" link to the Shop.
 * The caller decides whether to show it at all ([AdsManager.bannerAllowed]).
 */
@Composable
fun BannerAdSlot(onRemoveAds: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = onRemoveAds, modifier = Modifier.height(32.dp)) {
                Text(stringResource(R.string.ad_remove_ads), style = MaterialTheme.typography.labelMedium)
            }
            AdaptiveBanner()
        }
    }
}

@Composable
private fun AdaptiveBanner() {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        // Standard anchored adaptive size (50..90dp tall); the newer "large" variant can reach 150dp,
        // too much for Home. Deprecated in SDK 25 but still supported.
        @Suppress("DEPRECATION")
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(adSize) {
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, adView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }
        // Space is reserved at the ad's height so Home doesn't jump when the ad arrives.
        Box(Modifier.fillMaxWidth().height(adSize.height.dp), contentAlignment = Alignment.Center) {
            AndroidView(factory = { adView })
        }
    }
}
