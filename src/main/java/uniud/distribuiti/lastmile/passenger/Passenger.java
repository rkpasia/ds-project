package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

public class Passenger extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Passenger.class, () -> new Passenger());
    }

    public static class EmitRequestMessage {}

    public static class SelectCarMessage {}

    ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();

    private ActorRef transportRequest;

    public Passenger(){}

    // Inoltro richiesta di trasporto
    // Inizializzo nuovo attore
    // Inoltro richiesta con riferimento all attore figlio gestore della mia richiesta
    private void emitTransportRequest(EmitRequestMessage msg){
        transportRequest = getContext().actorOf(TransportRequest.props(), "PassengerTransportRequest");
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage()), transportRequest);
    }

    private void selectCar(SelectCarMessage msg){
        transportRequest.tell(new TransportCoordination.SelectCarMsg(), getSelf());
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
                .match(SelectCarMessage.class, this::selectCar)
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
