package com.qatra.app.ui.awareness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qatra.app.data.model.BloodGroup
import com.qatra.app.ui.DonorScreenStep
import com.qatra.app.ui.QatraViewModel
import com.qatra.app.ui.components.BloodGroupBadge
import com.qatra.app.ui.theme.*

@Composable
fun AwarenessHubScreen(
    viewModel: QatraViewModel,
    modifier: Modifier = Modifier
) {
    val campusDrives by viewModel.repository.campusDrives.collectAsState()
    var selectedGroupForCompat by remember { mutableStateOf(BloodGroup.O_NEG) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QatraRedSurface)
            .statusBarsPadding()
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
                    text = "Awareness & Health Hub",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = QatraGray900
                )
                Text(
                    text = "Alkhidmat Foundation Pakistan • Blood Knowledge Center",
                    style = MaterialTheme.typography.bodySmall,
                    color = QatraRedPrimary
                )
            }
            Icon(imageVector = Icons.Filled.School, contentDescription = null, tint = QatraRedPrimary)
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Blood Compatibility Visualizer Matrix Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🩸", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Blood Type Compatibility Matrix",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                        }
                        Text(
                            text = "Tap a blood type to see who they can give to and receive from.",
                            style = MaterialTheme.typography.bodySmall,
                            color = QatraGray600
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Blood Group Selector Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(BloodGroup.values()) { group ->
                                val isSelected = selectedGroupForCompat == group
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) QatraRedPrimary else QatraGray50,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) QatraRedPrimary else QatraGray200),
                                    modifier = Modifier.clickable { selectedGroupForCompat = group }
                                ) {
                                    Text(
                                        text = group.label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else QatraGray800,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val compatibilityInfo = when (selectedGroupForCompat) {
                            BloodGroup.O_NEG -> Pair("Universal Donor (All blood types)", "O- only")
                            BloodGroup.O_POS -> Pair("O+, A+, B+, AB+", "O+, O-")
                            BloodGroup.A_NEG -> Pair("A-, A+, AB-, AB+", "A-, O-")
                            BloodGroup.A_POS -> Pair("A+, AB+", "A+, A-, O+, O-")
                            BloodGroup.B_NEG -> Pair("B-, B+, AB-, AB+", "B-, O-")
                            BloodGroup.B_POS -> Pair("B+, AB+", "B+, B-, O+, O-")
                            BloodGroup.AB_NEG -> Pair("AB-, AB+", "AB-, A-, B-, O-")
                            BloodGroup.AB_POS -> Pair("AB+ only", "Universal Recipient (All blood types)")
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = QatraRedContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, QatraRedContainerDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Filled.VolunteerActivism, contentDescription = null, tint = QatraRedPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Can Give Blood To:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = QatraRedDark
                                    )
                                }
                                Text(
                                    text = compatibilityInfo.first,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900,
                                    modifier = Modifier.padding(start = 24.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Filled.HealthAndSafety, contentDescription = null, tint = QatraSuccess, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Can Receive Blood From:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = QatraSuccess
                                    )
                                }
                                Text(
                                    text = compatibilityInfo.second,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900,
                                    modifier = Modifier.padding(start = 24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Campus Blood Drives List
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🏫", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upcoming Campus Drives",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = QatraGray900
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = QatraRedContainer
                            ) {
                                Text(
                                    text = "${campusDrives.size} Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = QatraRedDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        campusDrives.forEach { drive ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = QatraGray50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, QatraGray200),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = drive.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = QatraGray900)
                                        Text(text = "📍 ${drive.universityVenue}", style = MaterialTheme.typography.bodySmall, color = QatraGray600)
                                        Text(text = "🗓 ${drive.dateStr} • ${drive.timeStr}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = QatraRedPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "${drive.registeredDonors}/${drive.targetQuotaUnits}", fontWeight = FontWeight.Bold, color = QatraGray900)
                                        Text(text = "Registered", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = QatraGray600)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Post-Donation Nutritional Recovery Guide
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🥗", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Post-Donation Recovery Diet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = QatraGray900
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• Hydrate: Drink 500ml extra water/electrolyte liquids right after donating.\n" +
                                    "• Iron-Rich Foods: Spinach (Palak), Lentils (Daal), Eggs, and Pomegranate (Anaar).\n" +
                                    "• Vitamin C: Oranges and lemons enhance iron absorption.\n" +
                                    "• Rest: Avoid strenuous lifting or workouts for 24 hours.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                            color = QatraGray800
                        )
                    }
                }
            }
        }
    }
}
