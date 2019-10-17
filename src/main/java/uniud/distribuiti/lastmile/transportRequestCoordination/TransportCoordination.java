package uniud.distribuiti.lastmile.transportRequestCoordination;

import akka.actor.ActorRef;
import uniud.distribuiti.lastmile.location.Location;
import uniud.distribuiti.lastmile.location.Route;
import uniud.distribuiti.lastmile.passenger.TransportRequest;

import java.io.Serializable;

public class TransportCoordination implements Serializable {

    // Messaggio di disponibilità macchina per passeggero
    // Deve contenere informazioni sufficienti per permettere al passeggero
    // di valutare la disponibilità ricevuta
    public static class CarAvailableMsg extends TransportCoordination{

        private final int routeLength;

        public CarAvailableMsg(int routeLength){
            this.routeLength = routeLength;
        }

        public int getRouteLength() {
            return routeLength;
        }

    }

    // Messaggio di indisponibilità della macchina
    // Questo può essere inviato attivamente oppure si può lasciare che
    // il passeggero scopra più tardi che la macchina non è più disponibile
    public static class CarUnavailableMsg extends TransportCoordination {}

    // Messaggio di richiesta prenotazione macchina da parte del passeggero
    public static class CarBookingRequestMsg extends TransportCoordination {

        public final ActorRef passenger;
        public final Route route;
        public final Location location;

        public CarBookingRequestMsg(){
            this.passenger = null;
            this.route = null;
            this.location = null;
        }

        public CarBookingRequestMsg(ActorRef passenger, Route route, Location location){
            this.passenger = passenger;
            this.route = route;
            this.location = location;
        }
    }

    // Messaggio di notifica che la macchina è stata prenotata da un altro passeggero
    public static class CarHasBeenBooked extends TransportCoordination {}

    // Messaggio conferma il booking di una macchina
    public static class CarBookingConfirmedMsg extends TransportCoordination {}

    // Messaggio smentisce il booking di una macchina
    public static class CarBookingRejectMsg extends TransportCoordination {}

    // Messaggio per richiedere la selezione di una macchina
    public static class SelectCarMsg extends TransportCoordination {}

    // Macchina ha raggiunto la location presso cui si trova il passeggero
    public static class CarArrivedToPassenger extends TransportCoordination {}

    // Macchina ha raggiunto la destinazione presso cui il passeggero ha richiesto di essere portato
    public static class DestinationReached extends TransportCoordination {

        private final Location location;
        private final int distance;

        public DestinationReached(Location location){
            this(location, 0);
        }

        public DestinationReached(Location location, int km){
            this.location = location;
            this.distance = km;
        }

        public Location getLocation() {
            return location;
        }

        public int getDistanceCovered() { return this.distance; }
    }

    public static class AbortTransportRequest extends TransportCoordination {}

    public static class UpdateLocation extends TransportCoordination {

        public final int location;

        public UpdateLocation(int newLocation){
            this.location = newLocation;
        }

    }

}
