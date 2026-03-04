package com.idk.anypay.ui.screens

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Data Model ───────────────────────────────────────────────────────────────

data class PhoneContact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: android.net.Uri? = null
)

// ─── Contact Loader ───────────────────────────────────────────────────────────

private suspend fun loadContacts(contentResolver: ContentResolver): List<PhoneContact> =
    withContext(Dispatchers.IO) {
        val contacts = mutableListOf<PhoneContact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use { c ->
            val idIdx    = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx  = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx   = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val seen     = mutableSetOf<Long>()
            while (c.moveToNext()) {
                val id     = c.getLong(idIdx)
                val name   = c.getString(nameIdx) ?: continue
                val number = c.getString(numIdx)   ?: continue
                val photo  = c.getString(photoIdx)?.let { android.net.Uri.parse(it) }
                if (seen.add(id)) contacts.add(PhoneContact(id, name, number, photo))
            }
        }
        contacts
    }

// ─── Load contact thumbnail bitmap ────────────────────────────────────────────

@Composable
private fun rememberContactPhoto(contactId: Long): ImageBitmap? {
    val context = LocalContext.current
    return remember(contactId) {
        try {
            val contactUri = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI, contactId
            )
            val stream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver, contactUri, false
            )
            stream?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } catch (_: Exception) { null }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayToContactScreen(
    hasContactsPermission: Boolean,
    onRequestContactsPermission: () -> Unit,
    onContactSelected: (phoneNumber: String, name: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val permissionGranted = remember(hasContactsPermission) {
        hasContactsPermission || ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    var searchQuery by remember { mutableStateOf("") }
    var contacts    by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            isLoading = true
            contacts  = loadContacts(context.contentResolver)
            isLoading = false
        }
    }

    val filtered = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pay to Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!permissionGranted) {
                // ── Ask for permission ──────────────────────────────────
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text("Contacts Permission Required",
                            style = MaterialTheme.typography.titleMedium)
                        Text("AnyPay needs access to your contacts so you can find and pay people quickly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Button(
                            onClick = onRequestContactsPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Contacts, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Grant Contacts Permission")
                        }
                    }
                }
            } else {
                // ── Search bar ─────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("Search by name or number…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = MaterialTheme.shapes.large
                )

                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator()
                                Text("Loading contacts…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    filtered.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text(
                                    if (searchQuery.isBlank()) "No contacts found"
                                    else "No contacts match \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> {
                        Text("${filtered.size} contact${if (filtered.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.id }) { contact ->
                                ContactRow(contact = contact, onClick = {
                                    // Extract last 10 digits for the phone number
                                    val digits = contact.phoneNumber.filter { it.isDigit() }.takeLast(10)
                                    onContactSelected(digits, contact.name)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Contact Row ──────────────────────────────────────────────────────────────

@Composable
private fun ContactRow(contact: PhoneContact, onClick: () -> Unit) {
    val avatarColors = listOf(
        Color(0xFF6366F1), Color(0xFF22C55E), Color(0xFFEF4444),
        Color(0xFFF59E0B), Color(0xFF3B82F6), Color(0xFFEC4899),
        Color(0xFF14B8A6), Color(0xFFA855F7)
    )
    val avatarColor = remember(contact.id) {
        avatarColors[(contact.id.toInt() and 0x7FFFFFFF) % avatarColors.size]
    }
    val initial = contact.name.firstOrNull()?.uppercaseChar() ?: '?'
    val photo   = rememberContactPhoto(contact.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar — contact photo or initial
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (photo == null) avatarColor.copy(alpha = 0.15f) else Color.Transparent)
                .then(
                    if (photo == null) Modifier.border(1.dp, avatarColor.copy(alpha = 0.3f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = contact.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    initial.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = avatarColor
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface)
            Text(contact.phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Pay",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

