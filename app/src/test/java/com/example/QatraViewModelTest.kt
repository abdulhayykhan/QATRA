package com.example

import com.example.ui.QatraViewModel
import com.example.ui.SeekerScreenStep
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

    @Test
    fun emptyOtp_doesNotAdvanceToRoleProfile() = runTest {
        val viewModel = QatraViewModel()
        viewModel.setSeekerStep(SeekerScreenStep.PHONE_VERIFICATION)

        val verified = viewModel.verifyOtpAndContinue("")

        assertFalse(verified)
        assertEquals(SeekerScreenStep.PHONE_VERIFICATION, viewModel.seekerStep.value)
    }

    @Test
    fun malformedOtp_doesNotAdvanceToRoleProfile() = runTest {
        val viewModel = QatraViewModel()
        viewModel.setSeekerStep(SeekerScreenStep.PHONE_VERIFICATION)

        val verified = viewModel.verifyOtpAndContinue("12AB")

        assertFalse(verified)
        assertEquals(SeekerScreenStep.PHONE_VERIFICATION, viewModel.seekerStep.value)
    }
}