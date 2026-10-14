package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.data.model.Bill
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.GalleryItem
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.RoyalMaroonPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.MandapViewModel
import com.example.util.JsonHelper
import com.example.util.PdfInvoiceGenerator
import com.example.util.PrintHelper
import com.example.util.WhatsAppHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEditScreen(
    viewModel: MandapViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val draft by viewModel.draftBill.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val galleryItems by viewModel.galleryItems.collectAsStateWithLifecycle()
    val business by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val currentBiz = business ?: BusinessProfile()

    var showCustomerPicker by remember { mutableStateOf(false) }
    var showGalleryPicker by remember { mutableStateOf(false) }
    var manualQtyIndex by remember { mutableStateOf<Int?>(null) }
    var manualQtyDraftText by remember { mutableStateOf("1") }

    var discountInput by remember(draft.discount) {
        mutableStateOf(if (draft.discount > 0) "%.0f".format(draft.discount) else "")
    }
    var advanceInput by remember(draft.advance) {
        mutableStateOf(if (draft.advance > 0) "%.0f".format(draft.advance) else "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (draft.id == 0L) "Create New Bill" else "Edit Bill #${draft.billNumber}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = draft.billNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveDraftBill { savedBill ->
                                val pdf = PdfInvoiceGenerator.generateInvoicePdf(
                                    context, savedBill, currentBiz, draft.items
                                )
                                PrintHelper.openPdfViewer(context, pdf)
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Preview PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveDraftBill { savedBill ->
                                val pdf = PdfInvoiceGenerator.generateInvoicePdf(
                                    context, savedBill, currentBiz, draft.items
                                )
                                WhatsAppHelper.shareBillViaWhatsApp(context, savedBill, currentBiz, pdf)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = WhatsAppGreen)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹%,.0f".format(draft.grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.saveDraftBill {
                                Toast.makeText(context, "Bill #${it.billNumber} saved successfully!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalMaroonPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_bill_submit_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Bill")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // Customer & Event Info Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Customer & Event Particulars",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showCustomerPicker = true }) {
                                Text("Pick Customer", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedTextField(
                            value = draft.customerName,
                            onValueChange = {
                                viewModel.updateDraftManualDetails(
                                    customerName = it,
                                    customerMobile = draft.customerMobile,
                                    customerAddress = draft.customerAddress,
                                    eventDate = draft.eventDate,
                                    eventType = draft.eventType,
                                    notes = draft.notes
                                )
                            },
                            label = { Text("Customer Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.customerMobile,
                                onValueChange = {
                                    viewModel.updateDraftManualDetails(
                                        customerName = draft.customerName,
                                        customerMobile = it,
                                        customerAddress = draft.customerAddress,
                                        eventDate = draft.eventDate,
                                        eventType = draft.eventType,
                                        notes = draft.notes
                                    )
                                },
                                label = { Text("Mobile *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = draft.eventDate,
                                onValueChange = {
                                    viewModel.updateDraftManualDetails(
                                        customerName = draft.customerName,
                                        customerMobile = draft.customerMobile,
                                        customerAddress = draft.customerAddress,
                                        eventDate = it,
                                        eventType = draft.eventType,
                                        notes = draft.notes
                                    )
                                },
                                label = { Text("Event Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.eventType,
                                onValueChange = {
                                    viewModel.updateDraftManualDetails(
                                        customerName = draft.customerName,
                                        customerMobile = draft.customerMobile,
                                        customerAddress = draft.customerAddress,
                                        eventDate = draft.eventDate,
                                        eventType = it,
                                        notes = draft.notes
                                    )
                                },
                                label = { Text("Event Type (e.g. Wedding)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = draft.customerAddress,
                                onValueChange = {
                                    viewModel.updateDraftManualDetails(
                                        customerName = draft.customerName,
                                        customerMobile = draft.customerMobile,
                                        customerAddress = it,
                                        eventDate = draft.eventDate,
                                        eventType = draft.eventType,
                                        notes = draft.notes
                                    )
                                },
                                label = { Text("Venue / Address") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Items in Bill Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Decoration Items (${draft.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showGalleryPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_items_from_gallery_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add From Gallery")
                    }
                }
            }

            if (draft.items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No items in this bill yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Tap 'Add From Gallery' to add Mandaps, Sofas, or Stages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(draft.items) { index, item ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.mandap_decor_hero),
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "₹%,.0f each • Total: ₹%,.0f".format(item.price, item.lineTotal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Stepper (+ / -)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateDraftItemQuantity(index, item.quantity - 1) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .clickable {
                                            manualQtyIndex = index
                                            manualQtyDraftText = item.quantity.toString()
                                        }
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${item.quantity}",
                                            style = MaterialTheme.typography.bodyMedium,
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
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateDraftItemQuantity(index, item.quantity + 1) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.removeDraftItem(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Calculation Breakdown Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Auto Price & Tax Calculations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                            Text("₹%,.0f".format(draft.subtotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        // Discount Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Discount (₹):", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = discountInput,
                                onValueChange = {
                                    discountInput = it
                                    val disc = it.toDoubleOrNull() ?: 0.0
                                    viewModel.setDraftDiscount(disc)
                                },
                                modifier = Modifier.width(130.dp),
                                singleLine = true,
                                placeholder = { Text("0") }
                            )
                        }

                        // GST 18% Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("GST (18%):", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (draft.gstEnabled) "+ ₹%,.0f".format(draft.gstAmount) else "Tax Excluded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (draft.gstEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = draft.gstEnabled,
                                onCheckedChange = { viewModel.setDraftGstEnabled(it) }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("₹%,.0f".format(draft.grandTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RoyalMaroonPrimary)
                        }

                        // Advance Received Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Advance Received (₹):", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = advanceInput,
                                onValueChange = {
                                    advanceInput = it
                                    val adv = it.toDoubleOrNull() ?: 0.0
                                    viewModel.setDraftAdvance(adv)
                                },
                                modifier = Modifier.width(130.dp),
                                singleLine = true,
                                placeholder = { Text("0") }
                            )
                        }

                        // Balance Due Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Balance Due:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("₹%,.0f".format(draft.balance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }

                        // Notes field
                        OutlinedTextField(
                            value = draft.notes,
                            onValueChange = {
                                viewModel.updateDraftManualDetails(
                                    customerName = draft.customerName,
                                    customerMobile = draft.customerMobile,
                                    customerAddress = draft.customerAddress,
                                    eventDate = draft.eventDate,
                                    eventType = draft.eventType,
                                    notes = it
                                )
                            },
                            label = { Text("Terms & Notes (appears on invoice)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Quick Print & PDF Action Buttons Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveDraftBill { savedBill ->
                                val pdf = PdfInvoiceGenerator.generateInvoicePdf(
                                    context, savedBill, currentBiz, draft.items
                                )
                                PrintHelper.printPdf(context, pdf, "Invoice_${savedBill.billNumber}")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print Bill")
                    }
                    Button(
                        onClick = {
                            viewModel.saveDraftBill { savedBill ->
                                val pdf = PdfInvoiceGenerator.generateInvoicePdf(
                                    context, savedBill, currentBiz, draft.items
                                )
                                WhatsAppHelper.shareBillViaWhatsApp(context, savedBill, currentBiz, pdf)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp")
                    }
                }
            }
        }
    }

    // Customer Selector Dialog
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Select Customer", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(customers) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateDraftCustomer(cust)
                                    showCustomerPicker = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(cust.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${cust.mobile} • ${cust.eventType} on ${cust.eventDate}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomerPicker = false }) { Text("Close") }
            }
        )
    }

    // Gallery Item Picker Dialog
    if (showGalleryPicker) {
        AlertDialog(
            onDismissRequest = { showGalleryPicker = false },
            title = { Text("Add Decoration Items", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(galleryItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addItemToDraft(item)
                                    Toast.makeText(context, "Added ${item.title}", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${item.category} • ₹%,.0f".format(item.price), style = MaterialTheme.typography.bodySmall, color = AmberGoldSecondary, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showGalleryPicker = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Manual Quantity Edit Dialog for Bill Draft
    manualQtyIndex?.let { index ->
        val item = draft.items.getOrNull(index)
        if (item != null) {
            AlertDialog(
                onDismissRequest = { manualQtyIndex = null },
                icon = {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                title = {
                    Text("ક્વોન્ટિટી દાખલ કરો (Set Quantity)", fontWeight = FontWeight.Bold)
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
                            text = "${item.category} • ₹%,.0f / નંગ".format(item.price),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = manualQtyDraftText,
                            onValueChange = { manualQtyDraftText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("જથ્થો / નંગ (Quantity) *") },
                            placeholder = { Text("e.g. 50, 100") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Select Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 10, 25, 50, 100, 200).forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        manualQtyDraftText = preset.toString()
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
                            val newQty = manualQtyDraftText.toIntOrNull() ?: 1
                            viewModel.updateDraftItemQuantity(index, newQty)
                            manualQtyIndex = null
                            Toast.makeText(context, "${item.title}: $newQty નંગ સેટ થયા", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("સેવ કરો (Save)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { manualQtyIndex = null }) {
                        Text("રદ કરો")
                    }
                }
            )
        }
    }
}
