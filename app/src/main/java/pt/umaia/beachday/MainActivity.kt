package pt.umaia.beachday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pt.umaia.beachday.ui.BeachDayApp
import pt.umaia.beachday.ui.theme.BeachDayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeachDayTheme {
                BeachDayApp()
            }
        }
    }
}