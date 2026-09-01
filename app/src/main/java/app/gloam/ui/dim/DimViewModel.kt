package app.gloam.ui.dim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.gloam.MainApplication
import app.gloam.data.AppPreferences
import app.gloam.shade.AutoOff
import app.gloam.shade.deadlineFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @param dimLevel 0–100, the one value the product is about.
 * @param warmth 0–100, how far the shade is tinted amber. Its *applied* strength is not this number
 *   — the ramp scales it by the headroom the dim level leaves — which is why the slider shows what
 *   was asked for rather than what the composite ended up with.
 * @param running whether the user has asked for the shade. **The stored intent, not the live state
 *   of the service** — the service can be killed by the ROM without the user having changed their
 *   mind, and this is the value that survives that.
 * @param lowerBacklight whether Gloam may take the backlight down before it draws the shade. Whether
 *   it *can* on this device is a different question, and not one a `ViewModel` can answer — it needs
 *   a `Context`, so the screen reads it.
 * @param autoOff how long a hand-started shade stays up. What was chosen, not what is left.
 * @param offAtMillis the instant the shade next comes down, or `null` for no deadline. Paired with
 *   [running] rather than derived from [autoOff]: the deadline was fixed when the shade started, and
 *   the choice can have moved since.
 */
data class DimUiState(
    val dimLevel: Int = 0,
    val warmth: Int = 0,
    val running: Boolean = false,
    val lowerBacklight: Boolean = true,
    val autoOff: AutoOff = AutoOff.Default,
    val offAtMillis: Long? = null,
)

class DimViewModel(
    private val preferences: AppPreferences,
) : ViewModel() {
    val state: StateFlow<DimUiState> =
        combine(
            preferences.dimLevel,
            preferences.warmth,
            preferences.shadeIntent,
            preferences.lowerBacklight,
            preferences.autoOff,
        ) { level, warmth, intent, lowerBacklight, autoOff ->
            DimUiState(
                dimLevel = level,
                warmth = warmth,
                running = intent.running,
                lowerBacklight = lowerBacklight,
                autoOff = autoOff,
                offAtMillis = intent.offAtMillis,
            )
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
     * Written on every drag like [setDimLevel], and for the same reason: the running service
     * collects this same `Flow`, so the tint follows the slider live.
     */
    fun setWarmth(warmth: Int) {
        viewModelScope.launch { preferences.setWarmth(warmth) }
    }

    /**
     * Records the intent **and the deadline it comes with**, in one write.
     *
     * **Starting the service is the screen's job, not this one's** — a `ViewModel` has no `Context`
     * to start one with, and giving it one is how a `ViewModel` starts outliving the thing it was
     * scoped to. The deadline is this one's, because it is arithmetic over a stored preference.
     *
     * The choice is read from the `Flow` rather than from [state]: `state` holds a default until its
     * first emission arrives, and the shade can be started from a cold launch before that lands.
     */
    fun beginShade() {
        viewModelScope.launch {
            val choice = preferences.autoOff.first()
            preferences.beginShade(deadlineFor(System.currentTimeMillis(), choice))
        }
    }

    /** The shade is down, by the button or because its deadline turned out to have passed. */
    fun endShade() {
        viewModelScope.launch { preferences.endShade() }
    }

    /**
     * The chip. **While the shade is up this also rewrites the deadline, from now rather than from
     * when the shade started** — which is the reading somebody expects from tapping "2 hours" while
     * looking at a screen, and it makes the control usable as "give me two more hours" without
     * inventing a second one.
     *
     * `beginShade` with `running` already `true` rather than a third setter: the pair exists so the
     * deadline is never written without the flag beside it.
     */
    fun setAutoOff(choice: AutoOff) {
        viewModelScope.launch {
            preferences.setAutoOff(choice)
            if (preferences.shadeIntent.first().running) {
                preferences.beginShade(deadlineFor(System.currentTimeMillis(), choice))
            }
        }
    }

    /**
     * Takes effect on a live shade without restarting it: the service collects the same `Flow`, so
     * switching this off releases the override and hands the user their own brightness slider back
     * while they are looking at it.
     */
    fun setLowerBacklight(enabled: Boolean) {
        viewModelScope.launch { preferences.setLowerBacklight(enabled) }
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
