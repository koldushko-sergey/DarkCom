package windows;

import implementationclasses.WriteServer;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;

public class MainWindowController {
    private Stage STAGE;
    private WriteServer WS;

    private double xOffset;
    private double yOffset;

    @FXML
    private AnchorPane headerPane;

    @FXML
    private ListView lv_Contacts;

    @FXML
    void b_Close_Click() { System.exit(0); }

    @FXML
    void b_Min_Click() { STAGE.setIconified(true); }

    public void Initialization(Stage stage, WriteServer ws) {
        STAGE = stage;
        WS = ws;
        headerPane.setOnMousePressed(event -> {
            xOffset = STAGE.getX() - event.getScreenX();
            yOffset = STAGE.getY() - event.getScreenY();
        });
        headerPane.setOnMouseDragged(event -> {
            STAGE.setX(event.getScreenX() + xOffset);
            STAGE.setY(event.getScreenY() + yOffset);
        });

        lv_Contacts.setCellFactory(new Callback<ListView<String>, ListCell<String>>(){
            @Override
            public ListCell<String> call(ListView<String> p) {

                ListCell<String> cell = new ListCell<String>(){

                    @Override
                    protected void updateItem(String t, boolean bln) {
                        super.updateItem(t, bln);
                        if (t != null) {
                            setText(t);
                            setStyle("-fx-background-image: url('../img/1.png')");
                        }
                    }

                };

                return cell;
            }
        });

        lv_Contacts.getItems().add("Contact 1");
        lv_Contacts.getItems().add("Contact 2");
        lv_Contacts.getItems().add("Contact 3");
        lv_Contacts.getItems().add("Contact 4");
       // ListCell lc = new ListCell();
       // lc.setText("Contact 1");
       // lc.setStyle("-fx-background-image: url('../img/1.png');");

    }
}
