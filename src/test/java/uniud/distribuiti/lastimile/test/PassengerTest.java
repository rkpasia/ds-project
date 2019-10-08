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

import java.time.Duration;

public class PassengerTest  {

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
                final ActorRef subject1 = system.actorOf(Passenger.props(), "Passenger1");
                final ActorRef subject2 = system.actorOf(Passenger.props(), "Passenger2");

                ActorRef mediator = DistributedPubSub.get(system).mediator();
                mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", getRef()), getRef());
                expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            subject1.tell(new Passenger.EmitRequestMessage(),getRef());
                            subject2.tell(new Passenger.EmitRequestMessage(),getRef());

                            expectMsgClass(Car.TransportRequestMessage.class);
                            expectMsgClass(Car.TransportRequestMessage.class);

                            subject1.tell(new Passenger.SelectCarMessage(),getRef());
                            subject2.tell(new Passenger.SelectCarMessage(),getRef());

                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}