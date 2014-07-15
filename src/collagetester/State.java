
package collagetester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 *
 * @author Archeress
 */
public class State implements Comparable<State> {

    TreeMap<Region, Image> region2Image;
    TreeMap<Region, Double> region2Value;
    TreeMap<Image, Region> image2Region;

    ArrayList<Region> stableRegions;
    ArrayList<Region> unstableRegions;
    int area;
    int[] solution;
    double value = 0;

    State(PriorityQueue<Region> regions, ArrayList<Image> src) {
        region2Image = new TreeMap<>();
        region2Value = new TreeMap<>();
        image2Region = new TreeMap<>();
        unstableRegions = new ArrayList<>();
        stableRegions = new ArrayList<>();
        PriorityQueue<Region> temp = new PriorityQueue<>();
        temp.addAll(regions);
        while (!temp.isEmpty()) {
            Region currentRegion = temp.remove();
            for (int i=0; i<CollageMaker.SOURCE_COUNT; i++){
                Image img = currentRegion.imageList.get(i);
                if (!image2Region.containsKey(img)){
                    region2Image.put(currentRegion, img);
                    region2Value.put(currentRegion, currentRegion.mValues[img.id]);
                    image2Region.put(img, currentRegion);
                    break;
                }
            }
            area += (currentRegion.width*currentRegion.height);
        }
   
        unstableRegions.addAll(regions);        
        value = evaluate();
        solution = setRegions();
    }//constructor

    State(State oldState) {
        region2Image = new TreeMap<>();
        region2Value = new TreeMap<>();
        image2Region = new TreeMap<>();
        unstableRegions = new ArrayList<>();
        stableRegions = new ArrayList<>();

        region2Image.putAll(oldState.region2Image);
        region2Value.putAll(oldState.region2Value);
        image2Region.putAll(oldState.image2Region);

        unstableRegions.addAll(oldState.unstableRegions);
        stableRegions.addAll(oldState.stableRegions);
        solution = new int[800];
        System.arraycopy(oldState.solution,0,solution,0,800);
        area = oldState.area;             
        value = oldState.value;
    }

    
    private double evaluate() {
        double sum = 0;
        double denom = 0;
        for (Double v : region2Value.values()) {
            sum += v;
        }
        
        return Math.sqrt(sum/area);
    }
    ArrayList<State> subdivide(Region currentRegion, Image currentImage){
        ArrayList<State> children = new ArrayList<>();
        State newState = null;
            if (unstableRegions.size()+stableRegions.size()+4 < CollageMaker.SOURCE_COUNT){                                
                ArrayList<Region> subList = currentRegion.subdivide();
                if (!subList.isEmpty()) {
                    newState = new State(this);
                    newState.unstableRegions.remove(currentRegion);
                    newState.region2Image.remove(currentRegion);
                    newState.region2Value.remove(currentRegion);
                    newState.image2Region.remove(currentImage);
                    boolean addState = true;
                    for (Region subReg : subList) {
                          Image subImg = null;
                          for (int i=0; i<subReg.imageList.size(); i++){
                             subImg = subReg.imageList.get(i);
                             if (!newState.image2Region.containsKey(subImg))
                                 break;
                          }//find an image for subregion
                          if (subImg == null){
                              addState = false; break;
                          }//how is this still null????
                          else {
                             newState.unstableRegions.add(subReg);
                             newState.region2Image.put(subReg, subImg);
                             newState.region2Value.put(subReg, subReg.mValues[subImg.id]);
                             newState.image2Region.put(subImg, subReg);
                          }//assign and run                
                    }//for each region
                    if (addState) children.add(newState);
                }
            }
            return children;
    }
    ArrayList<State> getChildren(int limit){
        ArrayList<State> childrenResult = new ArrayList<>();
        PriorityQueue<State> children = new PriorityQueue<>();
        for (Region currentRegion : unstableRegions){
            Image currentImage = region2Image.get(currentRegion);
            
            //subdivide region
            children.addAll(this.subdivide(currentRegion, currentImage));
            
            //keep region intact and determine children
            Image img=null;
            Region assignedRegion = null;
            int count = 0;
            for (int i = 0; i < currentRegion.imageList.size() && count < 20; i++) {
                img = currentRegion.imageList.get(i);
                
                State newState = new State(this);
                
                boolean addState = false;
                if (img.equals(currentImage)) {
                    addState = true;
                } else if (image2Region.containsKey(img)) {
                    assignedRegion = image2Region.get(img);
                    if (unstableRegions.contains(assignedRegion)) {
                        ArrayList<State> subList = this.subdivide(assignedRegion, img);
                        if (subList.isEmpty()) {
                            newState.region2Image.remove(currentRegion);
                            newState.region2Value.remove(currentRegion);
                            newState.image2Region.remove(currentImage);

                            newState.region2Image.remove(assignedRegion);
                            newState.region2Value.remove(assignedRegion);
                            newState.image2Region.remove(img);

                            newState.region2Image.put(currentRegion, img);
                            newState.region2Value.put(currentRegion, currentRegion.mValues[img.id]);
                            newState.image2Region.put(img, currentRegion);

                            newState.region2Image.put(assignedRegion, currentImage);
                            newState.region2Value.put(assignedRegion, assignedRegion.mValues[currentImage.id]);
                            newState.image2Region.put(currentImage, assignedRegion);

                            addState = true;
                        } else {
                            children.addAll(subList);
                        }
                        
                    }//if unstable is assigned to region.....swap
                }//if img is assigned
                else {
                    newState.region2Image.remove(currentRegion);
                    newState.region2Value.remove(currentRegion);
                    newState.image2Region.remove(currentImage);

                    newState.region2Image.put(currentRegion, img);
                    newState.region2Value.put(currentRegion, currentRegion.mValues[img.id]);
                    newState.image2Region.put(img, currentRegion);
                    addState = true;
                }//img has not been assigned
                if (addState) {
                    if(region2Value.get(currentRegion)/(currentRegion.width*currentRegion.height) < 100){
                        newState.unstableRegions.remove(currentRegion);
                        newState.stableRegions.add(currentRegion);
                    }
                    newState.value = newState.evaluate();
                    newState.solution = newState.setRegions();
                    children.add(newState);
                    count++;
                }
            }            
        }
        
        for (int i=0; i<limit&&!children.isEmpty(); i++)
            childrenResult.add(children.remove());
        
        return childrenResult;
    }
    ArrayList<State> getChildrenBak(int limit) {
        ArrayList<State> childrenResult = new ArrayList<>();
        PriorityQueue<State> children = new PriorityQueue<>();
        if (!unstableRegions.isEmpty()) {
            Region currentRegion = unstableRegions.get(0);       
            Image currentImage = region2Image.get(currentRegion);
       
            for (int i = 0; i < currentRegion.imageList.size(); i++) {
                Image img = currentRegion.imageList.get(i);
                State newState = new State(this);
                newState.stableRegions.add(newState.unstableRegions.remove(0));
                if (currentImage.equals(img)){                     
                     newState.solution = newState.setRegions();
                     children.add(newState);                    
                }
                else {
                    if (image2Region.containsKey(img)){  //img has been assigned
                        Region assignedRegion = image2Region.get(img);
                        if (!newState.stableRegions.contains(assignedRegion)){                                                        
                            newState.region2Image.remove(currentRegion);
                            newState.region2Value.remove(currentRegion);
                            newState.image2Region.remove(currentImage);
                            
                            newState.region2Image.remove(assignedRegion);
                            newState.region2Value.remove(assignedRegion);
                            newState.image2Region.remove(img);
                            
                            newState.region2Image.put(currentRegion, img);
                            newState.region2Value.put(currentRegion, currentRegion.mValues[img.id]);
                            newState.image2Region.put(img, currentRegion);

                            newState.region2Image.put(assignedRegion, currentImage);
                            newState.region2Value.put(assignedRegion, assignedRegion.mValues[currentImage.id]);
                            newState.image2Region.put(currentImage, assignedRegion);
                            newState.value = newState.evaluate();
                            
                            newState.solution = newState.setRegions();                            
                            children.add(newState);
                            
                        }
                    }
                    else {//img has not been assigned
                       
                        newState.region2Image.remove(currentRegion);
                        newState.region2Value.remove(currentRegion);
                        newState.image2Region.remove(currentImage);
                        
                        newState.region2Image.put(currentRegion, img);
                        newState.region2Value.put(currentRegion, currentRegion.mValues[img.id]);
                        newState.image2Region.put(img, currentRegion);
                                                                       
                        newState.value = newState.evaluate();
                        newState.solution = newState.setRegions();
                        
                        children.add(newState);                       
                        
                    }
                }
                
            }
            
        }
        
        for (int i=0; i<limit&&!children.isEmpty(); i++)
            childrenResult.add(children.remove());
        
        return childrenResult;
    }

    boolean isGoal() {
        return unstableRegions.isEmpty();
    }

    final int[] setRegions() {
        int[] result = new int [800];
        Arrays.fill(result, -1);
       
        for (Region currentRegion : region2Image.keySet()) {
            Image img = region2Image.get(currentRegion);
            int baseIndex = img.id * 4;
            result[baseIndex]   = currentRegion.topRow;
            result[baseIndex+1] = currentRegion.leftCol;
            result[baseIndex+2] = currentRegion.bottomRow;
            result[baseIndex+3] = currentRegion.rightCol;
            
        }
        
        return result;
    }
    void reset(){
        unstableRegions.addAll(stableRegions);
        stableRegions.clear();
    }

    @Override
    public int compareTo(State other) {
        if (value < other.value){
            return -1;
        }
        else if (value > other.value){
            return 1;
        }
        else if (region2Image.size() < other.region2Image.size()){
            return 1;
        } else if (region2Image.size() > other.region2Image.size()){
            return -1;
        }
        else 
        
            return 0;
    }
}
