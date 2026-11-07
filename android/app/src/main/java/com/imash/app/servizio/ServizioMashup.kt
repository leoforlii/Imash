package com.imash.app.servizio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.imash.app.MainActivity
import com.imash.app.R
import com.imash.app.data.Archivio
import com.imash.app.mashup.Fase
import com.imash.app.mashup.RisultatoMashup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

/**
 * Servizio in primo piano che crea il mashup di [Elaborazione], con la notifica di avanzamento:
 * così l'elaborazione continua anche a schermo spento o con l'app in secondo piano.
 */
class ServizioMashup : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var ultimaNotifica = 0L
    private var ultimaPercentuale = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stato = Elaborazione.stato.value
        val generatore = Elaborazione.generatore
        val ricomponi = stato?.ricomposizione != null
        if (stato == null || generatore == null || !(stato.inCorso || ricomponi) || Elaborazione.lavoro?.isActive == true) {
            if (Elaborazione.lavoro?.isActive != true) stopSelf()
            return START_NOT_STICKY
        }
        creaCanali()
        val tipo = when {
            Build.VERSION.SDK_INT >= 35 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            Build.VERSION.SDK_INT >= 29 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else -> 0
        }
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID_AVANZAMENTO, notificaAvanzamento(stato), tipo)
        else startForeground(ID_AVANZAMENTO, notificaAvanzamento(stato))
        NotificationManagerCompat.from(this).cancel(ID_ESITO)
        rilasciaWakeLock()   // se il servizio viene riavviato prima di onDestroy
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Imash:mashup")
            .apply { acquire(MAX_DURATA_MS) }

        Elaborazione.lavoro = scope.launch {
            val mp3 = Archivio.fileTemporaneo(this@ServizioMashup)
            // Ricomponendo si scrive a parte: se non riesce resta il mashup di prima.
            val uscita = if (ricomponi) File(mp3.parentFile, "mashup_nuovo.mp3") else mp3
            val esito = try {
                when (val r = generatore.genera(Elaborazione.durataObiettivo, uscita, Elaborazione.scelte) { passo ->
                        Elaborazione.aggiorna { it.applica(passo) }
                        registraFase()
                        aggiornaNotifica()
                    }) {
                    is RisultatoMashup.Creato -> Esito.Pronto(r)
                    is RisultatoMashup.TroppeCanzoni -> Esito.TroppeCanzoni(r.totale, r.massimo)
                }
            } catch (e: CancellationException) {
                Esito.Annullato
            } catch (e: Exception) {
                Log.e("Imash", "Mashup fallito", e)
                Esito.Errore(e.message ?: e.javaClass.simpleName)
            }
            if (ricomponi) concludiRicomposizione(esito, uscita, mp3)
            else {
                if (esito !is Esito.Pronto) {
                    uscita.delete()
                    generatore.pulisci()
                }
                Elaborazione.aggiorna { it.concluso(esito) }
                notificaEsito(esito)
            }
            Log.i("Imash", "Fine ($esito) dopo ${System.currentTimeMillis() - stato.inizio} ms")
            rilasciaWakeLock()
            ServiceCompat.stopForeground(this@ServizioMashup, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun concludiRicomposizione(esito: Esito, nuovo: File, mp3: File) {
        if (esito is Esito.Pronto && nuovo.renameTo(mp3.also { it.delete() })) {
            Elaborazione.aggiorna { it.copy(esito = esito, ricomposizione = null, salvato = false, messaggio = "Mashup aggiornato") }
            return
        }
        nuovo.delete()
        val testo = when (esito) {
            is Esito.TroppeCanzoni -> "Con questa transizione i brani superano il massimo: resta quella di prima"
            Esito.Annullato -> "Modifica annullata: resta il mashup di prima"
            else -> "Modifica non riuscita: resta il mashup di prima"
        }
        // Torna la scelta che corrisponde al mashup rimasto.
        (Elaborazione.stato.value?.esito as? Esito.Pronto)?.let { pronto ->
            Elaborazione.scelte = pronto.risultato.piano.transizioni.map { it }
        }
        Elaborazione.aggiorna { it.copy(ricomposizione = null, messaggio = testo) }
    }

    /** Android 15+: il servizio "mediaProcessing" ha un tempo massimo; se lo supera si interrompe. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Elaborazione.annulla()
    }

    override fun onDestroy() {
        scope.cancel()
        rilasciaWakeLock()
        super.onDestroy()
    }

    private fun rilasciaWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun creaCanali() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANALE_AVANZAMENTO, "Creazione del mashup", NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(CANALE_ESITO, "Mashup pronto", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun apriApp(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notificaAvanzamento(s: StatoElaborazione): Notification {
        val percentuale = (avanzamento(s) * 100).roundToInt()
        return NotificationCompat.Builder(this, CANALE_AVANZAMENTO)
            .setSmallIcon(R.drawable.ic_crea)
            .setContentTitle(if (s.ricomposizione != null) "Aggiorno il mashup" else "Sto creando il mashup")
            .setContentText(if (s.ricomposizione != null) "$percentuale%" else "Brano ${s.corrente + 1} di ${s.nomi.size} · $percentuale%")
            .setProgress(100, percentuale, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(apriApp())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /** Tempi di ogni fase nel log, per tarare i pesi di [StatoElaborazione.frazione]. */
    private var faseRegistrata: Fase? = null
    private fun registraFase() {
        val s = Elaborazione.stato.value ?: return
        if (s.fase == faseRegistrata) return
        faseRegistrata = s.fase
        Log.i("Imash", "Fase ${s.fase} dopo ${System.currentTimeMillis() - s.inizio} ms")
    }

    /** Al massimo due aggiornamenti al secondo, e solo se la percentuale è cambiata. */
    @SuppressLint("MissingPermission")
    private fun aggiornaNotifica() {
        val s = Elaborazione.stato.value ?: return
        val adesso = System.currentTimeMillis()
        val percentuale = (avanzamento(s) * 100).roundToInt()
        if (adesso - ultimaNotifica < 500 || percentuale == ultimaPercentuale) return
        ultimaNotifica = adesso
        ultimaPercentuale = percentuale
        val nm = NotificationManagerCompat.from(this)
        if (nm.areNotificationsEnabled()) nm.notify(ID_AVANZAMENTO, notificaAvanzamento(s))
    }

    @SuppressLint("MissingPermission")
    private fun notificaEsito(esito: Esito) {
        val (titolo, testo) = when (esito) {
            is Esito.Pronto -> "Mashup pronto" to "Tocca per ascoltarlo e salvarlo"
            is Esito.TroppeCanzoni -> "Troppe canzoni" to "Togli qualche brano oppure aumenta la durata"
            is Esito.Errore -> "Mashup non riuscito" to esito.messaggio
            Esito.Annullato -> return
        }
        val nm = NotificationManagerCompat.from(this)
        if (!nm.areNotificationsEnabled()) return
        nm.notify(
            ID_ESITO,
            NotificationCompat.Builder(this, CANALE_ESITO)
                .setSmallIcon(R.drawable.ic_crea)
                .setContentTitle(titolo)
                .setContentText(testo)
                .setAutoCancel(true)
                .setContentIntent(apriApp())
                .build(),
        )
    }

    private fun avanzamento(s: StatoElaborazione) = s.ricomposizione ?: s.frazione

    companion object {
        private const val CANALE_AVANZAMENTO = "avanzamento"
        private const val CANALE_ESITO = "esito"
        private const val ID_AVANZAMENTO = 1
        const val ID_ESITO = 2
        private const val MAX_DURATA_MS = 30 * 60 * 1000L
    }
}
