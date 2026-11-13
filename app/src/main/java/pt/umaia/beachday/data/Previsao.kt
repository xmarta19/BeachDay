package pt.umaia.beachday.data

data class DiaPrevisao(
    val dia: String,
    val temperaturaMaxima: Double,
    val probabilidadeChuva: Double,
    val classeVento: Int,
    val alturaMaximaMar: Double?,
    val temperaturaMaximaAgua: Double?,
    val nota: Int
)

fun calcularNota(
    temperaturaMaxima: Double,
    probabilidadeChuva: Double,
    classeVento: Int,
    alturaMaximaMar: Double?,
    temperaturaMaximaAgua: Double?
): Int {
    var nota = 10

    nota -= when {
        temperaturaMaxima >= 25 -> 0
        temperaturaMaxima >= 18 -> 2
        else -> 4
    }

    nota -= when {
        probabilidadeChuva <= 15 -> 0
        probabilidadeChuva <= 30 -> 1
        probabilidadeChuva <= 60 -> 3
        else -> 5
    }

    nota -= when {
        classeVento >= 4 -> 3
        classeVento == 3 -> 2
        else -> 0
    }

    if (alturaMaximaMar != null) {
        nota -= when {
            alturaMaximaMar > 2.5 -> 3
            alturaMaximaMar >= 1.5 -> 2
            else -> 0
        }
    }

    if (temperaturaMaximaAgua != null) {
        nota -= when {
            temperaturaMaximaAgua < 10 -> 3
            temperaturaMaximaAgua < 15 -> 2
            temperaturaMaximaAgua < 20 -> 1
            else -> 0
        }
    }

    return nota.coerceIn(0, 10)
}

fun descricaoVento(classe: Int): String = when (classe) {
    1 -> "Fraco"
    2 -> "Moderado"
    3 -> "Forte"
    4 -> "Muito forte"
    else -> "Sem dados"
}

data class PraiaRanking(
    val praia: Praia,
    val nota: Int?,
    val avisoGrave: Boolean = false
)

fun marcarAvisos(
    lista: List<PraiaRanking>,
    avisos: List<AvisoIpma>
): List<PraiaRanking> {
    val areasGraves = avisos
        .filter { it.awarenessLevelID == "orange" || it.awarenessLevelID == "red" }
        .mapNotNull { it.idAreaAviso }
        .toSet()

    return lista.map { item ->
        val area = item.praia.idAreaAviso
        item.copy(avisoGrave = area != null && area in areasGraves)
    }.sortedBy { it.avisoGrave }
}

data class VisitaComPraia(
    val id: Int,
    val data: Long,
    val nota: Int,
    val comentario: String?,
    val nomePraia: String
)