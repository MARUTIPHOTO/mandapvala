package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BusinessProfile
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.RoyalMaroonPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MandapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MandapViewModel,
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val business by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val allBusinesses by viewModel.allBusinesses.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val currentBiz = business ?: BusinessProfile()

    // Form states
    var name by remember(currentBiz.name) { mutableStateOf(currentBiz.name) }
    var ownerName by remember(currentBiz.ownerName) { mutableStateOf(currentBiz.ownerName) }
    var mobile by remember(currentBiz.mobile) { mutableStateOf(currentBiz.mobile) }
    var whatsapp by remember(currentBiz.whatsapp) { mutableStateOf(currentBiz.whatsapp) }
    var address by remember(currentBiz.address) { mutableStateOf(currentBiz.address) }
    var city by remember(currentBiz.city) { mutableStateOf(currentBiz.city) }
    var gstNumber by remember(currentBiz.gstNumber) { mutableStateOf(currentBiz.gstNumber) }
    var email by remember(currentBiz.email) { mutableStateOf(currentBiz.email) }

    // Payment fields
    var upiId by remember(currentBiz.upiId) { mutableStateOf(currentBiz.upiId) }
    var bankName by remember(currentBiz.bankName) { mutableStateOf(currentBiz.bankName) }
    var accountNumber by remember(currentBiz.bankAccount) { mutableStateOf(currentBiz.bankAccount) }
    var ifscCode by remember(currentBiz.bankIfsc) { mutableStateOf(currentBiz.bankIfsc) }
    var terms by remember(currentBiz.termsAndConditions) { mutableStateOf(currentBiz.termsAndConditions) }
    var catalogUrl by remember(currentBiz.catalogUrl) { mutableStateOf(currentBiz.catalogUrl) }

    var showNewBusinessDialog by remember { mutableStateOf(false) }
    var showSwitchBusinessDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("Business Settings & Branding", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Active White Label Business Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_mandap_logo),
                                    contentDescription = "Business Logo",
                                    modifier = Modifier.size(48.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AmberGoldSecondary
                                ) {
                                    Text(
                                        text = "WHITE LABEL ACTIVE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentBiz.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Owner: ${currentBiz.ownerName} • ${currentBiz.city}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showSwitchBusinessDialog = true }) {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Business (${allBusinesses.size})")
                            }
                            OutlinedButton(
                                onClick = { showNewBusinessDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Register New")
                            }
                        }
                    }
                }
            }

            // Google Account & One-Tap Sign In Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "G",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (!userProfile?.email.isNullOrBlank()) "Google એકાઉન્ટ લિંક્ડ" else "Google એકાઉન્ટ",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = userProfile?.email?.ifBlank { null } ?: "Google વડે સાઇન ઇન કરો",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (userProfile != null) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.signOut()
                                            Toast.makeText(context, "સફળતાપૂર્વક લોગ આઉટ થયા!", Toast.LENGTH_SHORT).show()
                                            onNavigateToAuth()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("લોગ આઉટ")
                                    }
                                }
                                OutlinedButton(
                                    onClick = onNavigateToAuth,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (!userProfile?.email.isNullOrBlank()) "ખાતું બદલો" else "સાઇન ઇન")
                                }
                            }
                        }
                    }
                }
            }

            // Google Play Subscription & 90-Day Free Trial Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSubscription() }
                        .testTag("settings_subscription_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (billingState.isSubscribed) {
                            RoyalMaroonPrimary.copy(alpha = 0.08f)
                        } else {
                            AmberGoldSecondary.copy(alpha = 0.15f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (billingState.isSubscribed) SuccessGreen else AmberGoldSecondary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = if (billingState.isSubscribed) Color.White else Color(0xFF422B00),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Google Play સબ્સ્ક્રિપ્શન",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (billingState.isSubscribed) {
                                            "પ્રો વર્ઝન સક્રિય (Active Pro)"
                                        } else {
                                            "૯૦ દિવસનો ફ્રી ટ્રાયલ (${billingState.trialDaysRemaining} દિવસ બાકી)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToSubscription,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalMaroonPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (billingState.isSubscribed) "મેનેજ કરો" else "ટ્રાયલ જુઓ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "અમર્યાદિત બુકિંગ, 1000+ ફોટો કેટલોગ અને WhatsApp PDF બિલિંગ Google Play Store દ્વારા સુરક્ષિત.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Referral Program & Free Trial Days Extension Card
            item {
                val refCode = userProfile?.referralCode ?: "MV-FREE"
                val refCount = userProfile?.referralCount ?: 0
                val bonusDays = userProfile?.bonusDaysAdded ?: 0

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
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
                                        text = "રેફરલ પ્રોગ્રામ (+60 દિવસ મફત)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "3 મિત્રો જોડાવા પર 60 દિવસ ફ્રી ટ્રાયલ ઉમેરાય છે",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Referral Code Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "તમારો રેફરલ કોડ:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = refCode,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MandapVala Referral Code", refCode))
                                        Toast.makeText(context, "કોડ કોપી થયો: $refCode", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                    IconButton(onClick = {
                                        val shareText = "મંડપ અને ડેકોરેશન બિઝનેસ માટે MandapVala એપ વાપરો!\nમારો રેફરલ કોડ: *$refCode* વાપરીને મેળવો 90 દિવસ ફ્રી ટ્રાયલ!\nhttps://marutiphoto.github.io/mandapvala/"
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Referral Code"))
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF25D366))
                                    }
                                }
                            }
                        }

                        Text(
                            text = "તમારા સફળ રેફરલ્સ: $refCount  •  બોનસ દિવસો: +$bonusDays દિવસ",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Dark / Light Theme Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = AmberGoldSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Royal Dark Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isDarkTheme) "Festive Maroon & Dark Gold canvas" else "Clean bright light theme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkTheme(it) },
                            modifier = Modifier.testTag("theme_toggle_switch")
                        )
                    }
                }
            }

            // Business Details Form Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Business Profile Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "This appears on all customer gallery links, PDF invoices, and estimates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Business Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner / Manager Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { mobile = it },
                                label = { Text("Mobile Number *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = whatsapp,
                                onValueChange = { whatsapp = it },
                                label = { Text("WhatsApp Number") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = gstNumber,
                                onValueChange = { gstNumber = it },
                                label = { Text("GST Number (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Office / Warehouse Address") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Payment & UPI Details Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bank & UPI Payment QR",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "A live UPI QR code is auto-generated on PDF invoices for instant customer payments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID (e.g. business@okaxis) *") },
                            placeholder = { Text("example@upi") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = { Text("Account No.") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it },
                                label = { Text("IFSC Code") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = terms,
                            onValueChange = { terms = it },
                            label = { Text("Terms & Conditions (Invoice Footer)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = catalogUrl,
                            onValueChange = { catalogUrl = it },
                            label = { Text("Customer Catalog Website URL") },
                            placeholder = { Text("https://marutiphoto.github.io/mandapvala/") },
                            supportingText = { Text("This URL is shared with customers on WhatsApp so they can browse mandap photos on mobile.") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val updated = currentBiz.copy(
                                    name = name.trim(),
                                    ownerName = ownerName.trim(),
                                    mobile = mobile.trim(),
                                    whatsapp = whatsapp.trim().ifEmpty { mobile.trim() },
                                    address = address.trim(),
                                    city = city.trim(),
                                    gstNumber = gstNumber.trim(),
                                    email = email.trim(),
                                    upiId = upiId.trim(),
                                    bankName = bankName.trim(),
                                    bankAccount = accountNumber.trim(),
                                    bankIfsc = ifscCode.trim(),
                                    termsAndConditions = terms.trim(),
                                    catalogUrl = catalogUrl.trim()
                                )
                                viewModel.updateBusinessProfile(updated)
                                Toast.makeText(context, "Business Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_business_profile_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalMaroonPrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Changes")
                        }
                    }
                }
            }

            // Cloud Sync & Architecture Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Multi-Business & Cloud Sync", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "• Offline First: Local Room Database persists everything securely on device.\n" +
                                    "• White Label: Each Mandap business gets their own branding, logo, invoices, and catalog.\n" +
                                    "• Firestore & Storage: Configured with production rules for multi-tenant cloud synchronization.\n" +
                                    "• Live Notifications: Customer submissions trigger instant sound alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = {
                            viewModel.resetSampleData()
                            Toast.makeText(context, "Sample data reset!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Sample Decor & Customers Data")
                        }
                    }
                }
            }

            // Privacy Policy & Google Play Compliance Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyPolicyDialog = true }
                        .testTag("settings_privacy_policy_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = RoyalMaroonPrimary.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "Privacy Policy",
                                            tint = RoyalMaroonPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "પ્રાઇવસી પોલિસી (Privacy Policy)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Google Play Policy & ડેટા સુરક્ષા નિયમો",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "તમારો ગ્રાહક અને બુકિંગ ડેટા Firebase માં કેવી રીતે સુરક્ષિત રહે છે અને એકાઉન્ટ ડિલીશનના નિયમો વાંચવા માટે અહીં ક્લિક કરો.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPrivacyPolicyDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("પોલિસી વાંચો")
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "MandapVala - Privacy Policy")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "MandapVala App - Privacy Policy & Data Safety:\n" +
                                            "• App: MandapVala by Maruti Photo\n" +
                                            "• Contact: marutiphoto1234@gmail.com\n" +
                                            "• Secure Firebase Cloud Database & Authentication\n" +
                                            "• Business Data Isolation (Strictly Private)\n" +
                                            "• Account & Data Deletion Supported\n" +
                                            "Catalog Link: ${currentBiz.catalogUrl.ifBlank { "https://marutiphoto.github.io/mandapvala/" }}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Privacy Policy"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalMaroonPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("શેર કરો")
                            }
                        }
                    }
                }
            }
        }
    }

    // Register New Business Dialog
    if (showNewBusinessDialog) {
        var newBizName by remember { mutableStateOf("") }
        var newOwnerName by remember { mutableStateOf("") }
        var newMobile by remember { mutableStateOf("") }
        var newCity by remember { mutableStateOf("") }
        var newUpi by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewBusinessDialog = false },
            title = { Text("Register New Mandap Business", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newBizName,
                        onValueChange = { newBizName = it },
                        label = { Text("Business Name *") },
                        placeholder = { Text("e.g. Royal Shahi Mandap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newOwnerName,
                        onValueChange = { newOwnerName = it },
                        label = { Text("Owner Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newMobile,
                        onValueChange = { newMobile = it },
                        label = { Text("Mobile *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCity,
                        onValueChange = { newCity = it },
                        label = { Text("City *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUpi,
                        onValueChange = { newUpi = it },
                        label = { Text("UPI ID (for payments)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBizName.isNotBlank() && newOwnerName.isNotBlank()) {
                            val newBiz = BusinessProfile(
                                id = "biz_${System.currentTimeMillis()}",
                                name = newBizName.trim(),
                                ownerName = newOwnerName.trim(),
                                mobile = newMobile.trim(),
                                whatsapp = newMobile.trim(),
                                city = newCity.trim(),
                                upiId = newUpi.trim(),
                                isActive = true
                            )
                            viewModel.updateBusinessProfile(newBiz)
                            showNewBusinessDialog = false
                            Toast.makeText(context, "New business registered!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = newBizName.isNotBlank() && newOwnerName.isNotBlank()
                ) {
                    Text("Create Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBusinessDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Switch Business Dialog
    if (showSwitchBusinessDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchBusinessDialog = false },
            title = { Text("Select Business Profile", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allBusinesses.size) { index ->
                        val b = allBusinesses[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchBusinessProfile(b)
                                    showSwitchBusinessDialog = false
                                    Toast.makeText(context, "Switched to ${b.name}", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (b.id == currentBiz.id) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(b.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${b.ownerName} • ${b.city} • ${b.mobile}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSwitchBusinessDialog = false }) { Text("Close") }
            }
        )
    }

    // Privacy Policy Dialog (Google Play Compliant)
    if (showPrivacyPolicyDialog) {
        val policyText = """
MandapVala - Privacy Policy & Data Safety
Effective Date: October 14, 2026
App Developer: Maruti Photo & Software Team
Contact: marutiphoto1234@gmail.com

1. Information We Collect:
- Business Profile: Name, Mobile, WhatsApp, Address, UPI ID, GST No.
- Client & Booking Records: Customer names, event dates, venues, decoration selections, bill totals.
- Account Credentials: Email & User ID via Firebase Authentication & Google Sign-In.

2. How We Use Information:
- Provide event calendar management, prevent double bookings, generate bills/PDFs, and sync data securely across devices.
- We DO NOT sell, rent, or trade your personal or business data to any third-party advertisers.

3. Third-Party Services:
- Google Play Services (Core Android features)
- Google Firebase Authentication (Secure 1-click & email login)
- Google Cloud Firestore (Secure real-time cloud database)

4. Security & Isolation:
- Each mandap owner's data is isolated and protected with zero-trust security rules.
- All cloud communications are encrypted via SSL/TLS.

5. Account & Data Deletion (Google Play Policy):
- You have the right to request deletion of your account and all data at any time.
- Send an email to marutiphoto1234@gmail.com with subject "Account Deletion Request - MandapVala". All your data will be permanently wiped within 30 days.

Website: https://marutiphoto.github.io/mandapvala/
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Privacy Policy",
                    tint = RoyalMaroonPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "પ્રાઇવસી પોલિસી (Privacy Policy)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            color = RoyalMaroonPrimary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "MandapVala • Maruti Photo Team",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoyalMaroonPrimary
                                )
                                Text(
                                    text = "સંપર્ક ઈમેલ: marutiphoto1234@gmail.com\nGoogle Play Store કમ્પ્લાયન્ટ પોલિસી",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "૧. અમે કયો ડેટા સાચવીએ છીએ?",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• તમારું નામ, પેઢીનું નામ, મોબાઈલ નંબર અને ઈમેલ.\n• ગ્રાહકોની વિગતો, ઇવેન્ટ તારીખો, ભાડાની રકમ અને ડેકોરેશન આઇટમ્સ.\n• બિલ/ઇન્વૉઇસ અને પેમેન્ટ સ્ટેટસ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "૨. ડેટાનો ઉપયોગ કેવી રીતે થાય છે?",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• ડબલ બુકિંગ અટકાવવા અને તારીખોનું મેનેજમેન્ટ કરવા.\n• ગ્રાહકો માટે સુંદર PDF બિલ અને કેટલોગ લિંક બનાવવા.\n• ગૂગલ ફાયરબેઝ (Firebase) ક્લાઉડમાં સુરક્ષિત બેકઅપ રાખવા.\n• અમે કોઈપણ યુઝરનો ડેટા ક્યારેય વેચતા નથી કે એડવર્ટાઇઝિંગ માટે આપતા નથી.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "૩. સુરક્ષા અને સિક્યોરિટી (Data Privacy)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• દરેક મંડપવાળાનો ડેટા સંપૂર્ણપણે અલગ અને પ્રાઇવેટ રહે છે. અન્ય કોઈ પણ વ્યક્તિ તમારો ડેટા જોઈ શકતી નથી.\n• તમામ ડેટા ટ્રાન્સફર SSL/TLS એન્ક્રિપ્શનથી સુરક્ષિત છે.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "૪. એકાઉન્ટ અને ડેટા ડિલીટ કરવાનો હક (Data Deletion)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• ગૂગલ પ્લે સ્ટોરના નિયમ મુજબ તમે કોઈપણ સમયે તમારું ખાતું અને ડેટા કાયમી ડિલીટ કરાવી શકો છો.\n• આ માટે marutiphoto1234@gmail.com પર ઈમેલ કરવાથી ૩૦ દિવસમાં બધો જ ડેટા સર્વર પરથી સંપૂર્ણ ભૂંસી નાખવામાં આવશે.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MandapVala Privacy Policy", policyText))
                            Toast.makeText(context, "પ્રાઇવસી પોલિસી કોપી થઈ ગઈ!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("કોપી")
                    }

                    Button(
                        onClick = {
                            try {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:marutiphoto1234@gmail.com?subject=Privacy%20Policy%20Inquiry%20-%20MandapVala")
                                }
                                context.startActivity(emailIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Email: marutiphoto1234@gmail.com", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalMaroonPrimary)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("સંપર્ક ઈમેલ")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("બંધ કરો")
                }
            }
        )
    }
}
