package software.glare.cid.ui;

import javafx.geometry.Insets;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by fdman on 15.03.2015.
 */
class UIConstants {
    private static final String APP_NAME = "Corrupted Images Detector";
    public static final String VERSION = "1.0";
    public static final String STATE = "Alpha version";
    public static final String MAIN_TITLE = APP_NAME + " " + VERSION + (StringUtils.isBlank(STATE) ? "" : " ") + STATE;
    public static final Insets INSETS_STD = new Insets(5, 5, 5, 5);
    public static final double GAP_STD = 10;
}
