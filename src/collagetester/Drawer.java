/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package collagetester;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;

class Drawer extends JFrame {

    class DrawerWindowListener extends WindowAdapter {

        public void windowClosing(WindowEvent event) {
            System.exit(0);
        }
    }

    class DrawerPanel extends JPanel {

        public void paint(Graphics g) {
            g.drawImage(originalImage, 66, 40, null);
            g.drawImage(filteredImage, 133 + originalImage.getWidth(), 40, null);
            g.drawImage(thirdImage, 66, Math.max(originalImage.getHeight(), filteredImage.getHeight())+50,null);
        }
    }

    DrawerPanel panel;
    int width, height;
    String myTitle;

    BufferedImage originalImage, filteredImage, thirdImage;

    final int EXTRA_HEIGHT = 150;
    final int EXTRA_WIDTH = 200;

    private void setPixels(BufferedImage image, int[][]pixels){
        for (int r=0; r<image.getHeight(); r++)
            for (int c=0; c<image.getWidth(); c++){
                int vt = pixels[r][c];
                image.setRGB(c, r, new Color(vt, vt, vt).getRGB());
            }                
    }
    
    public Drawer(String title, Image original, Image filtered, Image image3) {
        super();
        myTitle = title;
        this.originalImage = new BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB);
        setPixels(originalImage, original.pixels);
        this.filteredImage = new BufferedImage(filtered.width, filtered.height, BufferedImage.TYPE_INT_RGB);
        setPixels(filteredImage, filtered.pixels);
        if (image3 != null){
            thirdImage = new BufferedImage(image3.width, image3.height, BufferedImage.TYPE_INT_RGB);
            setPixels(thirdImage, image3.pixels);
        }

        panel = new DrawerPanel();
        getContentPane().add(panel);

        addWindowListener(new DrawerWindowListener());

        width = EXTRA_WIDTH + 2 * original.width;
        height = EXTRA_HEIGHT + 2 * original.height;

        setSize(width, height);
        setTitle(myTitle);

        setResizable(false);
        setVisible(true);
    }

    static public void main(String[] args) throws Exception {
        
        Image source = new Image("300px", "2.png");
        Image template = new Image("100px", "3.png");
        source = source.detectEdge();
        template = template.detectEdge();
        
        new Drawer("matching", source, template, source.correlate(template));

    }
}