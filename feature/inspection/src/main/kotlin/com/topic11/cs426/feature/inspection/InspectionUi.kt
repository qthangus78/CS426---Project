package com.topic11.cs426.feature.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.LoadingContent

@Composable
internal fun InspectionUi(
    state: InspectionState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is InspectionState.Loading -> InspectionLoadingScreen(state = state, modifier = modifier)
        is InspectionState.Editing -> InspectionEditingScreen(state = state, modifier = modifier)
        is InspectionState.Reviewing -> InspectionReviewingScreen(state = state, modifier = modifier)
        is InspectionState.ValidationFailed -> InspectionValidationFailedScreen(state = state, modifier = modifier)
        is InspectionState.Completed -> InspectionCompletedScreen(state = state, modifier = modifier)
    }
}

// ── Loading ──────────────────────────────────────────────────────────────────

@Composable
private fun InspectionLoadingScreen(
    state: InspectionState.Loading,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        LoadingContent(
            label = "Loading inspection…",
            modifier = Modifier.padding(innerPadding),
        )
    }
}

// ── Editing ──────────────────────────────────────────────────────────────────

@Composable
private fun InspectionEditingScreen(
    state: InspectionState.Editing,
    modifier: Modifier = Modifier,
) {
    val section = state.currentSection
    Scaffold(
        modifier = modifier,
        topBar = {
            FieldFlowTopAppBar(
                title = state.title,
                onBackClick = { state.eventSink(InspectionEvent.BackSelected) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ProgressRow(progress = state.progress) }

            item {
                Text(
                    text = "Section ${state.currentSectionIndex + 1} of ${state.sections.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (section != null) {
                item {
                    Text(text = section.title, style = MaterialTheme.typography.titleMedium)
                }

                items(section.items, key = { it.id }) { item ->
                    ChecklistItemRow(
                        item = item,
                        onAnswerChange = { answer ->
                            state.eventSink(InspectionEvent.AnswerChanged(item.id, answer))
                        },
                        onNoteChange = { note ->
                            state.eventSink(InspectionEvent.NoteChanged(item.id, note))
                        },
                        onEvidenceAdd = { ref ->
                            state.eventSink(InspectionEvent.EvidenceAdded(item.id, ref))
                        },
                    )
                }
            }

            item { HorizontalDivider() }

            item {
                SectionNavRow(
                    hasPrevious = state.hasPreviousSection,
                    hasNext = state.hasNextSection,
                    onPrevious = { state.eventSink(InspectionEvent.PreviousSection) },
                    onNext = { state.eventSink(InspectionEvent.NextSection) },
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { state.eventSink(InspectionEvent.SaveDraftSelected) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save Draft")
                    }
                    Button(
                        onClick = { state.eventSink(InspectionEvent.ReviewSelected) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Review")
                    }
                }
            }
        }
    }
}

// ── Reviewing ─────────────────────────────────────────────────────────────────

@Composable
private fun InspectionReviewingScreen(
    state: InspectionState.Reviewing,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            FieldFlowTopAppBar(
                title = state.title,
                onBackClick = { state.eventSink(InspectionEvent.BackSelected) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ProgressRow(progress = state.progress) }

            item {
                Text(
                    text = "Review your answers before completing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.sections.forEach { section ->
                item {
                    Text(text = section.title, style = MaterialTheme.typography.titleSmall)
                }
                items(section.items, key = { it.id }) { item ->
                    ReadOnlyItemRow(item = item)
                }
                item { HorizontalDivider() }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { state.eventSink(InspectionEvent.BackSelected) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Edit")
                    }
                    Button(
                        onClick = { state.eventSink(InspectionEvent.CompleteSelected) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Complete")
                    }
                }
            }
        }
    }
}

// ── ValidationFailed ──────────────────────────────────────────────────────────

@Composable
private fun InspectionValidationFailedScreen(
    state: InspectionState.ValidationFailed,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            FieldFlowTopAppBar(
                title = state.title,
                onBackClick = { state.eventSink(InspectionEvent.BackSelected) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(PaddingValues(horizontal = 20.dp, vertical = 16.dp))
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Please fix the following before completing:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )

            state.errors.forEach { error ->
                Text(
                    text = "• ${error.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider()

            Button(
                onClick = { state.eventSink(InspectionEvent.BackSelected) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Fix Errors")
            }
        }
    }
}

// ── Completed ─────────────────────────────────────────────────────────────────

@Composable
private fun InspectionCompletedScreen(
    state: InspectionState.Completed,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            FieldFlowTopAppBar(
                title = state.title,
                onBackClick = { state.eventSink(InspectionEvent.BackSelected) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(PaddingValues(horizontal = 20.dp, vertical = 24.dp))
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "✓ Inspection Complete",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = "Inspection complete" },
            )
            Text(
                text = "${state.summary.completedItems} of ${state.summary.totalItems} items answered",
                style = MaterialTheme.typography.bodyLarge,
            )
            LinearProgressIndicator(
                progress = { state.summary.fraction },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { state.eventSink(InspectionEvent.BackSelected) }) {
                Text("Back to Dashboard")
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun ProgressRow(progress: InspectionProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${progress.completedItems} of ${progress.totalItems} items answered",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistItemRow(
    item: ChecklistItemUi,
    onAnswerChange: (ChecklistAnswerUi) -> Unit,
    onNoteChange: (String) -> Unit,
    onEvidenceAdd: (String) -> Unit,
) {
    var showEvidencePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val requiredMarker = if (item.required) " *" else ""
        Text(
            text = "${item.prompt}$requiredMarker",
            style = MaterialTheme.typography.bodyMedium,
        )
        // Answer input — text field for Text answers, compliance buttons otherwise
        when (val answer = item.answer) {
            is ChecklistAnswerUi.Text -> {
                OutlinedTextField(
                    value = answer.value,
                    onValueChange = { onAnswerChange(ChecklistAnswerUi.Text(it)) },
                    label = { Text("Answer") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isCompliant = (item.answer as? ChecklistAnswerUi.Compliance)?.isCompliant
                    OutlinedButton(
                        onClick = { onAnswerChange(ChecklistAnswerUi.Compliance(isCompliant = true)) },
                        colors = if (isCompliant == true) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        } else ButtonDefaults.outlinedButtonColors(),
                    ) { Text("Pass") }
                    OutlinedButton(
                        onClick = { onAnswerChange(ChecklistAnswerUi.Compliance(isCompliant = false)) },
                        colors = if (isCompliant == false) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            )
                        } else ButtonDefaults.outlinedButtonColors(),
                    ) { Text("Fail") }
                }
            }
        }
        // Note field — always visible so inspectors can annotate any item
        OutlinedTextField(
            value = item.note,
            onValueChange = onNoteChange,
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        // Evidence button — opens picker sheet; shows count when evidence is attached
        TextButton(onClick = { showEvidencePicker = true }) {
            Text(
                if (item.evidenceCount > 0) "Evidence (${item.evidenceCount})" else "Add Evidence",
            )
        }
    }

    // Evidence picker — Phase 1 stub: both options fire a placeholder ref so the
    // evidenceCount increments and the UI reflects the attachment. Phase 2 replaces
    // the stubs with a real camera / gallery intent.
    if (showEvidencePicker) {
        ModalBottomSheet(onDismissRequest = { showEvidencePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Add Evidence",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(
                    onClick = {
                        onEvidenceAdd("photo-stub")
                        showEvidencePicker = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("📷  Take Photo (demo)")
                }
                TextButton(
                    onClick = {
                        onEvidenceAdd("gallery-stub")
                        showEvidencePicker = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("🖼  Choose from Gallery (demo)")
                }
                TextButton(
                    onClick = { showEvidencePicker = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyItemRow(item: ChecklistItemUi) {
    val answerLabel = when (val a = item.answer) {
        is ChecklistAnswerUi.Unanswered -> "—"
        is ChecklistAnswerUi.Compliance -> if (a.isCompliant) "Pass" else "Fail"
        is ChecklistAnswerUi.Text -> a.value
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = item.prompt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = answerLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionNavRow(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrevious, enabled = hasPrevious) { Text("← Previous") }
        TextButton(onClick = onNext, enabled = hasNext) { Text("Next →") }
    }
}

