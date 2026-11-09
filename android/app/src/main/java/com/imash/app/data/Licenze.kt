package com.imash.app.data

import androidx.annotation.RawRes
import com.imash.app.R

/** Un componente di terze parti incluso nell'app, con la sua licenza (testo completo in res/raw). */
data class Componente(
    val nome: String,
    val autori: String,
    val licenza: String,
    @param:RawRes val testo: Int,
    val sito: String,
    val nota: String? = null,
)

object Licenze {

    /** Codice sorgente dell'app, compresi LAME e il ponte JNI che lo usa (richiesto dalla LGPL). */
    const val CODICE_SORGENTE = "https://github.com/leoforlii/Imash"

    val componenti = listOf(
        Componente(
            nome = "LAME 3.100",
            autori = "The LAME Project",
            licenza = "GNU LGPL versione 2",
            testo = R.raw.licenza_lgpl,
            sito = "https://lame.sourceforge.io",
            nota = "Codifica gli mp3. È inclusa senza modifiche. Il suo codice sorgente e quello della parte " +
                "dell'app che la usa (cartella android/app/src/main/cpp) sono su $CODICE_SORGENTE: puoi " +
                "ricompilare la libreria con una tua versione di LAME.",
        ),
        Componente(
            nome = "AndroidX e Jetpack Compose",
            autori = "The Android Open Source Project",
            licenza = "Apache 2.0",
            testo = R.raw.licenza_apache2,
            sito = "https://developer.android.com/jetpack/androidx",
        ),
        Componente(
            nome = "Kotlin e kotlinx.coroutines",
            autori = "JetBrains s.r.o. e collaboratori",
            licenza = "Apache 2.0",
            testo = R.raw.licenza_apache2,
            sito = "https://github.com/Kotlin/kotlinx.coroutines",
        ),
        Componente(
            nome = "Reorderable",
            autori = "Calvin Liang",
            licenza = "Apache 2.0",
            testo = R.raw.licenza_apache2,
            sito = "https://github.com/Calvin-LL/Reorderable",
        ),
        Componente(
            nome = "Material Symbols (icone)",
            autori = "Google",
            licenza = "Apache 2.0",
            testo = R.raw.licenza_apache2,
            sito = "https://fonts.google.com/icons",
        ),
        Componente(
            nome = "Bricolage Grotesque (carattere)",
            autori = "The Bricolage Grotesque Project Authors",
            licenza = "SIL Open Font License 1.1",
            testo = R.raw.licenza_ofl_bricolage,
            sito = "https://github.com/ateliertriay/bricolage",
        ),
        Componente(
            nome = "Figtree (carattere)",
            autori = "The Figtree Project Authors",
            licenza = "SIL Open Font License 1.1",
            testo = R.raw.licenza_ofl_figtree,
            sito = "https://github.com/erikdkennedy/figtree",
        ),
    )
}
