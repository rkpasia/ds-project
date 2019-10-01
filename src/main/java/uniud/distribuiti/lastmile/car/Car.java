package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.LocationHelper;

import java.io.Serializable;

public class Car extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Car.class, () -> new Car());
    }

    // TODO: Verificare se ci sono altri serializzatori migliori
    public static class TransportRequestMessage implements Serializable {}

    private CarStatus status;
    private enum CarStatus {
        AVAILABLE,
        MATCHED,
        TRANSIT
    }

    private Location location;

    public Car(){
        this.status = CarStatus.AVAILABLE;
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", getSelf()), getSelf());

        this.location = LocationHelper.assignLocation();
    }

    private void evaluateRequest(TransportRequestMessage msg){
        System.out.println("VALUTAZIONE " + msg.toString());
        getContext().actorOf(TransportRequestMngr.props(getSender()), "CarTransportRequestManager");
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
                .match(TransportRequestMessage.class, this::evaluateRequest)
                .matchAny(o -> log.info("Messaggio non conosciuto"))
                .build();
    }

}
