package uniud.distribuiti.lastmile;

import akka.actor.Props;
import akka.actor.ActorSystem;

import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import uniud.distribuiti.lastmile.cluster.ClusterEventsListener;

public class LastMile {
  public static void main(String[] args) {

    Config config = ConfigFactory.load();
    ActorSystem clusterSystem = ActorSystem.create("ClusterSystem", config);
    AkkaManagement.get(clusterSystem).start();
    ClusterBootstrap.get(clusterSystem).start();
    clusterSystem.actorOf(Props.create(ClusterEventsListener.class), "ClusterEventsListener");

  }
}
