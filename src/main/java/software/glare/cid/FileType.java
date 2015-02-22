package software.glare.cid;

/**
 * Created by fdman on 13.04.2014.
 */
public enum FileType {
    NEF(new String[]{"NEF"}),
    JPG(new String[]{"JPG", "JPEG"}),
    GIF(new String[]{"GIF"}),
    CID(new String[]{"CID"}),;


    private final String[] extensions;

    FileType(String[] extensions) {
        this.extensions = extensions;
    }

    public String[] getExtensions() {
        return extensions;
    }
}
