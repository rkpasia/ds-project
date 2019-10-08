package uniud.distribuiti.lastmile.location;

import java.util.ArrayList;
import java.util.Iterator;

public class TransportRoute {

    private Route route;
    private int currentNode;
    private Iterator pathIterator;

    public TransportRoute(Route route){
        this.route = route;
        this.pathIterator = route.getPath().iterator();
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

    public Route getRoute(){
        return this.route;
    }
}
