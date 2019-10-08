package uniud.distribuiti.lastmile.location;

import java.util.ArrayList;

public class Route {

    private int distance;
    private ArrayList<Integer> path;

    public Route(int distance , ArrayList<Integer> path){
        this.distance = distance;
        this.path = path;
    }

    public int getDistance() {
        return distance;
    }

    public ArrayList<Integer> getPath() {
        return path;
    }
}