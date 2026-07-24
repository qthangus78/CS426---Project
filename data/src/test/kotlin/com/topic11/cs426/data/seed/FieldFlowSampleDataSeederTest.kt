package com.topic11.cs426.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.FieldFlowDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FieldFlowSampleDataSeederTest {
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
    fun `seed creates deterministic Room samples only once`() = runTest {
        val seeder = FieldFlowSampleDataSeeder(database)

        assertTrue(seeder.seedIfEmpty())
        assertFalse(seeder.seedIfEmpty())

        val summaries = database.inspectionDao().observeInspectionSummaries().first()
        assertEquals(3, summaries.size)
        assertEquals(
            listOf("computer-lab-i-44", "projector-p-204", "laboratory-a2-safety-check"),
            summaries.map { it.inspectionId },
        )
        assertEquals(listOf(2, 0, 4), summaries.map { it.completedItems })
        assertEquals(listOf(4, 4, 4), summaries.map { it.totalItems })
        assertEquals(1, database.syncDao().getCommands().size)
    }
}
