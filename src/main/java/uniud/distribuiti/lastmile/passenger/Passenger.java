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
        return Props.create(Passenger.class, Passenger::new);
    }

    public static class EmitRequestMessage {}

    public static class SelectCarMessage {}

    static class SelectionStopped {}

    private static class TransportRequestCompromised {}

    private ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();

    private ActorRef transportRequest;
    private ActorRef car;
    private int dest;

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
        try {
            LocationHelper locationHelper = new LocationHelper();
            this.location = locationHelper.assignLocation();
            this.dest = locationHelper.assignLocation().getNode();
        }catch (Exception ex){
                log.info(ex.getMessage());
        }
        this.status = PassengerStatus.IDLE;
    }

    // Inoltro richiesta di trasporto
    // Inizializzo nuovo attore
    // Inoltro richiesta con riferimento all attore figlio gestore della mia richiesta
    private void emitTransportRequest(EmitRequestMessage msg){
        if(this.transportRequest == null || this.transportRequest.isTerminated()){
            transportRequest = getContext().actorOf(TransportRequest.props(), "TRANSPORT_REQUEST@" + self().path().uid());
            getContext().watch(transportRequest);
        }

        // In questo momento tutti i passeggeri vogliono andare al nodo 0
        // TODO: Miglioramento del messaggio per gestire le location inviate
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(location.getNode(), dest)), transportRequest);

        // Richiesta emessa - sta gestendo la TransportRequest incaricata
        this.status = PassengerStatus.REQUEST_EMITTED;
    }

    private void selectCar(SelectCarMessage msg){
        log.info("SCELGO LA MACCHINA");
        transportRequest.tell(new TransportCoordination.SelectCarMsg(), getSelf());
        this.status = PassengerStatus.SELECTION_REQUESTED;
    }

    private void bookingConfirmed(TransportCoordination msg){
        // Se il passeggero è già in WAITING non effettuare l'aggiornamento
        if(this.status == PassengerStatus.WAITING_CAR) return;
        // Il passeggero entra in attesa della macchina prenotata
        this.status = PassengerStatus.WAITING_CAR;
        this.car = getSender();
        getContext().watch(car);
    }

    private void carSelectionStopped(SelectionStopped msg){
        // La selezione della macchina si è interrotta per un problema
        // Devo selezionare un altra macchina
        log.error("SELEZIONE MACCHINA INTERROTTA, PROVARE CON UN ALTRA MACCHINA");
        // Dovrà arrivare un nuovo messaggio di richiesta selezione dall'utente
        this.status = PassengerStatus.REQUEST_EMITTED;  // Torno a stato di richiesta emessa
    }

    private void updateLocation(TransportCoordination.UpdateLocation msg){
        this.location.setNode(msg.location);
    }

    private void carArrived(TransportCoordination msg){
        //this.car = getSender();     // Riferimento alla macchina che mi sta trasportando
        // Passenger is now in transit
        this.status = PassengerStatus.IN_TRANSPORT;
    }

    private void destinationReached(TransportCoordination.DestinationReached msg){
        this.location.setNode(msg.getLocation().getNode());
        log.info("DESTINAZIONE RAGGIUNTA");
        this.status = PassengerStatus.IDLE;
        // La TransportRequest ha completato il suo scopo
        if (!transportRequest.isTerminated()) {
            getContext().unwatch(transportRequest);
            getContext().stop(transportRequest);
        }
        getContext().unwatch(this.car);
    }

    private void carBrokenInLocation(Car.BrokenLocation msg){
        log.info("MACCHINA ROTTA CON PASSEGGERO A BORDO, RICHIEDO NUOVA");
        this.location.setNode(msg.location.getNode());
        //transportRequest.tell(msg, getSelf());
        // Richiesta nuova macchina per passeggero
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(location.getNode(), dest)), transportRequest);
        this.status = PassengerStatus.REQUEST_EMITTED;
        // TODO: Scheduling messaggio automatico per prenotare la macchina più vicina disponibile
    }

    private void carBroken(Car.CarBreakDown msg){
        log.info("MACCHINA HA AVUTO PROBLEMA MENTRE IN ARRIVO, RICHIEDO NUOVA");
        mediator.tell(new DistributedPubSubMediator.Publish("REQUEST", new Car.TransportRequestMessage(location.getNode(), dest)), transportRequest);
        this.status = PassengerStatus.REQUEST_EMITTED;
        // A questo punto l'utente può scegliere dall'applicazione la macchina nuova
        // TODO: Avviso interfaccia della nuova disponibilità di macchine a soddisfare la richiesta, eventualmente.
    }

    private void noTransportAvailable(ClusterServiceMessages msg){
        log.info("NESSUNA MACCHINA DISPONIBILE PER OFFRIRE IL SERVIZIO DI TRASPORTO, RIPROVARE PIU' TARDI");
        getContext().stop(transportRequest);
        this.status = PassengerStatus.IDLE;
    }

    // Metodo per la gestione della terminazione di un attore monitorato dal Passeggero
    // Supporta la gestione del flusso corretto di esecuzione della business logic
    private void terminationHandling(Terminated msg){

        log.info("RILEVATA LA MORTE DI UN ATTORE " + msg.actor().path().name());


        // Gestione terminazione transport request
        if(msg.actor().equals(transportRequest) && this.status == PassengerStatus.REQUEST_EMITTED){
            // Se la transport request termina prima
            // che sia riuscita a prenotare una macchina
            // allora c'è qualcosa che non va.
            // Devo riprendere l'operazione da capo

            getContext().unwatch(msg.getActor());
            mediator.tell(new DistributedPubSubMediator.Publish("ABORT_REQUEST", new TransportCoordination.AbortTransportRequest()), msg.getActor());
            // Emissione nuova transport request
            getSelf().tell(new EmitRequestMessage(), getSelf());
        }

        // Tutta una serie di conseguenze devono essere considerate
        if(msg.getActor().equals(transportRequest) && this.status == PassengerStatus.SELECTION_REQUESTED){
            getContext().unwatch(msg.getActor());
            mediator.tell(new DistributedPubSubMediator.Publish("ABORT_REQUEST", new TransportCoordination.AbortTransportRequest()), msg.getActor());
            getSelf().tell(new TransportRequestCompromised(), getSelf());
        }

        // Gestione ricezione terminazione della macchina
        if(this.status == PassengerStatus.WAITING_CAR && msg.getActor().equals(car)){
            log.info("LA MACCHINA CHE STO ASPETTANDO HA AVUTO UN PROBLEMA");
            getContext().unwatch(msg.getActor());
            // Se sto aspettando, mi dico di inizializzare una nuova richiesta di trasporto
            getSelf().tell(new Car.CarBreakDown(), getSelf());
        }
        if(this.status == PassengerStatus.IN_TRANSPORT && msg.getActor().equals(car)){
            log.info("LA MACCHINA CHE MI STA TRASPORTANDO HA AVUTO UN PROBLEMA");
            getContext().unwatch(msg.getActor());
            // Se ero in transito con la macchina, richiedo e scelgo automaticamente una nuova macchina
            getSelf().tell(new Car.BrokenLocation(this.location), getSelf());
        }

    }

    // Gestione ricezione messaggio di compromissione transport request
    private void requestCompromised(TransportRequestCompromised msg){
        // TODO: Notifica utente che la richiesta ha avuto un problema
        // Procedo con l'emissione automatica di una nuova richiesta di trasporto
        log.info("RICHIESTA DI TRASPORTO COMPROMESSA");
        getSelf().tell(new EmitRequestMessage(), getSelf());
    }

    @Override
    public Receive createReceive(){

        return receiveBuilder()
                .match(EmitRequestMessage.class, this::emitTransportRequest)
                .match(SelectCarMessage.class, this::selectCar)
                .match(TransportCoordination.CarBookingConfirmedMsg.class, this::bookingConfirmed)
                .match(TransportCoordination.CarArrivedToPassenger.class, this::carArrived)
                .match(TransportCoordination.DestinationReached.class, this::destinationReached)
                .match(TransportCoordination.UpdateLocation.class, this::updateLocation)
                .match(Car.BrokenLocation.class, this::carBrokenInLocation)
                .match(Car.CarBreakDown.class, this::carBroken)
                .match(SelectionStopped.class, this::carSelectionStopped)
                .match(TransportRequestCompromised.class, this::requestCompromised)
                .match(ClusterServiceMessages.NoCarsAvailable.class, this::noTransportAvailable)
                .match(Terminated.class, this::terminationHandling)
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
