package com.qatra.app.data.repository

import android.content.Context
import android.net.Uri
import com.qatra.app.data.model.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import timber.log.Timber

class SeekerRepository {

    // ── Session Expiry Detection ────────────────────────────────────────────
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(replay = 0)
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    data class SlipProcessingResult(
        val request: BloodRequest?,
        val routedToVerification: Boolean,
        val errorMessage: String? = null,
        val newVerificationItem: VerificationQueueItem? = null
    )

    private val _requests = MutableStateFlow<List<BloodRequest>>(emptyList())
    val requests: StateFlow<List<BloodRequest>> = _requests.asStateFlow()

    private val _matchedDonors = MutableStateFlow<List<MatchedDonor>>(emptyList())
    val matchedDonors: StateFlow<List<MatchedDonor>> = _matchedDonors.asStateFlow()

    private val _activeSeekerRequest = MutableStateFlow<BloodRequest?>(null)
    val activeSeekerRequest: StateFlow<BloodRequest?> = _activeSeekerRequest.asStateFlow()

    /** Mutable reference so sibling repositories (DonorRepository) can update the active request. */
    internal val activeSeekerRequestFlow: MutableStateFlow<BloodRequest?> = _activeSeekerRequest

    private val hospitals: List<Hospital> = HospitalCatalog.hospitals

    fun seedRequests() {
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
    }

    suspend fun uploadPrivateDocument(context: Context, imageUri: Uri, bucket: String): Boolean {
        val client = SupabaseClientProvider.client ?: return false

        return try {
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("The selected image could not be read.")
            val objectPath = "${System.currentTimeMillis()}-${imageUri.lastPathSegment ?: "document"}"
            client.storage.from(bucket).upload(objectPath, bytes)
            true
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to upload private document")
            if (isAuthError(exception)) _sessionExpiredEvent.tryEmit(Unit)
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
            return SlipProcessingResult(
                null,
                routedToVerification = true,
                errorMessage = exception.message ?: "Unable to read the hospital slip."
            )
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
            return SlipProcessingResult(null, routedToVerification = false, errorMessage = "Document upload failed.")
        }

        if (confidence < 85 || recognizedHospital == null || mrn == null) {
            val verificationItem = VerificationQueueItem(
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
            return SlipProcessingResult(
                request = null,
                routedToVerification = true,
                newVerificationItem = verificationItem
            )
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

    suspend fun dispatchProximityAlerts(requestId: String, radiusKm: Int): List<MatchedDonor> {
        val client = SupabaseClientProvider.client ?: return emptyList()
        val request = _requests.value.firstOrNull { it.id == requestId }
        if (requestId.toUuidOrNull() == null) return emptyList()

        return try {
            val responseJson = client.postgrest.rpc(
                function = "find_eligible_donors_for_request",
                parameters = buildJsonObject {
                    put("p_request_id", requestId)
                    put("p_radius_km", radiusKm)
                }
            ).data
            val result = Json.decodeFromString<JsonArray>(responseJson)

            val donors = result.mapNotNull { element ->
                val row = element as? JsonObject ?: return@mapNotNull null
                val bg = row["blood_group"]?.jsonPrimitive?.contentOrNull
                    ?.let(BloodGroup::fromString) ?: return@mapNotNull null
                MatchedDonor(
                    id = "Donor #${row["donor_id"]?.jsonPrimitive?.content ?: return@mapNotNull null}",
                    bloodGroup = bg,
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
            Timber.e(exception, "Failed to dispatch proximity alerts for request %s", requestId)
            if (isAuthError(exception)) _sessionExpiredEvent.tryEmit(Unit)
            _matchedDonors.value = emptyList()
            if (request != null) {
                _activeSeekerRequest.value = request.copy(activeDonorsInRadius = 0)
            }
            emptyList()
        }
    }

    private fun String.toUuidOrNull(): java.util.UUID? =
        runCatching { java.util.UUID.fromString(this) }.getOrNull()

    /**
     * Detects whether an exception represents an authentication/authorization failure
     * (HTTP 401 or 403) that indicates the session has expired and cannot be refreshed.
     */
    internal fun isAuthError(exception: Exception): Boolean {
        val message = exception.message ?: return false
        return message.contains("401") || message.contains("403") ||
            message.contains("Unauthorized", ignoreCase = true) ||
            message.contains("Forbidden", ignoreCase = true) ||
            message.contains("invalid_jwt", ignoreCase = true) ||
            message.contains("token_refresh_failed", ignoreCase = true)
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
}
