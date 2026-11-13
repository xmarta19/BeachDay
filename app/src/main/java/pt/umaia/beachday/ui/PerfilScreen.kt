package pt.umaia.beachday.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.data.Perfil
import pt.umaia.beachday.data.Regras
import pt.umaia.beachday.data.descricaoVento
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PerfilScreen(
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val perfil by viewModel.perfil.collectAsState()
    val regras by viewModel.regras.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "O que vais fazer à praia?",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "O perfil muda a forma como a nota é calculada.",
            style = MaterialTheme.typography.bodyMedium
        )

        Perfil.entries.forEach { opcao ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.escolherPerfil(opcao) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = perfil == opcao,
                    onClick = { viewModel.escolherPerfil(opcao) }
                )
                Column {
                    Text(opcao.nome, style = MaterialTheme.typography.titleMedium)
                    Text(opcao.descricao, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (perfil == Perfil.PERSONALIZADO) {
            RegrasPersonalizadas(
                regras = regras,
                onAlterar = { viewModel.alterarRegras(it) }
            )
        }
    }
}

@Composable
private fun RegrasPersonalizadas(
    regras: Regras,
    onAlterar: (Regras) -> Unit
) {
    Text(
        text = "Os teus limites",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp)
    )
    Text(
        text = "Fora destes limites, a praia perde pontos.",
        style = MaterialTheme.typography.bodySmall
    )

    Text("Temperatura mínima do ar: ${regras.temperaturaMinima.roundToInt()} °C")
    Slider(
        value = regras.temperaturaMinima.toFloat(),
        onValueChange = { onAlterar(regras.copy(temperaturaMinima = it.roundToInt().toDouble())) },
        valueRange = 15f..32f,
        steps = 16
    )

    Text("Probabilidade máxima de chuva: ${regras.chuvaMaxima.roundToInt()}%")
    Slider(
        value = regras.chuvaMaxima.toFloat(),
        onValueChange = { onAlterar(regras.copy(chuvaMaxima = (it / 5).roundToInt() * 5.0)) },
        valueRange = 0f..100f,
        steps = 19
    )

    Text("Vento máximo: ${descricaoVento(regras.ventoMaximo)}")
    Slider(
        value = regras.ventoMaximo.toFloat(),
        onValueChange = { onAlterar(regras.copy(ventoMaximo = it.roundToInt())) },
        valueRange = 1f..4f,
        steps = 2
    )

    Text("Altura máxima do mar: ${formatar(regras.marMaximo)} m")
    Slider(
        value = regras.marMaximo.toFloat(),
        onValueChange = { onAlterar(regras.copy(marMaximo = (it * 2).roundToInt() / 2.0)) },
        valueRange = 0.5f..4f,
        steps = 6
    )

    Text("Temperatura mínima da água: ${regras.aguaMinima.roundToInt()} °C")
    Slider(
        value = regras.aguaMinima.toFloat(),
        onValueChange = { onAlterar(regras.copy(aguaMinima = it.roundToInt().toDouble())) },
        valueRange = 10f..24f,
        steps = 13
    )

    OutlinedButton(onClick = { onAlterar(Regras()) }) {
        Text("Repor valores iniciais")
    }
}

private fun formatar(valor: Double): String =
    String.format(Locale.forLanguageTag("pt-PT"), "%.1f", valor)
