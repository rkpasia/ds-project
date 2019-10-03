package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.Route;
import uniud.distribuiti.lastmile.location.TransportRoute;
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
    private ActorRef passengerRef;

    private RequestManagerStatus status;
    private enum RequestManagerStatus {
        WAITING,            // In attesa di conferma da parte del passeggero
        AVAILABLE,          // Macchina disponibile
        NOT_AVAILABLE,      // Macchina non disponibile
        EXPIRED,            // Si presume che la macchina non venga più considerata dal passeggero
        REJECTED,           // Macchina è stata respinta dal passeggero
        CONFIRMED,          // Macchina è stata confermata dal passeggero
    }

    // Percorso che dovrà fare la macchina
    private Route route;
    private Location passengerLocation;     // Location del passeggero

    public static Props props(ActorRef transportRequest, Route route, Location passengerLocation){
        return Props.create(TransportRequestMngr.class, () -> new TransportRequestMngr(transportRequest, route, passengerLocation));
    }

    public TransportRequestMngr(ActorRef transportRequest, Route route, Location passengerLocation){
        this.transportRequest = transportRequest;
        this.route = route;
        this.passengerLocation = passengerLocation;
        this.status = RequestManagerStatus.WAITING;
        this.transportRequest.tell(new TransportCoordination.CarAvailableMsg(), getSelf());
    }

    // Metodo di gestione e forwarding della richiesta di prenotazione
    // Il metodo riceve e fa da intermediario con il TransportRequest del passeggero per confermare la prenotazione
    // della macchina. Se non riesce, viene mandato un messaggio di rifiuto prenotazione, che dovrà essere gestito
    // dal TransportRequest del passeggero.
    private void manageBookingRequest(TransportCoordination msg){
        log.info("GESTIONE BOOKING");

        if(msg instanceof TransportCoordination.CarBookingRequestMsg) {
            log.info("INOLTRO RICHIESTA A MACCHINA");
            this.passengerRef = getSender();
            getContext().getParent().tell(msg, getSelf());
        }

        if(msg instanceof TransportCoordination.CarBookingConfirmedMsg) {
            log.info("RICEVUTA CONFERMA DA MACCHINA, RISPONDO A PASSEGGERO");
            this.status = RequestManagerStatus.AVAILABLE;
            setupTransitManager();
            transportRequest.tell(msg, getContext().getParent());
        }

        if(msg instanceof TransportCoordination.CarBookingRejectMsg){
            log.info("RICEVUTA DISDETTA DA MACCHINA, RISPONDO A PASSEGGERO");
            this.status = RequestManagerStatus.NOT_AVAILABLE;
            transportRequest.tell(msg, getSelf());
        }

        if(msg instanceof TransportCoordination.CarHasBeenBooked){
            log.info("RICEVUTA NOTIFICA DA MACCHINA DI UNA PRENOTAZIONE DA UN ALTRO PASSEGGERO");
            this.status = RequestManagerStatus.NOT_AVAILABLE;
            transportRequest.tell(msg, getSelf());
        }
    }

    private void setupTransitManager(){
        getContext().actorOf(Props.create(TransitManager.class, () -> new TransitManager(new TransportRoute(route), passengerLocation, this.passengerRef)));
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
                .match(
                        TransportCoordination.CarHasBeenBooked.class,
                        this::manageBookingRequest
                )
                .matchAny(
                        o -> {

                        }
                )
                .build();
    }
}
