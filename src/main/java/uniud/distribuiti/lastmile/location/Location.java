package uniud.distribuiti.lastmile.location;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.*;
import java.util.Random;
import java.util.Scanner;
import java.util.ArrayList;

public class Location {

    private static final int NO_PARENT = -1;

    private static int[][]  graph = null;
    private static ArrayList<Integer> path = new ArrayList<Integer>();
    private static int distance = -1;
    private static int position;

    public Location() throws FileNotFoundException {

        ClassLoader classLoader = getClass().getClassLoader();

        InputStream inputStream = classLoader.getResourceAsStream("graph.txt");

        Scanner scan = new Scanner(inputStream);
        int row = scan.nextInt();
        int col = scan.nextInt();
        graph = new int[row][col];
        for(int r=0; r<row; r++) {
            for (int c = 0; c < col; c++) {
                graph[r][c] = scan.nextInt();

            }
        }
        position = new Random().nextInt(row);
    }

    public ArrayList<Integer> getPath() {
        return  path;
    }

    public int getPosition(){
        return position;
    }

    public int getDistance(){
        return distance;
    }

    public void setPosition( int newPosition){
        position = newPosition;
    }

    public static void shortestPath  (int startVertex, int finalVertex) {
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

        distance = shortestDistances[finalVertex];
        makePath(finalVertex,parents);
    }

    private static void makePath(int startVertex,
                                  int[] parents)
    {
        if (startVertex == NO_PARENT)
        {
            return;
        }
        makePath(parents[startVertex], parents);
        path.add(startVertex);
    }

    public static void printDistance () {
        System.out.println("Distance " + distance);
    }

    public static void printPath() {
        System.out.print("Path ");
        for(int value : path) System.out.print(value + " ");
    }
}
