// Ponte JNI tra Mp3Encoder (Kotlin) e LAME.
// Riceve audio float planare in [-1, 1] e restituisce i byte mp3 prodotti.
#include <jni.h>
#include <stdint.h>
#include "lame.h"

#define FUNZ(nome) Java_com_imash_app_audio_Mp3Encoder_##nome

// Errori del ponte (negativi come quelli di LAME, ma fuori dal loro intervallo): Mp3Encoder li
// trasforma in eccezione.
#define ERRORE_ARGOMENTI (-1000)
#define ERRORE_MEMORIA   (-1001)

// perPezzi: l'mp3 si codifica a pezzi in parallelo e i frame si riattaccano (CodificaParallela),
// quindi ogni frame deve stare in piedi da solo: niente serbatoio di bit (a 320 kbps la differenza
// non si sente) e niente frame "Info" iniziale.
JNIEXPORT jlong JNICALL FUNZ(nativeInit)(JNIEnv *env, jclass cls,
                                         jint sampleRate, jint canali, jint kbps, jboolean perPezzi) {
    lame_t l = lame_init();
    if (!l) return 0;
    lame_set_in_samplerate(l, sampleRate);
    lame_set_out_samplerate(l, sampleRate);
    lame_set_num_channels(l, canali);
    lame_set_mode(l, canali == 1 ? MONO : JOINT_STEREO);
    lame_set_VBR(l, vbr_off);
    lame_set_brate(l, kbps);
    lame_set_quality(l, 5);  // 5 = predefinito di LAME: a 320 kbps non si sente la differenza dal 2, ~2× più veloce
    if (perPezzi) {
        lame_set_disable_reservoir(l, 1);
        lame_set_bWriteVbrTag(l, 0);
    }
    if (lame_init_params(l) < 0) {
        lame_close(l);
        return 0;
    }
    return (jlong) (intptr_t) l;
}

// destra può essere null (mono): in quel caso si usa sinistra per entrambi.
JNIEXPORT jint JNICALL FUNZ(nativeEncode)(JNIEnv *env, jclass cls, jlong handle,
                                          jfloatArray sinistra, jfloatArray destra,
                                          jint campioni, jbyteArray uscita) {
    lame_t l = (lame_t) (intptr_t) handle;
    // Mai leggere oltre la fine degli array (scansione Vantage, mval-002).
    if (!l || !sinistra || !uscita || campioni < 0 ||
        campioni > (*env)->GetArrayLength(env, sinistra) ||
        (destra && campioni > (*env)->GetArrayLength(env, destra))) {
        return ERRORE_ARGOMENTI;
    }
    jsize dim = (*env)->GetArrayLength(env, uscita);
    jfloat *s = (*env)->GetPrimitiveArrayCritical(env, sinistra, NULL);
    jfloat *d = NULL;
    jbyte *u = NULL;
    int n = ERRORE_MEMORIA;
    if (!s) return ERRORE_MEMORIA;
    d = destra ? (*env)->GetPrimitiveArrayCritical(env, destra, NULL) : s;
    if (d) {
        u = (*env)->GetPrimitiveArrayCritical(env, uscita, NULL);
        if (u) {
            n = lame_encode_buffer_ieee_float(l, s, d, campioni, (unsigned char *) u, dim);
            (*env)->ReleasePrimitiveArrayCritical(env, uscita, u, 0);
        }
        if (destra) (*env)->ReleasePrimitiveArrayCritical(env, destra, d, JNI_ABORT);
    }
    (*env)->ReleasePrimitiveArrayCritical(env, sinistra, s, JNI_ABORT);
    return n;
}

JNIEXPORT jint JNICALL FUNZ(nativeFlush)(JNIEnv *env, jclass cls, jlong handle,
                                         jbyteArray uscita) {
    lame_t l = (lame_t) (intptr_t) handle;
    if (!l || !uscita) return ERRORE_ARGOMENTI;
    jsize dim = (*env)->GetArrayLength(env, uscita);
    jbyte *u = (*env)->GetPrimitiveArrayCritical(env, uscita, NULL);
    if (!u) return ERRORE_MEMORIA;
    int n = lame_encode_flush(l, (unsigned char *) u, dim);
    (*env)->ReleasePrimitiveArrayCritical(env, uscita, u, 0);
    return n;
}

JNIEXPORT void JNICALL FUNZ(nativeClose)(JNIEnv *env, jclass cls, jlong handle) {
    if (handle) lame_close((lame_t) (intptr_t) handle);
}
