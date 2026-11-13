package pt.umaia.beachday.data

enum class Perfil(val nome: String, val descricao: String) {
    APANHAR_SOL("Apanhar sol", "Um dia normal de praia: sol, areal e banho."),
    SURF("Surf e bodyboard", "Boas ondas e pouco vento. Só praias costeiras."),
    KITE("Kite e windsurf", "Vento suficiente, sem ser perigoso."),
    PERSONALIZADO("Personalizado", "Escolhes os teus próprios limites.")
}

data class Regras(
    val temperaturaMinima: Double = 25.0,
    val chuvaMaxima: Double = 15.0,
    val ventoMaximo: Int = 2,
    val marMaximo: Double = 1.5,
    val aguaMinima: Double = 20.0
)

data class DiaPrevisao(
    val dia: String,
    val temperaturaMaxima: Double,
    val probabilidadeChuva: Double,
    val classeVento: Int,
    val direcaoVento: String?,
    val relacaoVento: String?,
    val alturaMaximaMar: Double?,
    val temperaturaMaximaAgua: Double?,
    val nota: Int?
)

fun orientacaoPraia(praia: Praia): String? = when {
    praia.tipo == "fluvial" -> null
    praia.regiao == "Madeira" || praia.regiao == "Açores" -> null
    praia.regiao == "Algarve" -> "S"
    else -> "W"
}

fun relacaoVento(direcao: String?, orientacao: String?): String? {
    if (direcao == null || orientacao == null) return null

    return when (orientacao) {
        "W" -> when (direcao.uppercase()) {
            "E", "NE", "SE" -> "terral"
            "W", "NW", "SW" -> "do mar"
            "N", "S" -> "lateral"
            else -> null
        }
        "S" -> when (direcao.uppercase()) {
            "N", "NE", "NW" -> "terral"
            "S", "SE", "SW" -> "do mar"
            "E", "W" -> "lateral"
            else -> null
        }
        else -> null
    }
}

fun calcularNota(
    perfil: Perfil,
    regras: Regras,
    praia: Praia,
    temperaturaMaxima: Double,
    probabilidadeChuva: Double,
    classeVento: Int,
    direcaoVento: String?,
    alturaMaximaMar: Double?,
    temperaturaMaximaAgua: Double?
): Int? {
    val relacao = relacaoVento(direcaoVento, orientacaoPraia(praia))

    val penalizacao = when (perfil) {
        Perfil.APANHAR_SOL -> penalizacaoSol(
            temperaturaMaxima, probabilidadeChuva, classeVento,
            alturaMaximaMar, temperaturaMaximaAgua
        )
        Perfil.SURF -> {
            if (praia.tipo == "fluvial" || alturaMaximaMar == null) return null
            penalizacaoSurf(
                temperaturaMaxima, probabilidadeChuva, classeVento, relacao,
                alturaMaximaMar, temperaturaMaximaAgua
            )
        }
        Perfil.KITE -> penalizacaoKite(
            temperaturaMaxima, probabilidadeChuva, classeVento, relacao,
            alturaMaximaMar, temperaturaMaximaAgua
        )
        Perfil.PERSONALIZADO -> penalizacaoPersonalizada(
            regras, temperaturaMaxima, probabilidadeChuva, classeVento,
            alturaMaximaMar, temperaturaMaximaAgua
        )
    }

    return (10 - penalizacao).coerceIn(0, 10)
}

private fun penalizacaoSol(
    temperatura: Double,
    chuva: Double,
    vento: Int,
    mar: Double?,
    agua: Double?
): Int {
    var total = 0

    total += when {
        temperatura >= 25 -> 0
        temperatura >= 18 -> 2
        else -> 4
    }
    total += when {
        chuva <= 15 -> 0
        chuva <= 30 -> 1
        chuva <= 60 -> 3
        else -> 5
    }
    total += when {
        vento >= 4 -> 3
        vento == 3 -> 2
        else -> 0
    }
    if (mar != null) {
        total += when {
            mar > 2.5 -> 3
            mar >= 1.5 -> 2
            else -> 0
        }
    }
    if (agua != null) {
        total += when {
            agua < 10 -> 3
            agua < 15 -> 2
            agua < 20 -> 1
            else -> 0
        }
    }

    return total
}

private fun penalizacaoSurf(
    temperatura: Double,
    chuva: Double,
    vento: Int,
    relacao: String?,
    mar: Double,
    agua: Double?
): Int {
    var total = 0

    total += when {
        mar < 0.5 -> 5
        mar < 1.0 -> 2
        mar <= 2.5 -> 0
        mar <= 3.5 -> 2
        else -> 5
    }
    total += when (vento) {
        1 -> 0
        2 -> 1
        3 -> 3
        else -> 5
    }
    total += when (relacao) {
        "do mar" -> 2
        "lateral" -> 1
        else -> 0
    }
    total += when {
        chuva <= 30 -> 0
        chuva <= 60 -> 1
        else -> 3
    }
    if (temperatura < 18) total += 1
    if (agua != null && agua < 15) total += 1

    return total
}

private fun penalizacaoKite(
    temperatura: Double,
    chuva: Double,
    vento: Int,
    relacao: String?,
    mar: Double?,
    agua: Double?
): Int {
    var total = 0

    total += when (vento) {
        1 -> 5
        2 -> 0
        3 -> 1
        else -> 5
    }
    total += when (relacao) {
        "terral" -> 4
        "do mar" -> 1
        else -> 0
    }
    if (mar != null) {
        total += when {
            mar <= 2.5 -> 0
            mar <= 3.5 -> 2
            else -> 4
        }
    }
    total += when {
        chuva <= 30 -> 0
        chuva <= 60 -> 1
        else -> 3
    }
    if (temperatura < 15) total += 1
    if (agua != null && agua < 15) total += 1

    return total
}

private fun penalizacaoPersonalizada(
    regras: Regras,
    temperatura: Double,
    chuva: Double,
    vento: Int,
    mar: Double?,
    agua: Double?
): Int {
    var total = 0

    total += when {
        temperatura >= regras.temperaturaMinima -> 0
        temperatura >= regras.temperaturaMinima - 5 -> 2
        else -> 4
    }
    total += when {
        chuva <= regras.chuvaMaxima -> 0
        chuva <= regras.chuvaMaxima + 30 -> 2
        else -> 5
    }
    if (vento > regras.ventoMaximo) {
        total += 2 * (vento - regras.ventoMaximo)
    }
    if (mar != null) {
        total += when {
            mar <= regras.marMaximo -> 0
            mar <= regras.marMaximo + 1 -> 2
            else -> 3
        }
    }
    if (agua != null) {
        total += when {
            agua >= regras.aguaMinima -> 0
            agua >= regras.aguaMinima - 5 -> 1
            else -> 3
        }
    }

    return total
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

fun avisoAtivoEm(aviso: AvisoIpma, dia: String): Boolean {
    val inicio = aviso.startTime?.take(10) ?: dia
    val fim = aviso.endTime?.take(10) ?: dia
    return inicio <= dia && fim >= dia
}

fun marcarAvisos(
    lista: List<PraiaRanking>,
    avisos: List<AvisoIpma>,
    dia: String
): List<PraiaRanking> {
    val areasGraves = avisos
        .filter { it.awarenessLevelID == "orange" || it.awarenessLevelID == "red" }
        .filter { avisoAtivoEm(it, dia) }
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
