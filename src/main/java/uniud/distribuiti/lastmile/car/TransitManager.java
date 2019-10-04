package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.TransportRoute;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;

// Attore che gestisce il trasporto e lo spostamento
// di una macchina all'interno della rete geografica
public class TransitManager extends AbstractActorWithTimers {

    public static Props props(TransportRoute route, Location passengerLocation, ActorRef passenger){
        return Props.create(TransitManager.class, () -> new TransitManager(route, passengerLocation, passenger));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final TransportRoute route;
    private final Location passengerLocation;
    private final ActorRef passenger;
    private static Object TICK_KEY = "TransportTick";   // Chiave per i timer
    private static final class StartTick {}             // Tick inizio transito
    private static final class TransitTick {}           // Tick di transito

    public TransitManager(TransportRoute route, Location passengerLocation, ActorRef passenger){
        this.route = route;
        this.passenger = passenger;
        this.passengerLocation = passengerLocation;
        log.info("TRANSIT MANAGER STARTED");
        log.info("ROUTE: " + this.route.getRoute().getPath());
        getTimers().startSingleTimer(TICK_KEY, new StartTick(), Duration.ofMillis(1000));
    }

    private void goToNext(TransitTick msg){
        boolean hasNext = this.route.goToNext();        // Gestione comportamento tramite hasNext

        // Raggiunta la location del passeggero, informalo che la macchina è arrivata
        if(this.route.getCurrentNode() == this.passengerLocation.getNode()) {
            passenger.tell(new TransportCoordination.CarArrivedToPassenger(), getContext().parent());
        }

        // Quando ha raggiunto la fine del tragitto, informa macchina e passeggero
        if(!hasNext){
            endTransit();
        }

    }

    private void endTransit() {
        Location destination = new Location(this.route.getCurrentNode());
        context().actorSelection("/user/MACCHINA").tell(new TransportCoordination.DestinationReached(destination), getSelf());
        passenger.tell(new TransportCoordination.DestinationReached(destination), getSelf());
        getTimers().cancelAll();

        // TODO: Gestione fine vita TransitManager
        //  Valutare l'inserimento della terminazione dell'attore all'interno di Car
        //  ... (forse non è possibile data la gerarchia)
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(
                        StartTick.class,
                        msg -> {
                            getTimers().startPeriodicTimer(TICK_KEY, new TransitTick(), Duration.ofSeconds(5));
                        }
                )
                .match(
                        TransitTick.class,
                        this::goToNext
                )
                .build();
    }
}
