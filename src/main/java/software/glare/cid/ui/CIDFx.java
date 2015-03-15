package software.glare.cid.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Created by fdman on 24.06.2014.
 */
public class CIDFx extends Application {
    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) throws Exception {

        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
        SLF4JBridgeHandler.install();

        Scene scene = new Scene(new MainFormController(stage).getMainForm().getMainStackPane());
        scene.getStylesheets().add("css/style.css");
        stage.setScene(scene);
        stage.show();
        stage.setMinHeight(stage.getHeight());
        stage.setMinWidth(stage.getWidth());

    }
}
