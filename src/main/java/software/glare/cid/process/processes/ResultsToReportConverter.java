package software.glare.cid.process.processes;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.Constants;
import software.glare.cid.process.ProgressData;
import software.glare.cid.process.Report;
import software.glare.cid.process.processes.processor.result.BytesProcessResult;

import java.util.Calendar;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by fdman on 07.07.2014.
 */
public class ResultsToReportConverter extends PausableCallable {
    public static final int NOT_READY_RESULTS_DEQUE_MAX_RECOMMENDED_SIZE = Constants.INPUT_QUEUE_SIZE_NUM / 2;
    private final Logger log = LoggerFactory
            .getLogger(ResultsToReportConverter.class);
    //private final AtomicInteger linesAdded = new AtomicInteger(0);


    private final AtomicInteger maxNotReadyResultsSize = new AtomicInteger(0);

    /**
     * В этот дек попадают все Future результаты
     */
    private final LinkedBlockingDeque<Future<BytesProcessResult>> futureAlgorithmResults;

    /**
     * В этот дек попадают все Future результаты которые не успели выполниться, дек не ограничен размером, потенциально возможно разрастание (установим ограничение NOT_READY_RESULTS_DEQUE_MAX_RECOMMENDED_SIZE).
     */
    private final LinkedBlockingDeque<Future<BytesProcessResult>> futureNotReadyAlgorithmResults = new LinkedBlockingDeque<>();

    private final Report report;

    /**
     * @param futureAlgorithmResults - список, куда будут приходить результаты
     * @param report                 - отчет, который нужно наполнить результатами
     */
    public ResultsToReportConverter(LinkedBlockingDeque<Future<BytesProcessResult>> futureAlgorithmResults, Report report) {
        this.futureAlgorithmResults = futureAlgorithmResults;
        this.report = report;
    }


    @Override
    public Object call() throws Exception {
        log.trace("ResultsToReportConverter STARTED");
        long start = Calendar.getInstance().getTimeInMillis();
        processResultsAndFillReport();
        Long delta = Calendar.getInstance().getTimeInMillis() - start;
        log.trace("ResultsToReportConverter FINISHED. Total time: {} ms. maxNotReadyResultsSize is {}", delta, maxNotReadyResultsSize.intValue());
        return true;
    }

    private void processResultsAndFillReport() {
        ExecutorService notReadyDequeResultsToReportAppenderExecutorService = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("NotReadyDequeResultsToReportAppenderThread-%d").build());
        notReadyDequeResultsToReportAppenderExecutorService.submit(new NotReadyDequeResultsToReportAppender());
        while (!Thread.interrupted()) {
            try {
                pauseIfNeeded();
                if (futureAlgorithmResults.peekFirst() != null) {
                    Future<BytesProcessResult> bytesProcessResultFuture = futureAlgorithmResults.takeFirst();
                    if (bytesProcessResultFuture != null) {
                        if (bytesProcessResultFuture.isDone() || bytesProcessResultFuture.isCancelled()) {
                            try {
                                BytesProcessResult bytesProcessResult = bytesProcessResultFuture.get();
                                if (bytesProcessResult != null) {
                                    report.addLine(bytesProcessResult);
                                    //linesAdded.incrementAndGet();
                                } else {
                                    log.warn("Adding null result has benn detected");
                                }

                            } catch (ExecutionException e) {
                                log.error("futureAlgorithmResults ExecutionException:\n{}", ExceptionUtils.getStackTrace(e));
                            }
                        } else {
                            while (!Thread.interrupted()) {
                                if (futureNotReadyAlgorithmResults.size() < NOT_READY_RESULTS_DEQUE_MAX_RECOMMENDED_SIZE) {
                                    futureNotReadyAlgorithmResults.putLast(bytesProcessResultFuture);
                                    break;
                                }
                            }
                            /*if (Thread.interrupted()) {
                                break;
                            }*/
                        }
                    } else {
                        log.error("Some went wrong. Took null element from base deque");
                    }
                }
                if (Thread.interrupted() || (getFinishManager().getCanFinishFlag() && futureAlgorithmResults.peekFirst() == null && futureNotReadyAlgorithmResults.peekFirst() == null)) {
                    break;
                }
                if (maxNotReadyResultsSize.intValue() < futureNotReadyAlgorithmResults.size()) {
                    maxNotReadyResultsSize.set(futureNotReadyAlgorithmResults.size());
                }
            } catch (InterruptedException e) {
                break;
            }
        }
        notReadyDequeResultsToReportAppenderExecutorService.shutdownNow();
        //log.info("RTRC cnt: {}", linesAdded.get());

    }

    @Override
    public ProgressData getProgress() {
        return new ProgressData(
                (new Integer(futureAlgorithmResults.size() + futureNotReadyAlgorithmResults.size()).doubleValue()),
                /*"q1 " + futureAlgorithmResults.size() +"q2 "+futureNotReadyAlgorithmResults.size()*/"");
    }

    private class NotReadyDequeResultsToReportAppender implements Runnable {

        @Override
        public void run() {
            processDeque();
        }

        private void processDeque() {
            while (!Thread.interrupted()) {
                try {
                    if (futureNotReadyAlgorithmResults.peekFirst() != null) {
                        Future<BytesProcessResult> bytesProcessResultFuture = futureNotReadyAlgorithmResults.takeFirst();
                        if (bytesProcessResultFuture != null) {
                            if (bytesProcessResultFuture.isDone() || bytesProcessResultFuture.isCancelled()) {
                                try {
                                    if (!Thread.interrupted()) {
                                        report.addLine(bytesProcessResultFuture.get());
                                        //linesAdded.incrementAndGet();
                                    }
                                } catch (ExecutionException e) {
                                    log.error("futureAlgorithmResults ExecutionException:\n{}", ExceptionUtils.getStackTrace(e));
                                }
                            } else {
                                futureNotReadyAlgorithmResults.putLast(bytesProcessResultFuture);
                            }
                        } else {
                            log.error("Some went wrong. Took null element from priority deque");
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
