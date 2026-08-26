package com.qatra.app

import com.qatra.app.data.repository.AdminRepository
import com.qatra.app.data.repository.AuthRepository
import com.qatra.app.data.repository.DonorRepository
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.data.repository.SeekerRepository
import com.qatra.app.ui.AdminScreenStep
import com.qatra.app.ui.admin.AdminViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AdminViewModel {
        val repo = QatraRepository(
            AuthRepository(),
            SeekerRepository(),
            DonorRepository(),
            AdminRepository()
        )
        return AdminViewModel(repo)
    }

    // ── Initial State ──────────────────────────────────────────────────────

    @Test
    fun initialState_stepIsLogin2FA() {
        val vm = createViewModel()
        assertEquals(AdminScreenStep.LOGIN_2FA, vm.adminStep.value)
    }

    @Test
    fun initialState_isNotAuthenticated() {
        val vm = createViewModel()
        assertFalse(vm.isAdminAuthenticated.value)
    }

    @Test
    fun initialState_noAuthError() {
        val vm = createViewModel()
        assertNull(vm.adminAuthError.value)
    }

    @Test
    fun initialState_qrScanNotActive() {
        val vm = createViewModel()
        assertFalse(vm.qrScanActive.value)
    }

    @Test
    fun initialState_totpNotRequired() {
        val vm = createViewModel()
        assertFalse(vm.isTotpRequired.value)
    }

    // ── Step Transitions ───────────────────────────────────────────────────

    @Test
    fun setStep_changesToVerificationQueue() {
        val vm = createViewModel()
        vm.setStep(AdminScreenStep.VERIFICATION_QUEUE)
        assertEquals(AdminScreenStep.VERIFICATION_QUEUE, vm.adminStep.value)
    }

    @Test
    fun setStep_changesToFraudAudit() {
        val vm = createViewModel()
        vm.setStep(AdminScreenStep.FRAUD_AUDIT)
        assertEquals(AdminScreenStep.FRAUD_AUDIT, vm.adminStep.value)
    }

    @Test
    fun setStep_changesToDriveManagement() {
        val vm = createViewModel()
        vm.setStep(AdminScreenStep.DRIVE_MANAGEMENT)
        assertEquals(AdminScreenStep.DRIVE_MANAGEMENT, vm.adminStep.value)
    }

    @Test
    fun setStep_changesThroughMultipleSteps() {
        val vm = createViewModel()
        vm.setStep(AdminScreenStep.VERIFICATION_QUEUE)
        assertEquals(AdminScreenStep.VERIFICATION_QUEUE, vm.adminStep.value)

        vm.setStep(AdminScreenStep.FRAUD_AUDIT)
        assertEquals(AdminScreenStep.FRAUD_AUDIT, vm.adminStep.value)

        vm.setStep(AdminScreenStep.DRIVE_MANAGEMENT)
        assertEquals(AdminScreenStep.DRIVE_MANAGEMENT, vm.adminStep.value)
    }

    // ── Admin Login State ──────────────────────────────────────────────────

    @Test
    fun adminSignIn_withoutSupabaseClient_returnsFalse() = runTest {
        val vm = createViewModel()
        // Without a real Supabase client, sign-in returns false and sets error
        val result = vm.adminSignIn("admin@qatra.pk", "password123", "000000")
        assertFalse(result)
        assertFalse(vm.isAdminAuthenticated.value)
        assertNotNull(vm.adminAuthError.value)
    }

    @Test
    fun adminSignOut_clearsAuthState() = runTest {
        val vm = createViewModel()
        // Even without Supabase, signOut resets local state
        vm.adminSignOut()
        assertFalse(vm.isAdminAuthenticated.value)
        assertNull(vm.adminAuthError.value)
    }

    @Test
    fun adminSignIn_errorMessageIsSetOnFailure() = runTest {
        val vm = createViewModel()
        vm.adminSignIn("bad@email.com", "wrong", "000000")
        // Error should be non-null after failed sign-in
        val errorMsg = vm.adminAuthError.value
        assertNotNull(errorMsg)
    }

    // ── Campus Drive Scheduling ────────────────────────────────────────────

    @Test
    fun scheduleDrive_doesNotThrow() {
        val vm = createViewModel()
        // scheduleDrive is fire-and-forget; just verify no crash
        vm.scheduleDrive(
            title = "Blood Drive 2026",
            venue = "DUET Main Campus",
            targetQuota = 100,
            dateStr = "2026-09-15",
            timeStr = "09:00"
        )
    }
}
