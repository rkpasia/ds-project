package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Kill;
import akka.actor.PoisonPill;
import akka.cluster.Cluster;
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

public class BookingAndTransitErrorTest  {

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
                final ActorRef car = system.actorOf(Car.props(), "car");
                final ActorRef car2 = system.actorOf(Car.props(), "car2");

                TestKit fakePassenger = new TestKit(system);
                TestKit fakeCar = new TestKit(system);

                //iscriviamo la finta macchina ad un pubsub come la vera macchina
                // ci aspettiamo che l'iscrizione vada a buon fine
                ActorRef mediator = DistributedPubSub.get(system).mediator();
                mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", fakeCar.getRef()), getRef());
                expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);



                Cluster cluster = Cluster.get(system);

                within(
                        Duration.ofSeconds(1),
                        () -> {

                            fakePassenger.send(car,new Car.TransportRequestMessage(0,1));
                            fakePassenger.expectMsgClass(TransportCoordination.class);

                            ActorRef carTrm = fakePassenger.getLastSender();

                            //facciamo un test sul transit manager
                            //fallisce se la macchina nasce sul nodo 0 (perché manda i messaggi in contemporanea)
                            fakePassenger.send(carTrm,new TransportCoordination.CarBookingRequestMsg());
                            fakePassenger.expectMsgClass(TransportCoordination.CarBookingConfirmedMsg.class);
                            fakePassenger.expectMsgClass(Duration.ofSeconds(20),TransportCoordination.CarArrivedToPassenger.class);
                            fakePassenger.expectMsgClass(Duration.ofSeconds(20),TransportCoordination.UpdateLocation.class);
                            fakePassenger.expectMsgClass(Duration.ofSeconds(20),TransportCoordination.UpdateLocation.class);
                            fakePassenger.expectMsgClass(Duration.ofSeconds(20),TransportCoordination.DestinationReached.class);

                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            //ora introduciamo gli errori
                            fakePassenger.send(car,new Car.TransportRequestMessage(0,1));
                            fakePassenger.expectMsgClass(TransportCoordination.class);
                            ActorRef carTrm2 = fakePassenger.getLastSender();
                            fakePassenger.send(carTrm2,new TransportCoordination.CarBookingRequestMsg());
                            carTrm2.tell(PoisonPill.getInstance(),null);
                            fakePassenger.expectMsgClass(TransportCoordination.CarBookingConfirmedMsg.class);
                            //la macchina dyovrebbe comunque dare la conferma al passeggero
                            if(fakePassenger.getLastSender() != car)
                                throw new AssertionError("il trm è morto la macchina dovrebbe avvisare il passeggero");
                            fakePassenger.expectMsgClass(Duration.ofSeconds(20),TransportCoordination.CarArrivedToPassenger.class);
                            // ci aspettiamo anche un update location perché quando manda il mess carArrived manda anche un UpdateLocation
                            fakePassenger.expectMsgClass(TransportCoordination.UpdateLocation.class);

                            //uccidiamo il transit manager
                            fakePassenger.getLastSender().tell(Kill.getInstance(),null);
                            fakePassenger.expectMsgClass(Duration.ofSeconds(20),Car.BrokenLocation.class);

                            fakePassenger.send(car2,new Car.TransportRequestMessage(0,1));
                            fakePassenger.expectMsgClass(TransportCoordination.class);
                            ActorRef carTrm3 = fakePassenger.getLastSender();
                            fakePassenger.send(carTrm3,new TransportCoordination.CarBookingRequestMsg());
                            fakePassenger.expectMsgClass(TransportCoordination.CarBookingConfirmedMsg.class);
                            //uccido il transit request manager della macchina
                            system.actorSelection("akka://default/user/car2/TRANSIT_MANAGER").tell(PoisonPill.getInstance(),null);
                            fakePassenger.expectMsgClass(Car.CarBreakDown.class);

                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}