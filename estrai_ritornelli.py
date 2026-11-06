# Estrae il ritornello (sezione piu' ripetuta) da ogni mp3 della cartella.
# Uso: python estrai_ritornelli.py [cartella_mp3]   (default: cartella corrente)
import os
import sys
import glob

import scipy.ndimage
import scipy.signal

# pychorus 0.1 usa scipy.ndimage.filters, rimosso da scipy >= 1.9
if not hasattr(scipy.ndimage, "filters"):
    scipy.ndimage.filters = scipy.ndimage

import numpy as np
import librosa
import soundfile as sf

from pychorus.constants import N_FFT, SMOOTHING_SIZE_SEC, OVERLAP_PERCENT_MARGIN
from pychorus.similarity_matrix import TimeTimeSimilarityMatrix, TimeLagSimilarityMatrix
from pychorus.helpers import (
    local_maxima_rows, detect_lines, count_overlapping_lines, best_segment,
)


MIN_CLIP_SEC = 30          # durata minima cercata
MAX_CLIP_SEC = 60          # durata massima estratta
LUNGHEZZA_FISSA = 60.0     # finestra dal ritornello (include coda/strofa)


def trova_ritornello(chroma, song_length_sec, clip_length):
    """Come pychorus.find_chorus ma restituisce (start, end) in secondi."""
    num_samples = chroma.shape[1]
    tt = TimeTimeSimilarityMatrix(chroma, 0)
    tl = TimeLagSimilarityMatrix(chroma, 0)
    chroma_sr = num_samples / song_length_sec
    tl.denoise(tt.matrix, int(SMOOTHING_SIZE_SEC * chroma_sr))
    rows = local_maxima_rows(tl.matrix)
    lines = detect_lines(tl.matrix, rows, clip_length * chroma_sr)
    if not lines:
        return None
    scores = count_overlapping_lines(
        lines, OVERLAP_PERCENT_MARGIN * clip_length * chroma_sr,
        clip_length * chroma_sr)
    best = best_segment(scores)
    return best.start / chroma_sr, min(best.end / chroma_sr,
                                       best.start / chroma_sr + MAX_CLIP_SEC)


def segmento_piu_forte(y, sr, durata=20.0):
    """Fallback: finestra di `durata` secondi con energia media massima."""
    win = int(durata * sr)
    if len(y) <= win:
        return 0.0, len(y) / sr
    energia = np.convolve(y ** 2, np.ones(win) / win, mode="valid")
    start = int(np.argmax(energia)) / sr
    return start, start + durata


def processa(path_mp3, out_dir):
    y, sr = librosa.load(path_mp3, mono=True)  # analisi a 22050 Hz mono
    durata = len(y) / sr
    S = np.abs(librosa.stft(y, n_fft=N_FFT)) ** 2
    chroma = librosa.feature.chroma_stft(S=S, sr=sr)

    res = trova_ritornello(chroma, durata, MIN_CLIP_SEC)
    metodo = "ripetizione"
    if res is None:  # riprova con segmento piu' corto
        res = trova_ritornello(chroma, durata, 10)
    if res is None:  # fallback su energia
        res = segmento_piu_forte(y, sr)
        metodo = "energia(fallback)"

    start = res[0]
    end = min(start + LUNGHEZZA_FISSA, durata)
    if end - start < LUNGHEZZA_FISSA and durata > LUNGHEZZA_FISSA:
        # il ritornello e' vicino alla fine: sposta la finestra indietro
        start = durata - LUNGHEZZA_FISSA
        end = durata
    # output in stereo al sample rate originale
    y_out, sr_out = librosa.load(path_mp3, mono=False, sr=None)
    seg = y_out[:, int(start * sr_out):int(end * sr_out)]
    base = os.path.splitext(os.path.basename(path_mp3))[0]
    out = os.path.join(out_dir, base + " - ritornello.mp3")
    sf.write(out, seg.T, sr_out, format="MP3", compression_level=0.0)
    return out, start, end, metodo


def main(in_dir):
    if not os.path.isdir(in_dir):
        print("Cartella non trovata:", in_dir)
        sys.exit(1)
    out_dir = os.path.join(in_dir, "ritornelli")
    os.makedirs(out_dir, exist_ok=True)
    files = sorted(f for f in glob.glob(os.path.join(in_dir, "*.mp3"))
                   if os.path.basename(f) != "mashup.mp3")
    print(f"{len(files)} file trovati in {in_dir}\n")
    for f in files:
        nome = os.path.basename(f)
        try:
            out, start, end, metodo = processa(f, out_dir)
            print(f"[OK] {nome}")
            print(f"     ritornello {start:6.1f}s - {end:6.1f}s "
                  f"({end - start:.1f}s, {metodo})")
        except Exception as e:
            print(f"[ERRORE] {nome}: {e}")


if __name__ == "__main__":
    in_dir = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.getcwd()
    main(in_dir)
