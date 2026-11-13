package pt.umaia.beachday.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FavoritasScreen(
    onAbrirPraia: (Int) -> Unit,
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val favoritas by viewModel.favoritas.collectAsState()
    val dia by viewModel.dia.collectAsState()

    Column(modifier.fillMaxSize()) {
        Text(
            text = "As tuas favoritas para ${viewModel.nomeDia(dia)}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        if (favoritas.isEmpty()) {
            Text(
                text = "Ainda não tens favoritas. Abre uma praia para adicionar.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(favoritas, key = { it.praia.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.praia.nome) },
                        supportingContent = {
                            val aviso = if (item.avisoGrave) " · ⚠ Aviso" else ""
                            Text("${item.praia.regiao} · ${item.praia.tipo}$aviso")
                        },
                        trailingContent = {
                            Text(
                                text = item.nota?.let { "$it/10" } ?: "—",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        modifier = Modifier.clickable { onAbrirPraia(item.praia.id) }
                    )
                }
            }
        }
    }
}
