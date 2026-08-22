package com.example.ui.seeker

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.DonorScreenStep
import com.example.ui.MainTab
import com.example.ui.QatraViewModel
import com.example.ui.SeekerScreenStep
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. Splash & Onboarding Screen (Wireframe Page 2)
// ----------------------------------------------------
@Composable
fun SplashScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf("EN") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QatraRedSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Language Switcher & Admin entry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.setAdminStep(com.example.ui.AdminScreenStep.LOGIN_2FA) }) {
                Text(
                    text = "Admin Desk",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray600
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray300),
                modifier = Modifier.clickable {
                    selectedLanguage = if (selectedLanguage == "EN") "اردو" else "EN"
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = "Language",
                        tint = QatraRedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedLanguage == "EN") "🌐 EN / اردو" else "🌐 اردو / EN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = QatraGray900
                    )
                }
            }
        }

        // Center Hero Logo & Tagline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            QatraLogo(size = 90.dp, showSubtext = false)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "QATRA",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = QatraRedPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Connecting Verified Seekers to Eligible Donors in Minutes",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = QatraGray800,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Dual-Choice Action Prompts
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Primary Urgent Need Button
            Button(
                onClick = { viewModel.setSeekerStep(SeekerScreenStep.PHONE_VERIFICATION) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .testTag("btn_need_blood_urgently"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Need Blood Urgently",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Emergency Seeker",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            // Secondary Register as Voluntary Donor Button
            OutlinedButton(
                onClick = {
                    viewModel.setDonorStep(DonorScreenStep.CNIC_UPLOAD)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_register_donor"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, QatraRedPrimary),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = QatraRedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Register as Voluntary Donor",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = QatraRedPrimary
                    )
                }
            }

            // Quick App Shell Tour
            TextButton(
                onClick = { viewModel.enterMainShell(MainTab.EMERGENCY) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Explore Live Requests & Knowledge Hub →",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = QatraRedDark
                )
            }

            // Helpline and Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PhoneInTalk,
                        contentDescription = null,
                        tint = QatraRedDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Emergency Helpline (112)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = QatraRedDark
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alkhidmat Foundation Pakistan • A life-saving initiative",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = QatraGray600
                )
            }
        }
    }
}

// ----------------------------------------------------
// 2. Seeker Registration / Phone OTP (Wireframe Page 3)
// ----------------------------------------------------
@Composable
fun PhoneVerificationScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val phone by viewModel.phoneNumber.collectAsState()
    val otp by viewModel.otpCode.collectAsState()
    val timerSeconds by viewModel.otpTimerSeconds.collectAsState()
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(phone) {
        val activity = context as? Activity
        if (activity != null && phone.isNotBlank()) {
            viewModel.repository.sendFirebaseOtp(activity, phone) { sent ->
                if (!sent) {
                    otpError = "Unable to send verification code. Please try again."
                }
            }
        }
    }

    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    val timerFormatted = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Header with Back
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setSeekerStep(SeekerScreenStep.SPLASH) }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Phone Verification",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Spacer(modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Security Icon Shield
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(QatraRedContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = QatraRedPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Secure Your Account",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We'll send a one-time verification code to this number to verify your identity.",
                style = MaterialTheme.typography.bodyMedium,
                color = QatraGray600,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Phone Input
            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.phoneNumber.value = it },
                label = { Text("Mobile Number") },
                leadingIcon = {
                    Text(
                        text = "🇵🇰 +92",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_seeker_phone"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QatraRedPrimary,
                    focusedLabelColor = QatraRedPrimary
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Enter 6-digit OTP",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = QatraGray800
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6-digit OTP Input UI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0..5) {
                    val char = if (i < otp.length) otp[i].toString() else if (i == 0 && otp.isEmpty()) "1" else ""
                    val isCurrent = i == otp.length || (i == 0 && otp.isEmpty())

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) QatraRedPrimary else QatraGray400,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (isCurrent) QatraRedContainer.copy(alpha = 0.3f) else Color.White,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = QatraGray900
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (otpError != null) {
                Text(
                    text = otpError!!,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 180s Countdown Timer Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = QatraRedDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Resend OTP in $timerFormatted",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (timerSeconds > 0) QatraGray600 else QatraRedPrimary
                )
            }
        }

        Button(
            onClick = {
                if (isVerifyingOtp) return@Button
                otpError = null
                isVerifyingOtp = true

                viewModel.viewModelScope.launch {
                    val isValid = viewModel.verifyOtpAndContinue(otp)
                    isVerifyingOtp = false

                    if (!isValid) {
                        otpError = "Invalid code, please try again"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_verify_otp"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary),
            enabled = !isVerifyingOtp
        ) {
            if (isVerifyingOtp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Verify & Continue →",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

// ----------------------------------------------------
// 3. Quick Profile Setup (Wireframe Page 4)
// ----------------------------------------------------
@Composable
fun RoleSelectionProfileScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("Jane Doe") }
    var age by remember { mutableStateOf("30") }
    var gender by remember { mutableStateOf("F") } // M, F, O
    var district by remember { mutableStateOf("Karachi South") }
    var cnic by remember { mutableStateOf("42101-1234567-1") }
    var districtMenuExpanded by remember { mutableStateOf(false) }

    val karachiDistricts = listOf(
        "Karachi South",
        "Karachi East",
        "Karachi Central",
        "Karachi West",
        "Malir",
        "Korangi",
        "Kemari"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setSeekerStep(SeekerScreenStep.PHONE_VERIFICATION) }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Quick Profile Setup",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = QatraRedPrimary
                )
                Spacer(modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Please provide your basic details to help us match you quickly when every second counts.",
                style = MaterialTheme.typography.bodyMedium,
                color = QatraGray600
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Full Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Jane Doe") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_seeker_name"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QatraRedPrimary)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Age & Gender Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    placeholder = { Text("e.g. 30") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_seeker_age"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QatraRedPrimary)
                )

                // Gender Toggle Chips (M / F / O)
                Column(modifier = Modifier.weight(1.4f)) {
                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.labelSmall,
                        color = QatraGray600
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(QatraGray100, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("M", "F", "O").forEach { g ->
                            val isSelected = gender == g
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) QatraRedPrimary else Color.Transparent)
                                    .clickable { gender = g },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = g,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else QatraGray800
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary District Dropdown
            Box {
                OutlinedTextField(
                    value = district,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Primary District (Karachi)") },
                    trailingIcon = {
                        IconButton(onClick = { districtMenuExpanded = true }) {
                            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { districtMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = districtMenuExpanded,
                    onDismissRequest = { districtMenuExpanded = false }
                ) {
                    karachiDistricts.forEach { dist ->
                        DropdownMenuItem(
                            text = { Text(dist) },
                            onClick = {
                                district = dist
                                districtMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional CNIC
            OutlinedTextField(
                value = cnic,
                onValueChange = { cnic = it },
                label = { Text("CNIC (Optional)") },
                placeholder = { Text("e.g. 42101-1234567-1") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_seeker_cnic"),
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = QatraSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Providing CNIC adds a 'Verified' badge to your requests",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = QatraGray600
                        )
                    }
                }
            )
        }

        // Save Profile Button
        Button(
            onClick = {
                viewModel.seekerName.value = name
                viewModel.seekerAge.value = age
                viewModel.seekerGender.value = gender
                viewModel.seekerDistrict.value = district
                viewModel.seekerCnic.value = cnic
                viewModel.setSeekerStep(SeekerScreenStep.REQUEST_CREATION)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_save_profile"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
        ) {
            Text(
                text = "Save Profile ✔",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

// ----------------------------------------------------
// 4. Create Emergency Request Form (Wireframe Page 5)
// ----------------------------------------------------
@Composable
fun RequestCreationScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val selectedBloodGroup by viewModel.selectedBloodGroup.collectAsState()
    val selectedComponent by viewModel.selectedComponent.collectAsState()
    val units by viewModel.unitsRequired.collectAsState()
    val selectedHospital by viewModel.selectedHospital.collectAsState()
    val selectedUrgency by viewModel.selectedUrgency.collectAsState()

    var hospitalDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setSeekerStep(SeekerScreenStep.ROLE_PROFILE) }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Emergency Request",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = QatraRedPrimary
                    )
                }
            }

            // Blood Group 8-Grid
            item {
                Text(
                    text = "Blood Group Required",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Spacer(modifier = Modifier.height(8.dp))
                val groups = BloodGroup.values()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        groups.take(4).forEach { bg ->
                            BloodGroupBadge(
                                group = bg,
                                isSelected = selectedBloodGroup == bg,
                                onClick = { viewModel.selectedBloodGroup.value = bg },
                                size = 52.dp,
                                modifier = Modifier.testTag("bg_select_${bg.label}")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        groups.drop(4).forEach { bg ->
                            BloodGroupBadge(
                                group = bg,
                                isSelected = selectedBloodGroup == bg,
                                onClick = { viewModel.selectedBloodGroup.value = bg },
                                size = 52.dp,
                                modifier = Modifier.testTag("bg_select_${bg.label}")
                            )
                        }
                    }
                }
            }

            // Component Type Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Component Type",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = QatraGray900
                    )
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = QatraGray600,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BloodComponent.values().forEach { comp ->
                        val isSelected = selectedComponent == comp
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) QatraRedPrimary else QatraGray100,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) QatraRedDark else QatraGray200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectedComponent.value = comp }
                        ) {
                            Text(
                                text = comp.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) Color.White else QatraGray800,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            // Units Required Stepper
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QatraGray50, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Units Required",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = QatraGray900
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (units > 1) viewModel.unitsRequired.value = units - 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, QatraGray300, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease")
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "$units",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = QatraRedPrimary
                            )
                            Text(
                                text = "Bags",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = QatraGray600
                            )
                        }

                        IconButton(
                            onClick = { if (units < 10) viewModel.unitsRequired.value = units + 1 },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, QatraGray300, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase")
                        }
                    }
                }
            }

            // Hospital / Location Dropdown
            item {
                Text(
                    text = "Hospital / Location",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = selectedHospital.name,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.LocalHospital,
                                contentDescription = null,
                                tint = QatraRedPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { hospitalDropdownExpanded = true }) {
                                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hospitalDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = hospitalDropdownExpanded,
                        onDismissRequest = { hospitalDropdownExpanded = false }
                    ) {
                        viewModel.repository.hospitals.forEach { hosp ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(hosp.name, fontWeight = FontWeight.SemiBold)
                                        Text(hosp.district, style = MaterialTheme.typography.bodySmall, color = QatraGray600)
                                    }
                                },
                                onClick = {
                                    viewModel.selectedHospital.value = hosp
                                    hospitalDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Urgency Level Toggles
            item {
                Text(
                    text = "Urgency Level",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Spacer(modifier = Modifier.height(8.dp))

                // High Priority Option
                val isHigh = selectedUrgency == UrgencyLevel.HIGH_PRIORITY
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectedUrgency.value = UrgencyLevel.HIGH_PRIORITY }
                        .border(
                            width = if (isHigh) 2.dp else 1.dp,
                            color = if (isHigh) QatraUrgent else QatraGray300,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHigh) QatraUrgentContainer.copy(alpha = 0.5f) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(QatraUrgent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "High Priority",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                                Text(
                                    text = "Needed within 2 Hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QatraGray600
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = QatraUrgent
                        ) {
                            Text(
                                text = "Urgent",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Standard Option
                val isStandard = selectedUrgency == UrgencyLevel.STANDARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectedUrgency.value = UrgencyLevel.STANDARD }
                        .border(
                            width = if (isStandard) 2.dp else 1.dp,
                            color = if (isStandard) QatraWarning else QatraGray300,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStandard) QatraWarningContainer.copy(alpha = 0.5f) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(QatraWarning, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Standard",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                                Text(
                                    text = "Needed within 24 Hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QatraGray600
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = QatraWarning
                        ) {
                            Text(
                                text = "Routine",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Proceed to Slip Verification Button
        Button(
            onClick = { viewModel.setSeekerStep(SeekerScreenStep.SLIP_UPLOAD) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_proceed_slip_upload"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
        ) {
            Text(
                text = "Proceed to Slip Verification →",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

// ----------------------------------------------------
// 5. Hospital Requisition Slip Upload (Wireframe Page 6)
// ----------------------------------------------------
@Composable
fun RequisitionSlipUploadScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val slipSelected by viewModel.slipImageSelected.collectAsState()
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.slipImageUri.value = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (captured) cameraUri?.let { viewModel.slipImageUri.value = it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setSeekerStep(SeekerScreenStep.REQUEST_CREATION) }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Upload Hospital Slip",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Spacer(modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Official Hospital Requisition Slip Required",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Upload a clear photo of the blood requisition slip issued by the attending hospital.",
                style = MaterialTheme.typography.bodyMedium,
                color = QatraGray600
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Slip Upload Card / Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true }
                    .border(
                        width = 1.5.dp,
                        color = if (slipSelected) QatraRedPrimary else QatraGray400,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (slipSelected) Color.White else QatraGray50
                )
            ) {
                if (slipSelected) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = "Selected hospital slip",
                            tint = QatraRedPrimary,
                            modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = QatraSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hospital slip attached • Tap to replace",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = QatraSuccess
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(QatraRedContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                tint = QatraRedPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Take Photo or Upload from Gallery",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = QatraGray900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Supports JPG, PNG (Max 10MB)",
                            style = MaterialTheme.typography.bodySmall,
                            color = QatraGray600
                        )
                    }
                }
            }

            if (showPicker) {
                AlertDialog(
                    onDismissRequest = { showPicker = false },
                    title = { Text("Select hospital slip") },
                    text = { Text("Capture a new image or choose one from your gallery.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showPicker = false
                            val uri = createCameraImageUri(context, "hospital-slip-")
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        }) { Text("Camera") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPicker = false
                            galleryLauncher.launch("image/*")
                        }) { Text("Gallery") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI Fast-Track Callout Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = QatraRedContainer.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, QatraRedContainerDark)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = QatraRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Our AI extracts and verifies your prescription in real time to fast-track donor alerts.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = QatraRedDark
                    )
                }
            }
        }

        // Action Button
        Button(
            onClick = {
                viewModel.submitSlipAndVerify(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_verify_and_broadcast"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
        ) {
            Text(
                text = "Verify & Broadcast Emergency 🚀",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

// ----------------------------------------------------
// 6. Slip Verification Pending Modal (Wireframe Page 7)
// ----------------------------------------------------
@Composable
fun SlipVerificationPendingModal(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()
    val step1Done by viewModel.ocrStep1Completed.collectAsState()
    val step2Done by viewModel.ocrStep2Completed.collectAsState()
    val routedToVerification by viewModel.slipRoutedToVerification.collectAsState()
    val verificationError by viewModel.slipVerificationError.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spinner or Success Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(QatraRedContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isOcrProcessing) {
                        CircularProgressIndicator(
                            color = QatraRedPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = QatraSuccess,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        isOcrProcessing -> "Verifying Requisition Slip"
                        verificationError != null -> "Slip Processing Failed"
                        routedToVerification -> "Sent for Manual Review"
                        else -> "Verification Completed!"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Automated OCR & Doctor Stamp Validation",
                    style = MaterialTheme.typography.bodySmall,
                    color = QatraGray600
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (verificationError != null || routedToVerification) {
                    Text(
                        text = verificationError ?: "The slip was added to the 24/7 verification desk because key fields were unclear.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (verificationError != null) MaterialTheme.colorScheme.error else QatraGray800,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Progress Checkpoints
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Checkpoint 1: OCR Slip analysis
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (step1Done) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = QatraSuccess, modifier = Modifier.size(20.dp))
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = QatraRedPrimary, strokeWidth = 2.dp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scanning Requisition Slip... (OCR Analysis)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (step1Done) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (step1Done) QatraGray900 else QatraGray600
                        )
                    }

                    // Checkpoint 2: Doctor Stamp & MRN
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (step2Done) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = QatraSuccess, modifier = Modifier.size(20.dp))
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = QatraRedPrimary, strokeWidth = 2.dp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Validating Doctor Stamp & MRN...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (step2Done) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (step2Done) QatraGray900 else QatraGray600
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = QatraGray100
                ) {
                    Text(
                        text = "Takes ~3 seconds. Verified requests receive 4x faster donor responses.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = QatraGray800,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.setSeekerStep(
                            if (verificationError != null) SeekerScreenStep.SLIP_UPLOAD
                            else if (routedToVerification) SeekerScreenStep.SPLASH
                            else SeekerScreenStep.LIVE_STATUS_FEED
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
                ) {
                    Text(
                        when {
                            verificationError != null -> "Return to Slip Upload"
                            routedToVerification -> "Return to Home"
                            else -> "Go to Live Status Feed"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 7. Live Request Feed Status (Wireframe Page 8)
// ----------------------------------------------------
@Composable
fun LiveRequestFeedStatusScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val activeRequest by viewModel.repository.activeSeekerRequest.collectAsState()
    val allRequests by viewModel.repository.requests.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QatraRedSurface)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Emergency Feed",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Text(
                    text = "Real-time blood matching across Karachi hospitals",
                    style = MaterialTheme.typography.bodySmall,
                    color = QatraGray600
                )
            }

            IconButton(onClick = { viewModel.setSeekerStep(SeekerScreenStep.REQUEST_CREATION) }) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "New Request",
                    tint = QatraRedPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Active Request Status Card
            activeRequest?.let { req ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Top Bar of Active Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = QatraRedContainer
                                    ) {
                                        Text(
                                            text = "YOUR ACTIVE REQUEST",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = QatraRedDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = req.id,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = QatraGray800
                                    )
                                }

                                UrgencyBadge(urgency = req.urgency)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Blood Group & Units Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BloodGroupBadge(group = req.bloodGroup, isSelected = true, size = 52.dp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "${req.bloodGroup.label} • ${req.component.displayName}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = QatraGray900
                                    )
                                    Text(
                                        text = "${req.unitsRequired} Units Required • ${req.hospital.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QatraGray600
                                    )
                                    Text(
                                        text = "📍 ${req.hospital.district}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = QatraRedPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Radar Broadcast Visualizer
                            BroadcastRadarCard(
                                radiusKm = 10,
                                donorsNotified = req.activeDonorsInRadius,
                                responsesReceived = req.respondedDonorsCount
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Matched Donors Call to Action
                            Button(
                                onClick = { viewModel.setSeekerStep(SeekerScreenStep.MATCHED_DONORS) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_view_matched_donors"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
                            ) {
                                Text(
                                    text = "View Matched Donors (${req.respondedDonorsCount}) →",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Other Verified Requests in Karachi
            item {
                Text(
                    text = "OTHER ACTIVE REQUESTS IN KARACHI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray600
                )
            }

            items(allRequests.filter { it.id != activeRequest?.id }) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BloodGroupBadge(group = req.bloodGroup, isSelected = true, size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${req.bloodGroup.label} Required",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${req.createdAtMinutesAgo}m ago",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = QatraGray600
                                )
                            }
                            Text(
                                text = req.hospital.shortName,
                                style = MaterialTheme.typography.bodySmall,
                                color = QatraGray800
                            )
                            Text(
                                text = "${req.unitsRequired} Bags (${req.component.displayName})",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = QatraGray600
                            )
                        }
                        UrgencyBadge(urgency = req.urgency)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 8. Matched Donors Screen (Wireframe Page 9)
// ----------------------------------------------------
@Composable
fun MatchedDonorsScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val matchedDonors by viewModel.repository.matchedDonors.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setSeekerStep(SeekerScreenStep.LIVE_STATUS_FEED) }) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Matched Donors (${matchedDonors.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )
            Spacer(modifier = Modifier.weight(1.3f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Donor List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(matchedDonors) { donor ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = QatraGray50)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BloodGroupBadge(group = donor.bloodGroup, isSelected = true, size = 44.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = donor.id,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = QatraGray900
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${donor.bloodGroup.label} Donor",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QatraGray600
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = QatraSuccessContainer
                                        ) {
                                            Text(
                                                text = "Verified",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = QatraSuccess,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (donor.statusText.contains("Accepted")) QatraSuccessContainer else QatraWarningContainer
                            ) {
                                Text(
                                    text = donor.statusText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (donor.statusText.contains("Accepted")) QatraSuccess else QatraWarning,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Distance & ETA Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Place, contentDescription = null, tint = QatraRedPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${donor.distanceKm} km away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QatraGray800
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null, tint = QatraGray600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Est. Arrival: ${donor.etaMinutes} mins",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = QatraGray800
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action: Call via Masked Proxy
                        Button(
                            onClick = { viewModel.setSeekerStep(SeekerScreenStep.MASKED_CALL) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_call_masked_donor"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
                        ) {
                            Icon(imageVector = Icons.Filled.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Call via Masked Proxy", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 9. Masked Calling Screen (Wireframe Page 10)
// ----------------------------------------------------
@Composable
fun MaskedCallingScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val isCallActive by viewModel.isProxyCallActive.collectAsState()
    val secondsRemaining by viewModel.proxyCallSecondsRemaining.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startProxyCallCountdown()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QatraGray900)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QATRA MASKED CALL",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = QatraSuccess
                ) {
                    Text(
                        text = "Encrypted Bridge",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Donor Avatar / Badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(QatraRedPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "O-",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Donor #D-104",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Text(
                text = "Proxy Connected • 0300-XXXXXXX",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Call Duration: ${secondsRemaining / 60}:${String.format("%02d", secondsRemaining % 60)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = QatraSuccess
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Privacy Callout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Direct phone numbers are masked by QATRA Proxy for your safety and privacy.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // End Call Red Floating Button
            IconButton(
                onClick = {
                    viewModel.endProxyCall()
                    viewModel.setSeekerStep(SeekerScreenStep.CONFIRMATION)
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(QatraUrgent, CircleShape)
                    .testTag("btn_end_masked_call")
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "End Call & Confirm Delivery",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ----------------------------------------------------
// 10. Donation Confirmation & Feedback Screen (Wireframe Page 11)
// ----------------------------------------------------
@Composable
fun DonationConfirmationScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    var rating by remember { mutableStateOf(5) }
    var feedback by remember { mutableStateOf("Donor arrived swiftly at JPMC and transfusion started. Thank you so much!") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Green Success Banner Box
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = QatraSuccessContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = QatraSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Donation Completed!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = QatraSuccess
                    )
                    Text(
                        text = "2 Units PRBC delivered to JPMC",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QatraGray800
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Rate Your Experience",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your feedback helps Alkhidmat maintain high standards of voluntary response.",
                style = MaterialTheme.typography.bodySmall,
                color = QatraGray600,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5-Star Rating Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..5) {
                    IconButton(
                        onClick = { rating = i },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Star $i",
                            tint = if (i <= rating) Color(0xFFFFB800) else QatraGray400,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feedback Text Field
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Thank You Note / Experience (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )
        }

        // Submit & Close Request Button
        Button(
            onClick = {
                viewModel.setSeekerStep(SeekerScreenStep.SPLASH)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_submit_feedback"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
        ) {
            Text(
                text = "Submit Feedback & Close Request ✔",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}
