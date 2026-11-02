package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.GentleSoundMode
import com.example.ui.theme.DarkSurface

/** Picks one timer's own sound. Null is "Default": whichever sound is chosen in settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSoundSheet(
    selected: GentleSoundMode?,
    defaultSound: GentleSoundMode,
    onSelect: (GentleSoundMode?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            SheetHeader(title = "Sound", onDone = onDismiss)

            SettingsGroup {
                CheckRow(
                    title = "Default",
                    value = defaultSound.title,
                    isSelected = selected == null,
                    onClick = { onSelect(null) },
                    modifier = Modifier.testTag("timer_sound_default")
                )
                GentleSoundMode.values().forEach { mode ->
                    RowDivider()
                    CheckRow(
                        title = mode.title,
                        isSelected = mode == selected,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.testTag("timer_sound_${mode.name}")
                    )
                }
            }
        }
    }
}
