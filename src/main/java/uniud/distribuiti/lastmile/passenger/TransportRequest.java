package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.Props;
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
        // TODO: Notifica passeggero della ricezione di conferma della prenotazione da parte di una macchina
        //  Questo è l'entry poit della fase due della richiesta, la fase di coordinamento dello stato del trasporto.
        log.info("MACCHINA PRENOTATA {}", getSender());
        this.status = TransportRequestStatus.CONFIRMED;
    }

    // Metodo che disdice il booking di una macchina
    private void bookingRejected(TransportCoordination msg){
        log.info("PRENOTAZIONE MACCHINA RIFIUTATA, RIMUOVO MACCHINA DALLA LISTA {}", getSender());

        availableCars.removeIf(car -> car.getTransportRequestManager().equals(getSender()));

        // TODO: Che facciamo se la prenotazione è respinta?
        //  Bisogna avvisare il passeggero (quindi dare feedback anche all'utente)
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
                .matchAny(
                        o -> {
                            log.info("{} - MESSAGGIO NON SUPPORTATO - {}", getSelf(), o);
                        }
                )
                .build();
    }
}
