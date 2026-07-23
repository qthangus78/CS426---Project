package com.topic11.cs426

import com.slack.circuit.foundation.Circuit
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
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
            // Phase 2 demo repositories — replaced in Phase 3 with Room implementations
            // once Lĩnh's :data module is wired into the composition root.
            val inspectionRepository = DemoInspectionRepository()
            val templateRepository = DemoTemplateRepository()

            val observeInspectionSummaries = ObserveInspectionSummariesUseCase(inspectionRepository)
            val observeInspection = ObserveInspectionUseCase(inspectionRepository)

            val circuit = Circuit.Builder()
                .addPresenterFactory(DashboardPresenterFactory(observeInspectionSummaries))
                .addPresenterFactory(
                    InspectionPresenterFactory(
                        observeInspection = observeInspection,
                        templateRepository = templateRepository,
                    ),
                )
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
