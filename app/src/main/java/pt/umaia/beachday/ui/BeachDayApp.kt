package pt.umaia.beachday.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.umaia.beachday.ui.theme.BeachDay

@Composable
fun BeachDayApp() {
    val navController = rememberNavController()
    val praiasViewModel: PraiasViewModel = viewModel()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route

    fun abrirSeparador(destino: String) {
        navController.navigate(destino) {
            popUpTo("dias") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun voltar() {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = BeachDay.cores.fundo,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavegacaoInferior(rotaAtual = rotaAtual, onAbrir = { abrirSeparador(it) })
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dias",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dias") {
                DiasScreen(
                    onEscolherDia = { navController.navigate("praias") },
                    onAbrirPerfil = { abrirSeparador("perfil") },
                    viewModel = praiasViewModel
                )
            }

            composable("praias") {
                ListaPraiasScreen(
                    onAbrirPraia = { id -> navController.navigate("praia/$id") },
                    onMudarDia = { voltar() },
                    viewModel = praiasViewModel
                )
            }

            composable("favoritas") {
                FavoritasScreen(
                    onAbrirPraia = { id -> navController.navigate("praia/$id") },
                    viewModel = praiasViewModel
                )
            }

            composable("historico") {
                HistoricoScreen(viewModel = praiasViewModel)
            }

            composable("perfil") {
                PerfilScreen(viewModel = praiasViewModel)
            }

            composable("conta") {
                ContaScreen()
            }

            composable(
                route = "praia/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entrada ->
                DetalhePraiaScreen(
                    idPraia = entrada.arguments?.getInt("id") ?: 0,
                    viewModel = praiasViewModel,
                    onVoltar = {
                        if (navController.currentDestination?.route == "praia/{id}") {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
