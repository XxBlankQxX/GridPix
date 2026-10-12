package com.blanksstudio.gridpix.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.blanksstudio.gridpix.BuildConfig
import com.blanksstudio.gridpix.billing.Entitlements
import com.blanksstudio.gridpix.data.rules.AdRules
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.util.LocalDates
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All ads go through here (decision D29, "fair ads"):
 * - Google's consent form first (EU/UK), then the Mobile Ads SDK. Nothing loads before consent allows it.
 * - Banner on Home only, between-puzzle ad after every 3rd solve and at most every 4 minutes,
 *   no ads at all on the first day, none for `remove_ads` / `everything` owners.
 * - Optional "watch a video for a hint", at most [AdRules.REWARDED_PER_DAY] a day.
 * The rules themselves are in [AdRules] (unit-tested); this class only applies them.
 */
@Singleton
class AdsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val initialising = AtomicBoolean(false)
    private val showingFullScreen = AtomicBoolean(false)

    private val _adsReady = MutableStateFlow(false)
    /** True once consent allows ads and the SDK has started. */
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** Google requires a "privacy options" entry in Settings for players in the EU/UK. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var rewarded: RewardedAd? = null
    private var loadingRewarded = false
    private val rewardedLoaded = MutableStateFlow(false)

    /** Home banner: SDK ready, first day over, ads not bought off. */
    val bannerAllowed: Flow<Boolean> =
        combine(adsReady, settings.ownedProducts, settings.firstLaunchAt) { ready, owned, firstLaunch ->
            ready && !Entitlements.adsRemoved(owned) && graceOver(firstLaunch, System.currentTimeMillis())
        }

    /** The "watch a video for a hint" offer: a video is loaded and today's limit isn't used up. */
    fun rewardedAvailable(today: String): Flow<Boolean> =
        combine(rewardedLoaded, settings.rewardedLeft(today)) { loaded, left -> loaded && left > 0 }

    /** Call from MainActivity.onCreate. Errors never block the game; it just runs without ads. */
    fun gatherConsentAndInitialise(activity: Activity) {
        scope.launch { settings.recordFirstLaunchIfMissing(System.currentTimeMillis()) }

        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG && BuildConfig.ADS_DEBUG_EEA) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .build(),
                )
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consent form: ${formError.errorCode} ${formError.message}")
                    onConsentFlowFinished()
                }
            },
            { error ->
                Log.w(TAG, "Consent info update failed: ${error.errorCode} ${error.message}")
                onConsentFlowFinished()
            },
        )
        // Consent given on an earlier launch already allows ads; don't wait for the network.
        if (consentInformation.canRequestAds()) initialiseMobileAds()
    }

    private fun onConsentFlowFinished() {
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInformation.canRequestAds()) initialiseMobileAds()
    }

    private fun initialiseMobileAds() {
        if (!initialising.compareAndSet(false, true)) return
        // GridPix is rated Everyone and played by families: general-audience ads only.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {
                scope.launch {
                    _adsReady.value = true
                    loadInterstitial()
                    loadRewarded()
                }
            }
        }
    }

    /** Settings > Ad privacy options. */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Privacy options form: ${formError.errorCode} ${formError.message}")
        }
    }

    // ---- Between-puzzle ad -------------------------------------------------------------------

    /** Counts a fresh solve (not the tutorial) towards the next between-puzzle ad. */
    fun onPuzzleSolved() {
        scope.launch { settings.countSolveForAds() }
    }

    /**
     * Called when the player leaves the solved screen (Next / Home). Shows the between-puzzle ad
     * when one is due, then runs [then]; when none is due [then] runs straight away. A second tap
     * while the ad is opening is ignored, so the next screen can't open underneath the ad.
     */
    fun afterSolvedScreen(activity: Activity, then: () -> Unit) {
        if (!showingFullScreen.compareAndSet(false, true)) return
        scope.launch {
            val now = System.currentTimeMillis()
            val state = settings.adState()
            val allowed = !Entitlements.adsRemoved(settings.snapshotOwned()) &&
                graceOver(state.firstLaunchAt, now) &&
                (debugFast && state.solvesSinceInterstitial > 0 || AdRules.interstitialDue(state.solvesSinceInterstitial, state.lastInterstitialAt, now))
            val ad = interstitial
            if (!allowed || ad == null) {
                showingFullScreen.set(false)
                if (allowed) loadInterstitial()
                then()
                return@launch
            }
            interstitial = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    scope.launch { settings.markInterstitialShown(now) }
                }

                override fun onAdDismissedFullScreenContent() = finish()

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Interstitial failed to show: ${error.code} ${error.message}")
                    finish()
                }

                private fun finish() {
                    showingFullScreen.set(false)
                    loadInterstitial()
                    then()
                }
            }
            ad.show(activity)
        }
    }

    private fun loadInterstitial() {
        if (!_adsReady.value || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        scope.launch {
            // Owners never see one, so don't even request it.
            if (Entitlements.adsRemoved(settings.snapshotOwned())) {
                loadingInterstitial = false
                return@launch
            }
            InterstitialAd.load(
                context,
                BuildConfig.ADMOB_INTERSTITIAL_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitial = ad
                        loadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Interstitial failed to load: ${error.code} ${error.message}")
                        loadingInterstitial = false
                    }
                },
            )
        }
    }

    // ---- Watch a video for a hint -------------------------------------------------------------

    /**
     * Shows the video. [onResult] gets true when the player watched it to the end and the hint was
     * credited (false when closed early, or today's limit was reached in the meantime).
     */
    fun showRewardedHint(activity: Activity, onResult: (Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null || !showingFullScreen.compareAndSet(false, true)) {
            onResult(false)
            return
        }
        rewarded = null
        rewardedLoaded.value = false
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded failed to show: ${error.code} ${error.message}")
                finish()
            }

            private fun finish() {
                showingFullScreen.set(false)
                loadRewarded()
                scope.launch { onResult(earned && settings.creditRewardedHint(LocalDates.today())) }
            }
        }
        ad.show(activity) { earned = true }
    }

    private fun loadRewarded() {
        if (!_adsReady.value || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    rewardedLoaded.value = true
                    loadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed to load: ${error.code} ${error.message}")
                    loadingRewarded = false
                }
            },
        )
    }

    private fun graceOver(firstLaunchAt: Long?, now: Long): Boolean =
        debugFast || AdRules.graceOver(firstLaunchAt, now)

    private val debugFast = BuildConfig.DEBUG && BuildConfig.ADS_DEBUG_FAST

    private companion object {
        const val TAG = "AdsManager"
    }
}
