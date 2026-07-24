package com.topic11.cs426

import android.content.Context
import com.slack.circuit.foundation.Circuit
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.IssueRepository
import com.topic11.cs426.domain.repository.TemplateRepository
import com.topic11.cs426.domain.usecase.CalculateInspectionScoreUseCase
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.CreateMaintenanceIssueUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import com.topic11.cs426.domain.usecase.SaveInspectionDraftUseCase
import com.topic11.cs426.domain.usecase.ScheduleNextInspectionUseCase
import com.topic11.cs426.domain.usecase.ValidateInspectionUseCase
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
) : AutoCloseable {
    private var isClosed = false

    override fun close() {
        if (isClosed) return

        isClosed = true
    }

    companion object {
        /**
         * Creates the app-scoped object graph. [applicationContext] is intentionally accepted
         * at this Android boundary so Phase 3 can build and seed Room here without affecting
         * presentation wiring.
         */
        fun create(applicationContext: Context): FieldFlowCompositionRoot {
            // Phase 3 binding replacement point: create and seed Room-backed repositories here.
            val inspectionRepository = DemoInspectionRepository()
            val templateRepository = DemoTemplateRepository()
            val issueRepository = DemoIssueRepository()

            return createWithRepositories(
                inspectionRepository = inspectionRepository,
                templateRepository = templateRepository,
                issueRepository = issueRepository,
            )
        }

        private fun createWithRepositories(
            inspectionRepository: InspectionRepository,
            templateRepository: TemplateRepository,
            issueRepository: IssueRepository,
        ): FieldFlowCompositionRoot {
            val observeInspectionSummaries = ObserveInspectionSummariesUseCase(inspectionRepository)
            val observeInspection = ObserveInspectionUseCase(inspectionRepository)
            val saveInspectionDraft = SaveInspectionDraftUseCase(inspectionRepository)
            val validateInspection = ValidateInspectionUseCase()

            val completeInspection = CompleteInspectionUseCase(
                inspectionRepository = inspectionRepository,
                templateRepository = templateRepository,
                issueRepository = issueRepository,
                validateInspection = validateInspection,
                calculateScore = CalculateInspectionScoreUseCase(),
                createIssue = CreateMaintenanceIssueUseCase(issueRepository),
                scheduleNext = ScheduleNextInspectionUseCase(),
            )

            val circuit = Circuit.Builder()
                .addPresenterFactory(DashboardPresenterFactory(observeInspectionSummaries))
                .addPresenterFactory(
                    InspectionPresenterFactory(
                        observeInspection = observeInspection,
                        templateRepository = templateRepository,
                        saveInspectionDraft = saveInspectionDraft,
                        validateInspection = validateInspection,
                        completeInspection = completeInspection,
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
