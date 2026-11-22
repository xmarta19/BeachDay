package pt.umaia.beachday.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Tema { SISTEMA, CLARO, ESCURO }

/** Preferências da app guardadas no telemóvel (SharedPreferences). */
class Preferencias private constructor(context: Context) {
    private val guardadas = context.getSharedPreferences("beachday", Context.MODE_PRIVATE)

    private val _tema = MutableStateFlow(
        Tema.entries.find { it.name == guardadas.getString("tema", null) } ?: Tema.SISTEMA
    )
    val tema: StateFlow<Tema> = _tema

    fun escolherTema(novo: Tema) {
        _tema.value = novo
        guardadas.edit { putString("tema", novo.name) }
    }

    companion object {
        @Volatile
        private var instancia: Preferencias? = null

        fun obter(context: Context): Preferencias =
            instancia ?: synchronized(this) {
                instancia ?: Preferencias(context.applicationContext).also { instancia = it }
            }
    }
}
