package com.qatra.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class QatraFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Persist for MainActivity to upsert once a Supabase session exists.
        // Avoids constructing a full QatraRepository (mock seed + Supabase client) on the messaging thread.
        QatraPushState.savePendingToken(applicationContext, token)
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
}
