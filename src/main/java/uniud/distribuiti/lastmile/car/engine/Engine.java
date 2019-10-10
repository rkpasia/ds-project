package uniud.distribuiti.lastmile.car.engine;

public class Engine {

    private final double AVG_KM_PER_LITER;

    public Engine(double kmPerLiter) {
        this.AVG_KM_PER_LITER = kmPerLiter;
    }

    public double fuelConsumption(double km){
        return km / AVG_KM_PER_LITER;
    }
}
