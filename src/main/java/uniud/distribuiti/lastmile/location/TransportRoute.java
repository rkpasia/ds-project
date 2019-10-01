package uniud.distribuiti.lastmile.location;

import java.util.ArrayList;

public class TransportRoute {

    private ArrayList<Integer> path;
    private int currentNode;
    private int nextNode;

    public int getCurrentNode() {
        return currentNode;
    }

    public int getNextNode(){
        return nextNode;
    }

    public TransportRoute(ArrayList<Integer> thePath){
        this.path = thePath;
        currentNode = this.path.get(0);
        nextNode = this.path.get(1);
    }

    public void next(){
        if(this.path.size() >= 3){
            currentNode = nextNode;
            nextNode = path.get(2);
            path.remove(0);
        }
        else if(this.path.size() == 2){
            currentNode = nextNode;
            path.remove(0);
        }

    }
}
