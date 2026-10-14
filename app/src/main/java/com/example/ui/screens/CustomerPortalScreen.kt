package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BusinessProfile
import com.example.data.model.GalleryItem
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.RoyalMaroonPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MandapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalScreen(
    customerId: Long,
    customerName: String,
    customerMobile: String = "",
    viewModel: MandapViewModel,
    onBack: () -> Unit,
    onNavigateToBillDraft: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val business by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val galleryItems by viewModel.galleryItems.collectAsStateWithLifecycle()
    val selections by viewModel.portalSelection.collectAsStateWithLifecycle()

    val currentBiz = business ?: BusinessProfile()
    var submitted by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var previewItem by remember { mutableStateOf<GalleryItem?>(null) }
    var inputCustomerName by remember { mutableStateOf(customerName.ifBlank { "Customer" }) }
    var inputCustomerMobile by remember { mutableStateOf(customerMobile) }
    var manualQtyItem by remember { mutableStateOf<GalleryItem?>(null) }
    var manualQtyText by remember { mutableStateOf("1") }
    var showCustomItemDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Mandap", "Stage", "Flower", "Sofa", "Chair", "Lighting", "Gate", "Other")

    val filteredItems = galleryItems.filter { item ->
        selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
    }

    val totalItemsCount = selections.values.sum()
    val estimatedTotal = selections.entries.sumOf { (itemId, qty) ->
        val price = galleryItems.find { it.id == itemId }?.price ?: 0.0
        price * qty
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentBiz.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Customer Selection Portal",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RoyalMaroonPrimary
                )
            )
        },
        bottomBar = {
            if (!submitted && totalItemsCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$totalItemsCount Items Selected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Est. Total: ₹%,.0f".format(estimatedTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.submitCustomerSelection(
                                    customerName = inputCustomerName.ifBlank { "Customer" },
                                    customerId = customerId,
                                    customerMobile = inputCustomerMobile,
                                    onComplete = {
                                        submitted = true
                                        Toast.makeText(context, "Selection submitted successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("submit_selection_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Selection")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (submitted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = SuccessGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Selection Submitted!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thank you, $customerName! Your selected decoration choices have been sent directly to ${currentBiz.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The business owner has received a live notification and will finalize your booking quote.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            onNavigateToBillDraft?.invoke() ?: onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalMaroonPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("create_bill_from_portal_button")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📄 આ સિલેક્શનનું બિલ બનાવો (Create Bill)", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("પૂર્ણ (Done / Return to App)")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
            ) {
                // Greeting Card & Customer Contact Details
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Namaste, ${inputCustomerName.ifBlank { "Customer" }} ji! 🙏",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Browse our latest decoration inventory (${galleryItems.size} designs available). Tap any photo to zoom, and use + / - to pick your favorites.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = inputCustomerName,
                                    onValueChange = { inputCustomerName = it },
                                    label = { Text("તમારું નામ (Name)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                androidx.compose.material3.OutlinedTextField(
                                    value = inputCustomerMobile,
                                    onValueChange = { inputCustomerMobile = it },
                                    label = { Text("મોબાઈલ નંબર (Phone)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Category Filter Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Add Custom Requirement / Item Manually Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustomItemDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = AmberGoldSecondary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = AmberGoldSecondary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "+ ખાસ આઇટમ / ક્વોન્ટિટી જાતે ઉમેરો",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "કેટલોગ સિવાયની વસ્તુ અથવા ચોક્કસ જથ્થો લખો",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showCustomItemDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ઉમેરો", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Items list
                items(filteredItems, key = { it.id }) { item ->
                    val selectedQty = selections[item.id] ?: 0

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (selectedQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Photo Thumbnail with Click-to-Zoom
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { previewItem = item }
                            ) {
                                if (item.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = R.drawable.mandap_decor_hero),
                                        placeholder = painterResource(id = R.drawable.mandap_decor_hero)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.mandap_decor_hero),
                                        contentDescription = item.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        "Zoom",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Item Info
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { previewItem = item }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RoyalMaroonPrimary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = item.category,
                                        color = RoyalMaroonPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (item.price > 0) "₹%,.0f / event".format(item.price) else "Price on Request",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmberGoldSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.description.isNotBlank()) {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Quantity Stepper (+ / - & Manual Quantity Input)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (selectedQty > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updatePortalItemQty(item.id, -1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Clickable quantity chip that opens Manual Quantity Dialog
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .clickable {
                                                manualQtyItem = item
                                                manualQtyText = selectedQty.toString()
                                            }
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "$selectedQty",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit quantity manually",
                                                modifier = Modifier.size(11.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updatePortalItemQty(item.id, 1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    // Not selected yet: provide quick manual quantity input or 1-tap add
                                    OutlinedButton(
                                        onClick = {
                                            manualQtyItem = item
                                            manualQtyText = "1"
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("+ Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updatePortalItemQty(item.id, 1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Zoom Dialog for Customer
    previewItem?.let { item ->
        val currentQty = selections[item.id] ?: 0
        Dialog(
            onDismissRequest = { previewItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { previewItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RoyalMaroonPrimary
                        ) {
                            Text(
                                text = item.category,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                                error = painterResource(id = R.drawable.mandap_decor_hero),
                                placeholder = painterResource(id = R.drawable.mandap_decor_hero)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.mandap_decor_hero),
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1F2937),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (item.price > 0) "₹%,.0f / event".format(item.price) else "Price on Request",
                                        color = Color(0xFFFDE68A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            manualQtyItem = item
                                            manualQtyText = (if (currentQty > 0) currentQty else 1).toString()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Set Qty", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.updatePortalItemQty(item.id, 1)
                                            Toast.makeText(context, "Added to selection!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberGoldSecondary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (currentQty > 0) "+1 ($currentQty)" else "Select", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (item.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.description,
                                    color = Color(0xFFD1D5DB),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Quantity Dialog for exact number input
    manualQtyItem?.let { item ->
        AlertDialog(
            onDismissRequest = {
                focusManager.clearFocus()
                manualQtyItem = null
            },
            icon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text(
                    text = "ક્વોન્ટિટી દાખલ કરો (Set Quantity)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${item.category} • ${if (item.price > 0) "₹%,.0f / event".format(item.price) else "Price on Request"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualQtyText,
                        onValueChange = { manualQtyText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("જથ્થો / નંગ (Quantity) *") },
                        placeholder = { Text("e.g. 50, 100, 200") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Preset Quantity Chips
                    Text(
                        text = "ઝડપી પસંદગી (Quick Select):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 25, 50, 100, 200).forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    manualQtyText = preset.toString()
                                }
                            ) {
                                Text(
                                    text = "$preset",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val qty = manualQtyText.toIntOrNull() ?: 0
                        viewModel.setPortalItemExactQty(item.id, qty)
                        manualQtyItem = null
                        Toast.makeText(context, "${item.title}: $qty નંગ સેટ થયા", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("સેવ કરો (Save)")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if ((selections[item.id] ?: 0) > 0) {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.setPortalItemExactQty(item.id, 0)
                                manualQtyItem = null
                                Toast.makeText(context, "${item.title} હટાવી દેવામાં આવી", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("હટાવો", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = {
                        focusManager.clearFocus()
                        manualQtyItem = null
                    }) {
                        Text("રદ કરો")
                    }
                }
            }
        )
    }

    // Custom Item Dialog
    if (showCustomItemDialog) {
        var customTitle by remember { mutableStateOf("") }
        var customCategory by remember { mutableStateOf("Other") }
        var customQtyText by remember { mutableStateOf("1") }
        var customPriceText by remember { mutableStateOf("") }

        val availCategories = listOf("Mandap", "Stage", "Flower", "Sofa", "Chair", "Lighting", "Gate", "Other")

        AlertDialog(
            onDismissRequest = {
                focusManager.clearFocus()
                showCustomItemDialog = false
            },
            icon = {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = RoyalMaroonPrimary)
            },
            title = {
                Text("ખાસ આઇટમ ઉમેરો (Add Custom Item)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("આઇટમનું નામ (Item Name) *") },
                        placeholder = { Text("e.g. VIP Chairs, 25KV Generator") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("કેટેગરી પસંદ કરો:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availCategories) { cat ->
                            val sel = cat == customCategory
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { customCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customQtyText,
                            onValueChange = { customQtyText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("જથ્થો (Qty) *") },
                            placeholder = { Text("50") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customPriceText,
                            onValueChange = { customPriceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("ભાવ ₹ (Optional)") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val qty = customQtyText.toIntOrNull() ?: 1
                        val price = customPriceText.toDoubleOrNull() ?: 0.0
                        viewModel.addCustomPortalItem(
                            title = customTitle,
                            category = customCategory,
                            quantity = qty,
                            estimatedPrice = price
                        )
                        showCustomItemDialog = false
                        Toast.makeText(context, "$customTitle ($qty નંગ) ઉમેરાઈ ગઈ!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = customTitle.isNotBlank()
                ) {
                    Text("ઉમેરો (Add Item)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showCustomItemDialog = false
                }) {
                    Text("રદ કરો")
                }
            }
        )
    }
}
