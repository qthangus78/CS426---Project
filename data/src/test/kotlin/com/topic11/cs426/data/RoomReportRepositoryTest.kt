package com.topic11.cs426.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.data.seed.FieldFlowSampleDataSeeder
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.domain.model.ReportId
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
class RoomReportRepositoryTest {
    private lateinit var database: FieldFlowDatabase
    private lateinit var repository: RoomReportRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FieldFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomReportRepository(database.reportHistoryDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `successful export history is observable and persisted in Room`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val entry = ReportHistoryEntry(
            id = ReportId("report-json"),
            inspectionId = InspectionId("computer-lab-i-44"),
            format = ReportFormat.JSON,
            generatedAtMillis = 6_000L,
            displayFilename = "fieldflow-computer-lab-i-44-report-json.json",
            storageKey = "reports/fieldflow-computer-lab-i-44-report-json.json",
            mimeType = "application/json",
            sizeBytes = 512L,
        )

        repository.saveExport(entry)

        assertEquals(listOf(entry), repository.observeExportHistory().first())
        assertEquals(
            "reports/fieldflow-computer-lab-i-44-report-json.json",
            database.reportHistoryDao().getExport("report-json")?.storageKey,
        )
    }
}
