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
        override fun migrate(database: SupportSQLiteDatabase) = Unit
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
