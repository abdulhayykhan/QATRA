package com.qatra.app.ui.components

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qatra.app.data.model.BloodGroup
import com.qatra.app.data.model.Hospital
import com.qatra.app.data.model.UrgencyLevel
import com.qatra.app.ui.MainTab
import com.qatra.app.ui.theme.*
import java.io.File

fun createCameraImageUri(context: Context, prefix: String): Uri {
    val directory = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File.createTempFile(prefix, ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

@Composable
fun QatraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showSubtext: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(6.dp, CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(QatraRedPrimary, QatraRedDark)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocalHospital,
                contentDescription = "QATRA Logo",
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }
        if (showSubtext) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "QATRA",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = QatraRedPrimary
            )
            Text(
                text = "Alkhidmat Emergency Blood Platform",
                style = MaterialTheme.typography.bodySmall,
                color = QatraGray600
            )
        }
    }
}

@Composable
fun BloodGroupBadge(
    group: BloodGroup,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = if (isSelected) QatraRedPrimary else QatraRedContainer
    val textColor = if (isSelected) Color.White else QatraRedDark

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) QatraRedDark else QatraRedContainerDark,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = group.label,
            fontWeight = FontWeight.Bold,
            fontSize = if (size > 40.dp) 16.sp else 13.sp,
            color = textColor
        )
    }
}

@Composable
fun UrgencyBadge(
    urgency: UrgencyLevel,
    modifier: Modifier = Modifier
) {
    val isUrgent = urgency == UrgencyLevel.HIGH_PRIORITY
    val bgColor = if (isUrgent) QatraUrgentContainer else QatraWarningContainer
    val textColor = if (isUrgent) QatraUrgent else QatraWarning

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(textColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = urgency.badgeText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

@Composable
fun PulseBroadcastIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(modifier = modifier.size(36.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(32.dp * scale)
                .background(QatraRedPrimary.copy(alpha = alpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(QatraRedPrimary, CircleShape)
        )
    }
}

@Composable
fun QatraBottomNavigation(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(12.dp),
        containerColor = QatraWhite,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(MainTab.EMERGENCY, "Emergency", Icons.Filled.LocalHospital),
            Triple(MainTab.DONATE, "Donate", Icons.Filled.VolunteerActivism),
            Triple(MainTab.LEARN, "Health Hub", Icons.Filled.School),
            Triple(MainTab.DESK, "Admin Desk", Icons.Filled.AdminPanelSettings)
        )

        items.forEach { (tab, label, icon) ->
            val isSelected = selectedTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) QatraRedPrimary else QatraGray600
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) QatraRedPrimary else QatraGray600
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = QatraRedContainer
                ),
                modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
            )
        }
    }
}

@Composable
fun BroadcastRadarCard(
    radiusKm: Int,
    donorsNotified: Int,
    responsesReceived: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QatraRedSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseBroadcastIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Geo-Broadcast",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = QatraRedPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QatraRedPrimary
                ) {
                    Text(
                        text = "${radiusKm}km Radius",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$donorsNotified",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = QatraGray900
                    )
                    Text(
                        text = "Donors Alerted",
                        style = MaterialTheme.typography.bodySmall,
                        color = QatraGray600
                    )
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(QatraGray300)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$responsesReceived",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = QatraSuccess
                    )
                    Text(
                        text = "Responded (ETA < 20m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = QatraGray600
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveKarachiMapCanvas(
    hospitals: List<Hospital>,
    selectedHospital: Hospital?,
    radiusKm: Int,
    onHospitalSelected: (Hospital) -> Unit,
    modifier: Modifier = Modifier
) {
    val centerHospital = hospitals.firstOrNull { it.shortName == "JPMC" } ?: hospitals.first()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F0F8))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val centerX = width * centerHospital.xPercent
            val centerY = height * centerHospital.yPercent

            // Draw subtle Karachi grid & coastlines
            val gridColor = Color(0xFFD0DDEB)
            for (i in 0..6) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height * (i / 6f)),
                    end = Offset(width, height * (i / 6f)),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(width * (i / 6f), 0f),
                    end = Offset(width * (i / 6f), height),
                    strokeWidth = 1f
                )
            }

            // Draw Arabian Sea coastline indicator
            val waterColor = Color(0xFFB0D0E8)
            drawRect(
                color = waterColor,
                topLeft = Offset(0f, height * 0.85f),
                size = androidx.compose.ui.geometry.Size(width, height * 0.15f)
            )

            // Draw Concentric Radial Rings (5km, 10km, 15km)
            val ringRadii = listOf(
                Pair(width * 0.22f, "5 km"),
                Pair(width * 0.38f, "10 km"),
                Pair(width * 0.52f, "15 km")
            )

            ringRadii.forEach { (radius, _) ->
                drawCircle(
                    color = QatraRedPrimary.copy(alpha = 0.25f),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            }

            // Highlight chosen search radius fill
            val activeRadius = when (radiusKm) {
                5 -> width * 0.22f
                15 -> width * 0.52f
                else -> width * 0.38f
            }
            drawCircle(
                color = QatraRedPrimary.copy(alpha = 0.08f),
                radius = activeRadius,
                center = Offset(centerX, centerY)
            )
        }

        // Concentric Radius Labels
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "📍 Karachi Proximity Rings",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = QatraGray900
            )
            Text(
                text = "• Inner: 5km  • Mid: 10km  • Outer: 15km",
                style = MaterialTheme.typography.bodySmall,
                color = QatraGray600,
                fontSize = 10.sp
            )
        }

        // Render Hospital Pins on Map
        hospitals.forEach { hospital ->
            val isSelected = selectedHospital?.id == hospital.id
            val pinColor = if (hospital.shortName == "JPMC") QatraRedPrimary else if (hospital.isTraumaCenter) QatraUrgent else QatraInfo

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
                    .offset(
                        // ponytail: pins anchored top-left to fractional coords, matches Canvas rings. Center-anchor if pins must sit dead-on.
                        x = maxWidth * hospital.xPercent,
                        y = maxHeight * hospital.yPercent
                    )
                    .clickable { onHospitalSelected(hospital) }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 36.dp else 28.dp)
                            .shadow(4.dp, CircleShape)
                            .background(if (isSelected) QatraRedDark else pinColor, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalHospital,
                            contentDescription = hospital.name,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = hospital.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = QatraGray900
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MockRequisitionSlipView(
    hospitalName: String,
    doctorStampDetected: Boolean = true,
    mrn: String = "MRN-2026-9912",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, QatraGray400, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFDFD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Letterhead
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = hospitalName.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = QatraGray900
                    )
                    Text(
                        text = "EMERGENCY BLOOD REQUISITION / CROSS-MATCH FORM",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = QatraRedDark
                    )
                }
                Icon(
                    imageVector = Icons.Filled.LocalHospital,
                    contentDescription = null,
                    tint = QatraRedPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = QatraGray200)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Patient MRN: $mrn", style = MaterialTheme.typography.bodySmall, color = QatraGray800)
                Text(text = "Date: 20 Aug 2026", style = MaterialTheme.typography.bodySmall, color = QatraGray600)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Highlighted OCR Extracted Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = QatraRedPrimary,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .background(QatraRedContainer.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[OCR Extracted: 2 Units PRBC - O-Neg]",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = QatraRedDark
                        )
                        Text(
                            text = "Conf: 94%",
                            style = MaterialTheme.typography.labelSmall,
                            color = QatraSuccess
                        )
                    }
                    Text(
                        text = "Diagnosis: Acute Trauma / Pre-op Transfusion",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = QatraGray800
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Doctor Stamp Simulated Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (doctorStampDetected) {
                    Box(
                        modifier = Modifier
                            .border(1.5.dp, Color(0xFF1565C0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "DR. M. FARHAN FCPS (SURGERY)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFF1565C0)
                            )
                            Text(
                                text = "REG # PMDC-77492-S • STAMP VERIFIED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                }
            }
        }
    }
}
