package windows;

import implementationclasses.FuntionClient;
import implementationclasses.WriteServer;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class LoginWindowController {
    public Stage STAGE;
    private  WriteServer WS;

    private double xOffset;
    private double yOffset;

    @FXML
    private TextField tf_Password;

    @FXML
    public AnchorPane loadPane;

    @FXML
    private AnchorPane headerPane;

    @FXML
    private TextField tf_Login;

    @FXML
    void b_Login_Click() {
        loadPane.setVisible(true);
        FuntionClient.SendLoginToServer(tf_Login.getText(), tf_Password.getText());
    }

    @FXML
    void b_Close_Click() { System.exit(0); }

    @FXML
    void b_Min_Click() { STAGE.setIconified(true); }

    public void Initialization(Stage stage, WriteServer ws) {
        WS = ws;
        STAGE = stage;
        headerPane.setOnMousePressed(event -> {
            xOffset = STAGE.getX() - event.getScreenX();
            yOffset = STAGE.getY() - event.getScreenY();
        });
        headerPane.setOnMouseDragged(event -> {
            STAGE.setX(event.getScreenX() + xOffset);
            STAGE.setY(event.getScreenY() + yOffset);
        });
        FuntionClient.LWC = this;
    }
}
