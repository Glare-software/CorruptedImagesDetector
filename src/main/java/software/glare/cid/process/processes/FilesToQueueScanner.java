package software.glare.cid.process.processes;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.FileType;
import software.glare.cid.process.ProgressData;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: fdman
 * Date: 01.12.13
 */
public class FilesToQueueScanner extends PausableCallable {
    private final Logger log = LoggerFactory
            .getLogger(FilesToQueueScanner.class);

    private final BlockingQueue<Map<File, byte[]>> queue;
    private final String scanFolder;
    private final Set<FileType> fileTypes;
    private String currentScannedFile = "";
    private AtomicLong filesTotal = new AtomicLong(0);


    public FilesToQueueScanner(BlockingQueue<Map<File, byte[]>> queue, String scanFolder, Set<FileType> fileTypes) {
        this.queue = queue;
        this.scanFolder = scanFolder;
        this.fileTypes = fileTypes;
    }


    @Override
    public Object call() {
        log.info("FilesToQueueScanner STARTED");
        long start = Calendar.getInstance().getTimeInMillis();
        filesTotal.set(0L);
        try {
            FileVisitor<Path> myFilesVisitor = new MyFilesVisitor<>();
            Files.walkFileTree(new File(scanFolder).toPath(), myFilesVisitor);
        } catch (IOException e) {
            log.error("{}", ExceptionUtils.getStackTrace(e));
        }
        Long delta = Calendar.getInstance().getTimeInMillis() - start;
        log.info("FilesToQueueScanner FINISHED. Total time: {} ms. Accepted files: {}", delta, filesTotal);
        getNextFinishManager().setCanFinishFlag(true);
        return true;
    }

    @Override
    public ProgressData getProgress() {
        return new ProgressData(-filesTotal.doubleValue(), currentScannedFile);
    }

    private class MyFilesVisitor<T extends Path> extends SimpleFileVisitor<T> {
        private final Logger log = LoggerFactory
                .getLogger(MyFilesVisitor.class);


        @Override
        public FileVisitResult visitFile(T path, BasicFileAttributes attrs)
                throws IOException {
            while (!Thread.interrupted()) {
                File file = path.toFile();
                try {
                    pauseIfNeeded();
                    //log.trace("Visited file {}", file.getName());
                    if (file.exists() && file.isFile() && file.canRead()
                            && isFileExtensionMatchFilter(FilenameUtils.getExtension(file.getName().toLowerCase()))) {
                        //log.trace("Begin add file {} to queue", file.getName());
                        byte[] bytes = Files.readAllBytes(path);
                        filesTotal.incrementAndGet();
                        queue.put(Collections.unmodifiableMap(Collections.singletonMap(file, bytes)));
                        //log.trace("End add file {} to queue", file.getName());
                        currentScannedFile = file.getAbsolutePath();
                    } else {
                        log.trace("File {} skipped", file.getName());
                    }
                    return super.visitFile(path, attrs);
                } catch (InterruptedException | ClosedByInterruptException e) {
                    log.warn("Scan thread was interrupted. End at file {} (skipped)", file.getName());
                    return FileVisitResult.TERMINATE;
                }
            }
            log.warn("Scan thread was interrupted");
            return FileVisitResult.TERMINATE;
        }

        private boolean isFileExtensionMatchFilter(String extensionLowerCase) {
            return fileTypes.stream().filter(
                    fileType -> Arrays.asList(fileType.getExtensions()).stream().filter(
                            s -> s.toLowerCase().equals(extensionLowerCase)).count() > 0
            ).count() > 0;
        }
    }

}

