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
    if (temperaturaMaxima < 20) nota -= 2
    if (classeVento >= 3) nota -= 2
    if (probabilidadeChuva > 40) nota -= 3
    if (alturaMaximaMar != null && alturaMaximaMar > 1.5) nota -= 2
    if (temperaturaMaximaAgua != null && temperaturaMaximaAgua < 17) nota -= 1
    return nota.coerceIn(0, 10)
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