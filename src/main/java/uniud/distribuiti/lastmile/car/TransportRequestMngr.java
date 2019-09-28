package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import uniud.distribuiti.lastmile.transportRequestCoordination.TransportCoordination;

// Transport Request Manager acotr
// Attore responsabile della gestione di una richiesta
// ricevuta da un passeggero.
// - valuta la disponibilita della macchiana a soddisfare la richiesta
// - controlla distanza
public class TransportRequestMngr extends AbstractActor {

    // Riferimento ad attore per coordinamento richiesta di trasporto
    private ActorRef transportRequest;

    private RequestManagerStatus status;
    private enum RequestManagerStatus {
        EVALUATION,         // Valutazione richiesta
        AVAILABLE,          // Macchina disponibile
        NOT_AVAILABLE,      // Macchina non disponibile
        EXPIRED,            // Si presume che la macchina non venga più considerata dal passeggero
        REJECTED,           // Macchina è stata respinta dal passeggero
        CONFIRMED,          // Macchina è stata confermata dal passeggero
    }

    public static Props props(ActorRef transportRequest){
        return Props.create(TransportRequestMngr.class, () -> new TransportRequestMngr(transportRequest));
    }

    public TransportRequestMngr(ActorRef transportRequest){
        this.transportRequest = transportRequest;
        this.status = RequestManagerStatus.EVALUATION;

        // TODO: Implementazione valutazione della richiesta
        //  - considerare la posizione del passeggero
        //  - considerare il carburante a disposizione del mio parent
        //  - formulazione di una risposta per la richiesta (anche in caso negativo?)


        // Risposta fake di disponibilità
        transportRequest.tell(new TransportCoordination.CarAvailableMsg(), getSelf());
    }

    private void manageBookingRequest(TransportCoordination msg){
        // TODO: Verifica disponibilità a prenotare la macchina
        //  Questo metodo verifica che la macchina sia disponibile
        //  successivamente risponde con la disponibilità

        // supponiamo che al momento la disponibilità sia confermata
        transportRequest.tell(new TransportCoordination.CarBookingConfirmedMsg(), getContext().getParent());
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(
                        TransportCoordination.CarBookingRequestMsg.class,
                        this::manageBookingRequest
                )
                .matchAny(
                        o -> {

                        }
                )
                .build();
    }
}
