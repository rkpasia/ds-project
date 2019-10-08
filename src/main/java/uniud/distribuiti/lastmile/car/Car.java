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
import uniud.distribuiti.lastmile.location.LocationHelper;
import uniud.distribuiti.lastmile.location.TransportRoute;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;
import java.io.Serializable;

public class Car extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static class RequestStatusMsg {}

    public static Props props(){
        return Props.create(Car.class, () -> new Car());
    }

    // TODO: Verificare se ci sono altri serializzatori migliori
    public static class TransportRequestMessage implements Serializable {
        private final int destination;
        private final int passengerLocation;

        public TransportRequestMessage(int passengerLocation, int dest){
            this.passengerLocation = passengerLocation;
            this.destination = dest;
        }

        public int getDestination(){
            return this.destination;
        }

        public int getPassengerLocation(){
            return this.passengerLocation;
        }
    }

    private CarStatus status;
    public enum CarStatus {
        AVAILABLE,
        BOOKED,
        TRANSIT
    }

    private Location location;
    private Double fuel; // Carburante in litri
    private final Double kmPerLiter = 14.0;

    public Car(){
        this.status = CarStatus.AVAILABLE;
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", getSelf()), getSelf());

        // Inizializzazione standard carburante macchina
        this.fuel = 20.0;
        LocationHelper locationHelper = new LocationHelper();
        this.location = locationHelper.assignLocation();
    }

    private void carBooking(TransportCoordination.CarBookingRequestMsg msg){

        log.info("RICEVUTA RICHIESTA DI BOOKING");

        if(this.status == CarStatus.AVAILABLE) {
            log.info("SONO DISPONIBILE");
            this.status = CarStatus.BOOKED;

            getContext().getChildren().forEach(child ->{
                if(child != getSender()) child.tell(new TransportCoordination.CarHasBeenBooked(), getSelf());
                else getSender().tell(new TransportCoordination.CarBookingConfirmedMsg(), getSelf());
            });

            // Creazione TransitManager
            getContext().actorOf(Props.create(TransitManager.class, () -> new TransitManager(new TransportRoute(msg.route), msg.location, msg.passenger)), "TRANSIT_MANAGER@" + getSender().path().name());
        } else {
            log.info("NON SONO DISPONIBILE");
            getSender().tell(new TransportCoordination.CarBookingRejectMsg(), getSelf());
        }
    }

    // Metodo di valutazione della richiesta di trasporto
    private void evaluateRequest(TransportRequestMessage msg){
        if(this.status == CarStatus.AVAILABLE){
            log.info("VALUTAZIONE " + msg.toString());
            Route route = LocationHelper.defineRoute(this.location.getNode(), msg.getPassengerLocation(), msg.getDestination());
            if(haveEnoughFuel(route.getDistance())){
                log.info("CARBURANTE SUFFICIENTE - INVIO PROPOSTA");
                getContext().actorOf(TransportRequestMngr.props(getSender(), route, new Location(msg.getPassengerLocation())), "TRANSPORT_REQUEST_MANAGER@" + getSender().path().name());
            }
        } else {
            log.info("MACCHINA NON DISPONIBILE. IN TRANSITO OPPURE PRENOTATA.");
        }
    }

    private boolean haveEnoughFuel(int km){
        double fuelConsumption = km / this.kmPerLiter;
        double elapsedFuel = this.fuel - fuelConsumption;
        return (elapsedFuel) < 0 ? false : true;
    }

    private void transportCompleted(TransportCoordination.DestinationReached msg){
        this.location.setNode(msg.getLocation().getNode());
        log.info("PASSEGGERO TRASPORTATO A DESTINAZIONE");
        this.status = CarStatus.AVAILABLE;
        // TODO: La macchina deve aggiornare il proprio carburante
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(DistributedPubSubMediator.SubscribeAck.class, msg -> log.info("ISCRITTO RICEZIONE RICHIESTE"))
                .match(TransportCoordination.CarBookingRequestMsg.class, this::carBooking)
                .match(TransportRequestMessage.class, this::evaluateRequest)
                .match(TransportCoordination.DestinationReached.class, this::transportCompleted)
                .matchAny(o -> log.info("MESSAGGIO NON SUPPORTATO"))
                .build();
    }

}
