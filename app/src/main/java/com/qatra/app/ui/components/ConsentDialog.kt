package com.qatra.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qatra.app.ui.theme.*

/**
 * A full-screen consent dialog that presents a scrollable summary of the
 * QATRA Privacy Policy and Terms of Service. The user must check the
 * agreement checkbox before the Accept button becomes enabled.
 *
 * @param onAccept  Invoked when the user checks the box and taps Accept.
 * @param onDecline Invoked when the user taps Decline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var agreedToTerms by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Placeholder URL — replace with the hosted privacy policy URL when available.
    val privacyPolicyUrl = "https://qatra.app/privacy-policy"

    Dialog(
        onDismissRequest = { /* Non-dismissible — user must accept or decline */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = QatraWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ── Header ──────────────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Privacy Shield",
                        tint = QatraRedPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Privacy & Terms",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = QatraGray900
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please review before continuing",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = QatraGray600
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Scrollable Policy Summary ───────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = QatraGray200,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(QatraGray50, RoundedCornerShape(16.dp))
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    PolicySection(
                        icon = Icons.Filled.Fingerprint,
                        title = "Data We Collect",
                        items = listOf(
                            "Phone number — for OTP-based authentication",
                            "GPS location — for proximity donor matching and geo-fenced alerts",
                            "CNIC number — stored only as a one-way hash for identity verification",
                            "Blood group — required for donor–seeker matching",
                            "Medical documents — hospital requisition slips for request verification"
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PolicySection(
                        icon = Icons.Filled.Handshake,
                        title = "How Your Data Is Shared",
                        items = listOf(
                            "Matched donors see only your blood group, hospital, and urgency — never your phone number or CNIC",
                            "Seekers see only the donor's blood group and availability — never personal details",
                            "No data is sold, rented, or shared with advertisers"
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PolicySection(
                        icon = Icons.Filled.Lock,
                        title = "Security Measures",
                        items = listOf(
                            "All data encrypted in transit (TLS 1.2+) and at rest (Supabase encryption)",
                            "CNIC stored exclusively as a cryptographic hash",
                            "Row-Level Security (RLS) enforces access control at the database level",
                            "Masked calling protects phone numbers during donor–seeker communication"
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PolicySection(
                        icon = Icons.Filled.Schedule,
                        title = "Data Retention",
                        items = listOf(
                            "Account data retained while your account is active",
                            "Blood request data retained for 90 days after completion, then permanently deleted"
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PolicySection(
                        icon = Icons.Filled.PersonOff,
                        title = "Your Rights",
                        items = listOf(
                            "Request access to all personal data we hold about you",
                            "Request deletion of your account and associated data",
                            "Opt out of GPS location tracking via device settings",
                            "Opt out of push notifications via device settings"
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PolicySection(
                        icon = Icons.Filled.Cloud,
                        title = "Third-Party Services",
                        items = listOf(
                            "Google Firebase — phone authentication and push notifications",
                            "Supabase — encrypted data storage with row-level security"
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Age restriction notice
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = QatraWarningContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NoAccounts,
                                contentDescription = "Age Restriction",
                                tint = QatraWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "QATRA is not intended for users under the age of 18.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = QatraGray800
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── View Full Policy Link ───────────────────────────────────
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = QatraRedPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Full Privacy Policy",
                        color = QatraRedPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Agreement Checkbox ──────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QatraRedContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = QatraRedPrimary,
                            uncheckedColor = QatraGray400
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val annotatedText = buildAnnotatedString {
                        append("I agree to the ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = QatraGray900)) {
                            append("Terms of Service")
                        }
                        append(" and ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = QatraGray900)) {
                            append("Privacy Policy")
                        }
                    }
                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = QatraGray800),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Action Buttons ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decline
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = QatraGray600),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            width = 1.5.dp
                        )
                    ) {
                        Text(
                            "Decline",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    // Accept
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        enabled = agreedToTerms,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QatraRedPrimary,
                            disabledContainerColor = QatraGray300,
                            disabledContentColor = QatraGray400
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Accept",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Helper Composable ───────────────────────────────────────────────────────

@Composable
private fun PolicySection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    items: List<String>
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = QatraRedPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = QatraGray900
            )
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    items.forEach { item ->
        Row(
            modifier = Modifier.padding(start = 28.dp, bottom = 3.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "•",
                color = QatraRedLight,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item,
                style = MaterialTheme.typography.bodySmall.copy(color = QatraGray800),
                lineHeight = 18.sp
            )
        }
    }
}
