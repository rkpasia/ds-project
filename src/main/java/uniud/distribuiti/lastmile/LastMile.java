package uniud.distribuiti.lastmile;

import java.io.IOException;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ActorSystem;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.cluster.ClusterEventsListener;
import uniud.distribuiti.lastmile.passenger.Passenger;

public class LastMile {
  public static void main(String[] args) {

    String[] ports = {"2551", "2552", "0"};

    // Configuration of cluster seed nodes
    for(String port : ports) {
      Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());
      ActorSystem syst = ActorSystem.create("ClusterSystem", config);
      syst.actorOf(Props.create(ClusterEventsListener.class), "ClusterEventsListener");
    }




    ActorRef passenger = setSinglePassenger();
    ActorRef car = setSingleCar();


    car.tell("RICHIESTA", passenger);
    car.tell("RICHIESTA", passenger);
    car.tell("RICHIESTA", passenger);


  }

  private static ActorRef setSinglePassenger(){
    // Configurazione nuovo nodo con nuovo ActorSystem e attore principale Passeggero
    Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2600").withFallback(ConfigFactory.load());
    // Inizializzazione nuova gerarchia di attori - PASSEGGERO
    ActorSystem syst = ActorSystem.create("ClusterSystem", config);
    // Instanziazione di un nuovo attore Passeggero
    return syst.actorOf(Passenger.props(), "Passenger");
  }

  private static ActorRef setSingleCar(){
    // Configurazione nuovo nodo con nuovo ActorSystem e attore principale Passeggero
    Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2601").withFallback(ConfigFactory.load());
    // Inizializzazione nuova gerarchia di attori - MACCHINA
    ActorSystem syst = ActorSystem.create("ClusterSystem", config);
    // Instanziazione di un nuovo attore Passeggero
    return syst.actorOf(Car.props(), "Car");
  }
}
