package uniud.distribuiti.lastmile;

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

    passenger.tell(new Passenger.EmitRequestMessage(), null);
    
  }

  private static ActorRef setSinglePassenger(){
  private static ActorRef setSinglePassenger(String name){
    // Configurazione nuovo nodo con nuovo ActorSystem e attore principale Passeggero
    Config config = ConfigFactory.load();
    // Inizializzazione nuova gerarchia di attori - PASSEGGERO
    ActorSystem syst = ActorSystem.create("ClusterSystem", config);
    // Instanziazione di un nuovo attore Passeggero
    return syst.actorOf(Passenger.props(), "Passenger");
    return syst.actorOf(Passenger.props(), name);
  }

  private static ActorRef setSingleCar(){
  private static ActorRef setSingleCar(String name){
    // Configurazione nuovo nodo con nuovo ActorSystem e attore principale Passeggero
    Config config = ConfigFactory.load();
    // Inizializzazione nuova gerarchia di attori - MACCHINA
    ActorSystem syst = ActorSystem.create("ClusterSystem", config);
    // Instanziazione di un nuovo attore Passeggero
    return syst.actorOf(Car.props(), "Car");
    return syst.actorOf(Car.props(), name);
  }
}
