package com.topic11.cs426

import com.slack.circuit.foundation.Circuit
import com.topic11.cs426.data.FakeInspectionRepository
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.feature.assets.AssetsPresenterFactory
import com.topic11.cs426.feature.assets.AssetsUiFactory
import com.topic11.cs426.feature.dashboard.DashboardPresenterFactory
import com.topic11.cs426.feature.dashboard.DashboardUiFactory
import com.topic11.cs426.feature.inspection.InspectionPresenterFactory
import com.topic11.cs426.feature.inspection.InspectionUiFactory
import com.topic11.cs426.feature.issues.IssuesPresenterFactory
import com.topic11.cs426.feature.issues.IssuesUiFactory
import com.topic11.cs426.feature.reports.ReportsPresenterFactory
import com.topic11.cs426.feature.reports.ReportsUiFactory
import com.topic11.cs426.feature.templates.TemplatesPresenterFactory
import com.topic11.cs426.feature.templates.TemplatesUiFactory

class FieldFlowCompositionRoot private constructor(
    val circuit: Circuit,
) {
    companion object {
        fun create(): FieldFlowCompositionRoot {
            val inspectionRepository = FakeInspectionRepository()
            val observeInspectionSummaries = ObserveInspectionSummariesUseCase(inspectionRepository)

            val circuit = Circuit.Builder()
                .addPresenterFactory(DashboardPresenterFactory(observeInspectionSummaries))
                // TODO Phase 2: pass ObserveInspectionSessionUseCase once domain layer ships.
                .addPresenterFactory(InspectionPresenterFactory())
                .addPresenterFactory(AssetsPresenterFactory())
                .addPresenterFactory(TemplatesPresenterFactory())
                .addPresenterFactory(IssuesPresenterFactory())
                .addPresenterFactory(ReportsPresenterFactory())
                .addUiFactory(DashboardUiFactory())
                .addUiFactory(InspectionUiFactory())
                .addUiFactory(AssetsUiFactory())
                .addUiFactory(TemplatesUiFactory())
                .addUiFactory(IssuesUiFactory())
                .addUiFactory(ReportsUiFactory())
                .build()

            return FieldFlowCompositionRoot(circuit = circuit)
        }
    }
}
