/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package collagetester;

import java.util.Scanner;

/**
 *
 * @author Archeress
 */
public class CollageTester {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);
        int inputSize = keyboard.nextInt();
        int[] data = new int[inputSize];
        for (int i = 0; i < inputSize; i++) {
            data[i] = keyboard.nextInt();
        }
        System.err.println("Debug: Received data");
        CollageMaker tester = new CollageMaker();
        int[] ret = tester.compose(data);

        for (int i = 0; i < 800; i++) {
            System.out.println(ret[i]);
        }
    }

}
