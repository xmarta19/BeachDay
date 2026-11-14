package pt.umaia.beachday.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cores que não mudam entre o tema claro e o escuro
val Destaque = Color(0xFFE2636D)
val TopoAzul = Color(0xFF232C4D)
val TopoTexto = Color(0xFFF2F4FA)
val TopoTextoSecundario = Color(0xFFAAB2D4)
val TopoChip = Color(0x14FFFFFF)
val SegmentoAtivo = Color(0xFF2D3870)

// Degradês das notas: boa (7 a 10), média (4 a 6) e má (0 a 3)
val NotaBoa = listOf(Color(0xFF43B892), Color(0xFF3B90AE))
val NotaMedia = listOf(Color(0xFFE4A84E), Color(0xFFD9804F))
val NotaMa = listOf(Color(0xFFD85E6E), Color(0xFFA8508A))
val NotaSemDados = listOf(Color(0xFF8D96B8), Color(0xFF6B7494))

// Cores dos ícones das condições
val CorTemperatura = Color(0xFFE0875A)
val CorChuva = Color(0xFF6A8DDB)
val CorVento = Color(0xFF958AD6)
val CorMar = Color(0xFF3E98B6)
val CorAgua = Color(0xFF3DB58C)
val CorEstrela = Color(0xFFE5B04E)

@Immutable
data class CoresBeachDay(
    val escuro: Boolean,
    val fundo: Color,
    val cartao: Color,
    val texto: Color,
    val textoSecundario: Color,
    val bloco: Color,
    val linha: Color,
    val linhaForte: Color,
    val chip: Color,
    val chipTexto: Color,
    val negativo: Color,
    val positivo: Color,
    val azul: Color,
    val segmentoFundo: Color,
    val destaque: Color = Destaque,
    val topo: Color = TopoAzul,
    val topoTexto: Color = TopoTexto,
    val topoTextoSecundario: Color = TopoTextoSecundario,
    val topoChip: Color = TopoChip,
    val segmentoAtivo: Color = SegmentoAtivo
)

val CoresClaras = CoresBeachDay(
    escuro = false,
    fundo = Color(0xFFF3F5FA),
    cartao = Color(0xFFFFFFFF),
    texto = Color(0xFF121833),
    textoSecundario = Color(0xFF6B7494),
    bloco = Color(0xFFEEF1F8),
    linha = Color(0xFFE6EAF2),
    linhaForte = Color(0xFFD9DFEB),
    chip = Color(0xFFEEF1F8),
    chipTexto = Color(0xFF3B4566),
    negativo = Color(0xFFE0435A),
    positivo = Color(0xFF0F9A72),
    azul = Color(0xFF3D6BE0),
    segmentoFundo = Color(0xFF0F1530)
)

val CoresEscuras = CoresBeachDay(
    escuro = true,
    fundo = Color(0xFF10152A),
    cartao = Color(0xFF1A2140),
    texto = Color(0xFFF2F4FA),
    textoSecundario = Color(0xFF8D96B8),
    bloco = Color(0xFF141A33),
    linha = Color(0x0FFFFFFF),
    linhaForte = Color(0x1FFFFFFF),
    chip = Color(0x14FFFFFF),
    chipTexto = Color(0xFFC9D0EA),
    negativo = Color(0xFFE8848F),
    positivo = Color(0xFF3DB58C),
    azul = Color(0xFF9DB8FF),
    segmentoFundo = Color(0xFF161C36)
)

fun coresNota(nota: Int?): List<Color> = when {
    nota == null -> NotaSemDados
    nota >= 7 -> NotaBoa
    nota >= 4 -> NotaMedia
    else -> NotaMa
}

fun pincelNota(nota: Int?): Brush = Brush.linearGradient(coresNota(nota))

fun textoNota(nota: Int?): String = when {
    nota == null -> "Sem dados"
    nota >= 7 -> "Bom dia de praia"
    nota >= 4 -> "Dia razoável"
    else -> "Dia fraco"
}
