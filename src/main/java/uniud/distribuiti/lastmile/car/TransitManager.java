package uniud.distribuiti.lastmile.car;

import akka.actor.AbstractActor;
import akka.actor.Props;
import uniud.distribuiti.lastmile.location.TransportRoute;

// Attore che gestisce il trasporto e lo spostamento
// di una macchina all'interno della rete geografica
public class TransitManager extends AbstractActor {

    public static Props props(TransportRoute route){
        return Props.create(TransitManager.class, () -> new TransitManager(route));
    }

    private final TransportRoute route;

    public TransitManager(TransportRoute route){
        this.route = route;
    }

    @Override
    public Receive createReceive(){
        return receiveBuilder().build();
    }
}
