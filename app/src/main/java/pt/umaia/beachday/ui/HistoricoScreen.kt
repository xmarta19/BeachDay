package pt.umaia.beachday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.umaia.beachday.ui.theme.BeachDay
import pt.umaia.beachday.ui.theme.CorEstrela
import pt.umaia.beachday.ui.theme.EstiloNumero
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoricoScreen(
    onAbrirConta: () -> Unit,
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val visitas by viewModel.visitas.collectAsState()
    val formato = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("pt-PT"))
    val cores = BeachDay.cores

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            CabecalhoTitulo(
                titulo = "Histórico",
                subtitulo = when (visitas.size) {
                    0 -> "As praias onde já foste"
                    1 -> "1 visita registada"
                    else -> "${visitas.size} visitas registadas"
                },
                onAbrirConta = onAbrirConta
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingFolha,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (visitas.isEmpty()) {
                item {
                    Aviso(
                        icone = Icons.Outlined.History,
                        cor = cores.textoSecundario,
                        texto = "Ainda não registaste nenhuma visita. Abre uma praia e toca no +."
                    )
                }
            }

            items(visitas, key = { it.id }) { visita ->
                Cartao(preenchimento = PaddingValues(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CorEstrela.copy(alpha = 0.16f)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = null,
                                tint = CorEstrela,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${visita.nota}/5",
                                style = EstiloNumero.copy(fontSize = 14.sp),
                                color = cores.texto
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = visita.nomePraia,
                                style = MaterialTheme.typography.titleMedium,
                                color = cores.texto,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formato.format(Date(visita.data)).replace(".", ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = cores.textoSecundario
                            )
                            visita.comentario?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cores.texto,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box {
                            BotaoRedondo(
                                icone = Icons.Outlined.DeleteOutline,
                                descricao = "Apagar visita a ${visita.nomePraia}",
                                onClick = { viewModel.apagarVisita(visita.id) },
                                cor = cores.textoSecundario,
                                fundo = cores.chip,
                                tamanho = 36.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
