package pt.umaia.beachday.ui.definicoes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.data.local.Preferencias
import pt.umaia.beachday.data.local.Tema
import pt.umaia.beachday.ui.comum.AvatarConta
import pt.umaia.beachday.ui.comum.CabecalhoTitulo
import pt.umaia.beachday.ui.comum.Cartao
import pt.umaia.beachday.ui.comum.EcraBeachDay
import pt.umaia.beachday.ui.comum.PaddingFolha
import pt.umaia.beachday.ui.conta.ContaViewModel
import pt.umaia.beachday.ui.theme.BeachDay

@Composable
fun DefinicoesScreen(
    contaViewModel: ContaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias.obter(context) }
    val tema by preferencias.tema.collectAsState()
    val estado by contaViewModel.estado.collectAsState()
    val cores = BeachDay.cores

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            CabecalhoTitulo(
                titulo = "Definições",
                subtitulo = "Aparência, conta e informação da app.",
                onAbrirConta = { contaViewModel.abrirPainel() }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingFolha),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TituloSeccao("Aparência")
            Cartao {
                Text("Tema", style = MaterialTheme.typography.titleSmall, color = cores.texto)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(Tema.SISTEMA, "Sistema", Icons.Outlined.PhoneAndroid),
                        Triple(Tema.CLARO, "Claro", Icons.Outlined.LightMode),
                        Triple(Tema.ESCURO, "Escuro", Icons.Outlined.DarkMode)
                    ).forEach { (opcao, nome, icone) ->
                        OpcaoTema(
                            nome = nome,
                            icone = icone,
                            escolhida = tema == opcao,
                            onClick = { preferencias.escolherTema(opcao) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = "\"Sistema\" segue o modo claro ou escuro do telemóvel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.textoSecundario
                )
            }

            TituloSeccao("Conta")
            Cartao(modifier = Modifier.clickable(role = Role.Button) { contaViewModel.abrirPainel() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AvatarConta(onClick = null, tamanho = 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = estado.nome?.takeIf { it.isNotBlank() }
                                ?: estado.email?.substringBefore("@") ?: "Convidado",
                            style = MaterialTheme.typography.titleSmall,
                            color = cores.texto
                        )
                        Text(
                            text = estado.email ?: "Inicia sessão para guardar os dados na nuvem.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.textoSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = cores.textoSecundario
                    )
                }
            }

            TituloSeccao("Sobre")
            Cartao(preenchimento = PaddingValues(vertical = 4.dp), espaco = 0.dp) {
                LinhaInfo(Icons.Outlined.WbSunny, "Previsão do tempo e do mar", "IPMA · api.ipma.pt")
                HorizontalDivider(color = cores.linha)
                LinhaInfo(Icons.Outlined.Map, "Mapas", "© OpenStreetMap contributors")
            }
            Text(
                text = "BeachDay · Projeto UMAIA",
                style = MaterialTheme.typography.bodySmall,
                color = cores.textoSecundario,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TituloSeccao(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = BeachDay.cores.textoSecundario,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun OpcaoTema(
    nome: String,
    icone: ImageVector,
    escolhida: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cores = BeachDay.cores

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (escolhida) cores.destaque else cores.bloco)
            .selectable(selected = escolhida, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = if (escolhida) Color.White else cores.texto,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = nome,
            style = MaterialTheme.typography.labelMedium,
            color = if (escolhida) Color.White else cores.texto
        )
    }
}

@Composable
private fun LinhaInfo(icone: ImageVector, titulo: String, valor: String) {
    val cores = BeachDay.cores

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cores.azul.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icone, contentDescription = null, tint = cores.azul, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = cores.texto)
            Text(valor, style = MaterialTheme.typography.bodySmall, color = cores.textoSecundario)
        }
    }
}
