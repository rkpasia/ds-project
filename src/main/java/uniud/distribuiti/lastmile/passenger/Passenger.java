package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.car.Car;

public class Passenger extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Passenger.class, () -> new Passenger());
    }

    public static class EmitRequestMessage {}

    ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();

    public Passenger(){}

    // Inoltro richiesta di trasporto
    // Inizializzo nuovo attore
    // Inoltro richiesta con riferimento all attore figlio gestore della mia richiesta
    private void emitTransportRequest(EmitRequestMessage msg){
        ActorRef transportRequest = getContext().actorOf(TransportRequest.props(), "TransportRequest");
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage()), transportRequest);
    }

    @Override
    public Receive createReceive(){

        return receiveBuilder()
                .match(
                        String.class,
                        s -> {
                            log.info("Ricevuto {} da {}", s, getSender());
                        }
                )
                .match(EmitRequestMessage.class, this::emitTransportRequest)
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
