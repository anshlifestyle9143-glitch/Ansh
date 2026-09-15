package com.example

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.SessionDrawerSheet
import com.example.ui.components.VisionHeader
import com.example.ui.components.VisionTab
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceConversationScreen
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionTheme
import com.example.ui.viewmodel.VisionViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VisionViewModel by viewModels()

    private var pendingVoiceCall = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        handleIntent(intent)
        openOverlayPermissionSettings()

        setContent {
            VisionTheme {
                VisionApp(
                    viewModel = viewModel,
                    pendingVoiceCall = pendingVoiceCall
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        handleIntent(intent)
    }

    private fun openOverlayPermissionSettings() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {

        if (
            intent?.getBooleanExtra(
                EXTRA_OPEN_VOICE_CALL,
                false
            ) == true
        ) {

            pendingVoiceCall.value = true

            /*
             * Consume the wake-word intent immediately.
             * This prevents the same intent from being processed
             * again if the Activity lifecycle changes.
             */
            intent.removeExtra(EXTRA_OPEN_VOICE_CALL)
        }
    }

    companion object {
        const val EXTRA_OPEN_VOICE_CALL = "open_voice_call"
    }
}

@Composable
fun VisionApp(
    viewModel: VisionViewModel,
    pendingVoiceCall: MutableState<Boolean>
) {

    val context = LocalContext.current

    var currentTab by remember {
        mutableStateOf(VisionTab.HOME)
    }

    var showVoiceCall by remember {
        mutableStateOf(false)
    }

    var autoStartChatVoice by remember {
        mutableStateOf(false)
    }

    var showHistorySheet by remember {
        mutableStateOf(false)
    }

    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()

    LaunchedEffect(toastMessage) {

        toastMessage?.let {

            Toast.makeText(
                context,
                it,
                Toast.LENGTH_SHORT
            ).show()

            viewModel.clearToast()
        }
    }

    /*
     * WAKE-WORD → VOICE HANDOFF
     *
     * Wake-word service detects the trigger,
     * MainActivity receives the intent,
     * service is stopped,
     * then exactly one voice screen is opened.
     */
    LaunchedEffect(pendingVoiceCall.value) {

        if (
            pendingVoiceCall.value &&
            !showVoiceCall
        ) {

            viewModel.stopWakeWordService()

            showVoiceCall = true

            pendingVoiceCall.value = false
        }
    }

    /*
     * ANDROID BACK HANDLING
     *
     * Priority:
     * 1. Voice screen -> Home
     * 2. History sheet -> close sheet
     * 3. Any inner screen -> Home
     * 4. Home -> normal Android back
     */
    BackHandler(
        enabled =
            showVoiceCall ||
                    showHistorySheet ||
                    currentTab != VisionTab.HOME
    ) {

        when {

            showVoiceCall -> {

                showVoiceCall = false

                if (wakeWordEnabled) {
                    viewModel.startWakeWordService()
                }
            }

            showHistorySheet -> {
                showHistorySheet = false
            }

            currentTab != VisionTab.HOME -> {

                currentTab = VisionTab.HOME

                autoStartChatVoice = false
            }
        }
    }

    if (showVoiceCall) {

        VoiceConversationScreen(
            viewModel = viewModel,

            onExit = {

                showVoiceCall = false

                currentTab = VisionTab.HOME

                if (wakeWordEnabled) {
                    viewModel.startWakeWordService()
                }
            }
        )

    } else {

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = VisionBackground,

            topBar = {

                VisionHeader(
                    onHistoryClick = {
                        showHistorySheet = true
                    },

                    onNewChatClick = {

                        viewModel.startNewChat()

                        autoStartChatVoice = false

                        currentTab = VisionTab.CHAT
                    }
                )
            }

        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(VisionBackground)
            ) {

                when (currentTab) {

                    VisionTab.HOME -> {

                        DashboardScreen(
                            viewModel = viewModel,

                            onNavigate = { destination ->
                                currentTab = destination
                            },

                            onStartVoiceCall = {

                                viewModel.stopWakeWordService()

                                showVoiceCall = true
                            },

                            onShowHistory = {
                                showHistorySheet = true
                            },

                            onNewChat = {

                                viewModel.startNewChat()

                                autoStartChatVoice = false

                                currentTab = VisionTab.CHAT
                            }
                        )
                    }

                    VisionTab.CHAT -> {

                        ChatScreen(
                            viewModel = viewModel,
                            autoStartVoice = autoStartChatVoice,

                            onAutoStartHandled = {
                                autoStartChatVoice = false
                            }
                        )
                    }

                    VisionTab.MEMORY -> {

                        MemoryVaultScreen(
                            viewModel = viewModel
                        )
                    }

                    VisionTab.CREATOR -> {

                        CreatorScreen(
                            viewModel = viewModel
                        )
                    }

                    VisionTab.SETTINGS -> {

                        SettingsScreen(
                            viewModel = viewModel,

                            onBack = {
                                currentTab = VisionTab.HOME
                            },

                            onOpenMemory = {
                                currentTab = VisionTab.MEMORY
                            },

                            onOpenVisionInfo = {
                                currentTab = VisionTab.CREATOR
                            }
                        )
                    }
                }
            }
        }

        if (showHistorySheet) {

            SessionDrawerSheet(
                sessions = sessions,
                currentSessionId = currentSessionId,

                onSessionSelected = { sessionId ->

                    viewModel.selectSession(sessionId)

                    currentTab = VisionTab.CHAT

                    showHistorySheet = false
                },

                onDeleteSession = { sessionId ->
                    viewModel.deleteSession(sessionId)
                },

                onNewSession = {

                    viewModel.startNewChat()

                    autoStartChatVoice = false

                    currentTab = VisionTab.CHAT

                    showHistorySheet = false
                },

                onDismiss = {
                    showHistorySheet = false
                }
            )
        }
    }
}
