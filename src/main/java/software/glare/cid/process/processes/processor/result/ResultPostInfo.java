package software.glare.cid.process.processes.processor.result;

import software.glare.cid.Status;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by fdman on 04.02.2015.
 */
public class ResultPostInfo {
    private Map<Status, Long> byStatusesMap = new HashMap<>();

    public ResultPostInfo() {
        for (Status status : Status.values()) {
            byStatusesMap.put(status, 0L);
        }
    }

    private Status worstStatus = Status.FOLDER;

    private long totalNonFoldersInside = 0;

    public Map<Status, Long> getByStatusesMap() {
        return byStatusesMap;
    }

    public String getPostInfoFormatted() {
        StringBuilder sb = new StringBuilder("Total files: ");
        sb.append(totalNonFoldersInside).append("\n").
                append("Worst file status: ").append(worstStatus.toString()).append("\n");
        byStatusesMap.keySet().stream().filter(new Predicate<Status>() {
            @Override
            public boolean test(Status status) {
                return status != Status.SMTH_GOES_WRONG && status.getPriority() > Status.FOLDER.getPriority() && byStatusesMap.get(status) > 0;
            }
        }).sorted(new Comparator<Status>() {
            @Override
            public int compare(Status o1, Status o2) {
                return Integer.compare(o2.getPriority(), o1.getPriority());
            }
        }).forEachOrdered(status -> {
            sb.append("    ").append(status.toString()).append(": ").append(byStatusesMap.get(status)).append("\n");
        });
        return sb.toString();
    }

    public void addValueToByStatusesMap(Status status, long value) {
        addToByStatusesMap(byStatusesMap, status, value);
    }


    public Status getWorstStatus() {
        return worstStatus;
    }

    public void setWorstStatus(Status worstStatus) {
        this.worstStatus = worstStatus;
    }

    public long getTotalNonFoldersInside() {
        return totalNonFoldersInside;
    }

    public void setTotalNonFoldersInside(long totalNonFoldersInside) {
        this.totalNonFoldersInside = totalNonFoldersInside;
    }

    public static void addStatusesToFirstMap(Map<Status, Long> aMap1, Map<Status, Long> aMap2) {
        for (Status status : aMap2.keySet()) {
            addToByStatusesMap(aMap1, status, aMap2.get(status));
        }

    }

    private static void addToByStatusesMap(Map<Status, Long> aMap, Status status, long value) {
        aMap.put(status, aMap.get(status) + value);
    }
}
