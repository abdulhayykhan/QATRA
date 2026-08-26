package com.example.data.model

enum class BloodGroup(val label: String, val isRare: Boolean = false) {
    A_POS("A+"),
    A_NEG("A-", true),
    B_POS("B+"),
    B_NEG("B-", true),
    O_POS("O+"),
    O_NEG("O-", true),
    AB_POS("AB+"),
    AB_NEG("AB-", true);

    companion object {
        fun fromString(value: String): BloodGroup {
            return entries.find { it.label.equals(value, ignoreCase = true) } ?: O_POS
        }
    }
}

enum class BloodComponent(val displayName: String) {
    WHOLE_BLOOD("Whole Blood"),
    PRBC("PRBC"),
    PLATELETS("Platelets / Mega Unit"),
    PLASMA("Plasma (FFP)")
}

enum class UrgencyLevel(val title: String, val subtitle: String, val badgeText: String) {
    HIGH_PRIORITY("High Priority", "Needed within 2 Hours", "Urgent"),
    STANDARD("Standard", "Needed within 24 Hours", "Routine")
}

enum class RequestStatus {
    VERIFYING,
    BROADCASTING,
    DONOR_MATCHED,
    EN_ROUTE,
    FULFILLED,
    CLOSED
}

data class Hospital(
    val id: String,
    val name: String,
    val shortName: String,
    val address: String,
    val district: String,
    val xPercent: Float, // Mock map coordinates (0f - 1f)
    val yPercent: Float,
    val isTraumaCenter: Boolean = true
)

data class BloodRequest(
    val id: String, // e.g. REQ-8821
    val bloodGroup: BloodGroup,
    val component: BloodComponent,
    val unitsRequired: Int,
    val hospital: Hospital,
    val urgency: UrgencyLevel,
    val seekerName: String,
    val seekerPhoneMasked: String = "0300-XXXXXXX",
    val seekerCnicMasked: String = "42101-XXXXXXX-1",
    val status: RequestStatus = RequestStatus.BROADCASTING,
    val createdAtMinutesAgo: Int = 4,
    val activeDonorsInRadius: Int = 18,
    val respondedDonorsCount: Int = 2,
    val mrnNumber: String = "MRN-44018",
    val ocrConfidence: Int = 94,
    val isVerified: Boolean = true,
    val doctorStampVerified: Boolean = true
)

data class MatchedDonor(
    val id: String, // e.g. Donor #D-104
    val bloodGroup: BloodGroup,
    val distanceKm: Double,
    val etaMinutes: Int,
    val statusText: String, // "Accepted Dispatch", "En Route", "Arrived"
    val phoneMasked: String = "0300-XXXXXXX",
    val isVerified: Boolean = true,
    val lifetimeDonations: Int = 6
)

data class DonorProfile(
    val id: String,
    val name: String,
    val bloodGroup: BloodGroup,
    val phoneMasked: String = "0333-XXXXXXX",
    val cnicMasked: String = "42101-XXXXXXX-7",
    val isAvailableToDonate: Boolean = true,
    val isEligible: Boolean = true,
    val cooldownDaysRemaining: Int = 0,
    val lifetimeDonations: Int = 4,
    val tier: String = "Silver Tier",
    val district: String = "Karachi South",
    val isCnicVerified: Boolean = true
)

data class VerificationQueueItem(
    val id: String,
    val requestId: String,
    val hospitalName: String,
    val doctorStampDetected: Boolean,
    val mrn: String,
    val bloodGroup: BloodGroup,
    val units: Int,
    val ocrConfidence: Int,
    val bloodGroupConfidence: Int,
    val flagWarning: String? = null,
    val status: String = "Pending" // Pending, Approved, Rejected
)

data class FraudAuditItem(
    val id: String,
    val requestId: String,
    val seekerCnicMasked: String,
    val phoneMasked: String,
    val hospitalMrn: String,
    val ocrConfidence: Int,
    val flagReason: String,
    val actionStatus: String = "Flagged" // Flagged, Blacklisted, Whitelisted
)

data class CampusDrive(
    val id: String,
    val title: String,
    val universityVenue: String,
    val targetQuotaUnits: Int,
    val registeredDonors: Int,
    val dateStr: String,
    val timeStr: String,
    val status: String = "Scheduled"
)

data class DriveAttendee(
    val id: String,
    val name: String,
    val deptYear: String,
    val cnicStatus: String = "Verified",
    val preScreeningStatus: String = "Passed", // Passed, Pending, Failed
    val checkInStatus: String = "Checked In 10:42 AM" // or "Awaiting Check-in", "Ineligible"
)

data class AwarenessArticle(
    val id: String,
    val title: String,
    val category: String,
    val readTime: String,
    val summary: String,
    val fullContent: String
)

data class PreScreeningAnswer(
    val ageValid: Boolean = true,
    val weightValid: Boolean = true,
    val noRecentIllness: Boolean = true,
    val noRecentDonation: Boolean = true,
    val noRecentTattooOrSurgery: Boolean = true
)
