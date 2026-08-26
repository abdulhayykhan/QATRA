package com.qatra.app.data.repository

import com.qatra.app.BuildConfig
import com.qatra.app.data.model.Hospital
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Singleton provider for the configured Supabase client instance.
 * Extracted from QatraRepository so all sub-repositories share one client.
 */
object SupabaseClientProvider {

    val client: SupabaseClient? by lazy {
        if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }
        } else {
            null
        }
    }
}

/**
 * Seeded Karachi hospitals (PRD / Wireframes).
 * Shared across SeekerRepository and AdminRepository.
 */
object HospitalCatalog {
    val hospitals = listOf(
        Hospital(
            id = "H1",
            name = "Jinnah Postgraduate Medical Centre (JPMC)",
            shortName = "JPMC",
            address = "Rafiqui H.J. Shaheed Rd, Cantonment, Karachi",
            district = "Karachi South",
            xPercent = 0.50f,
            yPercent = 0.68f,
            isTraumaCenter = true
        ),
        Hospital(
            id = "H2",
            name = "Dr. Ruth K.M. Pfau Civil Hospital",
            shortName = "Civil Hospital",
            address = "Mission Rd, New Dehli Colony, Karachi",
            district = "Karachi South",
            xPercent = 0.44f,
            yPercent = 0.55f,
            isTraumaCenter = true
        ),
        Hospital(
            id = "H3",
            name = "Liaquat National Hospital & Medical College",
            shortName = "Liaquat National",
            address = "National Stadium Rd, Gulshan-e-Iqbal, Karachi",
            district = "Karachi East",
            xPercent = 0.58f,
            yPercent = 0.42f,
            isTraumaCenter = true
        ),
        Hospital(
            id = "H4",
            name = "The Indus Hospital",
            shortName = "Indus Hospital",
            address = "Korangi Crossing, Karachi",
            district = "Karachi Korangi",
            xPercent = 0.68f,
            yPercent = 0.76f,
            isTraumaCenter = true
        ),
        Hospital(
            id = "H5",
            name = "Aga Khan University Hospital (AKUH)",
            shortName = "Aga Khan Hospital",
            address = "Stadium Rd, Karachi",
            district = "Karachi East",
            xPercent = 0.60f,
            yPercent = 0.38f,
            isTraumaCenter = false
        ),
        Hospital(
            id = "H6",
            name = "Abbasi Shaheed Hospital",
            shortName = "Abbasi Shaheed",
            address = "Paposh Nagar, Nazimabad, Karachi",
            district = "Karachi Central",
            xPercent = 0.42f,
            yPercent = 0.32f,
            isTraumaCenter = true
        )
    )
}
