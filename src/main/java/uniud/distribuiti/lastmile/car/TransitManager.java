package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.AbstractActorWithTimers;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.location.TransportRoute;

import java.time.Duration;

// Attore che gestisce il trasporto e lo spostamento
// di una macchina all'interno della rete geografica
public class TransitManager extends AbstractActorWithTimers {

    public static Props props(TransportRoute route){
        return Props.create(TransitManager.class, () -> new TransitManager(route));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final TransportRoute route;
    private static Object TICK_KEY = "TransportTick";   // Chiave per i timer
    private static final class StartTick {}             // Tick inizio transito
    private static final class TransitTick {}           // Tick di transito

    public TransitManager(TransportRoute route){
        this.route = route;
        log.info("TRANSIT MANAGER STARTED");
        log.info("ROUTE: " + this.route.getRoute().getPath());
        getTimers().startSingleTimer(TICK_KEY, new StartTick(), Duration.ofMillis(1000));
    }

    private void goToNext(TransitTick msg){
        boolean hasNext = this.route.goToNext();        // Gestione comportamento tramite hasNext
        System.out.println(this.route.getCurrentNode());
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
