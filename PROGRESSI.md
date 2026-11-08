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

### 31. Ripresa su un altro PC: progetto clonato e SDK completato
- Il progetto è stato clonato da GitHub in `C:\Users\leona\Progetti\Imash`. Si tratta di un PC diverso da
  quello della voce 10: Android Studio è presente, ma l'SDK aveva solo le piattaforme 34-36 e mancavano
  NDK e CMake. L'emulatore disponibile è `Medium_Phone` (API 36.1).
- ✅ Scaricati i **Command-line Tools** (`commandlinetools-win-16111833_latest.zip`, 155 MB, da
  dl.google.com) e verificato lo SHA-1 `57d04f2d…5a63e9d` con l'indice ufficiale `repository2-3.xml`.
  Installati in `Sdk\cmdline-tools\latest`.
  Nota: ora `sdkmanager` passa al nuovo **Android CLI** (`android.exe sdk install`), che scrive i pacchetti
  con `/` al posto di `;` (es. `ndk/30.0.16248370`).
- ✅ Installati e verificati: **platforms/android-37.0**, **ndk/30.0.16248370**, **cmake/4.1.2**.
- ✅ Ricreato `android/local.properties` (percorso dell'SDK), che è escluso da git.
- Manca ancora la cartella `canzoni/` con i 3 brani di prova, anch'essa esclusa da git: la deve fornire
  l'utente. Poi si riprende dalla voce 29 (compilazione, emulatore, prova delle transizioni).

### 32. Prova delle transizioni (ripresa della voce 29)
- L'utente ha fornito i 3 brani (cartella `Desktop\Canzoni`, gli stessi delle prove precedenti: 3:55, 3:33,
  3:15), copiati in `canzoni/` (esclusa da git) e sull'emulatore in `Music/canzoni`.
- Compilazione debug: **BUILD SUCCESSFUL** in 12 min 53 s; emulatore `Medium_Phone_API_36.1` avviato in 40 s
  (questo PC: 16 core, 16 GB).
- ✅ Mashup creato in **627 s** (debug, emulatore), durata **10:13**:
  - brano 1 0:01→3:45, Fa# min · ↳ **Scambio dei bassi** (7,6 s)
  - brano 2 0:01→3:32, Sib min · ↳ **Scambio dei bassi** (7,7 s)
  - brano 3 0:00→3:13, Fa# min; tutti 123 BPM.
  - ⚠️ Atteso nella voce 29: 1→2 Scambio dei bassi, 2→3 **Mix a tempo**. Le tonalità rilevate dall'app sono
    diverse da quelle annotate nella voce 29 (Si min, La magg, Fa# min): da verificare dopo l'ascolto.
- ✅ mp3 controllato: **23.493 frame, tutti 320 kbps / 44,1 kHz, 0 byte non validi, 613,7 s**.
  Il controllo del livello nei passaggi non è stato fatto: su questo PC non c'è un decoder mp3 (né ffmpeg né librosa).
- Inviato all'utente per l'ascolto (`canzoni/risultati/mashup_transizioni.mp3`).
- Peso dell'app: debug **13,4 MB** (11,8 MB di codice Compose non ottimizzato), LAME 0,8 MB per 3 ABI, font 0,3 MB.

### 33. Ottimizzazione dei tempi
- Richiesta dell'utente: i tempi di attesa sono inaccettabili; proposta l'analisi in parallelo dei brani.
- Causa trovata: ogni brano veniva **decodificato 3 volte** (analisi, ricerca del picco, codifica), in versione debug.
- Proposte 6 ottimizzazioni, **approvate tutte** dall'utente. Applicate:
  1. **Versione release** (R8): firmata provvisoriamente con la chiave di debug per poterla installare
     (`signingConfig` in `build.gradle.kts`; chiave vera nella fase 6).
  2. **Decodifica unica**: nuovo `audio/CachePcm.kt` (`ScrittorePcm` / `LettorePcm`, PCM 16 bit stereo in
     `cache/lavoro/`). L'analisi salva il brano mentre lo analizza; il mix lo rilegge da lì.
  3. **Un solo mix**: il mix va in cache a 16 bit, ridotto prima del limite massimo possibile
     (`MashupRenderer.limiteMix`: somma dei due brani in transizione × 3,2 per lo scambio dei bassi) così non
     viene mai tagliato; poi viene riletto, portato al picco 0,95 e codificato. La cartella `lavoro/` viene
     cancellata alla fine, anche in caso di errore.
  4. **Analisi in parallelo**: `GeneratoreMashup.analizzaInParallelo`, un brano per core, al massimo 4.
     Verificato che le classi audio/DSP non abbiano stato condiviso modificabile.
  6. **LAME qualità 5** invece di 2 (`lame_jni.c`).
- ❌ Non applicata la 5 (**un solo spettrogramma** per battiti e croma): usano FFT diverse (2048/512 e
  4096/1024); unificarle cambierebbe la risoluzione del croma e quindi tonalità e transizioni, proprio mentre
  vanno verificate. Guadagno stimato piccolo rispetto alle altre.
- ✅ Test unitari: **40/40 superati** (4 nuovi: andata e ritorno PCM 16 bit, scala e limite, lettura fino al
  massimo, mix sempre sotto `limiteMix` con onde quadre, rumore, scambio dei bassi e mix a tempo).
- ✅ Release: **BUILD SUCCESSFUL** in 4 min 51 s, APK **2,42 MB** (debug 13,4 MB).
- ✅ Stesso mashup sull'emulatore con la release: **28 s invece di 627 s (circa 22× più veloce)**, piano
  identico (tagli, BPM, tonalità, transizioni); mp3 di nuovo 23.493 frame a 320 kbps, 0 byte non validi.
  Inviato all'utente (`canzoni/risultati/mashup_ottimizzato.mp3`).
- Non verificato: lo svuotamento della cache sul dispositivo (la release non permette `run-as`); il codice
  cancella `lavoro/` in un `finally`.
- **Prossimi passi**: ascolto dell'utente (transizioni 2→3 e tonalità), poi conferma prima della fase 4
  (Ritornelli). Se l'utente recupera dall'altro PC lavoro sulla fase 4 non caricato su GitHub, va scaricato prima.

### 34. Fase 4: modalità Ritornelli
- ✅ L'utente ha dato il via alla fase 4. Nota: l'utente pensava che la fase 4 fosse già fatta; su GitHub non c'è
  traccia (nessun codice, ultima voce la 30). Se sull'altro PC c'è lavoro non caricato, va confrontato con questo.
- Prima di scrivere: riusati `Croma` (con n_fft 16384, come pychorus), `Stft`, il mono a 22050 Hz già calcolato
  per i battiti e il pianificatore, che prevedeva già il limite dei 60 s per i ritornelli.
- ✅ Nuovo `dsp/RilevatoreRitornello.kt`, porting di `pychorus` 0.1 come lo usa `trova_ritornello`:
  matrici tempo-tempo e tempo-ritardo, `denoise` (medie mobili con somme scorrevoli, filtro gaussiano con bordi
  "reflect" di scipy), righe di massimo locale, segmenti sopra soglia (0,15, abbassata del 5% fino a 8 volte),
  punteggio per sovrapposizione, miglior segmento. Mantenuto il comportamento originale di `detect_lines_helper`
  (inizio del segmento non azzerato fra le righe). Più `segmentoPiuForte` (20 s di energia massima).
  Memoria: ~2 matrici N×N float (N ≈ 5,4 frame/s; ~14 MB per un brano di 4 min).
- ✅ `AnalizzatoreBrano(cercaRitornello)`: come `processa` di estrai_ritornelli.py: ripetizione di almeno 30 s,
  poi di 10 s, altrimenti energia; finestra di 60 s, spostata indietro se il brano finisce prima.
  Differenza: croma con intonazione 0 (librosa la stima), come già per la tonalità.
- ✅ `GeneratoreMashup(modalita)`: nuova `Modalita` (CanzoniIntere, Ritornelli); per i Ritornelli il tratto parte
  dalla **misura più vicina** all'inizio del ritornello (`tratto`), così il brano resta sul battere. Stessa regola
  delle Canzoni intere: fino al +20% si accorcia sulle misure, oltre "Troppe canzoni".
- Pannello di prova provvisorio: scelta "Canzoni intere" / "Ritornelli" e, nel riepilogo, ritornello e metodo.
- ✅ Test unitari: **44/44 superati** (4 nuovi: ritornello di 40 s ripetuto 3 volte fra strofe diverse,
  ripiego sull'energia, bordi "reflect", tratto sulla misura più vicina). Release: BUILD SUCCESSFUL in 2 min 27 s.
- ✅ Prova sull'emulatore (release) con i 3 brani, modalità Ritornelli: **27 s**, durata **2:41** ("brani troppo
  corti": 3 ritornelli da 60 s non arrivano a 10 minuti, comportamento previsto):
  - brano 1: ritornello 1:34→2:34 (ripetizione), usato 1:33→2:32 · ↳ Scambio dei bassi (7,6 s)
  - brano 2: ritornello 2:16→3:16 (ripetizione), usato 2:15→3:15 · ↳ Scambio dei bassi (7,7 s)
  - brano 3: ritornello 2:15→3:15 (ripetizione, finestra spostata alla fine), usato 2:15→3:13
  - mp3: **6.196 frame, tutti 320 kbps / 44,1 kHz, 0 byte non validi, 161,9 s**. Inviato all'utente
    (`canzoni/risultati/mashup_ritornelli.mp3`).
- Non verificato: confronto con lo script Python originale (su questo PC mancano librosa e pychorus).
- **Prossimo passo**: ascolto dell'utente; poi, con conferma, la fase 5 (interfaccia completa).

### 35. Unione con l'altro PC e fase 5, passo 1: schermata Brani
- ✅ Su richiesta della sessione principale (PC di casa) e con l'ok dell'utente: commit di tutto il lavoro di questo
  PC (voci 31-34) e push sul nuovo ramo **`altro-pc`** (commit `a8efb3a`, basato su `d44ced8`). L'unione con `main`
  (che ha già `CLAUDE.md` e una sua voce 31) la fa la sessione principale: **numerazione delle voci da sistemare**.
- ✅ L'utente ha dato il via alla fase 5 e ha approvato due scelte:
  - riordino con la libreria **`sh.calvin.reorderable` 3.1.0** (invece di un'implementazione propria);
  - **transizioni modificabili nella schermata Risultato** (per ogni passaggio Mix a tempo / Scambio dei bassi,
    poi il mashup si ricrea; i file intermedi resteranno nella cache fino a "Salva" o "Nuovo mashup").
- Piano della fase 5, un passo alla volta con conferma: 1 Brani, 2 Impostazioni, 3 Elaborazione con servizio e
  notifica, 4 Risultato con lettore e transizioni, 5 rifiniture (icone barra di stato, mp3 temporaneo, via il
  pannello di prova).
- ✅ Passo 1, schermata Brani (design 1, 1b, 1c):
  - `data/Brani.kt`: `BranoScelto` (uri, nome, durata), nome e durata letti senza decodificare
    (`OpenableColumns` + `MediaMetadataRetriever`); permesso di lettura persistente (servirà al servizio).
  - `data/Impostazioni.kt`: durata con DataStore (3-30 min, predefinita 10); la userà anche il passo 2.
  - `mashup/Anteprima.kt`: stima della durata prima dell'analisi (Ritornelli = al massimo 60 s per brano),
    stesso margine +20% di `DurationPlanner` (riusato `MARGINE_MASSIMO` e `MAX_CLIP_SEC`, niente doppioni).
  - `ui/BarraMashup.kt`: la barra del mashup (Canvas, giunture sfumate da 10 dp, parte oltre il limite a strisce
    con il bordo di errore). Ci si è spostata anche `minSec`, prima privata in `Navigazione.kt`.
  - `ui/StatoBrani.kt` (ViewModel) e `ui/SchermataBrani.kt`: selettore della modalità con la frase di aiuto,
    stato vuoto, avviso "Troppe canzoni" con "Apri impostazioni", anteprima, lista trascinabile con numero
    colorato e "Rimuovi", "Aggiungi mp3", "Crea mashup" attivo solo con ≥ 2 brani e senza "Troppe canzoni";
    file illeggibili saltati con uno snackbar; brani doppi ignorati.
  - 7 icone Material Symbols Rounded (licenza Apache 2.0) in `res/drawable/ic_*.xml`.
  - Tolta `GeneratoreMashup.DURATA_PREDEFINITA` (doppione): ora c'è `Impostazioni.DURATA_PREDEFINITA`.
  - Il pannello di prova è passato provvisoriamente nella schermata segnaposto "Elaborazione".
- ✅ Test: **47/47** (3 nuovi in `AnteprimaTest`: esempio 17:10 del design, entro il +20%, Ritornelli a 60 s).
  Release: BUILD SUCCESSFUL, APK 2,78 MB.
- ✅ Prova sull'emulatore: stato vuoto; 4 brani (13:26) → avviso "Troppe canzoni", barra a strisce oltre 12:00,
  "Crea mashup" disattivato; trascinamento dell'ultimo brano in cima (numeri, colori e barra aggiornati);
  rimosso un brano → 10:44 "accorciati a 10:00", pulsante attivo; Ritornelli → 3:00 "meno di 10:00";
  tema scuro corretto. Corretto il colore dell'icona di "Apri impostazioni" (era petrolio).
- Nota: l'anteprima non conosce ancora le transizioni, quindi vicino al limite il controllo vero resta quello
  del pianificatore dopo l'analisi.
- ✅ L'utente ha confermato il passo 1.

### 36. Fase 5, passo 2: schermata Impostazioni
- ✅ `ui/SchermataImpostazioni.kt` (design 4): "Durata del mashup" con il valore (`10 min`), cursore 3-30 a passi
  di 1 minuto, etichette 3 min / 30 min, frase sul +20% con il massimo calcolato (riusato
  `DurationPlanner.MARGINE_MASSIMO` e `minSec`), "Ripristina 10 minuti" disattivato quando vale già 10.
  Freccia "Torna ai brani". Il valore si salva in DataStore (`Impostazioni`, voce 35) solo quando si lascia
  il cursore, non a ogni movimento.
- `Navigazione.kt`: la rotta Impostazioni porta alla schermata vera (si apre dall'ingranaggio e da
  "Apri impostazioni" dell'avviso).
- ✅ Test unitari 47/47, release BUILD SUCCESSFUL.
- ✅ Prova sull'emulatore: 4 brani (13:26) → "Troppe canzoni" → "Apri impostazioni" → cursore da 10 a 14 min
  (frase: "fino a 16:48") → indietro: avviso sparito, "13:26 di brani, meno di 14:00", "Crea mashup" attivo;
  app chiusa e riaperta: la durata resta 14; "Ripristina" la riporta a 10 e si disattiva.
- ✅ L'utente ha confermato il passo 2.

### 37. Fase 5, passo 3: Elaborazione con servizio in primo piano e notifica
- ✅ `GeneratoreMashup`: nuovo avanzamento dettagliato `Passo` (AnalisiIniziata, Lettura ogni 5 s di audio,
  AnalisiFinita con l'analisi, Pianificato con il piano, Taglio per brano, Unione con la frazione codificata)
  al posto di `(Fase, brano)`; si può **annullare** (controllo a ogni blocco letto, nel decoder, nel mix e
  nella codifica; l'analisi finale di un brano non si interrompe, quindi l'annullamento arriva entro qualche s).
- ✅ `servizio/StatoElaborazione.kt` (senza Android, testato): avanzamento di ogni brano (fase, attivo, fatto,
  secondi letti, ritornello non trovato), percentuale con pesi Analisi 0,6 / Taglio 0,25 / Unione 0,15,
  "Brano X di N", esito (Pronto, TroppeCanzoni, Errore, Annullato).
- ✅ `servizio/Elaborazione.kt` (stato condiviso, un'elaborazione alla volta) e `servizio/ServizioMashup.kt`:
  servizio in primo piano di tipo **mediaProcessing** (Android 15+) o **dataSync** (10-14), wake lock parziale
  (max 30 min), notifica di avanzamento (al massimo 2 aggiornamenti/s) e, alla fine, notifica "Mashup pronto"
  o di errore che riapre l'app. `onTimeout` (limite di Android 15) annulla. Permessi nel manifest:
  FOREGROUND_SERVICE(_DATA_SYNC, _MEDIA_PROCESSING), POST_NOTIFICATIONS (chiesto a "Crea mashup" su
  Android 13+; se rifiutato si crea lo stesso), WAKE_LOCK.
- ✅ `ui/SchermataElaborazione.kt` (design 2): titolo, modalità e numero di brani, percentuale grande, barra del
  mashup che si riempie (fatto / in corso a strisce / in attesa: `BarraMashup` ha ora gli `stati`),
  "Brano X di N" e tempo rimasto stimato, righe dei brani con icona di stato, fasi Analisi/Taglio/Unione
  come etichette e barra di lettura, nota "Ritornello non trovato: usato il tratto più intenso".
  "Annulla" (e il tasto Indietro) chiede conferma ("Interrompere il mashup?" – Continua / Interrompi).
  "Troppe canzoni" dopo l'analisi ed errori mostrati con "Torna ai brani".
- `Navigazione.kt` riscritta: tolti il segnaposto della fase 1, il pannello di prova e l'anteprima dei colori;
  riaprendo l'app si torna all'elaborazione in corso o al risultato. Risultato **provvisorio** (riepilogo del
  piano, "Salva mp3", "Nuovo mashup") fino al passo 4.
- 2 icone in più (check_circle pieno, schedule). Test: **49/49** (2 nuovi in `StatoElaborazioneTest`).
- Problemi trovati e risolti: crash "Starting FGS with type none" con `ServiceCompat.startForeground` → ora
  `startForeground(id, notifica, tipo)` diretto. Il wake lock ora si rilascia a fine lavoro e non solo in
  `onDestroy` (prudenza: dal log di sistema non risultava comunque mai rimasto attivo).
- ✅ Prove sull'emulatore: elaborazione completa con lo stesso piano della voce 32 (10:13, due Scambio dei bassi),
  notifica con barra, passaggio automatico al risultato; annullamento con conferma → ritorno ai brani,
  servizio fermato, wake lock rilasciato.
- ⚠️ **Tempi**: sull'emulatore riavviato 65 s a schermo acceso (voce 33: 28 s con il pannello di prova), poi
  200 s a schermo spento e 429 s in una prova successiva, con l'emulatore in swap: misure troppo variabili per
  essere affidabili. Nella prova da 429 s il thread dell'interfaccia ha usato 70 s di CPU su 513 totali
  (indicatori animati disegnati in software dall'emulatore). Da misurare su un telefono vero (fase 6); se serve,
  si riducono le animazioni durante l'elaborazione.
- ✅ L'utente ha confermato il passo 3 e ha chiesto di salvare tutto su GitHub: commit `cd75b63` sul ramo `altro-pc`.

### 38. Fase 5, passo 4: Risultato con lettore e transizioni modificabili
- ✅ `GeneratoreMashup`: i file intermedi (brani decodificati) e l'analisi restano finché non si chiama
  `pulisci()`; `genera(…, scelte)` la seconda volta rifà solo piano, mix e codifica con le transizioni scelte.
  `SceltaTransizione.modificabile` (servono i battiti di entrambi i brani) e `conTipo` (Mix a tempo su 8 misure
  se restano entro 20 s, altrimenti 4; Scambio dei bassi su 4).
- ✅ `Elaborazione.ricomponi(passaggio, tipo)`: il servizio ricrea il mashup in un file a parte e lo sostituisce
  solo se riesce (altrimenti resta quello di prima e la scelta torna indietro, con un messaggio). Stato:
  `ricomposizione` (avanzamento), `salvato`, `messaggio`. `chiudi(context)` cancella file intermedi e mp3
  temporaneo (punto della fase 5 "cancellare l'mp3 temporaneo").
- ✅ `ui/Lettore.kt` (MediaPlayer sull'mp3 temporaneo) e `ui/SchermataRisultato.kt` (design 3): durata grande,
  "N canzoni unite / ritornelli uniti", barra-lettore con testina (tocco e trascinamento per spostarsi:
  `BarraMashup` ha ora `testina` e `sposta`), tempi, −10 s / play-pausa (72 dp) / +10 s, elenco "Nel mashup"
  con "da m:ss" e "in ascolto" (tocco = salta lì), fra un brano e l'altro la transizione con il menu
  Mix a tempo / Scambio dei bassi (senza battiti: "Dissolvenza, senza battiti", non modificabile), riquadro
  "Aggiorno il mashup…" con avanzamento e Annulla, "Salva mp3" e "Nuovo mashup". Uscendo senza aver salvato
  chiede conferma ("Uscire senza salvare?" – Resta / Esci). Tolto il risultato provvisorio da `Navigazione.kt`.
- 6 icone in più (replay_10, forward_10, play, pause, save, expand_more). Test: **52/52** (3 nuovi in
  `SceltaUtenteTest`: conTipo con 8 o 4 misure, senza battiti, inizi dei brani nel mashup).
- ✅ Prova sull'emulatore: mashup in 45 s (analisi 34 s, taglio 2 s, unione 10 s); tocco sul brano 2 → 3:36 e
  play ("in ascolto"); primo passaggio cambiato in Mix a tempo (15,2 s) → ricreato in ~12 s senza rifare
  l'analisi, durata 10:06; "Salva mp3" → file valido (23.202 frame a 320 kbps, 606,1 s), inviato all'utente
  (`canzoni/risultati/mashup_mix_a_tempo.mp3`); secondo passaggio in Mix a tempo → 9:58, poi Indietro →
  conferma → Esci → schermata dei brani, servizio fermato.
- Domanda dell'utente sulla velocità (taglio in parallelo, unione in un solo file): l'unione produce già un
  solo file; con i tempi misurati (analisi ~79%, taglio ~4%, unione ~17%) il taglio in parallelo non conviene,
  l'unione in parallelo a pezzi (LAME senza bit reservoir) farebbe risparmiare forse 7-8 s su 65; il grosso è
  l'analisi, da misurare su telefono. Proposto dopo il passo 4, in attesa di conferma.
- ✅ L'utente ha scelto di fare prima il passo 5 (rifiniture).

### 39. Fase 5, passo 5: rifiniture
- ✅ Icone della barra di stato in tema scuro (voce 26): verificato con avvio a freddo in tema scuro, sono chiare;
  già risolto da `enableEdgeToEdge` + tema della finestra in `values-night`. Nessuna modifica necessaria.
- ✅ Pulizia all'avvio (`Archivio.pulisciAvanzi`, chiamata da `MainActivity` se non c'è un mashup in corso):
  cancella file intermedi e mp3 temporanei lasciati da un'elaborazione interrotta (app chiusa dal sistema) e
  rilascia i permessi sui brani della volta prima (la lista riparte vuota). Il nome della cartella è in un solo
  punto: `GeneratoreMashup.cartellaLavoro`.
- ✅ Lint di Android: 5 avvisi, tutti risolti → **0 avvisi**:
  - `ModifierParameter`: parametro della maniglia rinominato `modifier` in `RigaBrano`;
  - `DataExtractionRules`: `res/xml/estrazione_dati.xml` (niente backup né trasferimento) e
    `res/xml/backup_completo.xml` (`fullBackupContent`) per Android < 12 (resta `allowBackup="false"`);
  - `StaticFieldLeak` su `Elaborazione.generatore`: falso allarme (solo application context), annotato;
  - `NotShrinkingResources`: `isShrinkResources = true` in release → APK da 2,78 a **2,48 MB**.
- ✅ Test 52/52. Prova sull'emulatore in tema scuro, modalità Ritornelli: 39 s, stesso risultato della voce 34
  (2:41, brani da 0:51 e 1:43, due Scambio dei bassi), "3 ritornelli uniti: i brani non bastano per 10:00".
- **Fase 5 completata.** Restano da decidere: ottimizzazioni di velocità (unione in parallelo, analisi) e la
  fase 6 (telefono vero, prestazioni in release, scansione di sicurezza, chiave di firma vera, togliere x86_64).
- ✅ L'utente ha chiesto di salvare: commit `ffe7104` sul ramo `altro-pc`.

### 40. Fase 6: firma vera, scansione di sicurezza, prova sul telefono
- ✅ Decisioni dell'utente: prova su telefono vero collegato via USB; scansione **Vantage** completa; chiave di firma
  creata dall'utente (password scelta da lui, mai vista da Claude).
- ✅ **Firma vera**: `build.gradle.kts` legge `android/keystore.properties` (storeFile `firma/imash.jks`, alias
  `imash`); file e cartella in `.gitignore` insieme a `*.jks`/`*.keystore`. Chiave creata dall'utente con
  `keytool -genkeypair … -keyalg RSA -keysize 4096 -validity 36500` (certificato CN=leonardo forli, C=IT).
  ⚠️ L'utente deve tenere una copia di riserva di `imash.jks` e della password: senza, niente aggiornamenti.
- ✅ **Release solo ARM** (arm64-v8a, armeabi-v7a); x86_64 resta nella debug per l'emulatore. Lint
  `ChromeOsAbiSupport` disattivato di proposito (niente Chromebook). APK release: **2,16 MB**.
- ✅ **Scansione Vantage** (pipeline mobile 01-06, artefatti in `.vantage/`, fuori da git): ricognizione, mappatura,
  10 agenti OWASP Mobile Top 10 in parallelo, validazione, report in italiano
  (`.vantage/artifacts/reports/report.md`). **Rischio complessivo: basso.** 4 candidati → 1 confermato:
  - **F-001 (Low, CVSS 2,9)**: senza `keystore.properties` la release si firmava in silenzio con la chiave di
    debug. **Corretto**: ora esce non firmata (`app-release-unsigned.apk`) con un avviso; la firma di debug
    solo chiedendola (`-PfirmaDebug`, versione "0.1-firmadebug"). Verificato spostando il file per una build.
  - Scartati ma applicati come rinforzi (scelta dell'utente): `InfoAudio` rifiuta frequenze fuori 8-384 kHz e
    canali fuori 1-8, `Resampler` rifiuta frequenze ≤ 0 (mval-001); `lame_jni.c` controlla dimensioni degli
    array, handle e NULL di `GetPrimitiveArrayCritical`, e `Mp3Encoder` controlla anche l'esito del flush (mval-002).
  - Tutto il resto pulito: nessun segreto, niente rete, nessun dato personale nei log, 5 permessi giustificati,
    release offuscata e non debuggabile, backup disattivato.
- ✅ Test **54/54** (2 nuovi in `ValoriAudioTest`), lint **0 avvisi**.
- ✅ **Telefono vero**: Samsung Galaxy S23+ (SM-S916B, Android 17, arm64). Installata la release firmata, brani di
  prova copiati in `Music/canzoni`, mashup creato dall'utente: **20 s in tutto** (analisi 11,2 s, taglio 1,9 s,
  unione 7,1 s), contro 45-65 s sull'emulatore. Nessun crash; memoria dell'app ~160 MB (PSS) a fine lavoro.
- Nota per la velocità: sul telefono l'unione pesa il 35% (7 s); l'unione in parallelo potrebbe toglierne qualche
  secondo. Da decidere con l'utente.
- ✅ L'utente ha chiesto di salvare: commit `e7cb150` sul ramo `altro-pc`.

### 41. Password della chiave fuori dal disco e unione in parallelo
- Domanda dell'utente: "è sicuro lasciare la password così scoperta?". Risposta: è la pratica standard (file locale,
  escluso da git), ma chiave e password stavano nella stessa cartella. **Scelta dell'utente: variabile d'ambiente.**
- ✅ `build.gradle.kts` legge la password da `IMASH_FIRMA_PASSWORD`; `keystore.properties` ha solo `storeFile` e
  `keyAlias` (password tolta riscrivendo il file, senza leggerla). Nuovo script **`android/firma-release.ps1`**:
  chiede la password nascosta (`Read-Host -AsSecureString`), la passa a Gradle solo per quella compilazione
  (`--no-configuration-cache`, così non finisce nei file di Gradle), ferma il demone e la cancella. Uso:
  `powershell -ExecutionPolicy Bypass -File firma-release.ps1`. Cancellata anche la vecchia cache di configurazione.
  Senza password la release esce non firmata con un avviso (verificato).
- ✅ **Unione in parallelo** (richiesta dell'utente), `audio/CodificaParallela.kt`: il mix si divide in al massimo
  4 pezzi su confini di frame, ognuno codificato dal suo LAME in parallelo, poi i frame si riattaccano in un solo
  mp3. Per giunture senza difetti: LAME senza serbatoio di bit e senza frame "Info" (`perPezzi` in `Mp3Encoder` e
  `lame_jni.c`), ogni pezzo parte 3 frame prima (riscaldamento, frame buttati) e legge 3 frame dopo (sguardo in
  avanti); il ritardo di LAME è lo stesso per tutti i pezzi, quindi i frame tenuti coprono gli stessi campioni
  della codifica unica. `LettorePcm.leggi` ha ora `inizio`; `FrameMp3` legge i frame MPEG-1 Layer III.
- ✅ Test **58/58** (4 nuovi in `CodificaParallelaTest`: pezzi contigui con riscaldamento e margine, brano
  cortissimo in un pezzo solo, frame da 1044/1045 byte, byte non validi), lint 0 avvisi.
- ✅ **Telefono** (Galaxy S23+, release firmata dallo script): unione **da 7,1 a 3,0 s**. Totale 22,4 s
  (analisi 17,8 s contro 11,2 s della prova prima: il codice dell'analisi non è cambiato, differenza dovuta allo
  stato del telefono, da riverificare). mp3 salvato dall'utente: **23.193 frame tutti a 320 kbps, 0 byte non
  validi, 605,9 s**; inviato all'utente da ascoltare alle giunture (~2:31, 5:03, 7:34).
- Nota: senza serbatoio di bit, a 320 kbps la qualità dovrebbe restare indistinguibile; da confermare all'ascolto.
- ✅ L'utente ha ascoltato le giunture: nessun difetto ("Tutto ok"). Salvato su `altro-pc`.
