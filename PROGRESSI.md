# Progressi del progetto – App Android "Mashup da mp3"

Registro di ogni attività svolta nel progetto, in ordine cronologico.

## Regole di lavoro concordate
- Niente codice ripetuto: prima di scrivere una funzione si controlla se esiste già.
- Ogni passo va confermato dall'utente prima di procedere.
- Ogni attività viene registrata in questo file.

---

## 2026-11-06

### 1. Analisi degli script esistenti
- Letti `estrai_ritornelli.py` (110 righe) e `mashup.py` (133 righe).
- `estrai_ritornelli.py`: trova il ritornello di ogni mp3 con `pychorus` + `librosa`
  (matrice di similarità sul chroma). Se non lo trova, usa il tratto con più energia.
  Esporta una finestra di 60 s in `ritornelli/<nome> - ritornello.mp3`.
- `mashup.py`: rileva i battiti (`librosa.beat.beat_track`), taglia su fine misura,
  normalizza il volume (RMS), unisce i brani con crossfade equal-power di 2 misure,
  regola le durate per arrivare a circa 10 minuti, applica fade-in/out ed esporta `mashup.mp3`.
- Dipendenze: `numpy`, `scipy`, `librosa`, `soundfile`, `pychorus`.

### 2. Ricerca di una skill di design
- Nessuna skill di design già abilitata nell'account.
- Trovati nel catalogo plugin (scheda di installazione mostrata all'utente):
  - **Mobile Design System**: design system Material 3 per app nuove (consigliata).
  - **frontend-design** (Anthropic): skill di design UI/UX generica.

### 3. Proposta di architettura (in attesa di conferma)
- Problema: `librosa` dipende da `numba`, che non gira su Android.
- Opzione A – **App Android + server Python** (consigliata): riusa gli script così come sono.
- Opzione B – **Tutto in Kotlin sul telefono**: funziona offline, ma va riscritta la logica audio.
- Decisione: **in attesa dell'utente**.

### 4. Creazione di questo file
- Creato `PROGRESSI.md` su richiesta dell'utente.

### 5. Decisione: tutto offline sul telefono
- L'utente ha installato le due skill di design. Nella sessione attuale non risultano
  ancora attive: probabilmente si caricano in una nuova sessione.
- L'utente non ha un server o un PC sempre acceso e l'app la useranno 2-3 persone,
  quindi **l'elaborazione avverrà tutta sul dispositivo, offline** (scelta l'opzione B).
- Gli script possono essere riscritti in un altro linguaggio, purché l'app permetta di:
  unire più mp3 in un mashup, oppure estrarre prima i ritornelli e poi unirli,
  restituendo alla fine un file mp3.
- Piano tecnico proposto (in attesa di conferma):
  - Kotlin + Jetpack Compose + Material 3.
  - Decodifica mp3 con `MediaCodec` (già incluso in Android).
  - Riscrittura in Kotlin di: FFT/STFT, chroma, rilevamento battiti, ricerca del
    ritornello (porting di `pychorus`), crossfade, normalizzazione e fade.
  - Codifica mp3 con LAME via NDK, perché Android non ha un encoder mp3 incluso.
  - Elaborazione a flusso, un brano alla volta, per non esaurire la memoria del telefono.

### 6. Requisiti confermati dall'utente
- **Tecnologia**: Kotlin + Jetpack Compose (Material 3) + LAME via NDK, output **mp3**. ✅
- **Durata del mashup**: fissa a **10 minuti** (tolleranza ±15 s), come in `mashup.py`. ✅
- **Modalità**:
  1. *Unisci canzoni intere*: comportamento di `mashup.py`. ✅
  2. *Ritornelli poi unisci*: `estrai_ritornelli.py`, poi `mashup.py` sui ritornelli. ✅
  - La modalità "solo estrai ritornelli" (un mp3 per ogni ritornello) **non** è richiesta.
- **Ordine dei brani**: riordinabili a mano con trascinamento. ✅

### 7. Controllo dell'ambiente di sviluppo sul PC
- **Android Studio**: non installato.
- **Android SDK / NDK / CMake**: non presenti (`ANDROID_HOME` non impostata).
- **Java (JDK)**: non trovato.
- **winget**: disponibile, utile per installare Android Studio.
- **Spazio libero su C:**: 87,4 GB, sufficiente (Android Studio + SDK + NDK ≈ 10-15 GB).
- **Python 3.14**: installato, con `librosa` e `pychorus`. Però l'import di `scipy` fallisce:
  *"Un criterio di controllo dell'applicazione ha bloccato il file"* (`_nd_image`).
  È Windows Smart App Control / App Control che blocca una DLL di scipy, quindi oggi gli
  script Python probabilmente non girano su questo PC. Non è bloccante per l'app Android,
  ma impedisce di confrontare i risultati con gli script originali.

### 8. Nuovo requisito: salvataggio dell'mp3 sul dispositivo
- L'mp3 prodotto dall'app deve poter essere scaricato/salvato nella memoria del telefono.
- Modalità di salvataggio confermata: **"Salva con nome" di sistema** (Storage Access
  Framework, `ACTION_CREATE_DOCUMENT`). Cartella e nome li sceglie l'utente; non servono
  permessi di archiviazione. ✅

### 9. Strumenti di design: Figma / Canva
- L'utente ha proposto Figma o Canva (non ancora installati).
- Nel registro connettori ci sono sia **Figma** sia **Canva** (non collegati).
- Il connettore Figma serve soprattutto a *leggere* un design già fatto in Figma e a
  trasformarlo in codice. Canva è pensato più per grafiche e locandine che per le
  schermate di un'app.
- Scelta confermata: **skill Mobile Design System** (Material 3). Figma e Canva non si usano. ✅

### 10. Installazione degli strumenti di sviluppo
- L'utente ha scelto di farmi installare Android Studio con winget
  (`winget install Google.AndroidStudio`).
- ✅ Installato **Android Studio 2026.2.2** in `C:\Program Files\Android\Android Studio`
  (hash dell'installer verificato). Il JDK è incluso (`jbr\bin\java.exe`).
- Da fare a cura dell'utente: primo avvio di Android Studio, con installazione di SDK, NDK e CMake.
- ✅ Verificato il setup fatto dall'utente in `C:\Users\leona\AppData\Local\Android\Sdk`:
  - Platform: `android-37.0`
  - Build-tools: `36.0.0`
  - NDK: `30.0.16248370`
  - CMake: `4.1.2`
  - Platform-tools (`adb`), emulatore e system image presenti; licenza SDK accettata.
- L'ambiente di sviluppo è pronto.

### 11. Prossimo passo: design
- In questa sessione le skill **Mobile Design System** e **frontend-design** non risultano
  ancora attive. Per usarle serve una nuova sessione.
- ✅ Aperta, su conferma dell'utente, la sessione **"Design app Android Mashup (Material 3)"**.
  Ha le stesse regole di lavoro e produrrà il design nella cartella `design/`.

### 12. Sessione di design: skill non disponibili
- Nella sessione "Design app Android Mashup (Material 3)" le skill **Mobile Design System**
  e **frontend-design** non risultano né tra le skill né tra i plugin attivi.
- In `C:\Users\leona\.claude\plugins` c'è solo la cache del catalogo (dove compare la
  descrizione di Mobile Design System): il plugin vero e proprio non è installato.
- Nessun file di design creato; la cartella `design/` non esiste ancora.
- Decisione dell'utente: **riattivare il plugin** (installarlo/abilitarlo di nuovo) e poi
  aprire una nuova sessione per il design, perché i plugin si caricano solo all'avvio.

### 13. Caricamento delle skill di design con `npx skills use`
- Su richiesta dell'utente, eseguiti:
  - `npx skills use "https://github.com/anthropics/skills" --skill "frontend-design"`
  - `npx skills use "https://github.com/vercel-labs/agent-skills" --skill "web-design-guidelines"`
- ✅ **frontend-design**: istruzioni lette. Chiede di proporre prima un piano di design
  (palette di 4-6 colori, caratteri, layout con wireframe, principi), di confrontarlo con
  gli stili "generici" e di farlo approvare prima di costruire.
- ✅ **web-design-guidelines** (Vercel): è una skill di *revisione*; scarica le regole da
  `vercel-labs/web-interface-guidelines` e controlla file già scritti. Si userà per
  rivedere i file del design una volta creati.
- La skill Mobile Design System resta non disponibile; il design prosegue con queste due.
- Proposta di palette e stile inviata all'utente: **in attesa di conferma**.

### 14. Installazione delle skill di Addy Osmani
- Su richiesta dell'utente, eseguito
  `npx skills add "https://github.com/addyosmani/agent-skills" -a claude-code -y`.
- ✅ Installate **25 skill** a livello di progetto in `.claude/skills/` (tra cui
  `frontend-ui-engineering`, `spec-driven-development`, `planning-and-task-breakdown`,
  `incremental-implementation`, `test-driven-development`, `debugging-and-error-recovery`,
  `performance-optimization`). Saranno disponibili come skill dalla prossima sessione.
- Le skill contengono anche due script (`constraint-driven-development/scripts/quality-gate.mjs`,
  `idea-refine/scripts/idea-refine.sh`): non eseguiti.
- Letta subito `frontend-ui-engineering`, utile al design: in più rispetto alla proposta
  chiede gli stati vuoto/errore/caricamento, contrasto 4,5:1, stato mai indicato dal solo
  colore, ombre minime, spaziature solo sulla scala dei token.

### 15. Ricerca di skill: Strix (sicurezza) e anteprima su telefono
- Ricerca fatta con `npx skills find` e nel catalogo plugin. Niente installato.
- **Strix**: il repository ufficiale è `usestrix/strix` (decine di migliaia di installazioni).
  Ci sono anche copie con 1 installazione di altri autori, da evitare.
  Skill principali: `penetration-testing-with-strix`, `find-security-vulnerabilities-in-code`,
  `application-security-testing`, `fix-security-vulnerabilities-with-strix`.
  - Strix nasce per app web e API. La nostra app è offline, senza server né rete: si
    può analizzare solo il codice (`find-security-vulnerabilities-in-code`), quando esiste.
  - Strix richiede Python, Docker e una chiave API di un modello linguistico.
  - Nel catalogo plugin c'è anche **Vantage** (analisi statica con OWASP Mobile Top 10),
    più adatto a un'app Android.
- **Anteprima su telefono**:
  - Per il design (adesso): plugin **render-visual-skill** (cornici di dispositivo in HTML,
    resi in PNG) oppure `michaelboeding/skills@device-framer` (poche installazioni).
    In alternativa le schermate HTML si possono mostrare già dentro una cornice di telefono
    Android, nel browser integrato, senza skill aggiuntive.
  - Per l'app vera (dopo): plugin **Android Emulator QA** o **adb-mcp**, oppure
    `limrun-inc/skills@limrun-android-emulator`, per avviare l'app sull'emulatore già
    installato e fare screenshot.
- Scelta: **in attesa dell'utente**.

### 16. Installazione di Strix e richiesta di Vantage
- Scelta dell'utente: **Strix** per la sicurezza e **cornice HTML** (senza skill) per l'anteprima.
- ✅ Eseguito `npx skills add usestrix/strix --skill find-security-vulnerabilities-in-code
  --skill application-security-testing --skill fix-security-vulnerabilities-with-strix
  --skill penetration-testing-with-strix -a claude-code -y`: 4 skill in `.claude/skills/`.
  Si useranno quando ci sarà il codice dell'app.
- **Vantage** è un plugin del catalogo claude.ai, non installabile con `npx`: mostrata
  all'utente la scheda di installazione. Da abilitare a cura dell'utente.

### 17. Creazione del design in `design/`
- Seguite le skill frontend-design e frontend-ui-engineering; palette e stile come proposti
  nella voce 13 (Petrolio `#0E6378` + Magenta `#B3266F`, Bricolage Grotesque + Figtree).
- ✅ Creati:
  - `design/tokens.css`: unica fonte di colori (chiaro e scuro), tipografia, spaziature, forme.
  - `design/componenti.css`: componenti costruiti solo con i token.
  - `design/catalogo.html`: token e componenti con i loro stati, in tema chiaro e scuro.
  - `design/schermate.html`: schermate in cornice di telefono 360 × 780 dp, chiaro e scuro:
    1. scelta dei brani, 1b. lista vuota, 2. elaborazione, 3. risultato.
  - `design/presentazione.css` e `.js`: solo per le pagine di presentazione. Il markup di ogni
    schermata è scritto una volta e duplicato in chiaro/scuro dallo script (niente codice ripetuto).
  - `design/README.md`: corrispondenza tra token/componenti e Jetpack Compose.
  - `.claude/launch.json`: server locale (`python -m http.server 5173 --directory design`)
    per vedere le pagine nel browser integrato.
- Verifiche nel browser:
  - contrasto ≥ 4,5:1 per tutte le 38 coppie di colori, nei due temi;
  - corretto un bug: l'icona dentro il pulsante dello stato vuoto era invisibile (stesso colore
    dello sfondo);
  - l'etichetta "Ritornelli poi unisci" andava a capo nel selettore: accorciata in
    **"Ritornelli"**, la frase sotto spiega la modalità;
  - l'avviso informativo usava un rosa troppo simile all'errore: ora usa il colore secondario.
- Design: **in attesa di approvazione dell'utente**.

### 18. Design approvato e nuovi requisiti
- ✅ L'utente ha **approvato il design**.
- Nuovi requisiti di funzionamento:
  1. **Impostazioni**: una sezione in cui scegliere la **durata del mashup**; predefinita 10 minuti.
     (Sostituisce la "durata fissa a 10 minuti" della voce 6.)
  2. **Modalità "Canzoni intere"**: se la somma dei brani supera di molto la durata scelta,
     l'app mostra un **avviso**: ci sono troppe canzoni; l'utente deve togliere/cambiare brani
     oppure aumentare la durata nelle impostazioni.
     Esempio dell'utente con 10 minuti: 3:23 + 4:12 + 1:35 + 4:40 + 3:20 = **17:10** → avviso.
- Chiarimenti confermati dall'utente:
  - **Soglia dell'avviso**: oltre il **+20%** della durata scelta. Con 10 minuti: fino a 12:00
    l'app accorcia le canzoni sulle misure (come `mashup.py`); da 12:01 in su compare l'avviso
    e "Crea mashup" resta disattivato. L'esempio (17:10) dà l'avviso. ✅
  - **Durata nelle impostazioni**: cursore da **3 a 30 minuti**, a passi di 1 minuto, con il
    valore scritto accanto; predefinito **10**. ✅
- Proposto all'utente di aggiornare il design: schermata Impostazioni e avviso "troppe
  canzoni" nella schermata di scelta dei brani. Confermato. ✅

### 19. Design aggiornato: Impostazioni e "Troppe canzoni"
- `design/schermate.html`:
  - nuova schermata **1c "Troppe canzoni"**: i 5 brani dell'esempio (17:10), avviso di errore
    con "Apri impostazioni", barra con la parte oltre 12:00 a strisce, "Crea mashup" disattivato;
  - nuova schermata **4 "Impostazioni"**: cursore della durata 3-30 minuti (10 predefinito),
    spiegazione del margine del 20%, "Ripristina 10 minuti";
  - icona **ingranaggio** nella barra in alto delle schermate dei brani (al posto di "Aggiungi
    mp3", che resta sotto la lista);
  - schermata 1 resa coerente con la nuova regola: 4 brani per 11:00, accorciati a 10:00
    (prima 6 brani per 21:05, che ora darebbero l'avviso).
- `design/componenti.css`: nuovi componenti `.limite` (parte oltre il massimo nella barra),
  `.cursore` (slider M3), `.impostazione`; pulsante nell'avviso; corretto lo sfondo del
  pulsante di testo disattivato.
- `design/catalogo.html`: aggiunti barra oltre il limite, cursore e avviso "Troppe canzoni".
- `design/README.md`: sezione "Durata del mashup e limite" e corrispondenze Compose
  (`Slider(valueRange = 3f..30f, steps = 26)`, salvataggio con DataStore).
- Verificato nel browser integrato, tema chiaro e scuro.

### 20. Verifica: dove sono installate skill e plugin
- **Solo in questo progetto** (`.claude/skills/`, elencate in `skills-lock.json`): le 25 skill
  di Addy Osmani e le 4 di Strix. Installate senza `-g`, quindi non visibili in altri progetti.
- **Su tutto l'account**: il plugin **Vantage**, abilitato su claude.ai; vale in ogni sessione.
- **Non installate**: frontend-design e web-design-guidelines, usate con `npx skills use`,
  che genera le istruzioni solo per la sessione in cui si lancia.
- Cartella globale `~/.claude/skills`: contiene solo `synced`, nessuna di queste skill.

### 21. Skill rese globali
- Su richiesta dell'utente, reinstallate con `-g` (per tutti i progetti, in `~/.claude/skills`):
  - `npx skills add "https://github.com/addyosmani/agent-skills" -g -a claude-code -y` → 25 skill;
  - `npx skills add usestrix/strix --skill … (4 skill) -g -a claude-code -y` → 4 skill.
- ✅ Verificato: `~/.claude/skills` ora contiene le 29 skill (+ `synced`). Saranno disponibili
  in ogni progetto dalla prossima sessione.
- Le copie nel progetto (`.claude/skills/`, `skills-lock.json`) sono ancora presenti:
  doppioni delle globali. Rimozione proposta all'utente e confermata. ✅
- ✅ Eseguito `npx skills remove --all` nel progetto: rimosse le 29 copie locali; poi cancellati
  la cartella vuota `.claude/skills/` e il file `skills-lock.json` rimasto vuoto.
  Nel progetto resta solo `.claude/launch.json` (server del design).
- ✅ Verificato: le 29 skill globali in `~/.claude/skills` sono ancora presenti.
- frontend-design e web-design-guidelines restano non installate.

### 22. Fine della fase di design
- L'utente conferma che il design copre tutte le funzioni richieste. ✅
- Ricordato all'utente: gli script Python restano solo come riferimento dell'algoritmo;
  l'app sarà tutta offline in Kotlin (voce 5). Nessun codice dell'app scritto finora.
- La sessione di design si chiude qui. Il prossimo passo (creazione del progetto Android e
  porting in Kotlin) torna alla sessione principale.

### 23. Nome dell'app: Imash
- Decisione dell'utente: l'app si chiama **Imash**. ✅
- Aggiornati nel design: titolo nella barra in alto delle schermate dei brani (1, 1b, 1c),
  titoli delle pagine `schermate.html` e `catalogo.html`, intestazioni di `tokens.css`,
  `componenti.css` e `README.md`.
- Nel README annotato che sarà anche il nome sotto l'icona (`app_name` in `strings.xml`);
  la parola "mashup" resta il nome del risultato ("Crea mashup", "Mashup pronto").
- Verificato nel browser integrato.

### 24. Ripresa nella sessione principale: piano di implementazione (proposta)
- Letti `PROGRESSI.md` (voci 12-23) e `design/README.md`. Requisiti aggiornati rispetto alla
  voce 6: durata scelta nelle Impostazioni (3-30 min, predefinita 10) e avviso "Troppe canzoni"
  oltre il +20% nella modalità "Canzoni intere".
- Nota sull'algoritmo: `mashup.py` accorcia ogni brano a 30-46 s. Con la nuova regola, nella
  modalità "Canzoni intere" si parte dalla durata intera e si tolgono misure finché il totale
  rientra nella durata scelta (lo stesso ciclo di aggiustamento di `mashup.py`, riusato).
- **Struttura del codice proposta** (ogni funzione scritta una sola volta e condivisa dalle due modalità):

  | Pacchetto | Classe | Sostituisce in Python |
  |---|---|---|
  | `audio` | `Mp3Decoder` (MediaCodec → PCM) | `librosa.load` (usato 3 volte negli script) |
  | `audio` | `Resampler` (44,1 kHz stereo per l'uscita, 22,05 kHz mono per l'analisi) | `sr=` di `librosa.load` |
  | `audio` | `Mp3Encoder` (LAME via JNI, a flusso) | `sf.write(..., format="MP3")` (2 volte) |
  | `dsp` | `Stft` (FFT condivisa) | `librosa.stft` |
  | `dsp` | `Chroma` | `librosa.feature.chroma_stft` |
  | `dsp` | `BeatTracker` | `librosa.beat.beat_track`, `griglia_misure` |
  | `dsp` | `ChorusDetector` (+ ripiego sull'energia) | `pychorus`, `trova_ritornello`, `segmento_piu_forte` |
  | `mashup` | `DurationPlanner` (tagli sulle misure, limite +20%) | ciclo delle durate in `mashup.py` |
  | `mashup` | `MashupRenderer` (RMS, crossfade, fade, normalizzazione) | `normalizza_rms`, `crossfade`, fine di `main` |
  | `ui` | Tema dai token, schermate Brani / Elaborazione / Risultato / Impostazioni, `BarraMashup` | — |
  | `data` | Impostazioni con DataStore | — |

- Memoria: un mashup di 30 minuti in float stereo occupa circa 630 MB. Per questo si elabora
  un brano alla volta e il risultato intermedio va in un file temporaneo nella cache; poi un
  secondo passaggio normalizza il volume e codifica l'mp3.
- **Fasi proposte** (ognuna compilata e provata prima della successiva):
  1. Scheletro del progetto Gradle (Kotlin, Compose, NDK), tema dai token, navigazione tra
     schermate vuote; avvio sull'emulatore.
  2. Ingresso/uscita: decodifica mp3, ricampionamento, LAME, "Salva con nome". Prova: due mp3
     uniti in coda in un mp3.
  3. Modalità **Canzoni intere**: STFT, battiti, `DurationPlanner`, crossfade, test unitari.
  4. Modalità **Ritornelli**: chroma e `ChorusDetector`.
  5. Interfaccia completa: riordino trascinando, barra del mashup, Impostazioni, avvisi,
     ascolto di anteprima, avanzamento.
  6. Prove sul telefono/emulatore e analisi di sicurezza con Vantage.
- Piano: **in attesa di conferma**.
- ✅ **Piano approvato** dall'utente; si parte dalla fase 1.
- Decisioni confermate:
  - **Ritornelli**: stessa regola di "Canzoni intere": fino al +20% si accorcia sulle misure,
    oltre compare l'avviso "Troppe canzoni". ✅
  - **LAME 3.100**: autorizzato il download del sorgente da lame.sourceforge.io (fase 2). ✅
  - **Elaborazione in background**: servizio in primo piano con notifica di avanzamento. ✅

### 25. Fase 1: versioni e download autorizzati
- Versioni rilevate (Android Studio 2026.2.2 include AGP 9.5 e Kotlin 2.4):
  Gradle **9.8.2**, AGP **9.5.1** (Kotlin integrato in AGP 9), Kotlin/Compose compiler **2.4.21**,
  Compose BOM **2026.09.00**, activity-compose **1.13.0**, lifecycle **2.11.0**,
  navigation-compose **2.10.2**, datastore **1.3.0**, core-ktx **1.19.1**, coroutines **1.11.0**.
- Download autorizzati dall'utente: wrapper Gradle (49 KB, GitHub gradle/gradle v9.8.2),
  distribuzione Gradle 9.8.2 (152 MB), librerie da Google Maven / Maven Central, font
  Figtree e Bricolage Grotesque (OFL, GitHub google/fonts).
- Dove vanno:
  - progetto Android in **`android/`** (sottocartella del progetto);
  - wrapper in `android/gradle/wrapper/`, font in `android/app/src/main/res/font/`;
  - Gradle e cache delle librerie in `C:\Users\leona\.gradle\` (posizione standard, condivisa).

### 26. Fase 1 completata: scheletro del progetto Android
- ✅ Scaricati e verificati: `gradle-wrapper.jar` (checksum SHA-256 identico a quello ufficiale),
  `gradlew`, `gradlew.bat`; font `figtree.ttf` e `bricolage_grotesque.ttf` (variabili, OFL).
  Anche la distribuzione Gradle è verificata con `distributionSha256Sum`.
- ✅ Creati in `android/`:
  - `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`
    (unico elenco delle versioni), `local.properties` (percorso SDK), `.gitignore`;
  - `app/build.gradle.kts`: package `com.imash.app`, minSdk 26, target/compileSdk 37, Compose;
  - `AndroidManifest.xml`, risorse (`app_name` = Imash, tema finestra chiaro/scuro, icona
    adattiva con la "barra del mashup", icona "indietro");
  - `ui/tema/Colori.kt`, `Tipografia.kt`, `Tema.kt`: colori (chiaro/scuro), colori dei brani,
    tipografia, spaziature e forme copiati da `design/tokens.css`;
  - `ui/Navigazione.kt`: le 4 schermate (Brani, Elaborazione, Risultato, Impostazioni) con un
    **unico** segnaposto condiviso, da sostituire nella fase 5;
  - `MainActivity.kt`.
- NDK/CMake non ancora collegati: si aggiungono nella fase 2 insieme a LAME.
- Compilazione: primo tentativo fallito per un `--` dentro un commento XML (`themes.xml`),
  corretto. Poi **BUILD SUCCESSFUL**, nessun avviso del compilatore Kotlin.
  Nota: la build è lenta (10-14 minuti), probabilmente per l'antivirus di Windows che
  analizza i file di Gradle.
- Emulatore `Medium_Phone_API_37.0`: al primo avvio il sistema Android si riavviava di
  continuo (schermo nero, PC con solo 2 GB di RAM libera durante la build). Fermati i
  processi Gradle e fatto un avvio a freddo con 4 GB: funziona (resta un avviso di lentezza
  di "System UI", che riguarda l'emulatore e non l'app).
- ✅ App installata e avviata: tema chiaro e scuro, font, colori dei brani e navigazione
  funzionano.
- Da sistemare nella fase 5: in tema scuro le icone della barra di stato restano scure.

### 27. Fase 2: lettura, ricampionamento, codifica mp3 e "Salva con nome"
- ✅ Scaricato **LAME 3.100** (`lame-3.100.tar.gz`, 1,5 MB, sourceforge). SHA-256
  `ddfe36ca…1da1e` verificato. Copiati in `android/app/src/main/cpp/lame/` solo l'encoder
  (`libmp3lame/`, `include/lame.h`) più `COPYING` (LGPL) e `README`, **senza modifiche**.
  Il tipo `ieee754_float32_t` (che di solito definisce `config.h`) è passato come definizione
  in CMake. Prova di compilazione con il clang dell'NDK: 20 file su 20 compilati.
- ✅ Nuovi file (prima di scriverli verificato che non esistesse codice audio):
  - `cpp/CMakeLists.txt`: libreria statica `mp3lame` + libreria JNI `imash_audio`;
  - `cpp/lame_jni.c`: init / encode (float planare) / flush / close;
  - `audio/Mp3Decoder.kt`: decodifica a flusso con MediaExtractor + MediaCodec (16 bit o float);
  - `audio/Resampler.kt`: ricampionamento a flusso con sinc finestrato (Blackman, 10 zeri per lato);
  - `audio/LettoreAudio.kt`: **unico punto** che sostituisce `librosa.load` (decodifica, canali
    mono/stereo come librosa, ricampionamento); servirà sia all'analisi (22050 Hz mono) sia al mashup
    (44100 Hz stereo);
  - `audio/Mp3Encoder.kt`: LAME CBR 320 kbps (= `compression_level=0.0` degli script);
  - `data/Archivio.kt`: file temporaneo nella cache e copia nel documento scelto con "Salva con nome";
  - `audio/UnioneInCoda.kt` + pannello "Prova fase 2" nella schermata provvisoria: **provvisori**,
    mettono i brani uno dopo l'altro; saranno sostituiti nella fase 3.
  - `build.gradle.kts`: NDK 30, CMake 4.1.2, ABI arm64-v8a, armeabi-v7a, x86_64.
- ✅ Test unitari `ResamplerTest`: **6/6 superati** (durata conservata, frequenza e ampiezza di un
  seno 48→44,1 kHz, filtro anti-aliasing 44,1→22,05 kHz, risultato uguale a blocchi o tutto
  insieme, passaggio invariato a pari frequenza, mono = media dei canali).
- Decisione dell'utente: la scelta dei brani accetta **qualunque audio** (`audio/*`: mp3, m4a, …);
  il risultato resta sempre mp3. ✅
- Prova sull'emulatore con i 3 file di `canzoni/` (m4a AAC, 44,1 kHz stereo; 235,6 + 213,8 + 195,2 s):
  - scelta multipla nel selettore di sistema ✅, unione ✅, "Salva con nome" in Download ✅;
  - mp3 risultante controllato frame per frame: **24.678 frame, tutti 320 kbps / 44,1 kHz,
    0 byte non validi, durata 644,6 s** (= somma dei brani).
  - ⚠️ Tempo: **30 minuti** per 10:45 di audio. Causa: CPU del PC al 100% occupata dall'emulatore
    (immagine con Play Store che lavora in background) e app in versione debug. Va rimisurato su un
    telefono vero o in versione release; se resta lento si ottimizza nella fase 3.

### 28. Fase 3: battiti, pianificazione della durata e unione con dissolvenze
- Richiesta dell'utente dopo la prova della fase 2: niente secondi di silenzio tra un brano e
  l'altro, il passaggio deve essere omogeneo o con una transizione. Risolto con il **taglio del
  silenzio** a inizio/fine brano (soglia -50 dB) e la **dissolvenza incrociata equal-power**.
- ✅ Nuovi file (prima verificato che non esistesse già codice equivalente):
  - `dsp/Fft.kt`, `dsp/Stft.kt`, `dsp/Mel.kt`: FFT radix-2, STFT con Hann periodica (center=True
    come librosa), banco Mel Slaney;
  - `dsp/BeatTracker.kt`: porting di `librosa.beat.beat_track` (onset strength, tempogramma con
    prior log-normale a 120 BPM, programmazione dinamica di Ellis, taglio dei battiti deboli);
  - `mashup/AnalisiBrano.kt`: energia a blocchi, inizio/fine del suono, misure (un battito su 4,
    solo con almeno 16 battiti), durata del battito mediana;
  - `mashup/DurationPlanner.kt`: ciclo di `mashup.py` (toglie/aggiunge una misura finché il totale
    è 10:00 ± 15 s), avviso "Troppe canzoni" oltre il +20%, dissolvenza = 2 misure (2,5–7 s) e mai
    oltre metà dei brani che unisce;
  - `mashup/MashupRenderer.kt`: dissolvenza equal-power, fade-in 1 s e fade-out 4 s, guadagno per
    RMS 0,12, silenzio di riempimento se un file è più corto del previsto;
  - `mashup/GeneratoreMashup.kt`: analisi → piano → passata 1 (picco) → passata 2 (scala a 0,95 e
    codifica mp3). Per ora solo la modalità "Canzoni intere".
- 🗑️ Eliminato `audio/UnioneInCoda.kt` (provvisorio della fase 2); il pannello di prova ora usa
  `GeneratoreMashup`.
- Correzioni durante lo sviluppo: `return@repeat` nel pianificatore non usciva dal ciclo (→ `for`
  + `break`); `DoubleArray.indexOf` non più risolto in Kotlin 2.4 (→ indice del taglio scelto);
  rimossi codice inutilizzato in `BeatTracker`.
- ✅ Test unitari: **23/23 superati** (6 Resampler, 7 DSP con battiti a 100/120/140 BPM, 10 mashup:
  esempio "17:10 → Troppe canzoni", accorciamento sulle misure, brani corti, dissolvenze, renderer,
  analisi del silenzio).
- Primo avvio sull'emulatore: errore di avvio del decoder AAC (timeout 0x80000000 dell'emulatore).
  `Mp3Decoder.avviaCodec` ora **riprova 3 volte**; gli errori finiscono anche nel log `Imash`.
- ✅ Prova sull'emulatore con i 3 file di `canzoni/` (10:45 in tutto, entro il +20%):
  - risultato **10:14**: brano 1 0:00→3:36 (di 3:55), brano 2 0:00→3:32 (di 3:33), brano 3
    0:00→3:13 (di 3:15), tagli sulle misure, 123 BPM rilevati;
  - "Salva con nome" in Download ✅; mp3 controllato: **23.515 frame, tutti 320 kbps / 44,1 kHz,
    0 byte non validi, 614,3 s**;
  - livello misurato ogni 0,5 s: **nessun tratto sotto -35 dB** tra un brano e l'altro → niente
    più silenzi nei passaggi.
  - ⚠️ Tempo: **90 minuti** sull'emulatore (3 letture complete di ogni brano: analisi, picco,
    codifica). Va misurato su un telefono vero / versione release; possibili ottimizzazioni:
    tenere in cache il PCM decodificato per evitare la terza lettura, analisi a 22050 Hz diretta.

### 29. Transizioni diverse a seconda dei brani (⏸️ IN PAUSA – da provare sull'emulatore)
- Richiesta dell'utente dopo aver ascoltato il mashup della fase 3: il passaggio tra il brano 1 e il 2
  "non ci sta molto"; proposta di transizioni diverse a seconda delle canzoni.
- Analisi del passaggio 1→2 (sul mashup della fase 3): il brano 1 veniva tagliato a metà di una sezione
  forte (-10 dB) mentre il brano 2 parte con un'intro debole (-17 dB) → buco a -23 dB; battiti non
  allineati (il brano 2 partiva dal primo suono, non dal battere); tonalità poco compatibili
  (brano 1 Si minore, brano 2 La maggiore, brano 3 Fa# minore; tutti ~123 BPM).
- Decisioni dell'utente (confermate):
  - transizioni scelte in automatico: **Mix a tempo** e **Scambio dei bassi** (scartati "Taglio sul
    battere" ed "Eco in uscita");
  - tagli a **fine frase** (blocchi di 8 misure) con il brano entrante sul primo battere;
  - scelta automatica **ma modificabile** dall'utente nella fase 5 (il pianificatore riceve già la
    lista delle transizioni, quindi basterà passargli quella scelta dall'utente).
- ✅ Nuovi file (prima verificato che non esistessero già: riusati `Stft`, `Fft` e la dissolvenza
  equal-power del renderer):
  - `dsp/Croma.kt`: cromagramma come `librosa.feature.chroma_stft` (servirà anche alla fase 4);
  - `dsp/Tonalita.kt`: tonalità con Krumhansl-Schmuckler, ruota di Camelot e compatibilità armonica;
  - `dsp/PassaBasso.kt`: biquad Butterworth (parte alta = segnale - bassi, ricostruzione perfetta);
  - `mashup/Transizioni.kt`: `TipoTransizione` (Mix a tempo, Scambio dei bassi, Dissolvenza se mancano
    i battiti) e `SceltaTransizione`: tonalità compatibili e scarto dei battiti ≤ 60 ms → Mix a tempo di
    8 misure (o 4 se il tempo differisce un po', max 20 s); altrimenti Scambio dei bassi di 4 misure.
- ✅ File modificati:
  - `mashup/AnalisiBrano.kt`: tonalità del brano; **battere** = battito (0-3) dove l'armonia cambia di più
    (`faseBattere`); griglia delle misure estesa all'indietro fino all'inizio del suono (`grigliaMisure`);
  - `mashup/DurationPlanner.kt`: riceve le transizioni; prova prima i tagli a fine frase (ogni 8 misure o
    fine naturale del brano), se non arriva a 10:00 ± 15 s ripiega sulle misure; la transizione copre
    esattamente le ultime N misure del brano uscente (battiti allineati), dimezzata se troppo lunga;
  - `mashup/MashupRenderer.kt`: Scambio dei bassi (alti in equal-power, bassi sotto 180 Hz scambiati in
    un battito a metà transizione);
  - `mashup/GeneratoreMashup.kt`: il brano parte dal primo battere; sceglie le transizioni;
  - `ui/Navigazione.kt` (provvisorio): il riepilogo mostra tonalità e transizione di ogni passaggio.
- ✅ Test unitari: **36/36 superati** (13 nuovi: croma di una nota, Camelot, stima della tonalità,
  passa-basso, battere, griglia, tagli a fine frase, ripiego sulle misure, scelta della transizione in
  4 casi, scambio dei bassi nel renderer).
- ⏸️ **Messo in pausa su richiesta dell'utente** durante la prova sull'emulatore:
  - l'emulatore si era bloccato appena acceso ("Process system non risponde", carico CPU 30) → spento
    e riavviato a freddo con `-cores 4`, aspettando che il carico scendesse sotto 3;
  - app nuova installata, 3 brani di `canzoni/` scelti, mashup avviato alle 20:01, arrivato ad
    "Analisi: brano 1 di 3" → **fermato** (emulatore e Gradle spenti).
- **Da fare alla ripresa:**
  1. riaccendere l'emulatore, aspettare che si calmi, rilanciare il mashup con i 3 brani (~90 min);
  2. controllare nel riepilogo tonalità e transizioni scelte (atteso: 1→2 Scambio dei bassi,
     2→3 Mix a tempo), salvare l'mp3, verificare i frame e il livello nei passaggi;
  3. mandare l'mp3 all'utente per l'ascolto;
  4. solo dopo, **chiedere conferma** all'utente prima di iniziare la fase 4 (Ritornelli).

### 30. Progetto caricato su GitHub
- Richiesta dell'utente: nuova repository con il progetto. Decisioni confermate: nome **Imash**
  (come l'app), **escluse** la cartella `canzoni/` (audio protetti da diritti) e i file di build/cache.
  Visibilità: l'utente ha creato la repository a mano su GitHub e l'ha resa **pubblica**.
- ✅ Installato GitHub CLI 2.103.3 con winget (il login con `gh` non è stato poi usato).
- ✅ `git init` (ramo `main`), `.gitignore` in radice (canzoni, `__pycache__`, build, `.gradle`,
  `.cxx`, `local.properties`), `.gitattributes` (`gradlew` sempre con fine riga LF).
- ✅ Primo commit `662dfee`: 107 file (app Android, sorgenti LAME con licenza LGPL, script Python,
  design, PROGRESSI.md). Controllato: nessun audio, nessun percorso locale, nessuna chiave.
- ✅ Push su **https://github.com/leoforlii/Imash** (ramo `main`). Il primo tentativo è stato
  rifiutato perché Windows aveva salvate le credenziali di un altro account GitHub (`myname1101`):
  l'indirizzo remoto ora contiene l'utente (`https://leoforlii@github.com/...`) e l'accesso è stato
  fatto dall'utente nel browser.
- Stato del lavoro invariato: voce 29 in pausa (prova sull'emulatore delle nuove transizioni da rifare).

### 31. Controllo della repo, sessione accessibile ovunque e CLAUDE.md
- Domanda dell'utente: è stato pushato tutto? ✅ Verificato: GitHub (`origin/main`, 2 commit) è
  identico a questo PC, nessun file non salvato; PROGRESSI.md contiene tutte le voci 1-30.
- Problema segnalato: dall'altro computer l'utente ha aperto una nuova sessione ("Progetto Imash da
  GitHub") scaricando la repo, ma mancavano alcune specifiche. Causa: le **regole di lavoro** erano
  salvate solo nella memoria di Claude di questo PC, non nella repo.
- ✅ Attivato **Remote Control** su questa sessione (accessibile anche dagli altri dispositivi).
- ✅ Creato **`CLAUDE.md`** nella radice (confermato dall'utente: regole + decisioni): le 4 regole di
  lavoro, le specifiche decise, lo stato delle fasi e i comandi. Ogni sessione di Claude su qualunque
  computer lo legge da sola all'avvio.
- ⏳ La sessione dell'altro computer risulta **offline** e non ha caricato niente su GitHub. Le è stato
  mandato un messaggio (consegnato quando quel computer si ricollega) con la richiesta di pushare il suo
  lavoro su un nuovo ramo `altro-pc`. Quando arriva: unirlo a `main` controllando i doppioni,
  aggiornare PROGRESSI.md e pushare.
