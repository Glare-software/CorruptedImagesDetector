package software.glare.cid.process;

import software.glare.cid.process.processes.processor.result.BytesProcessResult;

import java.util.List;

/**
 * Created by fdman on 08.07.2014.
 */
public abstract class Report {
    public abstract void addLine(BytesProcessResult bytesProcessResult);

    public abstract List<BytesProcessResult> getLines();
}
