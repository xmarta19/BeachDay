package pt.umaia.beachday.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@Composable
fun BeachDayApp() {
    val navController = rememberNavController()
    val praiasViewModel: PraiasViewModel = viewModel()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route

    fun abrirSeparador(destino: String) {
        navController.navigate(destino) {
            popUpTo("praias") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = rotaAtual == "praias",
                    onClick = { abrirSeparador("praias") },
                    icon = { Text("☀") },
                    label = { Text("Hoje") }
                )
                NavigationBarItem(
                    selected = rotaAtual == "favoritas",
                    onClick = { abrirSeparador("favoritas") },
                    icon = { Text("★") },
                    label = { Text("Favoritas") }
                )
                NavigationBarItem(
                    selected = rotaAtual == "historico",
                    onClick = { abrirSeparador("historico") },
                    icon = { Text("≡") },
                    label = { Text("Histórico") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "praias",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("praias") {
                ListaPraiasScreen(
                    onAbrirPraia = { id -> navController.navigate("praia/$id") },
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