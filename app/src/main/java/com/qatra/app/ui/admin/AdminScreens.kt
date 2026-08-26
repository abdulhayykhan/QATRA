package com.qatra.app.ui.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qatra.app.data.model.BloodGroup
import com.qatra.app.ui.AdminScreenStep
import com.qatra.app.ui.MainTab
import com.qatra.app.ui.QatraViewModel
import com.qatra.app.ui.SeekerScreenStep
import com.qatra.app.ui.components.MockRequisitionSlipView
import com.qatra.app.ui.theme.*
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. Admin Login & 2FA (Wireframe Page 20)
// ----------------------------------------------------
@Composable
fun AdminLogin2FAScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var signingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Red Top Header Brand Panel (per wireframe left-panel design adapted for mobile)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(QatraRedPrimary)
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QATRA",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    IconButton(onClick = { viewModel.setSeekerStep(SeekerScreenStep.SPLASH) }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Exit Admin", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connecting Verified Seekers to Eligible Donors in Minutes",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Life-Saving Speed. Secure, reliable, and instantaneous matching when every second counts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // Login Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Verification Desk Login",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )
            Text(
                text = "Enter your credentials to access the admin terminal.",
                style = MaterialTheme.typography.bodySmall,
                color = QatraGray600
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Email, contentDescription = "Email", tint = QatraGray600)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "Password", tint = QatraGray600)
                },
                trailingIcon = {
                    Icon(imageVector = Icons.Filled.Visibility, contentDescription = "Show password", tint = QatraGray600)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation()
            )

            // 2FA TOTP Code Field
            OutlinedTextField(
                value = totpCode,
                onValueChange = { totpCode = it },
                label = { Text("2FA TOTP Code") },
                placeholder = { Text("6-digit authenticator code") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Pin, contentDescription = "TOTP code", tint = QatraGray600)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    Text(
                        text = "Required for terminal access",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = QatraGray600
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (loginError != null) {
                Text(
                    text = loginError ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = QatraUrgent
                )
            }

            // Sign In Button
            Button(
                onClick = {
                    loginError = null
                    signingIn = true
                    scope.launch {
                        val ok = viewModel.adminSignIn(email, password, totpCode)
                        signingIn = false
                        if (ok) {
                            viewModel.setAdminStep(AdminScreenStep.VERIFICATION_QUEUE)
                        } else {
                            loginError = viewModel.adminAuthError.value
                                ?: "Invalid credentials. Contact IT support."
                        }
                    }
                },
                enabled = !signingIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_admin_login"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
            ) {
                Text(
                    text = if (signingIn) "Signing in…" else "Sign In to 24/7 Verification Desk",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { /* Forgot */ }) {
                    Text(text = "Forgot password?", color = QatraGray600, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { /* IT Support */ }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.SupportAgent, contentDescription = "IT Support", tint = QatraRedPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "IT Support", color = QatraRedPrimary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. 24/7 Verification Queue (Wireframe Page 21)
// ----------------------------------------------------
@Composable
fun VerificationQueueScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val queueItems by viewModel.repository.verificationQueue.collectAsState()
    val activeItem = queueItems.firstOrNull { it.status == "Pending" }

    if (activeItem == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Queue clear",
                tint = QatraSuccess,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Verification Queue Clear",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No pending requests awaiting verification.",
                style = MaterialTheme.typography.bodyMedium,
                color = QatraGray600,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { viewModel.setAdminStep(AdminScreenStep.FRAUD_AUDIT) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Security, contentDescription = "Fraud audit", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Fraud Audit Center")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { viewModel.setAdminStep(AdminScreenStep.DRIVE_MANAGEMENT) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Event, contentDescription = "Campus drives", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Campus Drive Management")
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Admin Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Verification Desk",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Text(
                    text = "Active Verification: Request #${activeItem.requestId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = QatraRedPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { viewModel.setAdminStep(AdminScreenStep.FRAUD_AUDIT) }) {
                    Icon(imageVector = Icons.Filled.Security, contentDescription = "Fraud Center", tint = QatraRedPrimary)
                }
                IconButton(onClick = { viewModel.setAdminStep(AdminScreenStep.DRIVE_MANAGEMENT) }) {
                    Icon(imageVector = Icons.Filled.Event, contentDescription = "Drives", tint = QatraGray800)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Document Viewer Box (Wireframe left panel)
            item {
                Text(
                    text = "Scanned Requisition Slip",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray800
                )
                Spacer(modifier = Modifier.height(6.dp))
                MockRequisitionSlipView(
                    hospitalName = activeItem.hospitalName,
                    doctorStampDetected = activeItem.doctorStampDetected,
                    mrn = activeItem.mrn
                )
            }

            // Extracted Data Verification Table (Wireframe right panel)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = QatraGray50)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted Data Verification",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = QatraRedContainer
                            ) {
                                Text(
                                    text = "Auto-Extracted",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = QatraRedDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "FIELD", style = MaterialTheme.typography.labelSmall, color = QatraGray600)
                            Text(text = "EXTRACTED VALUE", style = MaterialTheme.typography.labelSmall, color = QatraGray600)
                            Text(text = "CONFIDENCE", style = MaterialTheme.typography.labelSmall, color = QatraGray600)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = QatraGray200)

                        // Rows
                        ExtractedFieldRow(field = "Hospital", value = activeItem.hospitalName, confidence = "${activeItem.ocrConfidence}%", isLow = false)
                        ExtractedFieldRow(field = "Doctor Stamp", value = if (activeItem.doctorStampDetected) "Detected" else "Missing", confidence = "92%", isLow = false)
                        ExtractedFieldRow(field = "MRN", value = activeItem.mrn, confidence = "95%", isLow = false)
                        ExtractedFieldRow(
                            field = "Blood Group",
                            value = activeItem.bloodGroup.label,
                            confidence = "${activeItem.bloodGroupConfidence}%",
                            isLow = activeItem.bloodGroupConfidence < 85 // Flagged < 85% per PRD Wireframe Page 21
                        )
                        ExtractedFieldRow(field = "Units", value = "${activeItem.units} Bags", confidence = "90%", isLow = false)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Flag warning if any
                        if (activeItem.flagWarning != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = QatraWarningContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Filled.Warning, contentDescription = "Warning", tint = QatraWarning, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeItem.flagWarning ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = QatraWarning
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Warning note
            item {
                Text(
                    text = "Ensure the extracted data perfectly matches the scanned document. Incorrect blood group data can have severe consequences.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = QatraGray600
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Approve / Reject Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.adminRejectVerification(activeItem.id)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = QatraUrgent),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, QatraUrgent)
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Reject Request", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.adminApproveVerification(activeItem.id)
                    viewModel.setAdminStep(AdminScreenStep.FRAUD_AUDIT)
                },
                modifier = Modifier
                    .weight(1.3f)
                    .height(50.dp)
                    .testTag("btn_approve_request"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Approve", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Approve & Publish", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ExtractedFieldRow(
    field: String,
    value: String,
    confidence: String,
    isLow: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = field, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = QatraGray800)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = QatraGray900)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLow) {
                Icon(imageVector = Icons.Filled.Warning, contentDescription = "Low confidence", tint = QatraWarning, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = confidence,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isLow) QatraWarning else QatraSuccess
            )
            if (isLow) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = QatraWarningContainer
                ) {
                    Text(
                        text = "Check Manual",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = QatraWarning,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. Fraud Audit & Blacklist Center (Wireframe Page 22)
// ----------------------------------------------------
@Composable
fun FraudAuditCenterScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val fraudItems by viewModel.repository.fraudAuditItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Fraud Audit & Account Blacklist Center",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Text(
                    text = "Monitor, investigate, and manage suspicious activities and policy violations.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = QatraGray600
                )
            }
            IconButton(onClick = { viewModel.setAdminStep(AdminScreenStep.DRIVE_MANAGEMENT) }) {
                Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "Drives", tint = QatraRedPrimary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stat Badges (Flagged Submissions 12, Duplicate MRNs 4, Suspended Accounts 7)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminStatCard(title = "Flagged Submissions", count = "12", icon = Icons.Filled.Warning, color = QatraUrgent, modifier = Modifier.weight(1f))
            AdminStatCard(title = "Duplicate MRNs", count = "4", icon = Icons.Filled.FileCopy, color = QatraWarning, modifier = Modifier.weight(1f))
            AdminStatCard(title = "Suspended Accounts", count = "7", icon = Icons.Filled.Block, color = QatraGray800, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by CNIC or Hospital MRN...") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") }
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Audit Items List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(fraudItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = QatraGray50)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ID: ${item.requestId} • MRN: ${item.hospitalMrn}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (item.actionStatus == "Blacklisted") QatraUrgent else QatraWarningContainer
                            ) {
                                Text(
                                    text = item.flagReason,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (item.actionStatus == "Blacklisted") Color.White else QatraWarning,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "CNIC: ${item.seekerCnicMasked}", style = MaterialTheme.typography.bodySmall, color = QatraGray800)
                            Text(text = "Phone: ${item.phoneMasked}", style = MaterialTheme.typography.bodySmall, color = QatraGray600)
                        }

                        Text(
                            text = "OCR Match: ${item.ocrConfidence}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (item.ocrConfidence < 50) QatraUrgent else QatraWarning
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.repository.updateFraudItemAction(item.id, "Blacklisted") },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = QatraUrgent)
                            ) {
                                Text(text = "Blacklist CNIC", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                            }

                            OutlinedButton(
                                onClick = { viewModel.repository.updateFraudItemAction(item.id, "Whitelisted") },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "Whitelist", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = QatraGray50,
        border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = count, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = QatraGray900)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = QatraGray600,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------
// 4. Drive Management Dashboard (Wireframe Page 23)
// ----------------------------------------------------
@Composable
fun DriveManagementDashboardScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val attendees by viewModel.repository.attendees.collectAsState()
    val isScanning by viewModel.qrScanActive.collectAsState()

    var eventTitle by remember { mutableStateOf("NED Spring Blood Drive '26") }
    var selectedVenue by remember { mutableStateOf("NED University Main Auditorium") }
    var targetQuota by remember { mutableStateOf(150) }
    var eventDate by remember { mutableStateOf("25/08/2026") }
    var venueDropdownExpanded by remember { mutableStateOf(false) }

    val universityVenues = listOf(
        "NED University Main Auditorium",
        "IBA Main Campus, Student Center",
        "FAST NUCES CFD Ground",
        "Karachi University Arts Lobby",
        "Dow Medical College Campus"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Alkhidmat Campus Chapter & Drive Management",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Text(
                    text = "Manage university campus blood drives, coordinate logistics, and track live check-ins.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = QatraGray600
                )
            }
            IconButton(onClick = { viewModel.setAdminStep(AdminScreenStep.LOGIN_2FA) }) {
                Icon(imageVector = Icons.Filled.Logout, contentDescription = "Exit", tint = QatraRedPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stat Cards (Drives 6, Pre-Screened 420, Target 850 Units)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminStatCard(title = "Total Drives Scheduled", count = "6", icon = Icons.Filled.Event, color = QatraInfo, modifier = Modifier.weight(1f))
            AdminStatCard(title = "Pre-Screened Donors", count = "420", icon = Icons.Filled.People, color = QatraSuccess, modifier = Modifier.weight(1f))
            AdminStatCard(title = "Target Collection", count = "850 Units", icon = Icons.Filled.LocalHospital, color = QatraRedPrimary, modifier = Modifier.weight(1.2f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Schedule New Drive Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = QatraGray50)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "Add", tint = QatraRedPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Schedule New Drive",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = eventTitle,
                            onValueChange = { eventTitle = it },
                            label = { Text("Event Title") },
                            placeholder = { Text("e.g. Spring Blood Drive '26") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Venue Dropdown
                        Box {
                            OutlinedTextField(
                                value = selectedVenue,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("University / Campus Venue") },
                                trailingIcon = {
                                    IconButton(onClick = { venueDropdownExpanded = true }) {
                                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Expand")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { venueDropdownExpanded = true },
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = venueDropdownExpanded,
                                onDismissRequest = { venueDropdownExpanded = false }
                            ) {
                                universityVenues.forEach { venue ->
                                    DropdownMenuItem(
                                        text = { Text(venue) },
                                        onClick = {
                                            selectedVenue = venue
                                            venueDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = "$targetQuota",
                                onValueChange = { targetQuota = it.toIntOrNull() ?: 100 },
                                label = { Text("Target Quota (Units)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = eventDate,
                                onValueChange = { eventDate = it },
                                label = { Text("Date") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.repository.scheduleNewCampusDrive(
                                    title = eventTitle,
                                    venue = selectedVenue,
                                    targetQuota = targetQuota,
                                    dateStr = eventDate,
                                    timeStr = "10:00 AM - 04:00 PM"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_publish_drive_event"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
                        ) {
                            Icon(imageVector = Icons.Filled.Publish, contentDescription = "Publish", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Publish Event to Awareness Hub", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Live Attendance & QR Scanner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = QatraGray50)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = "QR Scanner", tint = QatraRedPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Live Attendance & QR Scanner",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = QatraSuccessContainer
                            ) {
                                Text(
                                    text = "● NED Drive Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = QatraSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // QR Scanner Viewfinder Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2B2D42))
                                .clickable { viewModel.simulateQrScan() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isScanning) {
                                    CircularProgressIndicator(color = QatraRedLight, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Verifying Student QR Code...", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Icon(imageVector = Icons.Filled.QrCode, contentDescription = "Scan QR code", tint = Color.White, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Click to activate camera for QR scanning", color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Attendee Check-In Table
                        Text(
                            text = "PRE-SCREENED VOLUNTEER & DONOR LIST",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = QatraGray600
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        attendees.forEach { attendee ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = attendee.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text(text = attendee.deptYear, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = QatraGray600)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (attendee.preScreeningStatus == "Passed") QatraSuccessContainer else if (attendee.preScreeningStatus == "Pending") QatraWarningContainer else QatraUrgentContainer
                                        ) {
                                            Text(
                                                text = attendee.preScreeningStatus,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = if (attendee.preScreeningStatus == "Passed") QatraSuccess else if (attendee.preScreeningStatus == "Pending") QatraWarning else QatraUrgent,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (attendee.checkInStatus == "Awaiting Check-in") {
                                        Button(
                                            onClick = { viewModel.repository.checkInAttendee(attendee.id) },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = QatraRedPrimary)
                                        ) {
                                            Text(text = "Manual Check-in", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                        }
                                    } else {
                                        Text(
                                            text = attendee.checkInStatus,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                            color = if (attendee.checkInStatus.startsWith("Checked")) QatraSuccess else QatraGray600
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
