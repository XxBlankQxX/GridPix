package com.blanksstudio.gridpix.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.blanksstudio.gridpix.data.rules.HintRules
import com.blanksstudio.gridpix.data.rules.StreakRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class GameSettings(
    val highlightMistakes: Boolean,
    val haptics: Boolean,
    val sound: Boolean,
    val theme: ThemeMode,
)

/** SPEC section 5 DataStore `settings`. Keys are stable strings; never rename without a migration. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // ---- Hints -------------------------------------------------------------------------------

    val hintWallet: Flow<HintRules.Wallet> = dataStore.data.map { it.wallet() }

    private fun Preferences.wallet() = HintRules.Wallet(
        freeHintsDate = this[KEY_FREE_HINTS_DATE] ?: "",
        freeHintsLeft = this[KEY_FREE_HINTS_LEFT] ?: HintRules.FREE_PER_DAY,
        balance = this[KEY_HINTS_BALANCE] ?: 0,
    )

    /** Spends one hint for [today]; false when none are available. */
    suspend fun spendHint(today: String): Boolean {
        var spent = false
        dataStore.edit { prefs ->
            val next = HintRules.spend(prefs.wallet(), today)
            if (next != null) {
                prefs.write(next)
                spent = true
            }
        }
        return spent
    }

    suspend fun creditHints(hints: Int) {
        dataStore.edit { prefs -> prefs.write(HintRules.credit(prefs.wallet(), hints)) }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(w: HintRules.Wallet) {
        this[KEY_FREE_HINTS_DATE] = w.freeHintsDate
        this[KEY_FREE_HINTS_LEFT] = w.freeHintsLeft
        this[KEY_HINTS_BALANCE] = w.balance
    }

    // ---- Daily streak ------------------------------------------------------------------------

    val streak: Flow<StreakRules.Streak> = dataStore.data.map {
        StreakRules.Streak(it[KEY_DAILY_STREAK] ?: 0, it[KEY_LAST_DAILY_DATE])
    }

    suspend fun onDailySolved(today: String, yesterday: String) {
        dataStore.edit { prefs ->
            val next = StreakRules.onDailySolved(
                StreakRules.Streak(prefs[KEY_DAILY_STREAK] ?: 0, prefs[KEY_LAST_DAILY_DATE]),
                today,
                yesterday,
            )
            prefs[KEY_DAILY_STREAK] = next.count
            prefs[KEY_LAST_DAILY_DATE] = next.lastDailyDate ?: ""
        }
    }

    // ---- Purchases (mirror of Play, re-verified at launch) -----------------------------------

    val ownedProducts: Flow<Set<String>> = dataStore.data.map { it[KEY_OWNED_PRODUCTS] ?: emptySet() }

    suspend fun setOwnedProducts(products: Set<String>) {
        dataStore.edit { it[KEY_OWNED_PRODUCTS] = products }
    }

    /** Purchase tokens already credited, so a consumable is never granted twice. */
    suspend fun markHintPurchaseCredited(purchaseToken: String): Boolean {
        var newlyCredited = false
        dataStore.edit { prefs ->
            val seen = prefs[KEY_CREDITED_TOKENS] ?: emptySet()
            if (purchaseToken !in seen) {
                prefs[KEY_CREDITED_TOKENS] = (seen.toList() + purchaseToken).takeLast(50).toSet()
                newlyCredited = true
            }
        }
        return newlyCredited
    }

    val everythingHintsGranted: Flow<Boolean> = dataStore.data.map { it[KEY_EVERYTHING_HINTS_GRANTED] ?: false }

    suspend fun setEverythingHintsGranted() {
        dataStore.edit { it[KEY_EVERYTHING_HINTS_GRANTED] = true }
    }

    // ---- Settings (SPEC S7) and small UI memory ----------------------------------------------

    val settings: Flow<GameSettings> = dataStore.data.map {
        GameSettings(
            highlightMistakes = it[KEY_HIGHLIGHT_MISTAKES] ?: false,
            haptics = it[KEY_HAPTICS] ?: true,
            sound = it[KEY_SOUND] ?: true,
            theme = it[KEY_THEME]?.let { name -> ThemeMode.entries.firstOrNull { t -> t.name == name } }
                ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun setHighlightMistakes(on: Boolean) = dataStore.edit { it[KEY_HIGHLIGHT_MISTAKES] = on }
    suspend fun setHaptics(on: Boolean) = dataStore.edit { it[KEY_HAPTICS] = on }
    suspend fun setSound(on: Boolean) = dataStore.edit { it[KEY_SOUND] = on }
    suspend fun setTheme(theme: ThemeMode) = dataStore.edit { it[KEY_THEME] = theme.name }

    /** Endless size the Play button remembers (SPEC S1). */
    val lastSize: Flow<Int> = dataStore.data.map { it[KEY_LAST_SIZE] ?: 10 }
    suspend fun setLastSize(size: Int) = dataStore.edit { it[KEY_LAST_SIZE] = size }

    val tutorialDone: Flow<Boolean> = dataStore.data.map { it[KEY_TUTORIAL_DONE] ?: false }
    suspend fun setTutorialDone() = dataStore.edit { it[KEY_TUTORIAL_DONE] = true }

    /** Shown once when free hints run out (SPEC section 6: one soft prompt). */
    val hintPromptShown: Flow<Boolean> = dataStore.data.map { it[KEY_HINT_PROMPT_SHOWN] ?: false }
    suspend fun setHintPromptShown() = dataStore.edit { it[KEY_HINT_PROMPT_SHOWN] = true }

    suspend fun snapshotOwned(): Set<String> = ownedProducts.first()

    private companion object {
        val KEY_HINTS_BALANCE = intPreferencesKey("hints_balance")
        val KEY_FREE_HINTS_DATE = stringPreferencesKey("free_hints_date")
        val KEY_FREE_HINTS_LEFT = intPreferencesKey("free_hints_left")
        val KEY_DAILY_STREAK = intPreferencesKey("daily_streak")
        val KEY_LAST_DAILY_DATE = stringPreferencesKey("last_daily_date")
        val KEY_OWNED_PRODUCTS = stringSetPreferencesKey("owned_products")
        val KEY_CREDITED_TOKENS = stringSetPreferencesKey("credited_purchase_tokens")
        val KEY_EVERYTHING_HINTS_GRANTED = booleanPreferencesKey("everything_hints_granted")
        val KEY_HIGHLIGHT_MISTAKES = booleanPreferencesKey("highlight_mistakes")
        val KEY_HAPTICS = booleanPreferencesKey("haptics")
        val KEY_SOUND = booleanPreferencesKey("sound")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LAST_SIZE = intPreferencesKey("last_size")
        val KEY_TUTORIAL_DONE = booleanPreferencesKey("tutorial_done")
        val KEY_HINT_PROMPT_SHOWN = booleanPreferencesKey("hint_prompt_shown")
    }
}
