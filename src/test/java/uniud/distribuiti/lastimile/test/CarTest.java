package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;

public class CarTest  {

    static ActorSystem system;

    // inizzializziamo l'actor system da testare
    @BeforeClass
    public static void setup() {

        system =ActorSystem.create();

    }

    //finiti i test spegnamo tutto
    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testIt() {

        new TestKit(system) {
            {
                //creiamo l'attore o gli attori che vogliamo testare
                final ActorRef subject = system.actorOf(Car.props(), "car1");
                final ActorRef subject1 = system.actorOf(Car.props(), "car2");

                ActorRef mediator = DistributedPubSub.get(system).mediator();



                within(
                        Duration.ofSeconds(10),
                        () -> {
                            //qui è possibile interagire con gli attori e vedere cosa ci si aspetta rispondano

                            subject.tell(new Car.TransportRequestMessage(0,0), getRef());
                            subject1.tell(new Car.TransportRequestMessage(0,0), getRef());

                            expectMsgClass(TransportCoordination.CarAvailableMsg.class);
                            expectMsgClass(TransportCoordination.CarAvailableMsg.class);

                            getLastSender().tell(new TransportCoordination.CarBookingRequestMsg(), getRef());
                            getLastSender().tell(new TransportCoordination.CarBookingRequestMsg(), getRef());

                            expectMsgClass(TransportCoordination.CarBookingConfirmedMsg.class);
                            expectMsgClass(TransportCoordination.CarBookingRejectMsg.class);

                            // non mi aspetto nessuna risposta perché ho gia mandato una richiesta a tutte le macchine
                            // pertanto avro gia un transport request manager istanziato per oni macchina
                            mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(0, 0)), getRef());
                            //expectMsgClass(TransportCoordination.CarAvailableMsg.class);

                            getLastSender().tell(new TransportCoordination.CarBookingRequestMsg(), getRef());
                            getLastSender().tell(new TransportCoordination.CarBookingRequestMsg(), getRef());

                            expectMsgClass(TransportCoordination.CarBookingRejectMsg.class);
                            expectMsgClass(TransportCoordination.CarBookingRejectMsg.class);

                            return null;
                        });
            }
        };
    }
}