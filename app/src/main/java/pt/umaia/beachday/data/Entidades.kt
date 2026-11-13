package pt.umaia.beachday.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Praia")
data class Praia(
    @PrimaryKey val id: Int,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val regiao: String,
    val tipo: String,
    val idLocalIpma: Int? = null,
    val idLocalTempo: Int? = null,
    val idLocalMar: Int? = null,
    val idAreaAviso: String? = null,
    val nomeLocalTempo: String? = null,
    val nomeLocalMar: String? = null,
    val favorita: Boolean = false
)

@Entity(
    tableName = "Visita",
    foreignKeys = [ForeignKey(
        entity = Praia::class,
        parentColumns = ["id"],
        childColumns = ["idPraia"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("idPraia")]
)
data class Visita(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idPraia: Int,
    val data: Long,
    val nota: Int,
    val comentario: String? = null
)

@Entity(tableName = "CacheTempo", primaryKeys = ["idLocal", "dia"])
data class CacheTempo(
    val idLocal: Int,
    val dia: String,
    val temperaturaMaxima: Double,
    val probabilidadeChuva: Double,
    val classeVento: Int,
    val atualizadoEm: Long,
    val direcaoVento: String? = null
)

@Entity(tableName = "CacheMar", primaryKeys = ["idLocal", "dia"])
data class CacheMar(
    val idLocal: Int,
    val dia: String,
    val alturaMaximaMar: Double,
    val temperaturaMaximaAgua: Double,
    val atualizadoEm: Long
)

@Entity(tableName = "PerfilUtilizador")
data class PerfilUtilizador(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val temperaturaMinima: Double = 25.0,
    val chuvaMaxima: Double = 15.0,
    val ventoMaximo: Int = 2,
    val marMinimo: Double? = null,
    val marMaximo: Double = 1.5,
    val aguaMinima: Double = 20.0
)
