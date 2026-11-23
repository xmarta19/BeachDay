package pt.umaia.beachday.ui.conta

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pt.umaia.beachday.R
import pt.umaia.beachday.ui.comum.AvatarPessoa
import pt.umaia.beachday.ui.comum.Aviso
import pt.umaia.beachday.ui.comum.Cartao
import pt.umaia.beachday.ui.comum.Etiqueta
import pt.umaia.beachday.ui.theme.BeachDay

/**
 * Painel de contas que abre ao tocar na foto do topo.
 * Todas as contas aparecem da mesma forma, numa lista:
 * toca numa conta para mudar para ela, e no × para a tirar da lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelContas(viewModel: ContaViewModel) {
    val aberto by viewModel.painelAberto.collectAsState()
    if (!aberto) return

    val estado by viewModel.estado.collectAsState()
    val contas by viewModel.contasGuardadas.collectAsState()
    val aCarregar by viewModel.aCarregar.collectAsState()
    val mensagem by viewModel.mensagem.collectAsState()
    val context = LocalContext.current
    val cores = BeachDay.cores
    var confirmarApagar by remember { mutableStateOf(false) }
    var opcoesAbertas by remember { mutableStateOf(false) }

    val email = estado.email
    val outrasContas = contas.filter { it.email != email }

    ModalBottomSheet(
        onDismissRequest = { viewModel.fecharPainel() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cores.fundo,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---------- Conta atual (em destaque; toca para ver as opções) ----------
            if (email != null) {
                Cartao(preenchimento = PaddingValues(0.dp), espaco = 0.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { opcoesAbertas = !opcoesAbertas }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AvatarPessoa(foto = estado.foto, texto = estado.nome ?: email, tamanho = 56.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = estado.nome?.takeIf { it.isNotBlank() } ?: email.substringBefore("@"),
                                style = MaterialTheme.typography.titleLarge,
                                color = cores.texto,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (estado.temGoogle) LogoGoogle(14.dp)
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cores.textoSecundario,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cores.bloco),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (opcoesAbertas) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = if (opcoesAbertas) "Esconder opções" else "Mostrar opções",
                                tint = cores.texto
                            )
                        }
                    }

                    if (opcoesAbertas) {
                        HorizontalDivider(color = cores.linha)
                        if (estado.temPassword && !estado.emailVerificado) {
                            LinhaAcao(
                                icone = Icons.Outlined.MarkEmailUnread,
                                texto = "Reenviar email de confirmação",
                                ativo = !aCarregar
                            ) { viewModel.reenviarVerificacao() }
                            Separador()
                            LinhaAcao(
                                icone = Icons.Outlined.Check,
                                texto = "Já confirmei o email",
                                ativo = !aCarregar
                            ) { viewModel.jaVerifiquei() }
                            Separador()
                        }
                        LinhaAcao(icone = Icons.Outlined.Sync, texto = "Sincronizar agora", ativo = !aCarregar) {
                            viewModel.sincronizar()
                        }
                        Separador()
                        LinhaAcao(
                            icone = Icons.AutoMirrored.Outlined.Logout,
                            texto = "Terminar sessão",
                            ativo = !aCarregar
                        ) { viewModel.terminarSessao() }
                        Separador()
                        LinhaAcao(
                            icone = Icons.Outlined.DeleteOutline,
                            texto = "Apagar conta",
                            ativo = !aCarregar,
                            cor = cores.negativo
                        ) { confirmarApagar = true }
                    }
                }
            } else {
                Cartao(preenchimento = PaddingValues(vertical = 4.dp), espaco = 0.dp) {
                    LinhaConvidado()
                    HorizontalDivider(color = cores.linha)
                    LinhaAcao(icone = Icons.AutoMirrored.Outlined.Login, texto = "Iniciar sessão", ativo = !aCarregar) {
                        viewModel.abrirFolha(Folha.ENTRAR)
                    }
                    Separador()
                    LinhaAcao(icone = Icons.Outlined.PersonAdd, texto = "Criar conta", ativo = !aCarregar) {
                        viewModel.abrirFolha(Folha.CRIAR)
                    }
                }
            }

            mensagem?.let { Aviso(icone = Icons.Outlined.Info, cor = cores.azul, texto = it) }

            // ---------- Outras contas ----------
            if (outrasContas.isNotEmpty()) {
                Cartao(preenchimento = PaddingValues(vertical = 4.dp), espaco = 0.dp) {
                    outrasContas.forEachIndexed { indice, conta ->
                        if (indice > 0) HorizontalDivider(color = cores.linha)
                        LinhaConta(
                            conta = conta,
                            ativo = !aCarregar,
                            onEntrar = { viewModel.mudarParaConta(context, conta) },
                            onRemover = { viewModel.removerConta(conta) }
                        )
                    }
                }
            }

            // ---------- Só duas ações gerais, como na Google ----------
            if (email != null || outrasContas.isNotEmpty()) {
                Cartao(preenchimento = PaddingValues(vertical = 4.dp), espaco = 0.dp) {
                    if (email != null) {
                        LinhaAcao(icone = Icons.Outlined.PersonAdd, texto = "Adicionar outra conta", ativo = !aCarregar) {
                            viewModel.abrirFolha(Folha.ENTRAR)
                        }
                        Separador()
                    }
                    LinhaAcao(
                        icone = Icons.AutoMirrored.Outlined.Logout,
                        texto = "Terminar sessão em todas as contas",
                        ativo = !aCarregar && (email != null || outrasContas.any { !it.sessaoTerminada })
                    ) { viewModel.terminarTodas() }
                }
            }
        }
    }

    if (confirmarApagar) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Apagar conta?") },
            text = {
                Text(
                    "A conta, a cópia na nuvem e os dados desta conta no telemóvel " +
                        "são apagados para sempre."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarApagar = false
                        viewModel.apagarConta()
                    }
                ) {
                    Text("Apagar", color = cores.negativo)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarApagar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Outra conta da lista.
 * - Sessão aberta (só mudaste de conta): toca na linha para voltar a ela.
 * - Sessão terminada: mostra "Iniciar sessão" e "Remover", como na Google.
 */
@Composable
private fun LinhaConta(
    conta: ContaGuardada,
    ativo: Boolean,
    onEntrar: () -> Unit,
    onRemover: () -> Unit
) {
    val cores = BeachDay.cores
    val base = Modifier.fillMaxWidth()
    val toque = if (!conta.sessaoTerminada) base.clickable(enabled = ativo, role = Role.Button, onClick = onEntrar) else base

    Column(toque.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarPessoa(foto = conta.foto, texto = conta.nome ?: conta.email, tamanho = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = conta.nome?.takeIf { it.isNotBlank() } ?: conta.email.substringBefore("@"),
                    style = MaterialTheme.typography.titleSmall,
                    color = cores.texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (conta.google) LogoGoogle(14.dp)
                    Text(
                        text = conta.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.textoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (conta.sessaoTerminada) {
                Etiqueta("Sessão terminada")
            }
        }

        if (conta.sessaoTerminada) {
            Row(
                modifier = Modifier.padding(start = 52.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotaoPequeno("Iniciar sessão", principal = true, ativo = ativo, onClick = onEntrar)
                BotaoPequeno("Remover", principal = false, ativo = ativo, onClick = onRemover)
            }
        }
    }
}

@Composable
private fun BotaoPequeno(texto: String, principal: Boolean, ativo: Boolean, onClick: () -> Unit) {
    val cores = BeachDay.cores
    val forma = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(forma)
            .then(
                if (principal) Modifier.background(cores.destaque)
                else Modifier.border(1.dp, cores.linhaForte, forma)
            )
            .clickable(enabled = ativo, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = if (principal) Color.White else cores.texto
        )
    }
}

@Composable
private fun LinhaConvidado() {
    val cores = BeachDay.cores

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(cores.bloco),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = cores.textoSecundario)
        }
        Column(Modifier.weight(1f)) {
            Text("Convidado", style = MaterialTheme.typography.titleSmall, color = cores.texto)
            Text(
                "Os dados estão só neste telemóvel.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.textoSecundario
            )
        }
    }
}

@Composable
private fun LinhaAcao(
    texto: String,
    ativo: Boolean,
    icone: ImageVector? = null,
    logo: (@Composable () -> Unit)? = null,
    cor: Color? = null,
    onClick: () -> Unit
) {
    val cores = BeachDay.cores
    val corTexto = if (!ativo) cores.textoSecundario else cor ?: cores.texto

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = ativo, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                logo != null -> logo()
                icone != null -> Icon(icone, contentDescription = null, tint = corTexto, modifier = Modifier.size(22.dp))
            }
        }
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = corTexto)
    }
}

@Composable
private fun Separador() {
    HorizontalDivider(color = BeachDay.cores.linha, modifier = Modifier.padding(start = 54.dp))
}

/**
 * Logótipo "G" oficial da Google (res/drawable/logo_google.png, do pacote oficial
 * "Sign in with Google"). O ficheiro já traz o círculo branco que a Google exige.
 */
@Composable
fun LogoGoogle(tamanho: Dp) {
    Image(
        painter = painterResource(R.drawable.logo_google),
        contentDescription = null,
        modifier = Modifier.size(tamanho)
    )
}
