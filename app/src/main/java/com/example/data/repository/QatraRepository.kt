package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.data.model.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class QatraRepository {
    var lastAuthErrorMessage: String? = null
        private set

    private var firebaseVerificationId: String? = null
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val supabaseClient: SupabaseClient? by lazy {
        if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }
        } else {
            null
        }
    }

    // Seeded Karachi Hospitals (PRD / Wireframes)
    val hospitals = listOf(
        Hospital(
            id = "H1",
            name = "Jinnah Postgraduate Medical Centre (JPMC)",
            shortName = "JPMC",
            address = "Rafiqui H.J. Shaheed Rd, Cantonment, Karachi",
            district = "Karachi South",
            xPercent = 0.50f,
            yPercent = 0.68f,
            isTraumaCenter = true
        ),
        Hospital(
            id = "H2",
            name = "Dr. Ruth K.M. Pfau Civil Hospital",
            shortName = "Civil Hospital",
            address = "Mission Rd, New Dehli Colony, Karachi",
            district = "Karachi South",
            xPercent = 0.44f,
            yPercent = 0.55f,
            isTraumaCenter = true
        ),
        Hospital(
            id = "H3",
            name = "Liaquat National Hospital & Medical College",
            shortName = "Liaquat National",
            address = "National Stadium Rd, Gulshan-e-Iqbal, Karachi",
            district = "Karachi East",
            xPercent = 0.58f,
            yPercent = 0.42f,
            isTraumaCenter = true
        ),
        Hospital(
            id = "H4",
            name = "The Indus Hospital",
            shortName = "Indus Hospital",
            address = "Korangi Crossing, Karachi",
            district = "Karachi Korangi",
            xPercent = 0.68f,
            yPercent = 0.76f,
            isTraumaCenter = true
        ),
        Hospital(
            id = "H5",
            name = "Aga Khan University Hospital (AKUH)",
            shortName = "Aga Khan Hospital",
            address = "Stadium Rd, Karachi",
            district = "Karachi East",
            xPercent = 0.60f,
            yPercent = 0.38f,
            isTraumaCenter = false
        ),
        Hospital(
            id = "H6",
            name = "Abbasi Shaheed Hospital",
            shortName = "Abbasi Shaheed",
            address = "Paposh Nagar, Nazimabad, Karachi",
            district = "Karachi Central",
            xPercent = 0.42f,
            yPercent = 0.32f,
            isTraumaCenter = true
        )
    )

    // In-memory state flows
    private val _requests = MutableStateFlow<List<BloodRequest>>(emptyList())
    val requests: StateFlow<List<BloodRequest>> = _requests.asStateFlow()

    private val _matchedDonors = MutableStateFlow<List<MatchedDonor>>(emptyList())
    val matchedDonors: StateFlow<List<MatchedDonor>> = _matchedDonors.asStateFlow()

    private val _activeSeekerRequest = MutableStateFlow<BloodRequest?>(null)
    val activeSeekerRequest: StateFlow<BloodRequest?> = _activeSeekerRequest.asStateFlow()

    private val _verificationQueue = MutableStateFlow<List<VerificationQueueItem>>(emptyList())
    val verificationQueue: StateFlow<List<VerificationQueueItem>> = _verificationQueue.asStateFlow()

    private val _fraudAuditItems = MutableStateFlow<List<FraudAuditItem>>(emptyList())
    val fraudAuditItems: StateFlow<List<FraudAuditItem>> = _fraudAuditItems.asStateFlow()

    private val _campusDrives = MutableStateFlow<List<CampusDrive>>(emptyList())
    val campusDrives: StateFlow<List<CampusDrive>> = _campusDrives.asStateFlow()

    private val _attendees = MutableStateFlow<List<DriveAttendee>>(emptyList())
    val attendees: StateFlow<List<DriveAttendee>> = _attendees.asStateFlow()

    private val _donorProfile = MutableStateFlow(
        DonorProfile(
            id = "DNR-001",
            name = "Alex Mercer",
            bloodGroup = BloodGroup.O_POS,
            phoneMasked = "0300-XXXXXXX",
            cnicMasked = "42101-XXXXXXX-7",
            isAvailableToDonate = true,
            isEligible = true,
            cooldownDaysRemaining = 0,
            lifetimeDonations = 4,
            tier = "Silver Tier",
            district = "Karachi South",
            isCnicVerified = true
        )
    )
    val donorProfile: StateFlow<DonorProfile> = _donorProfile.asStateFlow()

    init {
        seedInitialData()
    }

    private fun seedInitialData() {
        val req1 = BloodRequest(
            id = "REQ-8821",
            bloodGroup = BloodGroup.O_NEG,
            component = BloodComponent.PRBC,
            unitsRequired = 2,
            hospital = hospitals[0], // JPMC
            urgency = UrgencyLevel.HIGH_PRIORITY,
            seekerName = "Ahmed Khan",
            seekerPhoneMasked = "0300-XXXXXXX",
            seekerCnicMasked = "42101-XXXXXXX-1",
            status = RequestStatus.BROADCASTING,
            createdAtMinutesAgo = 2,
            activeDonorsInRadius = 18,
            respondedDonorsCount = 2,
            mrnNumber = "MRN-44018",
            ocrConfidence = 94,
            isVerified = true
        )

        val req2 = BloodRequest(
            id = "REQ-8819",
            bloodGroup = BloodGroup.A_POS,
            component = BloodComponent.WHOLE_BLOOD,
            unitsRequired = 1,
            hospital = hospitals[1], // Civil Hospital
            urgency = UrgencyLevel.HIGH_PRIORITY,
            seekerName = "Mohammad Bilal",
            seekerPhoneMasked = "0333-XXXXXXX",
            seekerCnicMasked = "42201-XXXXXXX-4",
            status = RequestStatus.BROADCASTING,
            createdAtMinutesAgo = 12,
            activeDonorsInRadius = 32,
            respondedDonorsCount = 4,
            mrnNumber = "MRN-88210",
            ocrConfidence = 96,
            isVerified = true
        )

        val req3 = BloodRequest(
            id = "REQ-8812",
            bloodGroup = BloodGroup.B_POS,
            component = BloodComponent.PLATELETS,
            unitsRequired = 3,
            hospital = hospitals[2], // Liaquat National
            urgency = UrgencyLevel.STANDARD,
            seekerName = "Zainab Tariq",
            seekerPhoneMasked = "0345-XXXXXXX",
            seekerCnicMasked = "42301-XXXXXXX-9",
            status = RequestStatus.BROADCASTING,
            createdAtMinutesAgo = 45,
            activeDonorsInRadius = 24,
            respondedDonorsCount = 3,
            mrnNumber = "MRN-19042",
            ocrConfidence = 91,
            isVerified = true
        )

        val req4 = BloodRequest(
            id = "REQ-8805",
            bloodGroup = BloodGroup.AB_NEG,
            component = BloodComponent.PRBC,
            unitsRequired = 1,
            hospital = hospitals[3], // Indus Hospital
            urgency = UrgencyLevel.HIGH_PRIORITY,
            seekerName = "Farooq Shah",
            seekerPhoneMasked = "0312-XXXXXXX",
            seekerCnicMasked = "42401-XXXXXXX-3",
            status = RequestStatus.BROADCASTING,
            createdAtMinutesAgo = 8,
            activeDonorsInRadius = 7,
            respondedDonorsCount = 1,
            mrnNumber = "MRN-33120",
            ocrConfidence = 89,
            isVerified = true
        )

        _requests.value = listOf(req1, req2, req3, req4)
        _activeSeekerRequest.value = req1

        _matchedDonors.value = listOf(
            MatchedDonor(
                id = "Donor #D-104",
                bloodGroup = BloodGroup.O_NEG,
                distanceKm = 2.3,
                etaMinutes = 12,
                statusText = "Accepted Dispatch",
                phoneMasked = "0300-XXXXXXX",
                isVerified = true,
                lifetimeDonations = 8
            ),
            MatchedDonor(
                id = "Donor #D-208",
                bloodGroup = BloodGroup.O_NEG,
                distanceKm = 5.1,
                etaMinutes = 22,
                statusText = "En Route",
                phoneMasked = "0321-XXXXXXX",
                isVerified = true,
                lifetimeDonations = 5
            ),
            MatchedDonor(
                id = "Donor #D-315",
                bloodGroup = BloodGroup.O_NEG,
                distanceKm = 7.8,
                etaMinutes = 35,
                statusText = "Notified",
                phoneMasked = "0345-XXXXXXX",
                isVerified = true,
                lifetimeDonations = 3
            )
        )

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

    // ==========================================
    // AUTHENTICATION & IDENTITY VALIDATION
    // ==========================================

    private fun normalizeE164PhoneNumber(phoneNumber: String): String {
        val digits = phoneNumber.filter(Char::isDigit)
        return when {
            digits.isEmpty() -> ""
            digits.startsWith("92") && digits.length == 12 -> "+$digits"
            digits.startsWith("0") && digits.length == 11 -> "+92${digits.drop(1)}"
            digits.length == 10 && digits.startsWith("3") -> "+92$digits"
            phoneNumber.startsWith("+") -> phoneNumber
            else -> "+$digits"
        }
    }

    private fun setLastAuthError(message: String?) {
        lastAuthErrorMessage = message
    }

    companion object {
        private val validProvinceDistrictRanges: List<IntRange> = listOf(
            1..16,
            21..29,
            31..39,
            41..49,
            51..59,
            61..69,
            71..79,
            81..89
        )

        /**
         * Format and district-code validation only. Pakistani CNICs have no checksum
         * digit - this does NOT confirm the CNIC is real or belongs to the submitting
         * user. Real identity confirmation requires NADRA Verisys integration, which is
         * out of scope for this build. // TODO: NADRA Verisys integration for production
         * identity verification.
         */
        fun validatePakistaniCnic(cnicNumber: String): Boolean {
            val normalized = cnicNumber.trim()
            if (normalized.length != 13 || normalized.any { !it.isDigit() }) return false

            val provincePrefix = normalized.substring(0, 2).toIntOrNull() ?: return false
            return validProvinceDistrictRanges.any { provincePrefix in it }
        }
    }

    fun sendFirebaseOtp(activity: Activity, phoneNumber: String, onResult: (Boolean) -> Unit) {
        val normalizedPhone = normalizeE164PhoneNumber(phoneNumber)
        if (normalizedPhone.isBlank()) {
            setLastAuthError("Phone number is required for OTP sign-in.")
            onResult(false)
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener {
                        setLastAuthError(it.message ?: "Firebase phone verification failed.")
                        onResult(false)
                    }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                setLastAuthError(exception.message ?: "Firebase phone verification failed.")
                onResult(false)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                firebaseVerificationId = verificationId
                setLastAuthError(null)
                onResult(true)
            }
        }

        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(normalizedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    suspend fun verifyOtp(code: String): Boolean {
        val verificationId = firebaseVerificationId ?: run {
            setLastAuthError("No Firebase OTP verification is pending.")
            return false
        }

        if (code.length != 6 || code.any { !it.isDigit() }) {
            setLastAuthError("The OTP must be a 6-digit number.")
            return false
        }

        val client = supabaseClient ?: run {
            setLastAuthError("Supabase client is not configured. Check SUPABASE_URL and SUPABASE_ANON_KEY.")
            return false
        }

        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val firebaseUser = firebaseAuth.signInWithCredentialAwait(credential)
            val firebaseToken = firebaseUser.getIdToken(true).awaitResult().token
                ?: throw IllegalStateException("Firebase did not return an ID token.")
            val response = httpClient.newCall(
                Request.Builder()
                    .url("${BuildConfig.SUPABASE_URL}/functions/v1/verify-firebase-phone")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    .post(
                        JSONObject().put("firebase_id_token", firebaseToken)
                            .toString()
                            .toRequestBody("application/json".toMediaType())
                    )
                    .build()
            ).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Phone session exchange failed (${response.code}).")
            }
            val session = JSONObject(response.body?.string().orEmpty())
            client.auth.importAuthToken(
                accessToken = session.getString("access_token"),
                refreshToken = session.getString("refresh_token")
            )
            firebaseVerificationId = null
            setLastAuthError(null)
            true
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: exception::class.simpleName ?: "Unknown OTP verification error")
            false
        }
    }

    private suspend fun FirebaseAuth.signInWithCredentialAwait(
        credential: PhoneAuthCredential
    ) = signInWithCredential(credential).awaitResult().user
        ?: throw IllegalStateException("Firebase did not return a signed-in user.")

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
            addOnCanceledListener { continuation.cancel() }
        }

    // Format and district-code validation only; this does not confirm identity ownership.
    // TODO: NADRA Verisys integration for production identity verification.
    suspend fun verifyCnic(cnicNumber: String): Boolean {
        return try {
            val isValid = validatePakistaniCnic(cnicNumber)
            if (isValid) {
                setLastAuthError(null)
            } else {
                setLastAuthError("CNIC format or district-code validation failed.")
            }
            isValid
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: exception::class.simpleName ?: "Unknown CNIC validation error")
            false
        }
    }

    data class SlipProcessingResult(
        val request: BloodRequest?,
        val routedToVerification: Boolean,
        val errorMessage: String? = null
    )

    suspend fun uploadPrivateDocument(context: Context, imageUri: Uri, bucket: String): Boolean {
        val client = supabaseClient ?: run {
            setLastAuthError("Supabase client is not configured. Check SUPABASE_URL and SUPABASE_ANON_KEY.")
            return false
        }

        return try {
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("The selected image could not be read.")
            val objectPath = "${System.currentTimeMillis()}-${imageUri.lastPathSegment ?: "document"}"
            client.storage.from(bucket).upload(objectPath, bytes)
            setLastAuthError(null)
            true
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: exception::class.simpleName ?: "Document upload failed")
            false
        }
    }

    suspend fun processHospitalSlipOcr(
        context: Context,
        imageUri: Uri,
        bloodGroup: BloodGroup,
        fallbackUnits: Int,
        fallbackHospital: Hospital,
        urgency: UrgencyLevel
    ): SlipProcessingResult {
        val ocrText = try {
            recognizeText(context, imageUri)
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: "Unable to read the hospital slip.")
            return SlipProcessingResult(null, routedToVerification = true, errorMessage = lastAuthErrorMessage)
        }

        val mrn = Regex("(?i)\\b(?:MRN|MR\\s*NO|MR#)\\s*[:#-]?\\s*([A-Z0-9-]{4,})\\b")
            .find(ocrText)?.groupValues?.getOrNull(1)
        val units = Regex("(?i)\\b(?:units?|qty|quantity)\\s*[:#-]?\\s*(\\d{1,2})\\b")
            .find(ocrText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val recognizedHospital = hospitals.firstOrNull { hospital ->
            ocrText.contains(hospital.name, ignoreCase = true) ||
                ocrText.contains(hospital.shortName, ignoreCase = true)
        }
        val confidence = when {
            recognizedHospital != null && mrn != null && units != null -> 96
            recognizedHospital != null && mrn != null -> 82
            mrn != null || units != null -> 68
            else -> 42
        }
        val extractedHospital = recognizedHospital ?: fallbackHospital
        val extractedUnits = units ?: fallbackUnits
        val requestId = "REQ-${(1000..9999).random()}"

        if (!uploadPrivateDocument(context, imageUri, "hospital-slips")) {
            return SlipProcessingResult(null, routedToVerification = false, errorMessage = lastAuthErrorMessage)
        }

        if (confidence < 85 || recognizedHospital == null || mrn == null) {
            _verificationQueue.value = listOf(
                VerificationQueueItem(
                    id = "VQ-${(1000..9999).random()}",
                    requestId = requestId,
                    hospitalName = recognizedHospital?.shortName ?: "Hospital not detected",
                    doctorStampDetected = false,
                    mrn = mrn ?: "Not detected",
                    bloodGroup = bloodGroup,
                    units = extractedUnits,
                    ocrConfidence = confidence,
                    bloodGroupConfidence = 0,
                    flagWarning = "OCR could not confidently identify the hospital and MRN. Manual review required.",
                    status = "Pending"
                )
            ) + _verificationQueue.value
            return SlipProcessingResult(null, routedToVerification = true)
        }

        val newRequest = BloodRequest(
            id = requestId,
            bloodGroup = bloodGroup,
            component = BloodComponent.PRBC,
            unitsRequired = extractedUnits,
            hospital = extractedHospital,
            urgency = urgency,
            seekerName = "Emergency Patient",
            seekerPhoneMasked = "0300-XXXXXXX",
            seekerCnicMasked = "42101-XXXXXXX-1",
            status = RequestStatus.BROADCASTING,
            createdAtMinutesAgo = 1,
            activeDonorsInRadius = (15..28).random(),
            respondedDonorsCount = 1,
            mrnNumber = mrn,
            ocrConfidence = confidence,
            isVerified = true
        )
        _activeSeekerRequest.value = newRequest
        _requests.value = listOf(newRequest) + _requests.value
        return SlipProcessingResult(newRequest, routedToVerification = false)
    }

    private suspend fun recognizeText(context: Context, imageUri: Uri): String =
        suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromFilePath(context, imageUri))
                .addOnSuccessListener { result ->
                    recognizer.close()
                    continuation.resume(result.text)
                }
                .addOnFailureListener { exception ->
                    recognizer.close()
                    continuation.resumeWith(Result.failure(exception))
                }
            continuation.invokeOnCancellation { recognizer.close() }
        }

    // // MOCK: Simulates PostGIS geo-query to dispatch push notifications to donors within radius
    suspend fun updateDonorLocation(latitude: Double, longitude: Double): Boolean {
        val client = supabaseClient ?: return false
        val donorId = client.auth.currentUserOrNull()?.id ?: return false

        return try {
            client.from("donor_locations").upsert(
                buildJsonObject {
                    put("donor_id", donorId)
                    put("latitude", latitude)
                    put("longitude", longitude)
                    put("location", "SRID=4326;POINT($longitude $latitude)")
                    put("source", "device")
                }
            )
            true
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: "Unable to update donor location.")
            false
        }
    }

    suspend fun registerFcmToken(context: Context, token: String): Boolean {
        context.getSharedPreferences("qatra_push", Context.MODE_PRIVATE)
            .edit()
            .putString("pending_fcm_token", token)
            .apply()

        val client = supabaseClient ?: return false
        val donorId = client.auth.currentUserOrNull()?.id ?: return false
        return try {
            client.from("donor_device_tokens").upsert(
                buildJsonObject {
                    put("donor_id", donorId)
                    put("token", token)
                    put("platform", "android")
                }
            )
            true
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: "Unable to register FCM token.")
            false
        }
    }

    suspend fun registerPendingFcmToken(context: Context): Boolean {
        val token = context.getSharedPreferences("qatra_push", Context.MODE_PRIVATE)
            .getString("pending_fcm_token", null) ?: return false
        return registerFcmToken(context, token)
    }

    suspend fun dispatchProximityAlerts(requestId: String, radiusKm: Int): List<MatchedDonor> {
        val client = supabaseClient ?: return emptyList()
        val request = _requests.value.firstOrNull { it.id == requestId }
        if (requestId.toUuidOrNull() == null) return emptyList()

        return try {
            val result = client.postgrest.rpc(
                function = "find_eligible_donors_for_request",
                parameters = buildJsonObject {
                    put("p_request_id", requestId)
                    put("p_radius_km", radiusKm)
                }
            ).decodeAs<JsonArray>()

            val donors = result.mapNotNull { element ->
                val row = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                val bloodGroup = row["blood_group"]?.jsonPrimitive?.contentOrNull
                    ?.let(BloodGroup::fromString) ?: return@mapNotNull null
                MatchedDonor(
                    id = "Donor #${row["donor_id"]?.jsonPrimitive?.content ?: return@mapNotNull null}",
                    bloodGroup = bloodGroup,
                    distanceKm = row["distance_km"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                    etaMinutes = row["eta_minutes"]?.jsonPrimitive?.intOrNull ?: 0,
                    statusText = row["status_text"]?.jsonPrimitive?.contentOrNull ?: "Notified",
                    phoneMasked = row["phone_masked"]?.jsonPrimitive?.contentOrNull ?: "0300-XXXXXXX",
                    isVerified = row["is_verified"]?.jsonPrimitive?.booleanOrNull ?: false,
                    lifetimeDonations = row["lifetime_donations"]?.jsonPrimitive?.intOrNull ?: 0
                )
            }
            _matchedDonors.value = donors
            if (request != null) {
                _activeSeekerRequest.value = request.copy(activeDonorsInRadius = donors.size)
            }
            donors
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: "Unable to find eligible donors.")
            _matchedDonors.value = emptyList()
            if (request != null) {
                _activeSeekerRequest.value = request.copy(activeDonorsInRadius = 0)
            }
            emptyList()
        }
    }

    private fun String.toUuidOrNull(): java.util.UUID? = runCatching { java.util.UUID.fromString(this) }.getOrNull()

    // // MOCK: Donor accepts emergency dispatch
    // // TODO: FR 1.4 — replace with real WebSocket broadcast lock & masked proxy route creation
    fun acceptEmergencyDispatch(donorId: String, requestId: String) {
        val current = _activeSeekerRequest.value
        if (current != null) {
            _activeSeekerRequest.value = current.copy(status = RequestStatus.DONOR_MATCHED)
        }
    }

    // // MOCK: Initiates masked proxy call without exposing raw phone numbers
    // // TODO: NFR 2.3 — replace with real Twilio Voice Proxy / Asterisk Telephony bridge
    fun initiateMaskedProxyCall(callerId: String, targetId: String): String {
        return "0300-XXXXXXX" // Always masked
    }

    // // MOCK: Completes donation and starts 90-day cooldown countdown
    // // TODO: FR 2.4 — replace with real server-side Cooldown Engine with Day-85 automated push triggers
    fun completeDonation(donorId: String, rating: Int, thankYouNote: String?) {
        val currentProfile = _donorProfile.value
        _donorProfile.value = currentProfile.copy(
            isAvailableToDonate = false,
            isEligible = false,
            cooldownDaysRemaining = 90,
            lifetimeDonations = currentProfile.lifetimeDonations + 1
        )
        val activeReq = _activeSeekerRequest.value
        if (activeReq != null) {
            _activeSeekerRequest.value = activeReq.copy(status = RequestStatus.FULFILLED)
        }
    }

    fun setDonorAvailability(available: Boolean) {
        val current = _donorProfile.value
        if (current.cooldownDaysRemaining == 0) {
            _donorProfile.value = current.copy(isAvailableToDonate = available)
        }
    }

    fun setDonorCooldownDemo(days: Int) {
        val current = _donorProfile.value
        _donorProfile.value = current.copy(
            cooldownDaysRemaining = days,
            isEligible = days == 0,
            isAvailableToDonate = days == 0
        )
    }

    fun approveVerificationItem(id: String) {
        _verificationQueue.value = _verificationQueue.value.map {
            if (it.id == id) it.copy(status = "Approved") else it
        }
    }

    fun rejectVerificationItem(id: String) {
        _verificationQueue.value = _verificationQueue.value.map {
            if (it.id == id) it.copy(status = "Rejected") else it
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
}
