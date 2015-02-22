package software.glare.cid;

public enum Status {
    FOLDER(Integer.MIN_VALUE),
    OK(0),
    SKIPPED(400),
    WARN(500),
    ERROR(900),
    CRITICAL(999),
    SMTH_GOES_WRONG(1000),;

    Status(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    private int priority = 0;

    @Override
    public String toString() {
        switch (this) {
            case FOLDER:
                return "Folder";
            case OK:
                return "Ok";
            case SKIPPED:
                return "Skipped";
            case WARN:
                return "Warning";
            case ERROR:
                return "Error";
            case CRITICAL:
                return "Critical";
            case SMTH_GOES_WRONG:
                return "Something goes wrong";

        }
        return super.toString();
    }
}
