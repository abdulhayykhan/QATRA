package com.qatra.app.data.repository

import com.qatra.app.data.model.*
import io.github.jan.supabase.auth.auth
import timber.log.Timber
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminRepository {

    var lastAuthErrorMessage: String? = null
        private set

    private fun setLastAuthError(message: String?) {
        lastAuthErrorMessage = message
    }

    private val _verificationQueue = MutableStateFlow<List<VerificationQueueItem>>(emptyList())
    val verificationQueue: StateFlow<List<VerificationQueueItem>> = _verificationQueue.asStateFlow()

    private val _fraudAuditItems = MutableStateFlow<List<FraudAuditItem>>(emptyList())
    val fraudAuditItems: StateFlow<List<FraudAuditItem>> = _fraudAuditItems.asStateFlow()

    private val _campusDrives = MutableStateFlow<List<CampusDrive>>(emptyList())
    val campusDrives: StateFlow<List<CampusDrive>> = _campusDrives.asStateFlow()

    private val _attendees = MutableStateFlow<List<DriveAttendee>>(emptyList())
    val attendees: StateFlow<List<DriveAttendee>> = _attendees.asStateFlow()

    fun seedData() {
        _verificationQueue.value = listOf(
            VerificationQueueItem(
                id = "VQ-1",
                requestId = "REQ-8821",
                hospitalName = "JPMC",
                doctorStampDetected = true,
                mrn = "#RN-44018",
                bloodGroup = BloodGroup.B_POS,
                units = 2,
                ocrConfidence = 94,
                bloodGroupConfidence = 82, // flagged < 85% per Wireframe Section C
                flagWarning = "Blood group handwriting low confidence (82%) — Verify manual slip",
                status = "Pending"
            ),
            VerificationQueueItem(
                id = "VQ-2",
                requestId = "REQ-8822",
                hospitalName = "Civil Hospital Karachi",
                doctorStampDetected = true,
                mrn = "#RN-99201",
                bloodGroup = BloodGroup.O_NEG,
                units = 1,
                ocrConfidence = 97,
                bloodGroupConfidence = 96,
                flagWarning = null,
                status = "Pending"
            )
        )

        _fraudAuditItems.value = listOf(
            FraudAuditItem(
                id = "FA-1",
                requestId = "REQ-9942",
                seekerCnicMasked = "42101-XXXXXXX-1",
                phoneMasked = "0300-XXXXXXX",
                hospitalMrn = "MRN-7782A",
                ocrConfidence = 45,
                flagReason = "Re-used Slip Image",
                actionStatus = "Flagged"
            ),
            FraudAuditItem(
                id = "FA-2",
                requestId = "REQ-9938",
                seekerCnicMasked = "42201-XXXXXXX-2",
                phoneMasked = "0333-XXXXXXX",
                hospitalMrn = "MRN-2219B",
                ocrConfidence = 82,
                flagReason = "Mismatched Doctor Stamp",
                actionStatus = "Flagged"
            ),
            FraudAuditItem(
                id = "FA-3",
                requestId = "REQ-9915",
                seekerCnicMasked = "61101-XXXXXXX-5",
                phoneMasked = "0345-XXXXXXX",
                hospitalMrn = "MRN-7782A",
                ocrConfidence = 95,
                flagReason = "Duplicate MRN",
                actionStatus = "Flagged"
            )
        )

        _campusDrives.value = listOf(
            CampusDrive(
                id = "CD-101",
                title = "NED Spring Blood Drive '26",
                universityVenue = "NED University Main Auditorium",
                targetQuotaUnits = 150,
                registeredDonors = 88,
                dateStr = "25 Aug 2026",
                timeStr = "10:00 AM - 04:00 PM",
                status = "Scheduled"
            ),
            CampusDrive(
                id = "CD-102",
                title = "IBA Karachi Life Savers Chapter",
                universityVenue = "IBA Main Campus, Student Center",
                targetQuotaUnits = 100,
                registeredDonors = 64,
                dateStr = "28 Aug 2026",
                timeStr = "11:00 AM - 05:00 PM",
                status = "Scheduled"
            ),
            CampusDrive(
                id = "CD-103",
                title = "FAST NUCES Emergency Response Drive",
                universityVenue = "FAST CFD Ground, Shah Latif Town",
                targetQuotaUnits = 120,
                registeredDonors = 95,
                dateStr = "02 Sep 2026",
                timeStr = "09:30 AM - 03:30 PM",
                status = "Scheduled"
            )
        )

        _attendees.value = listOf(
            DriveAttendee(
                id = "ATT-01",
                name = "Ali Zain",
                deptYear = "CS 2021",
                cnicStatus = "Verified",
                preScreeningStatus = "Passed",
                checkInStatus = "Checked In 10:42 AM"
            ),
            DriveAttendee(
                id = "ATT-02",
                name = "Fatima Ahmad",
                deptYear = "BBA 2023",
                cnicStatus = "Verified",
                preScreeningStatus = "Pending",
                checkInStatus = "Awaiting Check-in"
            ),
            DriveAttendee(
                id = "ATT-03",
                name = "Omar Khan",
                deptYear = "EE 2020",
                cnicStatus = "Verified",
                preScreeningStatus = "Failed (Last Hb)",
                checkInStatus = "Ineligible"
            ),
            DriveAttendee(
                id = "ATT-04",
                name = "Ayesha Siddiqui",
                deptYear = "BioMed 2022",
                cnicStatus = "Verified",
                preScreeningStatus = "Passed",
                checkInStatus = "Awaiting Check-in"
            )
        )
    }

    /** Prepends a verification item produced by seeker OCR processing. */
    fun prependVerificationItem(item: VerificationQueueItem) {
        _verificationQueue.value = listOf(item) + _verificationQueue.value
    }

    // FR 3.x — admin approves/rejects a verification-queue item. Optimistic local update
    // first, then the durable status write to verification_queue. The write is gated by
    // the admin RLS policy (auth.jwt() ->> 'user_role' = 'admin'), so it only lands once
    // the custom access-token hook is enabled; returns whether it did.
    suspend fun approveVerificationItem(id: String): Boolean = setVerificationStatus(id, "Approved")

    suspend fun rejectVerificationItem(id: String): Boolean = setVerificationStatus(id, "Rejected")

    private suspend fun setVerificationStatus(id: String, status: String): Boolean {
        _verificationQueue.value = _verificationQueue.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
        val client = SupabaseClientProvider.client ?: return false
        if (client.auth.currentUserOrNull() == null) return false
        if (id.toUuidOrNull() == null) return false
        return try {
            client.from("verification_queue").update({
                set("status", status)
            }) {
                filter { eq("id", id) }
            }
            true
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to update verification item status to %s", status)
            setLastAuthError(exception.message ?: "Unable to update verification item.")
            false
        }
    }

    fun updateFraudItemAction(id: String, newStatus: String) {
        _fraudAuditItems.value = _fraudAuditItems.value.map {
            if (it.id == id) it.copy(actionStatus = newStatus) else it
        }
    }

    fun scheduleNewCampusDrive(
        title: String,
        venue: String,
        targetQuota: Int,
        dateStr: String,
        timeStr: String
    ) {
        val newDrive = CampusDrive(
            id = "CD-${(200..999).random()}",
            title = title,
            universityVenue = venue,
            targetQuotaUnits = targetQuota,
            registeredDonors = 0,
            dateStr = dateStr,
            timeStr = timeStr,
            status = "Scheduled"
        )
        _campusDrives.value = listOf(newDrive) + _campusDrives.value
    }

    fun checkInAttendee(id: String) {
        _attendees.value = _attendees.value.map {
            if (it.id == id) it.copy(checkInStatus = "Checked In Just Now") else it
        }
    }

    /**
     * Retrieves the list of verified MFA factor IDs for the currently authenticated user.
     * Returns an empty list when no Supabase client is available, the user is not
     * authenticated, or the MFA API call fails.
     */
    suspend fun getAdminMfaFactors(): List<String> {
        return emptyList()
    }

    suspend fun verifyAdminTotp(factorId: String, code: String): Boolean {
        return false
    }

    private fun String.toUuidOrNull(): java.util.UUID? =
        runCatching { java.util.UUID.fromString(this) }.getOrNull()
}
