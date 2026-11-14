package pt.umaia.beachday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.umaia.beachday.ui.theme.BeachDay

@Composable
fun ContaScreen(
    modifier: Modifier = Modifier,
    viewModel: ContaViewModel = viewModel()
) {
    val email by viewModel.email.collectAsState()
    val aCarregar by viewModel.aCarregar.collectAsState()
    val mensagem by viewModel.mensagem.collectAsState()
    val cores = BeachDay.cores

    EcraBeachDay(
        modifier = modifier,
        cabecalho = {
            Text("Conta", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Opcional. Guarda as favoritas, as visitas e os perfis na nuvem.",
                style = MaterialTheme.typography.bodyMedium,
                color = cores.topoTextoSecundario
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(PaddingFolha),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val emailAtual = email
            if (emailAtual == null) {
                FormularioEntrada(viewModel, aCarregar)
            } else {
                SessaoIniciada(emailAtual, viewModel, aCarregar)
            }

            mensagem?.let { texto ->
                Aviso(icone = Icons.Outlined.Info, cor = cores.azul, texto = texto)
            }
        }
    }
}

@Composable
private fun EstadoNuvem(icone: ImageVector, titulo: String, subtitulo: String, ativo: Boolean) {
    val cores = BeachDay.cores
    val cor = if (ativo) cores.positivo else cores.textoSecundario

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(cor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = cores.texto)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = cores.textoSecundario)
        }
    }
}

@Composable
private fun FormularioEntrada(viewModel: ContaViewModel, aCarregar: Boolean) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verPassword by remember { mutableStateOf(false) }
    val cores = BeachDay.cores

    Cartao {
        EstadoNuvem(
            icone = Icons.Outlined.CloudOff,
            titulo = "Sem conta",
            subtitulo = "Os teus dados estão só neste telemóvel.",
            ativo = false
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { verPassword = !verPassword }) {
                    Icon(
                        imageVector = if (verPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (verPassword) "Esconder password" else "Mostrar password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotaoPrincipal(
                texto = if (aCarregar) "A entrar..." else "Entrar",
                onClick = { viewModel.entrar(email, password) },
                ativo = !aCarregar,
                modifier = Modifier.weight(1f)
            )
            BotaoSecundario(
                texto = "Criar conta",
                onClick = { viewModel.criarConta(email, password) },
                ativo = !aCarregar,
                modifier = Modifier.weight(1f)
            )
        }

        TextButton(
            onClick = { viewModel.recuperarPassword(email) },
            enabled = !aCarregar,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Esqueci-me da password", color = cores.textoSecundario)
        }
    }
}

@Composable
private fun SessaoIniciada(email: String, viewModel: ContaViewModel, aCarregar: Boolean) {
    Cartao {
        EstadoNuvem(
            icone = Icons.Outlined.CloudDone,
            titulo = "Cópia na nuvem ativa",
            subtitulo = email,
            ativo = true
        )
        Text(
            text = "As alterações às favoritas, visitas e perfis são enviadas automaticamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = BeachDay.cores.textoSecundario
        )
        BotaoPrincipal(
            texto = if (aCarregar) "A sincronizar..." else "Sincronizar agora",
            onClick = { viewModel.sincronizar() },
            ativo = !aCarregar,
            modifier = Modifier.fillMaxWidth()
        )
        BotaoSecundario(
            texto = "Terminar sessão",
            onClick = { viewModel.terminarSessao() },
            ativo = !aCarregar,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
