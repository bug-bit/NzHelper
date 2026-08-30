package me.neko.nzhelper.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.neko.nzhelper.core.database.dao.AiConfigDao
import me.neko.nzhelper.core.database.dao.RecycleBinDao
import me.neko.nzhelper.core.database.dao.SessionDao
import me.neko.nzhelper.core.database.dao.TaxonomyDao
import me.neko.nzhelper.core.database.dao.WebDavConfigDao
import me.neko.nzhelper.core.database.entity.AiConfigEntity
import me.neko.nzhelper.core.database.entity.RecycleBinEntity
import me.neko.nzhelper.core.database.entity.SessionEntity
import me.neko.nzhelper.core.database.entity.TaxonomyEntity
import me.neko.nzhelper.core.database.entity.WebDavConfigEntity
import me.neko.nzhelper.core.security.DbKeyProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        SessionEntity::class,
        RecycleBinEntity::class,
        WebDavConfigEntity::class,
        TaxonomyEntity::class,
        AiConfigEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun webDavConfigDao(): WebDavConfigDao
    abstract fun taxonomyDao(): TaxonomyDao
    abstract fun aiConfigDao(): AiConfigDao

    companion object {
        private const val DB_NAME = "records.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            ensureNativeLoaded()
            val passphrase = DbKeyProvider.get(context.applicationContext)
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `webdav_config` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`url` TEXT NOT NULL, " +
                            "`username` TEXT NOT NULL, " +
                            "`password` TEXT NOT NULL, " +
                            "`remotePath` TEXT NOT NULL, " +
                            "`autoBackup` INTEGER NOT NULL, " +
                            "`lastBackupTime` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `taxonomy` (" +
                            "`type` TEXT NOT NULL, " +
                            "`payloadJson` TEXT NOT NULL, " +
                            "PRIMARY KEY(`type`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ai_config` (" +
                            "`key` TEXT NOT NULL, " +
                            "`value` TEXT NOT NULL, " +
                            "PRIMARY KEY(`key`))"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `mode` TEXT NOT NULL DEFAULT 'SOLO_MALE'"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `climaxCount` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `partnerClimaxCount` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `partnerGender` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `partnerName` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `contraception` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "UPDATE `sessions` SET `climaxCount` = CASE WHEN `climax` = 1 THEN 1 ELSE 0 END"
                )
            }
        }

        @Volatile
        private var nativeLoaded = false

        private fun ensureNativeLoaded() {
            if (nativeLoaded) return
            synchronized(this) {
                if (!nativeLoaded) {
                    System.loadLibrary("sqlcipher")
                    nativeLoaded = true
                }
            }
        }
    }
}
