package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.dsl.Creators;
import uniud.distribuiti.lastmile.passenger.TransportRequest;

// Transport Request Manager acotr
// Attore responsabile della gestione di una richiesta
// ricevuta da un passeggero.
// - valuta la disponibilita della macchiana a soddisfare la richiesta
// - controlla distanza
public class TransportRequestMngr extends AbstractActor {

    public static Props props(ActorRef transportRequest){
        return Props.create(TransportRequestMngr.class, () -> new TransportRequestMngr(transportRequest));
    }

    public TransportRequestMngr(ActorRef transportRequest){
        this.transportRequest = transportRequest;

        //TODO: Implementazione valutazione della richiesta
        // - considerare la posizione del passeggero
        // - considerare il carburante a disposizione del mio parent
        // - formulazione di una risposta per la richiesta (anche in caso negativo?)


        // Risposta fake di disponibilità
        transportRequest.tell(new TransportRequest.AvailableCarMessage(), getSelf());
    }

    private ActorRef transportRequest;

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .matchAny(
                        o -> {

                        }
                )
                .build();
    }
}
