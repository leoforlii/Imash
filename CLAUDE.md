# Imash – istruzioni per Claude

App Android **Imash** (`com.imash.app`) che fa mashup di canzoni, offline sul telefono, partendo
dagli script `mashup.py` ed `estrai_ritornelli.py`. Usata da 2-3 persone.
La storia completa di ogni attività e decisione è in [PROGRESSI.md](PROGRESSI.md): leggila prima
di iniziare a lavorare.

## Regole di lavoro (obbligatorie, chieste dall'utente)

1. **Chiedere conferma prima di qualunque azione** (codice, installazioni, push, scelte di design).
2. **Mai codice ripetuto**: prima di scrivere una funzione, cercare se esiste già nel progetto
   e riusarla.
3. **Registrare ogni singola attività in `PROGRESSI.md`** (nuova voce numerata in fondo: cosa è
   stato fatto, decisioni dell'utente, file toccati, test, problemi).
4. **Scrivere sempre in italiano** con l'utente, anche i messaggi brevi di stato.

## Specifiche decise

- **Tecnologia**: Kotlin + Jetpack Compose + Material 3; minSdk 26, compile/targetSdk 37;
  LAME 3.100 via NDK (JNI) per l'mp3. Tutto offline, nessun server.
- **Ingresso**: qualunque audio (`audio/*`: mp3, m4a…), scelta multipla. **Uscita**: sempre mp3
  CBR 320 kbps, 44,1 kHz stereo, salvato con il "Salva con nome" di sistema (SAF).
- **Due modalità**: "Canzoni intere" e "Ritornelli" (estrae il ritornello, finestra di 60 s, poi unisce).
- **Durata del mashup**: scelta nelle Impostazioni, slider 3-30 min, predefinita 10 (DataStore).
  Fino a +20% della durata si accorcia sulle misure; oltre: avviso **"Troppe canzoni"**
  (vale anche per i Ritornelli).
- **Ordine dei brani** modificabile trascinandoli.
- **Elaborazione in background** con foreground service e notifica.
- **Mashup** (porting di `mashup.py`): silenzio tagliato a -50 dB, volume RMS 0,12, picco finale 0,95,
  fade-in 1 s / fade-out 4 s, tagli a **fine frase** (8 misure) con ripiego sulle misure, brano
  entrante sul **battere**.
- **Transizioni** scelte in automatico per ogni coppia (poi modificabili dall'utente nella fase 5):
  **Mix a tempo** (tonalità compatibili secondo Camelot e battiti allineati: 8 o 4 misure) oppure
  **Scambio dei bassi** (4 misure, bassi sotto 180 Hz scambiati a metà). Dissolvenza semplice se
  mancano i battiti.
- **Design** approvato in `design/`: Petrol `#0E6378` + Magenta `#B3266F`, font Bricolage Grotesque
  + Figtree, tema chiaro e scuro.
- La cartella `canzoni/` (brani di prova) **non va mai nella repository**.

## Fasi

1. Scheletro, tema, navigazione ✅
2. Lettura audio, ricampionamento, LAME, Salva con nome ✅
3. Battiti, pianificazione della durata, unione con dissolvenze ✅
   - Transizioni per coppia di brani: scritte e testate, **prova sull'emulatore da rifare** (voce 29)
4. Ritornelli (croma + porting di pychorus, ripiego sull'energia) – da confermare con l'utente
5. Interfaccia completa del design (schermate, trascinamento, Impostazioni, servizio con notifica)
6. Prove su telefono vero, controllo prestazioni in release, scansione di sicurezza

## Comandi

Da `android/` (in Git Bash, con `JAVA_HOME` = JBR di Android Studio):

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

`android/local.properties` (percorso dell'SDK) non è nella repo: Android Studio lo crea da solo.
