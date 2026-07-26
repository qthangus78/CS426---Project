package com.topic11.cs426.feature.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.EmptyState
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.LoadingContent
import com.topic11.cs426.core.designsystem.StatusBadge
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.ChecklistAnswerType

@Composable
internal fun TemplatesUi(
    state: TemplatesState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("templates-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Inspection templates",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(TemplatesEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                TemplatesState.Loading -> {
                    item { LoadingContent(label = "Loading templates") }
                }
                is TemplatesState.Empty -> {
                    item {
                        ListHeader(
                            title = "Inspection templates",
                            actionLabel = "Add template",
                            onAction = { state.eventSink(TemplatesEvent.AddSelected) },
                        )
                    }
                    item {
                        EmptyState(
                            title = "No templates yet",
                            message = "Add an inspection template to make it available for new inspections.",
                        )
                    }
                }
                is TemplatesState.Content -> {
                    item {
                        ListHeader(
                            title = "Inspection templates",
                            actionLabel = "Add template",
                            onAction = { state.eventSink(TemplatesEvent.AddSelected) },
                        )
                    }
                    items(
                        items = state.templates,
                        key = { template -> template.id.value },
                    ) { template ->
                        TemplateListCard(
                            template = template,
                            onClick = { state.eventSink(TemplatesEvent.TemplateSelected(template.id)) },
                        )
                    }
                }
                is TemplatesState.Error -> {
                    item {
                        EmptyState(
                            title = "Templates unavailable",
                            message = state.message,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TemplateDetailUi(
    state: TemplateDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("template-detail-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Template details",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(TemplateDetailEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            TemplateDetailState.Loading -> {
                LoadingContent(
                    label = "Loading template",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                )
            }
            is TemplateDetailState.Missing -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                ) {
                    EmptyState(
                        title = "Template not found",
                        message = "This template may have been removed.",
                    )
                }
            }
            is TemplateDetailState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        TemplateSummaryCard(template = state.template)
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = { state.eventSink(TemplateDetailEvent.EditSelected) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Edit template")
                            }
                            Button(
                                onClick = { state.eventSink(TemplateDetailEvent.StartInspectionSelected) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Start inspection")
                            }
                        }
                    }
                    items(
                        items = state.template.sections,
                        key = { section -> section.title },
                    ) { section ->
                        TemplateSectionCard(section = section)
                    }
                }
                if (state.startInspection.isVisible) {
                    TemplateStartInspectionDialog(
                        state = state.startInspection,
                        eventSink = state.eventSink,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TemplateEditorUi(
    state: TemplateEditorState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("template-editor-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = when (state) {
                    is TemplateEditorState.Editing -> state.title
                    else -> "Template"
                },
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(TemplateEditorEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            TemplateEditorState.Loading -> {
                LoadingContent(
                    label = "Loading template",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                )
            }
            is TemplateEditorState.Missing -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                ) {
                    EmptyState(
                        title = "Template not found",
                        message = "This template may have been removed.",
                    )
                }
            }
            is TemplateEditorState.Editing -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        TemplateForm(
                            state = state,
                            eventSink = state.eventSink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun TemplateListCard(
    template: TemplateListItemUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("template-card-${template.id.value}"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${template.versionLabel} • ${template.sectionCountLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TemplateSummaryCard(template: TemplateDetailUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "${template.versionLabel} • ${template.recurrenceLabel}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${template.sections.size} sections",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TemplateSectionCard(section: TemplateSectionUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
            )
            section.items.forEach { item ->
                ChecklistItemRow(item = item)
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(item: TemplateChecklistItemUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
        )
        item.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.required) StatusBadge(label = "Required", tone = StatusTone.InProgress)
            if (item.critical) StatusBadge(label = "Critical", tone = StatusTone.Warning)
            StatusBadge(label = item.weightLabel, tone = StatusTone.Neutral)
        }
        Text(
            text = item.answerTypeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TemplateForm(
    state: TemplateEditorState.Editing,
    eventSink: (TemplateEditorEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.validationMessages.forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.saveErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = state.form.name,
                onValueChange = { eventSink(TemplateEditorEvent.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Template name") },
                singleLine = true,
                enabled = !state.isSaving,
            )
            OutlinedTextField(
                value = state.form.recurrenceDays,
                onValueChange = { eventSink(TemplateEditorEvent.RecurrenceChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Recurrence days") },
                singleLine = true,
                enabled = !state.isSaving,
            )
            if (state.isCreate) {
                Text(
                    text = "Initial checklist",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = state.form.sectionTitle,
                    onValueChange = { eventSink(TemplateEditorEvent.SectionTitleChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Section title") },
                    singleLine = true,
                    enabled = !state.isSaving,
                )
                OutlinedTextField(
                    value = state.form.itemTitle,
                    onValueChange = { eventSink(TemplateEditorEvent.ItemTitleChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Checklist item") },
                    singleLine = true,
                    enabled = !state.isSaving,
                )
                OutlinedTextField(
                    value = state.form.itemDescription,
                    onValueChange = { eventSink(TemplateEditorEvent.ItemDescriptionChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Item description") },
                    enabled = !state.isSaving,
                )
                OutlinedTextField(
                    value = state.form.weight,
                    onValueChange = { eventSink(TemplateEditorEvent.WeightChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Weight") },
                    singleLine = true,
                    enabled = !state.isSaving,
                )
                ToggleRow(
                    label = "Required",
                    checked = state.form.required,
                    enabled = !state.isSaving,
                    onCheckedChange = { eventSink(TemplateEditorEvent.RequiredChanged(it)) },
                )
                ToggleRow(
                    label = "Critical",
                    checked = state.form.critical,
                    enabled = !state.isSaving,
                    onCheckedChange = { eventSink(TemplateEditorEvent.CriticalChanged(it)) },
                )
                Text(
                    text = "Answer type",
                    style = MaterialTheme.typography.labelLarge,
                )
                ChecklistAnswerType.entries.forEach { answerType ->
                    AnswerTypeRow(
                        answerType = answerType,
                        selected = answerType == state.form.answerType,
                        enabled = !state.isSaving,
                        onClick = { eventSink(TemplateEditorEvent.AnswerTypeSelected(answerType)) },
                    )
                }
            }
            Button(
                onClick = { eventSink(TemplateEditorEvent.SaveSelected) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving..." else "Save template")
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AnswerTypeRow(
    answerType: ChecklistAnswerType,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
        )
        Text(
            text = answerType.label(),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TemplateStartInspectionDialog(
    state: TemplateStartInspectionUi,
    eventSink: (TemplateDetailEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { eventSink(TemplateDetailEvent.StartInspectionDismissed) },
        title = { Text("Start inspection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (state.assets.isEmpty()) {
                    Text(
                        text = "No assets available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.assets.forEach { asset ->
                        AssetRow(
                            asset = asset,
                            selected = asset.id == state.selectedAssetId,
                            onClick = { eventSink(TemplateDetailEvent.AssetSelected(asset.id)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = state.canStart,
                onClick = { eventSink(TemplateDetailEvent.StartInspectionConfirmed) },
            ) {
                Text(if (state.isStarting) "Starting..." else "Start")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !state.isStarting,
                onClick = { eventSink(TemplateDetailEvent.StartInspectionDismissed) },
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AssetRow(
    asset: AssetOptionUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = asset.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            asset.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun ChecklistAnswerType.label(): String {
    return when (this) {
        ChecklistAnswerType.PASS_FAIL_NA -> "Pass / Fail / N/A"
        ChecklistAnswerType.YES_NO -> "Yes / No"
        ChecklistAnswerType.TEXT -> "Text"
        ChecklistAnswerType.NUMBER -> "Number"
        ChecklistAnswerType.SINGLE_CHOICE -> "Single choice"
    }
}

private fun TemplatesState.eventSinkOrNull(): ((TemplatesEvent) -> Unit)? {
    return when (this) {
        TemplatesState.Loading -> null
        is TemplatesState.Content -> eventSink
        is TemplatesState.Empty -> eventSink
        is TemplatesState.Error -> eventSink
    }
}

private fun TemplateDetailState.eventSinkOrNull(): ((TemplateDetailEvent) -> Unit)? {
    return when (this) {
        TemplateDetailState.Loading -> null
        is TemplateDetailState.Content -> eventSink
        is TemplateDetailState.Missing -> eventSink
    }
}

private fun TemplateEditorState.eventSinkOrNull(): ((TemplateEditorEvent) -> Unit)? {
    return when (this) {
        TemplateEditorState.Loading -> null
        is TemplateEditorState.Editing -> eventSink
        is TemplateEditorState.Missing -> eventSink
    }
}
