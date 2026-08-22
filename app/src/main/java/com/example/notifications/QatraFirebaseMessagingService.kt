package com.example.notifications

import com.example.data.repository.QatraRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class QatraFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            QatraRepository().registerFcmToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val requestId = data["request_id"] ?: return
        QatraPushState.publish(
            applicationContext,
            GeoAlertPayload(
                requestId = requestId,
                bloodGroup = data["blood_group"] ?: "Unknown",
                hospitalName = data["hospital_name"] ?: "Hospital",
                urgency = data["urgency"] ?: "High Priority",
                distanceKm = data["distance_km"] ?: "0",
                units = data["units"] ?: "1",
                component = data["component"] ?: "PRBC",
                etaMinutes = data["eta_minutes"] ?: "0"
            )
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}