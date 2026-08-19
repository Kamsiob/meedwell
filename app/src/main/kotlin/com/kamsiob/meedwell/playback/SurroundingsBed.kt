package com.kamsiob.meedwell.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the bed is doing, in one place both a screen and a service can read.
 *
 * **The bed needed an owner that outlives a screen.** Its state used to live only
 * in the coordinator, which lives in a view model, which is gone the moment the
 * activity finishes. So the notification had nothing to read, and anything the
 * notification did had nobody to tell.
 *
 * This holds the fact of what is playing. `SurroundingsService` reads it to draw
 * the notification, and writes to it when somebody taps pause in the shade; the
 * coordinator mirrors it into the interface. Whoever changes the sound updates
 * this, so there is one answer to "what is underneath" rather than two that can
 * disagree.
 */
object SurroundingsBed {

    data class State(
        val id: String? = null,
        val title: String = "",
        val playing: Boolean = false,
    ) {
        /** A bed exists when something is loaded, whether or not it is running. */
        val present: Boolean get() = id != null
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun set(id: String?, title: String, playing: Boolean) {
        _state.value = State(id = id, title = title, playing = playing)
    }

    fun setPlaying(playing: Boolean) {
        _state.value = _state.value.copy(playing = playing)
    }

    fun clear() {
        _state.value = State()
    }
}
