# Design – App Android "Imash" (mashup da mp3)

**Nome dell'app: Imash.** È il titolo nella barra in alto della schermata dei brani e sarà il
nome sotto l'icona (`android:label` / `app_name` in `strings.xml`). Dentro l'app la parola
"mashup" resta il nome del risultato ("Crea mashup", "Mashup pronto").

Design system Material 3 e concept delle schermate. Nessun codice Kotlin: questo è il
riferimento da cui partirà il progetto Android.

## File

| File | Contenuto |
|---|---|
| `tokens.css` | **Unica fonte dei token**: colori (tema chiaro e scuro), tipografia, spaziature, forme, movimento. |
| `componenti.css` | Tutti i componenti, costruiti solo con i token. |
| `catalogo.html` | Catalogo: token (con contrasto calcolato) e componenti con i loro stati, in tema chiaro e scuro. |
| `schermate.html` | Le schermate dentro una cornice di telefono da 360 × 780 dp, in tema chiaro e scuro. |
| `presentazione.css`, `presentazione.js` | Solo per le due pagine di presentazione (cornice, campioni, duplicazione chiaro/scuro). Non vanno portati nell'app. |

**Come aprirlo.** Le pagine caricano CSS e font esterni, quindi vanno servite da un server
locale, non aperte con doppio clic:

```bash
python -m http.server 5173 --directory design
```

Poi apri `http://localhost:5173/schermate.html`. In Claude Code la configurazione è già
pronta in `.claude/launch.json` (nome `design`).

## Idea centrale

La **barra del mashup**: una striscia che rappresenta i 10 minuti, con un segmento colorato
per brano, largo quanto il suo tratto nel mashup. Le dissolvenze di 2 misure sono le sfumature
tra un segmento e l'altro. La stessa barra compare in tutte le schermate:

1. **Scegli i brani**: anteprima dell'ordine; si aggiorna quando si trascinano i brani.
2. **Elaborazione**: si riempie brano per brano (fatto / in corso a strisce / in attesa).
3. **Risultato**: diventa il lettore, con la posizione di ascolto.

Il resto dell'interfaccia è sobrio: niente ombre (tranne la riga trascinata), gerarchia data
dai livelli di superficie M3.

## Token → Compose

| CSS | Compose |
|---|---|
| `--md-primary`, `--md-on-primary`, … | `lightColorScheme(primary = …)` / `darkColorScheme(…)` → `MaterialTheme.colorScheme.primary` |
| `--md-surface-container-low` … `-highest` | `surfaceContainerLow` … `surfaceContainerHighest` |
| `--brano-1` … `--brano-6`, `--brano-testo` | Estensione propria: `data class ColoriBrani(val lista: List<Color>, val testo: Color)` fornita con `staticCompositionLocalOf`, una istanza per tema. Indice = `(posizione) % 6`. |
| `--tipo-display-large` … `--tipo-label-small` | `Typography(displayLarge = TextStyle(…), …)` con `FontFamily` di Bricolage Grotesque (display, headline, titleLarge) e Figtree (il resto). Font scaricabili da Google Fonts o inclusi in `res/font`. |
| `font-variant-numeric: tabular-nums` | `TextStyle(fontFeatureSettings = "tnum")` per durate e percentuali. |
| `--sp-1` … `--sp-12` | Oggetto `Spaziature` con valori in `dp` (4, 8, 12, 16, 24, 32, 48). |
| `--forma-xs` … `--forma-xl` | `Shapes(extraSmall = RoundedCornerShape(4.dp), small = 8.dp, medium = 12.dp, large = 16.dp, extraLarge = 28.dp)`; pillola = `CircleShape`. |
| `--curva-standard`, `--durata-*` | `MotionScheme` standard di M3 (150 / 300 ms). |

## Componenti → Compose

| Componente (CSS) | Compose |
|---|---|
| `.appbar`, `.appbar-grande` | `TopAppBar`, `LargeTopAppBar` |
| `.btn-pieno`, `.btn-tonale`, `.btn-contorno`, `.btn-testo` | `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton` |
| `.btn-icona`, `.btn-play` | `IconButton`, `FilledIconButton` (forma `large`, 72 dp) |
| `.selettore` | `SingleChoiceSegmentedButtonRow` + `SegmentedButton` |
| `.barra-mashup`, `.segmento`, `.testina` | Componente proprio `BarraMashup`: `Canvas` con un rettangolo per brano e `Brush.horizontalGradient` sulle giunture; nel risultato gestisce tocco e trascinamento per spostare l'ascolto. |
| `.numero` | `Box` 32 dp, forma `small`, colore dal set `ColoriBrani` |
| `.lista-brani`, `.riga-brano` | `LazyColumn` + `ListItem` con riordino tramite trascinamento (per es. la libreria `sh.calvin.reorderable`, da valutare); stato trascinato = `surfaceContainerHighest` + elevazione 3 dp. |
| `.limite` | Parte di `BarraMashup`: oltre la durata massima, strisce + bordo `error` |
| `.cursore`, `.impostazione` | `Slider(valueRange = 3f..30f, steps = 26)` in una voce delle impostazioni con il valore scritto (`10 min`) |
| `.progresso` | `LinearProgressIndicator` |
| `.passo`, `.fasi` | Riga propria: icona di stato + titolo + tre etichette di fase (Analisi, Taglio, Unione) |
| `.avviso`, `.avviso.errore` | `Card` con `secondaryContainer` / `errorContainer` |
| `.snackbar` | `Snackbar` in `SnackbarHost` |
| `.dialogo` | `AlertDialog` |
| `.stato-vuoto` | Colonna centrata con icona, titolo, testo e `Button` |

Icone: Material Symbols Rounded (`androidx.compose.material:material-icons-extended`
oppure i vettoriali scaricati da Google Fonts).

## Durata del mashup e limite

- La durata si sceglie in **Impostazioni** con un cursore da **3 a 30 minuti**, a passi di 1
  minuto; predefinita **10 minuti**. Va salvata (per es. con DataStore) e ricordata.
- **"Canzoni intere"**: se il totale dei brani è tra la durata scelta e la durata + 20%,
  l'app accorcia le canzoni sulle misure. Oltre il +20% (con 10 minuti: oltre 12:00) compare
  l'avviso "Troppe canzoni", con il pulsante "Apri impostazioni", e "Crea mashup" si disattiva.
  Esempio: 3:23 + 4:12 + 1:35 + 4:40 + 3:20 = 17:10 → avviso.
- Schermate: 1c "Troppe canzoni" e 4 "Impostazioni" in `schermate.html`.

## Testi dell'interfaccia

- Modalità: **Canzoni intere** / **Ritornelli** (etichette corte perché stiano nel selettore;
  la frase sotto spiega cosa fa ciascuna).
- Azioni: **Aggiungi mp3**, **Crea mashup**, **Annulla**, **Salva mp3**, **Nuovo mashup**.
- Fasi: **Analisi** (battiti e ritornello), **Taglio** (sulle misure), **Unione** (dissolvenza).
- Stati e errori previsti: lista vuota, meno di 2 brani (pulsante disattivato), brani troppo
  corti per 10 minuti, file non leggibile (saltato), ritornello non trovato (si usa il tratto
  più intenso, come in `estrai_ritornelli.py`), conferma prima di interrompere.

## Accessibilità

- Contrasto testo/sfondo ≥ 4,5:1 per tutte le 38 coppie di colori, in entrambi i temi
  (calcolato e mostrato in `catalogo.html`).
- Lo stato non è mai indicato dal solo colore: ogni brano ha il numero scritto, ogni fase ha
  icona e testo.
- Aree di tocco ≥ 48 dp; descrizioni per i pulsanti con sola icona.
- Animazioni ridotte se il sistema lo chiede (`prefers-reduced-motion` → in Android
  "Rimuovi animazioni").

## Da decidere nell'implementazione

- Numero minimo di brani (nel design: 2).
- Libreria per il riordino trascinando, oppure implementazione propria.
- Se l'elaborazione continua a schermo spento (servizio in primo piano con notifica).
