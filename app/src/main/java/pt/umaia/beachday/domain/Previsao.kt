package pt.umaia.beachday.domain

import java.util.Locale
import pt.umaia.beachday.data.local.PerfilUtilizador
import pt.umaia.beachday.data.local.Praia
import pt.umaia.beachday.data.remote.AvisoIpma

const val NOME_PERFIL_PREDEFINIDO = "Apanhar sol"

enum class Condicao { TEMPERATURA, CHUVA, VENTO, MAR, AGUA }

data class Penalizacao(
    val condicao: Condicao,
    val motivo: String,
    val pontos: Int
)

data class Avaliacao(
    val nota: Int,
    val penalizacoes: List<Penalizacao>
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
    val nota: Int?,
    val penalizacoes: List<Penalizacao>
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
    perfil: PerfilUtilizador?,
    praia: Praia,
    temperaturaMaxima: Double,
    probabilidadeChuva: Double,
    classeVento: Int,
    alturaMaximaMar: Double?,
    temperaturaMaximaAgua: Double?
): Int? = avaliar(
    perfil, praia, temperaturaMaxima, probabilidadeChuva,
    classeVento, alturaMaximaMar, temperaturaMaximaAgua
)?.nota

fun perfilPrecisaDeMar(perfil: PerfilUtilizador?): Boolean = perfil?.marMinimo != null

fun avaliar(
    perfil: PerfilUtilizador?,
    praia: Praia,
    temperaturaMaxima: Double,
    probabilidadeChuva: Double,
    classeVento: Int,
    alturaMaximaMar: Double?,
    temperaturaMaximaAgua: Double?
): Avaliacao? {
    if (perfilPrecisaDeMar(perfil) && (praia.tipo == "fluvial" || alturaMaximaMar == null)) {
        return null
    }

    val lista = mutableListOf<Penalizacao>()

    if (perfil == null) {
        lista.penalizacoesSol(
            temperaturaMaxima, probabilidadeChuva, classeVento,
            alturaMaximaMar, temperaturaMaximaAgua
        )
    } else {
        lista.penalizacoesPersonalizadas(
            perfil, temperaturaMaxima, probabilidadeChuva, classeVento,
            alturaMaximaMar, temperaturaMaximaAgua
        )
    }

    val nota = (10 - lista.sumOf { it.pontos }).coerceIn(0, 10)
    return Avaliacao(nota, lista)
}

private fun MutableList<Penalizacao>.tirar(pontos: Int, condicao: Condicao, motivo: String) {
    if (pontos > 0) add(Penalizacao(condicao, motivo, pontos))
}

private fun decimal(valor: Double): String =
    String.format(Locale.forLanguageTag("pt-PT"), "%.1f", valor)

private fun inteiro(valor: Double): String =
    String.format(Locale.forLanguageTag("pt-PT"), "%.0f", valor)

private fun MutableList<Penalizacao>.penalizacoesSol(
    temperatura: Double,
    chuva: Double,
    vento: Int,
    mar: Double?,
    agua: Double?
) {
    tirar(
        when {
            temperatura >= 25 -> 0
            temperatura >= 18 -> 2
            else -> 4
        },
        Condicao.TEMPERATURA, "Temperatura de ${decimal(temperatura)} °C"
    )
    tirar(
        when {
            chuva <= 15 -> 0
            chuva <= 30 -> 1
            chuva <= 60 -> 3
            else -> 5
        },
        Condicao.CHUVA, "Chuva de ${inteiro(chuva)}%"
    )
    tirar(
        when {
            vento >= 4 -> 3
            vento == 3 -> 2
            else -> 0
        },
        Condicao.VENTO, "Vento ${descricaoVento(vento).lowercase()}"
    )
    if (mar != null) {
        tirar(
            when {
                mar > 2.5 -> 3
                mar >= 1.5 -> 2
                else -> 0
            },
            Condicao.MAR, "Mar de ${decimal(mar)} m"
        )
    }
    if (agua != null) {
        tirar(
            when {
                agua < 10 -> 3
                agua < 15 -> 2
                agua < 20 -> 1
                else -> 0
            },
            Condicao.AGUA, "Água a ${decimal(agua)} °C"
        )
    }
}

private fun MutableList<Penalizacao>.penalizacoesPersonalizadas(
    limites: PerfilUtilizador,
    temperatura: Double,
    chuva: Double,
    vento: Int,
    mar: Double?,
    agua: Double?
) {
    tirar(
        when {
            temperatura >= limites.temperaturaMinima -> 0
            temperatura >= limites.temperaturaMinima - 5 -> 2
            else -> 4
        },
        Condicao.TEMPERATURA, "Temperatura de ${decimal(temperatura)} °C"
    )
    tirar(
        when {
            chuva <= limites.chuvaMaxima -> 0
            chuva <= limites.chuvaMaxima + 30 -> 2
            else -> 5
        },
        Condicao.CHUVA, "Chuva de ${inteiro(chuva)}%"
    )
    tirar(
        if (vento > limites.ventoMaximo) 2 * (vento - limites.ventoMaximo) else 0,
        Condicao.VENTO, "Vento ${descricaoVento(vento).lowercase()}"
    )
    if (mar != null && limites.marMinimo != null) {
        tirar(
            when {
                mar >= limites.marMinimo -> 0
                mar >= limites.marMinimo - 0.5 -> 2
                else -> 5
            },
            Condicao.MAR, "Mar de ${decimal(mar)} m (pouca onda)"
        )
    }
    if (mar != null) {
        tirar(
            when {
                mar <= limites.marMaximo -> 0
                mar <= limites.marMaximo + 1 -> 2
                else -> 3
            },
            Condicao.MAR, "Mar de ${decimal(mar)} m"
        )
    }
    if (agua != null) {
        tirar(
            when {
                agua >= limites.aguaMinima -> 0
                agua >= limites.aguaMinima - 5 -> 1
                else -> 3
            },
            Condicao.AGUA, "Água a ${decimal(agua)} °C"
        )
    }
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
