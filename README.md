# Accordatore vocale

App Android per allenare l'intonazione della voce.

* **Accordatore:** canti e l'app mostra la nota, la frequenza, lo scostamento in cent e il tracciato della voce nel tempo.
* **Esercizi:** sequenze di note (con durata in movimenti e tempo in BPM) da seguire su un grafico a rullo di pianoforte. Le barre scorrono verso la linea verticale e la tua voce appare come una linea verde, arancio o rossa a seconda di quanto sei vicino alla nota. Si possono trasporre di semitono o di ottava.
* **Risultato:** percentuale di tempo intonato, tendenza a crescere o calare, scostamento e oscillazione nota per nota.

## Come funziona

* Microfono a 44,1 kHz (sorgente `VOICE_RECOGNITION`, senza controllo automatico del volume), finestra di 4096 campioni con avanzamento di 1024: circa 43 misure al secondo.
* Rilevamento della fondamentale con l'algoritmo YIN e interpolazione parabolica: errore di circa 1/2 cent su segnali puliti nell'estensione della voce (Mi2 a Do6).
* Filtro sulle ultime misure per scartare gli errori di ottava isolati senza appiattire il vibrato.
* Il punteggio ignora i primi 150 ms di ogni nota (attacco) e usa la mediana degli scostamenti.

## Compilare

Ogni push su GitHub avvia la action *Build Android*, che esegue i test e produce l'APK di debug tra gli artifact della build (`accordatore-vocale-debug`).

In locale serve Android Studio (o JDK 17 e Android SDK 35):

```
./gradlew testDebugUnitTest assembleDebug
```

L'APK si trova in `app/build/outputs/apk/debug/app-debug.apk`.
