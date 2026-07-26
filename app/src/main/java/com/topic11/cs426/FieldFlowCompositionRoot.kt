package com.topic11.cs426

import android.content.Context
import androidx.room.Room
import com.slack.circuit.foundation.Circuit
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.core.database.FieldFlowMigrations
import com.topic11.cs426.data.RoomAssetRepository
import com.topic11.cs426.data.RoomInspectionRepository
import com.topic11.cs426.data.RoomIssueRepository
import com.topic11.cs426.data.RoomReportRepository
import com.topic11.cs426.data.RoomTemplateRepository
import com.topic11.cs426.data.evidence.AndroidEvidenceStore
import com.topic11.cs426.data.evidence.EvidenceFileStorage
import com.topic11.cs426.data.report.FieldFlowReportExporter
import com.topic11.cs426.data.report.JsonReportExporter
import com.topic11.cs426.data.report.PdfReportExporter
import com.topic11.cs426.data.report.ReportFileStorage
import com.topic11.cs426.data.seed.FieldFlowSampleDataSeeder
import com.topic11.cs426.domain.repository.AssetRepository
import com.topic11.cs426.domain.repository.AppearancePreferenceRepository
import com.topic11.cs426.domain.repository.EvidenceStore
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.IssueRepository
import com.topic11.cs426.domain.repository.ReportExporter
import com.topic11.cs426.domain.repository.ReportRepository
import com.topic11.cs426.domain.repository.TemplateRepository
import com.topic11.cs426.domain.usecase.CalculateInspectionScoreUseCase
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.CreateAssetUseCase
import com.topic11.cs426.domain.usecase.CreateLocationUseCase
import com.topic11.cs426.domain.usecase.CreateMaintenanceIssueUseCase
import com.topic11.cs426.domain.usecase.CreateTemplateUseCase
import com.topic11.cs426.domain.usecase.ExportInspectionReportUseCase
import com.topic11.cs426.domain.usecase.GenerateInspectionReportUseCase
import com.topic11.cs426.domain.usecase.GetAssetUseCase
import com.topic11.cs426.domain.usecase.GetLocationUseCase
import com.topic11.cs426.domain.usecase.GetTemplateUseCase
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import com.topic11.cs426.domain.usecase.ObserveIssueUseCase
import com.topic11.cs426.domain.usecase.ObserveIssuesUseCase
import com.topic11.cs426.domain.usecase.ObserveLocationsUseCase
import com.topic11.cs426.domain.usecase.ObserveReportCandidatesUseCase
import com.topic11.cs426.domain.usecase.ObserveReportHistoryUseCase
import com.topic11.cs426.domain.usecase.ObserveThemeModeUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplateUseCase
import com.topic11.cs426.domain.usecase.SaveInspectionDraftUseCase
import com.topic11.cs426.domain.usecase.ScheduleNextInspectionUseCase
import com.topic11.cs426.domain.usecase.SetThemeModeUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import com.topic11.cs426.domain.usecase.UpdateAssetUseCase
import com.topic11.cs426.domain.usecase.UpdateIssueStatusUseCase
import com.topic11.cs426.domain.usecase.UpdateLocationUseCase
import com.topic11.cs426.domain.usecase.UpdateTemplateMetadataUseCase
import com.topic11.cs426.domain.usecase.ValidateInspectionUseCase
import com.topic11.cs426.feature.assets.AssetsPresenterFactory
import com.topic11.cs426.feature.assets.AssetsUiFactory
import com.topic11.cs426.feature.dashboard.DashboardPresenterFactory
import com.topic11.cs426.feature.dashboard.DashboardUiFactory
import com.topic11.cs426.feature.inspection.InspectionPresenterFactory
import com.topic11.cs426.feature.inspection.InspectionUiFactory
import com.topic11.cs426.feature.issues.IssuesPresenterFactory
import com.topic11.cs426.feature.issues.IssuesUiFactory
import com.topic11.cs426.feature.locations.LocationsPresenterFactory
import com.topic11.cs426.feature.locations.LocationsUiFactory
import com.topic11.cs426.feature.reports.ReportActionHandler
import com.topic11.cs426.feature.reports.ReportsPresenterFactory
import com.topic11.cs426.feature.reports.ReportsUiFactory
import com.topic11.cs426.feature.settings.SettingsPresenterFactory
import com.topic11.cs426.feature.settings.SettingsUiFactory
import com.topic11.cs426.feature.templates.TemplatesPresenterFactory
import com.topic11.cs426.feature.templates.TemplatesUiFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class FieldFlowCompositionRoot private constructor(
    val circuit: Circuit,
    val evidenceStore: EvidenceStore,
    val reportActionHandler: ReportActionHandler,
    val observeThemeMode: ObserveThemeModeUseCase,
    private val database: FieldFlowDatabase,
    private val appScope: CoroutineScope,
    private val seedFailure: AtomicReference<Throwable?>,
    internal val sampleDataSeedJob: Job,
) : AutoCloseable {
    private var isClosed = false

    internal val sampleDataSeedingFailure: Throwable?
        get() = seedFailure.get()

    override fun close() {
        if (isClosed) return

        appScope.cancel()
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
            val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            return try {
                val catalogDao = database.catalogDao()
                val inspectionDao = database.inspectionDao()
                val issueDao = database.issueDao()
                val reportFileStorage = ReportFileStorage(applicationContext.filesDir)
                val reportExporter = FieldFlowReportExporter(
                    jsonReportExporter = JsonReportExporter(reportFileStorage),
                    pdfReportExporter = PdfReportExporter(reportFileStorage),
                )
                createWithRepositories(
                    database = database,
                    appScope = appScope,
                    inspectionRepository = RoomInspectionRepository(database),
                    templateRepository = RoomTemplateRepository(catalogDao),
                    assetRepository = RoomAssetRepository(catalogDao),
                    issueRepository = RoomIssueRepository(issueDao),
                    reportRepository = RoomReportRepository(database.reportHistoryDao()),
                    reportExporter = reportExporter,
                    appearancePreferenceRepository = AndroidAppearancePreferenceRepository.create(applicationContext),
                    evidenceStore = AndroidEvidenceStore(
                        context = applicationContext,
                        fileStorage = EvidenceFileStorage(applicationContext.filesDir),
                        inspectionDao = inspectionDao,
                    ),
                    reportActionHandler = AndroidReportActionHandler(
                        context = applicationContext,
                        fileStorage = reportFileStorage,
                    ),
                    seedSampleData = {
                        FieldFlowSampleDataSeeder(database).seedIfEmpty()
                    },
                )
            } catch (throwable: Throwable) {
                appScope.cancel()
                database.close()
                throw throwable
            }
        }

        private fun createWithRepositories(
            database: FieldFlowDatabase,
            appScope: CoroutineScope,
            inspectionRepository: InspectionRepository,
            templateRepository: TemplateRepository,
            assetRepository: AssetRepository,
            issueRepository: IssueRepository,
            reportRepository: ReportRepository,
            reportExporter: ReportExporter,
            appearancePreferenceRepository: AppearancePreferenceRepository,
            evidenceStore: EvidenceStore,
            reportActionHandler: ReportActionHandler,
            seedSampleData: suspend () -> Unit,
        ): FieldFlowCompositionRoot {
            val observeInspectionSummaries = ObserveInspectionSummariesUseCase(inspectionRepository)
            val observeAssets = ObserveAssetsUseCase(assetRepository)
            val observeLocations = ObserveLocationsUseCase(assetRepository)
            val getLocation = GetLocationUseCase(assetRepository)
            val createLocation = CreateLocationUseCase(assetRepository)
            val updateLocation = UpdateLocationUseCase(assetRepository)
            val observeTemplates = ObserveTemplatesUseCase(templateRepository)
            val getAsset = GetAssetUseCase(assetRepository)
            val createAsset = CreateAssetUseCase(assetRepository)
            val updateAsset = UpdateAssetUseCase(assetRepository)
            val getTemplate = GetTemplateUseCase(templateRepository)
            val createTemplate = CreateTemplateUseCase(templateRepository)
            val updateTemplateMetadata = UpdateTemplateMetadataUseCase(templateRepository)
            val observeIssues = ObserveIssuesUseCase(issueRepository)
            val observeIssue = ObserveIssueUseCase(issueRepository)
            val updateIssueStatus = UpdateIssueStatusUseCase(issueRepository)
            val observeReportCandidates = ObserveReportCandidatesUseCase(inspectionRepository)
            val observeReportHistory = ObserveReportHistoryUseCase(reportRepository)
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
            val generateInspectionReport = GenerateInspectionReportUseCase(
                inspectionRepository = inspectionRepository,
                templateRepository = templateRepository,
                issueRepository = issueRepository,
            )
            val exportInspectionReport = ExportInspectionReportUseCase(
                generateInspectionReport = generateInspectionReport,
                reportExporter = reportExporter,
                reportRepository = reportRepository,
            )
            val observeThemeMode = ObserveThemeModeUseCase(appearancePreferenceRepository)
            val setThemeMode = SetThemeModeUseCase(appearancePreferenceRepository)

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
                .addPresenterFactory(
                    AssetsPresenterFactory(
                        observeAssets = observeAssets,
                        observeLocations = observeLocations,
                        getAsset = getAsset,
                        createAsset = createAsset,
                        updateAsset = updateAsset,
                        observeTemplates = observeTemplates,
                        startInspection = startInspection,
                    ),
                )
                .addPresenterFactory(
                    LocationsPresenterFactory(
                        observeLocations = observeLocations,
                        getLocation = getLocation,
                        createLocation = createLocation,
                        updateLocation = updateLocation,
                    ),
                )
                .addPresenterFactory(
                    TemplatesPresenterFactory(
                        observeTemplates = observeTemplates,
                        observeTemplate = observeTemplate,
                        getTemplate = getTemplate,
                        createTemplate = createTemplate,
                        updateTemplateMetadata = updateTemplateMetadata,
                        observeAssets = observeAssets,
                        startInspection = startInspection,
                    ),
                )
                .addPresenterFactory(
                    IssuesPresenterFactory(
                        observeIssues = observeIssues,
                        observeIssue = observeIssue,
                        observeAssets = observeAssets,
                        observeInspectionSummaries = observeInspectionSummaries,
                        updateIssueStatus = updateIssueStatus,
                    ),
                )
                .addPresenterFactory(
                    ReportsPresenterFactory(
                        observeReportCandidates = observeReportCandidates,
                        observeReportHistory = observeReportHistory,
                        generateInspectionReport = generateInspectionReport,
                        exportInspectionReport = exportInspectionReport,
                        reportActionHandler = reportActionHandler,
                    ),
                )
                .addPresenterFactory(
                    SettingsPresenterFactory(
                        observeThemeMode = observeThemeMode,
                        setThemeMode = setThemeMode,
                    ),
                )
                .addUiFactory(DashboardUiFactory())
                .addUiFactory(InspectionUiFactory())
                .addUiFactory(AssetsUiFactory())
                .addUiFactory(LocationsUiFactory())
                .addUiFactory(TemplatesUiFactory())
                .addUiFactory(IssuesUiFactory())
                .addUiFactory(ReportsUiFactory())
                .addUiFactory(SettingsUiFactory())
                .build()

            val seedFailure = AtomicReference<Throwable?>()
            val sampleDataSeedJob = launchFieldFlowStartupSeeding(
                scope = appScope,
                seedSampleData = seedSampleData,
                onFailure = seedFailure::set,
            )

            return FieldFlowCompositionRoot(
                circuit = circuit,
                evidenceStore = evidenceStore,
                reportActionHandler = reportActionHandler,
                observeThemeMode = observeThemeMode,
                database = database,
                appScope = appScope,
                seedFailure = seedFailure,
                sampleDataSeedJob = sampleDataSeedJob,
            )
        }
    }
}

internal fun launchFieldFlowStartupSeeding(
    scope: CoroutineScope,
    seedSampleData: suspend () -> Unit,
    onFailure: (Throwable) -> Unit,
): Job = scope.launch {
    try {
        seedSampleData()
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        onFailure(throwable)
    }
}
