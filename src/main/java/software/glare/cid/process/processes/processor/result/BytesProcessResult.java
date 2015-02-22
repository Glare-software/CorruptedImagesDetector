package software.glare.cid.process.processes.processor.result;


import software.glare.cid.Status;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BytesProcessResult implements Comparable<BytesProcessResult> {


    private Path path;
    private boolean isLeaf;
    private String resultName;
    private String description;
    private String details;
    private Status status = Status.FOLDER;
    private List<BytesProcessResult> childResults = new ArrayList<>();
    private ResultPostInfo resultPostInfo;

    public BytesProcessResult(String path) {
        this.isLeaf = false;
        this.path = new File(path).toPath();
    }

    public BytesProcessResult(Path path, String resultName) {
        this.isLeaf = true;
        this.path = path;
        this.resultName = resultName;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (this.status.getPriority() < status.getPriority()) {
            this.status = status;
        }
    }

    public void addChildResult(BytesProcessResult childProcessResult) {
        childResults.add(childProcessResult);
        if (childProcessResult.getDescription() != null) {
            setDescription((getDescription() != null ? getDescription() + "\n" : "") + childProcessResult.resultName + ": " + childProcessResult.getDescription());
        }
        if (childProcessResult.getDetails() != null) {
            setDetails((getDetails() != null ? getDetails() + "\n" : "") + childProcessResult.resultName + ": " + childProcessResult.getDetails());
        }
        setStatus(childProcessResult.getStatus());
    }

    @Override
    public String toString() {
        return "BytesProcessResult{" +
                "path=" + path +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", childResults=" + childResults +
                '}';
    }


    public String toString2() {
        return "path=" + path;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ResultPostInfo getResultPostInfo() {
        return resultPostInfo;
    }

    public void setResultPostInfo(ResultPostInfo resultPostInfo) {
        this.resultPostInfo = resultPostInfo;
    }

    @Override
    public int compareTo(BytesProcessResult o) {
        return getPath().compareTo(o.getPath());
    }


}

