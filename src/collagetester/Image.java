

package collagetester;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

class Image implements Comparable<Image>{
        static int nextCreateID = 0;
        int height, width;
        int[][] pixels;
        int id=-1;
        int createId = 0;
        
        private void init(int H, int W) {
            this.height = H;
            this.width = W;
            this.pixels = new int[H][W];
        }

        public Image(int H, int W) {
            init(H, W);
            createId = nextCreateID;
            nextCreateID++;
        }
        public Image(Image oldImage){
            height = oldImage.height;
            width = oldImage.width;
            id = oldImage.id;
            createId++;
            pixels = new int[height][width];
            for (int r=0; r<height; r++)
                System.arraycopy(oldImage.pixels[r], 0, pixels[r], 0, width);
       
        }
         public Image(String folder, String fileName) throws Exception {
            BufferedImage img;

            try {
                img = ImageIO.read(new File(folder, fileName));
            } catch (IOException e) {
                throw new Exception("Unable to read image from folder " + folder + " and file " + fileName + ".");
            }

            init(img.getHeight(), img.getWidth());

            int[] raster = img.getData().getPixels(0, 0, width, height, new int[height*width]);

            int pos = 0;
            for (int r=0; r < height; r++) {
                for (int c=0; c < width; c++) {
                    pixels[r][c] = raster[pos++];
                }
            }
            createId = nextCreateID;
            nextCreateID++;
        }
        
        int smooth1D(int pivot, int[] pixels){
            double[] gaussianFilter = {0.006, 0.061, 0.242, 0.383, 0.242,0.061, 0.006};
            double sum=0;
            int mid = gaussianFilter.length/2;
            int i= pivot-mid, j=0;
            if (i < 0){
                i=0; 
                j = mid-pivot;
            }
            while ((j<gaussianFilter.length) && (i<pixels.length)){
                sum += pixels[i]*gaussianFilter[j];
                j++;
                i++;
            }
            return (int)sum;
        }
        Image apply2DFilter(int[][] filter){
            Image result = new Image(height, width);
            int rMid = filter.length /2;
            int cMid = filter[0].length/2;
            
            for (int row=0; row<height; row++)
                for (int col=0; col<width; col++){
                    int rstart= row - rMid;                    
                    int frStart = 0;
                    if (rstart < 0){
                        rstart = 0; frStart = rMid - row;
                    }
                    int cstart= col - rMid;
                    int fcStart = 0;
                    if (cstart < 0){
                        cstart = 0; fcStart = cMid - col;
                    }
                    int sum = 0;
                    for (int r=rstart, fr=frStart; r<height&&fr<filter.length; r++, fr++)
                        for (int c=cstart, fc=fcStart; c<width&&fc<filter[0].length; c++, fc++)
                            sum += filter[fr][fc]*pixels[r][c];
                    
                                       
                    result.pixels[row][col] = (int)Math.sqrt(sum*sum) % 256;
                }
            
            return result;
        }
        Image gaussianSmooth(){
            
            Image result1 = new Image(height, width);
            Image result = new Image(height, width);
            for (int r=0; r<height; r++)
                for (int c=0; c<width; c++){
                    result1.pixels[r][c] = smooth1D(c,pixels[r]);
                }
             for (int c=0; c<width; c++){
                int[] colVec = new int[height];
                for (int r=0; r<height; r++)
                    colVec[r] = result1.pixels[r][c];
                for (int r=0; r<height; r++)
                    result.pixels[r][c] = smooth1D(r,colVec);
             }    
            return result;
        }
        static Image add(Image image1, Image image2){
            if ((image1.height == image2.height) && (image1.width == image2.width)){
                Image result = new Image(image1.height, image2.width);
                for (int r=0; r<image1.height; r++)
                    for (int c=0; c<image1.width; c++){
                       int value = (int)Math.sqrt(image1.pixels[r][c]*image1.pixels[r][c]+
                                                  image2.pixels[r][c]*image2.pixels[r][c]);
                       if (value > 255)
                           value = 255;
                       result.pixels[r][c] = value;
                    }
                return result;
            }
            else 
                return null;
        }
        Image thresholdImage(int threshold){
            Image result = new Image(height, width);
            for (int r=0; r<height; r++)
                for (int c=0; c<width; c++)
                    if (pixels[r][c] > threshold)
                        result.pixels[r][c] = 255;
                    else 
                        result.pixels[r][c] = 0;
            
            return result;
        }
        Image correlate(Image template){
            Image result = new Image (height, width);            
            int endRow = height - template.height;
            int endCol = width - template.width;
            int rMid = template.height/2;
            int cMid = template.width/2;
            
           
            int min = 256;
            int max = -1;
            for (int row=0; row<endRow; row++){ 
                for (int col=0; col<endCol; col++){
                    int sum = 0; 
                    int denom = 0;
                    for (int r=0; r<template.height; r++){
                        for (int c=0; c<template.width; c++){
                            int dz = pixels[row+r][col+c]-template.getPixel(r, c);
                            sum += dz*dz;
                            dz = pixels[row+r][col+c];
                            denom += dz*dz;
                        }
                    }
                    
                    double value = 1-(sum+1)/(denom+1);
                    if (value < 0) value = 0;
                    int value2 = (int)(255*value);
                    result.pixels[row+rMid][col+cMid] = value2;
                    if (value2 > max)
                        max = value2;
                    if (value2 < min)
                        min = value2;
                }
            }
            double delta = max-min+1;
            for (int row=0; row<endRow; row++){ 
                for (int col=0; col<endCol; col++){
                    int value = result.pixels[row+rMid][col+cMid];
                    
                    result.pixels[row+rMid][col+cMid] = (int)(255*(value-min+1)/delta);
                }
            }
                    
            return result;
            
        }
        Image detectEdge(){
            int[][] vertical = {{-1, 0, 1}, {-1, 0, 1}, {-1, 0, 1}};
            int[][] horizontal = {{-1, -1, -1}, {0, 0, 0}, {1, 1, 1}};
            Image smoothImage = this.gaussianSmooth();
            Image hImage = smoothImage.apply2DFilter(horizontal);
            Image vImage = smoothImage.apply2DFilter(vertical);
            Image addImage = add(hImage, vImage);
            Image result = new Image(this);
            for (int r=0; r<height; r++)
                for (int c=0; c<width; c++)
                    result.pixels[r][c] = addImage.pixels[r][c];
            
            return result;
        }
       
        Image (int id, int height, int width, int baseIndex, int[] data){
            this.id = id;
            this.height = height;
            this.width = width;
            this.pixels = new int[height][width];
           
            int index = baseIndex;
            for (int r=0; r<height; r++)
                for (int c=0; c<width; c++){
                    pixels[r][c] = data[index++];                    
                }          
            
            createId = nextCreateID;
            nextCreateID++;
        }
        
        int getPixel(int row, int col){
            return pixels[row][col];
        }//getPixel
        
        void printSelf(List<Integer> lst) {
            lst.add(height);
            lst.add(width);
            for (int r=0; r < height; r++) {
                for (int c=0; c < width; c++) {
                    lst.add(pixels[r][c]);
                }
            }
        }

        int intersect(int a, int b, int c, int d) {
            int from = Math.max(a, c);
            int to = Math.min(b, d);
            return from < to ? to - from : 0;
        }

        Image scale(int newH, int newW) {
            List<Integer> origRList = new ArrayList<Integer>();
            List<Integer> newRList = new ArrayList<Integer>();
            List<Integer> intrRList = new ArrayList<Integer>();
            List<Integer> origCList = new ArrayList<Integer>();
            List<Integer> newCList = new ArrayList<Integer>();
            List<Integer> intrCList = new ArrayList<Integer>();

            for (int origR = 0; origR < height; origR++) {
                int r1 = origR * newH, r2 = r1 + newH;
                for (int newR = 0; newR < newH; newR++) {
                    int r3 = newR * height, r4 = r3 + height;
                    int intr = intersect(r1, r2, r3, r4);
                    if (intr > 0) {
                        origRList.add(origR);
                        newRList.add(newR);
                        intrRList.add(intr);
                    }
                }
            }

            for (int origC = 0; origC < width; origC++) {
                int c1 = origC * newW, c2 = c1 + newW;
                for (int newC = 0; newC < newW; newC++) {
                    int c3 = newC * width, c4 = c3 + width;
                    int intr = intersect(c1, c2, c3, c4);
                    if (intr > 0) {
                        origCList.add(origC);
                        newCList.add(newC);
                        intrCList.add(intr);
                    }
                }
            }

            Image res = new Image(newH, newW);

            for (int i = 0; i < origRList.size(); i++) {
                int origR = origRList.get(i);
                int newR = newRList.get(i);
                int intrR = intrRList.get(i);

                for (int j = 0; j < origCList.size(); j++) {
                    int origC = origCList.get(j);
                    int newC = newCList.get(j);
                    int intrC = intrCList.get(j);

                    res.pixels[newR][newC] += intrR * intrC * pixels[origR][origC];
                }
            }

            for (int r = 0; r < newH; r++) {
                for (int c = 0; c < newW; c++) {
                    res.pixels[r][c] = (2 * res.pixels[r][c] + height * width) / (2 * height * width);
                }
            }

            return res;
        }
        void setId(int num){
            id = num;
        }
        
        @Override
        public boolean equals(Object obj){
            Image other = (Image)obj;
            return id == other.id;
        }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.id;
        return hash;
    }
        
        @Override
        public int compareTo(Image other){
            return other.id - id;
        }
    }