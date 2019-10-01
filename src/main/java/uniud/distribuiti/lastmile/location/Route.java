package uniud.distribuiti.lastmile.location;

import java.util.ArrayList;

public class  Route{

    public  int distance;
    public ArrayList<Integer> path;

    public Route(int distance , ArrayList<Integer> path){
        this.distance = distance;
        this.path = path;
    }

}