package com.qatra.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qatra.app.data.repository.AdminRepository
import com.qatra.app.data.repository.AuthRepository
import com.qatra.app.data.repository.DonorRepository
import com.qatra.app.data.repository.QatraRepository
import com.qatra.app.data.repository.SeekerRepository
import com.qatra.app.ui.QatraViewModel
import com.qatra.app.ui.SharedAuthViewModel
import com.qatra.app.ui.admin.AdminViewModel
import com.qatra.app.ui.donor.DonorCnicUploadScreen
import com.qatra.app.ui.donor.DonorViewModel
import com.qatra.app.ui.donor.PreScreeningChecklistScreen
import com.qatra.app.ui.seeker.SeekerViewModel
import com.qatra.app.ui.theme.QatraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the Donor flow critical-path screens.
 *
 * Each test renders the composable in isolation with [QatraTheme] and
 * verifies that key interactive elements are visible via their testTag.
 * No navigation or network calls are exercised.
 */
@RunWith(AndroidJUnit4::class)
class DonorFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Helper: build a real QatraViewModel backed by in-memory repositories ──

    private fun createViewModel(): QatraViewModel {
        val repo = QatraRepository(
            AuthRepository(),
            SeekerRepository(),
            DonorRepository(),
            AdminRepository()
        )
        return QatraViewModel(
            authVm = SharedAuthViewModel(repo),
            seekerVm = SeekerViewModel(repo),
            donorVm = DonorViewModel(repo),
            adminVm = AdminViewModel(repo)
        )
    }

    // ── 1. DonorCnicUploadScreen ──────────────────────────────────────────────

    @Test
    fun donorCnicScreen_cnicInput_isDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                DonorCnicUploadScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithTag("input_donor_cnic").assertIsDisplayed()
    }

    @Test
    fun donorCnicScreen_submitButton_isDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                DonorCnicUploadScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithTag("btn_submit_donor_cnic").assertIsDisplayed()
    }

    @Test
    fun donorCnicScreen_screenTitle_isVisible() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                DonorCnicUploadScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Donor CNIC Format Check").assertIsDisplayed()
    }

    @Test
    fun donorCnicScreen_encryptionCallout_isVisible() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                DonorCnicUploadScreen(viewModel = viewModel)
            }
        }
        composeTestRule
            .onNodeWithText("Data is AES-256 encrypted in an isolated vault and never exposed publicly.")
            .assertIsDisplayed()
    }

    // ── 2. PreScreeningChecklistScreen ────────────────────────────────────────

    @Test
    fun preScreeningScreen_continueButton_isDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                PreScreeningChecklistScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithTag("btn_health_continue").assertIsDisplayed()
    }
}
