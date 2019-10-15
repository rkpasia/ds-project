package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.cluster.ClusterServiceMessages;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.time.Duration;
import java.util.ArrayList;


// TransportRequest actor
// Questo è l'attore responsabile della gestione di una richiesta
// di trasporto effettuata dal passeggero parent di questo attore
public class TransportRequest extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private TransportRequestStatus status;
    private enum TransportRequestStatus {
        EMITTED,
        BOOKING,
        CONFIRMED
    }

    private ArrayList<CarInformation> availableCars = new ArrayList<CarInformation>();
    private int requestCallbacks = 0;
    private final int MAX_REQUEST_CALLBACKS = 6;
    private ActorRef selectedCarManager;

    public static class RequestMonitoring {}

    public static Props props(){
        return Props.create(TransportRequest.class, () -> new TransportRequest());
    }

    public TransportRequest(){
        this.status = TransportRequestStatus.EMITTED;
    }

    @Override
    public void preStart(){
        System.out.println("TRANSPORT REQUEST STARTED");
        requestMonitoring();
    }

    private void requestMonitoring() {
        getContext().system().scheduler().scheduleOnce(
                Duration.ofSeconds(5),
                getSelf(),
                new TransportRequest.RequestMonitoring(),
                getContext().system().dispatcher(),
                getSelf()
        );
    }

    private void monitorRequestStatus(RequestMonitoring msg){
        if(status == TransportRequestStatus.EMITTED && availableCars.isEmpty()){
            log.info("NESSUNA DISPONIBILITA RICEVUTA, RIPROVO");
            getContext().parent().tell(new Passenger.EmitRequestMessage(), getSelf());
            this.requestCallbacks += 1;
            if(requestCallbacks <= MAX_REQUEST_CALLBACKS){
                requestMonitoring();
            } else {
                log.info("TENTATIVI MASSIMI RAGGIUNTI");
                getContext().parent().tell(new ClusterServiceMessages.NoCarsAvailable(), getSelf());
            }
        }
    }

    private void evaluateCar(TransportCoordination.CarAvailableMsg msg){
        log.info("DISPONIBILITA RICEVUTA DA {}", getSender());

        // Nella lista di macchine Disponibili Abbiamo il riferimento al transportRequestManager
        // e le info della macchina
        availableCars.add(new CarInformation(msg.getRouteLength(), getSender()));

        // Monitoring del manager che mi ha dato disponibilità
        getContext().watch(getSender());
    }

    private void carUnavaiable(TransportCoordination msg){
        log.info("RIMUOVO LA MACCHINA DALLA LISTA (GIÀ PRENOTATA) {}", getSender());

        // Rimuovo la macchina se presente sulla lista
        availableCars.removeIf(car -> car.getTransportRequestManager().equals(getSender()));
    }

    // Selezione di una macchina che ha dato disponibilità al passeggero
    private void selectCar(TransportCoordination msg){
        // Con la strategia sottostante il software sarà molto flessibile perché permetterà l'implementazione
        // di tecniche di selezione più sofisticate.

        // Ordino la lista di macchine disponibili per EstTransTime e prendo il primo
        if(!availableCars.isEmpty()){
            availableCars.sort(new CarInformation.SortByEstTransTime());
            CarInformation car = availableCars.get(0);

            car.getTransportRequestManager().tell(new TransportCoordination.CarBookingRequestMsg(), getSender());
            selectedCarManager = car.getTransportRequestManager();
            this.status = TransportRequestStatus.BOOKING;
            log.info("PRENOTO LA MACCHINA {}", availableCars.get(0).getTransportRequestManager().path().parent().name());
        } else {
            // TODO: Che cosa fa il passeggero quando non ci sono più macchine disponibili?
            log.warning("NON CI SONO MACCHINE DISPONIBILI!!!");
        }
    }

    // Metodo che riceve la conferma della prenotazione di una macchina
    // Dovrà notificare il passeggero che entrerà in relazione diretta con la macchina per la fase di coordinamento
    // dello stato del trasporto.
    private void bookingConfirmation(TransportCoordination msg){
        log.info("MACCHINA PRENOTATA {}", getSender());
        // Invio messaggio al passeggero
        // msg::CarBookingConfirmed
        getContext().parent().tell(msg, getSender());
        this.status = TransportRequestStatus.CONFIRMED;
    }

    // Metodo che disdice il booking di una macchina
    private void bookingRejected(TransportCoordination msg){
        log.info("PRENOTAZIONE MACCHINA RIFIUTATA, RIMUOVO MACCHINA DALLA LISTA {}", getSender());

        availableCars.removeIf(car -> car.getTransportRequestManager().equals(getSender()));

        // TODO: Che facciamo se la prenotazione è respinta?
        //  Bisogna avvisare il passeggero (quindi dare feedback anche all'utente)
    }

    // Metodo per la gestione terminazioni attori in monitoraggio
    private void terminationHandling(Terminated msg){

        // Terminazione di un manager
        // Se termina durante lo stato di emissione...
        if(this.status == TransportRequestStatus.EMITTED){
            // lo rimuovo semplicemente dalla lista di selezionabili
            availableCars.removeIf(car -> car.getTransportRequestManager().equals(msg.getActor()));

            // Possibile la valutazione di nuova richiesta alla macchina per sapere la disponibilità nuovamente
            // (non strettamente necessario)
        }

        if(this.status == TransportRequestStatus.BOOKING){
            // Se sto prenotando...
            // e il manager della macchina che sto provando a prenotare muore..
            // allora devo fare qualcosa
            if(selectedCarManager.equals(msg.actor())){
                // Lo rimuovo dalle macchine disponibili
                availableCars.removeIf(car -> car.getTransportRequestManager().equals(msg.getActor()));
                // Avvisare il passeggero che la selezione della macchina ha avuto un problema
                carBookingHasStopped();
            }

            // Se non lo stavo prenotando, rimuovo semplicemente
            availableCars.removeIf(car -> car.getTransportRequestManager().equals(msg.getActor()));
        }

    }

    // La prenotazione di una macchina ha avuto un problema
    private void carBookingHasStopped(){
        // Avviso il passeggero che la selezione della macchina si è interrotta
        getContext().parent().tell(new Passenger.SelectionStopped(), getSelf());
        this.status = TransportRequestStatus.EMITTED;
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(
                        TransportCoordination.CarAvailableMsg.class,
                        this::evaluateCar
                )
                .match(
                        TransportCoordination.SelectCarMsg.class,
                        this::selectCar
                )
                .match(
                        TransportCoordination.CarBookingConfirmedMsg.class,
                        this::bookingConfirmation
                )
                .match(
                        TransportCoordination.CarBookingRejectMsg.class,
                        this::bookingRejected
                )
                .match(
                        TransportCoordination.CarUnavailableMsg.class,
                        this::carUnavaiable
                )
                .match(
                        TransportRequest.RequestMonitoring.class,
                        this::monitorRequestStatus
                )
                .match(
                        Terminated.class,
                        this::terminationHandling
                )
                .matchAny(
                        o -> {
                            log.info("{} - MESSAGGIO NON SUPPORTATO - {}", getSelf(), o);
                        }
                )
                .build();
    }
}
