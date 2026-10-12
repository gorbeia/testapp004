package com.example.testapp004.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testapp004.data.PendingIntentConsent
import com.example.testapp004.viewmodel.BackupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(state.consentRequest) {
        val consent = state.consentRequest as? PendingIntentConsent ?: return@LaunchedEffect
        consentLauncher.launch(IntentSenderRequest.Builder(consent.pendingIntent).build())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & restore") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                "Your data is saved to a private, hidden folder in your Google Drive that only this app can read.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            }
            Button(
                onClick = viewModel::backUp,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Back up now") }
            OutlinedButton(
                onClick = viewModel::requestRestore,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Restore from backup") }
            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }

    if (state.showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestore,
            title = { Text("Restore backup?") },
            text = { Text("This replaces all people, categories and relations on this device with the backup.") },
            confirmButton = { TextButton(onClick = viewModel::confirmRestore) { Text("Restore") } },
            dismissButton = { TextButton(onClick = viewModel::dismissRestore) { Text("Cancel") } },
        )
    }
}
