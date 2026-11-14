package pt.umaia.beachday.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.data.NOME_PERFIL_PREDEFINIDO
import pt.umaia.beachday.ui.theme.BeachDay
import pt.umaia.beachday.ui.theme.CorEstrela

@Composable
fun FavoritasScreen(
    onAbrirPraia: (Int) -> Unit,
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val favoritas by viewModel.favoritas.collectAsState()
    val dia by viewModel.dia.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val cores = BeachDay.cores

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            Text("Favoritas", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Notas para ${viewModel.nomeDia(dia)} · perfil ${perfil?.nome ?: NOME_PERFIL_PREDEFINIDO}",
                style = MaterialTheme.typography.bodyMedium,
                color = cores.topoTextoSecundario
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingFolha,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (favoritas.isEmpty()) {
                item {
                    Aviso(
                        icone = Icons.Outlined.StarBorder,
                        cor = CorEstrela,
                        texto = "Ainda não tens favoritas. Abre uma praia e toca na estrela."
                    )
                }
            }

            itemsIndexed(favoritas, key = { _, item -> item.praia.id }) { indice, item ->
                CartaoRanking(
                    posicao = indice + 1,
                    item = item,
                    onClick = { onAbrirPraia(item.praia.id) }
                )
            }
        }
    }
}
