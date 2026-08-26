package com.qatra.app.ui.donor

import android.Manifest
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qatra.app.data.model.*
import com.qatra.app.ui.DonorScreenStep
import com.qatra.app.ui.MainTab
import com.qatra.app.ui.QatraViewModel
import com.qatra.app.ui.SeekerScreenStep
import com.qatra.app.ui.components.*
import com.qatra.app.ui.theme.*
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. Donor Onboarding & Identity Binding (Wireframe Page 12)
// ----------------------------------------------------
@Composable
fun DonorCnicUploadScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    var cnic by remember { mutableStateOf("42101-9876543-7") }
    val frontUri by viewModel.donorFrontImageUri.collectAsState()
    val backUri by viewModel.donorBackImageUri.collectAsState()
    val context = LocalContext.current
    var pickerSide by remember { mutableStateOf<String?>(null) }
    var activePickerSide by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (activePickerSide == "front") viewModel.donorFrontImageUri.value = uri
            if (activePickerSide == "back") viewModel.donorBackImageUri.value = uri
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (captured) {
            cameraUri?.let { uri ->
                if (activePickerSide == "front") viewModel.donorFrontImageUri.value = uri
                if (activePickerSide == "back") viewModel.donorBackImageUri.value = uri
            }
        }
    }
    var isVerifyingCnic by remember { mutableStateOf(false) }
    var cnicError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
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
                IconButton(onClick = { viewModel.setSeekerStep(SeekerScreenStep.SPLASH) }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "QATRA",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = QatraRedPrimary
                )
                Spacer(modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Donor CNIC Format Check",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Please provide your valid CNIC details to ensure a secure donation process.",
                style = MaterialTheme.typography.bodyMedium,
                color = QatraGray600
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CNIC Number Input
            OutlinedTextField(
                value = cnic,
                onValueChange = { cnic = it },
                label = { Text("CNIC Number") },
                placeholder = { Text("42101-XXXXXXX-X") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_donor_cnic"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QatraRedPrimary)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (cnicError != null) {
                Text(
                    text = cnicError!!,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Upload CNIC Photos",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = QatraGray800
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Front & Back CNIC Upload Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Front CNIC
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (frontUri != null) QatraRedContainer.copy(alpha = 0.3f) else QatraGray50,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (frontUri != null) QatraRedLight else QatraGray300
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clickable { pickerSide = "front" }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (frontUri != null) Icons.Filled.CheckCircle else Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Front CNIC",
                            tint = if (frontUri != null) QatraSuccess else QatraRedPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Front CNIC Image",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = QatraGray800
                        )
                    }
                }

                // Back CNIC
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (backUri != null) QatraRedContainer.copy(alpha = 0.3f) else QatraGray50,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (backUri != null) QatraRedLight else QatraGray300
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clickable { pickerSide = "back" }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (backUri != null) Icons.Filled.CheckCircle else Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Back CNIC",
                            tint = if (backUri != null) QatraSuccess else QatraRedPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Back CNIC Image",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = QatraGray800
                        )
                    }
                }
            }

            if (pickerSide != null) {
                AlertDialog(
                    onDismissRequest = { pickerSide = null },
                    title = { Text("Select CNIC image") },
                    text = { Text("Capture a new image or choose one from your gallery.") },
                    confirmButton = {
                        TextButton(onClick = {
                            activePickerSide = pickerSide
                            val uri = createCameraImageUri(context, "cnic-${pickerSide}-")
                            cameraUri = uri
                            pickerSide = null
                            cameraLauncher.launch(uri)
                        }) { Text("Camera") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            activePickerSide = pickerSide
                            pickerSide = null
                            galleryLauncher.launch("image/*")
                        }) { Text("Gallery") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AES-256 Vault Encryption Privacy Callout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = QatraGray50,
                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Encrypted",
                        tint = QatraGray800,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Data is AES-256 encrypted in an isolated vault and never exposed publicly.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = QatraGray800
                    )
                }
            }
        }

        Button(
            onClick = {
                if (isVerifyingCnic) return@Button
                cnicError = null
                isVerifyingCnic = true

                viewModel.viewModelScope.launch {
                    val documentsUploaded = viewModel.uploadDonorCnicDocuments(context)
                    val isValid = documentsUploaded && viewModel.repository.verifyCnic(cnic)
                    isVerifyingCnic = false

                    if (isValid) {
                        viewModel.donorCnicNumber.value = cnic
                        viewModel.setDonorStep(DonorScreenStep.PRE_SCREENING)
                    } else {
                        cnicError = if (documentsUploaded) {
                            "Invalid code, please try again"
                        } else {
                            "Please upload both CNIC images before continuing."
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_submit_donor_cnic"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary),
            enabled = !isVerifyingCnic
        ) {
            if (isVerifyingCnic) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Submit & Proceed to Health Check",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

// ----------------------------------------------------
// 2. Pre-Screening Health Checklist (Wireframe Page 13)
// ----------------------------------------------------
@Composable
fun PreScreeningChecklistScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) } // 1 to 4
    var ageValid by remember { mutableStateOf(true) }
    var weightValid by remember { mutableStateOf(true) }
    var noIllness by remember { mutableStateOf(true) }
    var noRecentDonation by remember { mutableStateOf(true) }
    var noTattooOrSurgery by remember { mutableStateOf(true) }

    val progressPercent = when (step) {
        1 -> "25%"
        2 -> "50%"
        3 -> "75%"
        else -> "100%"
    }
    val progressFloat = step / 4f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
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
                IconButton(onClick = {
                    if (step > 1) step -= 1 else viewModel.setDonorStep(DonorScreenStep.CNIC_UPLOAD)
                }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Health Eligibility",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Spacer(modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step $step of 4",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray800
                )
                Text(
                    text = progressPercent,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = QatraRedPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = QatraRedPrimary,
                trackColor = QatraGray200
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quiz Content per Step
            when (step) {
                1 -> {
                    // Step 1: Basic Requirements (Age & Weight)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = QatraGray50)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(QatraRedContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.Badge, contentDescription = "Identity", tint = QatraRedPrimary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Basic Requirements",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "To ensure donor safety, please confirm you meet the minimum physical requirements.",
                                style = MaterialTheme.typography.bodySmall,
                                color = QatraGray600
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Age Check
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Age", fontWeight = FontWeight.Bold, color = QatraGray900)
                                        Text(text = "I am between 18 and 65 years old.", style = MaterialTheme.typography.bodySmall, color = QatraGray600)
                                    }
                                    Switch(
                                        checked = ageValid,
                                        onCheckedChange = { ageValid = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = QatraSuccess)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Weight Check
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Weight", fontWeight = FontWeight.Bold, color = QatraGray900)
                                        Text(text = "I weigh at least 50 kg (110 lbs).", style = MaterialTheme.typography.bodySmall, color = QatraGray600)
                                    }
                                    Switch(
                                        checked = weightValid,
                                        onCheckedChange = { weightValid = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = QatraSuccess)
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Step 2: Recent Illness / Medication (14-day hold)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = QatraGray50)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(QatraRedContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.Medication, contentDescription = "Health", tint = QatraRedPrimary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Recent Health Status",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Fever / Antibiotics (14-day Hold)", fontWeight = FontWeight.Bold, color = QatraGray900)
                                        Text(
                                            text = "No fever, active viral infection, or antibiotic courses in the past 14 days.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QatraGray600
                                        )
                                    }
                                    Switch(
                                        checked = noIllness,
                                        onCheckedChange = { noIllness = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = QatraSuccess)
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Step 3: Donation Interval (90-day cooldown)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = QatraGray50)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(QatraRedContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.AvTimer, contentDescription = "Cooldown", tint = QatraRedPrimary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Donation Interval",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "90-Day Cooldown Window", fontWeight = FontWeight.Bold, color = QatraGray900)
                                        Text(
                                            text = "It has been at least 90 days (3 months) since my last whole blood donation.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QatraGray600
                                        )
                                    }
                                    Switch(
                                        checked = noRecentDonation,
                                        onCheckedChange = { noRecentDonation = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = QatraSuccess)
                                    )
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Step 4: Travel & Tattoos (6-month deferral)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = QatraGray50)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(QatraRedContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.HealthAndSafety, contentDescription = "Safety", tint = QatraRedPrimary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Procedures & Deferrals",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Tattoos / Major Surgery", fontWeight = FontWeight.Bold, color = QatraGray900)
                                        Text(
                                            text = "No body piercings, tattoos, or major surgical procedures in the last 6 months.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QatraGray600
                                        )
                                    }
                                    Switch(
                                        checked = noTattooOrSurgery,
                                        onCheckedChange = { noTattooOrSurgery = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = QatraSuccess)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Continue Button
        Button(
            onClick = {
                if (step < 4) {
                    step += 1
                } else {
                    // Passed all 4 steps -> navigate to Donor Dashboard
                    viewModel.setDonorStep(DonorScreenStep.HOME_DASHBOARD)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_health_continue"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
        ) {
            Text(
                text = if (step < 4) "Continue" else "Complete & Activate Dashboard ✔",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

// ----------------------------------------------------
// 3. Donor Home Dashboard (Wireframe Page 14)
// ----------------------------------------------------
@Composable
fun DonorHomeDashboardScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val donorProfile by viewModel.repository.donorProfile.collectAsState()
    val isAvailable = donorProfile.isAvailableToDonate
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.setDonorAvailability(context, granted)
    }

    LaunchedEffect(isAvailable) {
        if (isAvailable) {
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasLocationPermission) {
                viewModel.setDonorAvailability(context, true)
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QatraRedSurface)
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QATRA",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = QatraRedPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = "Alerts", tint = QatraRedPrimary)
                    }
                    BloodGroupBadge(group = donorProfile.bloodGroup, isSelected = true, size = 36.dp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Available to Donate Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Available to Donate",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = QatraGray900
                        )
                        Text(
                            text = "Location-based alerts enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = QatraGray600
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isAvailable) QatraSuccess else QatraGray600, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAvailable) "Online" else "Offline (Paused)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isAvailable) QatraSuccess else QatraGray600
                            )
                        }
                    }

                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { available ->
                            if (available) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                viewModel.setDonorAvailability(context, false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = QatraSuccess
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status & Lives Saved Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Badge Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(QatraSuccessContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Status",
                                tint = QatraSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = QatraGray600
                        )
                        Text(
                            text = "Ready & Eligible",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = QatraGray900,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Lives Saved Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = QatraRedPrimary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Lives saved",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "12",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Text(
                            text = "Lives Saved",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = QatraRedDark
                        ) {
                            Text(
                                text = "🛡 Silver Tier",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Local Alerts Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🚨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local Alerts",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                        }
                        TextButton(onClick = { viewModel.setDonorStep(DonorScreenStep.INTERACTIVE_MAP) }) {
                            Text(text = "View All", color = QatraRedPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QatraRedContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, QatraRedContainerDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = "Location",
                                    tint = QatraRedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "3 Verified Emergencies near you",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraRedDark
                                )
                            }
                            Text(
                                text = "Karachi, Sindh - Within 10km",
                                style = MaterialTheme.typography.bodySmall,
                                color = QatraGray600,
                                modifier = Modifier.padding(start = 24.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.showGeoAlertModal.value = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
                            ) {
                                Text("Review Requests", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Demo Shortcut to Cooldown State
        OutlinedButton(
            onClick = {
                viewModel.repository.setDonorCooldownDemo(74)
                viewModel.setDonorStep(DonorScreenStep.COOLDOWN_STATE)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Preview Cooldown State View (74 Days Remaining) →", color = QatraGray800)
        }
    }
}

// ----------------------------------------------------
// 4. Geo-Fenced Push Notification Alert Modal (Wireframe Page 15)
// ----------------------------------------------------
@Composable
fun GeoFencedPushAlertModal(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val alert by viewModel.geoAlertPayload.collectAsState()
    val payload = alert ?: return
    val bloodGroup = BloodGroup.fromString(payload.bloodGroup)
    val isHighPriority = payload.urgency.equals("HIGH_PRIORITY", ignoreCase = true) ||
        payload.urgency.contains("high", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // High Priority Urgency Banner
                Surface(
                    color = QatraUrgent,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Urgency",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHighPriority) "High Priority — Needed in 2 Hours" else payload.urgency,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Blood Group & Hospital
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BloodGroupBadge(group = bloodGroup, isSelected = true, size = 48.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "${bloodGroup.label} Needed Immediately",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = "Location",
                                    tint = QatraRedPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = payload.hospitalName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QatraGray600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Distance & Drive Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = QatraGray50,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Distance", style = MaterialTheme.typography.labelSmall, color = QatraGray600)
                                Text(
                                    text = "${payload.distanceKm} km away",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = QatraGray50,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Est. Drive", style = MaterialTheme.typography.labelSmall, color = QatraGray600)
                                Text(
                                    text = "${payload.etaMinutes} mins",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Requirement Callout
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QatraRedContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, QatraRedContainerDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🩸", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Requirement: ${payload.units} Unit ${payload.component} required",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = QatraRedDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    Button(
                        onClick = {
                            viewModel.donorAcceptDispatch()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_accept_dispatch"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
                    ) {
                        Icon(imageVector = Icons.Filled.DirectionsCar, contentDescription = "Navigate", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Accept Dispatch & Route", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.showGeoAlertModal.value = false
                            viewModel.setDonorStep(DonorScreenStep.INTERACTIVE_MAP)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "View on Live Map", color = QatraGray900, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = { viewModel.showGeoAlertModal.value = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Dismiss", color = QatraGray600)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. Interactive Map View (Wireframe Page 16)
// ----------------------------------------------------
@Composable
fun InteractiveMapScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val hospitals = viewModel.repository.hospitals
    val selectedHospital by viewModel.selectedHospitalForMap.collectAsState()
    val radiusKm by viewModel.selectedMapRadiusKm.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setDonorStep(DonorScreenStep.HOME_DASHBOARD) }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Live Map",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Spacer(modifier = Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Radius Selection Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Radius:", style = MaterialTheme.typography.labelSmall, color = QatraGray600)
                listOf(5, 10, 15).forEach { r ->
                    val isSelected = radiusKm == r
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedMapRadiusKm.value = r },
                        label = { Text("${r} km") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QatraRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Karachi Map Canvas with concentric rings
            InteractiveKarachiMapCanvas(
                hospitals = hospitals,
                selectedHospital = selectedHospital,
                radiusKm = radiusKm,
                onHospitalSelected = { viewModel.selectedHospitalForMap.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
        }

        // Expandable Summary Card at Bottom (Per Wireframe Page 16)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodGroupBadge(group = BloodGroup.O_NEG, isSelected = true, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "O-Negative | ${selectedHospital?.shortName ?: "City Hospital (JPMC)"}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = QatraGray900
                        )
                        Text(
                            text = "Needed in 2 Hours",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = QatraUrgent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "📍 2.3 km", style = MaterialTheme.typography.bodySmall, color = QatraGray800)
                    Text(text = "⏱ 12 min", style = MaterialTheme.typography.bodySmall, color = QatraGray800)
                    Text(text = "👥 18 Donors", style = MaterialTheme.typography.bodySmall, color = QatraGray800)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.donorAcceptDispatch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_accept_dispatch_map"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
                ) {
                    Icon(imageVector = Icons.Filled.DirectionsCar, contentDescription = "Navigate", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Accept Dispatch", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. Navigation & Proxy Routing View (Wireframe Page 17)
// ----------------------------------------------------
@Composable
fun NavigationProxyRoutingScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QATRA",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = QatraRedPrimary
                )
                Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Notifications")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Turn-by-Turn Instruction Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = QatraGray900
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(QatraRedPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TurnRight,
                            contentDescription = "Turn",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "In 200m Turn right",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "onto Rafiqui H.J. Shaheed Rd",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "12 min", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "4.2 km", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Route Graphic Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE3EBF3)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = "Navigation",
                        tint = QatraRedPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Live Route Navigation • Karachi Traffic Nominal",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = QatraGray800
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Verified Destination Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = QatraGray50)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "VERIFIED DESTINATION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = QatraRedDark
                    )
                    Text(
                        text = "JPMC Blood Bank & Transfusion Center",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = QatraGray900
                    )
                    Text(
                        text = "📍 Rafiqui H.J. Shaheed Rd, Karachi",
                        style = MaterialTheme.typography.bodySmall,
                        color = QatraGray600
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Recipient Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Person, contentDescription = "Recipient", tint = QatraGray600)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Recipient: Ahmed Khan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Blood Type Needed: O-", style = MaterialTheme.typography.bodySmall, color = QatraRedDark)
                                }
                            }
                            UrgencyBadge(urgency = UrgencyLevel.HIGH_PRIORITY)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Seeker has been notified of your dispatch and may call you directly via masked line to coordinate arrival.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = QatraGray600
                    )
                }
            }
        }

        // Actions
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.donorFinishDonation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_donation_done"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QatraSuccess)
            ) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Done", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "I Have Arrived & Donated", fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = { viewModel.setDonorStep(DonorScreenStep.HOME_DASHBOARD) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Cancel Dispatch (Release to Next Donor)", color = QatraUrgent)
            }
        }
    }
}

// ----------------------------------------------------
// 7. Donation Complete & Cooldown Activation (Wireframe Page 18)
// ----------------------------------------------------
@Composable
fun DonationCompleteCooldownScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
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
                    text = "QATRA",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = QatraRedPrimary
                )
                Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Notifications")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Green Confirmation Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = QatraSuccessContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Donation confirmed",
                        tint = QatraSuccess,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Donation Confirmed!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = QatraSuccess
                    )
                    Text(
                        text = "Thank You for Saving a Life.",
                        style = MaterialTheme.typography.bodySmall,
                        color = QatraGray800
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Circular 90-Day Cooldown Ring
            Text(
                text = "Recovery Cooldown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .border(10.dp, QatraRedContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "90",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp
                        ),
                        color = QatraRedPrimary
                    )
                    Text(
                        text = "Days Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = QatraGray600
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cooldown Explanation Callout with Day-85 reminder notice
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = QatraWarningContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, QatraWarning.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Info",
                        tint = QatraWarning,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your donor profile is temporarily paused from emergency alerts to ensure full recovery. Day-85 reminder active.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = QatraGray900
                    )
                }
            }
        }

        // Action Button
        Button(
            onClick = {
                viewModel.enterMainShell(MainTab.LEARN)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
        ) {
            Text(
                text = "Explore Awareness & Health Hub →",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

// ----------------------------------------------------
// 8. Cooldown State View (Wireframe Page 19)
// ----------------------------------------------------
@Composable
fun CooldownStateScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QatraRedSurface)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QATRA",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = QatraRedPrimary
                    )
                    Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Notifications")
                }
            }

            // Locked Donor Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(QatraRedContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile", tint = QatraRedPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Alex Mercer",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                                Text(
                                    text = "O+ • 4 Lifetime Donations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QatraGray600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Badge: On Cooldown (74 Days Remaining)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = QatraWarningContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = "Cooldown",
                                    tint = QatraWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Status: On Cooldown (74 Days Remaining)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraWarning
                                )
                            }
                        }
                    }
                }
            }

            // Locked Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Available to Donate",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray600
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.Filled.Lock, contentDescription = "Locked", tint = QatraGray600, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = "Auto-unlocks on Day 90",
                                style = MaterialTheme.typography.bodySmall,
                                color = QatraGray600
                            )
                        }
                        Switch(
                            checked = false,
                            onCheckedChange = null,
                            enabled = false
                        )
                    }
                }
            }

            // Healthy Nutrition Tips Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🥗", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Healthy Nutrition Tips",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rebuild your iron stores faster with these 5 delicious, iron-rich recipes designed for donors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = QatraGray600
                        )
                    }
                }
            }

            // Awareness Library Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.enterMainShell(MainTab.LEARN) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📖", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Awareness Library",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Read stories from recipients and learn how your O+ blood makes an impact globally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = QatraGray600
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Explore Articles →",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = QatraRedPrimary
                        )
                    }
                }
            }

            // Upcoming Campus Drives Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏫", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upcoming Campus Drives",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "See where the QATRA mobile units will be stationed next month when you're eligible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = QatraGray600
                        )
                    }
                }
            }
        }
    }
}
