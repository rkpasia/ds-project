package uniud.distribuiti.client;

import uniud.distribuiti.lastmile.PassengerNode;

public class Passenger {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("NODO PASSENGER");
        PassengerNode passenger = new PassengerNode();

        passenger.sendRequest();

        Thread.sleep(15000);

        passenger.selectCar();
    }
}