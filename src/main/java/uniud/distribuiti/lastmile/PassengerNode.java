package uniud.distribuiti.lastmile;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import uniud.distribuiti.lastmile.passenger.Passenger;

public class PassengerNode {

    private ActorRef passenger;

    public PassengerNode() {
        Config config = ConfigFactory.load();

        String clusterSystemName = "ClusterSystem";
        ActorSystem system = ActorSystem.create(clusterSystemName, config);
        AkkaManagement.get(system).start();
        ClusterBootstrap.get(system).start();

        // NODE MAIN ACTOR
        this.passenger = system.actorOf(Passenger.props());
    }

    public void sendRequest(){
        this.passenger.tell(new Passenger.EmitRequestMessage(), null);
    }

    public void selectCar() { this.passenger.tell(new Passenger.SelectCarMessage(), null); }
}
