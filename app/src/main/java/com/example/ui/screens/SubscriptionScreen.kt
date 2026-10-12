package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.SubscriptionConstants
import com.example.billing.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.RoyalMaroonPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MandapViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: MandapViewModel,
    onBack: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var selectedPlanId by remember { mutableStateOf(SubscriptionConstants.PRODUCT_SUBSCRIPTION_YEARLY) }
    var showFaqDialog by remember { mutableStateOf(false) }

    LaunchedEffect(billingState.userMessage) {
        billingState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearBillingMessage()
        }
    }

    val plans = if (billingState.availablePlans.isNotEmpty()) {
        billingState.availablePlans
    } else {
        listOf(
            SubscriptionPlan(
                productId = SubscriptionConstants.PRODUCT_SUBSCRIPTION_YEARLY,
                title = "1 Year Pro Plan (૧ વર્ષનો પ્લાન)",
                description = "90 દિવસ ફ્રી ટ્રાયલ પછી માત્ર ₹149 / વર્ષ. તમામ ફીચર્સ અનલિમિટેડ.",
                formattedPrice = "₹149 / વર્ષ",
                billingPeriod = "વર્ષ",
                hasFreeTrial = true,
                trialDurationDays = 90
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Google Play સબ્સ્ક્રિપ્શન",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("subscription_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.restorePurchases()
                            Toast.makeText(context, "Google Play ખરીદી તપાસવામાં આવી રહી છે...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("restore_purchases_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restore Purchases"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
        ) {
            // Header Hero Banner
            item {
                SubscriptionHeroBanner(
                    isTrialActive = billingState.isTrialActive,
                    isSubscribed = billingState.isSubscribed,
                    daysRemaining = billingState.trialDaysRemaining,
                    endDate = billingState.trialEndDate
                )
            }

            // Current Trial / Subscription Status Card
            item {
                TrialStatusCard(
                    isSubscribed = billingState.isSubscribed,
                    isTrialActive = billingState.isTrialActive,
                    daysRemaining = billingState.trialDaysRemaining,
                    orderId = billingState.orderId,
                    onManageGooglePlay = {
                        viewModel.openPlayStoreSubscriptionSettings(context)
                    }
                )
            }

            // Plan Selection
            item {
                Text(
                    text = "પ્લાન પસંદ કરો (Choose Your Plan)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(plans) { plan ->
                SubscriptionPlanCard(
                    plan = plan,
                    isSelected = selectedPlanId == plan.productId,
                    onSelect = { selectedPlanId = plan.productId }
                )
            }

            // CTA Button to start 90-day Free Trial
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (activity != null) {
                                val launched = viewModel.launchBillingFlow(
                                    activity = activity,
                                    productId = selectedPlanId,
                                    onFallback = {
                                        viewModel.startLocal90DayFreeTrial()
                                        Toast.makeText(
                                            context,
                                            "90 દિવસનો ફ્રી ટ્રાયલ સફળતાપૂર્વક સક્રિય થયો!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                                if (!launched) {
                                    Toast.makeText(
                                        context,
                                        "90 દિવસનો ફ્રી ટ્રાયલ ચાલુ કરવામાં આવ્યો છે!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_free_trial_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalMaroonPrimary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AmberGoldSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "૯૦ દિવસનો ફ્રી ટ્રાયલ શરૂ કરો (Start Trial)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "✓ 90 દિવસ ફ્રી ટ્રાયલ  •  પછી માત્ર ₹149/વર્ષ  •  Google Play સુરક્ષા",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Referral Program Card (+60 Extra Days for inviting friends)
            item {
                SubscriptionReferralCard(
                    userProfile = userProfile,
                    onShareWhatsApp = { code -> shareReferralCodeOnWhatsApp(context, code) },
                    onCopyCode = { code ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("MandapVala Referral Code", code))
                        Toast.makeText(context, "રેફરલ કોડ કોપી થયો: $code", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Features Included
            item {
                FeaturesSection()
            }

            // Google Play Store Guarantee & FAQ Box
            item {
                GooglePlayPolicyCard(
                    onOpenPlayStore = {
                        viewModel.openPlayStoreSubscriptionSettings(context)
                    }
                )
            }
        }
    }
}

@Composable
fun SubscriptionHeroBanner(
    isTrialActive: Boolean,
    isSubscribed: Boolean,
    daysRemaining: Int,
    endDate: Long
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            RoyalMaroonPrimary,
            Color(0xFF500010),
            Color(0xFF280008)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_hero_banner"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Pill Badge
                Surface(
                    color = AmberGoldSecondary,
                    shape = RoundedCornerShape(50),
                    contentColor = Color.Black
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF6B4700)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "90 DAYS FREE TRIAL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color(0xFF422B00)
                        )
                    }
                }

                Text(
                    text = "મંડપવાલા પ્રો - ૯૦ દિવસ ફ્રી ટ્રાયલ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "૯૦ દિવસ (૩ મહિના) સુધી તમામ પ્રીમિયમ ફીચર્સનો સંપૂર્ણપણે મફત ઉપયોગ કરો. Google Play Store દ્વારા સુરક્ષિત.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )

                // Trial remaining progress badge
                if (isTrialActive || isSubscribed) {
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = AmberGoldSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSubscribed) "પ્રો સબ્સ્ક્રિપ્શન એક્ટિવ" else "ફ્રી ટ્રાયલ બાકી:",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = if (isSubscribed) "VERIFIED" else "$daysRemaining દિવસ બાકી",
                                color = AmberGoldSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrialStatusCard(
    isSubscribed: Boolean,
    isTrialActive: Boolean,
    daysRemaining: Int,
    orderId: String?,
    onManageGooglePlay: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trial_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSubscribed || isTrialActive) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSubscribed || isTrialActive) Icons.Default.CheckCircle else Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (isSubscribed || isTrialActive) SuccessGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when {
                                isSubscribed -> "ગૂગલ પ્લે સબ્સ્ક્રિપ્શન સક્રિય છે"
                                isTrialActive -> "૯૦ દિવસનો ફ્રી ટ્રાયલ ચાલુ છે"
                                else -> "ફ્રી ટ્રાયલ પૂર્ણ થઈ ગયો છે"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = when {
                                isSubscribed -> "Order ID: ${orderId ?: "Google Play Sub"}"
                                isTrialActive -> "$daysRemaining દિવસ બાકી છે (Full Pro Access)"
                                else -> "તમામ ફીચર્સ ચાલુ રાખવા માટે પ્લાન પસંદ કરો"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSubscribed) {
                    TextButton(onClick = onManageGooglePlay) {
                        Text("મેનેજ કરો")
                    }
                }
            }

            if (isTrialActive && !isSubscribed) {
                val progress = (daysRemaining.toFloat() / 90f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SuccessGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) RoyalMaroonPrimary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val containerColor = if (isSelected) {
        RoyalMaroonPrimary.copy(alpha = 0.05f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("plan_card_${plan.productId}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) RoyalMaroonPrimary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(RoyalMaroonPrimary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = plan.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = plan.trialDurationFormatted,
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = plan.formattedPrice,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = RoyalMaroonPrimary
                    )
                    Text(
                        text = "ટ્રાયલ પછી",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeaturesSection() {
    val features = listOf(
        SubscriptionFeature(
            icon = Icons.Default.LockOpen,
            title = "અમર્યાદિત બુકિંગ અને ઓર્ડર્સ",
            desc = "મંડપ, સાઉન્ડ, લાઇટિંગ અને કેટરિંગના અનલિમિટેડ બિલ અને બુકિંગ બનાવો"
        ),
        SubscriptionFeature(
            icon = Icons.Default.Collections,
            title = "૧ થી ૧૦૦૦ ફોટો ડિજિટલ કેટલોગ",
            desc = "તમારા ડેકોરેશન ફોટા અપલોડ કરો અને ગ્રાહકને લિંક શેર કરીને ઓર્ડર મેળવો"
        ),
        SubscriptionFeature(
            icon = Icons.Default.PictureAsPdf,
            title = "ઓટોમેટિક WhatsApp PDF ઇનવોઇસ",
            desc = "સિંગલ ક્લિકમાં ગ્રાહકને સુંદર WhatsApp PDF બિલ અને ક્વોટેશન મોકલો"
        ),
        SubscriptionFeature(
            icon = Icons.Default.QrCode2,
            title = "ઓનલાઇન UPI QR કોડ અને પેમેન્ટ ટ્રેકિંગ",
            desc = "એડવાન્સ અને બાકી રકમનું ઓટોમેટિક કેલ્ક્યુલેશન અને QR કોડ"
        ),
        SubscriptionFeature(
            icon = Icons.Default.CloudDone,
            title = "ક્લાઉડ બેકઅપ અને ડેટા સિક્યુરિટી",
            desc = "તમારા તમામ ગ્રાહકો અને હિસાબનો ડેટા હંમેશા સુરક્ષિત રહેશે"
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "પ્રો ફીચર્સ (Included in 90-Day Free Trial)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        features.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = RoyalMaroonPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = item.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class SubscriptionFeature(
    val icon: ImageVector,
    val title: String,
    val desc: String
)

@Composable
fun GooglePlayPolicyCard(onOpenPlayStore: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Google Play Store નીતિ અને ગેરંટી",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "• 90 દિવસના ફ્રી ટ્રાયલ દરમિયાન તમારા ખાતામાંથી કોઈ રકમ કપાશે નહીં.\n• તમે Google Play Store > Payments & subscriptions માં જઈને કોઈપણ સમયે ટ્રાયલ કેન્સલ કરી શકો છો.\n• સબ્સ્ક્રિપ્શન રિન્યુ થવાના 24 કલાક પહેલાં Google Play તરફથી રિમાઇન્ડર મોકલવામાં આવે છે.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            TextButton(
                onClick = onOpenPlayStore,
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Google Play સબ્સ્ક્રિપ્શન સેટિંગ્સ")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun SubscriptionReferralCard(
    userProfile: UserProfile?,
    onShareWhatsApp: (String) -> Unit,
    onCopyCode: (String) -> Unit
) {
    val refCode = userProfile?.referralCode ?: "MV-FREE"
    val refCount = userProfile?.referralCount ?: 0
    val bonusDays = userProfile?.bonusDaysAdded ?: 0

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_referral_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "રેફરલ સિસ્ટમ: +60 દિવસ ફ્રી મેળવો",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "દર 3 નવા મિત્રો જોડાવા પર 60 દિવસ વધુ મફત!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

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
                        Text(
                            text = "તમારો રેફરલ કોડ:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = refCode,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp,
                            color = RoyalMaroonPrimary
                        )
                    }

                    Row {
                        IconButton(onClick = { onCopyCode(refCode) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code")
                        }
                        IconButton(onClick = { onShareWhatsApp(refCode) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share on WhatsApp", tint = Color(0xFF25D366))
                        }
                    }
                }
            }

            val currentProgress = refCount % 3
            val needed = if (currentProgress == 0 && refCount > 0) 3 else (3 - currentProgress)

            Text(
                text = "તમે અત્યાર સુધી $refCount મિત્રોને રેફર કર્યા છે (બોનસ: +$bonusDays દિવસ મળ્યા).",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "હજુ $needed નવા મિત્રો તમારા કોડથી જોડાઈને રજીસ્ટર કરશે એટલે તરત જ +60 દિવસ ફ્રી ઉમેરાશે!",
                style = MaterialTheme.typography.bodySmall,
                color = RoyalMaroonPrimary,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedButton(
                onClick = { onShareWhatsApp(refCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_share_referral_subscription"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("WhatsApp પર મિત્રોને આમંત્રિત કરો (+60 Days)")
            }
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

