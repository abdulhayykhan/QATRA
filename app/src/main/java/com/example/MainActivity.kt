package com.example

import android.os.Bundle
import com.google.firebase.messaging.FirebaseMessaging
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.admin.*
import com.example.ui.awareness.AwarenessHubScreen
import com.example.ui.donor.*
import com.example.ui.seeker.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: QatraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModel.registerPushToken(applicationContext, token)
        }
        viewModel.registerPendingPushToken(applicationContext)
        viewModel.consumePendingGeoAlert(applicationContext)
        setContent {
            QatraTheme {
                QatraApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun QatraApp(viewModel: QatraViewModel) {
    val activeFlow by viewModel.activeFlow.collectAsState()
    val seekerStep by viewModel.seekerStep.collectAsState()
    val donorStep by viewModel.donorStep.collectAsState()
    val adminStep by viewModel.adminStep.collectAsState()
    val mainTab by viewModel.mainTab.collectAsState()
    val showGeoAlert by viewModel.showGeoAlertModal.collectAsState()

    // Handle Back Press
    BackHandler {
        when (activeFlow) {
            FlowType.SEEKER -> {
                when (seekerStep) {
                    SeekerScreenStep.SPLASH -> { /* exit */ }
                    SeekerScreenStep.PHONE_VERIFICATION -> viewModel.setSeekerStep(SeekerScreenStep.SPLASH)
                    SeekerScreenStep.ROLE_PROFILE -> viewModel.setSeekerStep(SeekerScreenStep.PHONE_VERIFICATION)
                    SeekerScreenStep.REQUEST_CREATION -> viewModel.setSeekerStep(SeekerScreenStep.ROLE_PROFILE)
                    SeekerScreenStep.SLIP_UPLOAD -> viewModel.setSeekerStep(SeekerScreenStep.REQUEST_CREATION)
                    SeekerScreenStep.VERIFICATION_MODAL -> viewModel.setSeekerStep(SeekerScreenStep.SLIP_UPLOAD)
                    SeekerScreenStep.LIVE_STATUS_FEED -> viewModel.setSeekerStep(SeekerScreenStep.SPLASH)
                    SeekerScreenStep.MATCHED_DONORS -> viewModel.setSeekerStep(SeekerScreenStep.LIVE_STATUS_FEED)
                    SeekerScreenStep.MASKED_CALL -> viewModel.setSeekerStep(SeekerScreenStep.MATCHED_DONORS)
                    SeekerScreenStep.CONFIRMATION -> viewModel.setSeekerStep(SeekerScreenStep.SPLASH)
                }
            }
            FlowType.DONOR -> {
                when (donorStep) {
                    DonorScreenStep.CNIC_UPLOAD -> viewModel.setSeekerStep(SeekerScreenStep.SPLASH)
                    DonorScreenStep.PRE_SCREENING -> viewModel.setDonorStep(DonorScreenStep.CNIC_UPLOAD)
                    DonorScreenStep.HOME_DASHBOARD -> viewModel.setSeekerStep(SeekerScreenStep.SPLASH)
                    DonorScreenStep.INTERACTIVE_MAP -> viewModel.setDonorStep(DonorScreenStep.HOME_DASHBOARD)
                    DonorScreenStep.NAVIGATION_ROUTING -> viewModel.setDonorStep(DonorScreenStep.HOME_DASHBOARD)
                    DonorScreenStep.DONATION_COMPLETE -> viewModel.setDonorStep(DonorScreenStep.HOME_DASHBOARD)
                    DonorScreenStep.COOLDOWN_STATE -> viewModel.setDonorStep(DonorScreenStep.HOME_DASHBOARD)
                }
            }
            FlowType.ADMIN -> {
                when (adminStep) {
                    AdminScreenStep.LOGIN_2FA -> viewModel.setSeekerStep(SeekerScreenStep.SPLASH)
                    AdminScreenStep.VERIFICATION_QUEUE -> viewModel.setAdminStep(AdminScreenStep.LOGIN_2FA)
                    AdminScreenStep.FRAUD_AUDIT -> viewModel.setAdminStep(AdminScreenStep.VERIFICATION_QUEUE)
                    AdminScreenStep.DRIVE_MANAGEMENT -> viewModel.setAdminStep(AdminScreenStep.FRAUD_AUDIT)
                }
            }
            FlowType.MAIN_SHELL -> {
                viewModel.setSeekerStep(SeekerScreenStep.SPLASH)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (activeFlow) {
            FlowType.SEEKER -> {
                AnimatedContent(
                    targetState = seekerStep,
                    label = "SeekerFlowTransition"
                ) { step ->
                    when (step) {
                        SeekerScreenStep.SPLASH -> SplashScreen(viewModel = viewModel)
                        SeekerScreenStep.PHONE_VERIFICATION -> PhoneVerificationScreen(viewModel = viewModel)
                        SeekerScreenStep.ROLE_PROFILE -> RoleSelectionProfileScreen(viewModel = viewModel)
                        SeekerScreenStep.REQUEST_CREATION -> RequestCreationScreen(viewModel = viewModel)
                        SeekerScreenStep.SLIP_UPLOAD -> RequisitionSlipUploadScreen(viewModel = viewModel)
                        SeekerScreenStep.VERIFICATION_MODAL -> SlipVerificationPendingModal(viewModel = viewModel)
                        SeekerScreenStep.LIVE_STATUS_FEED -> LiveRequestFeedStatusScreen(viewModel = viewModel)
                        SeekerScreenStep.MATCHED_DONORS -> MatchedDonorsScreen(viewModel = viewModel)
                        SeekerScreenStep.MASKED_CALL -> MaskedCallingScreen(viewModel = viewModel)
                        SeekerScreenStep.CONFIRMATION -> DonationConfirmationScreen(viewModel = viewModel)
                    }
                }
            }
            FlowType.DONOR -> {
                AnimatedContent(
                    targetState = donorStep,
                    label = "DonorFlowTransition"
                ) { step ->
                    when (step) {
                        DonorScreenStep.CNIC_UPLOAD -> DonorCnicUploadScreen(viewModel = viewModel)
                        DonorScreenStep.PRE_SCREENING -> PreScreeningChecklistScreen(viewModel = viewModel)
                        DonorScreenStep.HOME_DASHBOARD -> DonorHomeDashboardScreen(viewModel = viewModel)
                        DonorScreenStep.INTERACTIVE_MAP -> InteractiveMapScreen(viewModel = viewModel)
                        DonorScreenStep.NAVIGATION_ROUTING -> NavigationProxyRoutingScreen(viewModel = viewModel)
                        DonorScreenStep.DONATION_COMPLETE -> DonationCompleteCooldownScreen(viewModel = viewModel)
                        DonorScreenStep.COOLDOWN_STATE -> CooldownStateScreen(viewModel = viewModel)
                    }
                }
            }
            FlowType.ADMIN -> {
                AnimatedContent(
                    targetState = adminStep,
                    label = "AdminFlowTransition"
                ) { step ->
                    when (step) {
                        AdminScreenStep.LOGIN_2FA -> AdminLogin2FAScreen(viewModel = viewModel)
                        AdminScreenStep.VERIFICATION_QUEUE -> VerificationQueueScreen(viewModel = viewModel)
                        AdminScreenStep.FRAUD_AUDIT -> FraudAuditCenterScreen(viewModel = viewModel)
                        AdminScreenStep.DRIVE_MANAGEMENT -> DriveManagementDashboardScreen(viewModel = viewModel)
                    }
                }
            }
            FlowType.MAIN_SHELL -> {
                MainShellScreen(
                    viewModel = viewModel,
                    currentTab = mainTab,
                    onTabSelected = { viewModel.mainTab.value = it }
                )
            }
        }

        // Overlay Geo Alert Modal if triggered
        if (showGeoAlert) {
            GeoFencedPushAlertModal(viewModel = viewModel)
        }
    }
}

@Composable
fun MainShellScreen(
    viewModel: QatraViewModel,
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.EMERGENCY,
                    onClick = { onTabSelected(MainTab.EMERGENCY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.EMERGENCY) Icons.Filled.LocalHospital else Icons.Outlined.LocalHospital,
                            contentDescription = "Emergency"
                        )
                    },
                    label = { Text("Emergency", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QatraRedPrimary,
                        selectedTextColor = QatraRedPrimary,
                        indicatorColor = QatraRedContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.DONATE,
                    onClick = { onTabSelected(MainTab.DONATE) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.DONATE) Icons.Filled.VolunteerActivism else Icons.Outlined.VolunteerActivism,
                            contentDescription = "Donate"
                        )
                    },
                    label = { Text("Donate", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QatraRedPrimary,
                        selectedTextColor = QatraRedPrimary,
                        indicatorColor = QatraRedContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.LEARN,
                    onClick = { onTabSelected(MainTab.LEARN) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.LEARN) Icons.Filled.School else Icons.Outlined.School,
                            contentDescription = "Learn"
                        )
                    },
                    label = { Text("Health Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QatraRedPrimary,
                        selectedTextColor = QatraRedPrimary,
                        indicatorColor = QatraRedContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.DESK,
                    onClick = { onTabSelected(MainTab.DESK) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.DESK) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Admin Desk"
                        )
                    },
                    label = { Text("Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QatraRedPrimary,
                        selectedTextColor = QatraRedPrimary,
                        indicatorColor = QatraRedContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                MainTab.EMERGENCY -> LiveRequestFeedStatusScreen(viewModel = viewModel)
                MainTab.DONATE -> DonorHomeDashboardScreen(viewModel = viewModel)
                MainTab.LEARN -> AwarenessHubScreen(viewModel = viewModel)
                MainTab.DESK -> {
                    val isAdminAuth by viewModel.isAdminAuthenticated.collectAsState()
                    if (isAdminAuth) VerificationQueueScreen(viewModel = viewModel)
                    else AdminLogin2FAScreen(viewModel = viewModel)
                }
            }
        }
    }
}
