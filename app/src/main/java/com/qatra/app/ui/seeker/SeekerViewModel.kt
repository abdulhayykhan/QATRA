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

    // ── Actions ─────────────────────────────────────────────────────────────

    fun submitSlipAndVerify(context: Context) {
        val imageUri = slipImageUri.value ?: run {
            slipVerificationError.value = "Please select a hospital slip image first."
            return
        }
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
            }
        }
    }

    fun seekerDirectCallToDonor(donorPhone: String) {
        // Intent to dial donorPhone (tel:+XXXX) — gated to seeker role only.
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
