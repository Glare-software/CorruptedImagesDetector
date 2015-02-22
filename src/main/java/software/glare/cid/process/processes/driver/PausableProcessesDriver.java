package software.glare.cid.process.processes.driver;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.process.ProgressData;
import software.glare.cid.process.processes.PausableCallable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Created by fdman on 07.07.2014.
 */
public class PausableProcessesDriver implements IPausableProcessesDriver {
    private static final long PROCESSING_WAIT_MS = 100;
    protected ProcessDriverState processesDriverState = ProcessDriverState.STOPPED;
    private final List<PausableCallable<?>> callables;
    private final List<Future> callablesFutures;
    private final ExecutorService resultGetterExecutorService;
    private final Consumer<Void> onFinish;
    private final Consumer<Void> onCancel;
    private final Consumer<ProgressData> refreshProgress;
    private final Logger log = LoggerFactory
            .getLogger(PausableProcessesDriver.class);
    private volatile boolean scanCancelled = false;

    public PausableProcessesDriver(List<PausableCallable<?>> pausableCallables, Consumer<Void> onFinish, Consumer<Void> onCancel, Consumer<ProgressData> refreshProgress) {
        resultGetterExecutorService = Executors.newFixedThreadPool(pausableCallables.size(), new BasicThreadFactory.Builder().namingPattern("resultGetterExecutorService - %d").build());
        callablesFutures = new ArrayList<>(pausableCallables.size());
        this.callables = pausableCallables;
        this.onFinish = onFinish;
        this.onCancel = onCancel;
        this.refreshProgress = refreshProgress;

    }

    @Override
    public void startProcesses() {

        if (isAnyProcessInProgress()) {
            throw new IllegalStateException("Unable to start processes. Process already in progress");
        }

        synchronized (this) {
            if (processesDriverState == ProcessDriverState.STOPPED) {
                processesDriverState = ProcessDriverState.RUN;

                callables.forEach(pausableCallable -> {
                    Future objectFuture = resultGetterExecutorService.submit(pausableCallable);
                    callablesFutures.add(objectFuture);
                });


            } else {
                throw new IllegalStateException("Unable to start processes. Process is in state " + processesDriverState);
            }
        }

        new Thread(() -> {
            while (isAnyProcessInProgress()) {
                try {
                    StringBuilder sb = new StringBuilder();
                    Double d = 0d;
                    for (PausableCallable<?> callable : this.callables) {
                        //sb.append(callable.getName()).append(": ").append(callable.getProgress().getInfo()).append(" ");
                        sb.append(callable.getProgress().getInfo()).append(" ");
                        d += callable.getProgress().getTotal();
                        callable.getProgress();
                    }
                    refreshProgress.accept(new ProgressData(d, sb.toString()));// apply(new ProgressData(d, sb.toString()));
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                    processesDriverState = ProcessDriverState.STOPPED;
                    scanCancelled = true;
                    onCancel.accept(null);
                    return;
                }
            }
            if (!scanCancelled) {
                onFinish.accept(null);
            }
            processesDriverState = ProcessDriverState.STOPPED;
            resultGetterExecutorService.shutdown();
        }, "PausableProcessesDriver - interrupt/finish watcher").start();
    }

    @Override
    public void pauseProcesses() {
        synchronized (this) {
            if (processesDriverState == ProcessDriverState.RUN) {
                callables.forEach(PausableCallable::pause);
                processesDriverState = ProcessDriverState.PAUSED;
            } else {
                throw new IllegalStateException("Process is in state " + processesDriverState);
            }
        }
    }

    /**
     * That method is also used in case when user cancel paused processes.
     * Therefore we must to unlock processes before really interrupting them
     */
    public void unpauseProcesses() {
        synchronized (this) {
            if (processesDriverState == ProcessDriverState.PAUSED) {
                /*if (waitForUnlock) {
                    callables.forEach(pausableCallable -> {
                        while (pausableCallable.isPaused()) {
                            pausableCallable.unpause();
                        }
                    });
                } else {
                    callables.forEach(pausableCallable -> pausableCallable.unpause());
                }        */
                callables.forEach(PausableCallable::unpause);
                processesDriverState = ProcessDriverState.RUN;
            } else {
                throw new IllegalStateException("Unable to unpause processes. Process is in state " + processesDriverState);
            }
        }
    }

    @Override
    public void cancelProcesses() {
        synchronized (this) {
            if (processesDriverState == ProcessDriverState.PAUSED) { //resume before interrupt! And wait till processes became really unlocked
                unpauseProcesses();
            }
        }

        resultGetterExecutorService.shutdownNow();
        /*resultGetterExecutorService.shutdown();
        try {
            resultGetterExecutorService.awaitTermination(PROCESSING_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("processorExecService interrupted {} {} {}", PROCESSING_WAIT_MS, TimeUnit.MILLISECONDS, ExceptionUtils.getStackTrace(e));
        } finally {
            resultGetterExecutorService.shutdownNow();
        } */

        processesDriverState = ProcessDriverState.STOPPED;
        scanCancelled = true;
        onCancel.accept(null);
    }

    @Override
    public synchronized boolean isAnyProcessInProgress() {
        long activeProcesses = callablesFutures.stream().filter(objectFuture -> (!(objectFuture.isDone() || objectFuture.isCancelled()))).count();
        return activeProcesses > 0;
    }
}

enum ProcessDriverState {
    RUN,
    PAUSED,
    STOPPED
}