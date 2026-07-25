package com.topic11.cs426

import android.content.Context
import androidx.room.Room
import com.slack.circuit.foundation.Circuit
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.core.database.FieldFlowMigrations
import com.topic11.cs426.data.RoomInspectionRepository
import com.topic11.cs426.data.RoomIssueRepository
import com.topic11.cs426.data.RoomTemplateRepository
import com.topic11.cs426.data.seed.FieldFlowSampleDataSeeder
import com.topic11.cs426.data.sync.FakeRemoteSyncAdapter
import com.topic11.cs426.data.sync.FakeSyncOutcome
import com.topic11.cs426.data.sync.FakeSyncScenario
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FieldFlowCompositionRoot private constructor(
    val circuit: Circuit,
    private val database: FieldFlowDatabase?,
    private val applicationScope: CoroutineScope?,
) {
    fun close() {
        applicationScope?.cancel()
        database?.close()
    }

    companion object {
        fun create(
            context: Context,
            dataMode: DataMode = DataMode.ROOM,
        ): FieldFlowCompositionRoot {
            val database = when (dataMode) {
                DataMode.ROOM -> Room.databaseBuilder(
                    context.applicationContext,
                    FieldFlowDatabase::class.java,
                    FieldFlowDatabase.DATABASE_NAME,
                )
                    .addMigrations(*FieldFlowMigrations.ALL)
                    .build()

                DataMode.FAKE -> null
            }
            val inspectionRepository: InspectionRepository
            val templateRepository: TemplateRepository
            val issueRepository: IssueRepository
            when (dataMode) {
                DataMode.ROOM -> {
                    val roomDatabase = requireNotNull(database)
                    inspectionRepository = RoomInspectionRepository(roomDatabase)
                    templateRepository = RoomTemplateRepository(roomDatabase.catalogDao())
                    issueRepository = RoomIssueRepository(roomDatabase.issueDao())
                }

                DataMode.FAKE -> {
                    inspectionRepository = DemoInspectionRepository()
                    templateRepository = DemoTemplateRepository()
                    issueRepository = DemoIssueRepository()
                }
            }
            val applicationScope = database?.let {
                CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scope ->
                    scope.launch {
                        FieldFlowSampleDataSeeder(it).seedIfEmpty()
                    }
                    val fakeSync = FakeRemoteSyncAdapter(
                        syncDao = it.syncDao(),
                        scenario = FakeSyncScenario { FakeSyncOutcome.Success(delayMillis = 250L) },
                        clock = System::currentTimeMillis,
                    )
                    scope.launch {
                        it.syncDao().observeRetryableCommands().collect { commands ->
                            commands.forEach { command -> fakeSync.sync(command.id) }
                        }
                    }
                }
            }

            val observeInspectionSummaries = ObserveInspectionSummariesUseCase(inspectionRepository)
            val observeInspection = ObserveInspectionUseCase(inspectionRepository)
            val saveInspectionDraft = SaveInspectionDraftUseCase(inspectionRepository)
            val validateInspection = ValidateInspectionUseCase()
            val completeInspection = CompleteInspectionUseCase(
                inspectionRepository = inspectionRepository,
                templateRepository = templateRepository,
                validateInspection = validateInspection,
                calculateScore = CalculateInspectionScoreUseCase(),
                createIssue = CreateMaintenanceIssueUseCase(),
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

            return FieldFlowCompositionRoot(
                circuit = circuit,
                database = database,
                applicationScope = applicationScope,
            )
        }
    }
}

enum class DataMode {
    ROOM,
    FAKE,
}
