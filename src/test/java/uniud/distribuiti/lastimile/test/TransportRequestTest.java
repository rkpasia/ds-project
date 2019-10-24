package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.passenger.Passenger;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;

public class TransportRequestTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup(){

        system =ActorSystem.create();
    }

    @AfterClass
    public static void teardown(){
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void transportRequestEmission(){

        new TestKit(system){
            {

                final ActorRef car = system.actorOf(Car.props(), "car");
                final ActorRef passenger = system.actorOf(Passenger.props(), "passenger");

                final TestKit probe = new TestKit(system);
                probe.watch(passenger);
                within(
                        Duration.ofSeconds(10),
                        () -> {
                            passenger.tell(PoisonPill.getInstance(), ActorRef.noSender());
                            probe.expectMsgClass(Terminated.class);

                            probe.send(car,new Car.TransportRequestMessage(0,0));
                            probe.expectMsgClass(TransportCoordination.CarAvailableMsg.class);
                            expectNoMessage();
                            return null;
                        });


            }
        };

    }

}
