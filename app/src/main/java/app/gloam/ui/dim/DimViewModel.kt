package app.gloam.ui.dim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.gloam.MainApplication
import app.gloam.data.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @param dimLevel 0–100, the user's single control.
 * @param running whether the user has asked for the shade. **The stored intent, not the live state
 *   of the service** — the service can be killed by the ROM without the user having changed their
 *   mind, and this is the value that survives that.
 */
data class DimUiState(
    val dimLevel: Int = 0,
    val running: Boolean = false,
)

class DimViewModel(
    private val preferences: AppPreferences,
) : ViewModel() {
    val state: StateFlow<DimUiState> =
        combine(preferences.dimLevel, preferences.shadeRunning) { level, running ->
            DimUiState(level, running)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DimUiState())

    /**
     * Written on every drag, and that is deliberate rather than careless.
     *
     * DataStore serialises its writes and the running service is collecting this same `Flow`, so
     * writing here is what makes the shade follow the slider live. Debouncing would buy a few file
     * writes at the cost of the one thing that makes the control feel connected to the screen.
     */
    fun setDimLevel(level: Int) {
        viewModelScope.launch { preferences.setDimLevel(level) }
    }

    /**
     * Records the intent. **Starting the service is the screen's job, not this one's** — a
     * `ViewModel` has no `Context` to start one with, and giving it one is how a `ViewModel` starts
     * outliving the thing it was scoped to.
     */
    fun setRunning(running: Boolean) {
        viewModelScope.launch { preferences.setShadeRunning(running) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    DimViewModel(app.preferences)
                }
            }
    }
}
