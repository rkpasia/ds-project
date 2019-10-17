package uniud.distribuiti.lastimile.test;

import akka.actor.*;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.passenger.Passenger;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;
import java.util.ArrayList;

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
                final ActorRef subject = system.actorOf(Passenger.props(), "Passenger");

                ActorRef mediator = DistributedPubSub.get(system).mediator();
                mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", getRef()), getRef());
                expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);

                within(
                        Duration.ofSeconds(10),
                        () -> {

                            // chiedo a due passeggeri di emettere una richiesta di passaggio
                            subject.tell(new Passenger.EmitRequestMessage(),getRef());

                            // mi aspetto di ricevere la richiesta
                            expectMsgClass(Car.TransportRequestMessage.class);

                            // chiedo di prenotare un auto ma
                            // non mi aspetto risposte perché non ci sono auto nel sistema
                            subject.tell(new Passenger.SelectCarMessage(),getRef());

                            //non mi aspetto risposta
                            subject.tell(new TransportCoordination.CarArrivedToPassenger(),getRef());
                            subject.tell(new TransportCoordination.DestinationReached(new Location(0)),getRef());
                            // richiedo una nuova macchina
                            subject.tell(new Car.BrokenLocation(new Location(0)),getRef());
                            expectMsgClass(Car.TransportRequestMessage.class);


                            //aspettiamo sempre perché il shutdown del sistema potrebbe arrivare prima di alcuni messaggi che ci aspettiamo
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }

    @Test
    // Verifica deathWatch transport request
    public void transportRequestTermination(){

        new TestKit(system){
            {

                // Probe macchina per test
                class CarProbe extends TestKit {
                    public CarProbe(){
                        super(system);
                        ActorRef mediator = DistributedPubSub.get(system).mediator();
                        mediator.tell(new DistributedPubSubMediator.Subscribe("REQUEST", this.getRef()), this.getRef());
                    }
                }

                // Teseting passeggero
                ActorRef passenger = system.actorOf(Passenger.props(), "PASSEGGERO");
                // Macchina test di riferimento
                CarProbe car = new CarProbe();
                // Messaggio di iscrizione al canale PubSub
                car.expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);

                // Passeggero emette nuova richiesta
                passenger.tell(new Passenger.EmitRequestMessage(), null);
                // Macchina risponde alla richiesta con disponibilità
                car.expectMsgClass(Car.TransportRequestMessage.class);
                // Ottengo riferimento alla TransportRequest
                ActorRef transportRequest = car.getLastSender();

                // Manager test di riferimento
                TestKit manager = new TestKit(system);
                ActorRef mediator = DistributedPubSub.get(system).mediator();
                // Iscrizione del manager al canale PubSub per ricevere aborti di richieste di trasporto
                mediator.tell(new DistributedPubSubMediator.Subscribe("ABORT_REQUEST", manager.getRef()), manager.getRef());
                manager.expectMsgClass(DistributedPubSubMediator.SubscribeAck.class);

                // Avviso della disponibilità del manager la TransportRequest
                transportRequest.tell(new TransportCoordination.CarAvailableMsg(10), manager.getRef());
                // Terminazione TransportRequest
                transportRequest.tell(PoisonPill.getInstance(), getRef());

                // Manager deve ricevere il messaggio che comunica la terminazione della transport request
                manager.expectMsgClass(TransportCoordination.AbortTransportRequest.class);

                // Mi aspetto che la macchina riceva una nuova richiesta emessa dal passeggero
                // perché è terminato il TransportRequest
                car.expectMsgClass(Car.TransportRequestMessage.class);
                // Ottengo il nuovo TransportRequest
                ActorRef transportRequest2 = car.getLastSender();
                // Mi assicuro che sia diverso dal precedente
                assert(!transportRequest2.equals(transportRequest));

                // Passeggero seleziona la macchina da prenotare
                passenger.tell(new Passenger.SelectCarMessage(), getRef());
                // Terminazione della TransportRequest che sta prenotando
                transportRequest2.tell(PoisonPill.getInstance(), null);

                // La macchina si aspetta di ricevere una nuova transportRequest dal passeggero,
                // che ha iniziato un flusso completamente nuovo
                car.expectMsgClass(Car.TransportRequestMessage.class);
                // Assicuro che la nuova TR sia diversa dalla precedente
                assert(!car.getLastSender().equals(transportRequest2));
            }
        };
    }
}