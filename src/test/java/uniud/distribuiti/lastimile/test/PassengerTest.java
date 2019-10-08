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

                            // chiedo a due passeggeri di emettere una richiesta di passaggio
                            subject1.tell(new Passenger.EmitRequestMessage(),getRef());
                            subject2.tell(new Passenger.EmitRequestMessage(),getRef());

                            // mi aspetto di ricevere la richiesta
                            expectMsgClass(Car.TransportRequestMessage.class);
                            expectMsgClass(Car.TransportRequestMessage.class);

                            // chiedo di prenotare un auto ma
                            // non mi aspetto risposte perché non ci sono auto nel sistema
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