package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

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

        // Quando una macchina mi da disponibilità la richiedo subito [greedy] (proof of work)
        getSender().tell(new TransportCoordination.CarBookingRequestMsg(), getSelf());
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
