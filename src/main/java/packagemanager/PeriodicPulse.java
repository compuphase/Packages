/* 
 * "Borrowed code"
 */
package packagemanager;

/**
 * https://edencoding.com/periodic-background-tasks/
 * @author Ed Eden-Rump
 */
import javafx.animation.AnimationTimer;
public abstract class PeriodicPulse extends AnimationTimer {
    long nanosBetweenPulses;
    long lastPulseTimeStamp;
    public PeriodicPulse(double secondsBetweenPulses){
        //if negative time, default to 0.5 seconds.
        if(secondsBetweenPulses < 0) secondsBetweenPulses = 500000L;
        //convert seconds to nanos;
        nanosBetweenPulses = (long) (secondsBetweenPulses * 1000000000L);
    }
    @Override
    public void handle(long now) {
        //calculate time since last pulse in nanoseconds
        long nanosSinceLastPulse = now - lastPulseTimeStamp;
        //work out whether to fire another pulse
        if(nanosSinceLastPulse > nanosBetweenPulses){
            //reset timestamp
            lastPulseTimeStamp = now;
            //execute user's code
            run();
        }
    }
    abstract void run();
}
