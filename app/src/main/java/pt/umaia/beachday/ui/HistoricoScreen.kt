package pt.umaia.beachday.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoricoScreen(
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val visitas by viewModel.visitas.collectAsState()
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-PT"))

    Column(modifier.fillMaxSize()) {
        Text(
            text = "Histórico de visitas",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        if (visitas.isEmpty()) {
            Text(
                text = "Ainda não registaste nenhuma visita.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(visitas, key = { it.id }) { visita ->
                    ListItem(
                        headlineContent = { Text(visita.nomePraia) },
                        supportingContent = {
                            val data = formato.format(Date(visita.data))
                            val comentario = visita.comentario?.let { " · $it" } ?: ""
                            Text("$data · ${visita.nota}/5$comentario")
                        },
                        trailingContent = {
                            TextButton(onClick = { viewModel.apagarVisita(visita.id) }) {
                                Text("Apagar")
                            }
                        }
                    )
                }
            }
        }
    }
}