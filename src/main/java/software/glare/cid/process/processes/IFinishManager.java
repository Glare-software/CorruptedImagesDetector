package software.glare.cid.process.processes;

public interface IFinishManager {

    boolean getCanFinishFlag();

    void setCanFinishFlag(boolean atomicBoolean);
}
