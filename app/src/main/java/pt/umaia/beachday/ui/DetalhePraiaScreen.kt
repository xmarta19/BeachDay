package pt.umaia.beachday.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.data.AvisoIpma
import pt.umaia.beachday.data.DiaPrevisao
import pt.umaia.beachday.data.Perfil
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
    val abrirLink = LocalUriHandler.current
    val coordenadas = "${praia.latitude},${praia.longitude}"

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                abrirLink.openUri(
                    "https://www.google.com/maps/dir/?api=1&destination=$coordenadas"
                )
            }
        ) {
            Text("Como chegar")
        }
        OutlinedButton(
            onClick = {
                abrirLink.openUri(
                    "https://www.google.com/maps/search/?api=1&query=$coordenadas"
                )
            }
        ) {
            Text("Ver no mapa")
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
    val regras by viewModel.regras.collectAsState()

    val dias by remember(praia, perfil, regras) {
        viewModel.previsao(praia, perfil, regras)
    }.collectAsState(initial = emptyList())

    LaunchedEffect(praia.idLocalTempo, praia.idLocalMar) {
        viewModel.atualizarPrevisao(praia)
    }

    Text(
        text = "Notas para o perfil ${perfil.nome}",
        style = MaterialTheme.typography.titleMedium
    )

    if (perfil == Perfil.SURF && praia.tipo == "fluvial") {
        Text("O perfil Surf e bodyboard não se aplica a praias fluviais.")
    } else if (praia.tipo == "fluvial") {
        Text(
            text = "Sem dados do mar: a nota usa só o tempo.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (dias.isEmpty()) {
        Text("A carregar a previsão...")
    } else {
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
            Text("Temperatura máxima: ${dia.temperaturaMaxima} °C")
            Text("Probabilidade de chuva: ${dia.probabilidadeChuva}%")

            val direcao = dia.direcaoVento?.let { " de $it" } ?: ""
            val relacao = dia.relacaoVento?.let { " ($it)" } ?: ""
            Text("Vento: ${descricaoVento(dia.classeVento)}$direcao$relacao")

            dia.alturaMaximaMar?.let { Text("Altura máxima do mar: $it m") }
            dia.temperaturaMaximaAgua?.let { Text("Temperatura da água: $it °C") }
        }
    }
}
