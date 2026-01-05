package se.ryz.shiftlight;

import java.util.EventObject;

public class AnimationEvent extends EventObject {
    public enum EventType {
        IMAGE_ADDED,
        IMAGE_REMOVED,
        ANIMATION_CLEARED
    }

    private final EventType eventType;
    private final Image image;
    private final int index;

    public AnimationEvent(Animation source, EventType eventType, Image image, int index) {
        super(source);
        this.eventType = eventType;
        this.image = image;
        this.index = index;
    }

    public AnimationEvent(Animation source, EventType eventType) {
        this(source, eventType, null, -1);
    }

    public EventType getEventType() {
        return eventType;
    }

    public Image getImage() {
        return image;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public Animation getSource() {
        return (Animation) super.getSource();
    }
}

