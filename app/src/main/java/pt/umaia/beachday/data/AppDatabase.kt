package pt.umaia.beachday.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Praia::class,
        Visita::class,
        CacheTempo::class,
        CacheMar::class,
        PerfilUtilizador::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun praiaDao(): PraiaDao
    abstract fun perfilDao(): PerfilDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        private val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `PerfilUtilizador` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT NOT NULL,
                        `temperaturaMinima` REAL NOT NULL,
                        `chuvaMaxima` REAL NOT NULL,
                        `ventoMaximo` INTEGER NOT NULL,
                        `marMinimo` REAL,
                        `marMaximo` REAL NOT NULL,
                        `aguaMinima` REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun obter(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "beachday.db"
                )
                    .addMigrations(MIGRACAO_1_2)
                    .build()
                    .also { instancia = it }
            }
    }
}
