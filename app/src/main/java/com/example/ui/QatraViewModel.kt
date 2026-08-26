package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.QatraRepository
import com.example.notifications.GeoAlertPayload
import com.example.notifications.QatraPushState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class MainTab {
    EMERGENCY,
    DONATE,
    LEARN,
    DESK
}

enum class SeekerScreenStep {
    SPLASH,
    PHONE_VERIFICATION,
    ROLE_PROFILE,
    REQUEST_CREATION,
    SLIP_UPLOAD,
    VERIFICATION_MODAL,
    LIVE_STATUS_FEED,
    MATCHED_DONORS,
    MASKED_CALL,
    CONFIRMATION
}

enum class DonorScreenStep {
    CNIC_UPLOAD,
    PRE_SCREENING,
    HOME_DASHBOARD,
    INTERACTIVE_MAP,
    NAVIGATION_ROUTING,
    DONATION_COMPLETE,
    COOLDOWN_STATE
}

enum class AdminScreenStep {
    LOGIN_2FA,
    VERIFICATION_QUEUE,
    FRAUD_AUDIT,
    DRIVE_MANAGEMENT
}

enum class FlowType {
    SEEKER,
    DONOR,
    ADMIN,
    MAIN_SHELL
}

class QatraViewModel(
    val repository: QatraRepository = QatraRepository()
) : ViewModel() {

    // Main Navigation State
    private val _activeFlow = MutableStateFlow(FlowType.SEEKER)
    val activeFlow: StateFlow<FlowType> = _activeFlow.asStateFlow()

    val mainTab = MutableStateFlow(MainTab.EMERGENCY)

    private val _seekerStep = MutableStateFlow(SeekerScreenStep.SPLASH)
    val seekerStep: StateFlow<SeekerScreenStep> = _seekerStep.asStateFlow()

    private val _donorStep = MutableStateFlow(DonorScreenStep.CNIC_UPLOAD)
    val donorStep: StateFlow<DonorScreenStep> = _donorStep.asStateFlow()

    private val _adminStep = MutableStateFlow(AdminScreenStep.LOGIN_2FA)
    val adminStep: StateFlow<AdminScreenStep> = _adminStep.asStateFlow()

    // Auth & OTP State
    val phoneNumber = MutableStateFlow("300 1234567")
    val otpCode = MutableStateFlow("")
    private val _otpTimerSeconds = MutableStateFlow(180)
    val otpTimerSeconds: StateFlow<Int> = _otpTimerSeconds.asStateFlow()
    private var otpTimerJob: Job? = null

    // Seeker Profile State
    val seekerName = MutableStateFlow("Jane Doe")
    val seekerAge = MutableStateFlow("30")
    val seekerGender = MutableStateFlow("F") // M, F, O
    val seekerDistrict = MutableStateFlow("Karachi South")
    val seekerCnic = MutableStateFlow("42101-1234567-1")

    // Emergency Request Form State
    val selectedBloodGroup = MutableStateFlow(BloodGroup.O_NEG)
    val selectedComponent = MutableStateFlow(BloodComponent.PRBC)
    val unitsRequired = MutableStateFlow(2)
    val selectedHospital = MutableStateFlow(repository.hospitals[0])
    val selectedUrgency = MutableStateFlow(UrgencyLevel.HIGH_PRIORITY)

    // Slip Upload & Verification
    val slipImageUri = MutableStateFlow<Uri?>(null)
    val slipImageSelected: StateFlow<Boolean> = slipImageUri.map { it != null }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false
    )
    val isOcrProcessing = MutableStateFlow(false)
    val ocrStep1Completed = MutableStateFlow(false)
    val ocrStep2Completed = MutableStateFlow(false)
    val slipVerificationError = MutableStateFlow<String?>(null)
    val slipRoutedToVerification = MutableStateFlow(false)

    // Donor State
    val donorCnicNumber = MutableStateFlow("42101-9876543-7")
    val donorFrontImageUri = MutableStateFlow<Uri?>(null)
    val donorBackImageUri = MutableStateFlow<Uri?>(null)
    val showGeoAlertModal = MutableStateFlow(false)
    val geoAlertPayload = MutableStateFlow<GeoAlertPayload?>(null)
    val selectedMapRadiusKm = MutableStateFlow(10) // 5, 10, 15
    val selectedHospitalForMap = MutableStateFlow<Hospital?>(repository.hospitals[0])

    // Masked Proxy Call State
    val isProxyCallActive = MutableStateFlow(false)
    val proxyCallSecondsRemaining = MutableStateFlow(120)
    private var proxyCallJob: Job? = null
    private var donorLocationJob: Job? = null

    // Seeker Confirmation & Feedback
    val feedbackRating = MutableStateFlow(5)
    val feedbackNote = MutableStateFlow("")

    // Admin State
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()
    val adminAuthError = MutableStateFlow<String?>(null)

    // Feed State
    val feedFilterBloodGroup = MutableStateFlow<BloodGroup?>(null)
    val feedFilterUrgency = MutableStateFlow<UrgencyLevel?>(null)
    val feedTab = MutableStateFlow(0) // 0 = Urgent, 1 = Awareness

    // Campus Drive Schedule Form State
    val driveTitle = MutableStateFlow("NED Spring Blood Drive '26")
    val driveVenue = MutableStateFlow("NED University Main Auditorium")
    val driveQuota = MutableStateFlow(150)
    val driveDate = MutableStateFlow("25/08/2026")
    val driveTime = MutableStateFlow("10:00 AM")
    val qrScanActive = MutableStateFlow(false)

    init {
        startOtpCountdown()
        viewModelScope.launch {
            QatraPushState.latestGeoAlert.collect { alert ->
                if (alert != null) {
                    geoAlertPayload.value = alert
                    showGeoAlertModal.value = true
                }
            }
        }
    }

    fun registerPendingPushToken(context: Context) {
        viewModelScope.launch {
            repository.registerPendingFcmToken(context)
        }
    }

    fun registerPushToken(context: Context, token: String) {
        viewModelScope.launch {
            repository.registerFcmToken(context, token)
        }
    }

    fun consumePendingGeoAlert(context: Context) {
        QatraPushState.consume(context)?.let { alert ->
            geoAlertPayload.value = alert
            showGeoAlertModal.value = true
        }
    }

    fun startOtpCountdown() {
        otpTimerJob?.cancel()
        _otpTimerSeconds.value = 180
        otpTimerJob = viewModelScope.launch {
            while (_otpTimerSeconds.value > 0) {
                delay(1000)
                _otpTimerSeconds.value -= 1
            }
        }
    }

    fun startProxyCallCountdown() {
        proxyCallJob?.cancel()
        proxyCallSecondsRemaining.value = 120
        isProxyCallActive.value = true
        proxyCallJob = viewModelScope.launch {
            while (proxyCallSecondsRemaining.value > 0 && isProxyCallActive.value) {
                delay(1000)
                proxyCallSecondsRemaining.value -= 1
            }
            isProxyCallActive.value = false
        }
    }

    fun endProxyCall() {
        isProxyCallActive.value = false
        proxyCallJob?.cancel()
    }

    fun setSeekerStep(step: SeekerScreenStep) {
        _seekerStep.value = step
        _activeFlow.value = FlowType.SEEKER
    }

    fun setDonorStep(step: DonorScreenStep) {
        _donorStep.value = step
        _activeFlow.value = FlowType.DONOR
    }

    fun setAdminStep(step: AdminScreenStep) {
        _adminStep.value = step
        _activeFlow.value = FlowType.ADMIN
    }

    suspend fun adminSignIn(email: String, password: String, totpCode: String): Boolean {
        // ponytail: the TOTP field is accepted but not yet enforced. Supabase MFA (TOTP
        // enroll + challenge) is a documented Phase-1 pilot gap; password auth is real and
        // server-verified. The UI keeps the field so the terminal flow matches production.
        val ok = repository.adminSignIn(email, password)
        adminAuthError.value = if (ok) null else (repository.lastAuthErrorMessage ?: "Invalid credentials.")
        _isAdminAuthenticated.value = ok
        return ok
    }

    suspend fun adminSignOut() {
        repository.adminSignOut()
        _isAdminAuthenticated.value = false
        adminAuthError.value = null
    }

    fun enterMainShell(initialTab: MainTab = MainTab.EMERGENCY) {
        mainTab.value = initialTab
        _activeFlow.value = FlowType.MAIN_SHELL
    }

    suspend fun verifyOtpAndContinue(code: String): Boolean {
        val isValid = repository.verifyOtp(code)
        if (isValid) {
            setSeekerStep(SeekerScreenStep.ROLE_PROFILE)
        }
        return isValid
    }

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

    fun setDonorAvailability(context: Context, available: Boolean) {
        repository.setDonorAvailability(available)
        if (available) {
            startDonorLocationUpdates(context)
        } else {
            donorLocationJob?.cancel()
            donorLocationJob = null
        }
    }

    private fun startDonorLocationUpdates(context: Context) {
        if (donorLocationJob?.isActive == true) return
        donorLocationJob = viewModelScope.launch {
            while (repository.donorProfile.value.isAvailableToDonate) {
                val location = getCurrentLocation(context)
                if (location != null) {
                    repository.updateDonorLocation(location.latitude, location.longitude)
                }
                delay(3 * 60 * 1000L)
            }
        }
    }

    private suspend fun getCurrentLocation(context: Context): Location? {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) return null

        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        return suspendCancellableCoroutine { continuation ->
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
        }
    }

    override fun onCleared() {
        donorLocationJob?.cancel()
        super.onCleared()
    }

    suspend fun uploadDonorCnicDocuments(context: Context): Boolean {
        val frontUri = donorFrontImageUri.value
        val backUri = donorBackImageUri.value
        if (frontUri == null || backUri == null) {
            return false
        }
        val frontUploaded = repository.uploadPrivateDocument(context, frontUri, "cnic-documents")
        return frontUploaded && repository.uploadPrivateDocument(context, backUri, "cnic-documents")
    }

    // Donor accepts alert
    fun donorAcceptDispatch() {
        showGeoAlertModal.value = false
        repository.acceptEmergencyDispatch(
            donorId = "DNR-001",
            requestId = geoAlertPayload.value?.requestId ?: "REQ-8821"
        )
        _donorStep.value = DonorScreenStep.NAVIGATION_ROUTING
    }

    fun donorFinishDonation() {
        val note = feedbackNote.value.ifBlank { "Donation completed via QATRA emergency dispatch." }
        repository.completeDonation("DNR-001", feedbackRating.value, note)
        _donorStep.value = DonorScreenStep.DONATION_COMPLETE
    }

    fun submitSeekerFeedback() {
        // ponytail: feedback lives in VM state (feedbackRating/feedbackNote) — no backend sink for seeker ratings yet.
        setSeekerStep(SeekerScreenStep.SPLASH)
    }

    fun scheduleDrive() {
        repository.scheduleNewCampusDrive(
            title = driveTitle.value,
            venue = driveVenue.value,
            targetQuota = driveQuota.value,
            dateStr = driveDate.value,
            timeStr = driveTime.value
        )
    }

    fun simulateQrScan() {
        viewModelScope.launch {
            qrScanActive.value = true
            delay(1500)
            repository.checkInAttendee("ATT-02")
            qrScanActive.value = false
        }
    }
}
