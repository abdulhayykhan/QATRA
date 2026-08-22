package com.example.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GeoAlertPayload(
    val requestId: String,
    val bloodGroup: String,
    val hospitalName: String,
    val urgency: String,
    val distanceKm: String,
    val units: String,
    val component: String,
    val etaMinutes: String
)

object QatraPushState {
    private val _latestGeoAlert = MutableStateFlow<GeoAlertPayload?>(null)
    val latestGeoAlert = _latestGeoAlert.asStateFlow()

    fun publish(context: Context, alert: GeoAlertPayload) {
        _latestGeoAlert.value = alert
        context.getSharedPreferences("qatra_push", Context.MODE_PRIVATE)
            .edit()
            .putString("alert_request_id", alert.requestId)
            .putString("alert_blood_group", alert.bloodGroup)
            .putString("alert_hospital_name", alert.hospitalName)
            .putString("alert_urgency", alert.urgency)
            .putString("alert_distance_km", alert.distanceKm)
            .putString("alert_units", alert.units)
            .putString("alert_component", alert.component)
            .putString("alert_eta_minutes", alert.etaMinutes)
            .apply()
    }

    fun consume(context: Context): GeoAlertPayload? {
        val preferences = context.getSharedPreferences("qatra_push", Context.MODE_PRIVATE)
        val requestId = preferences.getString("alert_request_id", null) ?: return null
        val alert = GeoAlertPayload(
            requestId = requestId,
            bloodGroup = preferences.getString("alert_blood_group", "Unknown") ?: "Unknown",
            hospitalName = preferences.getString("alert_hospital_name", "Hospital") ?: "Hospital",
            urgency = preferences.getString("alert_urgency", "High Priority") ?: "High Priority",
            distanceKm = preferences.getString("alert_distance_km", "0") ?: "0",
            units = preferences.getString("alert_units", "1") ?: "1",
            component = preferences.getString("alert_component", "PRBC") ?: "PRBC",
            etaMinutes = preferences.getString("alert_eta_minutes", "0") ?: "0"
        )
        preferences.edit()
            .remove("alert_request_id")
            .remove("alert_blood_group")
            .remove("alert_hospital_name")
            .remove("alert_urgency")
            .remove("alert_distance_km")
            .remove("alert_units")
            .remove("alert_component")
            .remove("alert_eta_minutes")
            .apply()
        return alert
    }
}