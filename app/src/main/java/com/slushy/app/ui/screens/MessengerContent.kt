package com.slushy.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class ChatPreview(
    val id: String,
    val name: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
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
fun MessengerContent(onChatClick: (String) -> Unit = {}) {
    var showNewChat by remember { mutableStateOf(false) }
    val chats = remember { sampleChats() }

    val unreadChats = chats.filter { it.unreadCount > 0 }
    val readChats = chats.filter { it.unreadCount == 0 }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (unreadChats.isEmpty()) {
                item { EmptySection("You're all up-to-date.") }
            } else {
                items(unreadChats, key = { it.id }) { chat -> ChatItem(chat, onClick = { onChatClick(chat.id) }) }
            }

            if (unreadChats.isNotEmpty() && readChats.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            if (readChats.isEmpty()) {
                item { EmptySection("There are no messages.") }
            } else {
                items(readChats, key = { it.id }) { chat -> ChatItem(chat, onClick = { onChatClick(chat.id) }) }
            }
        }

        FloatingActionButton(
            onClick = { showNewChat = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, contentDescription = "New message")
        }

        if (showNewChat) {
            val bgMod = Modifier
                .fillMaxSize()
                .blur(6.dp)
            LazyColumn(modifier = bgMod) { }
            NewChatDialog(onDismiss = { showNewChat = false })
        }
    }
}

@Composable
private fun EmptySection(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChatItem(chat: ChatPreview, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarColors[chat.name.hashCode().mod(avatarColors.size)]),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = chat.name.first().uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTime(chat.lastMessageTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.size(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewChatDialog(onDismiss: () -> Unit) {
    var selectedContacts by remember { mutableStateOf(listOf<Contact>()) }
    var showContactPicker by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }

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
                    text = "New Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.size(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "To:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showContactPicker = true }
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    ) {
                        if (selectedContacts.isEmpty()) {
                            Text(
                                text = "Tap to select contacts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                selectedContacts.forEach { contact ->
                                    ContactChip(
                                        contact = contact,
                                        onRemove = {
                                            selectedContacts = selectedContacts.filter { it.id != contact.id }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )

                Spacer(modifier = Modifier.size(20.dp))

                Button(
                    onClick = {
                        if (selectedContacts.isNotEmpty()) onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedContacts.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send")
                }
            }
        }

        if (showContactPicker) {
            ContactPickerDialog(
                selectedIds = selectedContacts.map { it.id }.toSet(),
                onToggle = { contact ->
                    selectedContacts = if (selectedContacts.any { it.id == contact.id }) {
                        selectedContacts.filter { it.id != contact.id }
                    } else {
                        selectedContacts + contact
                    }
                },
                onDone = { showContactPicker = false },
            )
        }
    }
}

@Composable
private fun ContactChip(contact: Contact, onRemove: () -> Unit) {
    val displayName = if (contact.name.isNotBlank()) contact.name else contact.username

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ContactPickerDialog(
    selectedIds: Set<String>,
    onToggle: (Contact) -> Unit,
    onDone: () -> Unit,
) {
    val allContacts = remember { samplePickerContacts() }

    Dialog(
        onDismissRequest = onDone,
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
                    text = "Select Contacts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.size(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    items(allContacts, key = { it.id }) { contact ->
                        ContactPickerRow(
                            contact = contact,
                            isSelected = contact.id in selectedIds,
                            onToggle = { onToggle(contact) },
                        )
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun ContactPickerRow(
    contact: Contact,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val displayName = if (contact.name.isNotBlank()) contact.name else contact.username
    val displayLetter = displayName.first().uppercaseChar()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColors[contact.accountId.hashCode().mod(avatarColors.size)]),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayLetter.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> "${diff / 86_400_000}d"
    }
}

@Suppress("SameParameterValue")
private fun sampleChats(): List<ChatPreview> {
    val now = System.currentTimeMillis()
    return listOf(
        ChatPreview("1", "Alice Johnson", "See you tomorrow!", now - 300_000, 3),
        ChatPreview("2", "Bob Smith", "Sounds good", now - 3_600_000, 1),
        ChatPreview("3", "Charlie Brown", "Hey, how are you?", now - 86_400_000, 0),
        ChatPreview("4", "Diana Prince", "The meeting is at 3pm", now - 172_800_000, 0),
        ChatPreview("5", "Eve Adams", "Thanks!", now - 604_800_000, 0),
    )
}

@Suppress("SameParameterValue")
private fun samplePickerContacts(): List<Contact> {
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
