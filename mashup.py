# Unisce gli mp3 di una cartella in un mashup con tagli sulle battute e
# crossfade. Ogni brano ha durata variabile (un numero intero di misure da 4/4).
# Uso: python mashup.py [cartella_mp3]   (default: cartella corrente)
import os
import sys
import glob

import numpy as np
import librosa
import soundfile as sf

BASE_DIR = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.getcwd()
OUT_FILE = os.path.join(BASE_DIR, "mashup.mp3")

SR = 44100
DURATA_TARGET = 600.0   # ~10 minuti
TOLLERANZA = 15.0       # accetta 585-615s
XF_MISURE = 2           # crossfade = 2 misure (8 quarti) del brano uscente
FADE_IN_SEC = 1.0
FADE_OUT_SEC = 4.0


def carica(path):
    y, _ = librosa.load(path, mono=False, sr=SR)
    if y.ndim == 1:
        y = np.stack([y, y])
    return y


def griglia_misure(y):
    """Rileva i battiti e restituisce (istanti di fine misura, sec per battito)."""
    mono = librosa.to_mono(y)
    tempo, beat_frames = librosa.beat.beat_track(y=mono, sr=SR)
    beat_times = librosa.frames_to_time(beat_frames, sr=SR)
    if len(beat_times) < 16:
        return None, None
    spb = float(np.median(np.diff(beat_times)))  # secondi per battito
    return beat_times[::4], spb


def normalizza_rms(y, target=0.12):
    rms = np.sqrt(np.mean(y ** 2)) + 1e-9
    return y * (target / rms)


def crossfade(a, b, n):
    """Dissolvenza equal-power: fine di `a` contro inizio di `b`."""
    t = np.linspace(0, np.pi / 2, n)
    overlap = a[:, -n:] * np.cos(t)[None, :] + b[:, :n] * np.sin(t)[None, :]
    return np.concatenate([a[:, :-n], overlap, b[:, n:]], axis=1)


def main():
    files = sorted(f for f in glob.glob(os.path.join(BASE_DIR, "*.mp3"))
                   if os.path.basename(f) != "mashup.mp3")
    if not files:
        print("Nessun mp3 in", BASE_DIR)
        return

    # per ogni brano: audio, fine misure disponibili, sec/battito
    dati = []
    for f in files:
        y = carica(f)
        misure, spb = griglia_misure(y)
        dati.append({"file": f, "y": y, "misure": misure, "spb": spb})

    # obiettivi diversi per brano (media ~38s), ognuno agganciato alla
    # misura piu' vicina -> durate variabili e tagli musicali
    obiettivi = [30.0, 34.0, 38.0, 42.0, 46.0]
    for i, d in enumerate(dati):
        target = obiettivi[i % len(obiettivi)]
        if d["misure"] is None or len(d["misure"]) < 10:
            d["durata"] = min(target, d["y"].shape[1] / SR)
            continue
        idx = int(np.argmin(np.abs(d["misure"] - target)))
        d["durata"] = float(max(d["misure"][idx], d["misure"][7]))

    def xf_sec(i):  # durata crossfade tra brano i e i+1 (2 misure di i)
        return min(max(XF_MISURE * 4 * (dati[i]["spb"] or 0.5), 2.5), 7.0)

    def totale(durate):
        return sum(durate) - sum(xf_sec(i) for i in range(len(dati) - 1))

    # aggiusta di una misura alla volta finche' non si e' vicini al target
    durate = [d["durata"] for d in dati]
    for _ in range(400):
        tot = totale(durate)
        if abs(tot - DURATA_TARGET) <= TOLLERANZA:
            break
        if tot > DURATA_TARGET:  # togli una misura al piu' lungo
            i = int(np.argmax(durate))
        else:                    # aggiungi una misura al piu' corto con spazio
            candidati = [j for j, d in enumerate(dati)
                         if d["misure"] is not None
                         and len(d["misure"]) > 0
                         and durate[j] + 4 * d["spb"] <= d["misure"][-1] + 1]
            if not candidati:
                break
            i = min(candidati, key=lambda j: durate[j])
        d = dati[i]
        if d["misure"] is None:
            durate[i] = max(10.0, durate[i] - 4.0) if tot > DURATA_TARGET \
                else min(d["y"].shape[1] / SR, durate[i] + 4.0)
            continue
        target = durate[i] - 4 * d["spb"] if tot > DURATA_TARGET \
            else durate[i] + 4 * d["spb"]
        validi = d["misure"][d["misure"] > 10]
        if len(validi) == 0:
            continue
        durate[i] = float(validi[np.argmin(np.abs(validi - target))])

    mashup = None
    print(f"{len(dati)} brani, crossfade = {XF_MISURE} misure\n")
    for i, (d, dur) in enumerate(zip(dati, durate)):
        seg = normalizza_rms(d["y"][:, :int(dur * SR)])
        if mashup is None:
            mashup = seg
        else:
            mashup = crossfade(mashup, seg, int(xf_sec(i - 1) * SR))
        print(f"  {dur:5.1f}s  {os.path.basename(d['file'])}")

    fi, fo = int(FADE_IN_SEC * SR), int(FADE_OUT_SEC * SR)
    mashup[:, :fi] *= np.linspace(0, 1, fi)
    mashup[:, -fo:] *= np.linspace(1, 0, fo)
    mashup *= 0.95 / np.max(np.abs(mashup))

    sf.write(OUT_FILE, mashup.T, SR, format="MP3", compression_level=0.0)
    tot = mashup.shape[1] / SR
    print(f"\n{OUT_FILE}\nDurata: {int(tot // 60)}:{int(tot % 60):02d} ({tot:.0f}s)")


if __name__ == "__main__":
    main()
