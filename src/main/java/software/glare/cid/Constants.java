package software.glare.cid;

/**
 * User: fdman
 * Date: 01.12.13
 */
public class Constants {
    public static final int CPU_CORES_NUM = 4+1;
    public static final int INPUT_QUEUE_SIZE_NUM = (int) (CPU_CORES_NUM*1.2);
    public static final int PROCESSING_WAIT_SECONDS = 10;
    public static final String PROCESSED_FILES_NEW_EXTENSION = "cid";

}
