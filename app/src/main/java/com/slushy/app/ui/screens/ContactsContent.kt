package com.slushy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

data class Contact(
    val id: String,
    val name: String,
    val username: String,
    val accountId: String,
)

private val avatarColors = listOf(
    Color(0xFF4A90D9),
    Color(0xFF34D399),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
)

@Composable
fun ContactsContent(
    pendingRequests: List<Contact> = emptyList(),
    showRequestsDialog: Boolean = false,
    onDismissRequests: () -> Unit = {},
    onAcceptRequest: (Contact) -> Unit = {},
    onRejectRequest: (Contact) -> Unit = {},
    onAddRequest: (Contact) -> Unit = {},
) {
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    val contacts = remember { sampleContacts() }

    val showBlur = selectedContact != null || showAddContactDialog || showRequestsDialog

    Box(modifier = Modifier.fillMaxSize()) {
        ContactList(
            contacts = contacts,
            onContactClick = { selectedContact = it },
            modifier = if (showBlur) Modifier.blur(6.dp) else Modifier,
        )

        FloatingActionButton(
            onClick = { showAddContactDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add contact")
        }

        if (selectedContact != null) {
            ContactCardDialog(
                contact = selectedContact!!,
                onDismiss = { selectedContact = null },
                onMessage = { /* TODO */ },
                onCall = { /* TODO */ },
                onVideoCall = { /* TODO */ },
                onDelete = { selectedContact = null },
            )
        }

        if (showAddContactDialog) {
            AddContactDialog(
                onDismiss = { showAddContactDialog = false },
                onSend = { input ->
                    val isAccountId = input.startsWith("SL", ignoreCase = true)
                    val request = if (isAccountId) {
                        Contact(
                            id = "r${System.currentTimeMillis()}",
                            name = "",
                            username = "requesting_user",
                            accountId = input.uppercase(),
                        )
                    } else {
                        Contact(
                            id = "r${System.currentTimeMillis()}",
                            name = "",
                            username = input,
                            accountId = "SL9999999999",
                        )
                    }
                    onAddRequest(request)
                    showAddContactDialog = false
                },
            )
        }

        if (showRequestsDialog) {
            RequestsDialog(
                requests = pendingRequests,
                onDismiss = onDismissRequests,
                onAccept = onAcceptRequest,
                onReject = onRejectRequest,
            )
        }
    }
}

@Composable
private fun ContactList(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(contacts) {
        contacts.groupBy {
            val c = if (it.name.isNotBlank()) it.name.first() else it.username.first()
            c.uppercaseChar()
        }.toSortedMap()
    }
    val letters = remember { ('A'..'Z').toList() }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val letterIndexMap = remember(grouped) {
        val map = mutableMapOf<Char, Int>()
        var index = 0
        for (letter in letters) {
            val group = grouped[letter]
            if (group != null) {
                map[letter] = index
                index += 1 + group.size
            }
        }
        map
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            for ((letter, group) in grouped) {
                item {
                    LetterHeader(letter)
                }
                items(group, key = { it.id }) { contact ->
                    ContactRow(contact = contact, onClick = { onContactClick(contact) })
                }
            }
        }

        AlphabetScrollbar(
            letters = letters,
            letterIndexMap = letterIndexMap,
            lazyListState = lazyListState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(24.dp)
                .padding(end = 2.dp),
        )
    }
}

@Composable
private fun LetterHeader(letter: Char) {
    Text(
        text = letter.toString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    val displayName = if (contact.name.isNotBlank()) contact.name else contact.username
    val displayLetter = displayName.first().uppercaseChar()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarColors[contact.accountId.hashCode().mod(avatarColors.size)]),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayLetter.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (contact.name.isNotBlank()) {
                Text(
                    text = "@${contact.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AlphabetScrollbar(
    letters: List<Char>,
    letterIndexMap: Map<Char, Int>,
    lazyListState: LazyListState,
    modifier: Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val y = down.position.y
                        val height = size.height.toFloat()
                        val letterIndex = (y / height * letters.size).toInt()
                            .coerceIn(0, letters.size - 1)
                        val letter = letters[letterIndex]
                        letterIndexMap[letter]?.let { index ->
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(index)
                            }
                        }
                    }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                fontSize = 10.sp,
                color = if (letter in letterIndexMap)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ContactCardDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onCall: () -> Unit,
    onVideoCall: () -> Unit,
    onDelete: () -> Unit,
) {
    val displayName = if (contact.name.isNotBlank()) contact.name else "@${contact.username}"
    val displayLetter = displayName.first().uppercaseChar()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatarColors[contact.accountId.hashCode().mod(avatarColors.size)]),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayLetter.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                if (contact.name.isNotBlank()) {
                    Text(
                        text = "@${contact.username}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.size(20.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.size(12.dp))

                ContactInfoRow("Account ID", contact.accountId, Icons.AutoMirrored.Filled.Send)

                Spacer(modifier = Modifier.size(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ActionButton(icon = Icons.AutoMirrored.Filled.Send, label = "Message", onClick = onMessage)
                    ActionButton(icon = Icons.Default.Phone, label = "Call", onClick = onCall)
                    ActionButton(icon = Icons.Default.PlayArrow, label = "Video", onClick = onVideoCall)
                }

                Spacer(modifier = Modifier.size(20.dp))

                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Contact")
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Add Contact",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.size(20.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Username or Account ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.size(20.dp))

                Button(
                    onClick = { if (input.isNotBlank()) onSend(input.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = input.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Request")
                }
            }
        }
    }
}

@Composable
private fun RequestsDialog(
    requests: List<Contact>,
    onDismiss: () -> Unit,
    onAccept: (Contact) -> Unit,
    onReject: (Contact) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Contact Requests",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.size(16.dp))

                if (requests.isEmpty()) {
                    Text(
                        text = "No pending requests",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        requests.forEach { request ->
                            RequestItem(
                                request = request,
                                onAccept = { onAccept(request) },
                                onReject = { onReject(request) },
                            )
                            if (request != requests.last()) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun RequestItem(
    request: Contact,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val displayName = if (request.name.isNotBlank()) request.name else request.username
    val displayLetter = displayName.first().uppercaseChar()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColors[request.accountId.hashCode().mod(avatarColors.size)]),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayLetter.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (request.name.isNotBlank()) {
                Text(
                    text = "@${request.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onAccept,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF34D399).copy(alpha = 0.15f)),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Accept",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onReject,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Reject",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ContactInfoRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Suppress("SameParameterValue")
private fun sampleContacts(): List<Contact> {
    return listOf(
        Contact("1", "Alice Johnson", "alicej", "SL2501483921"),
        Contact("2", "Bob Smith", "bobsmith", "SL2502571846"),
        Contact("3", "Charlie Brown", "charlieb", "SL2503749265"),
        Contact("4", "Diana Prince", "dianap", "SL2504812374"),
        Contact("5", "", "eve_adams", "SL2405628491"),
        Contact("6", "", "frank_c", "SL2406483712"),
        Contact("7", "Grace Hopper", "graceh", "SL2507946183"),
        Contact("8", "", "henry_m", "SL2508231579"),
    )
}
