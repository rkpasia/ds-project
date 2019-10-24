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
import uniud.distribuiti.lastmile.passenger.TransportRequest;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;

public class TransportRequestActorTest  {

    private static ActorSystem system;

    // inizzializziamo l'actor system da testare
    @BeforeClass
    public static void setup() {

        system = ActorSystem.create();

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

    @Test
    // Verifica che la morte di una transport request
    // venga diffusa al passeggero e anche a tutti i manager coinvolti
    public void transportRequestDeathBroadcast(){
        new TestKit(system){
            {

                class CarProbe extends TestKit {
                    private CarProbe(){
                        super(system);
                        ActorRef mediator = DistributedPubSub.get(system).mediator();
                        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", this.getRef()), this.getRef());
                    }
                }

                class ManagerProbe extends TestKit {
                    private ManagerProbe(){
                        super(system);
                        ActorRef mediator = DistributedPubSub.get(system).mediator();
                        mediator.tell(new DistributedPubSubMediator.Subscribe("ABORT_REQUEST", this.getRef()), this.getRef());
                    }
                }

                ActorRef passenger = system.actorOf(Passenger.props(), "TRANSPORT_REQUEST");
                CarProbe car = new CarProbe();
                car.expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);
                ManagerProbe manager = new ManagerProbe();
                manager.expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);

                within(
                        Duration.ofSeconds(20),
                        () -> {
                            passenger.tell(new Passenger.EmitRequestMessage(), null);
                            car.expectMsgClass(Car.TransportRequestMessage.class);
                            ActorRef transportRequest = car.getLastSender();
                            transportRequest.tell(PoisonPill.getInstance(), null);
                            car.expectMsgClass(Car.TransportRequestMessage.class);
                            manager.expectMsgClass(TransportCoordination.AbortTransportRequest.class);
                            assert(manager.getLastSender().equals(transportRequest));
                            return null;
                        }
                );
            }
        };
    }
}