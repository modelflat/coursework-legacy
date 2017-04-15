package com.github.modelflat.coursework2;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created on 12.04.2017.
 */
public class BoxCountingCalculator {

    public static final int DEFAULT_MIN_BOX_SIZE = 5;

    private Image image;
    private int startBoxSize;
    private int endBoxSize;

    /**
     * Initializes BoxCountingCalculator with automatically calculated start & end box sizes;
     * @param image image to calculate Hausdorff dimension on
     */
    public BoxCountingCalculator(Image image) {
        this(image, (int) Math.min(DEFAULT_MIN_BOX_SIZE, Math.max(image.getWidth(), image.getHeight())),
                (int) Math.max(image.getHeight(), image.getWidth()) / DEFAULT_MIN_BOX_SIZE);
    }

    public BoxCountingCalculator(Image image, int maxSize) {
        this(image, DEFAULT_MIN_BOX_SIZE, maxSize);
    }

    public BoxCountingCalculator(Image image, int minSize, int maxSize) {
        this.image = image;
        this.startBoxSize= minSize;
        this.endBoxSize = maxSize;
    }

    public double calculate(Function<Color, Boolean> colorFunction) {
        long time  = System.nanoTime();
        double[][] boxes = calculateBoxes(colorFunction);
        System.out.println("calculateBoxes: " + (System.nanoTime() - time) / 1000);
        time = System.nanoTime();
        double[] theta = normalEquations2d(boxes[0], boxes[1]);
        System.out.println("normalEquations2d: " + (System.nanoTime() - time) / 1000);
        return theta[0]; // dim
    }

    private double[][] calculateBoxes(Function<Color, Boolean> colorFunction) {
        // PixelFormat<IntBuffer> pf = PixelFormat.getIntArgbInstance();
        PixelReader reader = image.getPixelReader();
        double[][] boxes = new double[2][endBoxSize - startBoxSize];
        // for all box sizes from 2 to maxXSize
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        ByteBuffer boxBuffer = null;
        for (int k = startBoxSize, bI = 0; k < endBoxSize; k++, bI++) {
            int sizeY = (h / k) + (h % k == 0 ? 0 : 1);
            int sizeX = (w / k) + (w % k == 0 ? 0 : 1);
            int totalBoxes = sizeX * sizeY;
            if (boxBuffer == null) {
                boxBuffer = ByteBuffer.allocate(totalBoxes);
            } else {
                // clear buffer
                for (int i = 0; i < totalBoxes; i++) {
                    //   activeBoxes += boxBuffer.get(i);
                    boxBuffer.put((byte) 0);
                }
                boxBuffer.flip();
            }
            int activeBoxes = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int result = colorFunction.apply(reader.getColor(x, y)) ? 1 : 0;
                    int idx = (y / k)*sizeX + (x / k);
                    byte prev = boxBuffer.get(idx);
                    if (result > prev) {
                        activeBoxes++;
                        boxBuffer.put(idx, (byte) (result | prev));
                    }
                }
            }
            //System.out.println(String.format("%d / %d", activeBoxes, totalBoxes));
            boxes[0][bI] = Math.log(1.0 / k);
            boxes[1][bI] = Math.log(activeBoxes);
            // System.out.println(String.format("[%d] (%d) %f %f", bI, k, Math.log(1.0 / k), Math.log(activeBoxes)));
        }
        return boxes;
    }

    /**
     * not optimized and generally bad, but takes <0.2ms to compute. compared to calculateBoxes it's perfectly
     * fine.
     * @param x
     * @param y
     * @return
     */
    private static double[] normalEquations2d(double[] x, double[] y) {
        //x^t * x
        double[][] xtx = new double[2][2];
        for (double aX : x) {
            xtx[0][1] += aX;
            xtx[0][0] += aX * aX;
        }
        xtx[1][0] = xtx[0][1];
        xtx[1][1] = x.length;

        //inverse
        double[][] xtxInv = new double[2][2];
        double d = 1 / (xtx[0][0]*xtx[1][1] - xtx[1][0]*xtx[0][1]);
        xtxInv[0][0] = xtx[1][1]*d;
        xtxInv[0][1] = -xtx[0][1]*d;
        xtxInv[1][0] = -xtx[1][0]*d;
        xtxInv[1][1] = xtx[0][0]*d;

        //times x^t
        double[][] xtxInvxt = new double[2][x.length];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < x.length; j++) {
                xtxInvxt[i][j] = xtxInv[i][0]*x[j] + xtxInv[i][1];
            }
        }

        //times y
        double[] theta = new double[2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < x.length; j++) {
                theta[i] += xtxInvxt[i][j]*y[j];
            }
        }

        return theta;
    }

}
