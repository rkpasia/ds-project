package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
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
        this.transportRequest.tell(new TransportCoordination.CarAvailableMsg(route.getDistance()), getSelf());

        // Iscrizione a ABORT_REQUEST channel
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe("ABORT_REQUEST", getSelf()), getSelf());
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
            getContext().getParent().tell(new TransportCoordination.CarBookingRequestMsg(passengerRef, route, passengerLocation), getSelf());
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

        if(msg instanceof TransportCoordination.CarHasBeenBooked){
            log.info("RICEVUTA NOTIFICA DA MACCHINA DI UNA PRENOTAZIONE DA UN ALTRO PASSEGGERO, RISPONDO A PASSEGGERO");
            this.status = RequestManagerStatus.NOT_AVAILABLE;
            transportRequest.tell(new TransportCoordination.CarUnavailableMsg(), getSelf());
            getContext().stop(getSelf());
        }
    }

    // Ricezione annullamento richiesta di trasporto
    private void abortRequest(TransportCoordination msg){
        // Verifica associazione richiesta
        if(transportRequest.equals(getSender())){
            // Interrompi il manager
            getContext().stop(getSelf());
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
                .match(
                        TransportCoordination.CarHasBeenBooked.class,
                        this::manageBookingRequest
                ).match(
                        TransportCoordination.CarBookingRejectMsg.class,
                        this::manageBookingRequest
                )
                .match(
                        TransportCoordination.AbortTransportRequest.class,
                        this::abortRequest
                )
                .match(DistributedPubSubMediator.SubscribeAck.class, msg -> log.info("ISCRITTO P<->S"))
                .matchAny(o -> log.info("MESSAGGIO NON SUPPORTATO"))
                .build();
    }
}
