package pt.umaia.beachday.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import pt.umaia.beachday.data.AvisoIpma
import pt.umaia.beachday.data.Condicao
import pt.umaia.beachday.data.DiaPrevisao
import pt.umaia.beachday.data.NOME_PERFIL_PREDEFINIDO
import pt.umaia.beachday.data.perfilPrecisaDeMar
import pt.umaia.beachday.data.Praia
import pt.umaia.beachday.data.avisoAtivoEm
import pt.umaia.beachday.data.descricaoVento

@Composable
fun DetalhePraiaScreen(
    idPraia: Int,
    viewModel: PraiasViewModel,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val praia by remember(idPraia) { viewModel.praia(idPraia) }
        .collectAsState(initial = null)
    val avisos by viewModel.avisos.collectAsState()
    val dia by viewModel.dia.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onVoltar) {
            Text("← Voltar")
        }

        val atual = praia
        if (atual == null) {
            Text("A carregar...")
        } else {
            Text(atual.nome, style = MaterialTheme.typography.headlineMedium)
            Text("${atual.regiao} · ${atual.tipo}")
            Text("Local IPMA: ${atual.nomeLocalTempo ?: "A associar..."}")
            Text("Zona do mar: ${atual.nomeLocalMar ?: "Não aplicável"}")
            avisos
                .filter { it.idAreaAviso == atual.idAreaAviso && avisoAtivoEm(it, dia) }
                .forEach { aviso -> CartaoAviso(aviso) }
            BotoesMapa(atual)
            AcoesPraia(atual, viewModel)
            PrevisaoPraia(atual, viewModel)
            Text(
                text = "Fonte: IPMA. Dados regionais, não medidos nesta praia.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BotoesMapa(praia: Praia) {
    val context = LocalContext.current
    val coordenadas = "${praia.latitude},${praia.longitude}"
    val nome = Uri.encode(praia.nome)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                abrirMapa(
                    context,
                    "https://www.google.com/maps/dir/?api=1&destination=$coordenadas",
                    null
                )
            }
        ) {
            Text("Como chegar")
        }
        OutlinedButton(
            onClick = {
                abrirMapa(
                    context,
                    "geo:$coordenadas?q=$coordenadas($nome)",
                    "https://www.google.com/maps/search/?api=1&query=$coordenadas"
                )
            }
        ) {
            Text("Ver no mapa")
        }
    }
}

private fun abrirMapa(context: Context, endereco: String, alternativa: String?) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, endereco.toUri()))
    } catch (erro: ActivityNotFoundException) {
        Log.w("BeachDay", "Nenhuma app abriu o mapa", erro)
        if (alternativa != null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, alternativa.toUri()))
        }
    }
}

@Composable
private fun CartaoAviso(aviso: AvisoIpma) {
    val grave = aviso.awarenessLevelID == "orange" || aviso.awarenessLevelID == "red"
    val nivel = when (aviso.awarenessLevelID) {
        "red" -> "vermelho"
        "orange" -> "laranja"
        else -> "amarelo"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (grave) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "⚠ Aviso $nivel: ${aviso.awarenessTypeName ?: "IPMA"}",
                style = MaterialTheme.typography.titleSmall
            )
            aviso.text?.takeIf { it.isNotBlank() }?.let { Text(it) }
        }
    }
}

@Composable
private fun AcoesPraia(praia: Praia, viewModel: PraiasViewModel) {
    var aRegistar by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { viewModel.definirFavorita(praia.id, !praia.favorita) }
        ) {
            Text(if (praia.favorita) "★ Favorita" else "☆ Favorita")
        }
        OutlinedButton(onClick = { aRegistar = true }) {
            Text("Registar visita")
        }
    }

    if (aRegistar) {
        DialogoVisita(
            nomePraia = praia.nome,
            onCancelar = { aRegistar = false },
            onGuardar = { nota, comentario ->
                viewModel.registarVisita(praia.id, nota, comentario)
                aRegistar = false
            }
        )
    }
}

@Composable
private fun DialogoVisita(
    nomePraia: String,
    onCancelar: () -> Unit,
    onGuardar: (Int, String) -> Unit
) {
    var nota by remember { mutableIntStateOf(5) }
    var comentario by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Visita a $nomePraia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nota da visita")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { valor ->
                        FilterChip(
                            selected = nota == valor,
                            onClick = { nota = valor },
                            label = { Text("$valor") }
                        )
                    }
                }
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text("Comentário (opcional)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(nota, comentario) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PrevisaoPraia(praia: Praia, viewModel: PraiasViewModel) {
    val perfil by viewModel.perfil.collectAsState()

    val dias by remember(praia, perfil) {
        viewModel.previsao(praia, perfil)
    }.collectAsState(initial = emptyList())

    var aCarregar by remember(praia.id) { mutableStateOf(true) }
    var erro by remember(praia.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(praia.idLocalTempo, praia.idLocalMar) {
        aCarregar = true
        erro = viewModel.carregarPrevisao(praia)
        aCarregar = false
    }

    val nomePerfil = perfil?.nome ?: NOME_PERFIL_PREDEFINIDO

    Text(
        text = "Notas para o perfil $nomePerfil",
        style = MaterialTheme.typography.titleMedium
    )

    if (perfilPrecisaDeMar(perfil) && praia.tipo == "fluvial") {
        Text("O perfil $nomePerfil pede ondas, por isso não se aplica a praias fluviais.")
    } else if (praia.tipo == "fluvial") {
        Text(
            text = "Sem dados do mar: a nota usa só o tempo.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (dias.isEmpty()) {
        Text(
            when {
                aCarregar -> "A carregar a previsão..."
                erro != null -> "$erro Tenta outra vez mais tarde."
                else -> "Sem previsão disponível para esta praia."
            }
        )
    } else {
        erro?.let {
            Text(
                text = "Não foi possível atualizar. A mostrar os dados guardados.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        val notas = dias.mapNotNull { it.nota }
        val melhorNota = notas.maxOrNull()
        val haEmpate = melhorNota != null && notas.count { it == melhorNota } > 1

        dias.forEach { dia ->
            val etiqueta = when {
                melhorNota == null || dia.nota != melhorNota -> null
                haEmpate -> "Empatado"
                else -> "Melhor dia"
            }
            CartaoDia(dia, etiqueta)
        }
    }
}

@Composable
private fun CartaoDia(dia: DiaPrevisao, etiqueta: String?) {
    fun pontos(condicao: Condicao): Int =
        dia.penalizacoes.filter { it.condicao == condicao }.sumOf { it.pontos }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${dataPorExtenso(dia.dia)}${etiqueta?.let { " · $it" } ?: ""}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Nota: ${dia.nota?.let { "$it/10" } ?: "—"}",
                style = MaterialTheme.typography.headlineSmall
            )

            LinhaCondicao(
                "Temperatura máxima: ${dia.temperaturaMaxima} °C",
                pontos(Condicao.TEMPERATURA)
            )
            LinhaCondicao(
                "Probabilidade de chuva: ${dia.probabilidadeChuva}%",
                pontos(Condicao.CHUVA)
            )

            val direcao = dia.direcaoVento?.let { " de $it" } ?: ""
            val relacao = dia.relacaoVento?.let { " ($it)" } ?: ""
            LinhaCondicao(
                "Vento: ${descricaoVento(dia.classeVento)}$direcao$relacao",
                pontos(Condicao.VENTO)
            )

            dia.alturaMaximaMar?.let {
                LinhaCondicao("Altura máxima do mar: $it m", pontos(Condicao.MAR))
            }
            dia.temperaturaMaximaAgua?.let {
                LinhaCondicao("Temperatura da água: $it °C", pontos(Condicao.AGUA))
            }
        }
    }
}

@Composable
private fun LinhaCondicao(texto: String, pontos: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, modifier = Modifier.weight(1f))
        if (pontos > 0) {
            Text(
                text = "-$pontos",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
