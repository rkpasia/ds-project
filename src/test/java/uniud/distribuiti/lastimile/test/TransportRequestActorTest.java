package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uniud.distribuiti.lastmile.passenger.TransportRequest;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;

public class TransportRequestActorTest  {

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
                final ActorRef transportRequest = parent.childActorOf(TransportRequest.props(), "TransportRequestManager");

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            transportRequest.tell(new TransportCoordination.CarAvailableMsg(0),getRef());

                            // dato che ci siamo proposti (solo noi) ci aspettiamo che ci scelga
                            transportRequest.tell(new TransportCoordination.SelectCarMsg(),getRef());
                            expectMsgClass(TransportCoordination.CarBookingRequestMsg.class);

                            transportRequest.tell(new TransportCoordination.CarBookingConfirmedMsg(),getRef());
                            transportRequest.tell(new TransportCoordination.CarBookingRejectMsg(),getRef());

                            //mi ripropongo (mi ero tolto precedentemente) e poi dico che non sono disponibile, quindi
                            // se faccio un'altra transport coordination non dovrei ricevere messaggi
                            transportRequest.tell(new TransportCoordination.CarAvailableMsg(0),getRef());
                            transportRequest.tell(new TransportCoordination.CarUnavailableMsg(),getRef());
                            transportRequest.tell(new TransportCoordination.SelectCarMsg(),getRef());


                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}