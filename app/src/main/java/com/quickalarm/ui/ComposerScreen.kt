package com.quickalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.quickalarm.alarm.HandoffResult
import com.quickalarm.alarm.ScheduledDay
import java.time.format.TextStyle
import java.util.Locale

/** Screen-local UI state for the composer sheet. */
sealed interface ComposerUiState {
    data object Editing : ComposerUiState
    data class Done(val result: HandoffResult) : ComposerUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    onConfirm: (hour: Int, minute: Int, title: String) -> HandoffResult,
    onDismiss: () -> Unit,
    onUnlockToType: () -> Unit = {},
) {
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var uiState by remember { mutableStateOf<ComposerUiState>(ComposerUiState.Editing) }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Grab handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp),
                        ),
                )
                Spacer(Modifier.height(16.dp))

                when (val state = uiState) {
                    is ComposerUiState.Editing -> EditingContent(
                        hour = hour,
                        minute = minute,
                        onHourChange = { hour = it },
                        onMinuteChange = { minute = it },
                        title = title,
                        onTitleChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        titleError = titleError,
                        focusRequester = focusRequester,
                        onSet = {
                            if (title.isBlank()) {
                                titleError = true
                            } else {
                                keyboard?.hide()
                                uiState = ComposerUiState.Done(
                                    onConfirm(hour, minute, title.trim()),
                                )
                            }
                        },
                        onCancel = onDismiss,
                        onUnlockToType = onUnlockToType,
                    )

                    is ComposerUiState.Done -> DoneContent(
                        result = state.result,
                        onDone = onDismiss,
                        onRetry = { uiState = ComposerUiState.Editing },
                    )
                }
            }
        }
    }

    // Auto-focus the title so the user can type immediately.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditingContent(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    titleError: Boolean,
    focusRequester: FocusRequester,
    onSet: () -> Unit,
    onCancel: () -> Unit,
    onUnlockToType: () -> Unit,
) {
    Text(
        text = "New alarm",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(16.dp))

    // 24-hour time wheels
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        WheelPicker(
            values = (0..23).toList(),
            selectedValue = hour,
            onValueChange = onHourChange,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        WheelPicker(
            values = (0..59).toList(),
            selectedValue = minute,
            onValueChange = onMinuteChange,
            modifier = Modifier.width(80.dp),
        )
    }

    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Title (required)") },
        singleLine = true,
        isError = titleError,
        supportingText = if (titleError) {
            { Text("Enter a title for the alarm") }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSet() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )

    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onUnlockToType) { Text("Unlock to type") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSet) { Text("Set alarm") }
        }
    }
}

@Composable
private fun DoneContent(
    result: HandoffResult,
    onDone: () -> Unit,
    onRetry: () -> Unit,
) {
    when (result) {
        is HandoffResult.Success -> {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Alarm set",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = successMessage(result),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Done") }
        }

        is HandoffResult.NoClockApp -> ErrorBlock(
            message = "No Clock app on this phone can handle alarm creation.",
            onDone = onDone,
            onRetry = null,
        )

        is HandoffResult.Failed -> ErrorBlock(
            message = "Couldn't set the alarm: ${result.message}",
            onDone = onDone,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun ErrorBlock(
    message: String,
    onDone: () -> Unit,
    onRetry: (() -> Unit)?,
) {
    Icon(
        imageVector = Icons.Filled.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.height(48.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Couldn't set alarm",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    Text(text = message, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(20.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDone) { Text("Close") }
        if (onRetry != null) {
            Spacer(Modifier.width(8.dp))
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

private fun successMessage(result: HandoffResult.Success): String {
    val time = String.format(Locale.getDefault(), "%02d:%02d", result.hour, result.minute)
    val dayText = when (result.day) {
        ScheduledDay.TODAY -> "today"
        ScheduledDay.TOMORROW -> {
            val date = com.quickalarm.alarm.SetAlarmHandoff.dateFor(result.day)
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            "tomorrow ($weekday)"
        }
    }
    return "\"${result.title}\" at $time, $dayText."
}
