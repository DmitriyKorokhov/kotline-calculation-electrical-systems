package data.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    fun init() {
        val appDataDir = File(System.getProperty("user.home"), ".ElectricEditorAppData")
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }

        val dbFile = File(appDataDir, "equipment.db")
        val isFirstLaunch = !dbFile.exists()

        val db = Database.connect("jdbc:sqlite:${dbFile.absolutePath}", "org.sqlite.JDBC")

        transaction(db) {
            SchemaUtils.create(
                BreakerModels, BreakerVariants,
                RcdModels, RcdVariants,
                RcboModels, RcboVariants,
                Cables,
                AtsModels, AtsVariants
            )
        }

        if (isFirstLaunch) {
            DbSeeder.seedInitialData()
        } else {
            DbSeeder.seedInitialData()
        }
    }
}
