package com.vajaniluca.accordatorevocale.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vajaniluca.accordatorevocale.AppViewModel
import com.vajaniluca.accordatorevocale.ui.screens.ExerciseEditorScreen
import com.vajaniluca.accordatorevocale.ui.screens.ExerciseListScreen
import com.vajaniluca.accordatorevocale.ui.screens.ExerciseRunScreen
import com.vajaniluca.accordatorevocale.ui.screens.ResultsScreen
import com.vajaniluca.accordatorevocale.ui.screens.SettingsScreen
import com.vajaniluca.accordatorevocale.ui.screens.TunerScreen

sealed interface Screen {
    data object Tuner : Screen
    data object Exercises : Screen
    data object SettingsTab : Screen
    data class Editor(val exerciseId: String?) : Screen
    data class Run(val exerciseId: String) : Screen
    data class Results(val exerciseId: String) : Screen
}

private val tabs = listOf(Screen.Tuner, Screen.Exercises, Screen.SettingsTab)

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        asked = true
    }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.RECORD_AUDIO) }

    if (!hasPermission) {
        PermissionScreen(
            asked = asked,
            onRequest = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
        return
    }

    // Il microfono resta acceso solo mentre l'app è in primo piano.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.startAudio()
                Lifecycle.Event.ON_STOP -> vm.stopAudio()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.stopAudio()
        }
    }

    var stack by remember { mutableStateOf(listOf<Screen>(Screen.Tuner)) }
    val top = stack.last()
    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun replaceTop(s: Screen) { stack = stack.dropLast(1) + s }
    BackHandler(enabled = stack.size > 1) {
        if (top is Screen.Run) vm.cancelExercise()
        pop()
    }

    Scaffold(
        bottomBar = {
            if (top in tabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = top == Screen.Tuner,
                        onClick = { stack = listOf(Screen.Tuner) },
                        icon = { Text("♪", fontSize = 22.sp) },
                        label = { Text("Accordatore") },
                    )
                    NavigationBarItem(
                        selected = top == Screen.Exercises,
                        onClick = { stack = listOf(Screen.Exercises) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Esercizi") },
                    )
                    NavigationBarItem(
                        selected = top == Screen.SettingsTab,
                        onClick = { stack = listOf(Screen.SettingsTab) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Impostazioni") },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (val s = top) {
                Screen.Tuner -> TunerScreen(vm)
                Screen.Exercises -> ExerciseListScreen(
                    vm,
                    onStart = { push(Screen.Run(it)) },
                    onEdit = { push(Screen.Editor(it)) },
                )
                Screen.SettingsTab -> SettingsScreen(vm)
                is Screen.Editor -> ExerciseEditorScreen(vm, s.exerciseId, onDone = ::pop)
                is Screen.Run -> ExerciseRunScreen(
                    vm,
                    s.exerciseId,
                    onBack = ::pop,
                    onFinished = { replaceTop(Screen.Results(s.exerciseId)) },
                )
                is Screen.Results -> ResultsScreen(
                    vm,
                    onRetry = { replaceTop(Screen.Run(s.exerciseId)) },
                    onClose = ::pop,
                )
            }
        }
    }
}

@Composable
private fun PermissionScreen(asked: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎤", fontSize = 56.sp)
        Text("Serve il microfono", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Per capire che nota stai cantando l'app deve ascoltare la tua voce. L'audio viene analizzato sul telefono e non viene registrato né inviato.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onRequest) { Text("Consenti microfono") }
        if (asked) {
            OutlinedButton(onClick = onOpenSettings) { Text("Apri impostazioni dell'app") }
        }
    }
}
