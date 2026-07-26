package com.topic11.cs426.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object FieldFlowMigrations {
    /**
     * Version 2 establishes explicit migration support for the persistence milestone.
     *
     * The schema introduced during version 1 development is unchanged, so this migration is
     * intentionally data-preserving and contains no destructive statements.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    /**
     * Version 3 adds successful report export history. It only creates the new table and indexes;
     * existing inspection, catalog, issue, evidence, and sync rows are preserved.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `report_exports` (
                    `id` TEXT NOT NULL,
                    `inspection_id` TEXT NOT NULL,
                    `format` TEXT NOT NULL,
                    `generated_at_ms` INTEGER NOT NULL,
                    `display_filename` TEXT NOT NULL,
                    `storage_key` TEXT NOT NULL,
                    `mime_type` TEXT NOT NULL,
                    `size_bytes` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`inspection_id`) REFERENCES `inspections`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_report_exports_inspection_id` " +
                    "ON `report_exports` (`inspection_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_report_exports_format` " +
                    "ON `report_exports` (`format`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_report_exports_generated_at_ms` " +
                    "ON `report_exports` (`generated_at_ms`)",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
