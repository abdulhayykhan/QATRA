package com.qatra.app.data.repository

import android.content.Context
import com.qatra.app.data.model.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DonorRepository {

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
         * digit — this does NOT confirm the CNIC is real or belongs to the submitting
         * user. Real identity confirmation requires NADRA Verisys integration, which is
         * out of scope for this build.
         * TODO: NADRA Verisys integration for production identity verification.
         */
        fun validatePakistaniCnic(cnicNumber: String): Boolean {
            val normalized = cnicNumber.trim()
            if (normalized.length != 13 || normalized.any { !it.isDigit() }) return false

            val provincePrefix = normalized.substring(0, 2).toIntOrNull() ?: return false
            return validProvinceDistrictRanges.any { provincePrefix in it }
        }
    }

    /**
     * Reference to the SeekerRepository's active request flow, set by the facade
     * so donor operations (accept dispatch, complete donation) can update it.
     */
    internal var activeSeekerRequestRef: MutableStateFlow<BloodRequest?>? = null

    var lastAuthErrorMessage: String? = null
        private set

    private fun setLastAuthError(message: String?) {
        lastAuthErrorMessage = message
    }

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

    // MOCK: Simulates PostGIS geo-query to update donor location for proximity matching.
    suspend fun updateDonorLocation(latitude: Double, longitude: Double): Boolean {
        val client = SupabaseClientProvider.client ?: return false
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

        val client = SupabaseClientProvider.client ?: return false
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

    // FR 1.4 — donor accepts an emergency dispatch. The durable side is the
    // accept_emergency_dispatch RPC (SECURITY DEFINER, keyed on auth.uid()): it records
    // the responder in matched_donor_requests and locks the request to DONOR_MATCHED.
    // Local state advances optimistically first so the demo flow continues even with no
    // live session; the DB write is best effort.
    suspend fun acceptEmergencyDispatch(requestId: String): Boolean {
        activeSeekerRequestRef?.value?.let { current ->
            activeSeekerRequestRef?.value = current.copy(status = RequestStatus.DONOR_MATCHED)
        }
        val client = SupabaseClientProvider.client ?: return false
        if (client.auth.currentUserOrNull() == null) return false
        if (requestId.toUuidOrNull() == null) return false
        return try {
            client.postgrest.rpc(
                function = "accept_emergency_dispatch",
                parameters = buildJsonObject { put("p_request_id", requestId) }
            )
            true
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: "Unable to accept dispatch.")
            false
        }
    }

    // FR 2.4 — donor completes a donation. The durable side is the complete_donation RPC
    // (SECURITY DEFINER, keyed on auth.uid()): it starts the 90-day cooldown and increments
    // lifetime donations. rating/thankYouNote have no backend sink yet, so they stay in
    // local state only. Local state advances optimistically first.
    suspend fun completeDonation(rating: Int, thankYouNote: String?): Boolean {
        val currentProfile = _donorProfile.value
        _donorProfile.value = currentProfile.copy(
            isAvailableToDonate = false,
            isEligible = false,
            cooldownDaysRemaining = 90,
            lifetimeDonations = currentProfile.lifetimeDonations + 1
        )
        activeSeekerRequestRef?.value?.let { activeReq ->
            activeSeekerRequestRef?.value = activeReq.copy(status = RequestStatus.FULFILLED)
        }
        val client = SupabaseClientProvider.client ?: return false
        if (client.auth.currentUserOrNull() == null) return false
        return try {
            client.postgrest.rpc(
                function = "complete_donation",
                parameters = buildJsonObject { }
            )
            true
        } catch (exception: Exception) {
            setLastAuthError(exception.message ?: "Unable to complete donation.")
            false
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

    private fun String.toUuidOrNull(): java.util.UUID? =
        runCatching { java.util.UUID.fromString(this) }.getOrNull()
}
