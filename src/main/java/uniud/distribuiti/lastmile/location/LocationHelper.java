package uniud.distribuiti.lastmile.location;

import scala.Int;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class LocationHelper {

    private static final int NO_PARENT = -1;

    private static int[][]  graph = null;
    private int distance;
    private ArrayList<Integer> path = new ArrayList<Integer>();

    public LocationHelper() throws FileNotFoundException {

        ClassLoader classLoader = getClass().getClassLoader();

        InputStream inputStream = classLoader.getResourceAsStream("graph.txt");

        Scanner scan = new Scanner(inputStream);
        int row = scan.nextInt();
        graph = new int[row][row];
        for(int r=0; r<row; ++r) {
            for (int c = 0; c < row; ++c) {
                graph[r][c] = scan.nextInt();

            }
        }
    }

    public int getDistance(){
        return this.distance;
    }

    public ArrayList<Integer> getPath() {
        return this.path;
    }

    public static Location assignLocation(){
        int dim = graph[0].length;
        return  new Location(new Random().nextInt(dim));
    }

    private void shortestPath  (int startVertex, int finalVertex) {
        int nVertices = graph[0].length;

        int[] shortestDistances = new int[nVertices];
        boolean[] added = new boolean[nVertices];

        for (int vertexIndex = 0; vertexIndex < nVertices;
             vertexIndex++) {
            shortestDistances[vertexIndex] = Integer.MAX_VALUE;
            added[vertexIndex] = false;
        }

        shortestDistances[startVertex] = 0;

        int[] parents = new int[nVertices];

        parents[startVertex] = NO_PARENT;

        for (int i = 1; i < nVertices; i++) {
            int nearestVertex = -1;
            int shortestDistance = Integer.MAX_VALUE;
            for (int vertexIndex = 0;
                 vertexIndex < nVertices;
                 vertexIndex++) {
                if (!added[vertexIndex] &&
                        shortestDistances[vertexIndex] <
                                shortestDistance) {
                    nearestVertex = vertexIndex;
                    shortestDistance = shortestDistances[vertexIndex];
                }
            }
            added[nearestVertex] = true;

            for (int vertexIndex = 0;
                 vertexIndex < nVertices;
                 vertexIndex++) {
                int edgeDistance = graph[nearestVertex][vertexIndex];

                if (edgeDistance > 0
                        && ((shortestDistance + edgeDistance) <
                        shortestDistances[vertexIndex])) {
                    parents[vertexIndex] = nearestVertex;
                    shortestDistances[vertexIndex] = shortestDistance +
                            edgeDistance;
                }
            }
        }

        this.distance = shortestDistances[finalVertex];

        this.path = new ArrayList<Integer>();
        makePath(finalVertex,parents);
    }

    private  void makePath(int startVertex,
                                  int[] parents)
    {
        if (startVertex == NO_PARENT)
        {
            return;
        }
        makePath(parents[startVertex], parents);
        this.path.add(startVertex);
    }

    public void shortestPathToDestination(int myVertex,int passengerVertex, int toVertex){
        this.shortestPath(myVertex,passengerVertex);
        int partialDistance = distance;
        ArrayList<Integer> partialPath = path;
        partialPath.remove(partialPath.size()-1);
        this.shortestPath(passengerVertex,toVertex);
        distance += partialDistance;
        partialPath.addAll(path);
        path = partialPath;
    }

}
