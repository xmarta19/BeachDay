package pt.umaia.beachday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.data.NOME_PERFIL_PREDEFINIDO
import pt.umaia.beachday.data.PerfilUtilizador
import pt.umaia.beachday.data.descricaoVento
import pt.umaia.beachday.ui.theme.BeachDay
import pt.umaia.beachday.ui.theme.NotaBoa
import java.util.Locale
import kotlin.math.roundToInt

private val etiquetasPredefinido = listOf(
    "25 °C ou mais", "Chuva até 15%", "Vento até moderado", "Mar até 1,5 m", "Água 20 °C+"
)

@Composable
fun PerfilScreen(
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val perfis by viewModel.perfis.collectAsState()
    val idPerfil by viewModel.idPerfil.collectAsState()
    val cores = BeachDay.cores

    var aEditar by remember { mutableStateOf<PerfilUtilizador?>(null) }
    var aApagar by remember { mutableStateOf<PerfilUtilizador?>(null) }

    val editado = aEditar
    if (editado != null) {
        EditorPerfil(
            inicial = editado,
            onGuardar = {
                viewModel.guardarPerfil(it)
                aEditar = null
            },
            onCancelar = { aEditar = null },
            modifier = modifier
        )
        return
    }

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            Text("Perfil", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "O que vais fazer à praia? O perfil escolhido decide como a nota é calculada.",
                style = MaterialTheme.typography.bodyMedium,
                color = cores.topoTextoSecundario
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingFolha),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val predefinidoEscolhido = idPerfil == 0 || perfis.none { it.id == idPerfil }

            CartaoPerfil(
                nome = NOME_PERFIL_PREDEFINIDO,
                subtitulo = if (predefinidoEscolhido) "Predefinido · em uso" else "Predefinido",
                etiquetas = etiquetasPredefinido.map { it to false },
                icone = Icons.Outlined.WbSunny,
                escolhido = predefinidoEscolhido,
                onEscolher = { viewModel.escolherPerfil(0) }
            )

            perfis.forEach { perfil ->
                val escolhido = perfil.id == idPerfil
                CartaoPerfil(
                    nome = perfil.nome,
                    subtitulo = if (escolhido) "Criado por ti · em uso" else "Criado por ti",
                    etiquetas = etiquetas(perfil),
                    icone = if (perfil.marMinimo != null) Icons.Outlined.Waves else Icons.Outlined.Tune,
                    escolhido = escolhido,
                    onEscolher = { viewModel.escolherPerfil(perfil.id) },
                    onEditar = { aEditar = perfil },
                    onApagar = { aApagar = perfil }
                )
            }

            BotaoTracejado(
                texto = "Criar perfil",
                icone = Icons.Outlined.Add,
                onClick = { aEditar = PerfilUtilizador(nome = "") }
            )
        }
    }

    val paraApagar = aApagar
    if (paraApagar != null) {
        AlertDialog(
            onDismissRequest = { aApagar = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Apagar perfil") },
            text = { Text("Queres apagar o perfil \"${paraApagar.nome}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.apagarPerfil(paraApagar)
                        aApagar = null
                    }
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                TextButton(onClick = { aApagar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoPerfil(
    nome: String,
    subtitulo: String,
    etiquetas: List<Pair<String, Boolean>>,
    icone: ImageVector,
    escolhido: Boolean,
    onEscolher: () -> Unit,
    onEditar: (() -> Unit)? = null,
    onApagar: (() -> Unit)? = null
) {
    val cores = BeachDay.cores
    val forma = RoundedCornerShape(22.dp)
    val borda = if (escolhido) {
        Modifier.border(2.dp, Brush.linearGradient(NotaBoa), forma)
    } else {
        Modifier.border(1.dp, cores.linha, forma)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(cores.cartao)
            .then(borda)
            .selectable(selected = escolhido, role = Role.RadioButton, onClick = onEscolher)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (escolhido) Modifier.background(Brush.linearGradient(NotaBoa))
                        else Modifier.background(cores.azul.copy(alpha = 0.14f))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = if (escolhido) Color.White else cores.azul,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(nome, style = MaterialTheme.typography.titleMedium, color = cores.texto)
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = cores.textoSecundario)
            }
            if (onEditar != null) {
                BotaoRedondo(
                    icone = Icons.Outlined.Edit,
                    descricao = "Editar $nome",
                    onClick = onEditar,
                    cor = cores.texto,
                    fundo = cores.chip,
                    tamanho = 36.dp
                )
            }
            if (onApagar != null) {
                BotaoRedondo(
                    icone = Icons.Outlined.DeleteOutline,
                    descricao = "Apagar $nome",
                    onClick = onApagar,
                    cor = cores.texto,
                    fundo = cores.chip,
                    tamanho = 36.dp
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            etiquetas.forEach { (texto, realce) ->
                if (realce) {
                    Etiqueta(texto, fundo = cores.azul.copy(alpha = 0.14f), cor = cores.azul)
                } else {
                    Etiqueta(texto)
                }
            }
        }
    }
}

@Composable
private fun EditorPerfil(
    inicial: PerfilUtilizador,
    onGuardar: (PerfilUtilizador) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var perfil by remember { mutableStateOf(inicial) }
    val cores = BeachDay.cores

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotaoRedondo(Icons.AutoMirrored.Outlined.ArrowBack, "Cancelar", onCancelar)
                Column {
                    Text(
                        text = if (inicial.id == 0) "Novo perfil" else "Editar perfil",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Fora destes limites, a praia perde pontos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.topoTextoSecundario
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(PaddingFolha),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = perfil.nome,
                onValueChange = { perfil = perfil.copy(nome = it) },
                label = { Text("Nome do perfil, por exemplo Surf") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Cartao {
                LinhaLimite("Temperatura mínima do ar", "${perfil.temperaturaMinima.roundToInt()} °C") {
                    Slider(
                        value = perfil.temperaturaMinima.toFloat(),
                        onValueChange = { perfil = perfil.copy(temperaturaMinima = it.roundToInt().toDouble()) },
                        valueRange = 15f..32f,
                        steps = 16
                    )
                }
                LinhaLimite("Probabilidade máxima de chuva", "${perfil.chuvaMaxima.roundToInt()}%") {
                    Slider(
                        value = perfil.chuvaMaxima.toFloat(),
                        onValueChange = { perfil = perfil.copy(chuvaMaxima = (it / 5).roundToInt() * 5.0) },
                        valueRange = 0f..100f,
                        steps = 19
                    )
                }
                LinhaLimite("Vento máximo", descricaoVento(perfil.ventoMaximo)) {
                    Slider(
                        value = perfil.ventoMaximo.toFloat(),
                        onValueChange = { perfil = perfil.copy(ventoMaximo = it.roundToInt()) },
                        valueRange = 1f..4f,
                        steps = 2
                    )
                }
            }

            Cartao {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Quero ondas", style = MaterialTheme.typography.titleSmall, color = cores.texto)
                        Text(
                            text = "Define uma altura mínima do mar. Exclui as praias fluviais.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.textoSecundario
                        )
                    }
                    Switch(
                        checked = perfil.marMinimo != null,
                        onCheckedChange = { ligado ->
                            perfil = perfil.copy(marMinimo = if (ligado) 1.0 else null)
                        }
                    )
                }

                perfil.marMinimo?.let { minimo ->
                    LinhaLimite("Altura mínima do mar", "${decimal(minimo)} m") {
                        Slider(
                            value = minimo.toFloat(),
                            onValueChange = {
                                val valor = (it * 2).roundToInt() / 2.0
                                perfil = perfil.copy(
                                    marMinimo = valor,
                                    marMaximo = maxOf(perfil.marMaximo, valor)
                                )
                            },
                            valueRange = 0.5f..3f,
                            steps = 4
                        )
                    }
                }

                LinhaLimite("Altura máxima do mar", "${decimal(perfil.marMaximo)} m") {
                    Slider(
                        value = perfil.marMaximo.toFloat(),
                        onValueChange = {
                            val valor = (it * 2).roundToInt() / 2.0
                            perfil = perfil.copy(
                                marMaximo = valor,
                                marMinimo = perfil.marMinimo?.let { minimo -> minOf(minimo, valor) }
                            )
                        },
                        valueRange = 0.5f..5f,
                        steps = 8
                    )
                }

                LinhaLimite("Temperatura mínima da água", "${perfil.aguaMinima.roundToInt()} °C") {
                    Slider(
                        value = perfil.aguaMinima.toFloat(),
                        onValueChange = { perfil = perfil.copy(aguaMinima = it.roundToInt().toDouble()) },
                        valueRange = 10f..24f,
                        steps = 13
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotaoPrincipal(
                    texto = "Guardar",
                    onClick = { onGuardar(perfil.copy(nome = perfil.nome.trim())) },
                    ativo = perfil.nome.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                BotaoSecundario(
                    texto = "Cancelar",
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LinhaLimite(rotulo: String, valor: String, controlo: @Composable () -> Unit) {
    val cores = BeachDay.cores

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.bodyMedium,
                color = cores.textoSecundario,
                modifier = Modifier.weight(1f)
            )
            Text(valor, style = MaterialTheme.typography.titleSmall, color = cores.texto)
        }
        controlo()
    }
}

private fun etiquetas(perfil: PerfilUtilizador): List<Pair<String, Boolean>> = buildList {
    add("${perfil.temperaturaMinima.roundToInt()} °C ou mais" to false)
    add("Chuva até ${perfil.chuvaMaxima.roundToInt()}%" to false)
    add("Vento até ${descricaoVento(perfil.ventoMaximo).lowercase()}" to false)
    perfil.marMinimo?.let { add("Quero ondas: ${decimal(it)} m+" to true) }
    add("Mar até ${decimal(perfil.marMaximo)} m" to false)
    add("Água ${perfil.aguaMinima.roundToInt()} °C+" to false)
}

private fun decimal(valor: Double): String =
    String.format(Locale.forLanguageTag("pt-PT"), "%.1f", valor)
