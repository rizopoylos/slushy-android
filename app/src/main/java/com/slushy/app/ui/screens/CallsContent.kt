package com.slushy.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class CallType { Missed, Incoming, Outgoing }

private data class CallLogEntry(
    val id: String,
    val name: String,
    val username: String,
    val type: CallType,
    val timestamp: Long,
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
fun CallsContent() {
    val calls = remember { sampleCalls() }
    var selectedCall by remember { mutableStateOf<CallLogEntry?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }

    val missedCalls = calls.filter { it.type == CallType.Missed }
    val otherCalls = calls.filter { it.type != CallType.Missed }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (missedCalls.isEmpty()) {
                item { EmptySection("No missed calls") }
            } else {
                items(missedCalls, key = { it.id }) { call ->
                    CallItem(
                        call = call,
                        onClick = { selectedCall = call },
                        onCall = { /* TODO: call */ },
                        onVideoCall = { /* TODO: video call */ },
                    )
                }
            }

            if (missedCalls.isNotEmpty() && otherCalls.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            if (otherCalls.isEmpty()) {
                item { EmptySection("No calls yet") }
            } else {
                items(otherCalls, key = { it.id }) { call ->
                    CallItem(
                        call = call,
                        onClick = { selectedCall = call },
                        onCall = { /* TODO: call */ },
                        onVideoCall = { /* TODO: video call */ },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showContactPicker = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Phone, contentDescription = "Make a call")
        }
    }

    selectedCall?.let { call ->
        val bgMod = Modifier
            .fillMaxSize()
            .blur(6.dp)
        LazyColumn(modifier = bgMod) { }
        CallDetailDialog(
            call = call,
            onDismiss = { selectedCall = null },
            onCall = { selectedCall = null },
            onVideoCall = { selectedCall = null },
        )
    }

    if (showContactPicker) {
        val bgMod = Modifier
            .fillMaxSize()
            .blur(6.dp)
        LazyColumn(modifier = bgMod) { }
        NewCallContactPickerDialog(
            onDismiss = { showContactPicker = false },
        )
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
private fun CallItem(
    call: CallLogEntry,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onVideoCall: () -> Unit,
) {
    var isSliderMode by remember { mutableStateOf(false) }
    var itemWidthPx by remember { mutableFloatStateOf(1f) }

    val animOffsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    var lastDragUptime by remember { mutableLongStateOf(0L) }
    var lastDragVelocity by remember { mutableFloatStateOf(0f) }

    val displayName = if (call.name.isNotBlank()) call.name else "@${call.username}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { itemWidthPx = it.width.toFloat() },
    ) {
        if (isSliderMode) {
            Row(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF34D399)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF4A90D9)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(animOffsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .then(
                    if (isSliderMode) Modifier
                    else Modifier.clickable(onClick = onClick)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColors[call.name.hashCode().mod(avatarColors.size)]),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayName.first().uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = callTypeColor(call.type),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = callTypeLabel(call.type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = callTypeColor(call.type),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatCallTime(call.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isSliderMode = true
                            lastDragUptime = 0L
                            lastDragVelocity = 0f
                            scope.launch { animOffsetX.snapTo(0f) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val now = change.uptimeMillis
                            if (lastDragUptime != 0L) {
                                val dt = (now - lastDragUptime) / 1000f
                                if (dt > 0f) {
                                    lastDragVelocity = dragAmount / dt
                                }
                            }
                            lastDragUptime = now

                            val newOffset = (animOffsetX.value + dragAmount)
                                .coerceIn(-itemWidthPx, itemWidthPx)
                            scope.launch { animOffsetX.snapTo(newOffset) }
                        },
                        onDragEnd = {
                            val fraction = animOffsetX.value / itemWidthPx
                            val isFastSwipe = abs(lastDragVelocity) > 1000f
                            val shouldTrigger = abs(fraction) >= 0.2f || isFastSwipe

                            if (shouldTrigger) {
                                val isRightSwipe = fraction > 0f
                                val target = if (isRightSwipe) itemWidthPx else -itemWidthPx
                                scope.launch {
                                    animOffsetX.animateTo(target, tween(150))
                                }
                                if (isRightSwipe) onCall() else onVideoCall()
                                scope.launch {
                                    delay(400)
                                    animOffsetX.snapTo(0f)
                                    isSliderMode = false
                                }
                            } else {
                                scope.launch {
                                    animOffsetX.animateTo(0f, spring(dampingRatio = 0.6f))
                                    isSliderMode = false
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animOffsetX.animateTo(0f, spring(dampingRatio = 0.6f))
                                isSliderMode = false
                            }
                        },
                    )
                },
        )
    }
}

@Composable
private fun CallDetailDialog(
    call: CallLogEntry,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onVideoCall: () -> Unit,
) {
    val displayName = if (call.name.isNotBlank()) call.name else "@${call.username}"

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
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(avatarColors[call.name.hashCode().mod(avatarColors.size)]),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayName.first().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (call.name.isNotBlank()) {
                    Text(
                        text = "@${call.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = callTypeColor(call.type),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (call.type) {
                            CallType.Missed -> "Missed call"
                            CallType.Incoming -> "Incoming call"
                            CallType.Outgoing -> "Outgoing call"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = callTypeColor(call.type),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.size(4.dp))

                Text(
                    text = formatDetailTimestamp(call.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.size(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .clickable(onClick = onCall),
                            shape = CircleShape,
                            color = Color(0xFF34D399),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Call",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .clickable(onClick = onVideoCall),
                            shape = CircleShape,
                            color = Color(0xFF4A90D9),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video call",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Video",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewCallContactPickerDialog(onDismiss: () -> Unit) {
    val allContacts = remember { sampleCallContacts() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(allContacts, searchQuery) {
        if (searchQuery.isBlank()) allContacts
        else allContacts.filter { contact ->
            contact.name.contains(searchQuery, ignoreCase = true) ||
            contact.username.contains(searchQuery, ignoreCase = true)
        }
    }

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
                    text = "New Call",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.size(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search contacts") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                )

                Spacer(modifier = Modifier.size(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    if (filteredContacts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No contacts found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(filteredContacts, key = { it.id }) { contact ->
                            val displayName = if (contact.name.isNotBlank()) contact.name else "@${contact.username}"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(avatarColors[contact.accountId.hashCode().mod(avatarColors.size)]),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = displayName.first().uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
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
                                    if (contact.name.isNotBlank()) {
                                        Text(
                                            text = "@${contact.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { /* TODO: call */ },
                                    shape = CircleShape,
                                    color = Color(0xFF34D399),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { /* TODO: video call */ },
                                    shape = CircleShape,
                                    color = Color(0xFF4A90D9),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Video call",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
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

private fun callTypeColor(type: CallType): Color = when (type) {
    CallType.Missed -> Color(0xFFEF4444)
    CallType.Incoming -> Color(0xFFF59E0B)
    CallType.Outgoing -> Color(0xFF34D399)
}

private fun callTypeLabel(type: CallType): String = when (type) {
    CallType.Missed -> "Missed"
    CallType.Incoming -> "Incoming"
    CallType.Outgoing -> "Outgoing"
}

private fun formatCallTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> "${diff / 86_400_000}d"
    }
}

private fun formatDetailTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val now = Instant.now()
    val zdt = instant.atZone(ZoneId.systemDefault())
    val nowZdt = now.atZone(ZoneId.systemDefault())
    val timeStr = DateTimeFormatter.ofPattern("h:mm a").format(zdt)
    return when {
        zdt.toLocalDate() == nowZdt.toLocalDate() -> "Today at $timeStr"
        zdt.toLocalDate() == nowZdt.toLocalDate().minusDays(1) -> "Yesterday at $timeStr"
        zdt.year == nowZdt.year -> "${DateTimeFormatter.ofPattern("MMMM d").format(zdt)} at $timeStr"
        else -> "${DateTimeFormatter.ofPattern("MMMM d, yyyy").format(zdt)} at $timeStr"
    }
}

@Suppress("SameParameterValue")
private fun sampleCalls(): List<CallLogEntry> {
    val now = System.currentTimeMillis()
    return listOf(
        CallLogEntry("1", "Alice Johnson", "alicej", CallType.Missed, now - 600_000),
        CallLogEntry("2", "Bob Smith", "bobsmith", CallType.Missed, now - 3_600_000),
        CallLogEntry("3", "Charlie Brown", "charlieb", CallType.Incoming, now - 86_400_000),
        CallLogEntry("4", "Diana Prince", "dianap", CallType.Outgoing, now - 172_800_000),
        CallLogEntry("5", "", "eve_adams", CallType.Incoming, now - 604_800_000),
    )
}

@Suppress("SameParameterValue")
private fun sampleCallContacts(): List<Contact> {
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
