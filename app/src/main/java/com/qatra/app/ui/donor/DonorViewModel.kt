package com.qatra.app.ui.donor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.qatra.app.data.model.Hospital
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.ui.DonorScreenStep
import com.qatra.app.notifications.GeoAlertPayload
import com.qatra.app.notifications.QatraPushState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Donor-specific ViewModel managing:
 * - Screen step navigation (DonorScreenStep)
 * - CNIC upload & pre-screening
 * - Dashboard & availability toggle
 * - Geo-alert reception & dispatch acceptance
 * - Map/navigation state
 * - Donation completion
 */
class DonorViewModel(
    val repository: QatraRepository
) : ViewModel() {

    // ── Step Navigation ──────────────────────────────────────────────────────
    private val _donorStep = MutableStateFlow(DonorScreenStep.CNIC_UPLOAD)
    val donorStep: StateFlow<DonorScreenStep> = _donorStep.asStateFlow()

    fun setStep(step: DonorScreenStep) {
        _donorStep.value = step
    }

    // ── Session Expiry (forwarded from repository) ──────────────────────────
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(replay = 0)
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    // ── CNIC Upload State ───────────────────────────────────────────────────
    val donorCnicNumber = MutableStateFlow("42101-9876543-7")
    val donorFrontImageUri = MutableStateFlow<Uri?>(null)
    val donorBackImageUri = MutableStateFlow<Uri?>(null)

    // ── Geo-Alert State ─────────────────────────────────────────────────────
    val showGeoAlertModal = MutableStateFlow(false)
    val geoAlertPayload = MutableStateFlow<GeoAlertPayload?>(null)

    // ── Map State ───────────────────────────────────────────────────────────
    val selectedMapRadiusKm = MutableStateFlow(10) // 5, 10, 15
    val selectedHospitalForMap = MutableStateFlow<Hospital?>(repository.hospitals[0])

    // ── Feedback (donation completion) ──────────────────────────────────────
    val feedbackRating = MutableStateFlow(5)
    val feedbackNote = MutableStateFlow("")

    // ── Location Tracking ───────────────────────────────────────────────────
    private var donorLocationJob: Job? = null

    init {
        viewModelScope.launch {
            QatraPushState.latestGeoAlert.collect { alert ->
                if (alert != null) {
                    geoAlertPayload.value = alert
                    showGeoAlertModal.value = true
                }
            }
        }
        // Forward session-expired events from the donor repository
        viewModelScope.launch {
            repository.donorRepository.sessionExpiredEvent.collect {
                _sessionExpiredEvent.emit(Unit)
            }
        }
    }

    fun consumePendingGeoAlert(context: Context) {
        QatraPushState.consume(context)?.let { alert ->
            geoAlertPayload.value = alert
            showGeoAlertModal.value = true
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
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) return null

        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        return suspendCancellableCoroutine { continuation ->
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            )
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
        }
    }

    suspend fun uploadDonorCnicDocuments(context: Context): Boolean {
        val frontUri = donorFrontImageUri.value
        val backUri = donorBackImageUri.value
        if (frontUri == null || backUri == null) return false
        val frontUploaded = repository.uploadPrivateDocument(context, frontUri, "cnic-documents")
        return frontUploaded && repository.uploadPrivateDocument(context, backUri, "cnic-documents")
    }

    fun donorAcceptDispatch() {
        val previousStep = _donorStep.value
        val previousShowModal = showGeoAlertModal.value
        val previousPayload = geoAlertPayload.value

        showGeoAlertModal.value = false
        val requestId = geoAlertPayload.value?.requestId
        _donorStep.value = DonorScreenStep.NAVIGATION_ROUTING

        if (requestId != null) {
            viewModelScope.launch {
                val success = repository.acceptEmergencyDispatch(requestId)
                if (!success) {
                    _donorStep.value = previousStep
                    showGeoAlertModal.value = previousShowModal
                    geoAlertPayload.value = previousPayload
                    Timber.w("Dispatch acceptance failed, rolled back donor step to %s", previousStep)
                }
            }
        } else {
            // No request ID — nothing to persist, no rollback needed
        }
    }

    fun donorFinishDonation() {
        val previousStep = _donorStep.value
        val note = feedbackNote.value.ifBlank { "Donation completed via QATRA emergency dispatch." }

        _donorStep.value = DonorScreenStep.DONATION_COMPLETE

        viewModelScope.launch {
            val success = repository.completeDonation(feedbackRating.value, note)
            if (!success) {
                _donorStep.value = previousStep
                Timber.w("Donation completion failed, rolled back donor step to %s", previousStep)
            }
        }
    }

    override fun onCleared() {
        donorLocationJob?.cancel()
        super.onCleared()
    }
}
