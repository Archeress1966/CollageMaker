/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package collagetester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

class Region implements Comparable<Region> {

    static Image targetImage;
    static ArrayList<Image> srcList;
    static TreeMap<Integer, Image> srcStorage;
    int topRow;
    int leftCol;
    int bottomRow;
    int rightCol;
    int srcImageId; //-1 means not assigned
    int height, width;
    double matchValue;
    ArrayList<Image> imageList;
    double[] mValues;
    int id;
    double edgeValue = 0.0;

    Region(int topRow, int leftCol, int bottomRow, int rightCol) {
        this.topRow = topRow;
        this.leftCol = leftCol;
        this.bottomRow = bottomRow;
        this.rightCol = rightCol;
        srcImageId = -1;
        width = rightCol - leftCol + 1;
        height = bottomRow - topRow + 1;
        imageList = new ArrayList<>();
        imageList.addAll(srcList);
        if (imageList.size() < 200){
            System.err.println("DEBUG: problem with imageList");
            System.exit(-100);
        }
        orderImage();
                      
    }

    /**
     * private double getAverage(){ double sum = 0; for (int r= topRow;
     * r<=bottomRow; r++) for (int c=leftCol; c<=rightCol; c++){ sum +=
     * targetImage.getPixel(r,c); } return sum / (width*height); }
     *
     * private double getVariance(){ double sum = 0; for (int r= topRow;
     * r<=bottomRow; r++) for (int c=leftCol; c<=rightCol; c++){ double delta =
     * targetImage.getPixel(r,c) - targetAvg; sum += delta * delta; } return sum
     * / (width*height); }
   **
     */
    void setImage(int id, double value) {
        srcImageId = id;
        matchValue = value;
    }

    void clearImage() {
        if (srcImageId > -1) {
            srcImageId = -1;
            matchValue = Double.MIN_VALUE;
        }

    }

    double matchSrc(Image src, TreeMap<Integer, Image> srcStorage) {

        if ((src.height < this.height) || (src.width < this.width)) {
            return Double.MAX_VALUE; //can not upscale images
        }
        if ((src.height != this.height) || (src.width != this.width)) {
            int key = src.id * 1000000 + this.height * 1000 + this.width;
            if (srcStorage.containsKey(key)) {
                src = srcStorage.get(key);
            } else {
                src = src.scale(this.height, this.width);
                srcStorage.put(key, src);
            }
        }
        double sum = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                double dz = targetImage.getPixel(topRow + r, leftCol + c) - src.getPixel(r, c);
                sum += dz * dz;
            }
        }
        return sum;
    }

    ArrayList<Region> subdivide() {
        ArrayList<Region> result = new ArrayList<>();
        if ((width > 8) && (height > 8)) {
            int halfRow = topRow + height / 2;
            int halfCol = leftCol + width / 2;

            if ((width > height) || (width == height)) { //vertical split
                result.add(new Region(topRow, leftCol, bottomRow, halfCol - 1));
                result.add(new Region(topRow, halfCol, bottomRow, rightCol));
            } else if ((width < height) || (width == height)) {//horiztonal split
                result.add(new Region(topRow, leftCol, halfRow - 1, rightCol));
                result.add(new Region(halfRow, leftCol, bottomRow, rightCol));
            } else {

                result.add(new Region(topRow, leftCol, halfRow - 1, halfCol - 1));
                result.add(new Region(topRow, halfCol, halfRow - 1, rightCol));
                result.add(new Region(halfRow, leftCol, bottomRow, halfCol - 1));
                result.add(new Region(halfRow, halfCol, bottomRow, rightCol));
            }
        }
        return result;

    }//subdivide region into fours

    double edgePixelPercent(Image edgeMap) {
        double result;
        int count = 0;
        for (int r = topRow; r <= bottomRow; r++) {
            for (int c = leftCol; c < rightCol; c++) {
                if (edgeMap.pixels[r][c] > 0) {
                    count++;
                }
            }
        }
        result = (double) count / (width * height);
        edgeValue = 1.0 - Math.abs(0.5-result);
        return result;
    }

    @Override
    public int compareTo(Region other) {
        if (edgeValue > other.edgeValue)
            return 1;
        else if (edgeValue < other.edgeValue)
            return -1;
        
        else {
            if (topRow > other.topRow)
                return 1;
            else if (topRow < other.topRow)
                return -1;
            if (bottomRow > other.bottomRow)
                return 1;
            else if (bottomRow < other.bottomRow)
                return -1;
            else if (leftCol > other.leftCol)
                return 1;
            else if (leftCol < other.leftCol)
                return -1;
            else if (rightCol > other.rightCol)
                return 1;
            else if (rightCol < other.rightCol)
                return -1;
            else 
                return 0;
        }
    }

    void setId(int num) {
        id = num;
    }

    private void orderImage() {
        mValues = new double[200];
        for (int i = 0; i < 200; i++) {
            mValues[i] = this.matchSrc(srcList.get(i), srcStorage);
        }
        Collections.sort(imageList, new ImageSort(mValues));
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d, %d) matchValue = %1.2f", topRow, leftCol, bottomRow, rightCol, matchValue);
    }

    
    static class RegionSortByArea implements Comparator<Region> {
        
        @Override
        public int compare(Region r1, Region r2) {
             if ((r1.width * r1.height) > (r2.width*r2.height))
                 return -1;
             else if (r1.width*r1.height < r2.width*r2.height)
                 return 1;
             else 
                 return 0;
            
                
        }
    };
    public class ImageSort implements Comparator<Image> {
        double[] mValue;
        ImageSort(double[] value){
            mValue = value;
        }
        @Override
        public int compare(Image img1, Image img2) {
            if (mValue[img1.id] < mValue[img2.id])
                return -1;
            else if (mValue[img1.id] > mValue[img2.id])
                return 1;
            else 
                return 0;
                
        }
    };
    @Override
    public boolean equals(Object obj){
        Region other = (Region)obj;
        
        return (topRow == other.topRow) && (leftCol == other.leftCol) &&
               (bottomRow == other.bottomRow) && (rightCol == other.rightCol);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.topRow;
        hash = 97 * hash + this.leftCol;
        hash = 97 * hash + this.bottomRow;
        hash = 97 * hash + this.rightCol;
        return hash;
    }
}//Region

