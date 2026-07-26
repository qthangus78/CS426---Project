package com.topic11.cs426.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FieldFlowMigrationTest {
    @get:Rule
    @Suppress("DEPRECATION")
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        requireNotNull(FieldFlowDatabase::class.java.canonicalName),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `migration 1 to 3 preserves existing records and adds report history`() {
        val databasePath = ApplicationProvider.getApplicationContext<Context>()
            .getDatabasePath(TEST_DATABASE)
            .absolutePath
        helper.createDatabase(databasePath, 1).apply {
            execSQL(
                "INSERT INTO locations (id, name, parent_id) VALUES (?, ?, ?)",
                arrayOf("location-lab", "Computer Laboratory", null),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            databasePath,
            FieldFlowDatabase.VERSION,
            true,
            FieldFlowMigrations.MIGRATION_1_2,
            FieldFlowMigrations.MIGRATION_2_3,
        ).use { database ->
            database.query(
                "SELECT name FROM locations WHERE id = ?",
                arrayOf("location-lab"),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Computer Laboratory", cursor.getString(0))
            }
            database.query("SELECT COUNT(*) FROM report_exports").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "fieldflow-migration-test"
    }
}
