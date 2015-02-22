package software.glare.cid.process.processes.driver;

/**
 * Created by fdman on 17.02.14.
 * can sunchronous manage by a pausable threads
 */

public interface IPausableProcessesDriver {

    void startProcesses();

    void pauseProcesses();

    void unpauseProcesses();

    void cancelProcesses();

    boolean isAnyProcessInProgress();
}
