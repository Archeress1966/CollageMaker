/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package collagetester;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;


/**
 *
 * @author Archeress
 */
public class CollageMaker {
    static int SOURCE_COUNT = 200;
    static int TIME_LIMIT = 10000;
    /**
    public Comparator<Region> RegionSortByHistRank = new Comparator<Region> (){
        @Override
        public int compare(Region reg1, Region reg2) {
            if (reg1.width*reg1.height > reg2.width*reg2.height)
                return -1;
            else if (reg1.width*reg1.height < reg2.width*reg2.height)
               return 1;
            else if (reg1.histogramRank > reg2.histogramRank)
                return 1;
           else if (reg1.histogramRank < reg2.histogramRank)
               return -1;
            if (reg1.targetVar > reg2.targetVar)
                return -1;
            else if (reg1.targetVar < reg2.targetVar)
                return 1;
            else 
                return 0;
        }
    };
    ***/
    ArrayList<Image> srcImages = new ArrayList<>();
    ArrayList<Image> srcEdgeImages = new ArrayList<>();
    TreeMap<Integer, Image> srcStorage = new TreeMap<>();
    Image targetImage;
    Image targetEdge;
    boolean[] used = new boolean[200];
    int minDim = 300;
    int assignCount;
    void setup(int[] data){
        targetImage = new Image(-1, data[0],data[1], 2, data);
        targetImage = targetImage;
        Region.targetImage = targetImage;
        Region.srcList = srcImages;
        Region.srcStorage = srcStorage;
        int currentHeight=data[0];
        int currentWidth=data[1];
        int baseIndex = 2 + currentHeight*currentWidth;
        for (int i=0; i<200; i++){
            currentHeight = data[baseIndex++];
            if (currentHeight < minDim)
                minDim = currentHeight;
            currentWidth  = data[baseIndex++];
            if (currentWidth < minDim)
                minDim = currentWidth;
            Image newImage = new Image(i, currentHeight, currentWidth, baseIndex, data);
            srcImages.add(newImage);
            baseIndex += currentHeight*currentWidth;
            assignCount = 0;
        }
    }
    int[] compose(int[] data){
        long startTime = System.currentTimeMillis();
        int[] result = new int[800];
        Arrays.fill(result, -1);
        setup(data);
        PriorityQueue<Region> pq = new PriorityQueue<>(200, new Region.RegionSortByArea());
        PriorityQueue<Region> holdPQ = new PriorityQueue<>();
        pq.add(new Region(0,0, targetImage.height-1, targetImage.width-1));
       // Image edgeMap = targetImage.detectEdge().thresholdImage(130);
        while (!pq.isEmpty()){
            Region currentRegion = pq.remove();
            ArrayList<Region> children = currentRegion.subdivide();
            if (children.isEmpty() || (holdPQ.size()+ pq.size() + children.size() >= SOURCE_COUNT/4)) 
                holdPQ.add(currentRegion);
            else{
          
                
                if ((Math.sqrt(currentRegion.mValues[10]/(currentRegion.width*currentRegion.height)) > 20)){
                    pq.addAll(children);
                }
                else
      
                    if ((currentRegion.height < minDim) && (currentRegion.width <minDim))
                         holdPQ.add(currentRegion);
                    else
                         pq.addAll(children);
            }
                
        }
        System.err.printf("Debug: after region divide.. #regions = %d\n", holdPQ.size());
       
            pq.clear();
            pq.addAll(holdPQ);
         
         //State bestState = simulatedAnnealing(startTime, pq);   
         State bestState = bestFirstSearch(startTime, pq);
         result = bestState.solution;
         
        return result;
    }
    State bestFirstSearch(long startTime, PriorityQueue<Region> pq){
         State currentState = new State(pq, srcImages);
         State bestState = currentState;
         PriorityQueue<State> stateQueue = new PriorityQueue<>();
         stateQueue.add(currentState);
         System.err.printf("bestValue = %1.2f\n",bestState.value);
         while (!stateQueue.isEmpty() && ((System.currentTimeMillis() - startTime) < TIME_LIMIT)) {
            currentState = stateQueue.remove();
            System.err.printf("currentValue = %1.5f\n", currentState.value);
            if (bestState.value > currentState.value) {
                    bestState = currentState;
                    System.err.printf("---->bestValue = %1.5f\n", bestState.value);
                    bestState.setRegions();

                }
            if (!currentState.isGoal()) {
                ArrayList<State> children = currentState.getChildren(200);
                if (!children.isEmpty()) {
                    stateQueue.addAll(children);
                }
            }
        }
        return bestState;
    }
    State simulatedAnnealing(long startTime, PriorityQueue<Region> pq){
         Random gen = new Random();
         State currentState = new State(pq, srcImages);
         State bestState = currentState;
         long currentTime = System.currentTimeMillis();
         long stopTime = startTime + TIME_LIMIT;
         ArrayList<State> children = null;
         double tempMax = TIME_LIMIT;
         boolean generateNew = true;
         while (currentTime < stopTime){
             //if (currentState.isGoal()) {
                 if (bestState.value > currentState.value) {
                     bestState = currentState;
                     System.err.printf("---->bestValue = %1.5f\n", bestState.value);
                     bestState.setRegions();

                 }
            // }
             children = currentState.getChildren(200);
             if (children.isEmpty())
                 currentState.reset();
             else {
                 double temperature = (1.0 - (currentTime - startTime)/tempMax)*100;
               
                 if (temperature <=0) break;
                 int selectedIndex = gen.nextInt(children.size());
                 State next = children.get(gen.nextInt(children.size()));
                 System.err.printf("currentValue = %1.5f nextValue = %1.5f\n\n", currentState.value, next.value);
                
                 if (next.value < currentState.value){
                     currentState = next;
                     if (currentState.value < bestState.value)
                         bestState = currentState;
                 }
                 else {
                     double delta = currentState.value - next.value;
                     if (gen.nextDouble() > Math.exp(delta/temperature)){
                         currentState=next;
                         generateNew = true;
                     }
                     else generateNew = false;
                 }
                 
             }
             currentTime = System.currentTimeMillis();
         }
         
         return bestState;
    }
}
