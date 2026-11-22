package pt.umaia.beachday.ui

import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.umaia.beachday.data.GestorContas
import pt.umaia.beachday.ui.theme.BeachDay

private const val ROTA_BOAS_VINDAS = "boasvindas"

// Ordem dos separadores da barra de baixo (para deslizar e para as animações)
private val ordemSeparadores = listOf("dias", "favoritas", "historico", "perfil", "definicoes")

private fun indiceSeparador(rota: String?): Int = when (rota) {
    "praias", "praia/{id}" -> 0
    else -> ordemSeparadores.indexOf(rota)
}

// Só se desliza entre separadores nestes ecrãs (no detalhe o mapa usa o gesto)
private val rotasComDeslizar = setOf("dias", "praias", "favoritas", "historico", "perfil", "conta")

@Composable
fun BeachDayApp() {
    val navController = rememberNavController()
    val praiasViewModel: PraiasViewModel = viewModel()
    val contaViewModel: ContaViewModel = viewModel()
    val avisos = remember { SnackbarHostState() }
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route
    val context = LocalContext.current
    val preferencias = remember { context.getSharedPreferences("beachday", Context.MODE_PRIVATE) }

    // O ecrã de boas-vindas só aparece na primeira vez (e se não houver sessão iniciada)
    val rotaInicial = remember {
        val jaViu = preferencias.getBoolean("boasVindasVistas", false)
        GestorContas.iniciar(context)
        val temSessao = GestorContas.authAtiva?.currentUser != null
        if (jaViu || temSessao) "dias" else ROTA_BOAS_VINDAS
    }

    fun sairDasBoasVindas() {
        preferencias.edit { putBoolean("boasVindasVistas", true) }
        navController.navigate("dias") {
            popUpTo(ROTA_BOAS_VINDAS) { inclusive = true }
        }
    }

    fun abrirSeparador(destino: String) {
        navController.navigate(destino) {
            popUpTo("dias") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val distanciaMinima = with(LocalDensity.current) { 80.dp.toPx() }

    // Mensagens da conta ("Sessão iniciada como...") aparecem numa barra no fundo
    // quando o painel e a folha já estão fechados.
    // A barra corre num escopo próprio: limpar a mensagem não a pode cancelar.
    val escopo = rememberCoroutineScope()
    val mensagemConta by contaViewModel.mensagem.collectAsState()
    val painelAberto by contaViewModel.painelAberto.collectAsState()
    val folhaAberta by contaViewModel.folha.collectAsState()
    LaunchedEffect(mensagemConta, painelAberto, folhaAberta) {
        val texto = mensagemConta
        if (texto != null && !painelAberto && folhaAberta == null) {
            contaViewModel.mensagem.value = null
            escopo.launch { avisos.showSnackbar(texto) }
        }
    }

    val abrirConta = { contaViewModel.abrirPainel() }

    fun voltar() {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = BeachDay.cores.fundo,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(avisos) },
        bottomBar = {
            if (rotaAtual != null && rotaAtual != ROTA_BOAS_VINDAS) {
                NavegacaoInferior(rotaAtual = rotaAtual, onAbrir = { abrirSeparador(it) })
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = rotaInicial,
            modifier = Modifier
                .padding(innerPadding)
                .pointerInput(rotaAtual) {
                    if (rotaAtual !in rotasComDeslizar) return@pointerInput
                    var deslocamento = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { deslocamento = 0f },
                        onHorizontalDrag = { _, dx -> deslocamento += dx },
                        onDragEnd = {
                            val atual = indiceSeparador(rotaAtual)
                            when {
                                // Dedo para a esquerda: separador seguinte
                                deslocamento < -distanciaMinima && atual < ordemSeparadores.lastIndex ->
                                    abrirSeparador(ordemSeparadores[atual + 1])
                                // Dedo para a direita: separador anterior
                                deslocamento > distanciaMinima && atual > 0 ->
                                    abrirSeparador(ordemSeparadores[atual - 1])
                            }
                        }
                    )
                },
            enterTransition = {
                val de = indiceSeparador(initialState.destination.route)
                val para = indiceSeparador(targetState.destination.route)
                if (de >= 0 && para >= 0 && de != para) {
                    slideInHorizontally(tween(250)) { largura -> if (para > de) largura else -largura }
                } else {
                    fadeIn(tween(200))
                }
            },
            exitTransition = {
                val de = indiceSeparador(initialState.destination.route)
                val para = indiceSeparador(targetState.destination.route)
                if (de >= 0 && para >= 0 && de != para) {
                    slideOutHorizontally(tween(250)) { largura -> if (para > de) -largura else largura }
                } else {
                    fadeOut(tween(200))
                }
            }
        ) {
            composable(ROTA_BOAS_VINDAS) {
                BoasVindasScreen(
                    onConvidado = { sairDasBoasVindas() },
                    onSessaoIniciada = { sairDasBoasVindas() },
                    viewModel = contaViewModel
                )
            }

            composable("dias") {
                DiasScreen(
                    onEscolherDia = { navController.navigate("praias") },
                    onAbrirPerfil = { abrirSeparador("perfil") },
                    onAbrirConta = abrirConta,
                    viewModel = praiasViewModel
                )
            }

            composable("praias") {
                ListaPraiasScreen(
                    onAbrirPraia = { id -> navController.navigate("praia/$id") },
                    onMudarDia = { voltar() },
                    onAbrirConta = abrirConta,
                    viewModel = praiasViewModel
                )
            }

            composable("favoritas") {
                FavoritasScreen(
                    onAbrirPraia = { id -> navController.navigate("praia/$id") },
                    onAbrirConta = abrirConta,
                    viewModel = praiasViewModel
                )
            }

            composable("historico") {
                HistoricoScreen(
                    onAbrirConta = abrirConta,
                    viewModel = praiasViewModel
                )
            }

            composable("perfil") {
                PerfilScreen(
                    onAbrirConta = abrirConta,
                    viewModel = praiasViewModel
                )
            }

            composable("definicoes") {
                DefinicoesScreen(contaViewModel = contaViewModel)
            }

            composable(
                route = "praia/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entrada ->
                DetalhePraiaScreen(
                    idPraia = entrada.arguments?.getInt("id") ?: 0,
                    viewModel = praiasViewModel,
                    onAbrirConta = abrirConta,
                    onVoltar = {
                        if (navController.currentDestination?.route == "praia/{id}") {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }

    // Painel de contas (foto do topo) e folha de entrar/criar conta, por cima de qualquer ecrã
    PainelContas(contaViewModel)
    FolhaConta(contaViewModel)
}
