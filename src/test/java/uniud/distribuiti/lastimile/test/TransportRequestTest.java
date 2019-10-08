package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.collection.immutable.Seq;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.passenger.Passenger;

import static junit.framework.TestCase.assertEquals;

public class TransportRequestTest {

    static ActorSystem system;

    private static TestProbe c;

    @BeforeClass
    public static void setup(){

        Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=3000").withFallback(ConfigFactory.load());
        system = ActorSystem.create("ClusterSystem", config);

        ActorRef mediator = DistributedPubSub.get(system).mediator();
        c = new TestProbe(system, "car");
        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", c.ref()), c.ref());

    }

    @AfterClass
    public static void teardown(){
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void transportRequestEmission(){

        new TestKit(system){
            {

                final Props createP = Props.create(Passenger.class);
                final ActorRef passenger = system.actorOf(createP, "Passenger");
                passenger.tell(new Passenger.EmitRequestMessage(), getRef());

                Seq ret = c.receiveN(2);
                System.out.println(ret.toList().toString());
                assertEquals(ret.last().getClass(), Car.TransportRequestMessage.class);

            }
        };

    }

}
