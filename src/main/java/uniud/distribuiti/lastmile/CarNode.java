package uniud.distribuiti.lastmile;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import uniud.distribuiti.lastmile.car.Car;

public class CarNode {

    public CarNode() {

        Config config = ConfigFactory.load();

        String clusterSystemName = "ClusterSystem";
        ActorSystem system = ActorSystem.create(clusterSystemName, config);
        AkkaManagement.get(system).start();
        ClusterBootstrap.get(system).start();

        // NODE MAIN ACTOR
        ActorRef car = system.actorOf(Car.props());
    }
}
