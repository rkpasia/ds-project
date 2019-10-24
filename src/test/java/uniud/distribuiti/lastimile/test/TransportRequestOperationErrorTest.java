package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Kill;
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

public class TransportRequestOperationErrorTest  {

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
                        Duration.ofSeconds(10),
                        () -> {


                            //mandiamo una finta richiesta alla macchina
                            fakePassenger.send(car,new Car.TransportRequestMessage(0,0));
                            fakePassenger.expectMsgClass(TransportCoordination.class);

                            //supponiamo che il trm della macchina si rompa
                            fakePassenger.getLastSender().tell(PoisonPill.getInstance(),null);

                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //devo poter fare un'altra richiesta e la macchina deve creare un nuovo trm
                            fakePassenger.send(car,new Car.TransportRequestMessage(0,0));
                            fakePassenger.expectMsgClass(TransportCoordination.class);

                            //valutiamo ora il caso in cui fallisca il tr del passeggero
                            passenger.tell(new Passenger.EmitRequestMessage(), null);
                            fakeCar.expectMsgClass(Car.TransportRequestMessage.class);
                            fakeCar.getLastSender().tell(PoisonPill.getInstance(),null);

                            // come ci aspettiamo viene creato un nuovo tr e la richiesta viene reinoltrata alle macchine
                            fakeCar.expectMsgClass(Car.TransportRequestMessage.class);
                            ActorRef tr = fakeCar.getLastSender();
                            tr.tell(new TransportCoordination.CarAvailableMsg(-1),fakeCar.getRef());

                            //simuliamo la morte del trm  per il tr
                            fakePassenger.send(fakeCar.getRef(), Kill.getInstance());


                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}