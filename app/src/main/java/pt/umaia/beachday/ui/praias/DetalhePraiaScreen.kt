package pt.umaia.beachday.ui.praias

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import pt.umaia.beachday.data.local.Praia
import pt.umaia.beachday.data.remote.AvisoIpma
import pt.umaia.beachday.domain.Condicao
import pt.umaia.beachday.domain.DiaPrevisao
import pt.umaia.beachday.domain.NOME_PERFIL_PREDEFINIDO
import pt.umaia.beachday.domain.avisoAtivoEm
import pt.umaia.beachday.domain.descricaoVento
import pt.umaia.beachday.domain.perfilPrecisaDeMar
import pt.umaia.beachday.ui.comum.AnelNota
import pt.umaia.beachday.ui.comum.AvatarConta
import pt.umaia.beachday.ui.comum.Aviso
import pt.umaia.beachday.ui.comum.BotaoPrincipal
import pt.umaia.beachday.ui.comum.BotaoRedondo
import pt.umaia.beachday.ui.comum.Cartao
import pt.umaia.beachday.ui.comum.EcraBeachDay
import pt.umaia.beachday.ui.comum.Etiqueta
import pt.umaia.beachday.ui.comum.PaddingFolha
import pt.umaia.beachday.ui.comum.Segmentado
import pt.umaia.beachday.ui.comum.diaSemanaCurto
import pt.umaia.beachday.ui.comum.maiuscula
import pt.umaia.beachday.ui.comum.numero
import pt.umaia.beachday.ui.theme.BeachDay
import pt.umaia.beachday.ui.theme.CorAgua
import pt.umaia.beachday.ui.theme.CorChuva
import pt.umaia.beachday.ui.theme.CorEstrela
import pt.umaia.beachday.ui.theme.CorMar
import pt.umaia.beachday.ui.theme.CorTemperatura
import pt.umaia.beachday.ui.theme.CorVento
import pt.umaia.beachday.ui.theme.EstiloNumero
import pt.umaia.beachday.ui.theme.coresNota
import pt.umaia.beachday.ui.theme.textoNota

@Composable
fun DetalhePraiaScreen(
    idPraia: Int,
    viewModel: PraiasViewModel,
    onVoltar: () -> Unit,
    onAbrirConta: () -> Unit,
    modifier: Modifier = Modifier
) {
    val praia by remember(idPraia) { viewModel.praia(idPraia) }
        .collectAsState(initial = null)

    val atual = praia
    if (atual == null) {
        EcraBeachDay(
            modifier = modifier,
            cabecalho = { BotaoRedondo(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", onVoltar) }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    } else {
        DetalhePraia(atual, viewModel, onVoltar, onAbrirConta, modifier)
    }
}

@Composable
private fun DetalhePraia(
    praia: Praia,
    viewModel: PraiasViewModel,
    onVoltar: () -> Unit,
    onAbrirConta: () -> Unit,
    modifier: Modifier
) {
    val avisos by viewModel.avisos.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val diaGeral by viewModel.dia.collectAsState()
    val cores = BeachDay.cores

    var diaEscolhido by remember(praia.id) { mutableStateOf(diaGeral) }
    var aRegistar by remember { mutableStateOf(false) }
    var aCarregar by remember(praia.id) { mutableStateOf(true) }
    var erro by remember(praia.id) { mutableStateOf<String?>(null) }

    val previsao by remember(praia, perfil) { viewModel.previsao(praia, perfil) }
        .collectAsState(initial = emptyList())

    LaunchedEffect(praia.idLocalTempo, praia.idLocalMar) {
        aCarregar = true
        erro = viewModel.carregarPrevisao(praia)
        aCarregar = false
    }

    val diaAtual = previsao.find { it.dia == diaEscolhido }
    val nota = diaAtual?.nota
    val nomePerfil = perfil?.nome ?: NOME_PERFIL_PREDEFINIDO
    val fluvial = praia.tipo == "fluvial"
    val naoAplicavel = perfilPrecisaDeMar(perfil) && fluvial

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BotaoRedondo(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", onVoltar)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoRedondo(Icons.Outlined.Add, "Registar visita", { aRegistar = true })
                    BotaoRedondo(
                        icone = if (praia.favorita) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                        descricao = if (praia.favorita) "Remover das favoritas" else "Adicionar às favoritas",
                        onClick = { viewModel.definirFavorita(praia.id, !praia.favorita) },
                        cor = if (praia.favorita) CorEstrela else LocalContentColor.current
                    )
                    AvatarConta(onClick = onAbrirConta)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AnelNota(nota)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = praia.nome,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${praia.regiao} · ${if (fluvial) "Fluvial" else "Costeira"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.topoTextoSecundario
                    )
                    val corNota = coresNota(nota).first()
                    Etiqueta(
                        texto = textoNota(nota),
                        fundo = corNota.copy(alpha = 0.18f),
                        cor = corNota
                    )
                }
            }

            Segmentado(
                opcoes = viewModel.dias.mapIndexed { indice, dia ->
                    val rotulo = when (indice) {
                        0 -> "Hoje"
                        1 -> "Amanhã"
                        else -> diaSemanaCurto(dia).maiuscula()
                    }
                    val notaDia = previsao.find { it.dia == dia }?.nota
                    "$rotulo · ${notaDia ?: "—"}"
                },
                escolhida = viewModel.dias.indexOf(diaEscolhido).coerceAtLeast(0),
                onEscolher = { diaEscolhido = viewModel.dias[it] }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingFolha),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            avisos
                .filter { it.idAreaAviso == praia.idAreaAviso && avisoAtivoEm(it, diaEscolhido) }
                .forEach { aviso -> CartaoAviso(aviso) }

            LinhaPerfil(nomePerfil, previsao, diaAtual)

            when {
                naoAplicavel -> Aviso(
                    icone = Icons.Outlined.Waves,
                    cor = cores.textoSecundario,
                    texto = "O perfil $nomePerfil pede ondas, por isso não se aplica a praias fluviais."
                )

                diaAtual == null -> Aviso(
                    icone = Icons.Outlined.WarningAmber,
                    cor = cores.textoSecundario,
                    texto = when {
                        aCarregar -> "A carregar a previsão..."
                        erro != null -> "$erro Tenta outra vez mais tarde."
                        else -> "Sem previsão disponível para este dia."
                    }
                )

                else -> {
                    if (erro != null) {
                        Text(
                            text = "Não foi possível atualizar. A mostrar os dados guardados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.negativo
                        )
                    }
                    CartaoCondicoes(diaAtual)
                    if (fluvial) {
                        Text(
                            text = "Sem dados do mar: a nota usa só o tempo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.textoSecundario
                        )
                    }
                }
            }

            CartaoMapa(praia)

            Text(
                text = "Tempo: ${praia.nomeLocalTempo ?: "a associar"} · " +
                    "Mar: ${praia.nomeLocalMar ?: "não aplicável"} · IPMA\n" +
                    "Dados regionais, não medidos nesta praia.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.textoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (aRegistar) {
        DialogoVisita(
            nomePraia = praia.nome,
            onCancelar = { aRegistar = false },
            onGuardar = { notaVisita, comentario ->
                viewModel.registarVisita(praia.id, notaVisita, comentario)
                aRegistar = false
            }
        )
    }
}

@Composable
private fun LinhaPerfil(nomePerfil: String, previsao: List<DiaPrevisao>, diaAtual: DiaPrevisao?) {
    val cores = BeachDay.cores
    val notas = previsao.mapNotNull { it.nota }
    val melhorNota = notas.maxOrNull()
    val notaDia = diaAtual?.nota
    val etiqueta = when {
        notaDia == null || notaDia != melhorNota -> null
        notas.count { it == melhorNota } > 1 -> "Empatado"
        else -> "Melhor dos 3 dias"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Perfil $nomePerfil",
            style = MaterialTheme.typography.titleSmall,
            color = cores.texto,
            modifier = Modifier.weight(1f)
        )
        if (etiqueta != null) {
            Etiqueta(
                texto = etiqueta,
                fundo = cores.positivo.copy(alpha = 0.14f),
                cor = cores.positivo
            )
        }
    }
}

@Composable
private fun CartaoCondicoes(dia: DiaPrevisao) {
    fun pontos(condicao: Condicao): Int =
        dia.penalizacoes.filter { it.condicao == condicao }.sumOf { it.pontos }

    val direcao = dia.direcaoVento?.let { " · de $it" } ?: ""
    val relacao = dia.relacaoVento?.let { " ($it)" } ?: ""

    val linhas = buildList {
        add(Condicao.TEMPERATURA to Triple(Icons.Outlined.Thermostat, CorTemperatura,
            "Temperatura máxima" to "${numero(dia.temperaturaMaxima)} °C"))
        add(Condicao.CHUVA to Triple(Icons.Outlined.Umbrella, CorChuva,
            "Probabilidade de chuva" to "${numero(dia.probabilidadeChuva)}%"))
        add(Condicao.VENTO to Triple(Icons.Outlined.Air, CorVento,
            "Vento" to "${descricaoVento(dia.classeVento)}$direcao$relacao"))
        dia.alturaMaximaMar?.let {
            add(Condicao.MAR to Triple(Icons.Outlined.Waves, CorMar,
                "Altura máxima do mar" to "${numero(it)} m"))
        }
        dia.temperaturaMaximaAgua?.let {
            add(Condicao.AGUA to Triple(Icons.Outlined.WaterDrop, CorAgua,
                "Temperatura da água" to "${numero(it)} °C"))
        }
    }

    Cartao(preenchimento = PaddingValues(horizontal = 16.dp, vertical = 4.dp), espaco = 0.dp) {
        linhas.forEachIndexed { indice, (condicao, dados) ->
            val (icone, cor, textos) = dados
            LinhaCondicao(icone, cor, textos.first, textos.second, pontos(condicao))
            if (indice < linhas.lastIndex) {
                HorizontalDivider(color = BeachDay.cores.linha)
            }
        }
    }
}

@Composable
private fun LinhaCondicao(
    icone: ImageVector,
    cor: Color,
    rotulo: String,
    valor: String,
    pontos: Int
) {
    val cores = BeachDay.cores

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(cor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(rotulo, style = MaterialTheme.typography.bodySmall, color = cores.textoSecundario)
            Text(valor, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cores.texto)
        }
        Text(
            text = if (pontos > 0) "-$pontos" else "0",
            style = EstiloNumero.copy(fontSize = 16.sp),
            color = if (pontos > 0) cores.negativo else cores.positivo
        )
    }
}

@Composable
private fun CartaoAviso(aviso: AvisoIpma) {
    val cores = BeachDay.cores
    val (nivel, cor) = when (aviso.awarenessLevelID) {
        "red" -> "vermelho" to cores.negativo
        "orange" -> "laranja" to CorTemperatura
        else -> "amarelo" to CorEstrela
    }

    Cartao(modifier = Modifier.border(1.dp, cor.copy(alpha = 0.5f), RoundedCornerShape(22.dp))) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(cor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Aviso $nivel: ${aviso.awarenessTypeName ?: "IPMA"}",
                    style = MaterialTheme.typography.titleSmall,
                    color = cores.texto
                )
                aviso.text?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = cores.texto)
                }
            }
        }
    }
}

@Composable
private fun CartaoMapa(praia: Praia) {
    val context = LocalContext.current
    val cores = BeachDay.cores
    val forma = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(forma)
            .border(1.dp, cores.linha, forma)
    ) {
        MapaPraia(praia, modifier = Modifier.fillMaxSize())

        BotaoPrincipal(
            texto = "Como chegar",
            detalhe = "abre o Google Maps",
            icone = Icons.Outlined.Directions,
            onClick = {
                abrirMapa(
                    context,
                    "https://www.google.com/maps/dir/?api=1&destination=${praia.latitude},${praia.longitude}"
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}

/** Mapa do OpenStreetMap (osmdroid) com um marcador na praia. */
@Composable
private fun MapaPraia(praia: Praia, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val mapa = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            // Enquanto se mexe no mapa, o ecrã não faz scroll
            setOnTouchListener { vista, evento ->
                vista.parent?.requestDisallowInterceptTouchEvent(true)
                if (evento.action == MotionEvent.ACTION_UP) vista.performClick()
                false
            }
        }
    }

    DisposableEffect(mapa) {
        mapa.onResume()
        onDispose {
            mapa.onPause()
            mapa.onDetach()
        }
    }

    AndroidView(
        factory = { mapa },
        modifier = modifier,
        update = { vista ->
            val ponto = GeoPoint(praia.latitude, praia.longitude)
            vista.controller.setZoom(14.0)
            vista.controller.setCenter(ponto)

            vista.overlays.removeAll { it is Marker }
            vista.overlays.add(
                Marker(vista).apply {
                    position = ponto
                    title = praia.nome
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )

            vista.invalidate()
        }
    )
}

private fun abrirMapa(context: Context, endereco: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, endereco.toUri()))
    } catch (erro: ActivityNotFoundException) {
        Log.w("BeachDay", "Nenhuma app abriu o mapa", erro)
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
        shape = RoundedCornerShape(24.dp),
        title = { Text("Visita a $nomePraia", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Como correu?", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { valor ->
                        FilterChip(
                            selected = nota == valor,
                            onClick = { nota = valor },
                            label = { Text("$valor") }
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text("Comentário (opcional)") },
                    shape = RoundedCornerShape(16.dp)
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
