package com.kgps.result.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kgps.result.security.BiometricAvailability
import com.kgps.result.security.BiometricResetAuthenticator
import com.kgps.result.security.BiometricVerificationResult
import com.kgps.result.ui.theme.AmberBg
import com.kgps.result.ui.theme.AmberBorder
import com.kgps.result.ui.theme.AmberNotice
import com.kgps.result.ui.theme.RoseBg
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary

@Composable
fun BiometricDeleteConfirmDialog(
    title: String,
    description: String,
    warningDescription: String = description,
    confirmActionLabel: String = "ফিঙ্গারপ্রিন্ট দিয়ে মুছুন",
    confirmButtonTestTag: String = "biometric_confirm_delete_btn",
    onDismiss: () -> Unit,
    onConfirmedDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var biometricAvailability by remember {
        mutableStateOf(BiometricResetAuthenticator.checkFingerprintAvailability(ctx))
    }
    var hasDeviceLockFallback by remember {
        mutableStateOf(BiometricResetAuthenticator.canUseDeviceCredentialFallback(ctx))
    }
    var authStatusMsg by remember { mutableStateOf<String?>(null) }
    var authStatusIsError by remember { mutableStateOf(false) }
    var fallbackTypedConfirm by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner, ctx) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                biometricAvailability = BiometricResetAuthenticator.checkFingerprintAvailability(ctx)
                hasDeviceLockFallback = BiometricResetAuthenticator.canUseDeviceCredentialFallback(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = RoseBg,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = "ফিঙ্গারপ্রিন্ট অনুমতি",
                        tint = RoseDanger,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = SlateText
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = SlateText,
                    lineHeight = 18.sp
                )

                Surface(
                    color = SlateBg,
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TealContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = null,
                                        tint = TealDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ফিঙ্গারপ্রিন্ট এক্সেস ও অনুমতি",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = SlateText
                                )
                                Text(
                                    text = "যেকোনো তথ্য মুছে ফেলার আগে ফিঙ্গারপ্রিন্ট যাচাই প্রয়োজন।",
                                    fontSize = 11.sp,
                                    color = SlateMuted
                                )
                            }
                        }

                        Text(
                            text = BiometricResetAuthenticator.deleteAvailabilityHintBn(biometricAvailability),
                            fontSize = 11.5.sp,
                            color = if (biometricAvailability == BiometricAvailability.READY) TealDark else AmberNotice,
                            lineHeight = 16.sp
                        )

                        authStatusMsg?.let { msg ->
                            Surface(
                                color = if (authStatusIsError) RoseBg else AmberBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (authStatusIsError) RoseDanger.copy(alpha = 0.5f) else AmberBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 11.5.sp,
                                    color = if (authStatusIsError) RoseDanger else AmberNotice,
                                    modifier = Modifier.padding(9.dp),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        if (biometricAvailability == BiometricAvailability.READY) {
                            Button(
                                onClick = {
                                    authStatusMsg = null
                                    BiometricResetAuthenticator.authenticateWithFingerprint(
                                        context = ctx,
                                        titleBn = "ফিঙ্গারপ্রিন্ট অনুমতি প্রয়োজন",
                                        subtitleBn = title,
                                        warningDescriptionBn = warningDescription,
                                        useDeviceCredentialFallback = false
                                    ) { res ->
                                        when (res) {
                                            BiometricVerificationResult.Verified -> {
                                                onConfirmedDelete()
                                                onDismiss()
                                            }
                                            BiometricVerificationResult.Cancelled -> {
                                                authStatusIsError = false
                                                authStatusMsg = "ফিঙ্গারপ্রিন্ট অনুমতি বাতিল করা হয়েছে। কোনো তথ্য মুছে ফেলা হয়নি।"
                                            }
                                            is BiometricVerificationResult.FailedAttempt -> {
                                                authStatusIsError = true
                                                authStatusMsg = res.messageBn
                                            }
                                            is BiometricVerificationResult.Error -> {
                                                authStatusIsError = true
                                                authStatusMsg = res.messageBn
                                            }
                                            is BiometricVerificationResult.Unavailable -> {
                                                biometricAvailability = res.availability
                                                authStatusIsError = true
                                                authStatusMsg = res.messageBn
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag(confirmButtonTestTag)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.size(7.dp))
                                Text(
                                    text = confirmActionLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        } else {
                            if (biometricAvailability == BiometricAvailability.NONE_ENROLLED) {
                                OutlinedButton(
                                    onClick = {
                                        BiometricResetAuthenticator.openBiometricEnrollSettings(ctx)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.size(6.dp))
                                    Text("সেটিংস থেকে ফিঙ্গারপ্রিন্ট যোগ করুন", fontSize = 12.sp)
                                }
                            }

                            if (hasDeviceLockFallback) {
                                Button(
                                    onClick = {
                                        authStatusMsg = null
                                        BiometricResetAuthenticator.authenticateWithFingerprint(
                                            context = ctx,
                                            titleBn = "নিরাপত্তা অনুমতি প্রয়োজন",
                                            subtitleBn = title,
                                            warningDescriptionBn = warningDescription,
                                            useDeviceCredentialFallback = true
                                        ) { res ->
                                            when (res) {
                                                BiometricVerificationResult.Verified -> {
                                                    onConfirmedDelete()
                                                    onDismiss()
                                                }
                                                BiometricVerificationResult.Cancelled -> {
                                                    authStatusIsError = false
                                                    authStatusMsg = "যাচাই বাতিল করা হয়েছে।"
                                                }
                                                is BiometricVerificationResult.FailedAttempt -> {
                                                    authStatusIsError = true
                                                    authStatusMsg = res.messageBn
                                                }
                                                is BiometricVerificationResult.Error -> {
                                                    authStatusIsError = true
                                                    authStatusMsg = res.messageBn
                                                }
                                                is BiometricVerificationResult.Unavailable -> {
                                                    authStatusIsError = true
                                                    authStatusMsg = res.messageBn
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag(confirmButtonTestTag)
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.size(7.dp))
                                    Text("ডিভাইস লক দিয়ে মুছুন", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            } else {
                                Surface(
                                    color = AmberBg,
                                    border = BorderStroke(1.dp, AmberBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Shield,
                                                contentDescription = null,
                                                tint = AmberNotice,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "বিকল্প নিরাপত্তা যাচাই",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = AmberNotice
                                            )
                                        }
                                        Text(
                                            text = "ডিভাইসে ফিঙ্গারপ্রিন্ট বা স্ক্রিন-লক না থাকায় নিশ্চিত করতে ইংরেজিতে DELETE লিখুন:",
                                            fontSize = 11.5.sp,
                                            color = SlateText,
                                            lineHeight = 15.sp
                                        )
                                        OutlinedTextField(
                                            value = fallbackTypedConfirm,
                                            onValueChange = { fallbackTypedConfirm = it.trim().uppercase() },
                                            label = { Text("DELETE লিখুন") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = {
                                                onConfirmedDelete()
                                                onDismiss()
                                            },
                                            enabled = fallbackTypedConfirm == "DELETE",
                                            colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag(confirmButtonTestTag)
                                        ) {
                                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("নিশ্চিত করে মুছুন", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল করুন")
            }
        }
    )
}
