package app.gloam.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.gloam.MainApplication
import app.gloam.data.AppPreferences
import app.gloam.data.DEFAULT_SCHEDULE_OFF_MINUTES
import app.gloam.data.DEFAULT_SCHEDULE_ON_MINUTES
import app.gloam.shade.Schedule
import app.gloam.shade.timeOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * The schedule screen's state, which **is** [Schedule] rather than a data class wrapping it.
 *
 * Every other screen here declares its own `…UiState` because it draws several unrelated
 * preferences at once; this one draws exactly the three values that already travel together as one
 * type, and a wrapper would be a second name for one thing — with a `copy()` in the middle of it
 * that could disagree. What the screen knows and the `Schedule` does not — whether the battery
 * exemption is missing, whether this phone has an autostart screen — are live `Context` reads that a
 * `ViewModel` cannot make at all (`CLAUDE.md`: a `ViewModel` never holds a `Context`), so they are
 * the screen's and are re-read on resume rather than held here.
 *
 * **Nothing here arms an alarm.** Writing the preference is the whole of it: `MainApplication`
 * collects `schedule` for the life of the process and arms from there (`docs/phase-4.md` §6). An
 * alarm whose existence depends on somebody looking at a screen is not an alarm.
 */
class ScheduleViewModel(
    private val preferences: AppPreferences,
) : ViewModel() {
    val state: StateFlow<Schedule> =
        preferences.schedule.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            // The same defaults the preference reads with, so the first frame shows what the second
            // one will rather than an off schedule at midnight that redraws a moment later.
            Schedule(
                enabled = false,
                onAt = timeOf(DEFAULT_SCHEDULE_ON_MINUTES),
                offAt = timeOf(DEFAULT_SCHEDULE_OFF_MINUTES),
            ),
        )

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setScheduleEnabled(enabled) }
    }

    /**
     * Both edges, always, because the three shapes a window can have are properties of the *pair*.
     *
     * The screen picks one time at a time and passes the other one back unchanged — which is what
     * keeps a half-written window, briefly degenerate or inverted, from ever reaching the collector
     * that arms the alarm from it.
     */
    fun setWindow(
        onAt: LocalTime,
        offAt: LocalTime,
    ) {
        viewModelScope.launch { preferences.setScheduleWindow(onAt, offAt) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    ScheduleViewModel(app.preferences)
                }
            }
    }
}
