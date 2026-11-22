package pt.umaia.beachday.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.ui.theme.BeachDay

/**
 * Folha que sobe do fundo do ecrã para iniciar sessão ou criar conta.
 * - ENTRAR / CRIAR: Google ou email e password.
 * - CONTA: voltar a uma conta conhecida. Só pede a password dessa conta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaConta(viewModel: ContaViewModel) {
    val folha by viewModel.folha.collectAsState()
    val aberta = folha ?: return

    val aCarregar by viewModel.aCarregar.collectAsState()
    val mensagem by viewModel.mensagem.collectAsState()
    val contaAlvo by viewModel.contaAlvo.collectAsState()
    val context = LocalContext.current
    val cores = BeachDay.cores

    val alvo = contaAlvo.takeIf { aberta == Folha.CONTA }
    var email by rememberSaveable(aberta, alvo?.email) { mutableStateOf(alvo?.email.orEmpty()) }
    var password by rememberSaveable(aberta, alvo?.email) { mutableStateOf("") }
    var verPassword by remember { mutableStateOf(false) }
    val criar = aberta == Folha.CRIAR

    ModalBottomSheet(
        onDismissRequest = { viewModel.fecharFolha() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cores.fundo,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (criar) "Criar conta" else "Iniciar sessão",
                style = MaterialTheme.typography.headlineSmall,
                color = cores.texto
            )

            if (alvo != null) {
                // ---------- Voltar a uma conta conhecida ----------
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AvatarPessoa(foto = alvo.foto, texto = alvo.nome ?: alvo.email, tamanho = 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = alvo.nome ?: alvo.email.substringBefore("@"),
                            style = MaterialTheme.typography.titleSmall,
                            color = cores.texto
                        )
                        Text(
                            text = alvo.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.textoSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // ---------- Google ou email ----------
                OutlinedButton(
                    onClick = { viewModel.entrarComGoogle(context) },
                    enabled = !aCarregar,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, cores.linhaForte),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LogoGoogle(20.dp)
                    Text(
                        text = "Continuar com Google",
                        style = MaterialTheme.typography.labelLarge,
                        color = cores.texto,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = cores.linhaForte)
                    Text("ou com email", style = MaterialTheme.typography.bodySmall, color = cores.textoSecundario)
                    HorizontalDivider(Modifier.weight(1f), color = cores.linhaForte)
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    // Deixa o Gestor de Passwords da Google sugerir as contas guardadas
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.EmailAddress }
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                supportingText = if (criar) {
                    { Text("Pelo menos 6 caracteres") }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { verPassword = !verPassword }) {
                        Icon(
                            imageVector = if (verPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (verPassword) "Esconder password" else "Mostrar password"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Password }
            )

            mensagem?.let { texto ->
                Aviso(icone = Icons.Outlined.Info, cor = cores.azul, texto = texto)
            }

            BotaoPrincipal(
                texto = when {
                    aCarregar && criar -> "A criar conta..."
                    aCarregar -> "A iniciar sessão..."
                    criar -> "Criar conta"
                    else -> "Iniciar sessão"
                },
                onClick = {
                    if (criar) viewModel.criarConta(context, email, password)
                    else viewModel.entrarComEmail(context, email, password)
                },
                ativo = !aCarregar,
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Ligações de baixo ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                when {
                    alvo != null -> TextButton(onClick = { viewModel.abrirFolha(Folha.ENTRAR) }) {
                        Text("Usar outra conta", color = cores.textoSecundario)
                    }
                    criar -> TextButton(onClick = { viewModel.abrirFolha(Folha.ENTRAR) }) {
                        Text("Já tens conta? Iniciar sessão", color = cores.textoSecundario)
                    }
                    else -> TextButton(onClick = { viewModel.abrirFolha(Folha.CRIAR) }) {
                        Text("Criar conta", color = cores.textoSecundario)
                    }
                }
                if (!criar) {
                    TextButton(
                        onClick = { viewModel.recuperarPassword(email) },
                        enabled = !aCarregar
                    ) {
                        Text("Esqueci-me da password", color = cores.textoSecundario)
                    }
                }
            }
        }
    }
}
