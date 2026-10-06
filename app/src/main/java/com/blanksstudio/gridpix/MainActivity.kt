package com.blanksstudio.gridpix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.data.settings.ThemeMode
import com.blanksstudio.gridpix.ui.navigation.GridPixNavHost
import com.blanksstudio.gridpix.ui.theme.GridPixTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.activity.viewModels
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val theme: StateFlow<ThemeMode> = settings.settings.map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by themeViewModel.theme.collectAsStateWithLifecycle()
            GridPixTheme(mode = theme) {
                GridPixNavHost()
            }
        }
    }
}
