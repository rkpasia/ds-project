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

    private CarStatus status;
    public enum CarStatus {
        AVAILABLE,
        BOOKED,
        TRANSIT,
        BROKEN
    }

    private Location location;
    private Double fuel; // Carburante in litri
    private final Double kmPerLiter = 14.0;

    public static Props props(){
        return Props.create(Car.class, () -> new Car());
    }

    public Car(){
        this.status = CarStatus.AVAILABLE;
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", getSelf()), getSelf());

        // Inizializzazione standard carburante macchina
        this.fuel = 20.0;
        LocationHelper locationHelper = new LocationHelper();
        this.location = locationHelper.assignLocation();
    }

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

    public static class CarBreakDown implements Serializable {}

    public static class BrokenLocation implements Serializable {

        public final Location location;

        public BrokenLocation(Location location){
            this.location = location;
        }
    }

    private void carBooking(TransportCoordination.CarBookingRequestMsg msg){

        log.info("RICEVUTA RICHIESTA DI BOOKING");

        if(this.status == CarStatus.AVAILABLE) {
            log.info("SONO DISPONIBILE");

            // Notifica ricezione booking e indisponibilità/disponibilità ai propri manager figli
            getContext().getChildren().forEach(child ->{
                if(child != getSender()) child.tell(new TransportCoordination.CarHasBeenBooked(), getSelf());
                else getSender().tell(new TransportCoordination.CarBookingConfirmedMsg(), getSelf());
            });

            // Creazione TransitManager
            getContext().actorOf(Props.create(TransitManager.class, () -> new TransitManager(new TransportRoute(msg.route), msg.location, msg.passenger)), "TRANSIT_MANAGER");
            // Macchina inizia transito verso passeggero
            this.status = CarStatus.TRANSIT;
        } else {
            log.info("NON SONO DISPONIBILE");
            getSender().tell(new TransportCoordination.CarBookingRejectMsg(), getSelf());
        }
    }

    // Metodo di valutazione della richiesta di trasporto
    private void evaluateRequest(TransportRequestMessage msg){

        if(this.status==CarStatus.AVAILABLE) {
            log.info("VALUTAZIONE " + msg.toString());
            Route route = LocationHelper.defineRoute(this.location.getNode(), msg.getPassengerLocation(), msg.getDestination());
            if (haveEnoughFuel(route.getDistance())) {
                Boolean existChild = getContext().findChild("TRANSPORT_REQUEST_MANAGER@" + getSender().path().name()).isPresent();
                if(! existChild) {
                    log.info("CARBURANTE SUFFICIENTE - INVIO PROPOSTA");
                    getContext().actorOf(TransportRequestMngr.props(getSender(), route, new Location(msg.getPassengerLocation())), "TRANSPORT_REQUEST_MANAGER@" + getSender().path().name());
                }else  log.info("TRANSPORT_REQUEST_MANAGER GIA CREATO");
            }
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

    // Gestione guasto anomalo macchina
    private void carBroken(CarBreakDown msg){
        log.info("MACCHINA GUASTA");
        this.status = CarStatus.BROKEN;
        if (getContext().findChild("TRANSIT_MANAGER").isPresent())
            getContext().actorSelection("TRANSIT_MANAGER").tell(new CarBreakDown(), getSelf());
    }

    private void carBrokenLocation(BrokenLocation msg){
        log.info("MACCHINA FERMATA");
        this.location.setNode(msg.location.getNode());
        // Manda un messaggio broadcast per richiedere una nuova macchina per il passeggero!

    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(DistributedPubSubMediator.SubscribeAck.class, msg -> log.info("ISCRITTO RICEZIONE RICHIESTE"))
                .match(TransportCoordination.CarBookingRequestMsg.class, this::carBooking)
                .match(TransportRequestMessage.class, this::evaluateRequest)
                .match(TransportCoordination.DestinationReached.class, this::transportCompleted)
                .match(CarBreakDown.class, this::carBroken)
                .match(BrokenLocation.class, this::carBrokenLocation)
                .matchAny(o -> log.info("MESSAGGIO NON SUPPORTATO"))
                .build();
    }

}
