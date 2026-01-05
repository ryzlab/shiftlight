package se.ryz.shiftlight;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Animation {
    public static final int MAX_IMAGES = 80;
    
    private List<Image> images;

    public Animation() {
        this.images = new ArrayList<>();
    }

    /**
     * Adds an Image to the Animation by parsing a CSV line.
     * Throws IllegalArgumentException if the maximum number of images is reached.
     * 
     * @param csvLine CSV line in the format: [1,2,4-6,8],startRPM,endRPM,startRed,startGreen,startBlue,endRed,endGreen,endBlue,blinkMode
     * @throws IllegalArgumentException if maximum images reached or CSV parsing fails
     */
    public void add(String csvLine) {
        if (images.size() >= MAX_IMAGES) {
            throw new IllegalArgumentException("Maximum number of images (" + MAX_IMAGES + ") reached");
        }
        
        Image image = new Image(csvLine);
        images.add(image);
    }

    /**
     * Removes an Image from the Animation by parsing the CSV line and finding a matching Image.
     * 
     * @param csvLine CSV line in the format: [1,2,4-6,8],startRPM,endRPM,startRed,startGreen,startBlue,endRed,endGreen,endBlue,blinkMode
     * @return true if an Image was removed, false otherwise
     */
    public boolean remove(String csvLine) {
        Image imageToRemove = new Image(csvLine);
        Iterator<Image> iterator = images.iterator();
        
        while (iterator.hasNext()) {
            Image image = iterator.next();
            if (image.equals(imageToRemove)) {
                iterator.remove();
                return true;
            }
        }
        
        return false;
    }

    /**
     * Clears all Images from the Animation.
     */
    public void clear() {
        images.clear();
    }

    /**
     * Gets the number of Images in the Animation.
     * 
     * @return the number of Images
     */
    public int size() {
        return images.size();
    }

    /**
     * Gets the list of Images in the Animation.
     * 
     * @return a copy of the list of Images
     */
    public List<Image> getImages() {
        return new ArrayList<>(images);
    }

    /**
     * Checks if the Animation is empty.
     * 
     * @return true if the Animation contains no Images
     */
    public boolean isEmpty() {
        return images.isEmpty();
    }
}

