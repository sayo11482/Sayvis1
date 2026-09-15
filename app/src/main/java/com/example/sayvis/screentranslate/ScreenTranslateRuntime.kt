package com.example.sayvis.screentranslate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth shared by the foreground service, the overlay renderer and the
 * Compose interface.
 *
 * The service owns the screen-capture session; the app is only a *window* onto it. Keeping
 * the state here (instead of inside the service or the view model) is what lets the owner
 * open the translator screen and watch live counters while the overlay is painting over
 * another app — and it means the feature survives the activity being destroyed.
 */
object ScreenTranslateRuntime {

    private const val MAX_RECENT = 14

    private val _status = MutableStateFlow(ScreenTranslateStatus())
    val status: StateFlow<ScreenTranslateStatus> = _status.asStateFlow()

    private val _stats = MutableStateFlow(ScreenTranslationStats())
    val stats: StateFlow<ScreenTranslationStats> = _stats.asStateFlow()

    /** Live counters plus the phase, for the status cards. */
    fun update(transform: (ScreenTranslateStatus) -> ScreenTranslateStatus) {
        _status.value = transform(_status.value)
    }

    fun setPhase(phase: ScreenTranslatePhase) {
        update { current ->
            val active = phase == ScreenTranslatePhase.RUNNING ||
                phase == ScreenTranslatePhase.PAUSED ||
                phase == ScreenTranslatePhase.STARTING
            current.copy(
                phase = phase,
                runningSince = when {
                    !active -> 0L
                    current.runningSince > 0L -> current.runningSince
                    else -> System.currentTimeMillis()
                }
            )
        }
    }

    fun permissionsChanged(overlayGranted: Boolean, captureConsentGranted: Boolean) {
        update {
            it.copy(
                overlayPermissionGranted = overlayGranted,
                captureConsentGranted = captureConsentGranted
            )
        }
    }

    fun enginesChanged(engineOnline: Boolean, dictionaryOnly: Boolean) {
        update { it.copy(engineOnline = engineOnline, dictionaryOnly = dictionaryOnly) }
    }

    fun fail(messageFa: String, messageEn: String) {
        update {
            it.copy(
                phase = ScreenTranslatePhase.ERROR,
                lastErrorFa = messageFa,
                lastErrorEn = messageEn
            )
        }
    }

    fun clearError() {
        update { it.copy(lastErrorFa = null, lastErrorEn = null) }
    }

    fun framesCaptured() {
        update { it.copy(framesCaptured = it.framesCaptured + 1) }
    }

    fun framesSkipped() {
        update { it.copy(framesSkipped = it.framesSkipped + 1) }
    }

    fun ocrCompleted(millis: Long) {
        update { it.copy(lastOcrMillis = millis) }
    }

    /** Publishes a finished plan: counters, live preview list and the overlay content. */
    fun publishPlan(plan: FrameTranslationPlan) {
        _stats.value = _stats.value.plus(plan)
        val recent = plan.regions.map { region ->
            TranslatedSegment(region.sourceText, region.persianText, region.provenance)
        }
        update { current ->
            current.copy(
                lastPlan = plan,
                frameFingerprints = current.frameFingerprints + 1,
                segmentsTranslated = current.segmentsTranslated + plan.regions.size,
                segmentsSkipped = current.segmentsSkipped + plan.skippedFragments,
                lastPlanEmpty = plan.regions.isEmpty(),
                secureContentSuspected = plan.secureContentSuspected,
                recentSegments = (recent.reversed() + current.recentSegments).take(MAX_RECENT)
            )
        }
    }

    /** Clears the overlay content but keeps permissions and counters. */
    fun clearPlan() {
        update { it.copy(lastPlan = null) }
    }

    fun reset() {
        _status.value = ScreenTranslateStatus(
            overlayPermissionGranted = _status.value.overlayPermissionGranted,
            captureConsentGranted = false
        )
        _stats.value = ScreenTranslationStats()
    }
}
