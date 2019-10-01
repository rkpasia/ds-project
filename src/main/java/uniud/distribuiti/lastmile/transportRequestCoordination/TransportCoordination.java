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

    // Messaggio conferma il booking di una macchina
    public static class CarBookingConfirmedMsg extends TransportCoordination {}

    // Messaggio smentisce il booking di una macchina
    public static class CarBookingRejectMsg extends TransportCoordination {}

    // Messaggio per richiedere la selezione di una macchina
    public static class SelectCarMsg extends TransportCoordination {}

}
