package uniud.distribuiti.lastmile.car.fuel;

public class FuelTank {

    private double fuel;                // In litri rimasti
    private final int TANK_CAPACITY;    // In litri

    public FuelTank(int tankCapacity){
        this.TANK_CAPACITY = tankCapacity;
        this.fuel = TANK_CAPACITY;
    }

    public boolean hasEnoughFuel(double fuelConsumption){
        return !((fuel - fuelConsumption) < 0);
    }

    // Carburante consumato
    public void fuelConsumed(double fuelConsumption){
        fuel -= fuelConsumption;
    }
    
    // Macchina ha bisogno di carburante se ne ha meno del 20%
    public boolean needFuel(){
        double percentLeft = fuel / TANK_CAPACITY * 100;
        return percentLeft < 20;
    }

    // Assumiamo che quando una macchina fa rifornimento, fa il pieno
    public void refuel(){
        fuel = TANK_CAPACITY;
    }

}
