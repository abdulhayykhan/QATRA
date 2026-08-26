package com.qatra.app

import com.qatra.app.data.repository.AdminRepository
import com.qatra.app.data.repository.AuthRepository
import com.qatra.app.data.repository.DonorRepository
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.data.repository.SeekerRepository
import com.qatra.app.ui.DonorScreenStep
import com.qatra.app.ui.donor.DonorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DonorViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DonorViewModel {
        val repo = QatraRepository(
            AuthRepository(),
            SeekerRepository(),
            DonorRepository(),
            AdminRepository()
        )
        return DonorViewModel(repo)
    }

    // ── Initial State ──────────────────────────────────────────────────────

    @Test
    fun initialState_stepIsCnicUpload() {
        val vm = createViewModel()
        assertEquals(DonorScreenStep.CNIC_UPLOAD, vm.donorStep.value)
    }

    @Test
    fun initialState_geoAlertModalIsHidden() {
        val vm = createViewModel()
        assertFalse(vm.showGeoAlertModal.value)
        assertNull(vm.geoAlertPayload.value)
    }

    @Test
    fun initialState_feedbackDefaults() {
        val vm = createViewModel()
        assertEquals(5, vm.feedbackRating.value)
        assertEquals("", vm.feedbackNote.value)
    }

    @Test
    fun initialState_cnicImageUrisAreNull() {
        val vm = createViewModel()
        assertNull(vm.donorFrontImageUri.value)
        assertNull(vm.donorBackImageUri.value)
    }

    @Test
    fun initialState_mapRadiusIsTenKm() {
        val vm = createViewModel()
        assertEquals(10, vm.selectedMapRadiusKm.value)
    }

    // ── Step Transitions ───────────────────────────────────────────────────

    @Test
    fun setStep_changesToPreScreening() {
        val vm = createViewModel()
        vm.setStep(DonorScreenStep.PRE_SCREENING)
        assertEquals(DonorScreenStep.PRE_SCREENING, vm.donorStep.value)
    }

    @Test
    fun setStep_changesToHomeDashboard() {
        val vm = createViewModel()
        vm.setStep(DonorScreenStep.HOME_DASHBOARD)
        assertEquals(DonorScreenStep.HOME_DASHBOARD, vm.donorStep.value)
    }

    @Test
    fun setStep_changesThroughMultipleSteps() {
        val vm = createViewModel()
        vm.setStep(DonorScreenStep.PRE_SCREENING)
        assertEquals(DonorScreenStep.PRE_SCREENING, vm.donorStep.value)

        vm.setStep(DonorScreenStep.HOME_DASHBOARD)
        assertEquals(DonorScreenStep.HOME_DASHBOARD, vm.donorStep.value)

        vm.setStep(DonorScreenStep.INTERACTIVE_MAP)
        assertEquals(DonorScreenStep.INTERACTIVE_MAP, vm.donorStep.value)
    }

    // ── CNIC Validation ────────────────────────────────────────────────────

    @Test
    fun validCnic_thirteenDigitsPassesValidation() {
        assertTrue(QatraRepository.validatePakistaniCnic("4210112345671"))
    }

    @Test
    fun validCnic_withDashesStrippedPasses() {
        assertTrue(QatraRepository.validatePakistaniCnic("42101-1234567-1".replace("-", "")))
    }

    @Test
    fun invalidCnic_tooShort() {
        assertFalse(QatraRepository.validatePakistaniCnic("421011234567"))
    }

    @Test
    fun invalidCnic_nonNumeric() {
        assertFalse(QatraRepository.validatePakistaniCnic("421011234567A"))
    }

    @Test
    fun invalidCnic_outOfRangeDistrictCode() {
        assertFalse(QatraRepository.validatePakistaniCnic("9010112345678"))
    }

    @Test
    fun invalidCnic_empty() {
        assertFalse(QatraRepository.validatePakistaniCnic(""))
    }

    // ── CNIC Number State ──────────────────────────────────────────────────

    @Test
    fun donorCnicNumber_initialValueSet() {
        val vm = createViewModel()
        assertEquals("42101-9876543-7", vm.donorCnicNumber.value)
    }

    @Test
    fun donorCnicNumber_canBeUpdated() {
        val vm = createViewModel()
        vm.donorCnicNumber.value = "42101-1234567-1"
        assertEquals("42101-1234567-1", vm.donorCnicNumber.value)
    }

    // ── Dispatch Acceptance ────────────────────────────────────────────────

    @Test
    fun donorAcceptDispatch_hidesGeoAlertAndNavigates() {
        val vm = createViewModel()
        vm.showGeoAlertModal.value = true
        vm.setStep(DonorScreenStep.HOME_DASHBOARD)

        vm.donorAcceptDispatch()

        assertFalse(vm.showGeoAlertModal.value)
        assertEquals(DonorScreenStep.NAVIGATION_ROUTING, vm.donorStep.value)
    }

    // ── Donation Completion ────────────────────────────────────────────────

    @Test
    fun donorFinishDonation_navigatesToComplete() {
        val vm = createViewModel()
        vm.setStep(DonorScreenStep.NAVIGATION_ROUTING)

        vm.donorFinishDonation()

        assertEquals(DonorScreenStep.DONATION_COMPLETE, vm.donorStep.value)
    }
}
