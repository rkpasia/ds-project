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
import uniud.distribuiti.lastmile.passenger.Passenger;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;

public class BookingAndTransitTest  {

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
                final ActorRef car = system.actorOf(Car.props(), "car");

                TestKit fakePassenger = new TestKit(system);
                TestKit fakeCar = new TestKit(system);

                //iscriviamo la finta macchina ad un pubsub come la vera macchina
                // ci aspettiamo che l'iscrizione vada a buon fine
                ActorRef mediator = DistributedPubSub.get(system).mediator();
                mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", fakeCar.getRef()), getRef());
                expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);

                within(
                        Duration.ofSeconds(20),
                        () -> {
                            //facciamo mandare dal passeggero una richiesta di trasporto
                            passenger.tell(new Passenger.EmitRequestMessage(),null);
                            //ci aspettiamo che la finta macchina riceva la richiesta
                            fakeCar.expectMsgClass(Car.TransportRequestMessage.class);

                            // prendo la ref del tr del passeggero
                            ActorRef tr = fakeCar.getLastSender();

                            // la macchina finta si propone con un offerta davvero conveniente
                            fakeCar.send(tr,new TransportCoordination.CarAvailableMsg(-1));

                            // vista la proposta dovrebbe scegliere la macchina finta
                            passenger.tell(new Passenger.SelectCarMessage(),null);
                            fakeCar.expectMsgClass(TransportCoordination.CarBookingRequestMsg.class);

                            //la macchina finta risponde che non può
                            fakeCar.send(tr,new TransportCoordination.CarBookingRejectMsg());

                            // proviamo a chiedere la stessa macchina con due passeggeri
                            fakePassenger.send(car, new Car.TransportRequestMessage(0,0));
                            fakePassenger.expectMsgClass(TransportCoordination.CarAvailableMsg.class);

                            ActorRef trm = fakePassenger.getLastSender();

                            //il passeggero prova a scegliere un altra macchina (dovrebbe scegliere quella vera)
                            passenger.tell(new Passenger.SelectCarMessage(),null);

                            //aspetto che il passeggero prenoti la macchina
                            fakePassenger.expectMsgClass(TransportCoordination.CarUnavailableMsg.class);

                            //provo a riprenotarla ma non ricevero mai risposta perché il trm è morto con carUnavaiablemsg
                            fakePassenger.send(trm,new TransportCoordination.CarBookingRequestMsg());



                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}