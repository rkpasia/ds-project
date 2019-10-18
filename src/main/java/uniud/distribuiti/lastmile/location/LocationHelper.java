package uniud.distribuiti.lastmile.location;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class LocationHelper {

    private static final int NO_PARENT = -1;

    private static int[][] graph;

    public LocationHelper() {
        this.setupGraph();
    }

    private void setupGraph(){
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("graph1.txt");
        Scanner scan = new Scanner(inputStream);
        int row = scan.nextInt();
        graph = new int[row][row];
        for(int r=0; r<row; ++r) {
            for (int c = 0; c < row; ++c) {
                graph[r][c] = scan.nextInt();
            }
        }
    }

    public static Location assignLocation(){
        int dim = LocationHelper.graph[0].length;
        return  new Location(new Random().nextInt(dim));
    }

    public static Route shortestPath  (int startVertex, int finalVertex) {
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



        int dist = shortestDistances[finalVertex];
        ArrayList<Integer> path = makePath(finalVertex,parents);

        return new  Route(dist,path);
    }

    private static ArrayList<Integer> makePath(int startVertex,
                                  int[] parents)
    {
        ArrayList<Integer> partialPath = new ArrayList<Integer>();

        if (startVertex == NO_PARENT)
        {
            return partialPath;
        }
        partialPath.addAll(makePath(parents[startVertex], parents));
        partialPath.add(startVertex);
        return  partialPath;
    }

    public static Route defineRoute(int myVertex, int passengerVertex, int toVertex){
        Route firstRoute = shortestPath(myVertex,passengerVertex);
        Route secondRoute = shortestPath(passengerVertex,toVertex);

        int distance = firstRoute.getDistance() + secondRoute.getDistance();
        ArrayList<Integer> path = firstRoute.getPath();
        secondRoute.getPath().remove(0);
        path.addAll(secondRoute.getPath());
        return new Route(distance,path);
    }

}
