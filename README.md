# Progetto Laboratorio III
**Studente:** Francesco Lionti (Matricola: 658189)

Questo progetto implementa un'applicazione Client-Server per un gioco basato su parole, sviluppata in Java. Il sistema fa uso di architetture di rete ibride (TCP via NIO e UDP per il broadcast asincrono), multithreading e concorrenza avanzata, con messaggistica strutturata in formato JSON.

## Scelte Progettuali

* **Aggiornamento Differito delle Classifiche:** Le statistiche globali (Win Rate, Streak, ecc.) vengono aggiornate solo al termine di ogni partita.
* **Partecipazione "Opt-in":** L'inserimento nelle partite non è automatico al susseguirsi dei match. Per evitare utenti inattivi (AFK), un giocatore partecipa a un match successivo solo se invia almeno una proposta (`submitProposal`).

## Architettura del Sistema

### Server
Il server ha un'architettura multithread concorrente che separa il livello di rete dalla logica di gioco:
* **Gestione Connessioni (NIO):** `ServerMain` gestisce le connessioni TCP con `ServerSocketChannel` in modalità NIO, delegando i task ai `ServerWorker` tramite un `CachedThreadPool` per garantire la massima scalabilità.
* **Controller e Concorrenza:** La logica centrale risiede in `ServerController`. L'accesso concorrente ai dati è protetto tramite `ConcurrentHashMap` e metodi con blocchi `synchronized`.
* **Eventi e UDP:** Un `GameTimerManager` gestisce la durata delle partite. Allo scadere del tempo, `UdpNotificationManager` invia notifiche broadcast UDP asincrone ai client contenenti le classifiche finali.
* **Persistenza e Streaming:** I dati (profili utente, storici, classifiche) sono serializzati su file JSON tramite la libreria Gson. Il caricamento delle partite dal dizionario avviene tramite uno streaming sequenziale (`GameProvider`) per non saturare la memoria RAM.

### Client
L'architettura lato client separa l'I/O interattivo (sincrono) dalla ricezione passiva degli eventi (asincrona):
* **Main Thread (TCP):** Gestisce l'input utente tramite interfaccia a riga di comando (CLI) e la comunicazione sincrona NIO verso il server per le richieste di gioco.
* **Thread Asincrono (UDP):** `UdpListener` resta in background per ascoltare i broadcast di fine partita dal server, utilizzando variabili `volatile` per sincronizzarsi con lo stato di login gestito dal thread principale.

## Protocollo di Comunicazione

La comunicazione si basa su payload JSON (estrapolati e istanziati a runtime tramite il pattern Factory Method). Le richieste e le risposte ereditano dalle classi astratte e polimorfiche `Request` e `Response`. Il campo `"operation"` funge da chiave per determinare quale oggetto concreto creare, rendendo il protocollo altamente scalabile.

Tutta la lettura dei pacchetti TCP è gestita byte per byte fino all'incontro del carattere di terminazione `\n`, al fine di preservare correttamente la codifica UTF-8 (logica incapsulata in `NioUtils`).

## Manuale Utente (CLI)

Il client offre un'interfaccia a riga di comando intuitiva suddivisa in due stati principali. È sufficiente digitare il numero corrispondente per eseguire il comando:

**Menu Non Autenticato:**
* `1` - Registrati
* `2` - Accedi
* `3` - Aggiorna Credenziali
* `0` - Esci

**Menu Autenticato:**
* `1` - Aggiorna Credenziali
* `2` - Logout
* `3` - Nuova Proposta
* `4` - Stato/Esito Partita (mostra anche il tempo rimanente)
* `5` - Statistiche Partita
* `6` - Classifica Globale
* `7` - Statistiche Personali
* `0` - Esci

> **Nota:** Poiché le notifiche di fine partita UDP sono asincrone, potrebbero apparire visivamente mentre si sta digitando un comando. Si consiglia di attendere la notifica quando si è a ridosso della scadenza del timer di gioco (consultabile dal comando Stato/Esito Partita) per evitare sovrapposizioni grafiche sul terminale.

## Istruzioni per Compilazione e Avvio

**Prerequisiti:** La libreria esterna Gson è già inclusa nella directory `lib/`. Non sono richieste installazioni aggiuntive.

### Compilazione Universale
Posizionarsi nella root del progetto (directory principale) ed eseguire il seguente comando su una singola riga:

```bash
javac -d bin -cp "lib/*" src/client/*.java src/server/*.java src/server/model/*.java src/protocol/**/*.java src/utils/*.java
```

## Avvio (Mac / Linux)
Avvia il server (in un terminale)
```bash
java -cp "bin:lib/*" server.ServerMain
```

Avvia il client (in un secondo terminale)
```bash
java -cp "bin:lib/*" client.ClientMain
```

## Avvio (Windows)
Avvia il server (in un terminale)
```bash
java -cp "bin;lib/*" server.ServerMain
```
Avvia il client (in un secondo terminale)
```bash
java -cp "bin;lib/*" client.ClientMain
```
## Avvio tramite JAR
Avvia il server
```bash
java -jar Server.jar
```
Avvia il client
```bash
java -jar Client.jar
```
