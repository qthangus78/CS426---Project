package com.topic11.cs426.feature.inspection

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.domain.model.InspectionSummary

@Immutable
data class InspectionState(
    val isLoading: Boolean,
    val inspection: InspectionSummary?,
    val eventSink: (InspectionEvent) -> Unit,
) : CircuitUiState

sealed interface InspectionEvent : CircuitUiEvent {
    data object BackSelected : InspectionEvent
}
