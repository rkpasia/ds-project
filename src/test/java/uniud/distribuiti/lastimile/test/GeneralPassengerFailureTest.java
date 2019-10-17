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

import java.time.Duration;

public class GeneralPassengerFailureTest {

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
    public void PassengerBrokenBeforCarArrived() {

        new TestKit(system) {
            {

                final ActorRef passenger = system.actorOf(Passenger.props(),"passenger");
                final ActorRef passenger2 = system.actorOf(Passenger.props(),"passenger2");
                final ActorRef car = system.actorOf(Car.props(), "car");


                TestKit watcher = new TestKit(system);
                watcher.watch(passenger);
                watcher.watch(passenger2);

                within(
                        Duration.ofSeconds(20),
                        () -> {

                            //il passeggero fa una richiesta al sistema
                            passenger.tell(new Passenger.EmitRequestMessage(),null);
                            passenger2.tell(new Passenger.EmitRequestMessage(),null);

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            passenger.tell(PoisonPill.getInstance(),null);
                            watcher.expectMsgClass(Terminated.class);

                            passenger2.tell(new Passenger.SelectCarMessage(),null);

                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            passenger2.tell(PoisonPill.getInstance(),null);
                            watcher.expectMsgClass(Terminated.class);

                            watcher.expectNoMessage();
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }

    @Test
    public void PassengerBrokenAfterCarArrived() {

        new TestKit(system) {
            {
                final ActorRef passenger = system.actorOf(Passenger.props(), "passenger");
                final ActorRef passenger2 = system.actorOf(Passenger.props(), "passenger2");
                final ActorRef car = system.actorOf(Car.props(), "car");


                TestKit watcher = new TestKit(system);
                watcher.watch(passenger);
                watcher.watch(passenger2);

                within(
                        Duration.ofSeconds(20),
                        () -> {

                            //il passeggero fa una richiesta al sistema
                            passenger.tell(new Passenger.EmitRequestMessage(), null);
                            passenger2.tell(new Passenger.EmitRequestMessage(), null);

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            passenger.tell(PoisonPill.getInstance(), null);
                            watcher.expectMsgClass(Terminated.class);

                            passenger2.tell(new Passenger.SelectCarMessage(), null);

                            try {
                                Thread.sleep(12000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            passenger2.tell(PoisonPill.getInstance(), null);
                            watcher.expectMsgClass(Terminated.class);

                            watcher.expectNoMessage();
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}