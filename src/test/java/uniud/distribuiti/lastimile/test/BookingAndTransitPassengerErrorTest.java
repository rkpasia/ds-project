package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.passenger.Passenger;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;

public class BookingAndTransitPassengerErrorTest {

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

                final ActorRef passenger = system.actorOf(Passenger.props(), "Passenger");

                TestKit fakeCar = new TestKit(system);
                TestKit fakeCar2 = new TestKit(system);
                TestKit fakeCar3 = new TestKit(system);

                //iscriviamo la finta macchina ad un pubsub come la vera macchina
                // ci aspettiamo che l'iscrizione vada a buon fine
                ActorRef mediator = DistributedPubSub.get(system).mediator();
                mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", fakeCar.getRef()), getRef());
                mediator.tell(new DistributedPubSubMediator.Subscribe("ABORT_REQUEST", fakeCar.getRef()), getRef());
                receiveN(2);

                within(
                        Duration.ofSeconds(20),
                        () -> {


                            passenger.tell(new Passenger.EmitRequestMessage(),null);
                            fakeCar.expectMsgClass(Car.TransportRequestMessage.class);

                            //simuliamo la morte di un tr quando il passeggero ha fatto la richiesta di booking
                            ActorRef tr = fakeCar.getLastSender();
                            fakeCar.getLastSender().tell(new TransportCoordination.CarAvailableMsg(-1),fakeCar.getRef());
                            passenger.tell(new Passenger.SelectCarMessage(),null);
                            tr.tell(PoisonPill.getInstance(),null);
                            fakeCar.expectMsgClass(TransportCoordination.AbortTransportRequest.class);


                            //simuliamo la morte del trm quando il passeggero ha fatto la richiesta di booking
                            fakeCar.expectMsgClass(Car.TransportRequestMessage.class);
                            fakeCar.getLastSender().tell(new TransportCoordination.CarAvailableMsg(-1),fakeCar.getRef());
                            passenger.tell(new Passenger.SelectCarMessage(),null);
                            //in questo caso non vengono inviati ulteriori messaggi, perché viene avvisato l'utente
                            // che deciderà se fare una nuova richiesta
                            fakeCar.send(fakeCar.getRef(),PoisonPill.getInstance());


                            mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", fakeCar2.getRef()), getRef());
                            receiveN(1);

                            // caso car Broken (si rompe la macchina in arrivo)
                            passenger.tell(new Passenger.EmitRequestMessage(),null);
                            fakeCar2.expectMsgClass(Car.TransportRequestMessage.class);

                            mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", fakeCar3.getRef()), getRef());
                            receiveN(1);

                            //simuliamo la morte di un tr quando il passeggero ha fatto la richiesta di booking
                            fakeCar2.getLastSender().tell(new TransportCoordination.CarAvailableMsg(-1),fakeCar2.getRef());
                            passenger.tell(new Passenger.SelectCarMessage(),null);
                            fakeCar2.expectMsgClass(TransportCoordination.CarBookingRequestMsg.class);
                            fakeCar2.getLastSender().tell(new TransportCoordination.CarBookingConfirmedMsg(),fakeCar2.getRef());

                            // TODO fallimento

                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            // la macchina si rompe quando sta andando dal passeggero
                            fakeCar2.send(fakeCar2.getRef(), PoisonPill.getInstance());
                            //il passeggero emette una nuova richiesta
                            //fakeCar.expectMsgClass(Car.TransportRequestMessage.class);

                            // caso car Break down (si rompe la macchina con sopra il passeggero)
                            // visto che prima si e rotta la macchina in arrivo il passeggero dovrebbe inviare subito un altra richiesta
                            /*
                            fakeCar3.expectMsgClass(Car.TransportRequestMessage.class);
                            fakeCar3.getLastSender().tell(new TransportCoordination.CarAvailableMsg(-1),fakeCar3.getRef());
                            passenger.tell(new Passenger.SelectCarMessage(),null);
                            fakeCar3.expectMsgClass(TransportCoordination.CarBookingRequestMsg.class);
                            fakeCar3.getLastSender().tell(new TransportCoordination.CarBookingConfirmedMsg(),fakeCar3.getRef());
                            passenger.tell( new TransportCoordination.CarArrivedToPassenger(),null);

                            fakeCar3.send(fakeCar3.getRef(), PoisonPill.getInstance());


                             */


                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}