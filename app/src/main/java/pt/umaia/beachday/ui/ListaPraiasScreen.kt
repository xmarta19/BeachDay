package pt.umaia.beachday.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.umaia.beachday.data.NOME_PERFIL_PREDEFINIDO
import pt.umaia.beachday.data.perfilPrecisaDeMar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val regioes = listOf(
    "Norte", "Centro", "Lisboa e Setúbal",
    "Alentejo", "Algarve", "Madeira", "Açores"
)

private val tipos = listOf("Costeiras", "Fluviais", "Todas")

@Composable
fun ListaPraiasScreen(
    onAbrirPraia: (Int) -> Unit,
    onMudarDia: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PraiasViewModel = viewModel()
) {
    val praias by viewModel.praias.collectAsState()
    val pesquisa by viewModel.pesquisa.collectAsState()
    val regiao by viewModel.regiao.collectAsState()
    val tipo by viewModel.tipo.collectAsState()
    val temRede by viewModel.temRede.collectAsState()
    val aAtualizar by viewModel.aAtualizar.collectAsState()
    val ultimaAtualizacao by viewModel.ultimaAtualizacao.collectAsState()
    val dia by viewModel.dia.collectAsState()
    val perfil by viewModel.perfil.collectAsState()
    val nomePerfil = perfil?.nome ?: NOME_PERFIL_PREDEFINIDO
    val soCosteiras = perfilPrecisaDeMar(perfil)
    val botaoBloqueado by viewModel.botaoBloqueado.collectAsState()
    val erro by viewModel.erroAtualizacao.collectAsState()

    val semDados = ultimaAtualizacao == null && !aAtualizar && (erro != null || !temRede)
    if (semDados) {
        EcraSemDados(
            motivo = if (!temRede) "Não tens ligação à internet." else erro.orEmpty(),
            podeTentar = temRede,
            onTentar = { viewModel.atualizar() },
            onMudarDia = onMudarDia,
            modifier = modifier
        )
        return
    }

    Column(modifier.fillMaxSize()) {
        TextButton(
            onClick = onMudarDia,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        ) {
            Text("← Mudar dia")
        }

        if (!temRede) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = "Sem ligação. A mostrar os últimos dados guardados.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (erro != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Não foi possível atualizar. A mostrar dados de " +
                        textoData(ultimaAtualizacao) + ".",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        OutlinedTextField(
            value = pesquisa,
            onValueChange = { viewModel.pesquisa.value = it },
            label = { Text("Pesquisar praia") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(regioes) { item ->
                FilterChip(
                    selected = regiao == item,
                    onClick = {
                        viewModel.escolherRegiao(if (regiao == item) null else item)
                    },
                    label = { Text(item) }
                )
            }
        }

        if (!soCosteiras) LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tipos) { item ->
                FilterChip(
                    selected = tipo == item,
                    onClick = { viewModel.tipo.value = item },
                    label = { Text(item) }
                )
            }
        }

        Text(
            text = "Melhores praias para ${viewModel.nomeDia(dia)}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        Text(
            text = when {
                soCosteiras ->
                    "Perfil $nomePerfil: só praias costeiras, porque pede ondas."
                tipo == "Costeiras" ->
                    "Perfil $nomePerfil. Praias com os mesmos dados do IPMA têm a mesma nota."
                else ->
                    "Perfil $nomePerfil. As praias fluviais não usam os dados do mar."
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = textoAtualizacao(ultimaAtualizacao),
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = { viewModel.atualizar() },
                enabled = temRede && !aAtualizar && !botaoBloqueado
            ) {
                Text(
                    when {
                        aAtualizar -> "A atualizar..."
                        botaoBloqueado -> "Atualizado agora"
                        else -> "Atualizar"
                    }
                )
            }
        }

        LazyColumn {
            items(praias, key = { it.praia.id }) { item ->
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

@Composable
private fun EcraSemDados(
    motivo: String,
    podeTentar: Boolean,
    onTentar: () -> Unit,
    onMudarDia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sem dados para mostrar", style = MaterialTheme.typography.headlineSmall)
        Text(motivo)
        Text(
            text = "A app precisa de ligar ao IPMA pelo menos uma vez.",
            style = MaterialTheme.typography.bodySmall
        )
        Button(onClick = onTentar, enabled = podeTentar) {
            Text("Tentar outra vez")
        }
        TextButton(onClick = onMudarDia) {
            Text("← Mudar dia")
        }
    }
}

private fun textoData(instante: Long?): String {
    if (instante == null) return "uma atualização anterior"

    val formato = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-PT"))
    return formato.format(Date(instante))
}

private fun textoAtualizacao(instante: Long?): String {
    if (instante == null) return "Ainda sem dados guardados"

    val formato = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-PT"))
    return "Atualizado: ${formato.format(Date(instante))}"
}
