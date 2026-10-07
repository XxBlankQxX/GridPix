package com.blanksstudio.gridpix.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Context
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.blanksstudio.gridpix.notifications.DailyReminder
import com.blanksstudio.gridpix.ui.common.rememberEnableReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.BuildConfig
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.data.settings.GameSettings
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.data.settings.ThemeMode
import com.blanksstudio.gridpix.ui.common.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Hosted from the repo's docs/ folder via GitHub Pages (see docs/privacy.html). */
const val PRIVACY_POLICY_URL = "https://xxblankqxx.github.io/GridPix/privacy.html"

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    val state: StateFlow<GameSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun setAutoCross(on: Boolean) = viewModelScope.launch { settings.setAutoCross(on) }
    fun setShareEnabled(on: Boolean) = viewModelScope.launch { settings.setShareEnabled(on) }
    fun setReminderEnabled(on: Boolean) = viewModelScope.launch {
        settings.setReminderEnabled(on)
        DailyReminder.sync(appContext, on, settings.settings.first().reminderHour)
    }
    fun setReminderHour(hour: Int) = viewModelScope.launch {
        settings.setReminderHour(hour)
        val current = settings.settings.first()
        DailyReminder.sync(appContext, current.reminderEnabled, hour)
    }
    fun setHighlightMistakes(on: Boolean) = viewModelScope.launch { settings.setHighlightMistakes(on) }
    fun setHaptics(on: Boolean) = viewModelScope.launch { settings.setHaptics(on) }
    fun setSound(on: Boolean) = viewModelScope.launch { settings.setSound(on) }
    fun setTheme(theme: ThemeMode) = viewModelScope.launch { settings.setTheme(theme) }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReplayTutorial: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deniedText = stringResource(R.string.reminder_denied)
    val enableReminder = rememberEnableReminder { granted ->
        viewModel.setReminderEnabled(granted)
        if (!granted) scope.launch { snackbar.showSnackbar(deniedText) }
    }
    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack, snackbarHostState = snackbar) { padding ->
        val s = settings ?: return@ScreenScaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_highlight_mistakes)) },
                supportingContent = { Text(stringResource(R.string.settings_highlight_mistakes_desc)) },
                trailingContent = { Switch(checked = s.highlightMistakes, onCheckedChange = viewModel::setHighlightMistakes) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_auto_cross)) },
                supportingContent = { Text(stringResource(R.string.settings_auto_cross_desc)) },
                trailingContent = { Switch(checked = s.autoCross, onCheckedChange = viewModel::setAutoCross) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reminder)) },
                supportingContent = { Text(stringResource(R.string.settings_reminder_desc)) },
                trailingContent = {
                    Switch(checked = s.reminderEnabled, onCheckedChange = { on -> if (on) enableReminder() else viewModel.setReminderEnabled(false) })
                },
            )
            if (s.reminderEnabled) {
                Text(
                    stringResource(R.string.settings_reminder_time),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8, 12, 18, 20).forEach { hour ->
                        FilterChip(
                            selected = s.reminderHour == hour,
                            onClick = { viewModel.setReminderHour(hour) },
                            label = { Text(stringResource(R.string.settings_reminder_hour, hour)) },
                        )
                    }
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_share)) },
                supportingContent = { Text(stringResource(R.string.settings_share_desc)) },
                trailingContent = { Switch(checked = s.shareEnabled, onCheckedChange = viewModel::setShareEnabled) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_haptics)) },
                trailingContent = { Switch(checked = s.haptics, onCheckedChange = viewModel::setHaptics) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_sound)) },
                trailingContent = { Switch(checked = s.sound, onCheckedChange = viewModel::setSound) },
            )
            HorizontalDivider()
            Text(
                stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
            val options = listOf(
                ThemeMode.SYSTEM to R.string.settings_theme_system,
                ThemeMode.LIGHT to R.string.settings_theme_light,
                ThemeMode.DARK to R.string.settings_theme_dark,
            )
            options.forEach { (mode, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTheme(mode) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(selected = s.theme == mode, onClick = { viewModel.setTheme(mode) })
                    Text(stringResource(label))
                }
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_replay_tutorial)) },
                modifier = Modifier.clickable(onClick = onReplayTutorial),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy)) },
                supportingContent = { Text(PRIVACY_POLICY_URL) },
                modifier = Modifier.clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())) }
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = { Text(stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME)) },
            )
        }
    }
}
