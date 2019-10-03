package uniud.distribuiti.lastmile.transportRequestCoordination;

import uniud.distribuiti.lastmile.car.Car;

import java.io.Serializable;

public class TransportCoordination implements Serializable {

    // Messaggio di disponibilità macchina per passeggero
    // Deve contenere informazioni sufficienti per permettere al passeggero
    // di valutare la disponibilità ricevuta
    public static class CarAvailableMsg extends TransportCoordination{}

    // Messaggio di indisponibilità della macchina
    // Questo può essere inviato attivamente oppure si può lasciare che
    // il passeggero scopra più tardi che la macchina non è più disponibile
    public static class CarUnavailableMsg extends TransportCoordination {}

    // Messaggio di richiesta prenotazione macchina da parte del passeggero
    public static class CarBookingRequestMsg extends TransportCoordination {}

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
    public static class DestinationReached extends TransportCoordination {}

}
