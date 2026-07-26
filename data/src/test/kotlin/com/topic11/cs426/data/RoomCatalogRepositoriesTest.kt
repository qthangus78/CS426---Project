package com.topic11.cs426.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.data.seed.FieldFlowSampleDataSeeder
import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.InspectionSection
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.model.TemplateId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomCatalogRepositoriesTest {
    private lateinit var database: FieldFlowDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FieldFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `template repository exposes stable revision id and ordered checklist`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val repository = RoomTemplateRepository(database.catalogDao())

        val summaries = repository.observeTemplates().first()
        val template = repository.getTemplate(TemplateId("sample-template-v1"))

        assertEquals(listOf("sample-template-v1"), summaries.map { it.id.value })
        assertEquals(4, template?.sections?.single()?.items?.size)
        assertEquals("sample-item-0", template?.sections?.single()?.items?.first()?.id?.value)
    }

    @Test
    fun `asset repository maps location name and full asset`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val repository = RoomAssetRepository(database.catalogDao())

        val summaries = repository.observeAssets().first()
        val asset = repository.getAsset(AssetId("sample-asset-lab-i44"))

        assertEquals("HCMUS", summaries.first { it.id == asset?.id }.locationName)
        assertEquals("LAB-I44", asset?.code)

        repository.saveAsset(requireNotNull(asset).copy(nextInspectionDueAtMillis = 42_000L))

        assertEquals(
            42_000L,
            repository.getAsset(asset.id)?.nextInspectionDueAtMillis,
        )
    }

    @Test
    fun `asset repository creates asset and exposes locations`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val repository = RoomAssetRepository(database.catalogDao())

        val locations = repository.observeLocations().first()
        val locationId = locations.single().id
        val asset = Asset(
            id = AssetId("asset-created"),
            name = "Room 202",
            code = "ROOM-202",
            locationId = locationId,
        )

        repository.saveAsset(asset)

        assertEquals(asset, repository.getAsset(asset.id))
        assertEquals(asset, repository.getAssetByCode("ROOM-202"))
        assertEquals("Room 202", repository.observeAssets().first().first { it.id == asset.id }.name)
        assertEquals(locationId, repository.getLocation(locationId)?.id)
    }

    @Test
    fun `asset repository preserves unique asset code constraint`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val repository = RoomAssetRepository(database.catalogDao())
        val locationId = repository.observeLocations().first().single().id
        val first = Asset(
            id = AssetId("asset-first"),
            name = "Room 203",
            code = "ROOM-203",
            locationId = locationId,
        )
        val duplicate = Asset(
            id = AssetId("asset-second"),
            name = "Room 204",
            code = "ROOM-203",
            locationId = locationId,
        )

        repository.saveAsset(first)
        var failure: Throwable? = null
        try {
            repository.saveAsset(duplicate)
        } catch (throwable: Throwable) {
            failure = throwable
        }

        assertEquals(IllegalArgumentException::class.java, failure?.javaClass)
        assertEquals("Asset code already exists: ROOM-203", failure?.message)
        assertEquals(first, repository.getAsset(first.id))
        assertEquals(null, repository.getAsset(duplicate.id))
    }

    @Test
    fun `template repository saves aggregate template without losing checklist semantics`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val repository = RoomTemplateRepository(database.catalogDao())
        val templateId = TemplateId("template-created")
        val sectionId = SectionId("template-created-section")
        val itemId = ChecklistItemId("template-created-item")
        val template = InspectionTemplate(
            id = templateId,
            name = "Room readiness",
            version = 1,
            recurrencePolicyDays = 45,
            sections = listOf(
                InspectionSection(
                    id = sectionId,
                    templateId = templateId,
                    title = "Equipment",
                    order = 0,
                    items = listOf(
                        ChecklistItem(
                            id = itemId,
                            sectionId = sectionId,
                            title = "Projector starts",
                            description = "Use the room remote.",
                            required = true,
                            critical = true,
                            weight = 3,
                            answerType = ChecklistAnswerType.PASS_FAIL_NA,
                        ),
                    ),
                ),
            ),
        )

        repository.saveTemplate(template)

        val saved = requireNotNull(repository.getTemplate(templateId))
        assertEquals(template.name, saved.name)
        assertEquals(template.recurrencePolicyDays, saved.recurrencePolicyDays)
        assertEquals(template.sections.single().title, saved.sections.single().title)
        assertEquals(template.sections.single().items.single().critical, saved.sections.single().items.single().critical)
        assertEquals(template.sections.single().items.single().weight, saved.sections.single().items.single().weight)
    }

    @Test
    fun `issue repository creates and updates through Room`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val repository = RoomIssueRepository(database.issueDao(), clock = { 9_000L })
        val inspection = requireNotNull(
            RoomInspectionRepository(database)
                .getInspection(com.topic11.cs426.domain.model.InspectionId("computer-lab-i-44")),
        )
        val issue = MaintenanceIssue(
            id = IssueId("issue-room"),
            inspectionId = inspection.id,
            assetId = inspection.assetId,
            severity = IssueSeverity.MAJOR,
            title = "Projector cable",
            status = MaintenanceIssueStatus.OPEN,
            createdAtMillis = 8_000L,
        )

        repository.createIssue(issue)
        repository.updateIssue(issue.copy(status = MaintenanceIssueStatus.RESOLVED))

        val issuesForInspection = repository.getIssuesForInspection(inspection.id)

        assertEquals(
            MaintenanceIssueStatus.RESOLVED,
            repository.observeIssues().first().single().status,
        )
        assertEquals(
            MaintenanceIssueStatus.RESOLVED,
            repository.observeIssue(issue.id).first()?.status,
        )
        assertEquals(listOf(issue.id), issuesForInspection.map { it.id })
        assertEquals(9_000L, database.issueDao().getIssue("issue-room")?.updatedAtMillis)
    }
}
