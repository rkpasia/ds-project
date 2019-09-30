package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.util.ArrayList;

// TransportRequest actor
// Questo è l'attore responsabile della gestione di una richiesta
// di trasporto effettuata dal passeggero parent di questo attore
public class TransportRequest extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private TransportRequestStatus status;
    private enum TransportRequestStatus {
        EMITTED,
        BOOKING,
        CONFIRMED
    }

    private ArrayList<ActorRef> availableCars = new ArrayList<ActorRef>();

    public static Props props(){
        return Props.create(TransportRequest.class, () -> new TransportRequest());
    }

    public TransportRequest(){
        this.status = TransportRequestStatus.EMITTED;
    }

    @Override
    public void preStart(){
        System.out.println("TRANSPORT REQUEST STARTED");
    }

    private void evaluateCar(TransportCoordination msg){
        log.info("DISPONIBILITA RICEVUTA DA {}", getSender());

        // TODO: Implementazione più sofisticata della memoria

        // Considerare la creazione di un oggetto tupla <CarRef, CarType, EstTransTime>
        // - EstTransTime tempo di trasporto stimato
        availableCars.add(getSender());
    }

    // Selezione di una macchina che ha dato disponibilità al passeggero
    private void selectCar(TransportCoordination msg){
        // Con la strategia sottostante il software sarà molto flessibile perché permetterà l'implementazione
        // di tecniche di selezione più sofisticate.
        // TODO: Gestione della priorità di selezione.
        //  - ordinamento per più veloce
        //  - selezione per tipo più veloce
        //  - non valuterei l'inserimento di altre tipologie di selezioni al momento

        // Scelgo sempre la prima macchina che mi ha risposto per il trasporto
        log.info("PRENOTO LA MACCHINA {}", availableCars.get(0));
        availableCars.get(0).tell(new TransportCoordination.CarBookingRequestMsg(), getSelf());
    }

    // Metodo che riceve la conferma della prenotazione di una macchina
    // Dovrà notificare il passeggero che entrerà in relazione diretta con la macchina per la fase di coordinamento
    // dello stato del trasporto.
    private void bookingConfirmation(TransportCoordination msg){
        // TODO: Notifica passeggero della ricezione di conferma della prenotazione da parte di una macchina
        //  Questo è l'entry poit della fase due della richiesta, la fase di coordinamento dello stato del trasporto.
        log.info("MACCHINA PRENOTATA {}", getSender());
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(
                        TransportCoordination.CarAvailableMsg.class,
                        this::evaluateCar
                )
                .match(
                        TransportCoordination.SelectCarMsg.class,
                        this::selectCar
                )
                .match(
                        TransportCoordination.CarBookingConfirmedMsg.class,
                        this::bookingConfirmation
                )
                .matchAny(
                        o -> {
                            log.info("{} - MESSAGGIO NON SUPPORTATO - {}", getSelf(), o);
                        }
                )
                .build();
    }
}
