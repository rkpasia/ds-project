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

public class GeneralCarFailureTest {

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
    public void CarBrokenBeforeArrived() {

        new TestKit(system) {
            {

                final ActorRef passenger = system.actorOf(Passenger.props(),"passenger");
                final ActorRef car = system.actorOf(Car.props(), "car");
                final ActorRef car2 = system.actorOf(Car.props(), "car2");


                TestKit watcher = new TestKit(system);
                watcher.watch(passenger);
                watcher.watch(car);
                watcher.watch(car2);

                within(
                        Duration.ofSeconds(20),
                        () -> {

                            //il passeggero fa una richiesta al sistema
                            passenger.tell(new Passenger.EmitRequestMessage(),null);

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            //una macchina si rompe prima della selezione
                            car.tell(PoisonPill.getInstance(),null);
                            watcher.expectMsgClass(Terminated.class);

                            passenger.tell(new Passenger.SelectCarMessage(),null);
                            // creiamo un altra macchina nel sistema per essere certi che
                            // il passeggero scelga car2 (supponiamo che non si sia prenotata all'inizio)
                            final ActorRef car3 = system.actorOf(Car.props(), "car3");

                            //una macchina si rompe dopo essere stata selezionata
                            car2.tell(PoisonPill.getInstance(),null);
                            watcher.expectMsgClass(Terminated.class);

                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                            passenger.tell(new Passenger.SelectCarMessage(),null);


                            watcher.expectNoMessage();
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }

    @Test
    public void CarBrokeAfterArrived() {

        new TestKit(system) {
            {

                final ActorRef passenger = system.actorOf(Passenger.props(),"passenger");
                final ActorRef car = system.actorOf(Car.props(), "car");
                final ActorRef car2 = system.actorOf(Car.props(), "car2");


                TestKit watcher = new TestKit(system);
                watcher.watch(passenger);
                watcher.watch(car);
                watcher.watch(car2);

                within(
                        Duration.ofSeconds(20),
                        () -> {

                            //il passeggero fa una richiesta al sistema
                            passenger.tell(new Passenger.EmitRequestMessage(),null);

                            passenger.tell(new Passenger.SelectCarMessage(),null);
                            // creiamo un altra macchina nel sistema per essere certi che
                            // il passeggero scelga car2 (supponiamo che non si sia prenotata all'inizio)
                            final ActorRef car3 = system.actorOf(Car.props(), "car3");

                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            //una macchina si rompe dopo essere stata selezionata
                            car2.tell(PoisonPill.getInstance(),null);
                            watcher.expectMsgClass(Terminated.class);

                            passenger.tell(new Passenger.SelectCarMessage(),null);

                            watcher.expectNoMessage();
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}