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

public class TransportRequestOperationTest  {

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
                system.actorOf(Car.props(), "car");

                TestKit fakePassenger = new TestKit(system);
                TestKit fakeCar = new TestKit(system);

                //iscriviamo la finta macchina ad un pubsub come la vera macchina
                // ci aspettiamo che l'iscrizione vada a buon fine
                ActorRef mediator = DistributedPubSub.get(system).mediator();
                mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", fakeCar.getRef()), getRef());
                expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);

                within(
                        Duration.ofSeconds(10),
                        () -> {
                            //dal finto passeggero mandiamo una richiesta di trasporto
                            fakePassenger.send(mediator,
                                    new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(0, 0)));

                            // ci aspettiamo che la finta macchina riceva il messaggio
                            fakeCar.expectMsgClass(Car.TransportRequestMessage.class);
                            // ci aspettiamo che la macchina risponda al finto passeggero
                            fakePassenger.expectMsgClass(TransportCoordination.CarAvailableMsg.class);

                            //facciamo mandare dal passeggero una vera richiesta di trasporto
                            passenger.tell(new Passenger.EmitRequestMessage(),null);
                            //ci aspettiamo che la finta macchina riceva la richiesta
                            fakeCar.expectMsgClass(Car.TransportRequestMessage.class);

                            // la finta macchina si propone al passeggero
                            fakeCar.send(passenger, new TransportCoordination.CarAvailableMsg(0));

                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}