package uniud.distribuiti.lastmile;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ActorSystem;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.cluster.ClusterEventsListener;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.passenger.Passenger;

import java.io.FileNotFoundException;

public class LastMile {
  public static void main(String[] args) throws FileNotFoundException {

    String[] ports = {"3000","3001"};

    // Configuration of cluster seed nodes
    for(String port : ports) {
      Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());
      ActorSystem syst = ActorSystem.create("ClusterSystem", config);
      syst.actorOf(Props.create(ClusterEventsListener.class), "ClusterEventsListener");
    }




    ActorRef passenger = setSinglePassenger();
    ActorRef car = setSingleCar();

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    passenger.tell("RICHIESTA", null);

    Location loc = new Location();

    Location.shortestPath(0,4);
    Location.printDistance();
    Location.printPath();

    /*car.tell("RICHIESTA", passenger);
    car.tell("RICHIESTA", passenger);
    car.tell("RICHIESTA", passenger);*/


  }

  private static ActorRef setSinglePassenger(){
    // Configurazione nuovo nodo con nuovo ActorSystem e attore principale Passeggero
    Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2551").withFallback(ConfigFactory.load());
    // Inizializzazione nuova gerarchia di attori - PASSEGGERO
    ActorSystem syst = ActorSystem.create("ClusterSystem", config);
    // Instanziazione di un nuovo attore Passeggero
    return syst.actorOf(Passenger.props(), "Passenger");
  }

  private static ActorRef setSingleCar(){
    // Configurazione nuovo nodo con nuovo ActorSystem e attore principale Passeggero
    Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2552").withFallback(ConfigFactory.load());
    // Inizializzazione nuova gerarchia di attori - MACCHINA
    ActorSystem syst = ActorSystem.create("ClusterSystem", config);
    // Instanziazione di un nuovo attore Passeggero
    return syst.actorOf(Car.props(), "Car");
  }
}
