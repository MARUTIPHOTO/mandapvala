package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.GoogleSignInManager
import com.example.billing.SubscriptionConstants
import com.example.ui.viewmodel.MandapViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionRequiredScreen(
    viewModel: MandapViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? Activity
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Block back navigation to home screen!
    BackHandler {
        Toast.makeText(context, "સબ્સ્ક્રિપ્શન વગર એપ્લિકેશન વાપરી શકાશે નહીં.", Toast.LENGTH_SHORT).show()
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("gu", "IN")) }

    val regDateStr = userProfile?.registeredAt?.let { dateFormatter.format(Date(it)) } ?: "—"
    val expDateStr = userProfile?.expiryDate?.let { dateFormatter.format(Date(it)) } ?: "—"
    val referralCode = userProfile?.referralCode ?: "MV-FREE"
    val referralCount = userProfile?.referralCount ?: 0
    val bonusDays = userProfile?.bonusDaysAdded ?: 0

    var selectedPlanId by remember { mutableStateOf(SubscriptionConstants.PRODUCT_SUBSCRIPTION_YEARLY) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Locked App Badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Subscription Locked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "સબ્સ્ક્રિપ્શન આવશ્યક છે",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Subscription Required to Continue",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expiry Notice Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚠️ તમારો 90 દિવસનો ફ્રી ટ્રાયલ સમય સમાપ્ત થઈ ગયો છે.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "તમારું એકાઉન્ટ $regDateStr ના રોજ નોંધાયેલું હતું અને $expDateStr ના રોજ ટ્રાયલ સમાપ્ત થયું છે.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "મંડપ બિલિંગ, ઓર્ડર અને ગેલેરી ચાલુ રાખવા માટે નીચેનામાંથી કોઈપણ વિકલ્પ પસંદ કરો:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // METHOD 1: GOOGLE PLAY BILLING SUBSCRIPTION
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "વિકલ્પ 1: Google Play સબ્સ્ક્રિપ્શન",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "ઓફિશિયલ Google Play Store બિલિંગ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1 Year Plan Card (Only Plan: ₹149 / Year)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlanId = SubscriptionConstants.PRODUCT_SUBSCRIPTION_YEARLY }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "૧ વર્ષનો પ્લાન (1 Year Plan)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ઓન્લી ₹149",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "આખા ૧ વર્ષ માટે અનલિમિટેડ ઓર્ડર્સ, ૧૦૦૦+ ફોટો કેટલોગ અને બિલિંગ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "₹149 / વર્ષ",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (activity != null) {
                                viewModel.launchBillingFlow(activity, selectedPlanId) {
                                    Toast.makeText(context, "Google Play Store સાથે કનેક્ટ કરી રહ્યા છીએ...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_subscribe_google_play"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Google Play દ્વારા સબ્સ્ક્રાઇબ કરો", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // METHOD 2: REFERRAL PROGRAM (+60 EXTRA DAYS FREE)
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "વિકલ્પ 2: મિત્રોને રેફર કરો (+60 દિવસ ફ્રી)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "દર 3 નવા બિઝનેસ જોડાવા પર 60 દિવસ વધુ મફત!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Unique Referral Code Display
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "તમારો રેફરલ કોડ:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = referralCode,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MandapVala Referral Code", referralCode))
                                        Toast.makeText(context, "રેફરલ કોડ કોપી થયો: $referralCode", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("btn_copy_ref_code")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code")
                                }

                                IconButton(
                                    onClick = {
                                        shareReferralCodeOnWhatsApp(context, referralCode)
                                    },
                                    modifier = Modifier.testTag("btn_share_ref_code")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share on WhatsApp", tint = Color(0xFF25D366))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress indicator
                    val currentProgress = referralCount % 3
                    val needed = if (currentProgress == 0 && referralCount > 0) 3 else (3 - currentProgress)
                    Text(
                        text = "તમે અત્યાર સુધી $referralCount મિત્રોને રેફર કર્યા છે (બોનસ: +$bonusDays દિવસ મળ્યા).",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "હજુ $needed નવા મિત્રો તમારા કોડથી રજીસ્ટર કરશે એટલે તરત જ બીજા 60 દિવસ ફ્રી ઉમેરાશે!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { shareReferralCodeOnWhatsApp(context, referralCode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_whatsapp_invite"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp પર મિત્રોને આમંત્રિત કરો")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions row: Restore Purchases & Sign Out
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.restorePurchases()
                        Toast.makeText(context, "ખરીદી ચકાસી રહ્યા છીએ...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Purchases", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = {
                        val cm = CredentialManager.create(context)
                        GoogleSignInManager.clearCredentials(context, cm, coroutineScope)
                        viewModel.signOut()
                        onSignOut()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun shareReferralCodeOnWhatsApp(context: Context, referralCode: String) {
    val message = """
        નમસ્તે! તમારા મંડપ અને ડેકોરેશન બિઝનેસ માટે MandapVala એપ ડાઉનલોડ કરો!
        
        ✅ ઓનલાઇન કેટેલોગ અને ઓર્ડર સિસ્ટમ
        ✅ વ્હોટ્સએપ PDF બિલ અને UPI QR કોડ
        ✅ 90 દિવસ તદ્દન મફત ટ્રાયલ! (ત્યારબાદ માત્ર ₹149/વર્ષ)
        
        મારો રેફરલ કોડ વાપરો: *$referralCode*
        
        વેબસાઇટ / કેટેલોગ: https://marutiphoto.github.io/mandapvala/
    """.trimIndent()

    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val genericIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(genericIntent, "Share Referral Code"))
    }
}
