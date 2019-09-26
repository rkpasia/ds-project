package uniud.distribuiti.lastmile;

import java.io.IOException;

import akka.actor.Props;
import akka.actor.ActorSystem;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import uniud.distribuiti.lastmile.cluster.ClusterEventsListener;

public class LastMile {
  public static void main(String[] args) {

    String[] ports = {"2551", "2552", "0"};

    // Configuration of cluster seed nodes
    for(String port : ports) {
      Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());
      ActorSystem syst = ActorSystem.create("ClusterSystem", config);
      syst.actorOf(Props.create(ClusterEventsListener.class), "ClusterEventsListener");
    }

  }
}
