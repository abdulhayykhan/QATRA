package com.qatra.app.data.repository

import android.app.Activity
import com.qatra.app.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import timber.log.Timber

class AuthRepository {
    var lastAuthErrorMessage: String? = null
        private set

    private var firebaseVerificationId: String? = null
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun normalizeE164PhoneNumber(phoneNumber: String): String {
        val digits = phoneNumber.filter(Char::isDigit)
        return when {
            digits.isEmpty() -> ""
            digits.startsWith("92") && digits.length == 12 -> "+$digits"
            digits.startsWith("0") && digits.length == 11 -> "+92${digits.drop(1)}"
            digits.length == 10 && digits.startsWith("3") -> "+92$digits"
            phoneNumber.startsWith("+") -> phoneNumber
            else -> "+$digits"
        }
    }

    private fun setLastAuthError(message: String?) {
        lastAuthErrorMessage = message
    }

    fun sendFirebaseOtp(activity: Activity, phoneNumber: String, onResult: (Boolean) -> Unit) {
        val normalizedPhone = normalizeE164PhoneNumber(phoneNumber)
        if (normalizedPhone.isBlank()) {
            setLastAuthError("Phone number is required for OTP sign-in.")
            onResult(false)
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener {
                        setLastAuthError(it.message ?: "Firebase phone verification failed.")
                        onResult(false)
                    }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                setLastAuthError(exception.message ?: "Firebase phone verification failed.")
                onResult(false)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                firebaseVerificationId = verificationId
                setLastAuthError(null)
                onResult(true)
            }
        }

        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(normalizedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    suspend fun verifyOtp(code: String): Boolean {
        val verificationId = firebaseVerificationId ?: run {
            setLastAuthError("No Firebase OTP verification is pending.")
            return false
        }

        if (code.length != 6 || code.any { !it.isDigit() }) {
            setLastAuthError("The OTP must be a 6-digit number.")
            return false
        }

        val client = SupabaseClientProvider.client ?: run {
            setLastAuthError("Supabase client is not configured. Check SUPABASE_URL and SUPABASE_ANON_KEY.")
            return false
        }

        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val firebaseUser = firebaseAuth.signInWithCredentialAwait(credential)
            val firebaseToken = firebaseUser.getIdToken(true).awaitResult().token
                ?: throw IllegalStateException("Firebase did not return an ID token.")
            val response = httpClient.newCall(
                Request.Builder()
                    .url("${BuildConfig.SUPABASE_URL}/functions/v1/verify-firebase-phone")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    .post(
                        JSONObject().put("firebase_id_token", firebaseToken)
                            .toString()
                            .toRequestBody("application/json".toMediaType())
                    )
                    .build()
            ).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Phone session exchange failed (${response.code}).")
            }
            val session = JSONObject(response.body?.string().orEmpty())
            client.auth.importAuthToken(
                accessToken = session.getString("access_token"),
                refreshToken = session.getString("refresh_token")
            )
            firebaseVerificationId = null
            setLastAuthError(null)
            true
        } catch (exception: Exception) {
            Timber.e(exception, "OTP verification failed")
            setLastAuthError(exception.message ?: exception::class.simpleName ?: "Unknown OTP verification error")
            false
        }
    }

    /**
     * Signs the admin in against the real Supabase auth backend with an email/password
     * credential. The admin identity carries a `user_role=admin` claim in its JWT, which is
     * what the row-level-security policies check — so authority lives on the server, not in
     * the client.
     */
    suspend fun adminSignIn(email: String, password: String): Boolean {
        val client = SupabaseClientProvider.client ?: return false
        return try {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            setLastAuthError(null)
            true
        } catch (exception: Exception) {
            Timber.e(exception, "Admin sign-in failed")
            setLastAuthError(exception.message ?: "Invalid admin credentials.")
            false
        }
    }

    /** Tears down the admin Supabase session so a signed-out terminal cannot keep admin authority. */
    suspend fun adminSignOut() {
        val client = SupabaseClientProvider.client ?: return
        runCatching { client.auth.signOut() }
    }

    private suspend fun FirebaseAuth.signInWithCredentialAwait(
        credential: PhoneAuthCredential
    ) = signInWithCredential(credential).awaitResult().user
        ?: throw IllegalStateException("Firebase did not return a signed-in user.")

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
            addOnCanceledListener { continuation.cancel() }
        }
}
