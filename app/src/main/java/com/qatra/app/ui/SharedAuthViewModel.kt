package com.qatra.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qatra.app.data.repository.QatraRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared authentication ViewModel managing cross-cutting concerns:
 * - Phone OTP sending / verification
 * - Role-based navigation (which flow is active)
 * - Push-token registration
 * - Common main-tab state
 */
class SharedAuthViewModel(
    val repository: QatraRepository
) : ViewModel() {

    // ── Active Navigation Flow ──────────────────────────────────────────────
    private val _activeFlow = MutableStateFlow(FlowType.SEEKER)
    val activeFlow: StateFlow<FlowType> = _activeFlow.asStateFlow()

    val mainTab = MutableStateFlow(MainTab.EMERGENCY)

    // ── Auth & OTP State ────────────────────────────────────────────────────
    val phoneNumber = MutableStateFlow("300 1234567")
    val otpCode = MutableStateFlow("")

    private val _otpTimerSeconds = MutableStateFlow(180)
    val otpTimerSeconds: StateFlow<Int> = _otpTimerSeconds.asStateFlow()
    private var otpTimerJob: Job? = null

    init {
        startOtpCountdown()
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

    suspend fun verifyOtpAndContinue(code: String, onVerified: () -> Unit): Boolean {
        val isValid = repository.verifyOtp(code)
        if (isValid) onVerified()
        return isValid
    }

    fun setActiveFlow(flow: FlowType) {
        _activeFlow.value = flow
    }

    fun enterMainShell(initialTab: MainTab = MainTab.EMERGENCY) {
        mainTab.value = initialTab
        _activeFlow.value = FlowType.MAIN_SHELL
    }

    // ── Push Token ──────────────────────────────────────────────────────────
    fun registerPendingPushToken(context: Context) {
        viewModelScope.launch { repository.registerPendingFcmToken(context) }
    }

    fun registerPushToken(context: Context, token: String) {
        viewModelScope.launch { repository.registerFcmToken(context, token) }
    }

    override fun onCleared() {
        otpTimerJob?.cancel()
        super.onCleared()
    }
}
