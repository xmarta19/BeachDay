package pt.umaia.beachday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.data.NOME_PERFIL_PREDEFINIDO
import pt.umaia.beachday.data.perfilPrecisaDeMar
import pt.umaia.beachday.ui.theme.BeachDay

private val regioes = listOf(
    "Norte", "Centro", "Lisboa e Setúbal",
    "Alentejo", "Algarve", "Madeira", "Açores"
)

// Valores usados no repositório e o texto mostrado no seletor
private val tipos = listOf("Costeiras", "Fluviais", "Todas")
private val nomesTipos = listOf("Mar", "Fluviais", "Todas")

@Composable
fun ListaPraiasScreen(
    onAbrirPraia: (Int) -> Unit,
    onMudarDia: () -> Unit,
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
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
    val botaoBloqueado by viewModel.botaoBloqueado.collectAsState()
    val erro by viewModel.erroAtualizacao.collectAsState()
    val nomePerfil = perfil?.nome ?: NOME_PERFIL_PREDEFINIDO
    val soCosteiras = perfilPrecisaDeMar(perfil)
    val cores = BeachDay.cores

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

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotaoRedondo(Icons.AutoMirrored.Outlined.ArrowBack, "Mudar dia", onMudarDia)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${viewModel.nomeDia(dia).maiuscula()} · ${dataCurta(dia)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.topoTextoSecundario
                    )
                    Text("Melhores praias", style = MaterialTheme.typography.titleLarge)
                }
                BotaoAtualizar(
                    aAtualizar = aAtualizar,
                    ativo = temRede && !botaoBloqueado,
                    acabouDeAtualizar = botaoBloqueado,
                    onClick = { viewModel.atualizar() }
                )
            }

            CampoPesquisa(
                valor = pesquisa,
                onMudar = { viewModel.pesquisa.value = it }
            )

            if (!soCosteiras) {
                Segmentado(
                    opcoes = nomesTipos,
                    escolhida = tipos.indexOf(tipo).coerceAtLeast(0),
                    onEscolher = { viewModel.tipo.value = tipos[it] }
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingFolha,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!temRede) {
                item {
                    Aviso(
                        icone = Icons.Outlined.CloudOff,
                        cor = cores.textoSecundario,
                        texto = "Sem ligação. A mostrar os últimos dados guardados."
                    )
                }
            } else if (erro != null) {
                item {
                    Aviso(
                        icone = Icons.Outlined.ErrorOutline,
                        cor = cores.negativo,
                        texto = "Não foi possível atualizar. ${textoAtualizacao(ultimaAtualizacao)}."
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SeletorRegiao(regiao) { viewModel.escolherRegiao(it) }
                    Text(
                        text = if (praias.size == 1) "1 praia · por nota" else "${praias.size} praias · por nota",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cores.textoSecundario
                    )
                }
            }

            item {
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
                    color = cores.textoSecundario,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

            if (praias.isEmpty()) {
                item {
                    Cartao {
                        Text(
                            text = "Nenhuma praia encontrada com estes filtros.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cores.textoSecundario
                        )
                    }
                }
            }

            itemsIndexed(praias, key = { _, item -> item.praia.id }) { indice, item ->
                CartaoRanking(
                    posicao = indice + 1,
                    item = item,
                    onClick = { onAbrirPraia(item.praia.id) }
                )
            }
        }
    }
}

@Composable
private fun CampoPesquisa(valor: String, onMudar: (String) -> Unit) {
    val cores = BeachDay.cores

    OutlinedTextField(
        value = valor,
        onValueChange = onMudar,
        placeholder = { Text("Pesquisar praia") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (valor.isNotEmpty()) {
            {
                IconButton(onClick = { onMudar("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Limpar pesquisa")
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cores.topoChip,
            unfocusedContainerColor = cores.topoChip,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = cores.topoTexto,
            unfocusedTextColor = cores.topoTexto,
            cursorColor = cores.topoTexto,
            focusedPlaceholderColor = cores.topoTextoSecundario,
            unfocusedPlaceholderColor = cores.topoTextoSecundario,
            focusedLeadingIconColor = cores.topoTextoSecundario,
            unfocusedLeadingIconColor = cores.topoTextoSecundario,
            focusedTrailingIconColor = cores.topoTextoSecundario,
            unfocusedTrailingIconColor = cores.topoTextoSecundario
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SeletorRegiao(regiao: String?, onEscolher: (String?) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    val cores = BeachDay.cores
    val forma = RoundedCornerShape(50)

    Box {
        Row(
            modifier = Modifier
                .clip(forma)
                .background(cores.cartao)
                .border(1.dp, cores.linhaForte, forma)
                .clickable(role = Role.DropdownList) { aberto = true }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.Place,
                contentDescription = null,
                tint = cores.textoSecundario,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = regiao ?: "Todas as regiões",
                style = MaterialTheme.typography.labelMedium,
                color = cores.texto,
                maxLines = 1
            )
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = cores.texto,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            (listOf<String?>(null) + regioes).forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(opcao ?: "Todas as regiões") },
                    onClick = {
                        onEscolher(opcao)
                        aberto = false
                    },
                    trailingIcon = if (opcao == regiao) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
fun Aviso(icone: ImageVector, cor: Color, texto: String) {
    Cartao(preenchimento = PaddingValues(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = BeachDay.cores.texto
            )
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
    val cores = BeachDay.cores

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotaoRedondo(Icons.AutoMirrored.Outlined.ArrowBack, "Mudar dia", onMudarDia)
                Text("Melhores praias", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cores.bloco),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = cores.textoSecundario,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = "Sem dados para mostrar",
                style = MaterialTheme.typography.headlineSmall,
                color = cores.texto
            )
            Text(
                text = motivo,
                style = MaterialTheme.typography.bodyMedium,
                color = cores.texto,
                textAlign = TextAlign.Center
            )
            Text(
                text = "A app precisa de ligar ao IPMA pelo menos uma vez.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.textoSecundario,
                textAlign = TextAlign.Center
            )
            BotaoPrincipal(
                texto = "Tentar outra vez",
                onClick = onTentar,
                ativo = podeTentar,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
