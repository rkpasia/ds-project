package uniud.distribuiti.lastimile.test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.LocationHelper;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;

public class CarTest  {

    static ActorSystem system;

    // inizzializziamo l'actor system da testare
    @BeforeClass
    public static void setup() {

        system = ActorSystem.create();

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
                //creiamo l'attore o gli attori che vogliamo testare
                final ActorRef subject = system.actorOf(Car.props(), "car1");

                ActorRef mediator = DistributedPubSub.get(system).mediator();



                within(
                        Duration.ofSeconds(15),
                        () -> {
                            //qui è possibile interagire con gli attori e vedere cosa ci si aspetta rispondano

                            // faccio una richiesta di trasporto singola
                            subject.tell(new Car.TransportRequestMessage(0,0), getRef());

                            // mi aspetto che sia disponibile
                            expectMsgClass(TransportCoordination.CarAvailableMsg.class);

                            // provo a prenotare la stessa
                            // prendo last perché voglio parlare con il transport request manager
                            getLastSender().tell(new TransportCoordination.CarBookingRequestMsg(), getRef());

                            // mi aspetto che mi dia una conferma
                            expectMsgClass(TransportCoordination.CarBookingConfirmedMsg.class);

                            // non mi aspetto nessuna risposta perché ho gia mandato una richiesta a tutte le macchine
                            // pertanto avro gia un transport request manager istanziato per ogni macchina
                            mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(0, 0)), getRef());

                            // riprovo a prenotare la macchina
                            getLastSender().tell(new TransportCoordination.CarBookingRequestMsg(), getRef());

                            //mi aspetto un rifiuto
                            expectMsgClass(TransportCoordination.CarBookingRejectMsg.class);

                            // finiamo di testare i messaggi della macchina qui non ci aspettiamo nessuna risposta in quanto
                            // stiamo testando solo il singolo attore macchina
                            subject.tell(new TransportCoordination.DestinationReached(new Location(0)),getRef());

                            // qui ci aspettiamo un messaggio perché in precedenza abbiamo effettuato una richiesta di booking
                            subject.tell(new Car.CarBreakDown(),getRef());
                            expectMsgClass(Car.BrokenLocation.class);

                            subject.tell(new Car.BrokenLocation(new Location(0)),getRef());

                            expectNoMessage();
                            return null;
                        });
            }
        };
    }


    @Test
    // Test corretto funzionamento supervisione terminazione TransitManager
    public void transitManagerTerminationTest(){

        new TestKit(system){
            {

                final ActorRef car = system.actorOf(Car.props(), "MACCHINA");
                TestKit transportRequest = new TestKit(system);
                // Nuova richiesta per la macchina
                car.tell(new Car.TransportRequestMessage(2, 0), transportRequest.getRef());
                // Mi aspetto che il TransitManager riceva la disponibilità della macchina
                transportRequest.expectMsgClass(TransportCoordination.CarAvailableMsg.class);

                // Riferimento passeggero
                TestKit passenger = new TestKit(system);
                // Prenoto la macchina, e suppongo di iniziare il trasporto
                car.tell(new TransportCoordination.CarBookingRequestMsg(
                        passenger.getRef(),
                        LocationHelper.defineRoute(3, 2, 0), new Location(3)),
                        transportRequest.getRef());

                // Dopo un determinato tempo (2 secondi) il TransitManager termina
                // C'è un problema con la macchina
                system.scheduler().scheduleOnce(Duration.ofSeconds(2), () -> {
                    // Selezione tramite future del riferimento all'attore TransitManager
                    try {
                        ActorRef result = Await.result(system.actorSelection("user/MACCHINA/TRANSIT_MANAGER")
                                .resolveOne(Timeout.create(Duration.ofMillis(100))), scala.concurrent.duration.Duration.Inf());
                        result.tell(PoisonPill.getInstance(), null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, system.getDispatcher());

                // Se il TransitManager ha un problema, è assunto che la macchina sia rotta
                // Il passeggero si aspetta il messaggio che indica la rottura
                passenger.expectMsgClass(Duration.ofSeconds(10), Car.CarBreakDown.class);
            }
        };

    }


}