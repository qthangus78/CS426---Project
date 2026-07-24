package com.topic11.cs426.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.data.seed.FieldFlowSampleDataSeeder
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
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

        assertEquals(
            MaintenanceIssueStatus.RESOLVED,
            repository.observeIssues().first().single().status,
        )
        assertEquals(9_000L, database.issueDao().getIssue("issue-room")?.updatedAtMillis)
    }
}
