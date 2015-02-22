package software.glare.cid.process;

/**
 * Created by fdman on 25.01.2015.
 */
public class ProgressData {
    private double interim;
    private double total;

    private String info;

    public ProgressData(Double total, String info) {
        this.total = total;
        this.info = info;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
