package uniud.distribuiti.lastmile.passenger;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

// TransportRequest actor
// Questo è l'attore responsabile della gestione di una richiesta
// di trasporto effettuata dal passeggero parent di questo attore
public class TransportRequest extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(){
        return Props.create(TransportRequest.class, () -> new TransportRequest());
    }

    public TransportRequest(){}

    @Override
    public void preStart(){
        System.out.println("TRANSPORT REQUEST STARTED");
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .matchAny(
                        o -> {
                             log.info("{} - MESSAGGIO NON SUPPORTATO - {}", getSelf(), o);
                        }
                )
                .build();
    }
}
