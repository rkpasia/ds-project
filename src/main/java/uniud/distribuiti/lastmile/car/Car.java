package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.Route;
import uniud.distribuiti.lastmile.location.LocationHelper;

import java.io.Serializable;

public class Car extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Car.class, () -> new Car());
    }

    // TODO: Verificare se ci sono altri serializzatori migliori
    public static class TransportRequestMessage implements Serializable {
        private final int destination;
        private final int passengerLocation;

        public TransportRequestMessage(int passengerLocation, int dest){
            this.passengerLocation = passengerLocation;
            this.destination = dest;
        }

        public int getDestination(){
            return this.destination;
        }

        public int getPassengerLocation(){
            return this.passengerLocation;
        }
    }

    private CarStatus status;
    private enum CarStatus {
        AVAILABLE,
        MATCHED,
        TRANSIT
    }

    private Location location;
    private Double fuel; // Carburante in litri
    private final Double kmPerLiter = 14.0;
    private Route route;

    public Car(){
        this.status = CarStatus.AVAILABLE;
        ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", getSelf()), getSelf());

        // Inizializzazione standard carburante macchina
        this.fuel = 20.0;
        LocationHelper locationHelper = new LocationHelper();
        this.location = locationHelper.assignLocation();
    }

    private void evaluateRequest(TransportRequestMessage msg){
        System.out.println("VALUTAZIONE " + msg.toString());
        this.route = LocationHelper.defineRoute(this.location.getNode(), msg.getPassengerLocation(), msg.getDestination());
        if(haveEnoughFuel(this.route.distance)){
            System.out.println("CARBURANTE SUFFICIENTE - INVIO PROPOSTA");
            getContext().actorOf(TransportRequestMngr.props(getSender()), getSender().path().name() + "CarTransportRequestManager");
        }
    }

    private boolean haveEnoughFuel(int km){
        double fuelConsumption = km / this.kmPerLiter;
        double elapsedFuel = this.fuel - fuelConsumption;
        return (elapsedFuel) < 0 ? false : true;
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
