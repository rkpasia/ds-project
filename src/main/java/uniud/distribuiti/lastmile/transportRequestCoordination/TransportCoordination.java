package uniud.distribuiti.lastmile.transportRequestCoordination;

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

    // Messaggio stabilisce l'accordo tra macchina e passeggero
    public static class CarBookingConfirmedMsg extends TransportCoordination {}

}
