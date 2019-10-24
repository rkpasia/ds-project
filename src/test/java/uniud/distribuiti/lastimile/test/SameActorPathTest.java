package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uniud.distribuiti.lastmile.passenger.Passenger;

import java.time.Duration;

public class SameActorPathTest  {

    private static ActorSystem system;

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

                final ActorRef passenger = system.actorOf(Passenger.props(), "Passenger");
                //final ActorRef car = system.actorOf(Car.props(), "car");
                TestKit watcher = new TestKit(system);


                within(
                        Duration.ofSeconds(10),
                        () -> {


                            watcher.watch(passenger);
                            passenger.tell(PoisonPill.getInstance(),null);
                            watcher.expectMsgClass(Terminated.class);
                            system.actorOf(Passenger.props(), "Passenger");

                            //ok una volta terminato me lo fa ricreare
                            /* // versione che fallisce

                            watcher.watch(passenger);
                            passenger.tell(PoisonPill.getInstance(),null);
                            system.actorOf(Passenger.props(), "Passenger");

                            // perché devo aspettare che passenger muoia

                             //secondo test
                            watcher.watch(car);
                            system.stop(car);
                            system.actorOf(Car.props(), "car");

                            // anche con stop fallisce perché devo aspettare che si stoppi
                             */




                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}