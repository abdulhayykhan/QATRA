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

    /** Whether the admin has passed email/password but still needs TOTP verification. */
    private val _isTotpRequired = MutableStateFlow(false)
    val isTotpRequired: StateFlow<Boolean> = _isTotpRequired.asStateFlow()

    /** Stores the MFA factor IDs that need TOTP verification. */
    private var pendingMfaFactorIds: List<String> = emptyList()

    // ── QR Scan State ───────────────────────────────────────────────────────
    val qrScanActive = MutableStateFlow(false)

    // ── Auth Actions ────────────────────────────────────────────────────────

    suspend fun adminSignIn(email: String, password: String, totpCode: String): Boolean {
        // Phase 6.4: Two-step admin authentication.
        // Step 1 — email/password against Supabase.
        val passwordOk = repository.adminSignIn(email, password)
        if (!passwordOk) {
            adminAuthError.value = repository.lastAuthErrorMessage ?: "Invalid credentials."
            _isAdminAuthenticated.value = false
            return false
        }

        // Step 2 — check for enrolled MFA factors and enforce TOTP.
        val factorIds = repository.getAdminMfaFactors()
        if (factorIds.isNotEmpty()) {
            pendingMfaFactorIds = factorIds
            // If the user already provided a TOTP code, verify immediately.
            if (totpCode.isNotBlank()) {
                return verifyTotp(totpCode)
            }
            // Otherwise signal the UI to prompt for TOTP.
            _isTotpRequired.value = true
            adminAuthError.value = null
            // Don't authenticate yet — TOTP pending.
            return false
        }

        // No MFA factors enrolled — password auth alone is sufficient.
        adminAuthError.value = null
        _isAdminAuthenticated.value = true
        return true
    }

    /**
     * Called from the UI when the user submits the TOTP code after
     * [adminSignIn] set [isTotpRequired] to true.
     */
    suspend fun verifyTotp(code: String): Boolean {
        val factorId = pendingMfaFactorIds.firstOrNull()
        if (factorId == null) {
            adminAuthError.value = "No MFA factor available for verification."
            return false
        }
        val ok = repository.verifyAdminTotp(factorId, code)
        if (ok) {
            adminAuthError.value = null
            _isTotpRequired.value = false
            _isAdminAuthenticated.value = true
            pendingMfaFactorIds = emptyList()
        } else {
            adminAuthError.value = "Invalid TOTP code. Please try again."
        }
        return ok
    }

    suspend fun adminSignOut() {
        repository.adminSignOut()
        _isAdminAuthenticated.value = false
        _isTotpRequired.value = false
        pendingMfaFactorIds = emptyList()
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
