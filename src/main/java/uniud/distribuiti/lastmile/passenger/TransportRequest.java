package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;


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

    private static class CarInformation{
        private TransportCoordination.CarAvailableMsg msg;
        private ActorRef transportRequestManager;

        public CarInformation(TransportCoordination.CarAvailableMsg msg, ActorRef transportRequestManager){

            this.msg =msg;
            this.transportRequestManager = transportRequestManager;
        }

        public TransportCoordination.CarAvailableMsg getMsg() {
            return msg;
        }

        public ActorRef getTransportRequestManager() {
            return transportRequestManager;
        }
    }
    private ArrayList<CarInformation> availableCars = new ArrayList<CarInformation>();
    class SortByEstTransTime implements Comparator<CarInformation>
    {

        public int compare(CarInformation a, CarInformation b)
        {
            return a.msg.getEstTransTime() - b.msg.getEstTransTime();
        }
    }

    public static Props props(){
        return Props.create(TransportRequest.class, () -> new TransportRequest());
    }

    public TransportRequest(){
        this.status = TransportRequestStatus.EMITTED;
    }

    @Override
    public void preStart(){
        System.out.println("TRANSPORT REQUEST STARTED");
    }

    private void evaluateCar(TransportCoordination.CarAvailableMsg msg){
        log.info("DISPONIBILITA RICEVUTA DA {}", getSender());

        //Nella lista di macchine Disponibili Abbiamo il riferimento al transportRequestManager
        // e le info della macchina
        availableCars.add(new CarInformation(msg,getSender()));

        // TODO come passo dalla evaluate car alla select ... devo aspettare un po prima di scegliere
    }

    private void carUnavaiable(TransportCoordination msg){
        log.info("RIMUOVO LA MACCHINA DALLA LISTA (GIÀ PRENOTATA) {}", getSender());

        // rimuovo la macchina se presente sulla lista
        for(int i =0; i<availableCars.size(); ++i)
        {
            if(availableCars.get(i).transportRequestManager == getSender()){
                availableCars.remove(i);
                break;
            }
        }
    }

    // Selezione di una macchina che ha dato disponibilità al passeggero
    private void selectCar(TransportCoordination msg){
        log.info("PRENOTO LA MACCHINA {}", availableCars.get(0));

        //ordino la lista di macchine disponibili per EstTransTime e prendo il primo
        Collections.sort(availableCars,new SortByEstTransTime());
        availableCars.get(0).transportRequestManager.tell(new TransportCoordination.CarBookingRequestMsg(), getSelf());
    }

    // Metodo che riceve la conferma della prenotazione di una macchina
    // Dovrà notificare il passeggero che entrerà in relazione diretta con la macchina per la fase di coordinamento
    // dello stato del trasporto.
    private void bookingConfirmation(TransportCoordination msg){
        // TODO: Notifica passeggero della ricezione di conferma della prenotazione da parte di una macchina
        //  Questo è l'entry poit della fase due della richiesta, la fase di coordinamento dello stato del trasporto.
        log.info("MACCHINA PRENOTATA {}", getSender());
    }

    // Metodo che disdice il booking di una macchina
    private void bookingRejected(TransportCoordination msg){
        log.info("PRENOTAZIONE MACCHINA RIFIUTATA, RIMUOVO MACCHINA DALLA LISTA {}", getSender());

        if(availableCars.contains(getSender()))
        availableCars.remove(getSender());
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
                .matchAny(
                        o -> {
                            log.info("{} - MESSAGGIO NON SUPPORTATO - {}", getSelf(), o);
                        }
                )
                .build();
    }
}
