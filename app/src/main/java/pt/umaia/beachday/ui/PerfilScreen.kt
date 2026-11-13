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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.data.NOME_PERFIL_PREDEFINIDO
import pt.umaia.beachday.data.PerfilUtilizador
import pt.umaia.beachday.data.descricaoVento
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PerfilScreen(
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val perfis by viewModel.perfis.collectAsState()
    val idPerfil by viewModel.idPerfil.collectAsState()

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
            text = "O perfil escolhido muda a forma como a nota é calculada.",
            style = MaterialTheme.typography.bodyMedium
        )

        LinhaPerfil(
            nome = NOME_PERFIL_PREDEFINIDO,
            descricao = "Perfil predefinido: um dia normal de praia, com sol, areal e banho.",
            escolhido = idPerfil == 0 || perfis.none { it.id == idPerfil },
            onEscolher = { viewModel.escolherPerfil(0) }
        )

        perfis.forEach { perfil ->
            LinhaPerfil(
                nome = perfil.nome,
                descricao = resumo(perfil),
                escolhido = perfil.id == idPerfil,
                onEscolher = { viewModel.escolherPerfil(perfil.id) },
                onEditar = { aEditar = perfil },
                onApagar = { aApagar = perfil }
            )
        }

        Button(
            onClick = { aEditar = PerfilUtilizador(nome = "") },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("+ Criar perfil")
        }
    }

    val paraApagar = aApagar
    if (paraApagar != null) {
        AlertDialog(
            onDismissRequest = { aApagar = null },
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

@Composable
private fun LinhaPerfil(
    nome: String,
    descricao: String,
    escolhido: Boolean,
    onEscolher: () -> Unit,
    onEditar: (() -> Unit)? = null,
    onApagar: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEscolher() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = escolhido, onClick = onEscolher)
            Column(Modifier.weight(1f)) {
                Text(nome, style = MaterialTheme.typography.titleMedium)
                Text(descricao, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (onEditar != null && onApagar != null) {
            Row(Modifier.padding(start = 40.dp)) {
                TextButton(onClick = onEditar) { Text("Editar") }
                TextButton(onClick = onApagar) { Text("Apagar") }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (inicial.id == 0) "Novo perfil" else "Editar perfil",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = perfil.nome,
            onValueChange = { perfil = perfil.copy(nome = it) },
            label = { Text("Nome do perfil, por exemplo Surf") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Fora destes limites, a praia perde pontos.",
            style = MaterialTheme.typography.bodySmall
        )

        Text("Temperatura mínima do ar: ${perfil.temperaturaMinima.roundToInt()} °C")
        Slider(
            value = perfil.temperaturaMinima.toFloat(),
            onValueChange = { perfil = perfil.copy(temperaturaMinima = it.roundToInt().toDouble()) },
            valueRange = 15f..32f,
            steps = 16
        )

        Text("Probabilidade máxima de chuva: ${perfil.chuvaMaxima.roundToInt()}%")
        Slider(
            value = perfil.chuvaMaxima.toFloat(),
            onValueChange = { perfil = perfil.copy(chuvaMaxima = (it / 5).roundToInt() * 5.0) },
            valueRange = 0f..100f,
            steps = 19
        )

        Text("Vento máximo: ${descricaoVento(perfil.ventoMaximo)}")
        Slider(
            value = perfil.ventoMaximo.toFloat(),
            onValueChange = { perfil = perfil.copy(ventoMaximo = it.roundToInt()) },
            valueRange = 1f..4f,
            steps = 2
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Quero ondas")
                Text(
                    text = "Define uma altura mínima do mar. Exclui as praias fluviais.",
                    style = MaterialTheme.typography.bodySmall
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
            Text("Altura mínima do mar: ${decimal(minimo)} m")
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

        Text("Altura máxima do mar: ${decimal(perfil.marMaximo)} m")
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

        Text("Temperatura mínima da água: ${perfil.aguaMinima.roundToInt()} °C")
        Slider(
            value = perfil.aguaMinima.toFloat(),
            onValueChange = { perfil = perfil.copy(aguaMinima = it.roundToInt().toDouble()) },
            valueRange = 10f..24f,
            steps = 13
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onGuardar(perfil.copy(nome = perfil.nome.trim())) },
                enabled = perfil.nome.isNotBlank()
            ) {
                Text("Guardar")
            }
            OutlinedButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    }
}

private fun resumo(perfil: PerfilUtilizador): String {
    val ondas = perfil.marMinimo?.let { "mar ${decimal(it)} a ${decimal(perfil.marMaximo)} m" }
        ?: "mar até ${decimal(perfil.marMaximo)} m"
    return "Mín. ${perfil.temperaturaMinima.roundToInt()} °C · chuva até " +
        "${perfil.chuvaMaxima.roundToInt()}% · vento até " +
        "${descricaoVento(perfil.ventoMaximo).lowercase()} · $ondas"
}

private fun decimal(valor: Double): String =
    String.format(Locale.forLanguageTag("pt-PT"), "%.1f", valor)
