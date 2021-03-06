package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.car.engine.Engine;
import uniud.distribuiti.lastmile.car.fuel.FuelTank;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.LocationHelper;
import uniud.distribuiti.lastmile.location.Route;
import uniud.distribuiti.lastmile.location.TransportRoute;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.io.Serializable;
import java.time.Duration;

public class Car extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private CarStatus status;
    public enum CarStatus {
        AVAILABLE,
        REFUEL,
        TRANSIT_TO_PASSENGER,
        TRANSIT_WITH_PASSENGER,
        BROKEN
    }

    private Location location;
    private final FuelTank fuelTank;
    private final Engine engine;
    private ActorRef passenger;
    private ActorRef bookingManager;
    private ActorRef transitManager;

    public static Props props(){
        return Props.create(Car.class, Car::new);
    }

    public Car(){
        this.status = CarStatus.AVAILABLE;
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", getSelf()), getSelf());

        // Inizializzazione standard proprietà macchina
        this.fuelTank = new FuelTank(20);
        this.engine = new Engine(14.00);
        try {
            LocationHelper locationHelper = new LocationHelper();
            this.location = locationHelper.assignLocation();
        }catch (Exception ex){
                log.info(ex.getMessage());
        }
        log.info("MACCHINA ISTANZIATA");
    }

    public static class TransportRequestMessage implements Serializable {
        private final int destination;
        private final int passengerLocation;

        public TransportRequestMessage(int passengerLocation, int dest){
            this.passengerLocation = passengerLocation;
            this.destination = dest;
        }

        int getDestination(){
            return this.destination;
        }

        int getPassengerLocation(){
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

    private static class RefuelCompleted {}

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
            transitManager = getContext().actorOf(TransitManager.props(new TransportRoute(msg.route), msg.location, msg.passenger), "TRANSIT_MANAGER");

            // Monitoring attori TransitManager e Passeggero
            getContext().watch(transitManager);
            getContext().watch(msg.passenger);

            // Aggiornamento variabili di stato
            this.status = CarStatus.TRANSIT_TO_PASSENGER;
            this.passenger = msg.passenger;
            this.bookingManager = getSender();
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
                boolean existManager = getContext().findChild("TRANSPORT_REQUEST_MANAGER@" +getSender().path().uid()).isPresent();
                // se mi arriva una nuova richiesta di trasporto ma ho gia un trm associato alla macchina
                // il passeggero potrebbe aver avuto un problema quindi fermo e ricreo il trm
                if(! existManager) {
                    log.info("CARBURANTE SUFFICIENTE - INVIO PROPOSTA");
                    ActorRef manager = getContext().actorOf(TransportRequestMngr.props(getSender(), route, new Location(msg.getPassengerLocation())), "TRANSPORT_REQUEST_MANAGER@" + getSender().path().uid());
                    // Monitoring manager
                    getContext().watch(manager);
                }else{
                    log.info("TRANSPORT_REQUEST_MANAGER GIA CREATO");
                }
            }
        }
    }

    private boolean haveEnoughFuel(int km){
        // Fuel consumption data dal tipo di engine
        // Il kilometraggio è dato dal percorso che deve fare
        // Il risultato è dato dall'interazione tra FuelTank e Engine
        double fuelConsumption = engine.fuelConsumption(km);
        return fuelTank.hasEnoughFuel(fuelConsumption);
    }

    private void updateLocation(TransportCoordination.UpdateLocation msg){
        this.location.setNode(msg.location);
    }

    private void transportCompleted(TransportCoordination.DestinationReached msg){
        this.location.setNode(msg.getLocation().getNode());
        log.info("PASSEGGERO TRASPORTATO A DESTINAZIONE");
        // Aggiorna carburante rimasto all'interno del serbatoio
        this.fuelTank.fuelConsumed(engine.fuelConsumption(msg.getDistanceCovered()));
        // Verifica se c'è necessità di fare rifornimento
        if (fuelTank.needFuel()) {
            this.status = CarStatus.REFUEL;
            context().system().scheduler()
                    .scheduleOnce(
                            Duration.ofSeconds(10),
                            getSelf(),
                            new Car.RefuelCompleted(),
                            context().system().dispatcher(),
                            null
                    );
        } else {
            this.status = CarStatus.AVAILABLE;
        }
        getContext().unwatch(passenger);
        getContext().unwatch(bookingManager);
        getContext().unwatch(transitManager);
        getContext().stop(transitManager);
        getContext().stop(bookingManager);
    }

    private void abortTransportRequest(TransportCoordination msg){
        // Ricevo dal manager il messaggio di annullamento della transport request
        // Fermo il manager
        log.info("ABORT TRANSPORT REQUEST " + getSender().path().name());
        getContext().unwatch(getSender());
        getContext().stop(getSender());
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

    private void carRefuelCompleted(RefuelCompleted msg){
        this.fuelTank.refuel();
        this.status = CarStatus.AVAILABLE;
    }

    // Metodo gestione terminazione attori che sta monitorando
    private void terminationHandling(Terminated msg){

        log.info("RILEVATA LA MORTE DI UN ATTORE " + msg.actor().path().name());
        
        // Gestione terminazione TransportRequestManager
        // Se termina il manager che ha prenotato la macchina, ed essa è in transito verso il passeggero
        // potrebbe essere che il passeggero non sappia che io ho accettato di trasportarlo.
        // La terminazione di qualsiasi altro TransitManager non è un problema rilevante.
        if(msg.getActor().equals(bookingManager) && this.status == CarStatus.TRANSIT_TO_PASSENGER){
            // Invio una conferma per certezza al passeggero
            passenger.tell(new TransportCoordination.CarBookingConfirmedMsg(), getSelf());
        }

        // TransitManager è terminato
        // Gestione terminazione TransitManager
        // Che cosa succede quando muore il transit manager? Che cosa vuol dire?
        else if(msg.actor().equals(transitManager)){
            log.warning("TRANSIT_MANAGER TERMINATO");
            // Supponiamo che la terminazione del TransitManager implichi un problema fisico della macchina
            // Se in transito con il passeggero, notifico dove è avvenuto il guasto
            if (this.status == CarStatus.TRANSIT_WITH_PASSENGER) passenger.tell(new Car.BrokenLocation(this.location), getSelf());
            // Se in transito verso il passeggero, notifico che la macchina ha avuto un guasto
            else if (this.status == CarStatus.TRANSIT_TO_PASSENGER) passenger.tell(new Car.CarBreakDown(), getSelf());
        }

        // Il passeggero di questa macchina è terminato e la macchina sta arrivando da lui
        else if(this.status == CarStatus.TRANSIT_TO_PASSENGER && msg.actor().equals(passenger)){
            log.warning("PASSEGGERO TERMINATO");
            // Fermo il transit manager
            if (getContext().findChild("TRANSIT_MANAGER").isPresent()) {
                ActorRef transitManager = getContext().findChild("TRANSIT_MANAGER").orElse(null);
                getContext().unwatch(transitManager);
                getContext().stop(transitManager);
                getContext().stop(bookingManager);
            }
            // Macchina torna disponibile
            this.status = CarStatus.AVAILABLE;
        }
    }



    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(DistributedPubSubMediator.SubscribeAck.class, msg -> log.info("ISCRITTO RICEZIONE RICHIESTE"))
                .match(TransportRequestMessage.class, this::evaluateRequest)
                .match(TransportCoordination.CarBookingRequestMsg.class, this::carBooking)
                .match(TransportCoordination.DestinationReached.class, this::transportCompleted)
                .match(TransportCoordination.AbortTransportRequest.class, this::abortTransportRequest)
                .match(TransportCoordination.UpdateLocation.class, this::updateLocation)
                .match(CarBreakDown.class, this::carBroken)
                .match(BrokenLocation.class, this::carBrokenLocation)
                .match(RefuelCompleted.class, this::carRefuelCompleted)
                .match(TransportCoordination.CarArrivedToPassenger.class, msg -> this.status= CarStatus.TRANSIT_WITH_PASSENGER)
                .match(Terminated.class, this::terminationHandling)
                .matchAny(o -> log.info("MESSAGGIO NON SUPPORTATO"))
                .build();
    }

}
