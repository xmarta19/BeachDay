package pt.umaia.beachday.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.data.NOME_PERFIL_PREDEFINIDO
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DiasScreen(
    onEscolherDia: () -> Unit,
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val perfil by viewModel.perfil.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quando queres ir à praia?",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Perfil: ${perfil?.nome ?: NOME_PERFIL_PREDEFINIDO}",
            style = MaterialTheme.typography.bodyMedium
        )

        viewModel.dias.forEach { dia ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.escolherDia(dia)
                        onEscolherDia()
                    }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = viewModel.nomeDia(dia).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(dataPorExtenso(dia))
                }
            }
        }

        Text(
            text = "O IPMA só tem previsão do mar para 3 dias.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun dataPorExtenso(dia: String): String {
    val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val saida = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-PT"))
    return entrada.parse(dia)?.let { saida.format(it) } ?: dia
}
