package com.qatra.app

import com.qatra.app.data.model.BloodGroup
import com.qatra.app.data.model.BloodComponent
import com.qatra.app.data.model.UrgencyLevel
import com.qatra.app.data.repository.AdminRepository
import com.qatra.app.data.repository.AuthRepository
import com.qatra.app.data.repository.DonorRepository
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.data.repository.SeekerRepository
import com.qatra.app.ui.SeekerScreenStep
import com.qatra.app.ui.seeker.SeekerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeekerViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SeekerViewModel {
        val repo = QatraRepository(
            AuthRepository(),
            SeekerRepository(),
            DonorRepository(),
            AdminRepository()
        )
        return SeekerViewModel(repo)
    }

    // ── Initial State ──────────────────────────────────────────────────────

    @Test
    fun initialState_stepIsSplash() {
        val vm = createViewModel()
        assertEquals(SeekerScreenStep.SPLASH, vm.seekerStep.value)
    }

    @Test
    fun initialState_bloodGroupIsONeg() {
        val vm = createViewModel()
        assertEquals(BloodGroup.O_NEG, vm.selectedBloodGroup.value)
    }

    @Test
    fun initialState_componentIsPRBC() {
        val vm = createViewModel()
        assertEquals(BloodComponent.PRBC, vm.selectedComponent.value)
    }

    @Test
    fun initialState_unitsRequiredIsTwo() {
        val vm = createViewModel()
        assertEquals(2, vm.unitsRequired.value)
    }

    @Test
    fun initialState_urgencyIsHighPriority() {
        val vm = createViewModel()
        assertEquals(UrgencyLevel.HIGH_PRIORITY, vm.selectedUrgency.value)
    }

    @Test
    fun initialState_proxyCallIsNotActive() {
        val vm = createViewModel()
        assertFalse(vm.isProxyCallActive.value)
        assertEquals(0, vm.proxyCallSecondsRemaining.value)
    }

    @Test
    fun initialState_ocrFlagsAreFalse() {
        val vm = createViewModel()
        assertFalse(vm.isOcrProcessing.value)
        assertFalse(vm.ocrStep1Completed.value)
        assertFalse(vm.ocrStep2Completed.value)
    }

    // ── Step Transitions ───────────────────────────────────────────────────

    @Test
    fun setStep_changesToPhoneVerification() {
        val vm = createViewModel()
        vm.setStep(SeekerScreenStep.PHONE_VERIFICATION)
        assertEquals(SeekerScreenStep.PHONE_VERIFICATION, vm.seekerStep.value)
    }

    @Test
    fun setStep_changesToRequestCreation() {
        val vm = createViewModel()
        vm.setStep(SeekerScreenStep.REQUEST_CREATION)
        assertEquals(SeekerScreenStep.REQUEST_CREATION, vm.seekerStep.value)
    }

    @Test
    fun setStep_changesToSlipUpload() {
        val vm = createViewModel()
        vm.setStep(SeekerScreenStep.SLIP_UPLOAD)
        assertEquals(SeekerScreenStep.SLIP_UPLOAD, vm.seekerStep.value)
    }

    @Test
    fun setStep_changesThroughMultipleSteps() {
        val vm = createViewModel()
        vm.setStep(SeekerScreenStep.PHONE_VERIFICATION)
        assertEquals(SeekerScreenStep.PHONE_VERIFICATION, vm.seekerStep.value)

        vm.setStep(SeekerScreenStep.ROLE_PROFILE)
        assertEquals(SeekerScreenStep.ROLE_PROFILE, vm.seekerStep.value)

        vm.setStep(SeekerScreenStep.REQUEST_CREATION)
        assertEquals(SeekerScreenStep.REQUEST_CREATION, vm.seekerStep.value)
    }

    // ── Blood Group Selection ──────────────────────────────────────────────

    @Test
    fun bloodGroupSelection_updatesState() {
        val vm = createViewModel()
        vm.selectedBloodGroup.value = BloodGroup.AB_NEG
        assertEquals(BloodGroup.AB_NEG, vm.selectedBloodGroup.value)
    }

    @Test
    fun bloodGroupSelection_updatesToBPos() {
        val vm = createViewModel()
        vm.selectedBloodGroup.value = BloodGroup.B_POS
        assertEquals(BloodGroup.B_POS, vm.selectedBloodGroup.value)
    }

    // ── Proxy Call State ───────────────────────────────────────────────────

    @Test
    fun startProxyCallCountdown_setsActiveAndTimer() {
        val vm = createViewModel()
        vm.startProxyCallCountdown()
        assertTrue(vm.isProxyCallActive.value)
        assertEquals(300, vm.proxyCallSecondsRemaining.value)
    }

    @Test
    fun endProxyCall_resetsState() {
        val vm = createViewModel()
        vm.startProxyCallCountdown()
        assertTrue(vm.isProxyCallActive.value)

        vm.endProxyCall()
        assertFalse(vm.isProxyCallActive.value)
        assertEquals(0, vm.proxyCallSecondsRemaining.value)
    }

    // ── Feedback State ─────────────────────────────────────────────────────

    @Test
    fun initialState_feedbackDefaults() {
        val vm = createViewModel()
        assertEquals(5, vm.feedbackRating.value)
        assertEquals("", vm.feedbackNote.value)
    }

    @Test
    fun submitSeekerFeedback_resetsToSplash() {
        val vm = createViewModel()
        vm.setStep(SeekerScreenStep.CONFIRMATION)
        assertEquals(SeekerScreenStep.CONFIRMATION, vm.seekerStep.value)

        vm.submitSeekerFeedback()
        assertEquals(SeekerScreenStep.SPLASH, vm.seekerStep.value)
    }
}
