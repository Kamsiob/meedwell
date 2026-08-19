package com.kamsiob.meedwell.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What is downloading, readable from anywhere.
 *
 * The queue used to live inside the coordinator, which lives inside the view
 * model, which lives as long as the screen does. So downloads stopped the moment
 * Android decided the app was no longer worth keeping around, and a pack you had
 * set going before locking your phone was simply not there later.
 *
 * The work moved into a foreground service. This object is what the two halves
 * talk through: the service writes, the interface reads, and neither holds a
 * reference to the other. A plain object rather than a bound service because the
 * only thing the screen needs is a few numbers, and binding for that would be
 * ceremony around a value.
 */
object SurroundingsDownloads {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    data class State(
        /** Waiting, in order, not counting the one in hand. */
        val queued: List<String> = emptyList(),
        /** The recording being fetched right now, or null. */
        val workingOn: String? = null,
        /** How far through that one is, 0 through 1. */
        val progress: Float = 0f,
        /** Why a recording failed, by id, so a row can say so. */
        val failures: Map<String, String> = emptyMap(),
    ) {
        val busy: Boolean get() = workingOn != null || queued.isNotEmpty()
        val remaining: Int get() = queued.size + if (workingOn != null) 1 else 0
    }

    internal fun set(update: (State) -> State) {
        _state.value = update(_state.value)
    }

    /** Cleared when a recording is removed, so a stale failure never sticks. */
    fun forget(id: String) = set { it.copy(failures = it.failures - id) }
}
