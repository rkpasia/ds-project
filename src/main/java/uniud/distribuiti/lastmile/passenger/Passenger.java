package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Passenger extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Passenger.class, () -> new Passenger());
    }

    ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();

    public Passenger(){}

    @Override
    public Receive createReceive(){

        return receiveBuilder()
                .match(
                        String.class,
                        s -> {
                            log.info("Ricevuto {} da {}", s, getSender());
                            mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", "RICHIESTA"), getSelf());
                        }
                )
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
