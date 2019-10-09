package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uniud.distribuiti.lastmile.car.TransportRequestMngr;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.Route;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;
import java.util.ArrayList;

public class TransportRequestManagerTest  {

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
                TestKit parent = new TestKit(system);
                final ActorRef transportRequestManager = parent.childActorOf(TransportRequestMngr.props
                        (getRef(),new Route(0,new ArrayList<Integer>()),new Location(0)), "TransportRequestManager");




                final TestKit watcher = new TestKit(system);
                watcher.watch(transportRequestManager);


                within(
                        Duration.ofSeconds(10),
                        () -> {

                            // alla creazione del trm ci rispondera con CarAvaiable perché ci vede come tr
                            expectMsgClass(TransportCoordination.CarAvailableMsg.class);

                            // ci aspettiamo che il trm ricevuta una richiesta di booking la inotlri a suo padre
                            // pertanto creiamo un padre fittizzio e vediamo se riceve la richiesta
                            transportRequestManager.tell(new TransportCoordination.CarBookingRequestMsg(),getRef());
                            parent.expectMsgClass(TransportCoordination.CarBookingRequestMsg.class);

                            // il trm ci vede come tr quindi ci inoltra la conferma
                            transportRequestManager.tell(new TransportCoordination.CarBookingConfirmedMsg(),getRef());
                            expectMsgClass(TransportCoordination.CarBookingConfirmedMsg.class);
                            transportRequestManager.tell(new TransportCoordination.CarBookingRejectMsg(),getRef());
                            expectMsgClass(TransportCoordination.CarBookingRejectMsg.class);

                            // qui il trm dovrebbe spegnersi quindi ci aspettiamo la risposta e la sua dipartita
                            transportRequestManager.tell(new TransportCoordination.CarHasBeenBooked(),getRef());
                            expectMsgClass(TransportCoordination.CarUnavailableMsg.class);
                            watcher.expectMsgClass(Terminated.class);


                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}