package uniud.distribuiti.lastmile.car.fuel;

public class FuelTank {

    private double fuel;

    public FuelTank(double fuel){
        this.fuel = fuel;
    }

    public boolean hasEnoughFuel(double fuelConsumption){
        return (fuel - fuelConsumption) < 0 ? false : true;
    }

}
