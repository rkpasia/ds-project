package uniud.distribuiti.lastmile.location;

import java.util.ArrayList;
import java.util.Iterator;

public class TransportRoute {

    private ArrayList<Integer> path;
    private int currentNode;
    private Iterator pathIterator;

    public TransportRoute(ArrayList<Integer> thePath){
        this.path = thePath;
        this.pathIterator = path.iterator();
        this.currentNode = (int)pathIterator.next();
    }

    public int getCurrentNode() {
        return currentNode;
    }

    public boolean goToNext(){
        if(pathIterator.hasNext()){
            this.currentNode = (int)pathIterator.next();
            return true;
        } else {
            return false;
        }
    }
}
