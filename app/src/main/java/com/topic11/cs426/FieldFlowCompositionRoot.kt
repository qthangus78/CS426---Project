package com.topic11.cs426

import android.content.Context
import androidx.room.Room
import com.slack.circuit.foundation.Circuit
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.core.database.FieldFlowMigrations
import com.topic11.cs426.data.RoomAssetRepository
import com.topic11.cs426.data.RoomInspectionRepository
import com.topic11.cs426.data.RoomTemplateRepository
import com.topic11.cs426.data.evidence.AndroidEvidenceStore
import com.topic11.cs426.data.evidence.EvidenceFileStorage
import com.topic11.cs426.data.seed.FieldFlowSampleDataSeeder
import com.topic11.cs426.domain.repository.AssetRepository
import com.topic11.cs426.domain.repository.EvidenceStore
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.TemplateRepository
import com.topic11.cs426.domain.usecase.CalculateInspectionScoreUseCase
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.CreateMaintenanceIssueUseCase
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplateUseCase
import com.topic11.cs426.domain.usecase.SaveInspectionDraftUseCase
import com.topic11.cs426.domain.usecase.ScheduleNextInspectionUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
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
import kotlinx.coroutines.runBlocking

class FieldFlowCompositionRoot private constructor(
    val circuit: Circuit,
    val evidenceStore: EvidenceStore,
    private val database: FieldFlowDatabase,
) : AutoCloseable {
    private var isClosed = false

    override fun close() {
        if (isClosed) return

        database.close()
        isClosed = true
    }

    companion object {
        /**
         * Creates the app-scoped object graph. [applicationContext] is intentionally accepted
         * at this Android boundary so Room-backed repositories stay out of feature modules.
         */
        fun create(applicationContext: Context): FieldFlowCompositionRoot {
            val database = Room.databaseBuilder(
                applicationContext,
                FieldFlowDatabase::class.java,
                FieldFlowDatabase.DATABASE_NAME,
            )
                .addMigrations(*FieldFlowMigrations.ALL)
                .build()

            return try {
                runBlocking {
                    FieldFlowSampleDataSeeder(database).seedIfEmpty()
                }

                val catalogDao = database.catalogDao()
                val inspectionDao = database.inspectionDao()
                createWithRepositories(
                    database = database,
                    inspectionRepository = RoomInspectionRepository(database),
                    templateRepository = RoomTemplateRepository(catalogDao),
                    assetRepository = RoomAssetRepository(catalogDao),
                    evidenceStore = AndroidEvidenceStore(
                        context = applicationContext,
                        fileStorage = EvidenceFileStorage(applicationContext.filesDir),
                        inspectionDao = inspectionDao,
                    ),
                )
            } catch (throwable: Throwable) {
                database.close()
                throw throwable
            }
        }

        private fun createWithRepositories(
            database: FieldFlowDatabase,
            inspectionRepository: InspectionRepository,
            templateRepository: TemplateRepository,
            assetRepository: AssetRepository,
            evidenceStore: EvidenceStore,
        ): FieldFlowCompositionRoot {
            val observeInspectionSummaries = ObserveInspectionSummariesUseCase(inspectionRepository)
            val observeAssets = ObserveAssetsUseCase(assetRepository)
            val observeTemplates = ObserveTemplatesUseCase(templateRepository)
            val startInspection = StartInspectionUseCase(
                inspectionRepository = inspectionRepository,
                templateRepository = templateRepository,
            )
            val observeInspection = ObserveInspectionUseCase(inspectionRepository)
            val observeTemplate = ObserveTemplateUseCase(templateRepository)
            val saveInspectionDraft = SaveInspectionDraftUseCase(inspectionRepository)
            val validateInspection = ValidateInspectionUseCase()
            val completeInspection = CompleteInspectionUseCase(
                inspectionRepository = inspectionRepository,
                templateRepository = templateRepository,
                assetRepository = assetRepository,
                validateInspection = validateInspection,
                calculateScore = CalculateInspectionScoreUseCase(),
                createIssue = CreateMaintenanceIssueUseCase(),
                scheduleNext = ScheduleNextInspectionUseCase(),
            )

            val circuit = Circuit.Builder()
                .addPresenterFactory(
                    DashboardPresenterFactory(
                        observeInspectionSummaries = observeInspectionSummaries,
                        observeAssets = observeAssets,
                        observeTemplates = observeTemplates,
                        startInspection = startInspection,
                    ),
                )
                .addPresenterFactory(
                    InspectionPresenterFactory(
                        observeInspection = observeInspection,
                        observeTemplate = observeTemplate,
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
                evidenceStore = evidenceStore,
                database = database,
            )
        }
    }
}
