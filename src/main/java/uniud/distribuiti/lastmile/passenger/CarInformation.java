package uniud.distribuiti.lastmile.passenger;

import akka.actor.ActorRef;

public class CarInformation {

    private int routeLength;
    private ActorRef transportRequestManager;

    public CarInformation(int routeLength, ActorRef transportRequestManager){

        this.routeLength = routeLength;
        this.transportRequestManager = transportRequestManager;
    }

    public int  getRouteLength() {
        return routeLength;
    }

    public ActorRef getTransportRequestManager() {
        return transportRequestManager;
    }

}
