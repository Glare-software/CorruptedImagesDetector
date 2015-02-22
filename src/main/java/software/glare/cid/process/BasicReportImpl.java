package software.glare.cid.process;

import software.glare.cid.process.processes.processor.result.BytesProcessResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by fdman on 19.07.2014.
 */
public class BasicReportImpl extends Report {
    private List<BytesProcessResult> bytesProcessResults = Collections.synchronizedList(new ArrayList<>());//CopyOnWriteArrayList<>();

    @Override
    public void addLine(BytesProcessResult bytesProcessResult) {
        bytesProcessResults.add(bytesProcessResult);
    }

    @Override
    public List<BytesProcessResult> getLines() {
        return bytesProcessResults;
    }

    @Override
    public String toString() {
        return "BasicReportImpl{" +
                "bytesProcessResults=" + bytesProcessResults +
                '}';
    }
}
