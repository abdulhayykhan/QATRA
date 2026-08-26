package com.qatra.app.data.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.qatra.app.data.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin facade that delegates to role-specific repositories.
 * All logic lives in the sub-repositories; this class exists solely to preserve
 * the existing public API surface that QatraViewModel depends on.
 */
class QatraRepository(
    internal val authRepository: AuthRepository,
    internal val seekerRepository: SeekerRepository,
    internal val donorRepository: DonorRepository,
    internal val adminRepository: AdminRepository
) {

    init {
        // Wire cross-repository state references so donor operations can update the
        // active seeker request flow owned by SeekerRepository.
        donorRepository.activeSeekerRequestRef = seekerRepository.activeSeekerRequestFlow

        seedInitialData()
    }

    // Expose lastAuthErrorMessage from the auth repository (primary consumer).
    var lastAuthErrorMessage: String?
        get() = authRepository.lastAuthErrorMessage
            ?: donorRepository.lastAuthErrorMessage
            ?: adminRepository.lastAuthErrorMessage
        private set(value) { /* no-op: sub-repositories manage their own error state */ }

    /** Seeded Karachi Hospitals (PRD / Wireframes). */
    val hospitals: List<Hospital> = HospitalCatalog.hospitals

    // ---------- StateFlow delegations ----------

    val requests: StateFlow<List<BloodRequest>> = seekerRepository.requests
    val matchedDonors: StateFlow<List<MatchedDonor>> = seekerRepository.matchedDonors
    val activeSeekerRequest: StateFlow<BloodRequest?> = seekerRepository.activeSeekerRequest
    val verificationQueue: StateFlow<List<VerificationQueueItem>> = adminRepository.verificationQueue
    val fraudAuditItems: StateFlow<List<FraudAuditItem>> = adminRepository.fraudAuditItems
    val campusDrives: StateFlow<List<CampusDrive>> = adminRepository.campusDrives
    val attendees: StateFlow<List<DriveAttendee>> = adminRepository.attendees
    val donorProfile: StateFlow<DonorProfile> = donorRepository.donorProfile

    private fun seedInitialData() {
        seekerRepository.seedRequests()
        adminRepository.seedData()
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    fun sendFirebaseOtp(activity: Activity, phoneNumber: String, onResult: (Boolean) -> Unit) {
        authRepository.sendFirebaseOtp(activity, phoneNumber, onResult)
    }

    suspend fun verifyOtp(code: String): Boolean = authRepository.verifyOtp(code)

    suspend fun adminSignIn(email: String, password: String): Boolean =
        authRepository.adminSignIn(email, password)

    suspend fun adminSignOut() = authRepository.adminSignOut()

    // ==========================================
    // DONOR — CNIC VALIDATION
    // ==========================================

    companion object {
        fun validatePakistaniCnic(cnicNumber: String): Boolean =
            DonorRepository.validatePakistaniCnic(cnicNumber)
    }

    suspend fun verifyCnic(cnicNumber: String): Boolean =
        donorRepository.verifyCnic(cnicNumber)

    // ==========================================
    // SEEKER — SLIP PROCESSING, DOCUMENTS, PROXIMITY
    // ==========================================

    suspend fun uploadPrivateDocument(context: Context, imageUri: Uri, bucket: String): Boolean =
        seekerRepository.uploadPrivateDocument(context, imageUri, bucket)

    suspend fun processHospitalSlipOcr(
        context: Context,
        imageUri: Uri,
        bloodGroup: BloodGroup,
        fallbackUnits: Int,
        fallbackHospital: Hospital,
        urgency: UrgencyLevel
    ): SeekerRepository.SlipProcessingResult {
        val result = seekerRepository.processHospitalSlipOcr(
            context, imageUri, bloodGroup, fallbackUnits, fallbackHospital, urgency
        )
        // Bridge OCR-produced verification items into the AdminRepository queue.
        result.newVerificationItem?.let { item ->
            adminRepository.prependVerificationItem(item)
        }
        return result
    }

    suspend fun dispatchProximityAlerts(requestId: String, radiusKm: Int): List<MatchedDonor> =
        seekerRepository.dispatchProximityAlerts(requestId, radiusKm)

    // ==========================================
    // DONOR — LOCATION, FCM, DISPATCH, COMPLETION, COOLDOWN
    // ==========================================

    suspend fun updateDonorLocation(latitude: Double, longitude: Double): Boolean =
        donorRepository.updateDonorLocation(latitude, longitude)

    suspend fun registerFcmToken(context: Context, token: String): Boolean =
        donorRepository.registerFcmToken(context, token)

    suspend fun registerPendingFcmToken(context: Context): Boolean =
        donorRepository.registerPendingFcmToken(context)

    suspend fun acceptEmergencyDispatch(requestId: String): Boolean =
        donorRepository.acceptEmergencyDispatch(requestId)

    suspend fun completeDonation(rating: Int, thankYouNote: String?): Boolean =
        donorRepository.completeDonation(rating, thankYouNote)

    fun setDonorAvailability(available: Boolean) =
        donorRepository.setDonorAvailability(available)

    fun setDonorCooldownDemo(days: Int) =
        donorRepository.setDonorCooldownDemo(days)

    // ==========================================
    // ADMIN — VERIFICATION QUEUE, FRAUD AUDIT, CAMPUS DRIVES
    // ==========================================

    suspend fun approveVerificationItem(id: String): Boolean =
        adminRepository.approveVerificationItem(id)

    suspend fun rejectVerificationItem(id: String): Boolean =
        adminRepository.rejectVerificationItem(id)

    fun updateFraudItemAction(id: String, newStatus: String) =
        adminRepository.updateFraudItemAction(id, newStatus)

    fun scheduleNewCampusDrive(
        title: String,
        venue: String,
        targetQuota: Int,
        dateStr: String,
        timeStr: String
    ) = adminRepository.scheduleNewCampusDrive(title, venue, targetQuota, dateStr, timeStr)

    fun checkInAttendee(id: String) = adminRepository.checkInAttendee(id)
}
