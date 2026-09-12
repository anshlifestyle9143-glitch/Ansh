package com.example

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.EngineSelectorSheet
import com.example.ui.components.SessionDrawerSheet
import com.example.ui.components.VisionHeader
import com.example.ui.components.VisionTab
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EnginesScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceConversationScreen
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionTheme
import com.example.ui.viewmodel.VisionViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VisionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VisionTheme {
                VisionApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VisionApp(viewModel: VisionViewModel) {

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

    var showEngineSheet by remember {
        mutableStateOf(false)
    }

    var showHistorySheet by remember {
        mutableStateOf(false)
    }

    val activeEngine by viewModel.activeEngine.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

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
     * ANDROID BACK HANDLING
     *
     * Priority:
     * 1. Voice screen -> Home
     * 2. Engine sheet -> close sheet
     * 3. History sheet -> close sheet
     * 4. Any inner screen -> Home
     * 5. Home -> normal Android back
     */
    BackHandler(
        enabled = showVoiceCall ||
                showEngineSheet ||
                showHistorySheet ||
                currentTab != VisionTab.HOME
    ) {

        when {
            showVoiceCall -> {
                showVoiceCall = false
            }

            showEngineSheet -> {
                showEngineSheet = false
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
            }
        )

    } else {

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = VisionBackground,

            topBar = {

                VisionHeader(
                    activeEngine = activeEngine,

                    onEngineClick = {
                        showEngineSheet = true
                    },

                    onHistoryClick = {
                        showHistorySheet = true
                    },

                    onNewChatClick = {
                        viewModel.createNewSession()

                        // Normal Chat.
                        // Voice auto-start nahi hoga.
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

                    // ---------------- HOME ----------------

                    VisionTab.HOME -> {

                        DashboardScreen(
                            viewModel = viewModel,

                            onNavigate = { destination ->
                                currentTab = destination
                            },

                            onStartVoiceCall = {
                                showVoiceCall = true
                            },

                            onShowHistory = {
                                showHistorySheet = true
                            },

                            onNewChat = {

                                viewModel.createNewSession()

                                // Home -> Chat
                                // Voice auto-start nahi hoga.
                                autoStartChatVoice = false

                                currentTab = VisionTab.CHAT
                            }
                        )
                    }

                    // ---------------- CHAT ----------------

                    VisionTab.CHAT -> {

                        ChatScreen(
                            viewModel = viewModel,

                            autoStartVoice = autoStartChatVoice,

                            onAutoStartHandled = {
                                autoStartChatVoice = false
                            }
                        )
                    }

                    // ---------------- MEMORY ----------------

                    VisionTab.MEMORY -> {

                        MemoryVaultScreen(
                            viewModel = viewModel
                        )
                    }

                    // ---------------- AI ENGINES ----------------

                    VisionTab.ENGINES -> {

                        EnginesScreen(
                            viewModel = viewModel
                        )
                    }

                    // ---------------- VISION INFO ----------------

                    VisionTab.CREATOR -> {

                        CreatorScreen(
                            viewModel = viewModel
                        )
                    }

                    // ---------------- SETTINGS ----------------

                    VisionTab.SETTINGS -> {

                        SettingsScreen(

                            onBack = {
                                currentTab = VisionTab.HOME
                            },

                            onOpenEngines = {
                                currentTab = VisionTab.ENGINES
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

        // ---------------- ENGINE SELECTOR ----------------

        if (showEngineSheet) {

            EngineSelectorSheet(

                selectedEngine = activeEngine,

                onEngineSelected = { engine ->

                    viewModel.setEngine(engine)
                    showEngineSheet = false
                },

                onDismiss = {
                    showEngineSheet = false
                }
            )
        }

        // ---------------- HISTORY ----------------

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

                    viewModel.createNewSession()

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
