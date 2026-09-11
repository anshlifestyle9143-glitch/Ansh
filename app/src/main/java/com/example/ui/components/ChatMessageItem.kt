package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.VisionAssistantBubble
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionCodeBackground
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionLilacLight
import com.example.ui.theme.VisionLilacPill
import com.example.ui.theme.VisionPrimaryPurple
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextPrimary
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.theme.VisionUserBubble
import com.example.ui.theme.VisionUserBubbleText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "USER"
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("chat_message_${message.id}"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Vision Avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VisionDeepPlum),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Vision AI",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Header Info for assistant
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "Vision",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = VisionDeepPlum
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = VisionLilacPill,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, VisionCardBorder)
                    ) {
                        Text(
                            text = message.engineName,
                            style = MaterialTheme.typography.labelSmall,
                            color = VisionDeepPlum,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (message.latencyMs > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${message.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = VisionTextMuted
                        )
                    }
                }
            }

            // Message Bubble
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 18.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = if (isUser) VisionUserBubble else VisionAssistantBubble,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) VisionDeepPlum else VisionCardBorder.copy(alpha = 0.8f)
                ),
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FormattedMessageContent(content = message.content, isUser = isUser)
                }
            }

            // Action row (Copy, TTS, Timestamp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                val timeString = remember(message.timestamp) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                }
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = VisionTextMuted
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Vision Message", message.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        tint = VisionTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (!isUser) {
                    IconButton(
                        onClick = onSpeakClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isSpeaking) "Stop voice" else "Read aloud",
                            tint = if (isSpeaking) VisionPrimaryPurple else VisionTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            // User Avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VisionPrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FormattedMessageContent(content: String, isUser: Boolean = false) {
    val context = LocalContext.current
    val parts = remember(content) { parseMarkdownParts(content) }
    val textColor = if (isUser) VisionUserBubbleText else VisionTextPrimary

    Column {
        parts.forEach { part ->
            when (part) {
                is ContentPart.CodeBlock -> {
                    CodeBlockCard(code = part.code, language = part.language)
                }
                is ContentPart.Heading -> {
                    Text(
                        text = part.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) VisionLilacLight else VisionDeepPlum,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is ContentPart.BulletPoint -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) VisionLilacLight else VisionPrimaryPurple
                        )
                        Text(
                            text = part.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            lineHeight = 22.sp
                        )
                    }
                }
                is ContentPart.NormalText -> {
                    Text(
                        text = part.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(code: String, language: String) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = VisionCodeBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, VisionCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            // Code header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B2930))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = VisionLilacLight,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Code", code)
                            clipboard.setPrimaryClip(clip)
                            copied = true
                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) VisionLilacLight else Color(0xFFCAC4D0),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copied) VisionLilacLight else Color(0xFFCAC4D0)
                    )
                }
            }

            // Code body
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                ),
                color = Color(0xFFF4EFF4),
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

sealed class ContentPart {
    data class NormalText(val text: String) : ContentPart()
    data class Heading(val text: String) : ContentPart()
    data class BulletPoint(val text: String) : ContentPart()
    data class CodeBlock(val code: String, val language: String) : ContentPart()
}

fun parseMarkdownParts(rawText: String): List<ContentPart> {
    val parts = mutableListOf<ContentPart>()
    val codeBlockRegex = Regex("```([a-zA-Z0-9_]*)\\n?([\\s\\S]*?)```")

    var currentIndex = 0
    val matches = codeBlockRegex.findAll(rawText).toList()

    for (match in matches) {
        val range = match.range
        if (range.first > currentIndex) {
            val textBefore = rawText.substring(currentIndex, range.first)
            processTextLines(textBefore, parts)
        }

        val language = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd()
        parts.add(ContentPart.CodeBlock(code = code, language = language))

        currentIndex = range.last + 1
    }

    if (currentIndex < rawText.length) {
        val remainingText = rawText.substring(currentIndex)
        processTextLines(remainingText, parts)
    }

    return parts
}

private fun processTextLines(text: String, parts: MutableList<ContentPart>) {
    val lines = text.split("\n")
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isBlank()) continue

        when {
            trimmed.startsWith("### ") -> parts.add(ContentPart.Heading(trimmed.removePrefix("### ")))
            trimmed.startsWith("## ") -> parts.add(ContentPart.Heading(trimmed.removePrefix("## ")))
            trimmed.startsWith("# ") -> parts.add(ContentPart.Heading(trimmed.removePrefix("# ")))
            trimmed.startsWith("- ") -> parts.add(ContentPart.BulletPoint(trimmed.removePrefix("- ")))
            trimmed.startsWith("* ") -> parts.add(ContentPart.BulletPoint(trimmed.removePrefix("* ")))
            trimmed.startsWith("• ") -> parts.add(ContentPart.BulletPoint(trimmed.removePrefix("• ")))
            else -> parts.add(ContentPart.NormalText(trimmed))
        }
    }
}

