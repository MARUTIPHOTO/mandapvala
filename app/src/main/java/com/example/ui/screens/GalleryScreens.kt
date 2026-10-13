package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.net.URLEncoder
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BusinessProfile
import com.example.data.model.Customer
import com.example.data.model.GalleryItem
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.RoyalMaroonPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MandapViewModel
import com.example.util.ImageStorageHelper
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val MANDAP_CATEGORIES = listOf("All", "Mandap", "Sofa", "Chair", "Flower", "Lighting", "Stage", "Gate", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MandapViewModel,
    onOpenCustomerPortal: (Long, String) -> Unit = { _, _ -> },
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val business by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val currentBiz = business ?: BusinessProfile()

    val items by viewModel.galleryItems.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.galleryCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.gallerySearchQuery.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<GalleryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<GalleryItem?>(null) }
    var previewItem by remember { mutableStateOf<GalleryItem?>(null) }

    // Multi-photo bulk upload state (up to 100 photos)
    var selectedBulkUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showBulkUploadDialog by remember { mutableStateOf(false) }

    // Customer Selection Link dialog state
    var showCustomerLinkDialog by remember { mutableStateOf(false) }

    // Multi-image picker launcher (up to 100 photos)
    val bulkPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedBulkUris = uris
            showBulkUploadDialog = true
        }
    }

    val filteredItems = items.filter { item ->
        val matchesCat = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesSearch
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gallery & Catalog", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${items.size} Photos in Gallery",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick Share Link icon
                    IconButton(
                        onClick = { showCustomerLinkDialog = true },
                        modifier = Modifier.testTag("gallery_share_link_button")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Selection Link",
                            tint = RoyalMaroonPrimary
                        )
                    }
                    // Quick Bulk Upload icon
                    IconButton(
                        onClick = {
                            bulkPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("gallery_bulk_upload_button")
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Photos",
                            tint = RoyalMaroonPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bulk Upload FAB
                FloatingActionButton(
                    onClick = {
                        bulkPhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = AmberGoldSecondary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("bulk_upload_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Upload Photos")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload (1-100)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Add Single Item FAB
                FloatingActionButton(
                    onClick = {
                        itemToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_gallery_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Prominent Action Buttons Bar: Upload 100 Photos & Send Customer Link
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            bulkPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_upload_photos_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalMaroonPrimary)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Photos", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showCustomerLinkDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_send_link_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customer Link", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Visible Brand Link & Quick Share Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onOpenCustomerPortal(0L, "Customer") },
                color = AmberGoldSecondary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AmberGoldSecondary.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Selection Link",
                            tint = RoyalMaroonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "📱 Customer Selection Portal (ઇન-એપ ફોર્મ)",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ટચ કરીને ગ્રાહક માટે કેટલોગ ખોલો અને ઓર્ડર લો",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RoyalMaroonPrimary,
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(
                        onClick = { onOpenCustomerPortal(0L, "Customer") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open In App", tint = RoyalMaroonPrimary)
                    }
                    IconButton(
                        onClick = { showCustomerLinkDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = RoyalMaroonPrimary)
                    }
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setGallerySearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("gallery_search_input"),
                placeholder = { Text("Search Mandaps, Sofas, Lighting...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setGallerySearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Horizontal Filter Bar
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MANDAP_CATEGORIES) { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { viewModel.setGalleryCategoryFilter(cat) }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of Items
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Collections,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No gallery photos found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap 'Upload Photos' to select up to 100 photos from your phone gallery at once!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                bulkPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Photos Now (Up to 100)")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        GalleryItemCard(
                            item = item,
                            onClick = { previewItem = item },
                            onEdit = {
                                itemToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Bulk Upload Photos Dialog (Up to 100 photos)
    if (showBulkUploadDialog && selectedBulkUris.isNotEmpty()) {
        BulkUploadPhotosDialog(
            uris = selectedBulkUris,
            onDismiss = {
                showBulkUploadDialog = false
                selectedBulkUris = emptyList()
            },
            onUploadComplete = { newItems ->
                scope.launch {
                    viewModel.saveGalleryItems(newItems)
                    showBulkUploadDialog = false
                    selectedBulkUris = emptyList()
                    Toast.makeText(context, "${newItems.size} photos added to Gallery!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Add / Edit Single Dialog (with photo selection)
    if (showAddEditDialog) {
        GalleryItemAddEditDialog(
            item = itemToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { newItem ->
                viewModel.saveGalleryItem(newItem)
                showAddEditDialog = false
            }
        )
    }

    // Full Screen Photo Zoom / Preview Dialog
    previewItem?.let { item ->
        FullScreenPhotoDialog(
            item = item,
            onDismiss = { previewItem = null },
            onEdit = {
                previewItem = null
                itemToEdit = item
                showAddEditDialog = true
            }
        )
    }

    // Delete Confirmation
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Gallery Item?") },
            text = { Text("Are you sure you want to remove '${item.title}' from inventory?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGalleryItem(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Customer Selection Link Dialog
    if (showCustomerLinkDialog) {
        CustomerLinkShareDialog(
            customers = customers,
            business = currentBiz,
            onDismiss = { showCustomerLinkDialog = false },
            onOpenPortal = { custId, custName ->
                showCustomerLinkDialog = false
                onOpenCustomerPortal(custId, custName)
            }
        )
    }
}

@Composable
fun GalleryItemCard(
    item: GalleryItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Visual Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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

                // Category Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RoyalMaroonPrimary.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.category,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Available Qty badge
                if (item.availableQuantity > 1) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Qty: ${item.availableQuantity}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Price Tag on bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (item.price > 0) "₹%,.0f".format(item.price) else "Price on Request",
                        color = Color(0xFFFDE68A),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info & Actions
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to view",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bulk Photo Upload Dialog:
 * Lets decorator select up to 100 photos, choose a category, base name, and default price.
 * Copies photos to local storage and creates gallery items in batch.
 */
@Composable
fun BulkUploadPhotosDialog(
    uris: List<Uri>,
    onDismiss: () -> Unit,
    onUploadComplete: (List<GalleryItem>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var category by remember { mutableStateOf("Mandap") }
    var baseTitle by remember { mutableStateOf("Mandap Setup") }
    var defaultPriceText by remember { mutableStateOf("15000") }
    var defaultQtyText by remember { mutableStateOf("1") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    val categories = listOf("Mandap", "Stage", "Flower", "Sofa", "Chair", "Lighting", "Gate", "Other")

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = RoyalMaroonPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload ${uris.size} Photos", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${uris.size} photos selected from your device. Configure details for the batch:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Horizontal preview of selected photos
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uris.take(15)) { uri ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    if (uris.size > 15) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+${uris.size - 15}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Category selector
                Text("Category *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val selected = category == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                category = cat
                                if (baseTitle == "Mandap Setup" || baseTitle.endsWith("Setup") || baseTitle.endsWith("Design")) {
                                    baseTitle = "$cat Design"
                                }
                            }
                        ) {
                            Text(
                                text = cat,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Base Title
                OutlinedTextField(
                    value = baseTitle,
                    onValueChange = { baseTitle = it },
                    label = { Text("Base Title *") },
                    placeholder = { Text("e.g. Royal Stage Design") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = defaultPriceText,
                        onValueChange = { defaultPriceText = it },
                        label = { Text("Rent Price (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultQtyText,
                        onValueChange = { defaultQtyText = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isUploading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Saving photos to gallery... ${(uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isUploading) {
                        isUploading = true
                        scope.launch {
                            val savedItems = withContext(Dispatchers.IO) {
                                val price = defaultPriceText.toDoubleOrNull() ?: 0.0
                                val qty = defaultQtyText.toIntOrNull() ?: 1
                                val resultList = mutableListOf<GalleryItem>()

                                uris.forEachIndexed { index, uri ->
                                    val localPath = ImageStorageHelper.saveUriToInternalStorage(context, uri)
                                    val itemTitle = if (uris.size > 1) {
                                        "$baseTitle ${index + 1}"
                                    } else {
                                        baseTitle
                                    }
                                    resultList.add(
                                        GalleryItem(
                                            title = itemTitle,
                                            category = category,
                                            price = price,
                                            availableQuantity = qty,
                                            imageUrl = localPath,
                                            description = "$category setup for events & celebrations"
                                        )
                                    )
                                    uploadProgress = (index + 1).toFloat() / uris.size
                                }
                                resultList
                            }
                            onUploadComplete(savedItems)
                        }
                    }
                },
                enabled = !isUploading && baseTitle.isNotBlank()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Upload All (${uris.size})")
                }
            }
        },
        dismissButton = {
            if (!isUploading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Add / Edit Single Item Dialog with Photo Picker
 */
@Composable
fun GalleryItemAddEditDialog(
    item: GalleryItem?,
    onDismiss: () -> Unit,
    onSave: (GalleryItem) -> Unit
) {
    val context = LocalContext.current
    val isEdit = item != null
    var title by remember { mutableStateOf(item?.title ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Mandap") }
    var priceText by remember { mutableStateOf(item?.price?.let { "%.0f".format(it) } ?: "") }
    var quantityText by remember { mutableStateOf(item?.availableQuantity?.toString() ?: "1") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var imageUriPath by remember { mutableStateOf(item?.imageUrl ?: "") }

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = ImageStorageHelper.saveUriToInternalStorage(context, uri)
            imageUriPath = localPath
        }
    }

    val categories = listOf("Mandap", "Sofa", "Chair", "Flower", "Lighting", "Stage", "Gate", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Inventory Item" else "Add Decor Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo Picker Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clickable {
                            singlePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUriPath.isNotBlank()) {
                        AsyncImage(
                            model = imageUriPath,
                            contentDescription = "Selected Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                "Change Photo",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap to select item photo",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g. Royal Wedding Mandap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val selected = category == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Rent Price (₹) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Specifications") },
                    placeholder = { Text("Dimensions, color, flower type...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val qty = quantityText.toIntOrNull() ?: 1
                    if (title.isNotBlank()) {
                        onSave(
                            GalleryItem(
                                id = item?.id ?: 0,
                                businessId = item?.businessId ?: "biz_default",
                                title = title.trim(),
                                category = category,
                                price = price,
                                availableQuantity = qty,
                                imageUrl = imageUriPath,
                                description = description.trim(),
                                isAvailable = true
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEdit) "Update" else "Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Full Screen High-Resolution Photo Viewer
 */
@Composable
fun FullScreenPhotoDialog(
    item: GalleryItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
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
                // Top controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // High-resolution photo
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

                // Metadata Card
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
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = RoyalMaroonPrimary
                            ) {
                                Text(
                                    text = item.category,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = if (item.price > 0) "₹%,.0f / event".format(item.price) else "Price on Request",
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
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

/**
 * Customer Link Share Dialog:
 * Lets decorator select an existing customer (e.g. Hardik Shah) from added customers list
 * and with 1 click directly send the catalog link to their WhatsApp,
 * or enter a new customer name & number, copy link, or preview in app.
 */
@Composable
fun CustomerLinkShareDialog(
    customers: List<Customer>,
    business: BusinessProfile,
    onDismiss: () -> Unit,
    onOpenPortal: (Long, String) -> Unit
) {
    val context = LocalContext.current
    var selectedCustomer by remember { mutableStateOf<Customer?>(customers.firstOrNull()) }
    var customerSearch by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var isNewCustomerMode by remember { mutableStateOf(customers.isEmpty()) }

    val filteredCustomers = remember(customers, customerSearch) {
        if (customerSearch.isBlank()) customers
        else customers.filter {
            it.name.contains(customerSearch, ignoreCase = true) || it.mobile.contains(customerSearch)
        }
    }

    val customerName = if (isNewCustomerMode) manualName.ifBlank { "Valued Customer" } else (selectedCustomer?.name ?: "Valued Customer")
    val customerPhone = if (isNewCustomerMode) manualPhone else (selectedCustomer?.mobile ?: "")
    val customerId = if (isNewCustomerMode) 0L else (selectedCustomer?.id ?: 0L)
    val bizPhone = business.whatsapp.ifBlank { business.mobile }.replace("[^0-9]".toRegex(), "")

    val encCustName = try { URLEncoder.encode(customerName.trim(), "UTF-8") } catch (e: Exception) { customerName.trim() }
    val cleanCustPhone = customerPhone.trim()

    val baseCatalogUrl = "https://marutiphoto.github.io/mandapvala/"
    val catalogLink = "$baseCatalogUrl?phone=$bizPhone&biz=${business.id}&cust=$customerId&cname=$encCustName&cphone=$cleanCustPhone"

    fun sendToWhatsApp(name: String, phone: String, id: Long) {
        val encName = try { URLEncoder.encode(name.trim(), "UTF-8") } catch (e: Exception) { name.trim() }
        val cleanPhone = phone.trim()
        val customLink = "$baseCatalogUrl?phone=$bizPhone&biz=${business.id}&cust=$id&cname=$encName&cphone=$cleanPhone"
        WhatsAppHelper.shareCustomerGalleryLink(
            context = context,
            customerName = name,
            customerMobile = cleanPhone,
            business = business,
            customerId = id,
            customUrl = customLink
        )
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = RoyalMaroonPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Send Selection Link", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("કસ્ટમર સિલેક્શન કેટલોગ લિંક", style = MaterialTheme.typography.labelSmall, color = RoyalMaroonPrimary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ગ્રાહકને કેટલોગ લિંક મોકલો જેથી તેઓ પોતાના ફોન પર મંડપ, સ્ટેજ, સોફાની ડિઝાઇન અને ભાડું જોઈને પસંદગી કરી શકે:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Toggle tabs: Added Customers vs New Customer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isNewCustomerMode) RoyalMaroonPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isNewCustomerMode = false }
                    ) {
                        Text(
                            "સેવ કરેલા ગ્રાહક (${customers.size})",
                            color = if (!isNewCustomerMode) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isNewCustomerMode) RoyalMaroonPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isNewCustomerMode = true }
                    ) {
                        Text(
                            "+ નવો નંબર / ગ્રાહક",
                            color = if (isNewCustomerMode) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                if (!isNewCustomerMode) {
                    // Search bar for customers
                    if (customers.size > 3) {
                        OutlinedTextField(
                            value = customerSearch,
                            onValueChange = { customerSearch = it },
                            placeholder = { Text("ગ્રાહક શોધો (Search customer...)") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text("ગ્રાહક પસંદ કરો અને 1-ક્લિકમાં WhatsApp પર મોકલો:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    if (filteredCustomers.isEmpty()) {
                        Text("કોઈ ગ્રાહક મળ્યા નથી.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        // Vertical scrollable list of customers
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((filteredCustomers.size * 64).coerceIn(64, 200).dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredCustomers, key = { it.id }) { cust ->
                                val isSelected = selectedCustomer?.id == cust.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) BorderStroke(1.5.dp, RoyalMaroonPrimary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCustomer = cust }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(RoyalMaroonPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = cust.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = cust.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = cust.mobile,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }

                                        // Direct 1-Click WhatsApp Button on customer row
                                        Button(
                                            onClick = {
                                                sendToWhatsApp(cust.name, cust.mobile, cust.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send WhatsApp",
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Customer Name (ગ્રાહકનું નામ)") },
                        placeholder = { Text("e.g. Hardik Shah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualPhone,
                        onValueChange = { manualPhone = it },
                        label = { Text("WhatsApp Number (મોબાઇલ નંબર)") },
                        placeholder = { Text("10 digit mobile number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (manualPhone.trim().length >= 10) {
                                sendToWhatsApp(manualName.ifBlank { "Customer" }, manualPhone, 0L)
                            } else {
                                Toast.makeText(context, "કૃપા કરીને ૧૦ અંકનો સાચો મોબાઈલ નંબર દાખલ કરો", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send to WhatsApp (વોટ્સએપ પર મોકલો)", fontWeight = FontWeight.Bold)
                    }
                }

                // Link Preview Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ગ્રાહક માટે કેટલોગ લિંક (Fixed Portal):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = catalogLink,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        onDismiss()
                        onOpenPortal(customerId, customerName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📱 એપમાં કસ્ટમર પોર્ટલ ખોલો (In-App)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy Link Button
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Mandap Selection Link", catalogLink))
                            Toast.makeText(context, "Catalog link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Link", fontSize = 12.sp)
                    }

                    // Preview Customer View in app
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenPortal(customerId, customerName)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    sendToWhatsApp(customerName, customerPhone, customerId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send on WhatsApp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
