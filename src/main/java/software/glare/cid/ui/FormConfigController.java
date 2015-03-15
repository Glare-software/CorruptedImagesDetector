package software.glare.cid.ui;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by fdman on 15.03.2015.
 */
class FormConfigController {
    private static final String BACKING_STORE_AVAIL = "BackingStoreAvailableTest";
    public static final String PREFERENCES = "bidfxuipreferences";

    public boolean isBackingStoreAvailable() {
        Preferences prefs = Preferences.userRoot().node("");
        try {
            boolean oldValue = prefs.getBoolean(BACKING_STORE_AVAIL, false);
            prefs.putBoolean(BACKING_STORE_AVAIL, !oldValue);
            prefs.flush();
        } catch (BackingStoreException e) {
            return false;
        }
        return true;
    }

    public void save(FormConfig formConfig) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JAXBContext ctx = JAXBContext.newInstance(FormConfig.class);

            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(formConfig, baos);
            baos.flush();
            Preferences prefs = Preferences.userNodeForPackage(CIDFx.class);
            prefs.putByteArray(PREFERENCES, baos.toByteArray());
            prefs.flush();
        } catch (JAXBException | IOException | BackingStoreException e) {
            e.printStackTrace();
        }

    }

    public FormConfig restore() {
        FormConfig formConfig = null;
        Preferences prefs = Preferences.userNodeForPackage(CIDFx.class);
        if (prefs != null) {
            byte[] restoredSettings = null;
            restoredSettings = prefs.getByteArray(PREFERENCES, restoredSettings);
            if (restoredSettings != null) {
                try {
                    JAXBContext readCtx = JAXBContext.newInstance(FormConfig.class);
                    Unmarshaller um = readCtx.createUnmarshaller();
                    formConfig = (FormConfig) um.unmarshal(new ByteArrayInputStream(restoredSettings));
                } catch (JAXBException e) {
                    e.printStackTrace();
                }
            }
        }
        return formConfig;
    }
}
