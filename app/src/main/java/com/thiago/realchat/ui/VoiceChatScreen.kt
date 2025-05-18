package com.thiago.realchat.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiago.realchat.ui.theme.RealChatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(viewModel: VoiceChatViewModel = viewModel()) {
    RealChatTheme {
        val context = LocalContext.current
        val activity = context as? Activity
        val uiState by viewModel.uiState.collectAsState()

        // Permission state holders
        var hasMicPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
        var showRationale by remember { mutableStateOf(false) }
        var permanentlyDenied by remember { mutableStateOf(false) }

        // Start conversation automatically if permission already granted (e.g., after configuration change)
        LaunchedEffect(hasMicPermission) {
            if (hasMicPermission) {
                viewModel.startConversation(context)
            }
        }

        // Launcher to request the permission
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasMicPermission = granted
            if (granted) {
                viewModel.startConversation(context)
            } else {
                // Determine whether to show rationale or treat as permanently denied
                if (activity != null) {
                    showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.RECORD_AUDIO
                    )
                    permanentlyDenied = !showRationale
                }
            }
        }

        // UI
        when {
            hasMicPermission -> {
                // Main conversation UI
                ConversationScaffold(uiState)
            }
            showRationale -> {
                // Show rationale dialog explaining why we need the permission
                PermissionRationaleDialog(
                    onDismiss = { showRationale = false },
                    onConfirm = {
                        showRationale = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
            permanentlyDenied -> {
                // Ask user to enable permission from settings
                PermissionPermanentlyDeniedScreen(onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                })
            }
            else -> {
                // Initial request UI
                RequestPermissionScreen(onRequest = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                })
            }
        }
    }
}

@Composable
private fun ConversationScaffold(uiState: VoiceChatUiState) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message)
                }
            }
            when {
                uiState.isRecording -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                uiState.isThinking -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RequestPermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RealChat needs access to your microphone for voice conversations.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequest) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun PermissionRationaleDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Microphone Permission Required") },
        text = {
            Text("RealChat requires access to your microphone so you can speak to the AI assistant. Without this permission, the app won't be able to hear you.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PermissionPermanentlyDeniedScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Microphone permission has been permanently denied. Please enable it in app settings to continue.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text("Open Settings")
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.sender == Sender.USER) Alignment.End else Alignment.Start
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (alignment == Alignment.End) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (message.sender == Sender.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
} 