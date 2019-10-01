package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

// Transport Request Manager acotr
// Attore responsabile della gestione di una richiesta
// ricevuta da un passeggero.
// - valuta la disponibilita della macchiana a soddisfare la richiesta
// - controlla distanza
public class TransportRequestMngr extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    // Riferimento ad attore per coordinamento richiesta di trasporto
    private ActorRef transportRequest;

    private RequestManagerStatus status;
    private enum RequestManagerStatus {
        EVALUATION,         // Valutazione richiesta
        AVAILABLE,          // Macchina disponibile
        NOT_AVAILABLE,      // Macchina non disponibile
        EXPIRED,            // Si presume che la macchina non venga più considerata dal passeggero
        REJECTED,           // Macchina è stata respinta dal passeggero
        CONFIRMED,          // Macchina è stata confermata dal passeggero
    }

    public static Props props(ActorRef transportRequest){
        return Props.create(TransportRequestMngr.class, () -> new TransportRequestMngr(transportRequest));
    }

    public TransportRequestMngr(ActorRef transportRequest){
        this.transportRequest = transportRequest;
        this.status = RequestManagerStatus.EVALUATION;
        this.transportRequestEvaluation();
    }

    // Metodo di valutazione della richiesta di trasporto
    private void transportRequestEvaluation(){
        // TODO: Implementazione valutazione della richiesta
        //  - considerare la posizione del passeggero
        //  - considerare il carburante a disposizione del mio parent
        //  - formulazione di una risposta per la richiesta (anche in caso negativo?)
        
        // Risposta fake di disponibilità
        transportRequest.tell(new TransportCoordination.CarAvailableMsg(), getSelf());
    }

    private void manageBookingRequest(TransportCoordination msg){
        // TODO: Verifica disponibilità a prenotare la macchina
        //  Questo metodo verifica che la macchina sia disponibile
        //  successivamente risponde con la disponibilità

        log.info("GESTIONE BOOKING");

        if(msg instanceof TransportCoordination.CarBookingRequestMsg) {
            log.info("INOLTRO RICHIESTA A MACCHINA");
            getContext().getParent().tell(msg, getSelf());
        }

        if(msg instanceof TransportCoordination.CarBookingConfirmedMsg) {
            log.info("RICEVUTA CONFERMA DA MACCHINA, RISPONDO A PASSEGGERO");
            transportRequest.tell(msg, getContext().getParent());
        }

        if(msg instanceof TransportCoordination.CarBookingRejectMsg){
            log.info("RICEVUTA DISDETTA DA MACCHINA, RISPONDO A PASSEGGERO");
            transportRequest.tell(msg, getSelf());
        }
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(
                        TransportCoordination.CarBookingRequestMsg.class,
                        this::manageBookingRequest
                )
                .match(
                        TransportCoordination.CarBookingConfirmedMsg.class,
                        this::manageBookingRequest
                )
                .matchAny(
                        o -> {

                        }
                )
                .build();
    }
}
