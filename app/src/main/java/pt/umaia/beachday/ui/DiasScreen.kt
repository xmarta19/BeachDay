package pt.umaia.beachday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.umaia.beachday.data.NOME_PERFIL_PREDEFINIDO
import pt.umaia.beachday.data.PraiaRanking
import pt.umaia.beachday.ui.theme.BeachDay
import pt.umaia.beachday.ui.theme.EstiloNumero
import pt.umaia.beachday.ui.theme.pincelNota
import pt.umaia.beachday.ui.theme.textoNota

@Composable
fun DiasScreen(
    onEscolherDia: () -> Unit,
    onAbrirPerfil: () -> Unit,
    viewModel: PraiasViewModel,
    modifier: Modifier = Modifier
) {
    val perfil by viewModel.perfil.collectAsState()
    val melhores by viewModel.melhoresPorDia.collectAsState()
    val ultimaAtualizacao by viewModel.ultimaAtualizacao.collectAsState()
    val aAtualizar by viewModel.aAtualizar.collectAsState()
    val botaoBloqueado by viewModel.botaoBloqueado.collectAsState()
    val temRede by viewModel.temRede.collectAsState()
    val cores = BeachDay.cores

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cores.destaque),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WbSunny,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "BeachDay",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                BotaoAtualizar(
                    aAtualizar = aAtualizar,
                    ativo = temRede && !botaoBloqueado,
                    acabouDeAtualizar = botaoBloqueado,
                    onClick = { viewModel.atualizar() }
                )
            }

            Text(
                text = "Quando vais à praia?",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(cores.topoChip)
                    .clickable(role = Role.Button, onClick = onAbrirPerfil)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = "Perfil: ${perfil?.nome ?: NOME_PERFIL_PREDEFINIDO}",
                    style = MaterialTheme.typography.labelMedium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingFolha),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            viewModel.dias.forEachIndexed { indice, dia ->
                val abrir = {
                    viewModel.escolherDia(dia)
                    onEscolherDia()
                }
                if (indice == 0) {
                    CartaoHoje(dia, melhores[dia], abrir)
                } else {
                    CartaoDia(
                        dia = dia,
                        nome = viewModel.nomeDia(dia).maiuscula(),
                        melhor = melhores[dia],
                        onClick = abrir
                    )
                }
            }

            Text(
                text = "${textoAtualizacao(ultimaAtualizacao)} · Dados do IPMA",
                style = MaterialTheme.typography.bodySmall,
                color = cores.textoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
            Text(
                text = "O IPMA só tem previsão do mar para 3 dias.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.textoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CartaoHoje(dia: String, melhor: PraiaRanking?, onClick: () -> Unit) {
    val nota = melhor?.nota

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(pincelNota(nota))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        // Círculo decorativo no canto
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
        )

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "HOJE · ${dataCurta(dia).uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = textoNota(nota),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                        color = Color.White
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = nota?.toString() ?: "—",
                        style = EstiloNumero.copy(fontSize = 40.sp, lineHeight = 40.sp),
                        color = Color.White
                    )
                    if (nota != null) {
                        Text(
                            text = "/10",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = melhor?.let { "Melhor: ${it.praia.nome}" } ?: "Ver as praias de hoje",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CartaoDia(dia: String, nome: String, melhor: PraiaRanking?, onClick: () -> Unit) {
    val cores = BeachDay.cores
    val forma = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(cores.cartao)
            .border(1.dp, cores.linha, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cores.bloco),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = diaDoMes(dia),
                style = EstiloNumero.copy(fontSize = 20.sp, lineHeight = 22.sp),
                color = cores.texto
            )
            Text(mesCurto(dia), fontSize = 11.sp, color = cores.textoSecundario)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(nome, style = MaterialTheme.typography.titleMedium, color = cores.texto)
            Text(
                text = melhor?.let { "Melhor: ${it.praia.nome}" } ?: diaSemanaCurto(dia).maiuscula(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = cores.textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        NotaQuadrado(melhor?.nota)
    }
}
