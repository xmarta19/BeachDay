package pt.umaia.beachday.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.umaia.beachday.data.PraiaRanking
import pt.umaia.beachday.ui.theme.BeachDay
import pt.umaia.beachday.ui.theme.EstiloNumero
import pt.umaia.beachday.ui.theme.coresNota
import pt.umaia.beachday.ui.theme.pincelNota
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val PaddingFolha = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)

private val Portugues: Locale = Locale.forLanguageTag("pt-PT")

/**
 * Estrutura comum a todos os ecrãs: topo azul-escuro em cima
 * e uma "folha" com o canto superior esquerdo arredondado por baixo.
 */
@Composable
fun EcraBeachDay(
    cabecalho: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    val cores = BeachDay.cores

    Column(
        modifier
            .fillMaxSize()
            .background(cores.topo)
    ) {
        CompositionLocalProvider(LocalContentColor provides cores.topoTexto) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = cabecalho
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 44.dp))
                .background(cores.fundo),
            content = conteudo
        )
    }
}

@Composable
fun BotaoRedondo(
    icone: ImageVector,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ativo: Boolean = true,
    cor: Color = LocalContentColor.current,
    fundo: Color = BeachDay.cores.topoChip,
    tamanho: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(fundo)
            .clickable(enabled = ativo, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icone,
            contentDescription = descricao,
            tint = if (ativo) cor else cor.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BotaoAtualizar(
    aAtualizar: Boolean,
    ativo: Boolean,
    acabouDeAtualizar: Boolean,
    onClick: () -> Unit
) {
    if (aAtualizar) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(BeachDay.cores.topoChip),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
        }
    } else {
        BotaoRedondo(
            icone = if (acabouDeAtualizar) Icons.Outlined.Check else Icons.Outlined.Refresh,
            descricao = if (acabouDeAtualizar) "Atualizado agora" else "Atualizar previsão",
            onClick = onClick,
            ativo = ativo
        )
    }
}

@Composable
fun Segmentado(
    opcoes: List<String>,
    escolhida: Int,
    onEscolher: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = BeachDay.cores

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cores.segmentoFundo)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        opcoes.forEachIndexed { indice, texto ->
            val ativa = indice == escolhida
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (ativa) cores.segmentoAtivo else Color.Transparent)
                    .selectable(
                        selected = ativa,
                        role = Role.Tab,
                        onClick = { onEscolher(indice) }
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = texto,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ativa) cores.topoTexto else cores.topoTextoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    preenchimento: PaddingValues = PaddingValues(16.dp),
    espaco: Dp = 12.dp,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    val cores = BeachDay.cores
    val forma = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(cores.cartao)
            .border(1.dp, cores.linha, forma)
            .padding(preenchimento),
        verticalArrangement = Arrangement.spacedBy(espaco),
        content = conteudo
    )
}

@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    fundo: Color = BeachDay.cores.chip,
    cor: Color = BeachDay.cores.chipTexto,
    icone: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(fundo)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(14.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = cor)
    }
}

@Composable
fun NotaQuadrado(nota: Int?, modifier: Modifier = Modifier, tamanho: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(RoundedCornerShape(14.dp))
            .background(pincelNota(nota)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nota?.toString() ?: "—",
            style = EstiloNumero.copy(fontSize = 18.sp),
            color = Color.White
        )
    }
}

@Composable
fun AnelNota(nota: Int?, modifier: Modifier = Modifier) {
    val cor = coresNota(nota).first()
    val cores = BeachDay.cores

    Box(modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val traco = 8.dp.toPx()
            val inicio = Offset(traco / 2, traco / 2)
            val area = Size(size.width - traco, size.height - traco)

            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = inicio,
                size = area,
                style = Stroke(traco)
            )
            if (nota != null && nota > 0) {
                drawArc(
                    color = cor,
                    startAngle = -90f,
                    sweepAngle = 360f * nota / 10f,
                    useCenter = false,
                    topLeft = inicio,
                    size = area,
                    style = Stroke(traco, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = nota?.toString() ?: "—",
                style = EstiloNumero.copy(fontSize = 30.sp, lineHeight = 32.sp),
                color = cores.topoTexto
            )
            Text("de 10", fontSize = 11.sp, color = cores.topoTextoSecundario)
        }
    }
}

@Composable
fun CartaoRanking(
    posicao: Int,
    item: PraiaRanking,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tipo = if (item.praia.tipo == "fluvial") " · Fluvial" else ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(pincelNota(item.nota))
            .clickable(onClick = onClick)
            .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "#$posicao · ${item.praia.regiao}$tipo",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = item.praia.nome,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.avisoGrave) {
                Etiqueta(
                    texto = "Aviso do IPMA",
                    fundo = Color.Black.copy(alpha = 0.2f),
                    cor = Color.White,
                    icone = Icons.Outlined.WarningAmber
                )
            }
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.nota?.toString() ?: "—",
                    style = EstiloNumero.copy(fontSize = 26.sp, lineHeight = 28.sp),
                    color = Color.White
                )
                if (item.nota != null) {
                    Text("/10", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

fun Modifier.bordaTracejada(cor: Color, raio: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = cor,
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
        ),
        cornerRadius = CornerRadius(raio.toPx())
    )
}

@Composable
fun BotaoTracejado(texto: String, icone: ImageVector, onClick: () -> Unit) {
    val cores = BeachDay.cores

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .bordaTracejada(cores.linhaForte, 18.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp), tint = cores.texto)
        Spacer(Modifier.size(8.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = cores.texto)
    }
}

@Composable
fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    ativo: Boolean = true,
    detalhe: String? = null
) {
    Button(
        onClick = onClick,
        enabled = ativo,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BeachDay.cores.destaque,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.height(52.dp)
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
        if (detalhe != null) {
            Text(
                text = " · $detalhe",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BotaoSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ativo: Boolean = true
) {
    val cores = BeachDay.cores

    OutlinedButton(
        onClick = onClick,
        enabled = ativo,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cores.linhaForte),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = cores.cartao,
            contentColor = cores.texto
        ),
        modifier = modifier.height(52.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

private data class Separador(
    val rota: String,
    val nome: String,
    val icone: ImageVector,
    val rotas: Set<String>
)

private val separadores = listOf(
    Separador("dias", "Praias", Icons.Outlined.Home, setOf("dias", "praias", "praia/{id}")),
    Separador("favoritas", "Favoritas", Icons.Outlined.StarBorder, setOf("favoritas")),
    Separador("historico", "Histórico", Icons.Outlined.History, setOf("historico")),
    Separador("perfil", "Perfil", Icons.Outlined.Tune, setOf("perfil")),
    Separador("conta", "Conta", Icons.Outlined.Person, setOf("conta"))
)

@Composable
fun NavegacaoInferior(rotaAtual: String?, onAbrir: (String) -> Unit) {
    val cores = BeachDay.cores
    val forma = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cores.fundo)
            .navigationBarsPadding()
            .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(forma)
                .background(cores.cartao)
                .border(1.dp, cores.linha, forma)
                .padding(8.dp)
        ) {
            separadores.forEach { separador ->
                val ativo = rotaAtual in separador.rotas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .selectable(
                            selected = ativo,
                            role = Role.Tab,
                            onClick = { onAbrir(separador.rota) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (ativo) cores.destaque else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = separador.icone,
                            contentDescription = null,
                            tint = if (ativo) Color.White else cores.textoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = separador.nome,
                        fontSize = 11.sp,
                        fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (ativo) cores.texto else cores.textoSecundario,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---------- Datas ----------

private fun converter(dia: String): Date? =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dia)

private fun formatar(dia: String, padrao: String): String =
    converter(dia)?.let { SimpleDateFormat(padrao, Portugues).format(it) } ?: dia

/** "sáb, 14 nov" */
fun dataCurta(dia: String): String = formatar(dia, "EEE, d MMM").replace(".", "")

/** "sáb" */
fun diaSemanaCurto(dia: String): String = formatar(dia, "EEE").replace(".", "")

/** "nov" */
fun mesCurto(dia: String): String = formatar(dia, "MMM").replace(".", "")

fun diaDoMes(dia: String): String = dia.takeLast(2).trimStart('0')

fun textoAtualizacao(instante: Long?): String {
    if (instante == null) return "Ainda sem dados guardados"
    val formato = SimpleDateFormat("dd/MM 'às' HH:mm", Portugues)
    return "Atualizado: ${formato.format(Date(instante))}"
}

fun String.maiuscula(): String = replaceFirstChar { it.titlecase(Portugues) }

fun numero(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toInt().toString()
    else String.format(Portugues, "%.1f", valor)
