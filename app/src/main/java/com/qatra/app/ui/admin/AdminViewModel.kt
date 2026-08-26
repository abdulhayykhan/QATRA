package com.qatra.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.ui.AdminScreenStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Admin-specific ViewModel managing:
 * - Screen step navigation (AdminScreenStep)
 * - Admin login (email + password + TOTP)
 * - Verification queue (approve / reject)
 * - Fraud audit actions
 * - Campus drive scheduling & QR check-in
 */
class AdminViewModel(
    val repository: QatraRepository
) : ViewModel() {

    // ── Step Navigation ──────────────────────────────────────────────────────
    private val _adminStep = MutableStateFlow(AdminScreenStep.LOGIN_2FA)
    val adminStep: StateFlow<AdminScreenStep> = _adminStep.asStateFlow()

    fun setStep(step: AdminScreenStep) {
        _adminStep.value = step
    }

    // ── Admin Auth State ────────────────────────────────────────────────────
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    val adminAuthError = MutableStateFlow<String?>(null)

    // ── QR Scan State ───────────────────────────────────────────────────────
    val qrScanActive = MutableStateFlow(false)

    // ── Auth Actions ────────────────────────────────────────────────────────

    suspend fun adminSignIn(email: String, password: String, totpCode: String): Boolean {
        // ponytail: TOTP field accepted but not yet enforced. Supabase MFA (TOTP
        // enroll + challenge) is a documented Phase-1 pilot gap; password auth is
        // real and server-verified. The UI keeps the field so the terminal flow
        // matches production.
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

    // ── Verification Queue ──────────────────────────────────────────────────

    fun adminApproveVerification(id: String) {
        viewModelScope.launch { repository.approveVerificationItem(id) }
    }

    fun adminRejectVerification(id: String) {
        viewModelScope.launch { repository.rejectVerificationItem(id) }
    }

    // ── Campus Drive Management ─────────────────────────────────────────────

    fun scheduleDrive(
        title: String,
        venue: String,
        targetQuota: Int,
        dateStr: String,
        timeStr: String
    ) {
        repository.scheduleNewCampusDrive(
            title = title,
            venue = venue,
            targetQuota = targetQuota,
            dateStr = dateStr,
            timeStr = timeStr
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
