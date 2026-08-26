package com.qatra.app

import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.ui.QatraViewModel
import com.qatra.app.ui.SeekerScreenStep
import com.qatra.app.ui.SharedAuthViewModel
import com.qatra.app.ui.seeker.SeekerViewModel
import com.qatra.app.ui.donor.DonorViewModel
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QatraViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): QatraViewModel {
        val repo = QatraRepository()
        return QatraViewModel(
            authVm = SharedAuthViewModel(repo),
            seekerVm = SeekerViewModel(repo),
            donorVm = DonorViewModel(repo),
            adminVm = AdminViewModel(repo)
        )
    }

    @Test
    fun emptyOtp_doesNotAdvanceToRoleProfile() = runTest {
        val viewModel = createViewModel()
        viewModel.setSeekerStep(SeekerScreenStep.PHONE_VERIFICATION)

        val verified = viewModel.verifyOtpAndContinue("")

        assertFalse(verified)
        assertEquals(SeekerScreenStep.PHONE_VERIFICATION, viewModel.seekerStep.value)
    }

    @Test
    fun malformedOtp_doesNotAdvanceToRoleProfile() = runTest {
        val viewModel = createViewModel()
        viewModel.setSeekerStep(SeekerScreenStep.PHONE_VERIFICATION)

        val verified = viewModel.verifyOtpAndContinue("12AB")

        assertFalse(verified)
        assertEquals(SeekerScreenStep.PHONE_VERIFICATION, viewModel.seekerStep.value)
    }
}
