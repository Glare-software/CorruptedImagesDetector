package software.glare.cid.process.processes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.process.ProgressData;

import java.util.concurrent.Callable;

/**
 * Created by fdman on 06.07.2014.
 */
public abstract class PausableCallable<T> implements Callable<T>, IPausable {

    private final Logger log = LoggerFactory.getLogger(PausableCallable.class);

    private volatile boolean paused;

    private IFinishManager finishManager;
    private IFinishManager nextFinishManager;

    protected PausableCallable() {
    }

    /**
     * Use that method at any safe place where U can pause your thread
     */
    protected void pauseIfNeeded() {
        //log.trace("{} pauseIfNeeded check", getName());
        try {
            if (paused) {
                synchronized (this) {
                    while (paused) {
                        //log.trace("{} paused", getName());
                        wait();
                        //log.trace("{} unpaused", getName());
                    }
                    paused = false;
                }
            }
        } catch (InterruptedException ignored) {

            //For interrupt unset the pause flag. Also if u wish to cancel the process
        }
    }

    @Override
    public void unpause() {
        //log.trace("Runnable {} unpaused", getName());
        paused = false;
        synchronized (this) {
            //log.trace("Runnable {} unpaused - before notify", getName());
            notify();
            //log.trace("Runnable {} unpaused - after notify", getName());
        }
    }

    @Override
    public synchronized void pause() {
        paused = true;
        //log.trace("{} paused flag was set", getName());
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public abstract ProgressData getProgress();

    public IFinishManager getFinishManager() {
        return finishManager;
    }

    public void setFinishManager(IFinishManager finishManaged) {
        this.finishManager = finishManaged;
    }

    public IFinishManager getNextFinishManager() {
        return nextFinishManager;
    }

    public void setNextFinishManager(IFinishManager nextFinishManager) {
        this.nextFinishManager = nextFinishManager;
    }


}


