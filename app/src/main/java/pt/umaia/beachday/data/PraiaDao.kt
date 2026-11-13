package pt.umaia.beachday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PraiaDao {
    @Query("SELECT COUNT(*) FROM Praia")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirTodas(praias: List<Praia>)

    @Query("""
        SELECT * FROM Praia
        WHERE (:regiao IS NULL OR regiao = :regiao)
        AND nome LIKE '%' || :pesquisa || '%'
        ORDER BY nome
    """)
    fun pesquisar(pesquisa: String, regiao: String?): Flow<List<Praia>>

    @Query("SELECT * FROM Praia WHERE id = :id")
    fun obter(id: Int): Flow<Praia?>

    @Query("SELECT COUNT(*) FROM Praia WHERE idLocalTempo IS NULL")
    suspend fun contarSemAssociacao(): Int

    @Query("SELECT * FROM Praia")
    suspend fun listarTodas(): List<Praia>

    @Update
    suspend fun atualizarTodas(praias: List<Praia>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTempo(dados: List<CacheTempo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarMar(dados: List<CacheMar>)

    @Query("SELECT * FROM CacheTempo WHERE idLocal = :id AND dia >= :hoje ORDER BY dia LIMIT 3")
    fun obterTempo(id: Int, hoje: String): Flow<List<CacheTempo>>

    @Query("SELECT * FROM CacheMar WHERE idLocal = :id AND dia >= :hoje ORDER BY dia LIMIT 3")
    fun obterMar(id: Int, hoje: String): Flow<List<CacheMar>>

    @Query("""
        SELECT DISTINCT idLocalTempo FROM Praia
        WHERE idLocalTempo IS NOT NULL
        AND (:regiao IS NULL OR regiao = :regiao)
    """)
    suspend fun locaisTempo(regiao: String?): List<Int>

    @Query("SELECT * FROM CacheTempo WHERE dia = :hoje")
    fun tempoDoDia(hoje: String): Flow<List<CacheTempo>>

    @Query("SELECT * FROM CacheMar WHERE dia = :hoje")
    fun marDoDia(hoje: String): Flow<List<CacheMar>>

    @Query("SELECT MAX(atualizadoEm) FROM CacheTempo")
    fun ultimaAtualizacao(): Flow<Long?>

    @Query("SELECT MAX(atualizadoEm) FROM CacheTempo")
    suspend fun ultimaAtualizacaoAgora(): Long?

    @Query("SELECT MAX(atualizadoEm) FROM CacheTempo WHERE idLocal = :id")
    suspend fun atualizacaoTempo(id: Int): Long?

    @Query("SELECT MAX(atualizadoEm) FROM CacheMar WHERE idLocal = :id")
    suspend fun atualizacaoMar(id: Int): Long?

    @Query("UPDATE Praia SET favorita = :favorita WHERE id = :id")
    suspend fun definirFavorita(id: Int, favorita: Boolean)

    @Query("SELECT * FROM Praia WHERE favorita = 1 ORDER BY nome")
    fun favoritas(): Flow<List<Praia>>

    @Insert
    suspend fun inserirVisita(visita: Visita)

    @Query("DELETE FROM Visita WHERE id = :id")
    suspend fun apagarVisita(id: Int)

    @Query("""
        SELECT Visita.id, Visita.data, Visita.nota, Visita.comentario,
               Praia.nome AS nomePraia
        FROM Visita
        INNER JOIN Praia ON Praia.id = Visita.idPraia
        ORDER BY Visita.data DESC
    """)
    fun visitas(): Flow<List<VisitaComPraia>>
}
