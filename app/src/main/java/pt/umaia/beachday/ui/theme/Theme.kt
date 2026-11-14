package pt.umaia.beachday.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalCores = staticCompositionLocalOf { CoresClaras }

// Acesso às cores da app: BeachDay.cores.fundo, BeachDay.cores.cartao, ...
object BeachDay {
    val cores: CoresBeachDay
        @Composable
        @ReadOnlyComposable
        get() = LocalCores.current
}

@Composable
fun BeachDayTheme(
    escuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val cores = if (escuro) CoresEscuras else CoresClaras

    val esquema = if (escuro) {
        darkColorScheme(
            primary = cores.destaque,
            onPrimary = Color.White,
            secondary = cores.azul,
            background = cores.fundo,
            onBackground = cores.texto,
            surface = cores.cartao,
            onSurface = cores.texto,
            surfaceVariant = cores.bloco,
            onSurfaceVariant = cores.textoSecundario,
            surfaceContainer = cores.cartao,
            surfaceContainerHigh = cores.cartao,
            surfaceContainerHighest = cores.bloco,
            secondaryContainer = cores.segmentoAtivo,
            onSecondaryContainer = Color.White,
            outline = cores.linhaForte,
            outlineVariant = cores.linha,
            error = cores.negativo
        )
    } else {
        lightColorScheme(
            primary = cores.destaque,
            onPrimary = Color.White,
            secondary = cores.azul,
            background = cores.fundo,
            onBackground = cores.texto,
            surface = cores.cartao,
            onSurface = cores.texto,
            surfaceVariant = cores.bloco,
            onSurfaceVariant = cores.textoSecundario,
            surfaceContainer = cores.cartao,
            surfaceContainerHigh = cores.cartao,
            surfaceContainerHighest = cores.bloco,
            secondaryContainer = cores.chip,
            onSecondaryContainer = cores.texto,
            outline = cores.linhaForte,
            outlineVariant = cores.linha,
            error = cores.negativo
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                // O topo é sempre azul-escuro, por isso os ícones da barra de estado são claros
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !escuro
            }
        }
    }

    CompositionLocalProvider(LocalCores provides cores) {
        MaterialTheme(
            colorScheme = esquema,
            typography = Tipografia,
            content = content
        )
    }
}
