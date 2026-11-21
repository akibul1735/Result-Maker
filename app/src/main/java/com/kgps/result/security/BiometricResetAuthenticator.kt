package com.kgps.result.security

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    READY,
    NONE_ENROLLED,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

sealed interface BiometricVerificationResult {
    data object Verified : BiometricVerificationResult
    data object Cancelled : BiometricVerificationResult
    data class FailedAttempt(val messageBn: String) : BiometricVerificationResult
    data class Error(val errorCode: Int, val messageBn: String) : BiometricVerificationResult
    data class Unavailable(val availability: BiometricAvailability, val messageBn: String) : BiometricVerificationResult
}

object BiometricResetAuthenticator {

    private const val FINGERPRINT_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun findFragmentActivity(context: Context): FragmentActivity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return null
    }

    fun checkFingerprintAvailability(context: Context): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(FINGERPRINT_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            else -> BiometricAvailability.UNSUPPORTED
        }
    }

    fun canUseDeviceCredentialFallback(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val manager = BiometricManager.from(context)
        val status = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun availabilityHintBn(availability: BiometricAvailability): String = when (availability) {
        BiometricAvailability.READY ->
            "ডাটা রিসেট নিশ্চিত করতে আপনার ডিভাইসের ফিঙ্গারপ্রিন্ট সেন্সরে আঙুল স্পর্শ করুন।"
        BiometricAvailability.NONE_ENROLLED ->
            "এই ডিভাইসে এখনো কোনো ফিঙ্গারপ্রিন্ট নিবন্ধিত নেই। নিরাপত্তা সেটিংস থেকে ফিঙ্গারপ্রিন্ট যোগ করুন অথবা নিচের বিকল্প যাচাই ব্যবহার করুন।"
        BiometricAvailability.NO_HARDWARE ->
            "এই ডিভাইসে ফিঙ্গারপ্রিন্ট সেন্সর পাওয়া যায়নি। নিচের নিরাপত্তা যাচাই পদ্ধতি ব্যবহার করুন।"
        BiometricAvailability.HARDWARE_UNAVAILABLE ->
            "ফিঙ্গারপ্রিন্ট সেন্সর বর্তমানে ব্যস্ত বা অস্থায়ীভাবে অনুপলব্ধ। কিছুক্ষণ পর আবার চেষ্টা করুন।"
        BiometricAvailability.SECURITY_UPDATE_REQUIRED ->
            "ডিভাইসের বায়োমেট্রিক নিরাপত্তা আপডেট প্রয়োজন।"
        BiometricAvailability.UNSUPPORTED ->
            "এই ডিভাইসে বায়োমেট্রিক যাচাই সমর্থিত নয়।"
    }

    fun openBiometricEnrollSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(AndroidSettings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    AndroidSettings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    FINGERPRINT_AUTHENTICATORS
                )
            }
        } else {
            Intent(AndroidSettings.ACTION_SECURITY_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallback = Intent(AndroidSettings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun authenticateWithFingerprint(
        context: Context,
        titleBn: String = "ফিঙ্গারপ্রিন্ট যাচাই প্রয়োজন",
        subtitleBn: String = "ডাটা রিসেট করার চূড়ান্ত অনুমতি দিন",
        warningDescriptionBn: String,
        useDeviceCredentialFallback: Boolean = false,
        onResult: (BiometricVerificationResult) -> Unit
    ) {
        val activity = findFragmentActivity(context)
        if (activity == null) {
            onResult(
                BiometricVerificationResult.Error(
                    errorCode = -1,
                    messageBn = "অ্যাক্টিভিটি সেশন পাওয়া যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"
                )
            )
            return
        }

        val availability = checkFingerprintAvailability(activity)
        val allowDeviceCredential =
            useDeviceCredentialFallback && canUseDeviceCredentialFallback(activity)

        if (availability != BiometricAvailability.READY && !allowDeviceCredential) {
            onResult(
                BiometricVerificationResult.Unavailable(
                    availability = availability,
                    messageBn = availabilityHintBn(availability)
                )
            )
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricVerificationResult.Verified)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(
                    BiometricVerificationResult.FailedAttempt(
                        "ফিঙ্গারপ্রিন্ট মেলেনি। অনুগ্রহ করে আবার চেষ্টা করুন।"
                    )
                )
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        onResult(BiometricVerificationResult.Cancelled)
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                        onResult(
                            BiometricVerificationResult.Unavailable(
                                availability = availability,
                                messageBn = errString.toString().ifBlank { availabilityHintBn(availability) }
                            )
                        )
                    }
                    else -> {
                        onResult(
                            BiometricVerificationResult.Error(
                                errorCode = errorCode,
                                messageBn = "যাচাই ব্যর্থ হয়েছে: ${errString.ifBlank { "অজানা ত্রুটি" }}"
                            )
                        )
                    }
                }
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titleBn)
            .setSubtitle(subtitleBn)
            .setDescription(warningDescriptionBn)
            .setConfirmationRequired(true)

        if (allowDeviceCredential && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setAllowedAuthenticators(FINGERPRINT_AUTHENTICATORS)
                .setNegativeButtonText("বাতিল করুন")
        }

        prompt.authenticate(builder.build())
    }
}
