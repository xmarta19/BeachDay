package pt.umaia.beachday.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {
    @Query("SELECT * FROM PerfilUtilizador ORDER BY nome")
    fun todos(): Flow<List<PerfilUtilizador>>

    @Query("SELECT * FROM PerfilUtilizador")
    suspend fun lista(): List<PerfilUtilizador>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(perfil: PerfilUtilizador): Long

    @Delete
    suspend fun apagar(perfil: PerfilUtilizador)

    @Query("DELETE FROM PerfilUtilizador")
    suspend fun apagarTodos()
}
