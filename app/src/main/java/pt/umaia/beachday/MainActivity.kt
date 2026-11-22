package pt.umaia.beachday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import pt.umaia.beachday.data.GestorContas
import pt.umaia.beachday.data.Preferencias
import pt.umaia.beachday.data.Tema
import pt.umaia.beachday.ui.BeachDayApp
import pt.umaia.beachday.ui.theme.BeachDayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GestorContas.iniciar(this)
        val preferencias = Preferencias.obter(this)

        setContent {
            val tema by preferencias.tema.collectAsState()
            val escuro = when (tema) {
                Tema.SISTEMA -> isSystemInDarkTheme()
                Tema.CLARO -> false
                Tema.ESCURO -> true
            }

            BeachDayTheme(escuro = escuro) {
                BeachDayApp()
            }
        }
    }
}
