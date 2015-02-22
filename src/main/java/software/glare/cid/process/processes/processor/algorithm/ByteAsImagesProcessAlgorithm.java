package software.glare.cid.process.processes.processor.algorithm;


import it.tidalwave.imageio.nef.NEFImageReader;
import it.tidalwave.imageio.raw.RAWImageReadParam;
import it.tidalwave.imageio.raw.Source;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.Status;
import software.glare.cid.process.processes.processor.result.BytesProcessResult;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: fdman
 * Date: 22.12.13
 */
public class ByteAsImagesProcessAlgorithm implements IAlgorithm {
    private static final AtomicLong counter = new AtomicLong(0);
    private final long id;
    public static final long BYTE_ARRAY_ANALYZE_GAP_DETECTION_THRESHOLD = 3846;

    private final Logger log = LoggerFactory
            .getLogger(ByteAsImagesProcessAlgorithm.class);

    public ByteAsImagesProcessAlgorithm() {
        id = counter.getAndIncrement();
    }

    @Override
    public String toString() {
        return "ByteAsImagesProcessAlgorithm{" +
                "path=" + path +
                '}';
    }

    private byte[] bytes;
    private File file;
    private Path path;

    @Override
    public void setData(byte[] bytes, File file) {
        //log.trace("{} Set file {} ({} bytes)", id, file.getName(), bytes == null ? 0 : bytes.length);
        if (this.bytes != null || this.file != null) {
            throw new IllegalArgumentException("Previous data was not cleared or new data is null " + file.getName());
        }
        this.bytes = bytes;
        this.file = file;
        this.path = file.toPath();
    }

    @Override
    public void clearData() {
        //log.trace("{} Temporary data cleared for {}", id, file == null ? "<nofile>" : file.getName());
        this.bytes = null;
        this.file = null;
        this.path = null;
    }

    public synchronized BytesProcessResult doWork() {
        BytesProcessResult result = new BytesProcessResult(path, "root result");
        result.setStatus(Status.OK);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            result.addChildResult(tryToReadMetaInfo(bais));
            switch (FilenameUtils.getExtension(file.getName().toUpperCase())) {
                case "NEF":
                    result.addChildResult(readNEF(bais));
                    break;
                case "JPEG":
                case "JPG":
                case "GIF":
                case "CID":
                    result.addChildResult(readAsCommonImage());
                    break;
            }
            if (result.getStatus() != Status.OK) {
                if (BYTE_ARRAY_ANALYZE_GAP_DETECTION_THRESHOLD >= bytes.length) {
                    result.setDescription("Gaps analyzer skipped cause image size is smaller than gap detection threshold");
                    result.setStatus(Status.SKIPPED);
                } else {
                    result.addChildResult(analyseBytesForGaps());
                }
            }
        } catch (IOException e) {
            result.setDescription(ExceptionUtils.getStackTrace(e));
            result.setDetails(ExceptionUtils.getStackTrace(e));
            result.setStatus(Status.SMTH_GOES_WRONG);
        }
        return result;
    }

    protected BytesProcessResult readAsCommonImage() {
        BytesProcessResult readAsCommonImageResult = new BytesProcessResult(path, "Reading as standard image");

        try {
            java.awt.Toolkit.getDefaultToolkit().createImage(bytes);
        } catch (Exception e) {
            readAsCommonImageResult.setDescription(ExceptionUtils.getMessage(e));
            readAsCommonImageResult.setDetails(ExceptionUtils.getStackTrace(e));
            readAsCommonImageResult.setStatus(Status.ERROR);
        }
        try {
            BufferedImage image = ImageIO.read(new FileInputStream(file));
            if (image == null) {
                readAsCommonImageResult.setDescription("ImageIO.read result is null");
                readAsCommonImageResult.setStatus(Status.WARN);
            }
        } catch (Exception e) {
            readAsCommonImageResult.setDescription(ExceptionUtils.getMessage(e));
            readAsCommonImageResult.setDetails(ExceptionUtils.getStackTrace(e));
            readAsCommonImageResult.setStatus(Status.ERROR);
        }
        return readAsCommonImageResult;
    }

    //TODO use external reader? http://www.insflug.org/raw/Downloads/
    protected BytesProcessResult readNEF(ByteArrayInputStream bais) {
        BytesProcessResult readNEFResult = new BytesProcessResult(path, "NEF file reading");
        readNEFResult.addChildResult(readSpecifiedSourceFromNef(bais, Source.DEFAULT_SOURCE_PROCESSED_IMAGE));
        readNEFResult.addChildResult(readSpecifiedSourceFromNef(bais, Source.DEFAULT_SOURCE_FULL_SIZE_PREVIEW));
        readNEFResult.addChildResult(readSpecifiedSourceFromNef(bais, Source.DEFAULT_SOURCE_RAW_IMAGE));
        return readNEFResult;
    }


    protected BytesProcessResult tryToReadMetaInfo(ByteArrayInputStream bais) {
        BytesProcessResult metaInfoResult = new BytesProcessResult(path, "Meta info reading");
        try {
            String extension = FilenameUtils.getExtension(file.getName());
            if (extension.toUpperCase().equals("CID")) {
                String[] extensions = file.getName().split("\\.");
                if (extensions.length >= 3) {
                    extension = extensions[extensions.length - 3];

                    if (!(extension.toUpperCase().equals("JPG") || //better to use FilenameUtils.isExtension() here ...
                            extension.toUpperCase().equals("JPEG") ||
                            extension.toUpperCase().equals("GIF") ||
                            extension.toUpperCase().equals("PNG") ||
                            extension.toUpperCase().equals("NEF"))) {
                        return metaInfoResult;
                    }
                }
            }
            final ImageReader reader = ImageIO.getImageReadersByFormatName(extension).next();
            reader.setInput(ImageIO.createImageInputStream(bais));
            reader.getImageMetadata(0);
        } catch (Exception e) {
            metaInfoResult.setDescription(ExceptionUtils.getMessage(e));
            metaInfoResult.setDetails(ExceptionUtils.getStackTrace(e));
            metaInfoResult.setStatus(Status.WARN);
        }
        return metaInfoResult;


    }

    protected BytesProcessResult readSpecifiedSourceFromNef(ByteArrayInputStream bais, String sourceType) {
        BytesProcessResult readSpecifiedSourceFromNefResult = new BytesProcessResult(path, "Reading " + sourceType + "from NEF ");
        try {
            final ImageReader reader = ImageIO.getImageReadersByFormatName("nef").next();
            ImageInputStream imageInputStream = new MemoryCacheImageInputStream(bais);
            reader.setInput(imageInputStream);
            NEFImageReader nefreader = (NEFImageReader) reader;
            BufferedImage image = nefreader.read(0, new RAWImageReadParam(
                    sourceType));
        } catch (Exception e) {
            readSpecifiedSourceFromNefResult.setDescription(ExceptionUtils.getMessage(e));
            readSpecifiedSourceFromNefResult.setDetails(ExceptionUtils.getStackTrace(e));
            readSpecifiedSourceFromNefResult.setStatus(Status.WARN);
        }
        return readSpecifiedSourceFromNefResult;
    }

    protected BytesProcessResult analyseBytesForGaps() {
        BytesProcessResult byteGapsAnalyzerResult = new BytesProcessResult(path, "Analysing file for a byte gaps");
        //stupid byte analyzes goes on!
        StringBuilder gapsInfo = new StringBuilder();
        double summOfGaps = detectGapsInByteArray(gapsInfo);
        double percentOfGapsInFile = summOfGaps / bytes.length * 100;
        if (percentOfGapsInFile > 99) {
            byteGapsAnalyzerResult.setDescription("Null file detected. Gap percent is " + percentOfGapsInFile + "%");
            byteGapsAnalyzerResult.setStatus(Status.CRITICAL);
        } else if (percentOfGapsInFile > 50) {
            byteGapsAnalyzerResult.setDescription("Gaps in file detected. Gap percent is " + percentOfGapsInFile + "%");
            byteGapsAnalyzerResult.setStatus(Status.ERROR);
        } else if (percentOfGapsInFile > 10) {
            byteGapsAnalyzerResult.setDescription("Gaps in file detected. Gap percent is " + percentOfGapsInFile + "%");
            byteGapsAnalyzerResult.setStatus(Status.WARN);
        } else if (percentOfGapsInFile == 0) {
            //do noting
        } else {
            byteGapsAnalyzerResult.setDescription("Some gaps in file detected. Probably file is OK. Gap percent is " + percentOfGapsInFile + "%");
            byteGapsAnalyzerResult.setStatus(Status.OK);
        }
        byteGapsAnalyzerResult.setDetails(gapsInfo.toString());


        return byteGapsAnalyzerResult;
    }

    protected long detectGapsInByteArray(StringBuilder gapsInfo) {
        long currentPos = 0;
        boolean startCountGaps = false;
        long startCountGapPos = 0;
        long summOfGaps = 0;
        for (byte b : bytes) {
            if (!startCountGaps && b == 0) {
                startCountGaps = true;
                startCountGapPos = currentPos;
            }
            if (startCountGaps && b != 0) {
                if (currentPos - startCountGapPos > BYTE_ARRAY_ANALYZE_GAP_DETECTION_THRESHOLD) {
                    gapsInfo.append("Detected gap from ").append(startCountGapPos).append(" till ").append(currentPos).append(". Size is ").append(currentPos - startCountGapPos).append("\n");
                    summOfGaps += currentPos - startCountGapPos;
                }
                startCountGaps = false;
                startCountGapPos = 0;
            }
            currentPos++;
        }
        //In case if data ends with null
        if (startCountGaps) {
            if (currentPos - startCountGapPos > BYTE_ARRAY_ANALYZE_GAP_DETECTION_THRESHOLD) {
                gapsInfo.append("Detected gap from ").append(startCountGapPos).append(" till ").append(currentPos).append(". Size is ").append(currentPos - startCountGapPos).append("\n");
                summOfGaps += currentPos - startCountGapPos;
            }
        }
        return summOfGaps;
    }

}

