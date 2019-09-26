package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Car extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Car.class, () -> new Car());
    }

    public Car(){}

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(
                        String.class,
                        s -> {
                            log.info("Ricevuto {} da {}", s, getSender());
                            getSender().tell("DISPONIBILE", getSelf());
                        })
                .matchAny(o -> log.info("Messaggio non conosciuto"))
                .build();
    }

}
