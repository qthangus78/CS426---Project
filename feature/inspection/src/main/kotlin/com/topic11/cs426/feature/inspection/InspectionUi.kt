package com.topic11.cs426.feature.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(PaddingValues(horizontal = 20.dp, vertical = 16.dp))
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProgressRow(progress = state.progress)

            Text(
                text = "Section ${state.currentSectionIndex + 1} of ${state.sections.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (section != null) {
                Text(text = section.title, style = MaterialTheme.typography.titleMedium)

                section.items.forEach { item ->
                    ChecklistItemRow(
                        item = item,
                        onAnswerChange = { answer ->
                            state.eventSink(InspectionEvent.AnswerChanged(item.id, answer))
                        },
                    )
                }
            }

            HorizontalDivider()

            SectionNavRow(
                hasPrevious = state.hasPreviousSection,
                hasNext = state.hasNextSection,
                onPrevious = { state.eventSink(InspectionEvent.PreviousSection) },
                onNext = { state.eventSink(InspectionEvent.NextSection) },
            )

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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(PaddingValues(horizontal = 20.dp, vertical = 16.dp))
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProgressRow(progress = state.progress)

            Text(
                text = "Review your answers before completing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.sections.forEach { section ->
                Text(text = section.title, style = MaterialTheme.typography.titleSmall)
                section.items.forEach { item ->
                    ReadOnlyItemRow(item = item)
                }
                HorizontalDivider()
            }

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
            FieldFlowTopAppBar(title = state.title)
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

@Composable
private fun ChecklistItemRow(
    item: ChecklistItemUi,
    onAnswerChange: (ChecklistAnswerUi) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val requiredMarker = if (item.required) " *" else ""
        Text(
            text = "${item.prompt}$requiredMarker",
            style = MaterialTheme.typography.bodyMedium,
        )
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

