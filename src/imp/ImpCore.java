/*
 * ImpCore.java  Created on February 16, 2009, 2:24 PM
 * By: Rodrigo González del Cueto
 * 
 */

package imp;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageOp;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Random;
import org.im4java.core.*;              // ImageMagick API
import edu.wlu.cs.levy.CG.*;            // KD-Tree
import java.util.Calendar;

/**
 * This class is a container for a ARGB image encoded into a planar array
 * of 32-bit signed integers.
 * @author RdelCueto
 */
class ARGBitMap {
    /** Image Dimensions */
    public int w, h;
    /** Planar image array */
    public int bitmap[];

    /**
     * ARGBBitmap Constructor.
     * @param bitmap Planar array of signed integers with a (w*h) length
     * containing the ARGB image.
     * @param w Image width.
     * @param h Image length.
     */
    public ARGBitMap(int bitmap[], int w, int h) {
        this.bitmap = new int[bitmap.length];
        System.arraycopy(bitmap, 0, this.bitmap, 0, bitmap.length);
        this.w = w;
        this.h = h;
    }
}

/**
 * This class is models a layer inside IMP.
 * A layer contains:
 *   Two linked-lists (undo, redo) which resamble the state of a
 * layer before and after each modification, each with a size limit of 8.
 *
 *   A 2D coordinate that resambes the location inside the canvas.
 *   An opacy value which indicates the fill amount in the canvas image.
 *   A visible flag which states if the layer should or should't affect the
 * canvas image.
 *   A blendType flag, which indicates how the layer should affect the canvas
 * image.
 *
 * @author RdelCueto
 */
class Layer {

    /** Layer undo history limit */
    public static final int UNDO_LIMIT = 8;
    /** Normal blending mode.*/
    public static final int BLEND_NORMAL = 0;
    /** Additive blending mode.*/
    public static final int BLEND_ADD = 1;
    /** Substractive blending mode.*/
    public static final int BLEND_SUBSTRACT = 2;
    /** Multiplying blending mode.*/
    public static final int BLEND_MULTIPLY = 3;
    /** Dividing blending mode.*/
    public static final int BLEND_DIVIDE = 4;

    /** Location relative to the x-axis inside the canvas.*/
    public int canvasXCoord;
    /** Location relative to the y-axis inside the canvas.*/
    public int canvasYCoord;
    /** Layer's blend mode. */
    int blendType = 0;
    /** Layer's opacy level. */
    public float opacy;
    /** Layer's visibility state. */
    public boolean visible;
    /** Layer's undo available states. */
    private LinkedList<ARGBitMap> undo;
    /** Layer's redo available states. */
    private LinkedList<ARGBitMap> redo;

    /**
     * Layer Constructor (Empty).
     * @param x The x-axis coordinate inside the canvas.
     * @param y The y-axis coordinate inside the canvas.
     */
    public Layer(int x, int y) {
        undo = new LinkedList<ARGBitMap>();
        redo = new LinkedList<ARGBitMap>();
        canvasXCoord = x;
        canvasYCoord = y;
        opacy = 1.0f;
        visible = true;
    }

    /**
     * Layer Constructor (w/ARGBitmap).
     * @param img ARGBitmap image object containing the planar int array image.
     * @param x The x-axis coordinate inside the canvas.
     * @param y The y-axis coordinate inside the canvas.
     */
    public Layer(ARGBitMap img, int x, int y) {
        undo = new LinkedList<ARGBitMap>();
        redo = new LinkedList<ARGBitMap>();
        undo.addFirst(img);
        canvasXCoord = x;
        canvasYCoord = y;
        opacy = 1.0f;
        visible = true;
    }

    /**
     * Returns the width of a layer.
     * @return pixel width
     */
    public int getWidth() {
        return undo.getFirst().w;
    }

    /**
     * Returns the heigth of a layer.
     * @return pixel heigth
     */
    public int getHeight() {
        return undo.getFirst().h;
    }

    /**
     * This method modifies the location of a layer relative to the canvas.
     * @param x New x-axis coordinate.
     * @param y New y-axis coordinate.
     */
    public void move(int x, int y) {
        canvasXCoord = x;
        canvasYCoord = y;
    }

    /**
     * This method returns a copy of the layer's current state image.
     * @return ARGBitmap planar image.
     */
    public ARGBitMap duplicate() {
        ARGBitMap original = undo.getFirst();
        ARGBitMap copy = new ARGBitMap(original.bitmap, original.w, original.h);
        return copy;
    }

    /**
     * This method adds a new state to the layer.
     * @param updatedImage
     */
    public void update(ARGBitMap updatedImage) {
        redo.clear();
        if(undo.size() == UNDO_LIMIT)
            undo.removeLast();
        undo.addFirst(updatedImage);
    }

    /**
     * This method discards an specific state of the layer's modifications.
     * @param n The index of the n-th modification to be discarded.
     */
    public void discard(int n) {
        undo.remove(n);
    }

    /**
     * This method tells the layer to go back to the previous state.
     * The current state is stored inside the redo linked-list.
     * @return The number of available undo states after the call.
     */
    public int undo() {
        redo.addFirst(undo.pop());
        return undo.size() - 1;
    }

    /**
     * This method tells the layer to restore the modified state.
     * The current state is removed from the undo linked-list
     * and placed back into the undo linked-list.
     * @return The number of available redo states after the call.
     */
    public int redo() {
        undo.addFirst(redo.pop());
        return redo.size() - 1;
    }

    /**
     * This method returns the number of undo states available.
     * @return current undo states
     */
    public int undos() {
        return undo.size() - 1;
    }

    /**
     * This method returns the number of undo states available.
     * @return current undo states
     */
    public int redos() {
        return redo.size() - 1;
    }

    /**
     * Returns the current ARGBitmap of the layer.
     * @return ARGBitmap object with the current layer's state.
     */
    public ARGBitMap getLayerImg() {
        return undo.getFirst();
    }

    /**
     * Returns a cropped instance of the current ARGBitmap of the layer.
     * @param startX Start relative point within the canvas in the x-axis.
     * @param startY Start relative point within the canvas in the y-axis.
     * @param endX End relative point within the canvas in the x-axis.
     * @param endY End relative point within the canvas in the y-axis.
     * @param canvas_Width The width of the canvas.
     * @param canvas_Height The height of the canvas.
     * @return The cropped ARGBitmap instance.
     */
    public ARGBitMap getLayerImg(
        int startX, int startY,
        int endX, int endY,
        int canvas_Width, int canvas_Height) {

        int width = startX - endX;
        int height = startY - endY;
        ARGBitMap layer = undo.getFirst();
        int layerBitmap[] = layer.bitmap;
        int w = layer.w;
        int croppedLayer[] = new int[canvas_Width*canvas_Height];
        int x = canvasXCoord;
        int y = canvasYCoord;
        for(int i = startY; i < endY; i++) {
            for(int j = startX; j < endX; j++) {
                croppedLayer[y*canvas_Width + x] =
                        layerBitmap[i*w + j];
                x++;
            }
            x = canvasXCoord;
            y++;
        }
        return new ARGBitMap(croppedLayer, canvas_Width, canvas_Height);
    }
}

/**
 * This class models an image in the RGB 3D space. The coordinates of the image
 * are the average value of all the pixels in it.
 * @author RdelCueto
 */
class ImageNode implements Serializable {
    /** The image's average RGB values. */
    double rgbAvg[];
    /** Relative path to the image's file. */
    String imageFile;

    /**
     * ImageNode Constructor.
     * @param rgb an array of doubles representing the 3D coordinates of the
     * image.
     * @param filePath The image's relative path.
     */
    ImageNode(double rgb[], String filePath) {
        rgbAvg = new double[3];
        rgbAvg[0] = rgb[0];
        rgbAvg[1] = rgb[1];
        rgbAvg[2] = rgb[2];
        imageFile = filePath;
    }
}

/**
 * This is class is IMP's main class, every filter/function inside is
 * implemented within this class.
 * @author RdelCueto
 */
public class ImpCore {

    /** Logic mask to extract the least significant byte from an integer. */
    static final int byteMask = 0xff;
    /** Stores the number of cores available to the JVM. */
    int nCores;
    /** Keeps track of the currently selected layer, when using layer based
     * functions/filters. */
    int selectedLayer;
    /** Array with the current canvas rendering. */
    int canvas_ARGB[];
    /** The area of the canvas in pixels. Length of canvas_ARGB. */
    int canvasRes;
    /** The width of the canvas. */
    int canvasWidth;
    /** The height of the canvas. */
    int canvasHeight;
    /** List containing all the layers in the image.*/
    private ArrayList<Layer> layers;
    /** List containing the average RGB values and File pointer, to any
     * pre-processed image. (Image Mosaic)
     */
    private LinkedList<ImageNode> nodes;
    private int tilesW, tilesH;
    Random rnd = new Random();

    /**
     * Empty Constructor.
     */
    public ImpCore() {
        nCores = Runtime.getRuntime().availableProcessors();
    }

    /**
     * This method initializes IMP's core, given the canvas dimensions.
     * @param width The canvas width.
     * @param height The canvas height.
     */
    public void initialize(int width, int height) {
        if(layers != null) layers.clear();
        canvas_ARGB = null;
        layers = new ArrayList<Layer>();
//        layerUndoOrder = new ArrayList<Integer>();
        canvasWidth = width;
        canvasHeight = height;
        canvasRes = canvasWidth * canvasHeight;
        canvas_ARGB = new int[canvasHeight*canvasWidth];
    }



     //----------------------- //
    //   Canvas Functions     //
   //----------------------- //



    /**
     * This method modifies the canvas dimensions.
     * @param width The new canvas width.
     * @param height The new canvas height.
     */
    public void resizeCanvas(int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
        canvasRes = canvasWidth * canvasHeight;
    }

    /**
     * This method returns the current canvas image in a BufferedImage with
     * INT_RGB format. This method is used when saving images in formats without
     * transparency/alpha support.
     * @return A BufferedImage instance with the canvas image.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory to
     * store the new canvas instance.
     */
    public BufferedImage getCanvasNoAlpha() throws OutOfMemoryError {
        BufferedImage img = new BufferedImage(
                    canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, canvasWidth, canvasHeight, killAlpha(),
                0, canvasWidth);
        return img;
    }

    /**
     * This method returns the current canvas image in a BufferedImage with
     * INT_ARGB format.
     * @return BufferedImage instance with the canvas image.
     */
    public BufferedImage getCanvas() {
        BufferedImage img = new BufferedImage(
                    canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, canvasWidth, canvasHeight, canvas_ARGB,
                0, canvasWidth);
        return img;
    }

    /**
     * This method calls a redraw of the current canvas image and returns a
     * BufferedImage with INT_ARGB format.
     * @return BufferedImage instance with the canvas image.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public BufferedImage updateCanvas() throws OutOfMemoryError {
        BufferedImage img = new BufferedImage(
                    canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, canvasWidth, canvasHeight, drawCanvas(-1),
                0, canvasWidth);
        return img;
    }

    /**
     * This method strips the alpha channel from the canvas image, and returns
     * the resulting array.
     * @return The image planar array of the canvas without alpha channel.
     */
    public int[] killAlpha() {
        for(int i = 0; i < canvasRes; i++)
            canvas_ARGB[i] = canvas_ARGB[i] & 0x00FFFFFF;
        return canvas_ARGB;
    }

    /**
     * This method renders the canvas image by blending the specified layers
     * together.
     * @return The planar array image of the rendered canvas.
     * @param nLayers The number of layers to flatten, starting from the
     * selectedLayer. A value of -1 implies rendering all of the layers.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public int[] drawCanvas(int nLayers) throws OutOfMemoryError {

        if (nLayers != -1) {
            nLayers = selectedLayer + nLayers;
        }
        else {
            selectedLayer = 0;
            nLayers = getNumberofLayers();
        }
        float layerOpacy, opacy;
        float F[] = new float[canvasRes];
        for(int i = 0; i < canvasRes; i++)
            F[i] = 0.0f;
        int A[] = new int[canvasRes];
        int R[] = new int[canvasRes];
        int G[] = new int[canvasRes];
        int B[] = new int[canvasRes];

        float r, g, b;
        float cR[] = new float[canvasRes];
        float cG[] = new float[canvasRes];
        float cB[] = new float[canvasRes];

        for(int i = 0; i < canvasRes; i++) {
            cR[i] = cG[i] = cB[i] = 1.0f;
        }

        int k, l;
        int bType = 0;
        for(int n = selectedLayer; n < nLayers; n++) {
            layerOpacy = layers.get(n).opacy;
            if(layers.get(n).visible && layerOpacy != 0.0) {
                int layer[] = drawLayer(layers.get(n));
                bType = layers.get(n).blendType;
                for(int i = 0; i < canvasHeight; i++) {
                    for(int j = 0; j < canvasWidth; j++) {
                        l = k = i*canvasWidth + j;
                        if(F[k] < 1.0) {
                            if(bType == Layer.BLEND_ADD) {
                                opacy = (((layer[l] >> 24) & byteMask)*(1-F[k])*
                                        (layerOpacy))/255.0f;
//                                A[k] += (int)(((layer[l] >> 24) & byteMask)*
//                                        (layerOpacy));

                                R[k] += (int)(((layer[l] >> 16) & byteMask)*
                                        opacy);
                                G[k] += (int)(((layer[l] >> 8)  & byteMask)*
                                        opacy);
                                B[k] += (int)((layer[l]  & byteMask)*
                                        opacy);
                            }
                            else if(bType == Layer.BLEND_SUBSTRACT) {
                                opacy = (((layer[l] >> 24) & byteMask)*(1-F[k])*
                                        (layerOpacy))/255.0f;
//                                A[k] -= (int)(((layer[l] >> 24) & byteMask)*
//                                        (layerOpacy));

                                R[k] -= (int)(((layer[l] >> 16) & byteMask)*
                                        opacy);
                                G[k] -= (int)(((layer[l] >> 8)  & byteMask)*
                                        opacy);
                                B[k] -= (int)((layer[l]  & byteMask)*
                                        opacy);
                            }
                            else if(bType == Layer.BLEND_MULTIPLY) {
                                opacy = (((layer[l] >> 24) & byteMask)*(1-F[k])*
                                        (layerOpacy))/255.0f;
//                                A[k] *= (int)(((layer[l] >> 24) & byteMask)*
//                                        (layerOpacy));

                                r = ((layer[l] >> 16) & byteMask)/255.0f;
                                cR[k] *= (r*opacy + (1 - opacy));
                                g = ((layer[l] >> 8)  & byteMask)/255.0f;
                                cG[k] *= (g*opacy + (1 - opacy));
                                b = (layer[l]  & byteMask)/255.0f;
                                cB[k] *= (b*opacy + (1 - opacy));
                            }
                            else if(bType == Layer.BLEND_DIVIDE) {
                                opacy = (((layer[l] >> 24) & byteMask)*(1-F[k])*
                                        (-layerOpacy + 1.0f))/255.0f;
//                                A[k] /= (int)(((layer[l] >> 24) & byteMask)*
//                                        (layerOpacy));

                                r = ((layer[l] >> 16) & byteMask)/255.0f;
                                cR[k] *= (r*opacy + (1 - opacy));
                                g = ((layer[l] >> 8)  & byteMask)/255.0f;
                                cG[k] *= (g*opacy + (1 - opacy));
                                b = (layer[l]  & byteMask)/255.0f;
                                cB[k] *= (b*opacy + (1 - opacy));

                                if(r != 0)
                                    cR[k] /= r;
                                else
                                    cR[k] /= 0.000976563d;

                                if(g != 0)
                                    cG[k] /= g;
                                else
                                    cG[k] /= 0.000976563d;

                                if(b != 0)
                                    cB[k] /= b;
                                else
                                    cB[k] /= 0.000976563d;
                            }
                            else { // BLEND_NORMAL
                                opacy = (((layer[l] >> 24) & byteMask)*(1-F[k])*
                                        (layerOpacy))/255.0f;
                                A[k] += (int)(((layer[l] >> 24) & byteMask)*
                                        (layerOpacy));

                                R[k] += (int)(((layer[l] >> 16) & byteMask)
                                        *opacy);
                                G[k] += (int)(((layer[l] >> 8)  & byteMask)
                                        *opacy);
                                B[k] += (int)((layer[l]  & byteMask)*opacy);
                                F[k] += opacy;
                            }
                        }
                        else break;
                    }
                }
            }
        }

        if(canvas_ARGB.length != canvasRes)
            canvas_ARGB = new int[canvasRes];

        for(int i = 0; i < canvasHeight; i++) {
            for(int j = 0; j < canvasWidth; j++) {
                l = k = i*canvasWidth + j;
                R[k] *= cR[k];
                G[k] *= cG[k];
                B[k] *= cB[k];
                
                R[k] = R[k] > 255 ? 255 : R[k] < 0 ? 0 : R[k];
                G[k] = G[k] > 255 ? 255 : G[k] < 0 ? 0 : G[k];
                B[k] = B[k] > 255 ? 255 : B[k] < 0 ? 0 : B[k];
                
                canvas_ARGB[l] =
                        (255 << 24) + (R[k] << 16) + (G[k] << 8) + B[k];
            }
        }
        return canvas_ARGB;
    }

     //----------------------- //
    //    Layer Functions     //
   //----------------------- //


    /**
     * This method gets the planar image array of the layer's projection in the
     * canvas image area.
     * @param layer The layer to be drawn.
     * @return The planar image array.
     */
    int[] drawLayer(Layer layer) {

        int startX, endX, startY, endY;

        startX = layer.canvasXCoord;
        if(startX > 0) startX = 0;

        endX = layer.getWidth();
        if(startX + endX > canvasWidth)
            endX -= endX + startX - canvasWidth;

        startY = layer.canvasYCoord;
        if(startY > 0) startY = 0;

        endY = layer.getHeight();
        if(startY + endY > canvasHeight)
            endY -= endY + startY - canvasHeight;

        return layer.getLayerImg(startX, startY,
                endX, endY,
                canvasWidth, canvasHeight).bitmap;
    }

    /**
     * This method adds a new layer.
     * @param layerPos Position of the new layer.
     * @param layer The ARGBitmap of the new layer.
     * @param x The x-axis location relative to the canvas.
     * @param y The y-axis location relative to the canvas.
     */
    private void addLayer(int layerPos, ARGBitMap layer, int x, int y) {
        Layer newLayer = new Layer(layer, x, y);
        layers.add(layerPos, newLayer);
    }

    /**
     * This method removes a given layer.
     * @param layerPos The index of the layer to be removed.
     */
    private void deleteLayer(int layerPos) {
        layers.remove(layerPos);
    }

    /**
     * This method calls deleteLayer to remove the selected layer.
     */
    public void discardLayer() {
        deleteLayer(selectedLayer);
    }

    /**
     * This method duplicates the current selected layer.
     */
    public void duplicateLayer() {
        ARGBitMap imgClone = layers.get(selectedLayer).duplicate();
        addLayer(selectedLayer, imgClone, 0, 0);
    }

    public void bypassLastState() {
        layers.get(selectedLayer).discard(1);
    }

    /**
     * This method returns the current selected layer's width.
     * @return Layer's pixel width.
     */
    public int getLayerWidth() {
        return layers.get(selectedLayer).getWidth();
    }

    /**
     * This method returns the current selected layer's height.
     * @return Layer's pixel height.
     */
    public int getLayerHeight() {
        return layers.get(selectedLayer).getHeight();
    }

    /**
     * This method sets the opacy of the layer with the selectedLayer index.
     * @param opacy The new opacy level for the layer.
     */
    public void setOpacy(float opacy) {
        layers.get(selectedLayer).opacy = opacy;
    }

    /**
     * This method gets the current opacy of the layer with the selectedLayer
     * index.
     * @return The opacy level of the selected Layer.
     */
    public int getOpacy() {
        return (int)(layers.get(selectedLayer).opacy*100);
    }

    /**
     * This method sets the blending type for the image with the selectedLayer
     * index.
     * @param type The blending type.
     */
    public void setBlendType(int type) {
        layers.get(selectedLayer).blendType = type;
    }

    /**
     * This method gets the current blending type of the image with the
     * selectedLayer index.
     * @return The type of blending of the selected Layer.
     */
    public int getBlendType() {
        return layers.get(selectedLayer).blendType;
    }

    /**
     * This method swaps positions of the selected layer with the layer on top.
     */
    public void moveUp() {
        if(selectedLayer < getNumberofLayers() - 1) {
            layers.add(selectedLayer + 1, layers.remove(selectedLayer));
            selectedLayer++;
        }
    }

    /**
     * This method swaps positions of the selected layer with the layer below.
     */
    public void moveDown() {
        if(selectedLayer > 0) {
            layers.add(selectedLayer - 1, layers.remove(selectedLayer));
            selectedLayer--;
        }
    }

    /**
     * This method gets the current visible state of the selected Layer.
     * @return Visibility state.
     */
    public boolean isVisible() {
        return layers.get(selectedLayer).visible;
    }

    /**
     * This method swaps the visibility state of the selected layer.
     */
    public void toggleVisible() {
        if(layers.get(selectedLayer).visible)
            layers.get(selectedLayer).visible = false;
        else
            layers.get(selectedLayer).visible = true;
    }

    /**
     * This method adds a new layer with the flatten current canvas image.
     */
    public void flatten2NewLayer() {
        selectedLayer = 0;
        int flattenImg[] = drawCanvas(-1);
        layers.add(selectedLayer, new Layer(
                new ARGBitMap(flattenImg, canvasWidth, canvasHeight), 0, 0));
    }

    /**
     * This method replaces two consecutive layers with a single flatten layer.
     */
    public void mergeDown() {
        int b = layers.get(selectedLayer+1).blendType;
        int flattenImg[] = drawCanvas(2);
        layers.remove(selectedLayer);
        layers.remove(selectedLayer);
        layers.add(selectedLayer, new Layer(
                new ARGBitMap(flattenImg, canvasWidth, canvasHeight), 0, 0));
        setBlendType(b);
    }

    /**
     * This method applies a mergeDown and stores it into the bottom layer's
     * state.
     */
    public void mergeDownIntoLayer() {
        int flattenImg[] = drawCanvas(2);
        layers.remove(selectedLayer);
        layers.get(selectedLayer).update(
                new ARGBitMap(flattenImg, canvasWidth, canvasHeight));
    }

    /**
     * This method flattens all layers into a single one.
     */
    public void flattenLayers() {
        int flattenImg[] = drawCanvas(-1);
        layers.clear();
        addLayer(0, new ARGBitMap(flattenImg, canvasWidth, canvasHeight),
                0, 0);
    }

    /**
     * This method creates a new layer given a BufferedImage.
     * @param layerPos The position of the new layer.
     * @param image BufferedImage to be imported.
     */
    public void importImage2Layer(int layerPos, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] img = image.getRGB(0, 0, width, height, null, 0, width);
        addLayer(layerPos, new ARGBitMap(img, width, height), 0, 0);
    }

    /**
     * This method copies a planar image array.
     * @param img The planar array image to be copied.
     * @return The planar array image copy.
     */
    private int[] getImageCopy(int[] img) {
        int imgSize = img.length;
        int[] copy = new int[imgSize];
        System.arraycopy(img, 0, copy, 0, imgSize);
        return copy;
    }

    /**
     * This method returns the current number of layers.
     * @return number of layers.
     */
    public int getNumberofLayers() {
        return layers.size();
    }

    /**
     * This method calls the undo function in layer with the selectedLayer
     * index.
     * @return The number of undo states available after the call.
     */
    public int undo() {
        return layers.get(selectedLayer).undo();
    }

    /**
     * This method returns the undo states available for the layer with the
     * selectedLayer index.
     * @return Undo states available
     */
    public int getLayerUndoLevels() {
        return layers.get(selectedLayer).undos();
    }

    /**
     * This method calls the redo function in layer with the selectedLayer
     * index.
     * @return The number of redo states available after the call.
     */
    public int redo() {
        return layers.get(selectedLayer).redo();
    }

    /**
     * This method returns the redo states available for the layer with the
     * selectedLayer index.
     * @return Redo states available
     */
    public int getLayerRedoLevels() {
        return layers.get(selectedLayer).redos();
    }


     //----------------------- //
    //         Filters        //
   //----------------------- //

    /**
     * This method creates an autostereogram of the height field represented
     * by the image in the canvas.
     * @param width The width of the stereogram's base pattern.
     */
    public void stereoGram(int width) {
        int pattern[] = new int[width*canvasHeight];
        int depth = (255*4)/width;
        int r,g,b, pos;
        int ident, patPos, patIdx, npatIdx;
        for(int y = 0; y < canvasHeight; y++) {
            pos = y*width;
            for(int x = 0; x < width; x++) {
                r = rnd.nextInt(256);
                g = rnd.nextInt(256);
                b = rnd.nextInt(256);
                pattern[pos + x] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
        }
        selectedLayer = 0;
        flatten2NewLayer();
        int[] image = layers.remove(0).getLayerImg().bitmap;

        for(int y = 0; y < canvasHeight; y++) {
            ident = patIdx = npatIdx = 0;
            patPos = y*width;
            pos = y*canvasWidth;
            for(int x = 0; x < canvasWidth; x++) {
                patIdx = patIdx < width ? patIdx : patIdx - width;
                ident = (image[pos + x] & byteMask)/depth;
                npatIdx = patIdx + ident;
                npatIdx = npatIdx < width ? npatIdx : npatIdx - width;
                image[pos + x] = pattern[patPos + patIdx++] =
                        pattern[patPos + npatIdx];

            }
        }
        addLayer(0, new ARGBitMap(image, canvasWidth, canvasHeight), 0, 0);
    }

    /**
     * This method creates an ASCII autostereogram of the height field
     * represented by the canva's image.
     * @return The string of characters representing the stereogram.
     */
    public String asciiStereoGram() {
        int width = 12;
        int pattern[] = new int[width*canvasHeight];
        int depth = (255*4)/width;
        int c, pos, k;
        int ident, patPos, patIdx, npatIdx;
        for(int y = 0; y < canvasHeight; y++) {
            pos = y*width;
            for(int x = 0; x < width; x++) {
                c = rnd.nextInt(25) + 65;
                pattern[pos + x] = c;
            }
        }
        flatten2NewLayer();
        selectedLayer = 0;
        mosaic(8, 16);
        int[] image = layers.remove(0).getLayerImg().bitmap;
        StringBuilder ascii3d = new StringBuilder();
        patPos = 0;

        for(int y = 0; y < canvasHeight; y+=16) {
            ident = patIdx = npatIdx = 0;
            patPos += width;
            pos = y*canvasWidth;
            for(int x = 0; x < canvasWidth; x+=8) {
                patIdx = patIdx < width ? patIdx : patIdx - width;
                ident = (image[pos + x] & byteMask);
                if(ident != 0) ident = ident/depth + 1;
                npatIdx = patIdx + ident;
                npatIdx = npatIdx < width ? npatIdx : npatIdx - width;
                pattern[patPos + patIdx] = pattern[patPos + npatIdx];
//                System.out.print(pattern[patPos + patIdx] + " ");
                ascii3d.append((char)pattern[patPos + patIdx++]);
            }
//            System.out.println();
            ascii3d.append('\n');
        }
//        System.out.println(ascii3d);
        return ascii3d.toString();
    }

    /**
     * This method implements a bilinear resize filter on a planar image array.
     * @param image The original planar image array.
     * @param width The original image's width.
     * @param height The original image's height.
     * @param newW The new canvas width.
     * @param newH The new canvas height.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public int[] resize(int[] image, int width, int height, int newW, int newH)
             throws OutOfMemoryError {

        int oldCanvas[] = new int[(width+1)*(height+1)];
        int res = width * height;
        int i, j, k, h;

        h = 0;
        for (i = 0; i < height; i++) {
            k = i*width;
            for (j = 0; j < width; j++) {
                oldCanvas[h++] = image[k++];
            }
            oldCanvas[h++] = image[k-1];
        }
        k = res-width;
        for(i = 0; i < width; i++) {
            oldCanvas[h++] = image[k++];
        }

        int newCanvas[] = new int[newH*newW];

        double deltaX = (width - 1.0d)/(newW - 1.0d);
        double deltaY = (height - 1.0d)/(newH - 1.0d);

        int m, n;
        double r,g,b;
        int p0[] = new int[3];
        int p1[] = new int[3];
        int p2[] = new int[3];
        int p3[] = new int[3];
        double xoff, yoff, cx, cy;
        int x1, x2, y1, y2;

        m = n = 0;
        cy = 0;
        for(int y = 0; y < newH; y++) {
            y1 = (int)cy;
            y2 = y1 + 1;
            m = y1*(width+1);
            n = y2*(width+1);

            yoff = cy - y1;
            cx = 0;

            k = y*newW;
            for(int x = 0; x < newW; x++) {
                x1 = (int)cx;
                xoff = cx - x1;
                x2 = x1 + 1;

                p0[0] = (oldCanvas[m+x1] >> 16) & byteMask;
                p0[1] = (oldCanvas[m+x1] >> 8) & byteMask;
                p0[2] = (oldCanvas[m+x1]) & byteMask;
                p1[0] = (oldCanvas[m+x2] >> 16) & byteMask;
                p1[1] = (oldCanvas[m+x2] >> 8) & byteMask;
                p1[2] = (oldCanvas[m+x2]) & byteMask;
                p2[0] = (oldCanvas[n+x1] >> 16) & byteMask;
                p2[1] = (oldCanvas[n+x1] >> 8) & byteMask;
                p2[2] = (oldCanvas[n+x1]) & byteMask;
                p3[0] = (oldCanvas[n+x2] >> 16) & byteMask;
                p3[1] = (oldCanvas[n+x2] >> 8) & byteMask;
                p3[2] = (oldCanvas[n+x2]) & byteMask;

                r = (p0[0]*(1.0d-xoff) + p1[0]*xoff)*(1.0d-yoff)
                        + (p2[0]*(1.0d-xoff) + p3[0]*xoff)*yoff;
                g = (p0[1]*(1.0d-xoff) + p1[1]*xoff)*(1.0d-yoff)
                        + (p2[1]*(1.0d-xoff) + p3[1]*xoff)*yoff;
                b = (p0[2]*(1.0d-xoff) + p1[2]*xoff)*(1.0d-yoff)
                        + (p2[2]*(1.0d-xoff) + p3[2]*xoff)*yoff;

                newCanvas[k+x] =
                        (255 << 24) | ((int)r << 16) | ((int)g << 8) | (int)b;
                cx += deltaX;
            }
            cy += deltaY;
        }
        return newCanvas;
    }

    /**
     * This method will resize the flatten canvas image using the bilinear
     * interpolation resize method.
     * @param newW The new canvas width.
     * @param newH The new canvas height.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void resize(int newW, int newH) throws OutOfMemoryError {
        flattenLayers();
        int image[] = layers.remove(0).getLayerImg().bitmap;
        
        layers.add(
            new Layer(
                new ARGBitMap(
                resize(image, canvasWidth, canvasHeight, newW, newH),
                newW, newH), 0, 0));
        resizeCanvas(newW, newH);
    }

    /**
     * This method implements a clockwise rotation of the canvas.
     * The filter first flattens every layer, and then applies the rotation.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void rotateCW() throws OutOfMemoryError {
        flattenLayers();
        int newCanvas[] = layers.remove(0).getLayerImg().bitmap;
        int m;
        int k = 0;

        for(int x = 0; x < canvasWidth; x++) {
            m = canvasRes - canvasWidth + x;
            for(int y = canvasHeight - 1; y > - 1; y--) {
                newCanvas[k] = canvas_ARGB[m];
                m -= canvasWidth;
                k++;
            }
        }
        layers.add(
            new Layer(
                new ARGBitMap(newCanvas, canvasHeight, canvasWidth), 0, 0));
        resizeCanvas(canvasHeight, canvasWidth);
    }

    /**
     * This method implements a counter-clockwise rotation of the canvas.
     * The filter first flattens every layer, and then applies the rotation.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void rotateCCW() throws OutOfMemoryError {
        flattenLayers();
        int newCanvas[] = layers.remove(0).getLayerImg().bitmap;
        int m;
        int k = 0;

        for(int x = canvasWidth - 1; x > - 1; x--) {
            m = x;
            for(int y = 0; y < canvasHeight; y++) {
                newCanvas[k] = canvas_ARGB[m];
                m += canvasWidth;
                k++;
            }
        }
        layers.add(
            new Layer(
                new ARGBitMap(newCanvas, canvasHeight, canvasWidth), 0, 0));
        resizeCanvas(canvasHeight, canvasWidth);
    }

    /**
     * This method implements a 180 degrees rotation of the canvas.
     * The filter first flattens every layer, and then applies the rotation.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void rotate180() throws OutOfMemoryError {
        flattenLayers();
        int newCanvas[] = layers.remove(0).getLayerImg().bitmap;
        int m;
        int k = 0;

        for(int y = canvasHeight - 1; y > -1; y--) {
            m = y*canvasWidth;
            for(int x = canvasWidth - 1; x > -1; x--) {
                newCanvas[k] = canvas_ARGB[m+x];
                k++;
            }
        }
        layers.add(
            new Layer(
                new ARGBitMap(newCanvas, canvasWidth, canvasHeight), 0, 0));
    }

    /**
     * This method implements a vertical flip of the canvas.
     * The filter first flattens every layer, and then flips the canvas.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void flipV() throws OutOfMemoryError {
        flattenLayers();
        int newCanvas[] = layers.remove(0).getLayerImg().bitmap;
        int m;
        int k = 0;

        for(int y = canvasHeight - 1; y > -1; y--) {
            m = y*canvasWidth;
            for(int x = 0; x < canvasWidth; x++) {
                newCanvas[k] = canvas_ARGB[m+x];
                k++;
            }
        }
        layers.add(
            new Layer(
                new ARGBitMap(newCanvas, canvasWidth, canvasHeight), 0, 0));
    }

    /**
     * This method implements a horizontal flip of the canvas.
     * The filter first flattens every layer, and then flips the canvas.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
        public void flipH() throws OutOfMemoryError {
        flattenLayers();
        int newCanvas[] = layers.remove(0).getLayerImg().bitmap;
        int m;
        int k = 0;

        for(int y = 0; y < canvasHeight; y++) {
            m = y*canvasWidth;
            for(int x = canvasWidth - 1; x > -1; x--) {
                newCanvas[k] = canvas_ARGB[m+x];
                k++;
            }
        }
        layers.add(
            new Layer(
                new ARGBitMap(newCanvas, canvasWidth, canvasHeight), 0, 0));
    }

    /**
     * This method implements the brightness and contrast filter.
     *
     * @param brightness The brightness scaling factor
     * @param contrast The contrast multiplying factor.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void bnc(float brightness, float contrast) throws OutOfMemoryError {
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        int w = layer.w;
        int h = layer.h;
        int a, r, g, b, k;
        brightness *= 255;
        contrast = (float)Math.pow((contrast*100 + 100.0)/100, 2);
        for(int i = 0; i < h; i++) {
            for(int j = 0; j < w; j++) {
                k = i*w+j;
                a = (bitmap[k] >> 24) & byteMask;
                r = (bitmap[k] >> 16) & byteMask;
                g = (bitmap[k] >> 8) & byteMask;
                b = bitmap[k] & byteMask;

                r += brightness;
                g += brightness;
                b += brightness;

                if(contrast != 0) {
                    r = (int)((r - 127.0) * contrast + 127.0);
                    g = (int)((g - 127.0) * contrast + 127.0);
                    b = (int)((b - 127.0) * contrast + 127.0);
                }

                r = r > 255 ? 255 : r < 0 ? 0 : r;
                g = g > 255 ? 255 : g < 0 ? 0 : g;
                b = b > 255 ? 255 : b < 0 ? 0 : b;

                bitmap[k] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method implements the color balancing filter.
     * @param rDelta Red channel delta.
     * @param gDelta Green channel delta.
     * @param bDelta Blue channel delta.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void colorBalance(float rDelta, float gDelta, float bDelta)
     throws OutOfMemoryError{
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        int w = layer.w;
        int h = layer.h;
        int a, r, g, b;
        int k;
        for(int i = 0; i < h; i++) {
            for(int j = 0; j < w; j++) {
                k = i*w+j;
                a = (bitmap[k] >> 24) & byteMask;
                r = (bitmap[k] >> 16) & byteMask;
                g = (bitmap[k] >> 8) & byteMask;
                b = bitmap[k] & byteMask;

                r += rDelta;
                g += gDelta;
                b += bDelta;

                r = r > 255 ? 255 : r < 0 ? 0 : r;
                g = g > 255 ? 255 : g < 0 ? 0 : g;
                b = b > 255 ? 255 : b < 0 ? 0 : b;

                bitmap[k] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method implements the colorize filter.
     * @param R Red mask value.
     * @param G Green mask value.
     * @param B Blue mask value.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void colorize(float R, float G, float B) throws OutOfMemoryError {
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        int w = layer.w;
        int h = layer.h;
        int a, r, g, b;
        int k;
        R /= 255; G /= 255; B /=255;
        for(int i = 0; i < h; i++) {
            for(int j = 0; j < w; j++) {
                k = i*w+j;
                a = (bitmap[k] >> 24) & byteMask;
                r = (bitmap[k] >> 16) & byteMask;
                g = (bitmap[k] >> 8) & byteMask;
                b = bitmap[k] & byteMask;

                r *= R;
                g *= G;
                b *= B;

                r = r > 255 ? 255 : r < 0 ? 0 : r;
                g = g > 255 ? 255 : g < 0 ? 0 : g;
                b = b > 255 ? 255 : b < 0 ? 0 : b;

                bitmap[k] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method rebalances the image channels given each channel's new
     * weight.
     * @param R Red channel weight.
     * @param G Green channel weight.
     * @param B Blue channel weight.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void colorWeightBalance(float R, float G, float B)
     throws OutOfMemoryError {
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        int w = layer.w;
        int h = layer.h;
        int a, r, g, b;
        int k;
        float AvrRGB = (R + G + B)/3;

        float wR = R/AvrRGB;
        float wG = G/AvrRGB;
        float wB = B/AvrRGB;

        for(int i = 0; i < h; i++) {
            for(int j = 0; j < w; j++) {

                k = i*w+j;
                a = (bitmap[k] >> 24) & byteMask;
                r = (bitmap[k] >> 16) & byteMask;
                g = (bitmap[k] >> 8) & byteMask;
                b = bitmap[k] & byteMask;

                r *= wR;
                g *= wG;
                b *= wB;

                r = r > 255 ? 255 : r < 0 ? 0 : r;
                g = g > 255 ? 255 : g < 0 ? 0 : g;
                b = b > 255 ? 255 : b < 0 ? 0 : b;

                bitmap[k] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method implements the invert filter.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void invert() throws OutOfMemoryError {
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        int w = layer.w;
        int h = layer.h;
        int a,r,g,b,k;


        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                k = y*w+x;
                a = (bitmap[k] >> 24) & byteMask;
                r = (bitmap[k] >> 16) & byteMask;
                g = (bitmap[k] >> 8) & byteMask;
                b = bitmap[k] & byteMask;

                r = 255 - r;
                g = 255 - g;
                b = 255 - b;

                bitmap[k] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method gets the gray scale planar image by getting the average value
     * of the 3 channels.
     * @return The grayscale planar image array.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public int[] getGrayScale() throws OutOfMemoryError {
        int length = canvas_ARGB.length;
        int[] grayScale = new int[length];
        for (int i = 0; i < length; i++) {
            grayScale[i] =
                    ((canvas_ARGB[i] >> 16) & byteMask) +
                    ((canvas_ARGB[i] >> 8) & byteMask) +
                    (canvas_ARGB[i] & byteMask);
            grayScale[i] /= 3;
        }
        return grayScale;
    }

    /**
     * This method implements the desaturate filter.
     * It recieves different weights per channel.
     * @param R Red channel weight.
     * @param G Green channel weight.
     * @param B Blue channel weight.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void desaturate(float R, float G, float B) throws OutOfMemoryError {
        colorWeightBalance(R, G, B);
        int bitmap[] = layers.get(selectedLayer).getLayerImg().bitmap;
        int sum;
        int length = bitmap.length;
        for (int i = 0; i < length; i++) {
            sum =
                    ((bitmap[i] >> 16) & byteMask) +
                    ((bitmap[i] >> 8) & byteMask) +
                    (bitmap[i] & byteMask);
            sum /= 3;
            bitmap[i] =
                    (bitmap[i] & 0xff000000) | (sum << 16) | (sum << 8) | (sum);
        }
    }


    /**
     * This method rasters a circle of a specified radius into a planar image
     * array of a specified size using Bresenham's circle algorithm.
     * (radius <= size).
     * @param size The size of the rasterized image
     * @param radius The radius of the rasterized circle
     * @param color Rasterized circle color.
     * @param bcolor Background fill color.
     * @return Rasterized planar image array.
     */
    public int[] rasterCircle(int size, int radius, int color, int bcolor) {
        
        int circle[] = new int[size*size];
        if(radius > size) {
            for(int i = 0; i < circle.length; i++)
                circle[i] = color;
            return circle;
        }

        for(int i = 0; i < circle.length; i++)
            circle[i] = bcolor;

        if(radius < 1) return circle;
        
        int x0, y0;
        x0 = y0 = (int) Math.floor(1.0*size/2);

        int f = 1 - radius;
        int ddF_x = 1;
        int ddF_y = -2 * radius;
        int x = 0;
        int y = radius;

        circle[(y0+radius)*size + x0] = color;
        circle[(y0-radius)*size + x0] = color;
        circle[y0*size + (x0+radius)] = color;
        circle[y0*size + (x0-radius)] = color;

        while(x < y) {
            if(!(ddF_x == 2 * x + 1)) break;
            if(!(ddF_y == -2 * y)) break;
            if(!(f == x*x + y*y - radius*radius + 2*x - y + 1)) break;

            if(f >= 0) {
                y--;
                ddF_y += 2;
                f += ddF_y;
            }
            x++;
            ddF_x += 2;
            f += ddF_x;

            circle[(y0+y)*size + (x0+x)] = 255;
            circle[(y0+y)*size + (x0-x)] = 255;
            circle[(y0-y)*size + (x0+x)] = 255;
            circle[(y0-y)*size + (x0-x)] = 255;
            circle[(x0+x)*size + (y0+y)] = 255;
            circle[(x0+x)*size + (y0-y)] = 255;
            circle[(x0-x)*size + (y0+y)] = 255;
            circle[(x0-x)*size + (y0-y)] = 255;
        }

        boolean in = false;
        int limit = 0;
        int row = -1;
        for(int i = 0; i < circle.length; i++) {
            if(i%size == 0) {
                row++;
                in = false;
            }
            if(!in && circle[i] != bcolor) {
                in = true;
                limit = row*size + (size - i%size);
            }
            if(in && (i == limit)) in = false;
            if(in) circle[i] = color;
        }
        return circle;
    }

    /**
     * This method rasters a line of a specified radius into a planar image
     * array of a specified size.
     * (radius <= size).
     * @param size The size of the rasterized image
     * @param radius The size of the rasterized line
     * @param color Rasterized line color.
     * @param bcolor Background fill color.
     * @return Rasterized planar image array.
     */
    int[] rasterLine(int size, int radius, int color, int bcolor) {
        int line[] = new int[size];
        if(radius > size) {
            for(int i = 0; i < line.length; i++)
                line[i] = color;
            return line;
        }

        for(int i = 0; i < line.length; i++)
            line[i] = bcolor;

        if(radius < 1) return line;

        int limit = 0;
        boolean in = false;

        for(int i = 0; i < line.length; i++) {
            if(!in && i%size == radius) {
                in = true;
                limit = size - radius;
            }
            if(in && (i == limit)) in = false;
            if(in) line[i] = color;
        }

        return line;
    }

    /**
     * This method uses a halftone technic to posterize the image using a
     * defined pattern.
     * @param size The size in pixels of the pattern. (circle => radius,
     * line => width).
     * @param patternType True => Circles, False => Lines
     * @throws java.lang.OutOfMemoryError
     */
    void halftone(int size, boolean patternType) throws OutOfMemoryError {
        
        int white = (255 << 24) | (255 << 16) | (255 << 8) | 255;
        int black = (255 << 24);

        BufferedImage pattern;
        int patternTiles[][];
        BufferedImageOp resizePattern;
        
        if(patternType) {
            resizePattern = new AffineTransformOp(
                AffineTransform.getScaleInstance(0.5, 0.5),
                new RenderingHints(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR));

            pattern = new BufferedImage(
                size*2+1, size*2+1, BufferedImage.TYPE_INT_ARGB);
            patternTiles = new int[size][size*size];

            for(int i = 0; i < size; i++) {
                pattern.setRGB(0, 0, size*2+1, size*2+1,
                        rasterCircle(size*2+1, size-i, black, white),
                        0, size*2+1);

                patternTiles[i] = resizePattern.filter(pattern, null).getRGB(
                        0, 0, size, size, null, 0, size);
            }
        }
        else {
            resizePattern = new AffineTransformOp(
                AffineTransform.getScaleInstance(0.5, 1.0),
                new RenderingHints(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC));
            pattern = new BufferedImage(
                size*2+1, 1, BufferedImage.TYPE_INT_ARGB);
            patternTiles = new int[size][size];

            for(int i = 0; i < size; i++) {
                pattern.setRGB(0, 0, size*2+1, 1,
                        rasterLine(size*2+1, size-i, white, black),
                        0, size*2+1);

                patternTiles[i] = resizePattern.filter(pattern, null).getRGB(
                        0, 0, size, 1, null, 0, size);
            }
        }
        
        desaturate(1, 1, 1);
        ditheredPosterize(size);
        if(patternType) mosaic(size, size);
        else mosaic(size, 1);
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        layers.get(selectedLayer).discard(2);
        layers.get(selectedLayer).discard(1);
        layers.get(selectedLayer).discard(0);
        int image[] = layer.bitmap;

        if(patternType) {
            int row = 0;
            for(int i = 0; i < layer.h; i+=size) {
                int offs;
                if(row%2 == 0) offs = 0;
                else {
                    offs=size/2;
                    for(int y = i; y < layer.h; y++) {
                        for(int x = 0; x < size/2; x++) {
                            image[i*layer.w + x] = white;
                        }
                    }
                }
                for(int j = offs; j < layer.w; j+=size) {
                    int value =
                            (int)(((image[i*layer.w + j] & byteMask)*1d
                            /255d)*(size-1));
                    int y2 = 0;
                    for(int y = i; (y < i+size) && (y < layer.h); y++) {
                        int x2 = 0;
                        for(int x = j; (x < j+size) && (x < layer.w); x++) {
                            image[y*layer.w + x] =
                                    patternTiles[value][y2*size + x2];
                            x2++;
                        }
                        y2++;
                    }
                }
                row++;
            }
        }
        else {
            for(int i = 0; i < layer.h; i++) {
                for(int j = 0; j < layer.w; j+=size) {
                    int value =
                            (int)(((image[i*layer.w + j] & byteMask)*1d
                            /255d)*(size-1));
                    int x2 = 0;
                    for(int x = j; (x < j+size) && (x < layer.w); x++) {
                        image[i*layer.w + x] = patternTiles[value][x2];
                        x2++;
                    }
                }
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method returns the rounding error for a given value, the color level
     * step width and the half of the step.
     * @param step
     * @param hstep
     * @param value
     * @return The absolute rounding error.
     */
    int getError(int step, int hstep, int value) {
        value %= step;
        if(value >= hstep) return value;
        else return -value;
    }

    /**
     * This method returns the value of a pixel after adding the error
     * precalculated values for each channel and rounding the value to the
     * closest available color.
     *
     * @param value Original pixel value.
     * @param errR Red channel error.
     * @param errG Green channel error.
     * @param errB Blue channel error.
     * @param step Posterize step width.
     * @return Modified value.
     */
    int ditheredValue(int value, int errR, int errG, int errB, int step) {

        int r = ((errR + ((value >> 16) & byteMask))/step)*step;
        int g = ((errG + ((value >> 8) & byteMask))/step)*step;
        int b = ((errB + (value & byteMask))/step)*step;

        r = r > 255 ? 255 : r < 0 ? 0 : r;
        g = g > 255 ? 255 : g < 0 ? 0 : g;
        b = b > 255 ? 255 : b < 0 ? 0 : b;

//        System.out.print("----\n");
//        printARGB(value);
//        printARGB((255 << 24) | (r << 16) | (g << 8) | b);
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * This method posterizes the image using alternating Floyd-Steinberg
     * matrix by rows.
     * @param levels Tone levels per channel.
     */
    void ditheredPosterize(int levels) {

        if(levels > 256) levels = 256;
        if(levels < 1) levels = 1;

        int step = 256/levels;
        int hstep = step/2;

        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int image[] = layer.bitmap;
        int width = layer.w;
        int height = layer.h;
        int res = width * height;
        int dres = 2*res;
        int currPixel;
        int pixel;
        int errors[] = new int[res*3];
        int error;

        for(int i = 1; i < height-1; i++) {
            for(int j = 1; j < width-1; j++) {
                currPixel = i*width+j;
                pixel = image[currPixel];

                error = getError(step, hstep, (pixel >> 16) & byteMask);
                errors[currPixel+1] += (error*7) >> 4;
                errors[currPixel+width-1] += (error*3) >> 4;
                errors[currPixel+width] += (error*5) >> 4;
                errors[currPixel+width+1] += error >> 4;

                error = getError(step, hstep, (pixel >> 8) & byteMask);
                errors[res+currPixel+1] += (error*7) >> 4;
                errors[res+currPixel+width-1] += (error*3) >> 4;
                errors[res+currPixel+width] += (error*5) >> 4;
                errors[res+currPixel+width+1] += error >> 4;

                error = getError(step, hstep, pixel & byteMask);
                errors[dres+currPixel+1] += (error*7) >> 4;
                errors[dres+currPixel+width-1] += (error*3) >> 4;
                errors[dres+currPixel+width] += (error*5) >> 4;
                errors[dres+currPixel+width+1] += error >> 4;

                image[currPixel] =
                            ditheredValue(
                            image[currPixel],
                            errors[currPixel],
                            errors[currPixel+res],
                            errors[currPixel+dres],
                            step);
            }
            currPixel = width*(height-1);
            for(int k = 1; k < width-1; k++) {
                currPixel++;
                image[currPixel] =
                                ditheredValue(
                                image[currPixel],
                                errors[currPixel],
                                errors[currPixel+res],
                                errors[currPixel+dres],
                                step);
            }
            currPixel = 0;
            for(int k = 0; k < res - width; k+=width) {
                image[k] =
                                ditheredValue(
                                image[k],
                                errors[k],
                                errors[k+res],
                                errors[k+dres],
                                step);
                image[k+width-1] =
                                ditheredValue(
                                image[k],
                                errors[k],
                                errors[k+res],
                                errors[k+dres],
                                step);
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method implements the posterization effect. It reduces the amount
     * of colors of the image to the given levels.
     * @param levels Amount of colors.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void posterize(int levels) throws OutOfMemoryError {
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        int w = layer.w;
        int h = layer.h;
        int k, l;
        int normal = 256/levels;
        int r,g,b;
        for(int y = 0; y < h; y++) {
            k = y*w;
            for(int x = 0; x < w; x++) {
                l = k + x;
                r = (bitmap[l] >> 16) & byteMask;
                g = (bitmap[l] >> 8) & byteMask;
                b = bitmap[l] & byteMask;

                r = (r/normal)*normal;
                g = (g/normal)*normal;
                b = (b/normal)*normal;

                bitmap[l] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    void bw() {
        desaturate(1, 1, 1);
        int image[] = layers.get(selectedLayer).getLayerImg().bitmap;
        for(int i = 0; i < image.length; i++)
            if((image[i] & byteMask) > 127)
                image[i] = (255 << 24) | (255 << 16) | (255 << 8) | 255;
            else
                image[i] = (255 << 24);
    }

    /**
     * This method implements the Oil Paint effect filter.
     * @param passes Number of recursive filter passes.
     * @param brushSize The size of the brush effect in pixels.
     * @param colors The number of colors in the palette.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void oilPaintEffect(int passes, int brushSize, int colors)
     throws OutOfMemoryError{
        for(int n = 0; n < passes; n++) {
            posterize(colors);
            int oldBitmap[] = layers.get(selectedLayer).getLayerImg().bitmap;
            ARGBitMap layer = layers.get(selectedLayer).duplicate();
            layers.get(selectedLayer).discard(0);
            int bitmap[] = layer.bitmap;

            int matSize = brushSize;
            int mHalf = matSize/2;
            int matArea = matSize*matSize;

            int quantify[] = new int[matArea*2];
            int it = 0;
            boolean has = false;
            int c;
            int pixelColor = 0;
            int max;

            int lx, ly;
            int w = layer.w;
            int h = layer.h;
            int k = 0;
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    // Reset Quantifier
                    for(c = 0; c < matArea*2; c++)
                        quantify[c] = -1;
                    it = 1;
                    has = false;
                    for(int i = 0; i < matSize; i++) {
                        for(int j = 0; j < matSize; j++) {
                            lx = x + j;
                            ly = y + i;
                            if( ly < h && lx < w) {
                                for(c = 0;
                                    c < it && c < matArea;
                                    c++) {
                                    if(quantify[c] == oldBitmap[ly*w + lx]) {
                                        has = true;
                                        break;
                                    }
                                }
                                if(has) {
                                    quantify[c+matArea]++;
                                }
                                else {
                                    quantify[it - 1] = oldBitmap[ly*w + lx];
                                    quantify[it - 1 + matArea] = 1;
                                    it++;
                                }
                                has = false;
                            }
                        }
                    }

                    max = 0;
                    for(c = 0; c < matArea; c++)
                        if(quantify[c + matArea] > max) {
                            max = quantify[c + matArea];
                            pixelColor = quantify[c];
                        }
                    bitmap[k++] = pixelColor;
                }
            }
            layers.get(selectedLayer).update(layer);
            if(n > 0)
                layers.get(selectedLayer).discard(1);
        }
    }

    /**
     * This method implements a matrix convolution within each pixel of the
     * image.
     * @param convMat The square matrix defining the convolution
     * @param matSize The size of the convolution matrix.
     * @param convDet The constant by which the final result is divided by.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void matrixConvolution(
            float[] convMat, int matSize, float convDet)
            throws OutOfMemoryError {

        int mHalf = matSize/2;
        int offA, offB;
        int ix, iy;

        int oldBitmap[] = layers.get(selectedLayer).getLayerImg().bitmap;
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        int w = layer.w;
        int h = layer.h;
        float r, g, b;
        int k = 0;

        float convColorR, convColorG, convColorB;
        int sqW, sqH;

        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                r = g = b = 0.0f;
                for(int i = -mHalf; i <= mHalf; i++) {
                    iy = y+i;
                    if(iy > 0 && iy < h)
                        offA = iy*w;
                    else
                        offA = y*w;
                    offB = matSize*(i + mHalf) + mHalf;
                    for(int j = -mHalf; j <= mHalf; j++) {
                        float f = convMat[offB+j];
                        if(f != 0) {
                            ix = x+j;
                            if(!(ix > 0 && ix <w))
                                ix = x;
                            r += f * ((oldBitmap[offA + ix] >> 16) & byteMask);
                            g += f * ((oldBitmap[offA + ix] >> 8) & byteMask);
                            b += f * (oldBitmap[offA + ix] & byteMask);
                        }
                    }
                }
                
                if(convDet != 0) {
                    r /= convDet;
                    g /= convDet;
                    b /= convDet;
                }

                r = r > 255 ? 255 : r < 0 ? 0 : r;
                g = g > 255 ? 255 : g < 0 ? 0 : g;
                b = b > 255 ? 255 : b < 0 ? 0 : b;
                bitmap[k++] =
                        (255 << 24) | ((int)r << 16) | ((int)g << 8) | (int)b;
            }
        }

        layers.get(selectedLayer).update(layer);
    }

    /**
     * Blurring by 3x3 matrix convolution.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void blur() throws OutOfMemoryError {
        float [] blurMatrix = {
            0.0f, 0.2f, 0.0f,
            0.2f, 0.2f, 0.2f,
            0.0f, 0.2f, 0.0f};
        matrixConvolution(blurMatrix, 3, 1);
    }

    /**
     * Extra blurring by 5x5 matrix convolution.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void moreBlur() throws OutOfMemoryError {
        float [] blurMoreMatrix = {
            0f, 0f, 1f, 0f, 0f,
            0f, 1f, 1f, 1f, 0f,
            1f, 1f, 1f, 1f, 1f,
            0f, 1f, 1f, 1f, 0f,
            0f, 0f, 1f, 0f, 0f };
        matrixConvolution(blurMoreMatrix, 5, 13);
    }

    /**
     * Sharpening by 3x3 matrix convolution.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void sharpen() throws OutOfMemoryError {
        float [] sharpenMatrix = {
            -1.0f, -1.0f, -1.0f,
            -1.0f, 9.0f, -1.0f,
            -1.0f, -1.0f, -1.0f};
        matrixConvolution(sharpenMatrix, 3, 1);
    }

    /**
     * Sharpening by 5x5 matrix convolution.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void moreSharpen() throws OutOfMemoryError {
        float [] sharpenMoreMatrix = {
            -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
            -1.0f, 2.0f, 2.0f, 2.0f, -1.0f,
            -1.0f, 2.0f, 16.0f, 2.0f, -1.0f,
            -1.0f, 2.0f, 2.0f, 2.0f, -1.0f,
            -1.0f, -1.0f, -1.0f, -1.0f, -1.0f};
        matrixConvolution(sharpenMoreMatrix, 5, 16);
    }

    /**
     * Edge detect by 3x3 matrix convolution.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void edgeDetect() throws OutOfMemoryError {
        float [] edgeDetectHLR = {
            1.0f, 0.0f, -1.0f,
            2f, 0.0f, -2.0f,
            1.0f, 0.0f, -1.0f};

        float [] edgeDetectHRL = {
            -1.0f, 0.0f, 1.0f,
            -2.0f, 0.0f, 2.0f,
            -1.0f, 0.0f, 1.0f};

        float [] edgeDetectVUD = {
            1.0f, 2.0f, 1.0f,
            0.0f, 0.0f, 0.0f,
            -1.0f, -2.0f, -1.0f};

        float [] edgeDetectVDU = {
            -1.0f, -2.0f, -1.0f,
            0.0f, 0.0f, 0.0f,
            1.0f, 2.0f, 1.0f};

        duplicateLayer();
        matrixConvolution(edgeDetectVDU, 3, 6);
        setBlendType(1);
        selectedLayer++;
        duplicateLayer();
        matrixConvolution(edgeDetectHLR, 3, 6);
        setBlendType(1);
        selectedLayer--;
        mergeDown();
        selectedLayer++;
        duplicateLayer();
        matrixConvolution(edgeDetectVUD, 3, 6);
        setBlendType(1);
        selectedLayer--;
        mergeDown();
        selectedLayer++;
        duplicateLayer();
        matrixConvolution(edgeDetectHRL, 3, 6);
        selectedLayer--;
        mergeDown();
        mergeDownIntoLayer();
        
    }

    /**
     * Emboss by 3x3 matrix convolution.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void emboss() throws OutOfMemoryError {
        float [] emboss = {
            2.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, -1.0f};
        matrixConvolution(emboss, 3, 1);
        bnc(0.5f, 0);
        layers.get(selectedLayer).discard(1);
        desaturate(1, 1, 1);
        layers.get(selectedLayer).discard(1);
    }

    /**
      Motion blur by 9x9 matrix convolution.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void motionBlur() throws OutOfMemoryError {
        float [] motionBlurMatrix =
            {1, 0 ,0 ,0, 0, 0, 0, 0, 0,
             0, 1, 0, 0, 0, 0, 0, 0, 0,
             0, 0, 1, 0, 0, 0, 0, 0, 0,
             0, 0, 0, 1, 0, 0, 0, 0, 0,
             0, 0, 0, 0, 1, 0, 0, 0, 0,
             0, 0, 0, 0, 0, 1, 0, 0, 0,
             0, 0, 0, 0, 0, 0, 1, 0, 0,
             0, 0, 0, 0, 0, 0, 0, 1, 0,
             0, 0, 0, 0, 0, 0, 0, 0, 1};
        matrixConvolution(motionBlurMatrix, 9, 9);
    }

    /**
     * This method implements a wave effect running from top to bottom.
     * @param f Wave frequency.
     * @param a Wave amplitud.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    void waveEffect(float f, float a) throws OutOfMemoryError {
        int original[] = layers.get(selectedLayer).getLayerImg().bitmap;
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        float clockDiv = 6.283185f*f/layer.h;
        int delta, pos;
        int k = 0;

        for(int i = 0; i < layer.h; i++) {
            delta = (int)(Math.sin(clockDiv*i)*a);
            for(int j = 0; j < layer.w; j++) {
                pos = j + delta;
                if((pos < 0) || !(pos < layer.w))
                    bitmap[k+j] = 0;
                else
                    bitmap[k+j] = original[k+pos];
            }
            k += layer.w;
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method implements the mosaic effect. It creates a new lower
     * resolution image with the same pixel dimensions by creating tiles of
     * size m x n.
     * @param m Tile width.
     * @param n Tile height.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void mosaic(int m, int n) throws OutOfMemoryError {
        ARGBitMap layer = layers.get(selectedLayer).duplicate();
        int bitmap[] = layer.bitmap;
        int w = layer.w;
        int h = layer.h;
        int r, g, b;
        int k;

        int mRes = m*n;
        int sqW, sqH;

        for(int i = 0; i < h; i+=n) {
            sqH = i+n < h ? i+n : h;
            for(int j = 0; j < w; j+=m) {
                sqW = j+m < w ? j+m : w;
                r = g = b = 0;
                for(int y = i; y < sqH && y < h; y++) {
                    for(int x = j; x < sqW && x < w; x++) {
                        k = y*w + x;
                        r += (bitmap[k] >> 16) & byteMask;
                        g += (bitmap[k] >> 8) & byteMask;
                        b += bitmap[k] & byteMask;
                    }
                }

                r /= mRes;
                g /= mRes;
                b /= mRes;

                r = r > 255 ? 255 : r < 0 ? 0 : r;
                g = g > 255 ? 255 : g < 0 ? 0 : g;
                b = b > 255 ? 255 : b < 0 ? 0 : b;

                for(int y = i; y < sqH; y++) {
                    for(int x = j; x < sqW; x++) {
                        k = y*w + x;
                        bitmap[k] = (255 << 24) | (r << 16) | (g << 8) | b;
                    }
                }
            }
        }
        layers.get(selectedLayer).update(layer);
    }

    /**
     * This method implements the recursive mosaic algorithm.
     * It recreates an image by resampling the image into a smaller resolution,
     * and using it as a tile with a color mask to recreate the original image.
     * The method flattens all layers.
     *
     * @param iScale Scale of the new image.
     * @param mScale Scale of the tiles.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void recurMosaic(float iScale, float mScale)
            throws OutOfMemoryError {
        flattenLayers();
        int bitmap[] = layers.get(0).getLayerImg().bitmap;
        int w = canvasWidth;
        int h = canvasHeight;

        int nw = (int)(w * mScale);
        int nh = (int)(h * mScale);

        int niw = (int)(w * iScale);
        int nih = (int)(h * iScale);

        BufferedImage image = new BufferedImage(
                w, h, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, w, h, bitmap, 0, w);

        // Mosaic Tile
        BufferedImageOp makeTile = new AffineTransformOp(
                AffineTransform.getScaleInstance(mScale, mScale),
                new RenderingHints(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC));

        BufferedImage scaledImage = makeTile.filter(image, null);
        int mosaicTile[] = scaledImage.getRGB(0, 0, nw, nh, null, 0, nw);

        resize(niw, nih);
        mosaic(nw, nh);
        bitmap = layers.remove(0).getLayerImg().bitmap;
        System.gc();

        scaledImage = null;
        image = null;

        int sqW, sqH;
        int p,q;

        int r, g, b;
        int argb, m, n;

        for(int i = 0; i < nih; i+=nh) {
            sqH = i+nh < nih ? i+nh : nih;
            for(int j = 0; j < niw; j+=nw) {
                sqW = j+nw < niw ? j+nw : niw;
                p = 0;
                for(int k = i; k < sqH; k++) {
                    q = 0;
                    for(int l = j; l < sqW; l++) {
                        m = k*niw + l;
                        r = (bitmap[m] >> 16) & byteMask;
                        g = (bitmap[m] >> 8) & byteMask;
                        b = bitmap[m] & byteMask;

                        n = p*nw + q;
                        r *= (mosaicTile[n] >> 16) & byteMask;
                        g *= (mosaicTile[n] >> 8) & byteMask;
                        b *= mosaicTile[n] & byteMask;

                        r /= 255;
                        g /= 255;
                        b /= 255;

                        r = r > 255 ? 255 : r < 0 ? 0 : r;
                        g = g > 255 ? 255 : g < 0 ? 0 : g;
                        b = b > 255 ? 255 : b < 0 ? 0 : b;

                        bitmap[m] = (255 << 24) | (r << 16) | (g << 8) | b;
                        q++;
                    }
                    p++;
                }
            }
        }
        if(niw > canvasWidth)
            resizeCanvas(niw, canvasHeight);
        if(nih > canvasHeight)
            resizeCanvas(canvasWidth, nih);
        
        layers.add(new Layer(
                new ARGBitMap(bitmap, niw, nih), 0, 0));
    }

    /**
     * This method gets the average RGB value of an planar image array.
     * @param image The planar image array.
     * @return A double 3D array representing the RGB average.
     */
    public double[] getRGBAvr(int image[]) {
        double length = image.length;
        double avgR, avgG, avgB;
        avgR = avgB = avgG = 0.0;
        for(int i = 0; i < length; i++) {
            avgR += ((image[i] >> 16) & byteMask);
            avgG += ((image[i] >> 8) & byteMask);
            avgB += (image[i] & byteMask);
        }
        double[] pointRGB = {
            rnd.nextDouble() - 0.5d + (avgR/length),
            rnd.nextDouble() - 0.5d + (avgG/length),
            rnd.nextDouble() - 0.5d + (avgB/length)};
        return pointRGB;
    }

    /**
     * This method is used to pre-process a batch of images. The function is to
     * resize all images to a new dimension and obtain the RGB average of every
     * image. The batch process is done using an ImageMagick API for Java
     * (IM4Java).
     *
     * @param images The array of File objects representing the images to
     * process.
     * @param folder The File representing the folder into which IMP will place
     * all of the processed images and the RGB average data.
     * @param width The width of the processed images.
     * @param height The height of the processed images.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void processImages(File images[], File folder,
            int width, int height) throws OutOfMemoryError {

        ImageIO.setUseCache(false);
        StringBuilder log = new StringBuilder("--- Image Processing Log ---\n");
        String dirPath = folder.getPath() + File.separatorChar + "mosaicTiles";
        new File(dirPath).mkdirs();

        // Tile Creation Operator
        IMOperation resizeOp = new IMOperation();
        resizeOp.addImage();
        resizeOp.resize(width, height, '!');
        resizeOp.addImage("png:-");
        Stream2BufferedImage s2b = new Stream2BufferedImage();
        ConvertCmd cmd = new ConvertCmd();
        cmd.setOutputConsumer(s2b);

        BufferedImage im;
        int r, g, b;
        int pixels[];
        File outputImage;
        nodes = new LinkedList<ImageNode>();
        long timeStamp = System.currentTimeMillis();
        System.out.println("Processing " + images.length + " images...");
        int progress = 0;
        for(int i = 0; i < images.length; i++) {
            String fileSrc = images[i].getPath();
            String fileDst = dirPath + File.separatorChar + i + ".png";

            try {
                cmd.run(resizeOp, fileSrc);
                im = s2b.getImage();
                pixels = im.getRGB(0, 0, width, height, null, 0, width);
                outputImage = new File(fileDst);
                ImageIO.write(im, "png", outputImage);
                nodes.add(new ImageNode(getRGBAvr(pixels), outputImage.getName()));
            }
            catch(InterruptedException ie) {ie.printStackTrace();}
            catch(IOException io) {
                log.append("IOError reading file! " + fileSrc + "\n");
//                io.printStackTrace();
            }
            catch(IM4JavaException i4) {
                log.append("Error converting image! " + fileSrc + "\n");
//                i4.printStackTrace();
            }

            if(i*100/images.length != progress) {
                progress = i*100/images.length;
                System.out.print("\r" + progress + "%");
            }
        }
        System.out.print("\r100%");
        System.out.println(
                "Processed " + nodes.size() + " in " +
                (System.currentTimeMillis()-timeStamp)/1000 + " seconds.");

        // Write images' RGB Average Data to File
        if(nodes != null && nodes.size() > 0) {
            System.out.println("Writing image data into " +
                    folder.getPath() + File.separator + "imageTiles.data");
            try {
                FileOutputStream fos = new FileOutputStream(new File(
                        folder.getPath() + File.separator + "imageTiles.data"));
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeInt(width);
                oos.writeInt(height);
                oos.writeObject(nodes);
                oos.close();
                tilesW = width;
                tilesH = height;
                System.out.println("Image Processing finalized succesfully.");
            }
            catch(FileNotFoundException fnf) {
                fnf.printStackTrace();
                log.append("Error! File not found, unable to write data.\n");
            }
            catch(IOException io) {
                io.printStackTrace();
                log.append("IOError! Unable to write data.\n");
            }
        }
        log.append("--- End of Log ---");
        System.err.println(log.toString());
    }

    /**
     * This method implements the Image Mosaic creation.
     * The method needs to get/read an image batch together with its RGB
     * average. With this information each image represents a point in a three
     * dimensional space, where it's coordinates are (R, G, B). Using a KD-Tree,
     * we store all the images as points in space. Transversing this tree, the
     * function can efficently obtain the closest n neighbors to any point.
     * This method can apply a jitter to obtain the n-th closest point and
     * obtain a more heterogeneous mosaic. The function also can apply a
     * multiply blending technic to create a better fusion of the tiles with
     * the original image.
     *
     * The method makes use of Simon Levy and Bjoern Heckel, KD-Tree and nearest
     * neighbor (NN) algorithm implementation.
     *
     * @param folder The folder containing the pre-processed images and RGB
     * average information.
     * @param name Image file name output prefix.
     * @param iScale The scale factor of the new image.
     * @param blend Blending flag. If true, the algorithm applies blending.
     * @param blendAmount The blending percent.
     * @param jitter The amount of jitter applied when finding the closest
     * neighbor.
     * @throws java.lang.OutOfMemoryError In case there's not enough memory.
     */
    public void processImageMosaic(
            File folder, String name,
            float iScale, boolean blend, float blendAmount, int jitter)
            throws OutOfMemoryError{

        ImageIO.setUseCache(false);
        if (nodes == null) {
            try {
                System.out.println("Reading image nodes...");
                FileInputStream fis = new FileInputStream(new File(
                        folder.getPath() + File.separator + "imageTiles.data"));
                ObjectInputStream ois = new ObjectInputStream(fis);
                tilesW = ois.readInt();
                tilesH = ois.readInt();
                Object obj = ois.readObject();
                if (obj instanceof LinkedList)
                    nodes = (LinkedList<ImageNode>) obj;
                else {
                    System.out.println("Error parsing tiles data file!");
                    return;
                }
                ois.close();
            }
            catch(IOException io) {io.printStackTrace();}
            catch(ClassNotFoundException cnf) {cnf.printStackTrace();}
        }
        
        if(nodes == null) {
            System.err.println("Error! Image nodes list is empty. Exiting...");
            return;
        }
        System.out.println("Image nodes ready!\nBuilding KD-Tree...");

        KDTree<File> imageTree = new KDTree<File>(3);
        ImageNode node;
        while(!nodes.isEmpty()) {
            node = nodes.pop();
            try {
                imageTree.insert(node.rgbAvg,
                        new File(folder.getPath() + File.separatorChar +
                        "mosaicTiles" + File.separatorChar + node.imageFile));
            }
            catch (KeyDuplicateException dup) {
                System.err.println("Duplicate coordinate! Skipping image...");
            }
            catch (KeySizeException kse) {kse.printStackTrace();}
        }
        System.out.println("KD-Tree ready!\nRendering mosaic...");
        
        node = null;
        nodes = null;

        int niw = (int)(canvasWidth*iScale);
        int nih = (int)(canvasHeight*iScale);

        flatten2NewLayer();
        ARGBitMap flat = layers.remove(0).getLayerImg();
        resize(niw, nih);
        mosaic(tilesW, tilesH);
        int bitmap[] = layers.remove(0).getLayerImg().bitmap;

        int r, g, b;
        BufferedImage image;
        int mosaicTile[];
        int sqW, sqH;
        int p,q;
        int m, n;
        int rndN;

        double rgb[] = new double[3];
        System.out.println(
                "Image gallery size: " + imageTree.size() +
                " images (" + tilesW + "x" + tilesH + ")px\n" +
                "Number of tiles in the image: " +
                ((niw*nih)/(tilesW*tilesH)) + '\n' +
                "Starting mosaic building process...");
        long timeStamp = System.currentTimeMillis();
        for(int i = 0; i < nih; i+=tilesH) {
            sqH = i+tilesH < nih ? i+tilesH : nih;
            for(int j = 0; j < niw; j+=tilesW) {
                m = i*niw + j;
                rgb[0] = (bitmap[m] >> 16) & byteMask;
                rgb[1] = (bitmap[m] >> 8) & byteMask;
                rgb[2] = bitmap[m] & byteMask;
                try {
                    if(jitter > 0) {
                        rndN = rnd.nextInt(jitter);
                        image = ImageIO.read(
                                (File)imageTree.nearest(rgb, rndN+1).get(0));
                    }
                    else {
                        image = ImageIO.read((File)imageTree.nearest(rgb));
                    }

                    mosaicTile = image.getRGB(0, 0, tilesW, tilesH, null, 0, tilesW);
                    if (mosaicTile != null) {
                        sqW = j+tilesW < niw ? j+tilesW : niw;
                        p = 0;
                        for(int k = i; k < sqH; k++) {
                            q = 0;
                            for(int l = j; l < sqW; l++) {
                                m = k*niw + l;
                                n = p*tilesW + q;

                                r = (mosaicTile[n] >> 16) & byteMask;
                                g = (mosaicTile[n] >> 8) & byteMask;
                                b = mosaicTile[n] & byteMask;

                                if(blend && blendAmount != 0) {
                                    r = (int)(r*(rgb[0]*blendAmount/255f +
                                            1f - blendAmount));
                                    g = (int)(g*(rgb[1]*blendAmount/255f +
                                            1f - blendAmount));
                                    b = (int)(b*(rgb[2]*blendAmount/255f +
                                            1f - blendAmount));
                                }

                                bitmap[m] =
                                        (255 << 24) | (r << 16) | (g << 8) | b;
                                q++;
                            }
                            p++;
                        }
                    }
                }
                catch (IOException io) {
                    io.printStackTrace();
                    io.getCause();
                }
                catch (KeySizeException kse) {
                    kse.printStackTrace();
                }
            }
        }
        System.out.println(
            "Done! Time elapsed: " +
            (System.currentTimeMillis() - timeStamp)/1000 + " seconds.\n" +
            "Writing file...");

        Calendar date = Calendar.getInstance();
        File mosaicOutput = new File(folder.getPath() + File.separatorChar + "mosaic-" +
                    date.get(Calendar.HOUR_OF_DAY) +
                    date.get(Calendar.MINUTE) +
                    date.get(Calendar.SECOND) + '_' +
                    name + ".png");
        try {
            BufferedImage mosaic =
                    new BufferedImage(niw, nih, BufferedImage.TYPE_INT_ARGB);
            mosaic.setRGB(0, 0, niw, nih, bitmap, 0, niw);
            ImageIO.write(mosaic, "png",
                    mosaicOutput);
            System.out.println("Succesfully wrote " + mosaicOutput.getPath());
        }
        catch (IOException io) {
            System.out.println("Error writing " + mosaicOutput.getPath());
            io.printStackTrace();
        }
        layers.add(new Layer(flat, 0, 0));
        resizeCanvas(flat.w, flat.h);
    }

    /**
     * This method returns one of the pre-defined ascii character mappings.
     * @param option The desired pre-defined mapping index.
     * @return The desired pre-defined mapping.
     */
    public char[] setAsciiMaps(int option) {
        char asciiMap[] = new char[256];
        for(int i = 0; i < 256; i ++)
            asciiMap[i] = (char)(-1);

        switch(option) {

            case 0:
                asciiMap[0] = ' ';
                asciiMap[28] = '.';
                asciiMap[56] = ':';
                asciiMap[84] = '+';
                asciiMap[112] = 'j';
                asciiMap[140] = '6';
                asciiMap[168] = 'b';
                asciiMap[196] = 'H';
                asciiMap[224] = 'M';
                break;

            case 1:
                asciiMap[0] = ' ';
                asciiMap[28] = '.';
                asciiMap[56] = ':';
                asciiMap[84] = 'c';
                asciiMap[112] = 'o';
                asciiMap[140] = 'C';
                asciiMap[168] = '0';
                asciiMap[196] = '8';
                asciiMap[224] = '@';
                break;

            case 2:
                asciiMap[0] = ' ';
                asciiMap[16] = '.';
                asciiMap[32] = ':';
                asciiMap[48] = 'i';
                asciiMap[64] = 'c';
                asciiMap[80] = 'u';
                asciiMap[96] = 'o';
                asciiMap[112] = 'e';
                asciiMap[128] = 'C';
                asciiMap[144] = 'U';
                asciiMap[160] = '0';
                asciiMap[176] = 'Q';
                asciiMap[192] = 'G';
                asciiMap[208] = 'S';
                asciiMap[224] = '8';
                asciiMap[240] = '@';
                break;

            case 3:
                asciiMap[0] = ' ';
                asciiMap[9] = '\'';
                asciiMap[27] = '.';
                asciiMap[36] = '~';
                asciiMap[45] = ':';
                asciiMap[54] = ';';
                asciiMap[63] = '!';
                asciiMap[72] = '>';
                asciiMap[81] = '+';
                asciiMap[90] = '=';
                asciiMap[99] = 'i';
                asciiMap[107] = 'c';
                asciiMap[115] = 'j';
                asciiMap[126] = 't';
                asciiMap[135] = 'J';
                asciiMap[144] = 'Y';
                asciiMap[153] = '5';
                asciiMap[162] = '6';
                asciiMap[171] = 'S';
                asciiMap[180] = 'X';
                asciiMap[189] = 'D';
                asciiMap[198] = 'Q';
                asciiMap[207] = 'K';
                asciiMap[216] = 'H';
                asciiMap[225] = 'N';
                asciiMap[234] = 'W';
                asciiMap[243] = 'M';
                break;

            default:
                asciiMap[0] = ' ';
                asciiMap[127] = '#';
        }
        return asciiMap;
    }

    /**
     * This method fills the empty gaps of a partial ascii map definition.
     * @param asciiMap The partially filled ascii character mapping.
     * @return The complete ascii character mapping.
     */
    public char[] fixAsciiMap(char[] asciiMap) {
        char lastChar = ' ';
        for(int i = 0; i < 256; i++) {
            if(asciiMap[i] == (char)(-1))
                asciiMap[i] = lastChar;
            else
                lastChar = asciiMap[i];
        }
        return asciiMap;
    }

    /**
     * This method implements the Image 2 Ascii Color filter.
     * @param mosaicRes The desired horizontal resolution.
     * @param msg The string emblemed message.
     * @param w Web-safe palette flag.
     * @return Resulting ascii string.
     */
    public String getMsgHTML(int mosaicRes, String msg, boolean w) {
        int msgIterator = 0;
        int msgSize = msg.length() - 1 ;
        int k;
        int dmosaicRes = mosaicRes*2;

        long timeStamp = System.currentTimeMillis();
        flatten2NewLayer();
        mosaic(mosaicRes, dmosaicRes);
        if(w) posterize(5);
        else posterize(8);
        int[] img = layers.remove(0).getLayerImg().bitmap;
        int[] bnw = getGrayScale();
        
        StringBuilder currImage = new StringBuilder("<PRE>");
        StringBuilder colorAsciiImage = new StringBuilder("<html>\n  <head>\n" +
                "    <title>IMP: Ascii Image Export</title>\n  </head>\n" +
                "<body style=\"background: Black;\" lang=en" +
                " leftmargin=0 topmargin=0>\n<STYLE>\n<!--\n");

        String hexColor = "";
        String allHex = "";
        int currColor, lastColor;
        lastColor = -1;
        HashSet<Integer> colors = new HashSet<Integer>();

        for(int i = 0; i < canvasHeight; i += dmosaicRes) {
            for(int j = 0; j < canvasWidth ; j += mosaicRes) {
                k = i*canvasWidth + j;

                currColor = img[k] & 0x00ffffff;
                if((currColor >> 8) == 0) allHex = "0000";
                else if ((currColor >> 16) == 0) allHex = "00";
                else allHex = "";
                
                hexColor = Integer.toHexString(currColor);
                allHex += hexColor;

                if(lastColor != currColor) {
                    if(lastColor != -1)
                        currImage.append("</span>");
                    lastColor = currColor;
                    currImage.append("<span id=\"c" + hexColor + "\">");
                    if(colors.add(currColor)) {
                        colorAsciiImage.append("  #c" + hexColor +
                                " { color: #" + allHex + "; }\n");
                    }
                }

                currImage.append(msg.charAt(msgIterator));
                if(msgIterator < msgSize) msgIterator++;
                else msgIterator = 0;
            }
            currImage.append("</span>\n");
            lastColor = -1;
        }
        System.out.println(
            "Done! Time elapsed:" +
            (System.currentTimeMillis() - timeStamp) + "ms.");
        return "" + colorAsciiImage + "  // -->\n</STYLE>\n" +
                currImage + "  </PRE>\n</body>\n</html>";
    }

    /**
     * This method implements the Image 2 Ascii Color filter.
     * @param mosaicRes The desired horizontal resolution.
     * @param asciiMap The ascii character mapping.
     * @param w Web-safe palette flag.
     * @return Resulting ascii string.
     */
    public String getColorAsciiHTML(int mosaicRes, char[] asciiMap, boolean w) {
        int k;
        int dmosaicRes = mosaicRes*2;

        long timeStamp = System.currentTimeMillis();
        flatten2NewLayer();
        mosaic(mosaicRes, dmosaicRes);
        if(w) posterize(5);
        else posterize(8);
        int[] img = layers.remove(0).getLayerImg().bitmap;
        int[] bnw = getGrayScale();

        StringBuilder currImage = new StringBuilder("<PRE>");
        StringBuilder colorAsciiImage = new StringBuilder("<html>\n  <head>\n" +
                "    <title>IMP: Ascii Image Export</title>\n  </head>\n" +
                "<body style=\"background: Black;\" lang=en" +
                " leftmargin=0 topmargin=0>\n<STYLE>\n<!--\n");

        String hexColor = "";
        String allHex = "";
        int currColor, lastColor;
        lastColor = -1;
        HashSet<Integer> colors = new HashSet<Integer>();

        for(int i = 0; i < canvasHeight; i += dmosaicRes) {
            for(int j = 0; j < canvasWidth ; j += mosaicRes) {
                k = i*canvasWidth + j;

                currColor = img[k] & 0x00ffffff;
                if((currColor >> 8) == 0) allHex = "0000";
                else if ((currColor >> 16) == 0) allHex = "00";
                else allHex = "";

                hexColor = Integer.toHexString(currColor);
                allHex += hexColor;

                if(lastColor != currColor) {
                    if(lastColor != -1)
                        currImage.append("</span>");
                    lastColor = currColor;
                    currImage.append("<span id=\"c" + hexColor + "\">");
                    if(colors.add(currColor)) {
                        colorAsciiImage.append("  #c" + hexColor +
                                " { color: #" + allHex + "; }\n");
                    }
                }

                currImage.append(asciiMap[bnw[k]]);
            }
            currImage.append("</span>\n");
            lastColor = -1;
        }
        System.out.println(
            "Done! Time elapsed:" +
            (System.currentTimeMillis() - timeStamp) + "ms.");
        return "" + colorAsciiImage + "  // -->\n</STYLE>\n" + currImage +
                "  </PRE>\n</body>\n</html>";
    }

    /**
     * This method implements the Image 2 Ascii B/W filter.
     * @param mosaicRes The desired horizontal resolution.
     * @param asciiMap The ascii character mapping.
     * @param w Web-safe palette flag.
     * @return Resulting ascii string.
     */
    public String getAsciiHTML(int mosaicRes, char[] asciiMap, boolean w) {
        int k;
        int currColor;
        int dmosaicRes = mosaicRes*2;

        long timeStamp = System.currentTimeMillis();
        flatten2NewLayer();
        if(w)
            posterize(5);
        mosaic(mosaicRes, dmosaicRes);

        int[] img = layers.remove(0).getLayerImg().bitmap;
        int[] bnw = getGrayScale();

        String color = "";
        String tmpColor;
        StringBuilder currImage = new StringBuilder("<PRE>");
        StringBuilder colorAsciiImage = new StringBuilder("<html>\n  <head>\n" +
                "    <title>IMP: Ascii Image Export</title>\n  </head>\n" +
                "<body style=\"background: Black; color: White;\" lang=en" +
                " leftmargin=0 topmargin=0>\n<STYLE>\n<!--\n");

        for(int i = 0; i < canvasHeight; i += dmosaicRes) {
            for(int j = 0; j < canvasWidth ; j += mosaicRes) {
                k = i*canvasWidth + j;
                currImage.append(asciiMap[bnw[k]]);
            }
            currImage.append('\n');
        }
        System.out.println(
            "Done! Time elapsed:" +
            (System.currentTimeMillis() - timeStamp) + "ms.");
        return "" + colorAsciiImage + "  // -->\n</STYLE>\n" + currImage +
                "  </PRE>\n</body>\n</html>";
    }

    void printARGB(int argb) {
        System.out.println(
                "A: " + ((argb >> 24) & byteMask) +
                " R: " + ((argb >> 16) & byteMask) +
                " G: " + ((argb >> 8) & byteMask) +
                " B: " + (argb & byteMask));
    }
}