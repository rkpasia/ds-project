package uniud.distribuiti.lastmile.location;

import java.io.Serializable;

public class Location implements Serializable {

    private int node;

    public Location(int node){
        this.node = node;
    }

    public int getNode(){
        return this.node;
    }

    public void setNode(int newNode){
        this.node=newNode;
    }
}
