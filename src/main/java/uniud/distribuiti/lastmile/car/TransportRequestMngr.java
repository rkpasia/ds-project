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
        WAITING,            // In attesa di conferma da parte del passeggero
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
        this.status = RequestManagerStatus.WAITING;
    }

    // Metodo di gestione e forwarding della richiesta di prenotazione
    // Il metodo riceve e fa da intermediario con il TransportRequest del passeggero per confermare la prenotazione
    // della macchina. Se non riesce, viene mandato un messaggio di rifiuto prenotazione, che dovrà essere gestito
    // dal TransportRequest del passeggero.
    private void manageBookingRequest(TransportCoordination msg){
        log.info("GESTIONE BOOKING");

        if(msg instanceof TransportCoordination.CarBookingRequestMsg) {
            log.info("INOLTRO RICHIESTA A MACCHINA");
            getContext().getParent().tell(msg, getSelf());
        }

        if(msg instanceof TransportCoordination.CarBookingConfirmedMsg) {
            log.info("RICEVUTA CONFERMA DA MACCHINA, RISPONDO A PASSEGGERO");
            this.status = RequestManagerStatus.AVAILABLE;
            transportRequest.tell(msg, getContext().getParent());
        }

        if(msg instanceof TransportCoordination.CarBookingRejectMsg){
            log.info("RICEVUTA DISDETTA DA MACCHINA, RISPONDO A PASSEGGERO");
            this.status = RequestManagerStatus.NOT_AVAILABLE;
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
