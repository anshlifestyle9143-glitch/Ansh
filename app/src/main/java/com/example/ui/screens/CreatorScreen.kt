package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.R
import com.example.data.auth.AuthManager
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionCardBg
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionEmerald
import com.example.ui.theme.VisionRose
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel
import kotlinx.coroutines.launch

@Composable
fun CreatorScreen(
    viewModel: VisionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val userEmail by viewModel.userEmail.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VisionBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // Creator Hero Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VisionCardBg,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VisionCardBorder.copy(alpha = 0.8f)
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(VisionDeepPlum),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Ansh Yadav",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ansh Yadav",
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = VisionDeepPlum
                )

                Text(
                    text = "Creator & AI Systems Architect",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = VisionDeepPlum
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Vision was engineered as a high-performance native Android AI assistant. Featuring autonomous local memory injection, modular multi-model orchestration, and an elegant artistic interface.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VisionTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cloud Account
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VisionCardBg,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VisionCardBorder.copy(alpha = 0.8f)
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Cloud Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = VisionDeepPlum
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (userEmail != null) {
                    Text(
                        text = "Signed in as $userEmail",
                        color = VisionTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.signOut() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VisionCardBorder,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.restoreFromCloud() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VisionEmerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore from Cloud")
                    }
                } else {
                    Text(
                        text = "Sign in to sync memory across devices",
                        color = VisionTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val webClientId =
                                    context.getString(R.string.default_web_client_id)

                                val result =
                                    AuthManager.signInWithGoogle(
                                        context,
                                        webClientId
                                    )

                                viewModel.onSignInResult(result)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VisionDeepPlum,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign in with Google")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Storage & Reset Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VisionCardBg,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VisionCardBorder.copy(alpha = 0.8f)
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VisionRose,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Data Privacy & Local Vault",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = VisionDeepPlum
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "All chat logs, memory facts, and engine configurations remain 100% on-device inside your private SQLite database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VisionTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VisionRose.copy(alpha = 0.12f),
                        contentColor = VisionRose
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        VisionRose.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_data_button")
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        "Reset Local Vault & History",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = VisionCardBg,
            title = {
                Text(
                    "Reset All Data?",
                    fontWeight = FontWeight.Bold,
                    color = VisionRose
                )
            },
            text = {
                Text(
                    "This will clear all chat sessions, messages, and custom memories from the local database. Default Vision system facts will be restored.",
                    color = VisionTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VisionRose,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Reset Everything",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text(
                        "Cancel",
                        color = VisionTextMuted
                    )
                }
            }
        )
    }
}
