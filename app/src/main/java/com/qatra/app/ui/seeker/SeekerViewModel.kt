package com.qatra.app.ui.seeker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qatra.app.data.model.*
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.ui.SeekerScreenStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Seeker-specific ViewModel managing:
 * - Screen step navigation (SeekerScreenStep)
 * - Seeker profile state
 * - Emergency request creation form
 * - Hospital slip upload & OCR verification
 * - Feedback & proxy call state
 */
class SeekerViewModel(
    val repository: QatraRepository
) : ViewModel() {

    // ── Step Navigation ──────────────────────────────────────────────────────
    private val _seekerStep = MutableStateFlow(SeekerScreenStep.SPLASH)
    val seekerStep: StateFlow<SeekerScreenStep> = _seekerStep.asStateFlow()

    fun setStep(step: SeekerScreenStep) {
        _seekerStep.value = step
    }

    // ── Session Expiry (forwarded from repository) ──────────────────────────
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(replay = 0)
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    // ── Seeker Profile State ────────────────────────────────────────────────
    val seekerName = MutableStateFlow("Jane Doe")
    val seekerAge = MutableStateFlow("30")
    val seekerGender = MutableStateFlow("F") // M, F, O
    val seekerDistrict = MutableStateFlow("Karachi South")
    val seekerCnic = MutableStateFlow("42101-1234567-1")

    // ── Emergency Request Form State ────────────────────────────────────────
    val selectedBloodGroup = MutableStateFlow(BloodGroup.O_NEG)
    val selectedComponent = MutableStateFlow(BloodComponent.PRBC)
    val unitsRequired = MutableStateFlow(2)
    val selectedHospital = MutableStateFlow(repository.hospitals[0])
    val selectedUrgency = MutableStateFlow(UrgencyLevel.HIGH_PRIORITY)

    // ── Slip Upload & Verification ──────────────────────────────────────────
    val slipImageUri = MutableStateFlow<Uri?>(null)
    val slipImageSelected: StateFlow<Boolean> = slipImageUri.map { it != null }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false
    )
    val isOcrProcessing = MutableStateFlow(false)
    val ocrStep1Completed = MutableStateFlow(false)
    val ocrStep2Completed = MutableStateFlow(false)
    val slipVerificationError = MutableStateFlow<String?>(null)
    val slipRoutedToVerification = MutableStateFlow(false)

    // ── Feedback State ──────────────────────────────────────────────────────
    val feedbackRating = MutableStateFlow(5)
    val feedbackNote = MutableStateFlow("")

    // ── Proxy Call State (MaskedCallingScreen) ──────────────────────────────
    val isProxyCallActive = MutableStateFlow(false)
    val proxyCallSecondsRemaining = MutableStateFlow(0)
    private var proxyCallJob: Job? = null

    // ── Dial Event (emitted when seeker wants to directly call a donor) ─────
    private val _dialEvent = MutableSharedFlow<String>(replay = 0)
    val dialEvent = _dialEvent.asSharedFlow()

    init {
        // Forward session-expired events from the seeker repository
        viewModelScope.launch {
            repository.seekerRepository.sessionExpiredEvent.collect {
                _sessionExpiredEvent.emit(Unit)
            }
        }
    }

    fun startProxyCallCountdown() {
        proxyCallJob?.cancel()
        isProxyCallActive.value = true
        proxyCallSecondsRemaining.value = 300
        proxyCallJob = viewModelScope.launch {
            while (proxyCallSecondsRemaining.value > 0) {
                delay(1000)
                proxyCallSecondsRemaining.value -= 1
            }
            isProxyCallActive.value = false
        }
    }

    fun endProxyCall() {
        proxyCallJob?.cancel()
        isProxyCallActive.value = false
        proxyCallSecondsRemaining.value = 0
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    fun submitSlipAndVerify(context: Context) {
        val imageUri = slipImageUri.value ?: run {
            slipVerificationError.value = "Please select a hospital slip image first."
            return
        }
        val previousStep = _seekerStep.value
        _seekerStep.value = SeekerScreenStep.VERIFICATION_MODAL
        isOcrProcessing.value = true
        ocrStep1Completed.value = false
        ocrStep2Completed.value = false
        slipVerificationError.value = null
        slipRoutedToVerification.value = false

        viewModelScope.launch {
            ocrStep1Completed.value = true
            val result = repository.processHospitalSlipOcr(
                context = context,
                imageUri = imageUri,
                bloodGroup = selectedBloodGroup.value,
                fallbackUnits = unitsRequired.value,
                fallbackHospital = selectedHospital.value,
                urgency = selectedUrgency.value
            )
            ocrStep2Completed.value = result.request != null || result.routedToVerification
            isOcrProcessing.value = false
            slipVerificationError.value = result.errorMessage
            slipRoutedToVerification.value = result.routedToVerification
            if (result.request != null) {
                repository.dispatchProximityAlerts(result.request.id, radiusKm = 10)
                delay(800)
                _seekerStep.value = SeekerScreenStep.LIVE_STATUS_FEED
            } else if (result.errorMessage != null && !result.routedToVerification) {
                // OCR/upload failed completely — rollback to previous step
                _seekerStep.value = previousStep
                Timber.w("Slip processing failed, rolled back seeker step to %s", previousStep)
            }
            // If routedToVerification is true, stay on VERIFICATION_MODAL — admin review needed
        }
    }

    fun seekerDirectCallToDonor(donorPhone: String) {
        // Emit dial event so the UI layer can launch the phone dialer intent.
        viewModelScope.launch {
            _dialEvent.emit(donorPhone)
        }
        _seekerStep.value = SeekerScreenStep.CONFIRMATION
    }

    fun submitSeekerFeedback() {
        // Feedback lives in VM state — no backend sink for seeker ratings yet.
        setStep(SeekerScreenStep.SPLASH)
    }

    override fun onCleared() {
        proxyCallJob?.cancel()
        super.onCleared()
    }
}
