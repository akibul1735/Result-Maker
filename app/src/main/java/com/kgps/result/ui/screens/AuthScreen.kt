package com.kgps.result.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.ui.theme.RoseBg
import com.kgps.result.ui.theme.RoseDanger
import com.kgps.result.ui.theme.SlateBg
import com.kgps.result.ui.theme.SlateMuted
import com.kgps.result.ui.theme.SlateOutline
import com.kgps.result.ui.theme.SlateText
import com.kgps.result.ui.theme.TealContainer
import com.kgps.result.ui.theme.TealDark
import com.kgps.result.ui.theme.TealPrimary
import com.kgps.result.ui.theme.TealSoft

@Composable
fun AuthScreen(
    isSigningIn: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SlateBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = TealContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = "Result Maker",
                                tint = TealDark,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Result Maker",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateText
                        )
                        Text(
                            text = "প্রাথমিক বিদ্যালয়ের সম্মিলিত ফলাফল ও অগ্রগতি প্রতিবেদন",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TealPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Google অ্যাকাউন্ট দিয়ে প্রবেশ করে নতুন বিদ্যালয় তৈরি করুন অথবা স্কুল কোডের মাধ্যমে সহকর্মীদের সাথে যুক্ত হয়ে একসাথে নম্বর এন্ট্রি করুন।",
                            fontSize = 12.5.sp,
                            color = SlateMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }

                    Surface(
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AuthFeatureRow(
                                icon = Icons.Rounded.CloudDone,
                                title = "তাৎক্ষণিক ক্লাউড সিঙ্ক",
                                subtitle = "অফলাইনেও এন্ট্রি করা যাবে, অনলাইন হলেই স্বয়ংক্রিয়ভাবে আপডেট হবে।"
                            )
                            AuthFeatureRow(
                                icon = Icons.Rounded.GroupAdd,
                                title = "একাধিক শিক্ষক একসাথে কাজ",
                                subtitle = "ভিন্ন ভিন্ন বিষয়ে একসাথে ব্যাচ এন্ট্রি করলেও কারো নম্বর মুছবে না।"
                            )
                            AuthFeatureRow(
                                icon = Icons.Rounded.PictureAsPdf,
                                title = "NCTB A4 বুকলেট PDF",
                                subtitle = "সকল শ্রেণির প্রান্তিক ও চূড়ান্ত মূল্যায়ন প্রতিবেদন এক ক্লিকে প্রিন্টযোগ্য PDF।"
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = RoseBg,
                            border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_screen_error_text")
                        ) {
                            Text(
                                text = errorMessage,
                                color = RoseDanger,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onGoogleSignIn,
                        enabled = !isSigningIn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_screen_sign_in_btn")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(10.dp))
                            Text("সাইন-ইন হচ্ছে...", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(10.dp))
                            Text("Google দিয়ে সাইন-ইন করুন", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        }
                    }

                    Text(
                        text = "ডেটা Firebase Cloud Firestore-এ নিরাপদে সংরক্ষিত থাকে।",
                        fontSize = 11.sp,
                        color = SlateMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.padding(top = 2.dp).size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = SlateText)
            Text(subtitle, fontSize = 11.5.sp, color = SlateMuted, lineHeight = 16.sp)
        }
    }
}
