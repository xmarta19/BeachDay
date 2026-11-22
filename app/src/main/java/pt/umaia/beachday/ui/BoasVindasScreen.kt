package pt.umaia.beachday.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.umaia.beachday.ui.theme.BeachDay

/**
 * Ecrã da primeira vez que se abre a app:
 * entrar com Google, entrar com email ou continuar como convidado.
 */
@Composable
fun BoasVindasScreen(
    onConvidado: () -> Unit,
    onSessaoIniciada: () -> Unit,
    viewModel: ContaViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val aCarregar by viewModel.aCarregar.collectAsState()
    val cores = BeachDay.cores

    // Quando a sessão é iniciada (Google ou email), segue para a app.
    // As mensagens aparecem na barra do fundo do ecrã.
    LaunchedEffect(estado.email) {
        if (estado.email != null) onSessaoIniciada()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cores.topo)
    ) {
        // Círculos decorativos
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-90).dp, y = 220.dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(cores.destaque),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.WbSunny,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "BeachDay",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = cores.topoTexto
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "A melhor praia para ir hoje, com a previsão do IPMA.",
                style = MaterialTheme.typography.bodyLarge,
                color = cores.topoTextoSecundario
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Nota de 0 a 10", fundo = cores.topoChip, cor = cores.topoTexto)
                Etiqueta("3 dias", fundo = cores.topoChip, cor = cores.topoTexto)
                Etiqueta("Perfis", fundo = cores.topoChip, cor = cores.topoTexto)
            }

            Spacer(Modifier.weight(1f))


            BotaoEntrada(
                texto = "Iniciar sessão",
                icone = Icons.Outlined.AccountCircle,
                principal = true,
                aCarregar = aCarregar,
                onClick = { viewModel.abrirFolha(Folha.ENTRAR) }
            )
            Spacer(Modifier.height(10.dp))
            BotaoEntrada(
                texto = "Criar conta",
                icone = Icons.Outlined.PersonAdd,
                principal = false,
                aCarregar = false,
                ativo = !aCarregar,
                onClick = { viewModel.abrirFolha(Folha.CRIAR) }
            )
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onConvidado,
                enabled = !aCarregar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continuar como convidado",
                    style = MaterialTheme.typography.labelLarge,
                    color = cores.topoTexto
                )
            }
            Text(
                text = "Como convidado, os dados ficam só neste telemóvel. " +
                    "Podes iniciar sessão mais tarde no ícone de pessoa, no topo.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.topoTextoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

}

@Composable
private fun BotaoEntrada(
    texto: String,
    icone: ImageVector,
    principal: Boolean,
    aCarregar: Boolean,
    onClick: () -> Unit,
    ativo: Boolean = !aCarregar
) {
    val cores = BeachDay.cores

    OutlinedButton(
        onClick = onClick,
        enabled = ativo,
        shape = RoundedCornerShape(18.dp),
        border = if (principal) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (principal) Color.White else Color.Transparent,
            contentColor = if (principal) cores.topo else cores.topoTexto,
            disabledContainerColor = if (principal) Color.White.copy(alpha = 0.6f) else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (aCarregar) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = cores.topo
            )
        } else {
            Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}
