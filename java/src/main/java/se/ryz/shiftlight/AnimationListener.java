package se.ryz.shiftlight;

import java.util.EventListener;

public interface AnimationListener extends EventListener {
    void animationChanged(AnimationEvent event);
}

