package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.car.Car;
import uniud.distribuiti.lastmile.cluster.ClusterServiceMessages;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.LocationHelper;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

public class Passenger extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(Passenger.class, () -> new Passenger());
    }

    public static class EmitRequestMessage {}

    public static class SelectCarMessage {}

    private ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();

    private ActorRef transportRequest;
    private ActorRef car;

    private Location location;
    private PassengerStatus status;

    private enum PassengerStatus {
        IDLE,
        REQUEST_EMITTED,
        SELECTION_REQUESTED,
        WAITING_CAR,
        IN_TRANSPORT
    }

    public Passenger(){
        LocationHelper locationHelper = new LocationHelper();
        this.location = locationHelper.assignLocation();
        this.status = PassengerStatus.IDLE;
    }

    // Inoltro richiesta di trasporto
    // Inizializzo nuovo attore
    // Inoltro richiesta con riferimento all attore figlio gestore della mia richiesta
    private void emitTransportRequest(EmitRequestMessage msg){
        if(this.transportRequest == null || this.transportRequest.isTerminated()){
            transportRequest = getContext().actorOf(TransportRequest.props(), "TRANSPORT_REQUEST@" + self().path().name());
            getContext().watch(transportRequest);
        }

        // In questo momento tutti i passeggeri vogliono andare al nodo 0
        // TODO: Miglioramento del messaggio per gestire le location inviate
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(location.getNode(), 0)), transportRequest);

        // Richiesta emessa - sta gestendo la TransportRequest incaricata
        this.status = PassengerStatus.REQUEST_EMITTED;
    }

    private void selectCar(SelectCarMessage msg){
        log.info("SCELGO LA MACCHINA");
        transportRequest.tell(new TransportCoordination.SelectCarMsg(), getSelf());
        this.status = PassengerStatus.SELECTION_REQUESTED;
    }

    private void carArrived(TransportCoordination msg){
        this.car = getSender();     // Riferimento alla macchina che mi sta trasportando
        // Passenger is now in transit
        this.status = PassengerStatus.IN_TRANSPORT;
    }

    private void destinationReached(TransportCoordination.DestinationReached msg){
        this.location.setNode(msg.getLocation().getNode());
        log.info("DESTINAZIONE RAGGIUNTA");
        this.status = PassengerStatus.IDLE;
    }

    private void carBrokenInLocation(Car.BrokenLocation msg){
        log.info("MACCHINA ROTTA CON PASSEGGERO A BORDO, RICHIEDO NUOVA");
        this.location.setNode(msg.location.getNode());
        //transportRequest.tell(msg, getSelf());
        // Richiesta nuova macchina per passeggero
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(location.getNode(), 0)), transportRequest);
        this.status = PassengerStatus.REQUEST_EMITTED;
        // TODO: Scheduling messaggio automatico per prenotare la macchina più vicina disponibile
    }

    private void carBroken(Car.CarBreakDown msg){
        log.info("MACCHINA HA AVUTO PROBLEMA MENTRE IN ARRIVO, RICHIEDO NUOVA");
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(location.getNode(), 0)), transportRequest);
        this.status = PassengerStatus.REQUEST_EMITTED;
        // A questo punto l'utente può scegliere dall'applicazione la macchina nuova
    }

    private void noTransportAvailable(ClusterServiceMessages msg){
        log.info("NESSUNA MACCHINA DISPONIBILE PER OFFRIRE IL SERVIZIO DI TRASPORTO, RIPROVARE PIU' TARDI");
        getContext().stop(transportRequest);
        this.status = PassengerStatus.IDLE;
    }

    // Metodo per la gestione della terminazione di un attore monitorato dal Passeggero
    // Supporta la gestione del flusso corretto di esecuzione della business logic
    private void terminationHandling(Terminated msg){

        // Gestione terminazione transport request
        if(msg.actor().equals(transportRequest) && this.status == PassengerStatus.REQUEST_EMITTED){
            // Se la transport request termina prima
            // che sia riuscita a prenotare una macchina
            // allora c'è qualcosa che non va.
            // Devo riprendere l'operazione da capo

            // TODO: Notifica broadcast della terminazione transport request?
            //  Faccio ammazzare tutti i manager associati a questa transport request?
            //  Probabilmente soluzione più semplice.

            // Emissione nuova transport request
            // getSelf().tell(new EmitRequestMessage(), getSelf());
        }

    }

    @Override
    public Receive createReceive(){

        return receiveBuilder()
                .match(EmitRequestMessage.class, this::emitTransportRequest)
                .match(SelectCarMessage.class, this::selectCar)
                .match(TransportCoordination.CarArrivedToPassenger.class, this::carArrived)
                .match(TransportCoordination.DestinationReached.class, this::destinationReached)
                .match(Car.BrokenLocation.class, this::carBrokenInLocation)
                .match(Car.CarBreakDown.class, this::carBroken)
                .match(ClusterServiceMessages.NoCarsAvailable.class, this::noTransportAvailable)
                .match(Terminated.class, this::terminationHandling)
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
