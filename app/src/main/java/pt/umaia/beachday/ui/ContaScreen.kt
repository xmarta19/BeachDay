package pt.umaia.beachday.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ContaScreen(
    modifier: Modifier = Modifier,
    viewModel: ContaViewModel = viewModel()
) {
    val email by viewModel.email.collectAsState()
    val aCarregar by viewModel.aCarregar.collectAsState()
    val mensagem by viewModel.mensagem.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Conta", style = MaterialTheme.typography.headlineSmall)

        val emailAtual = email
        if (emailAtual == null) {
            FormularioEntrada(viewModel, aCarregar)
        } else {
            SessaoIniciada(emailAtual, viewModel, aCarregar)
        }

        mensagem?.let { texto ->
            Card(Modifier.fillMaxWidth()) {
                Text(texto, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun FormularioEntrada(viewModel: ContaViewModel, aCarregar: Boolean) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text(
        text = "A conta é opcional. Com ela, as favoritas, as visitas e os perfis " +
            "ficam guardados na nuvem e podes recuperá-los noutro telemóvel.",
        style = MaterialTheme.typography.bodyMedium
    )

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { viewModel.entrar(email, password) },
            enabled = !aCarregar
        ) {
            Text(if (aCarregar) "A entrar..." else "Entrar")
        }
        OutlinedButton(
            onClick = { viewModel.criarConta(email, password) },
            enabled = !aCarregar
        ) {
            Text("Criar conta")
        }
    }

    TextButton(
        onClick = { viewModel.recuperarPassword(email) },
        enabled = !aCarregar
    ) {
        Text("Esqueci-me da password")
    }
}

@Composable
private fun SessaoIniciada(email: String, viewModel: ContaViewModel, aCarregar: Boolean) {
    Text("Sessão iniciada como $email")
    Text(
        text = "As alterações às favoritas, visitas e perfis são enviadas automaticamente.",
        style = MaterialTheme.typography.bodySmall
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { viewModel.sincronizar() },
            enabled = !aCarregar
        ) {
            Text(if (aCarregar) "A sincronizar..." else "Sincronizar agora")
        }
        OutlinedButton(
            onClick = { viewModel.terminarSessao() },
            enabled = !aCarregar
        ) {
            Text("Terminar sessão")
        }
    }
}
