package com.example.sayvis.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide live status of the ambient listening avatar.
 *
 * The foreground service writes, the floating overlay and the in-app screen both
 * read. Being a plain singleton object keeps the service and the (Compose) UI
 * perfectly in sync without any binder plumbing, because SAYVIS runs everything in
 * one process.
 */
object ListenBus {

    enum class Mode { OFF, LISTENING, OWNER_VOICE }

    /** One "owner voice recognised" hit, kept as a small on-device history. */
    data class ListenEvent(val at: Long, val score: Float)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _mode = MutableStateFlow(Mode.OFF)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    /** Smoothed 0..1 ambient amplitude, for the avatar pulse and the level meter. */
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _lastOwnerAt = MutableStateFlow(0L)
    val lastOwnerAt: StateFlow<Long> = _lastOwnerAt.asStateFlow()

    private val _lastScore = MutableStateFlow(0f)
    val lastScore: StateFlow<Float> = _lastScore.asStateFlow()

    private val _history = MutableStateFlow<List<ListenEvent>>(emptyList())
    val history: StateFlow<List<ListenEvent>> = _history.asStateFlow()

    @Volatile
    private var ownerPulseGeneration = 0L

    fun setMode(next: Mode) {
        ownerPulseGeneration++
        _mode.value = next
    }

    fun publishLevel(raw: Float) {
        _level.value = raw.coerceIn(0f, 1f)
    }

    /** Marks an owner-voice hit; the green pulse fades back to listening after 5 s. */
    fun pushOwnerEvent(at: Long, score: Float) {
        _lastOwnerAt.value = at
        _lastScore.value = score
        _history.value = (listOf(ListenEvent(at, score)) + _history.value).take(12)
        val generation = ++ownerPulseGeneration
        _mode.value = Mode.OWNER_VOICE
        scope.launch {
            delay(OWNER_PULSE_MS)
            if (ownerPulseGeneration == generation && _mode.value == Mode.OWNER_VOICE) {
                _mode.value = Mode.LISTENING
            }
        }
    }

    fun ownerRecently(withinMs: Long = 60_000, now: Long = System.currentTimeMillis()): Boolean =
        now - _lastOwnerAt.value < withinMs

    private const val OWNER_PULSE_MS = 5_000L
}
