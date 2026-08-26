package com.qatra.app.ui

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qatra.app.data.model.*
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.notifications.GeoAlertPayload
import com.qatra.app.ui.admin.AdminViewModel
import com.qatra.app.ui.donor.DonorViewModel
import com.qatra.app.ui.seeker.SeekerViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

private const val PREFS_NAME = "qatra_consent_prefs"
private const val KEY_TERMS_ACCEPTED = "has_accepted_terms"

// ── Enums (shared across all screens) ────────────────────────────────────────

enum class MainTab { EMERGENCY, DONATE, LEARN, DESK }

enum class SeekerScreenStep {
    SPLASH, PHONE_VERIFICATION, ROLE_PROFILE, REQUEST_CREATION, SLIP_UPLOAD,
    VERIFICATION_MODAL, LIVE_STATUS_FEED, MATCHED_DONORS,
    MASKED_CALL, DIRECT_CALL, CONFIRMATION
}

enum class DonorScreenStep {
    CNIC_UPLOAD, PRE_SCREENING, HOME_DASHBOARD, INTERACTIVE_MAP,
    NAVIGATION_ROUTING, DONATION_COMPLETE, COOLDOWN_STATE
}

enum class AdminScreenStep { LOGIN_2FA, VERIFICATION_QUEUE, FRAUD_AUDIT, DRIVE_MANAGEMENT }

enum class FlowType { SEEKER, DONOR, ADMIN, MAIN_SHELL }

// ── Facade ViewModel ─────────────────────────────────────────────────────────

/**
 * Coordination facade that delegates to role-specific ViewModels:
 * - [authVm]  → shared auth / OTP / active flow
 * - [seekerVm]→ seeker profile, request creation, slip OCR, feedback
 * - [donorVm] → CNIC upload, geo-alerts, availability, donation completion
 * - [adminVm] → admin login, verification queue, fraud audit, drive mgmt
 *
 * All screen composables still receive `viewModel: QatraViewModel`; this
 * class forwards property access and method calls to the appropriate sub-VM.
 */
class QatraViewModel(
    val authVm: SharedAuthViewModel,
    val seekerVm: SeekerViewModel,
    val donorVm: DonorViewModel,
    val adminVm: AdminViewModel
) : ViewModel() {

    /** Shared repository reference (convenience for screens that read `viewModel.repository.X`). */
    val repository: QatraRepository = authVm.repository

    // ── Session Expiry (aggregated from all sub-ViewModels) ─────────────────
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(replay = 0)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    init {
        // Forward session-expired events from seeker and donor sub-ViewModels
        viewModelScope.launch {
            seekerVm.sessionExpiredEvent.collect {
                Timber.w("Session expired event received from seeker flow")
                handleSessionExpired()
            }
        }
        viewModelScope.launch {
            donorVm.sessionExpiredEvent.collect {
                Timber.w("Session expired event received from donor flow")
                handleSessionExpired()
            }
        }
    }

    /**
     * Called when a Supabase call fails with an auth error (401/403) indicating
     * the session has expired and the SDK's internal token refresh also failed.
     * Resets navigation to the splash screen so the user can re-authenticate.
     */
    fun handleSessionExpired() {
        Timber.w("Session expired — resetting auth state and navigating to splash")
        authVm.setActiveFlow(FlowType.SEEKER)
        seekerVm.setStep(SeekerScreenStep.SPLASH)
        donorVm.setStep(DonorScreenStep.CNIC_UPLOAD)
        _sessionExpiredEvent.tryEmit(Unit)
    }

    // ── Auth / Shared Delegation ────────────────────────────────────────────

    val activeFlow: StateFlow<FlowType> = authVm.activeFlow
    val mainTab: MutableStateFlow<MainTab> = authVm.mainTab
    val phoneNumber: MutableStateFlow<String> = authVm.phoneNumber
    val otpCode: MutableStateFlow<String> = authVm.otpCode
    val otpTimerSeconds: StateFlow<Int> = authVm.otpTimerSeconds

    fun registerPendingPushToken(context: Context) = authVm.registerPendingPushToken(context)
    fun registerPushToken(context: Context, token: String) = authVm.registerPushToken(context, token)
    fun startOtpCountdown() = authVm.startOtpCountdown()

    suspend fun verifyOtpAndContinue(code: String): Boolean {
        return authVm.verifyOtpAndContinue(code) {
            setSeekerStep(SeekerScreenStep.ROLE_PROFILE)
        }
    }

    fun enterMainShell(initialTab: MainTab = MainTab.EMERGENCY) = authVm.enterMainShell(initialTab)

    // ── Seeker Delegation ───────────────────────────────────────────────────

    val seekerStep: StateFlow<SeekerScreenStep> = seekerVm.seekerStep
    val seekerName = seekerVm.seekerName
    val seekerAge = seekerVm.seekerAge
    val seekerGender = seekerVm.seekerGender
    val seekerDistrict = seekerVm.seekerDistrict
    val seekerCnic = seekerVm.seekerCnic
    val selectedBloodGroup = seekerVm.selectedBloodGroup
    val selectedComponent = seekerVm.selectedComponent
    val unitsRequired = seekerVm.unitsRequired
    val selectedHospital = seekerVm.selectedHospital
    val selectedUrgency = seekerVm.selectedUrgency
    val slipImageUri: MutableStateFlow<Uri?> = seekerVm.slipImageUri
    val slipImageSelected: StateFlow<Boolean> = seekerVm.slipImageSelected
    val isOcrProcessing = seekerVm.isOcrProcessing
    val ocrStep1Completed = seekerVm.ocrStep1Completed
    val ocrStep2Completed = seekerVm.ocrStep2Completed
    val slipVerificationError = seekerVm.slipVerificationError
    val slipRoutedToVerification = seekerVm.slipRoutedToVerification
    val feedbackRating = seekerVm.feedbackRating
    val feedbackNote = seekerVm.feedbackNote
    val isProxyCallActive = seekerVm.isProxyCallActive
    val proxyCallSecondsRemaining = seekerVm.proxyCallSecondsRemaining
    val dialEvent = seekerVm.dialEvent

    fun setSeekerStep(step: SeekerScreenStep) {
        seekerVm.setStep(step)
        authVm.setActiveFlow(FlowType.SEEKER)
    }

    fun startProxyCallCountdown() = seekerVm.startProxyCallCountdown()
    fun endProxyCall() = seekerVm.endProxyCall()
    fun submitSlipAndVerify(context: Context) = seekerVm.submitSlipAndVerify(context)
    fun seekerDirectCallToDonor(donorPhone: String) = seekerVm.seekerDirectCallToDonor(donorPhone)
    fun submitSeekerFeedback() = seekerVm.submitSeekerFeedback()

    // ── Donor Delegation ────────────────────────────────────────────────────

    val donorStep: StateFlow<DonorScreenStep> = donorVm.donorStep
    val donorCnicNumber = donorVm.donorCnicNumber
    val donorFrontImageUri: MutableStateFlow<Uri?> = donorVm.donorFrontImageUri
    val donorBackImageUri: MutableStateFlow<Uri?> = donorVm.donorBackImageUri
    val showGeoAlertModal = donorVm.showGeoAlertModal
    val geoAlertPayload: MutableStateFlow<GeoAlertPayload?> = donorVm.geoAlertPayload
    val selectedMapRadiusKm = donorVm.selectedMapRadiusKm
    val selectedHospitalForMap = donorVm.selectedHospitalForMap

    fun setDonorStep(step: DonorScreenStep) {
        donorVm.setStep(step)
        authVm.setActiveFlow(FlowType.DONOR)
    }

    fun consumePendingGeoAlert(context: Context) = donorVm.consumePendingGeoAlert(context)
    fun setDonorAvailability(context: Context, available: Boolean) = donorVm.setDonorAvailability(context, available)
    suspend fun uploadDonorCnicDocuments(context: Context): Boolean = donorVm.uploadDonorCnicDocuments(context)
    fun donorAcceptDispatch() = donorVm.donorAcceptDispatch()
    fun donorFinishDonation() = donorVm.donorFinishDonation()

    // ── Admin Delegation ────────────────────────────────────────────────────

    val adminStep: StateFlow<AdminScreenStep> = adminVm.adminStep
    val isAdminAuthenticated: StateFlow<Boolean> = adminVm.isAdminAuthenticated
    val isTotpRequired: StateFlow<Boolean> = adminVm.isTotpRequired
    val adminAuthError = adminVm.adminAuthError
    val qrScanActive = adminVm.qrScanActive

    fun setAdminStep(step: AdminScreenStep) {
        adminVm.setStep(step)
        authVm.setActiveFlow(FlowType.ADMIN)
    }

    suspend fun adminSignIn(email: String, password: String, totpCode: String): Boolean =
        adminVm.adminSignIn(email, password, totpCode)

    suspend fun adminVerifyTotp(code: String): Boolean =
        adminVm.verifyTotp(code)

    suspend fun adminSignOut() = adminVm.adminSignOut()
    fun adminApproveVerification(id: String) = adminVm.adminApproveVerification(id)
    fun adminRejectVerification(id: String) = adminVm.adminRejectVerification(id)
    fun simulateQrScan() = adminVm.simulateQrScan()

    // ── Consent Gate ────────────────────────────────────────────────────────

    private val _hasAcceptedTerms = MutableStateFlow(false)
    val hasAcceptedTerms: StateFlow<Boolean> = _hasAcceptedTerms.asStateFlow()

    /**
     * Reads the persisted consent state from SharedPreferences.
     * Should be called once at app startup (e.g. in MainActivity.onCreate).
     */
    fun loadConsentState(context: Context) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _hasAcceptedTerms.value = prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    /**
     * Persists acceptance of Terms of Service and Privacy Policy to
     * SharedPreferences and updates the in-memory state.
     */
    fun acceptTerms(context: Context) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, true).apply()
        _hasAcceptedTerms.value = true
    }

    /**
     * Called when the user declines the terms — keeps the state as false
     * so the consent gate remains visible.
     */
    fun declineTerms() {
        _hasAcceptedTerms.value = false
    }

    // ── Feed State (used in main shell / awareness) ─────────────────────────

    val feedFilterBloodGroup = MutableStateFlow<BloodGroup?>(null)
    val feedFilterUrgency = MutableStateFlow<UrgencyLevel?>(null)
    val feedTab = MutableStateFlow(0) // 0 = Urgent, 1 = Awareness
}
