package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.LocationHelper;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

public class Passenger extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Passenger.class, () -> new Passenger());
    }

    public static class EmitRequestMessage {}

    public static class SelectCarMessage {}

    public static class CarArrivedMessage {}

    private ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();

    private ActorRef transportRequest;
    private ActorRef car;

    private Location location;

    public Passenger(){
        LocationHelper locationHelper = new LocationHelper();
        this.location = locationHelper.assignLocation();
    }

    // Inoltro richiesta di trasporto
    // Inizializzo nuovo attore
    // Inoltro richiesta con riferimento all attore figlio gestore della mia richiesta
    private void emitTransportRequest(EmitRequestMessage msg){
        transportRequest = getContext().actorOf(TransportRequest.props(), self().path().name() + "-PassengerTransportRequest");

        // In questo momento tutti i passeggeri vogliono andare al nodo 0
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(location.getNode(), 0)), transportRequest);
    }

    private void selectCar(SelectCarMessage msg){
        transportRequest.tell(new TransportCoordination.SelectCarMsg(), getSelf());
    }

    private void carArrived(CarArrivedMessage msg){
        this.car = getSender();     // Riferimento alla macchina che mi sta trasportando
        // Passenger is now in transit
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
                .match(CarArrivedMessage.class, this::carArrived)
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
