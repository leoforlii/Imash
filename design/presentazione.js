/* =====================================================================
   Solo per le pagine di presentazione del design.
   1. Ogni <template data-doppio="telefono|pannello"> viene mostrato due
      volte, in tema chiaro e scuro, senza duplicare il markup.
   2. I campioni di colori, tipografia, spaziature e forme sono generati
      leggendo i valori reali da tokens.css (niente valori copiati a mano).
   ===================================================================== */

const TEMI = [
  { nome: "chiaro", classe: "" },
  { nome: "scuro", classe: "tema-scuro" },
];

function creaTelefono(contenuto) {
  const telefono = document.createElement("div");
  telefono.className = "telefono app";
  telefono.innerHTML = `
    <div class="barra-stato" aria-hidden="true">
      <span>16:20</span>
      <span><span class="icona piena">signal_cellular_alt</span> <span class="icona piena">battery_5_bar</span></span>
    </div>
    <div class="schermo"></div>
    <div class="barra-gesti" aria-hidden="true"></div>`;
  telefono.querySelector(".schermo").append(contenuto);
  return telefono;
}

function creaPannello(contenuto) {
  const pannello = document.createElement("div");
  pannello.className = "pannello app";
  pannello.append(contenuto);
  return pannello;
}

function duplicaPerTema(template) {
  const tipo = template.dataset.doppio;
  const coppia = document.createElement("div");
  coppia.className = "coppia";
  for (const tema of TEMI) {
    const voce = document.createElement("figure");
    voce.className = "coppia-voce" + (tipo === "telefono" ? " telefono-voce" : "");
    const contenuto = template.content.cloneNode(true);
    const cornice = tipo === "telefono" ? creaTelefono(contenuto) : creaPannello(contenuto);
    if (tema.classe) cornice.classList.add(tema.classe);
    const didascalia = document.createElement("figcaption");
    didascalia.textContent = `Tema ${tema.nome}`;
    voce.append(cornice, didascalia);
    coppia.append(voce);
  }
  template.replaceWith(coppia);
}

/* ---------- Contrasto WCAG ---------- */
function luminanza(hex) {
  const n = hex.replace("#", "");
  const [r, g, b] = [0, 2, 4].map(i => parseInt(n.slice(i, i + 2), 16) / 255)
    .map(c => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}
function contrasto(a, b) {
  const [l1, l2] = [luminanza(a), luminanza(b)].sort((x, y) => y - x);
  return (l1 + 0.05) / (l2 + 0.05);
}
function leggiToken(elemento, nome) {
  return getComputedStyle(elemento).getPropertyValue(`--${nome}`).trim();
}

/* data-campioni="sfondo:testo sfondo:testo ..." */
function riempiCampioni(contenitore) {
  for (const coppia of contenitore.dataset.campioni.trim().split(/\s+/)) {
    const [sfondo, testo] = coppia.split(":");
    const hexSfondo = leggiToken(contenitore, sfondo);
    const hexTesto = leggiToken(contenitore, testo);
    const rapporto = contrasto(hexSfondo, hexTesto);
    const campione = document.createElement("div");
    campione.className = "campione";
    campione.style.background = `var(--${sfondo})`;
    campione.style.color = `var(--${testo})`;
    campione.innerHTML = `<b>${sfondo.replace(/^md-/, "")}</b>
      <small>${hexSfondo.toUpperCase()} · testo ${testo.replace(/^md-/, "")}</small>
      <small class="${rapporto >= 4.5 ? "contrasto-ok" : "contrasto-ko"}">${rapporto.toFixed(1).replace(".", ",")}:1</small>`;
    contenitore.append(campione);
  }
}

/* data-tipi="display-large headline-medium ..." */
function riempiTipi(contenitore) {
  const testo = contenitore.dataset.esempio || "Notte di luglio 10:00";
  for (const nome of contenitore.dataset.tipi.trim().split(/\s+/)) {
    const riga = document.createElement("div");
    riga.className = "tipo";
    riga.innerHTML = `<code>${nome}</code><span style="font: var(--tipo-${nome})">${testo}</span>`;
    contenitore.append(riga);
  }
}

/* data-misure="sp-1 sp-2 ..." (quadrati) oppure data-forme="forma-xs ..." */
function riempiMisure(contenitore) {
  const spaziature = contenitore.dataset.misure;
  const nomi = (spaziature || contenitore.dataset.forme).trim().split(/\s+/);
  for (const nome of nomi) {
    const valore = leggiToken(contenitore, nome);
    const voce = document.createElement("div");
    voce.className = "misura" + (spaziature ? "" : " forma");
    voce.innerHTML = spaziature
      ? `<i style="width: var(--${nome}); height: var(--${nome})"></i>${nome}<br>${valore}`
      : `<i style="border-radius: var(--${nome})"></i>${nome}<br>${valore}`;
    contenitore.append(voce);
  }
}

document.querySelectorAll("template[data-doppio]").forEach(duplicaPerTema);
document.querySelectorAll("[data-campioni]").forEach(riempiCampioni);
document.querySelectorAll("[data-tipi]").forEach(riempiTipi);
document.querySelectorAll("[data-misure], [data-forme]").forEach(riempiMisure);
