package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.GoogleSignInManager
import com.example.ui.viewmodel.MandapViewModel

@Composable
fun AuthScreen(
    viewModel: MandapViewModel,
    onAuthSuccess: () -> Unit,
    onSkip: () -> Unit = onAuthSuccess,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Sign Up, 1: Sign In

    // Sign Up fields
    var name by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Silent auto sign-in attempt on startup if authorized account is cached
    LaunchedEffect(Unit) {
        GoogleSignInManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = coroutineScope,
            onTokenReceived = { idToken ->
                isLoading = true
                viewModel.signInWithGoogle(idToken) { result ->
                    isLoading = false
                    result.onSuccess {
                        Toast.makeText(context, "Google વડે ઓટો સાઇન ઇન સફળ!", Toast.LENGTH_SHORT).show()
                        onAuthSuccess()
                    }
                }
            },
            onNoCredential = {
                // Wait for manual click
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Brand Header
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "MandapVala Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "MandapVala",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "મંડપ & ડેકોરેશન મેનેજમેન્ટ સિસ્ટમ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 90-Day Free Trial Highlight Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "🎁 90 દિવસ તદ્દન મફત ટ્રાયલ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "તમામ સુવિધાઓ 90 દિવસ ફ્રી! કોઈ પાસવર્ડ કે OTP વગર સીધા 1-ક્લિકમાં શરૂ કરો.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // PRIMARY HERO: ONE-TAP GOOGLE SIGN-IN CARD
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_google_one_tap")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google વડે 1-ક્લિક સાઇન ઇન",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "પાસવર્ડ કે OTP વગર • સીધા ઈમેલથી",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Referral Code field for new Google Sign In users
                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it },
                        label = { Text("મિત્રનો રેફરલ કોડ (જો હોય તો - વૈકલ્પિક)") },
                        leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_google_referral_code")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val activity = context as? Activity
                            if (activity == null) {
                                errorMessage = "Activity context is required for Google Sign-In."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            GoogleSignInManager.launchInteractiveGoogleSignIn(
                                activity = activity,
                                credentialManager = credentialManager,
                                scope = coroutineScope,
                                onTokenReceived = { idToken ->
                                    viewModel.signInWithGoogle(
                                        idToken = idToken,
                                        referredByCode = referralCode.ifBlank { null }
                                    ) { result ->
                                        isLoading = false
                                        result.onSuccess {
                                            Toast.makeText(context, "Google સાઇન ઇન સફળ! 90 દિવસ ફ્રી ટ્રાયલ શરૂ થઈ.", Toast.LENGTH_LONG).show()
                                            onAuthSuccess()
                                        }.onFailure { err ->
                                            errorMessage = err.message ?: "Google સાઇન ઇન નિષ્ફળ રહ્યું."
                                        }
                                    }
                                },
                                onError = { errorMsg ->
                                    isLoading = false
                                    errorMessage = errorMsg
                                },
                                onCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_google_signin"),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Google એકાઉન્ટથી સાઇન ઇન કરો",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Quick 1-Click Trial Button (No password, zero friction)
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            viewModel.quickStartTrial(
                                name = "Mandap Partner",
                                businessName = "Shree Mandap & Decorators",
                                referralCode = referralCode.ifBlank { null }
                            ) { result ->
                                isLoading = false
                                result.onSuccess {
                                    Toast.makeText(context, "90 દિવસ ફ્રી ટ્રાયલ સફળતાપૂર્વક શરૂ થઈ!", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                }.onFailure { err ->
                                    errorMessage = err.message ?: "સાઇન ઇન નિષ્ફળ."
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_quick_trial_signin"),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ઝડપી 1-ક્લિક શરૂ કરો (પાસવર્ડ વગર)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚡ એક ક્લિકમાં 90 દિવસ ફ્રી ટ્રાયલ આપોઆપ શરૂ થઈ જશે",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Skip for now button
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("btn_auth_skip_to_home")
                    ) {
                        Text(
                            text = "અત્યારે સ્કીપ કરો (ગેસ્ટ તરીકે આગળ વધો) →",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Error display if any
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.quickStartTrial(
                                        name = "Mandap Partner",
                                        businessName = "Shree Mandap & Decorators"
                                    ) { result ->
                                        isLoading = false
                                        result.onSuccess { onAuthSuccess() }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("btn_error_quick_continue")
                            ) {
                                Text("ઝડપી સાઇન ઇનથી આગળ વધો")
                            }
                            TextButton(onClick = onSkip) {
                                Text("સ્કીપ કરો", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Divider: Alternative Email Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  અથવા મેન્યુઅલ રજીસ્ટ્રેશન  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // Tab Selector: Sign Up vs Sign In
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        errorMessage = null
                    },
                    text = { Text("નવું એકાઉન્ટ (Sign Up)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_signup")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        errorMessage = null
                    },
                    text = { Text("લોગિન (Sign In)", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_signin")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // SIMPLE SIGN UP FORM
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ઈમેલ (Email) *") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_signup_email")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("પાસવર્ડ (Password - ઓછામાં ઓછા 6 અક્ષર) *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_signup_password")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("મંડપ / ફર્મનું નામ (વૈકલ્પિક - Optional)") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    singleLine = true,
                    placeholder = { Text("શ્રી મંડપ & ડેકોરેટર્સ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_signup_biz")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it },
                    label = { Text("રેફરલ કોડ (જો હોય તો - વૈકલ્પિક)") },
                    leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_signup_refcode")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "કૃપા કરીને ઈમેલ અને પાસવર્ડ દાખલ કરો."
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = "પાસવર્ડ ઓછામાં ઓછા 6 અક્ષરનો હોવો જોઈએ."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        focusManager.clearFocus()

                        val safeName = name.ifBlank { "Mandap Partner" }
                        val safeBiz = businessName.ifBlank { "Shree Mandap & Decorators" }

                        viewModel.signUp(
                            name = safeName,
                            businessName = safeBiz,
                            email = email.trim(),
                            password = password.trim(),
                            mobile = mobile.trim(),
                            referralCode = referralCode.ifBlank { null }
                        ) { result ->
                            isLoading = false
                            result.onSuccess {
                                Toast.makeText(context, "એકાઉન્ટ સફળતાપૂર્વક બન્યું! 90 દિવસ ફ્રી ટ્રાયલ શરૂ થઈ.", Toast.LENGTH_LONG).show()
                                onAuthSuccess()
                            }.onFailure { err ->
                                errorMessage = err.message ?: "નોંધણી નિષ્ફળ રહી. કૃપા કરીને ફરી પ્રયાસ કરો."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_signup_submit"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "90 દિવસ ફ્રી ટ્રાયલ સાથે એકાઉન્ટ બનાવો",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

            } else {
                // SIGN IN FORM
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ઈમેલ (Email)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_signin_email")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("પાસવર્ડ (Password)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_signin_password")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "કૃપા કરીને ઈમેલ અને પાસવર્ડ દાખલ કરો."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        focusManager.clearFocus()

                        viewModel.signIn(email, password) { result ->
                            isLoading = false
                            result.onSuccess {
                                Toast.makeText(context, "સ્વાગત છે!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            }.onFailure { err ->
                                errorMessage = err.message ?: "લોગિન નિષ્ફળ રહ્યું. ઈમેલ અને પાસવર્ડ તપાસો."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_signin_submit"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "લોગિન કરો (Sign In)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer note
            Text(
                text = "Google Firebase & Google Play Security દ્વારા સુરક્ષિત\nબધી ડેટા ક્લાઉડ ડેટાબેઝમાં સાચવવામાં આવે છે",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
