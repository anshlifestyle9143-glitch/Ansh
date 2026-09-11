package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import com.example.ui.components.VisionBottomNav
import com.example.ui.components.VisionHeader
import com.example.ui.components.VisionTab
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EnginesScreen
import com.example.ui.screens.MemoryVaultScreen
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
    var currentTab by remember { mutableStateOf(VisionTab.HOME) }
    var showEngineSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val activeEngine by viewModel.activeEngine.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VisionBackground,
        topBar = {
            VisionHeader(
                activeEngine = activeEngine,
                onEngineClick = { showEngineSheet = true },
                onHistoryClick = { showHistorySheet = true },
                onNewChatClick = {
                    viewModel.createNewSession()
                    currentTab = VisionTab.CHAT
                }
            )
        },
        bottomBar = {
            VisionBottomNav(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
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
    VisionTab.HOME -> DashboardScreen(viewModel = viewModel, onNavigate = { currentTab = it })
    VisionTab.CHAT -> ChatScreen(viewModel = viewModel)
            when (currentTab) {
                VisionTab.CHAT -> ChatScreen(viewModel = viewModel)
                VisionTab.MEMORY -> MemoryVaultScreen(viewModel = viewModel)
                VisionTab.ENGINES -> EnginesScreen(viewModel = viewModel)
                VisionTab.CREATOR -> CreatorScreen(viewModel = viewModel)
            }
        }
    }

    if (showEngineSheet) {
        EngineSelectorSheet(
            selectedEngine = activeEngine,
            onEngineSelected = { engine ->
                viewModel.setEngine(engine)
            },
            onDismiss = { showEngineSheet = false }
        )
    }

    if (showHistorySheet) {
        SessionDrawerSheet(
            sessions = sessions,
            currentSessionId = currentSessionId,
            onSessionSelected = { sessionId ->
                viewModel.selectSession(sessionId)
                currentTab = VisionTab.CHAT
            },
            onDeleteSession = { sessionId ->
                viewModel.deleteSession(sessionId)
            },
            onNewSession = {
                viewModel.createNewSession()
                currentTab = VisionTab.CHAT
            },
            onDismiss = { showHistorySheet = false }
        )
    }
}
