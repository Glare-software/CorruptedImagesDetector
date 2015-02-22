package software.glare.cid.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.Constants;
import software.glare.cid.FileType;
import software.glare.cid.process.processes.*;
import software.glare.cid.process.processes.driver.PausableProcessesDriver;
import software.glare.cid.process.processes.processor.algorithm.IAlgorithm;
import software.glare.cid.process.processes.processor.result.BytesProcessResult;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Driver - mediator of a pausable processes
 * Created by fdman on 19.07.2014.
 */
public class ScanPerformer {
    private final PausableProcessesDriver pausableProcessesDriver;


    public ScanPerformer(String folderPath, Set<FileType> fileTypes, Class<? extends IAlgorithm> algorithmClass, Report report, Consumer<Void> onFinish, Consumer<Void> onCancel, Consumer<ProgressData> refreshProgress) {

        BlockingQueue<Map<File, byte[]>> filesQueue = new ArrayBlockingQueue<>(Constants.INPUT_QUEUE_SIZE_NUM);
        LinkedBlockingDeque<Future<BytesProcessResult>> algorithmResultsDeque = new LinkedBlockingDeque<>(Constants.INPUT_QUEUE_SIZE_NUM);
        List<PausableCallable<?>> pausableCallables = new LinkedList<>();
        PausableCallable<?> zero = new TotalFilesCounter(folderPath, fileTypes);
        PausableCallable<?> first = new FilesToQueueScanner(filesQueue, folderPath, fileTypes);
        PausableCallable<?> second = new BrokenImagesDetector(Constants.CPU_CORES_NUM, filesQueue, algorithmResultsDeque, algorithmClass);
        PausableCallable<?> third = new ResultsToReportConverter(algorithmResultsDeque, report);

        IFinishManager zeroManager = new CommonFinishManager("TotalFilesCounter");
        IFinishManager firstManager = new CommonFinishManager("FilesToQueueScanner");
        IFinishManager secondManager = new CommonFinishManager("BrokenImagesDetector");
        IFinishManager thirdManager = new CommonFinishManager("ResultsToReportConverter");

        zero.setFinishManager(zeroManager);
        zero.setNextFinishManager(firstManager);
        first.setFinishManager(firstManager);
        first.setNextFinishManager(secondManager);
        second.setFinishManager(secondManager);
        second.setNextFinishManager(thirdManager);
        third.setFinishManager(thirdManager);
        third.setNextFinishManager(null);

        pausableCallables.add(zero);
        pausableCallables.add(first);
        pausableCallables.add(second);
        pausableCallables.add(third);

        pausableProcessesDriver = new PausableProcessesDriver(pausableCallables, onFinish, onCancel, refreshProgress);
    }

    public synchronized void performScan() {
        pausableProcessesDriver.startProcesses();
    }

    public synchronized void cancelScan() {
        pausableProcessesDriver.cancelProcesses();
    }

    public synchronized void pauseScan() {
        pausableProcessesDriver.pauseProcesses();
    }

    public synchronized void unpauseScan() {
        pausableProcessesDriver.unpauseProcesses();
    }

    private class CommonFinishManager implements IFinishManager {
        private final Logger log = LoggerFactory
                .getLogger(CommonFinishManager.class);
        private final String name;

        private AtomicBoolean flag = new AtomicBoolean(false);

        private CommonFinishManager(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean getCanFinishFlag() {
            //log.debug("{} get canFinishFlag is {}", getName(), flag);
            return flag.get();
        }

        @Override
        public void setCanFinishFlag(boolean flag) {
            //log.debug("{} set canFinishFlag to {}", getName(), flag);
            this.flag.set(flag);
        }
    }

}
