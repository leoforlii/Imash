package com.imash.app.dsp

/** Tonalità di un brano: [tonica] 0 = Do … 11 = Si. */
data class Tonalita(val tonica: Int, val minore: Boolean) {

    /** Numero sulla ruota di Camelot (1-12): le minori sono "A", le maggiori "B" (Do magg = 8B). */
    val camelot: Int
        get() {
            val maggiore = if (minore) (tonica + 3) % 12 else tonica   // relativa maggiore
            return (maggiore * 7 + 7) % 12 + 1
        }

    /**
     * Compatibili per il mix armonico (regole di Camelot): stessa tonalità, relativa
     * maggiore/minore, oppure un passo sulla ruota (una quinta sopra o sotto).
     */
    fun compatibileCon(altra: Tonalita): Boolean {
        val d = (camelot - altra.camelot).mod(12)
        return if (minore == altra.minore) d == 0 || d == 1 || d == 11 else d == 0
    }

    override fun toString() = NOMI[tonica] + if (minore) " min" else " magg"

    companion object {
        private val NOMI = listOf("Do", "Do#", "Re", "Mib", "Mi", "Fa", "Fa#", "Sol", "Lab", "La", "Sib", "Si")

        // Profili di Krumhansl-Kessler (tonica in posizione 0).
        private val MAGGIORE = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
        private val MINORE = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)

        /**
         * Algoritmo di Krumhansl-Schmuckler: la tonalità il cui profilo (ruotato sulla tonica)
         * è più correlato al croma medio del brano. null se il croma è piatto (silenzio, rumore).
         */
        fun stima(cromaMedio: DoubleArray): Tonalita? {
            var migliore: Tonalita? = null
            var massimo = Double.NEGATIVE_INFINITY
            for (tonica in 0 until 12) for (minore in listOf(false, true)) {
                val profilo = if (minore) MINORE else MAGGIORE
                val r = correlazione(cromaMedio) { profilo[(it - tonica).mod(12)] }
                if (r > massimo) {
                    massimo = r
                    migliore = Tonalita(tonica, minore)
                }
            }
            return if (massimo.isNaN() || massimo == Double.NEGATIVE_INFINITY) null else migliore
        }

        private fun correlazione(x: DoubleArray, y: (Int) -> Double): Double {
            val mx = x.average()
            val my = (0 until 12).sumOf(y) / 12
            var sxy = 0.0
            var sxx = 0.0
            var syy = 0.0
            for (i in 0 until 12) {
                val a = x[i] - mx
                val b = y(i) - my
                sxy += a * b
                sxx += a * a
                syy += b * b
            }
            return if (sxx == 0.0) Double.NaN else sxy / kotlin.math.sqrt(sxx * syy)
        }
    }
}
