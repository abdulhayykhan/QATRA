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
import com.qatra.app.ui.donor.DonorViewModel
import com.qatra.app.ui.seeker.SeekerViewModel
import com.qatra.app.ui.seeker.SplashScreen
import com.qatra.app.ui.seeker.RoleSelectionProfileScreen
import com.qatra.app.ui.seeker.RequestCreationScreen
import com.qatra.app.ui.theme.QatraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the Seeker flow critical-path screens.
 *
 * Each test renders the composable in isolation with [QatraTheme] and
 * verifies that key interactive elements are visible via their testTag.
 * No navigation or network calls are exercised.
 */
@RunWith(AndroidJUnit4::class)
class SeekerFlowTest {

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

    // ── 1. SplashScreen ───────────────────────────────────────────────────────

    @Test
    fun splashScreen_needBloodUrgentlyButton_isDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                SplashScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithTag("btn_need_blood_urgently").assertIsDisplayed()
    }

    @Test
    fun splashScreen_registerDonorButton_isDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                SplashScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithTag("btn_register_donor").assertIsDisplayed()
    }

    @Test
    fun splashScreen_headlineText_isVisible() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                SplashScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("QATRA").assertIsDisplayed()
    }

    @Test
    fun splashScreen_tagline_isVisible() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                SplashScreen(viewModel = viewModel)
            }
        }
        composeTestRule
            .onNodeWithText("Connecting Verified Seekers to Eligible Donors in Minutes")
            .assertIsDisplayed()
    }

    // ── 2. RoleSelectionProfileScreen ─────────────────────────────────────────

    @Test
    fun roleSelectionProfileScreen_inputFieldsAndSaveButton_areDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                RoleSelectionProfileScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithTag("input_seeker_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_seeker_cnic").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_save_profile").assertIsDisplayed()
    }

    // ── 3. RequestCreationScreen ──────────────────────────────────────────────

    @Test
    fun requestCreationScreen_bloodGroupSelector_isDisplayed() {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            QatraTheme {
                RequestCreationScreen(viewModel = viewModel)
            }
        }
        // A+ is one of the blood-group chips rendered by the composable
        composeTestRule.onNodeWithTag("bg_select_A+").assertIsDisplayed()
    }
}
