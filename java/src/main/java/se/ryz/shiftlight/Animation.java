package se.ryz.shiftlight;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Animation {
    public static final int MAX_IMAGES = 80;
    
    private List<Image> images;
    private final List<AnimationListener> listeners;

    public Animation() {
        this.images = new ArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Adds an Image to the Animation by parsing a CSV line.
     * Throws IllegalArgumentException if the maximum number of images is reached.
     * Emits an IMAGE_ADDED event to all registered listeners.
     * 
     * @param csvLine CSV line in the format: [1,2,4-6,8],startRPM,endRPM,startRed,startGreen,startBlue,endRed,endGreen,endBlue,blinkMode
     * @throws IllegalArgumentException if maximum images reached or CSV parsing fails
     */
    public void add(String csvLine) {
        add(csvLine, null);
    }

    public void add(String csvLine, VariableParser variableParser) {
        if (images.size() >= MAX_IMAGES) {
            throw new IllegalArgumentException("Maximum number of images (" + MAX_IMAGES + ") reached");
        }
        
        Image image = variableParser != null ? new Image(csvLine, variableParser) : new Image(csvLine);
        int index = images.size();
        images.add(image);
        fireAnimationEvent(new AnimationEvent(this, AnimationEvent.EventType.IMAGE_ADDED, image, index));
    }

    /**
     * Removes an Image from the Animation by parsing the CSV line and finding a matching Image.
     * Emits an IMAGE_REMOVED event to all registered listeners if an image was removed.
     * 
     * @param csvLine CSV line in the format: [1,2,4-6,8],startRPM,endRPM,startRed,startGreen,startBlue,endRed,endGreen,endBlue,blinkMode
     * @return true if an Image was removed, false otherwise
     */
    public boolean remove(String csvLine) {
        Image imageToRemove = new Image(csvLine);
        Iterator<Image> iterator = images.iterator();
        int index = 0;
        
        while (iterator.hasNext()) {
            Image image = iterator.next();
            if (image.equals(imageToRemove)) {
                iterator.remove();
                fireAnimationEvent(new AnimationEvent(this, AnimationEvent.EventType.IMAGE_REMOVED, image, index));
                return true;
            }
            index++;
        }
        
        return false;
    }

    /**
     * Removes an Image from the Animation by reference.
     * Emits an IMAGE_REMOVED event to all registered listeners if an image was removed.
     * 
     * @param imageToRemove the Image to remove
     * @return true if an Image was removed, false otherwise
     */
    public boolean remove(Image imageToRemove) {
        Iterator<Image> iterator = images.iterator();
        int index = 0;
        
        while (iterator.hasNext()) {
            Image image = iterator.next();
            if (image == imageToRemove || image.equals(imageToRemove)) {
                iterator.remove();
                fireAnimationEvent(new AnimationEvent(this, AnimationEvent.EventType.IMAGE_REMOVED, image, index));
                return true;
            }
            index++;
        }
        
        return false;
    }

    /**
     * Clears all Images from the Animation.
     * Emits an ANIMATION_CLEARED event to all registered listeners.
     */
    public void clear() {
        if (!images.isEmpty()) {
            images.clear();
            fireAnimationEvent(new AnimationEvent(this, AnimationEvent.EventType.ANIMATION_CLEARED));
        }
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

    /**
     * Adds an AnimationListener to receive events when the Animation changes.
     * 
     * @param listener the listener to add
     */
    public void addAnimationListener(AnimationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an AnimationListener so it no longer receives events.
     * 
     * @param listener the listener to remove
     */
    public void removeAnimationListener(AnimationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fires an AnimationEvent to all registered listeners.
     * 
     * @param event the event to fire
     */
    private void fireAnimationEvent(AnimationEvent event) {
        for (AnimationListener listener : listeners) {
            listener.animationChanged(event);
        }
    }

    /**
     * Generates a formatted string with evaluated CSV lines.
     * The output starts with "BEGIN" and ends with "END", with evaluated CSV lines in between.
     * 
     * @return formatted string with BEGIN/END markers and evaluated CSV lines
     */
    public String generateProgramOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN\n");
        
        for (Image image : images) {
            // Use toCsvLine() which returns the evaluated CSV (with numeric values)
            sb.append(image.toCsvLine()).append("\n");
        }
        
        sb.append("END");
        return sb.toString();
    }
}

