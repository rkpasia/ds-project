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

    String[] ports = {"3000", "3001"};

    // Configuration of cluster seed nodes
    for (String port : ports) {
      Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());
      ActorSystem syst = ActorSystem.create("ClusterSystem", config);
      syst.actorOf(Props.create(ClusterEventsListener.class), "ClusterEventsListener");
    }


    ActorRef passenger1 = setSinglePassenger("PASSEGGERO1");
    ActorRef passenger2 = setSinglePassenger("PASSEGGERO2");
    ActorRef passenger3 = setSinglePassenger("PASSEGGERO3");
    ActorRef car1 = setSingleCar("MACCHINA");
    ActorRef car2 = setSingleCar("MACCHINA");

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // TODO: Mettere uno sleep tra una emissione e l'altra
    passenger1.tell(new Passenger.EmitRequestMessage(), null);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    passenger2.tell(new Passenger.EmitRequestMessage(), null);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    passenger3.tell(new Passenger.EmitRequestMessage(), null);


    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    passenger1.tell(new Passenger.SelectCarMessage(), null);


  }

    private static ActorRef setSinglePassenger (String name){
      // Configurazione nuovo nodo con nuovo ActorSystem e attore principale Passeggero
      Config config = ConfigFactory.load();
      // Inizializzazione nuova gerarchia di attori - PASSEGGERO
      ActorSystem syst = ActorSystem.create("ClusterSystem", config);
      // Instanziazione di un nuovo attore Passeggero
      return syst.actorOf(Passenger.props(), name);
    }
    private static ActorRef setSingleCar (String name){
      // Configurazione nuovo nodo con nuovo ActorSystem e attore principale Passeggero
      Config config = ConfigFactory.load();
      // Inizializzazione nuova gerarchia di attori - MACCHINA
      ActorSystem syst = ActorSystem.create("ClusterSystem", config);
      // Instanziazione di un nuovo attore Passeggero
      return syst.actorOf(Car.props(), name);
    }
  }
