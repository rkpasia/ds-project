package uniud.distribuiti.lastmile.passenger;

import akka.actor.ActorRef;

import java.util.Comparator;

class CarInformation {

    private int routeLength;
    private ActorRef transportRequestManager;

    CarInformation(int routeLength, ActorRef transportRequestManager){

        this.routeLength = routeLength;
        this.transportRequestManager = transportRequestManager;
    }

     int getRouteLength() {
        return routeLength;
    }

    ActorRef getTransportRequestManager() {
        return transportRequestManager;
    }

    public static class SortByEstTransTime implements Comparator<CarInformation> {
        public int compare(CarInformation a, CarInformation b)
        {
            return a.getRouteLength() - b.getRouteLength();
        }
    }

}
