package software.glare.cid.process.processes;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.FileType;
import software.glare.cid.process.ProgressData;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: fdman
 * Date: 01.12.13
 */
public class TotalFilesCounter extends PausableCallable {
    private final Logger log = LoggerFactory
            .getLogger(TotalFilesCounter.class);
    private AtomicLong filesTotal = new AtomicLong(0);
    private final String scanFolder;
    private final Set<FileType> fileTypes;


    public TotalFilesCounter(String scanFolder, Set<FileType> fileTypes) {
        this.scanFolder = scanFolder;
        this.fileTypes = fileTypes;
    }


    @Override
    public Object call() {
        log.trace("TotalFilesCounter STARTED");
        long start = Calendar.getInstance().getTimeInMillis();

        filesTotal.set(0L);
        try {
            Files.walkFileTree(new File(scanFolder).toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    while (!Thread.interrupted()) {
                        pauseIfNeeded();
                        File file = path.toFile();
                        if (file.exists() && file.isFile() && file.canRead()
                                && isFileExtensionMatchFilter(FilenameUtils.getExtension(file.getName().toLowerCase()))) {
                            filesTotal.incrementAndGet();
                        }
                        return super.visitFile(path, attrs);
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

            });
        } catch (IOException e) {
            log.error("{}", ExceptionUtils.getStackTrace(e));
        }
        Long delta = Calendar.getInstance().getTimeInMillis() - start;
        log.trace("TotalFilesCounter FINISHED. Total time: {} ms. Accepted files: {}", delta, filesTotal);
        getNextFinishManager().setCanFinishFlag(true);
        return true;
    }

    @Override
    public ProgressData getProgress() {
        return new ProgressData(filesTotal.doubleValue(), "");
    }
}

