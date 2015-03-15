package software.glare.cid.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by fdman on 15.03.2015.
 */
@XmlRootElement
class FormConfig implements Serializable {
    private BooleanProperty jpgCheckBoxSelectedState = new SimpleBooleanProperty();
    private BooleanProperty gifCheckBoxSelectedState = new SimpleBooleanProperty();
    private BooleanProperty nefCheckBoxSelectedState = new SimpleBooleanProperty();
    private BooleanProperty bidCheckBoxSelectedState = new SimpleBooleanProperty();
    private StringProperty folderPath = new SimpleStringProperty("Select a folder to start scan");

    public boolean getJpgCheckBoxSelectedState() {
        return jpgCheckBoxSelectedState.get();
    }

    public BooleanProperty jpgCheckBoxSelectedStateProperty() {
        return jpgCheckBoxSelectedState;
    }

    public void setJpgCheckBoxSelectedState(boolean jpgCheckBoxSelectedState) {
        this.jpgCheckBoxSelectedState.set(jpgCheckBoxSelectedState);
    }

    public boolean getGifCheckBoxSelectedState() {
        return gifCheckBoxSelectedState.get();
    }

    public BooleanProperty gifCheckBoxSelectedStateProperty() {
        return gifCheckBoxSelectedState;
    }

    public void setGifCheckBoxSelectedState(boolean gifCheckBoxSelectedState) {
        this.gifCheckBoxSelectedState.set(gifCheckBoxSelectedState);
    }

    public boolean getNefCheckBoxSelectedState() {
        return nefCheckBoxSelectedState.get();
    }

    public BooleanProperty nefCheckBoxSelectedStateProperty() {
        return nefCheckBoxSelectedState;
    }

    public void setNefCheckBoxSelectedState(boolean nefCheckBoxSelectedState) {
        this.nefCheckBoxSelectedState.set(nefCheckBoxSelectedState);
    }

    public boolean getBidCheckBoxSelectedState() {
        return bidCheckBoxSelectedState.get();
    }

    public BooleanProperty bidCheckBoxSelectedStateProperty() {
        return bidCheckBoxSelectedState;
    }

    public void setBidCheckBoxSelectedState(boolean bidCheckBoxSelectedState) {
        this.bidCheckBoxSelectedState.set(bidCheckBoxSelectedState);
    }


    public String getFolderPath() {
        return folderPath.get();
    }

    public void setFolderPath(String folderPathValue) {
        folderPath.set(folderPathValue);
    }

    public StringProperty getFolderPathProperty() {
        return folderPath;
    }

}
