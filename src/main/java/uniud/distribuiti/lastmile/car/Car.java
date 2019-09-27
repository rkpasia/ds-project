package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Car extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Car.class, () -> new Car());
    }

    private enum CarStatus {
        AVAILABLE,
        MATCHED,
        TRANSIT
    }

    private CarStatus status;

    public Car(){
        this.status = CarStatus.AVAILABLE;
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", getSelf()), getSelf());
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(
                        String.class,
                        s -> {
                            log.info("Ricevuto {} da {}", s, getSender());
                            if(this.status == CarStatus.AVAILABLE){
                                getSender().tell("DISPONIBILE", getSelf());
                                this.status = CarStatus.MATCHED;
                            } else {
                                log.info("NON DISPONIBILE");
                            }

                        })
                .match(DistributedPubSubMediator.SubscribeAck.class, msg -> log.info("subscribed"))
                .matchAny(o -> log.info("Messaggio non conosciuto"))
                .build();
    }

}
