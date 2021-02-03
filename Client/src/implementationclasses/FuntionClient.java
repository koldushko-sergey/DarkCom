package implementationclasses;

import classresourse.Message;
import com.google.gson.Gson;
import javafx.application.Platform;
import windows.Dialogs;
import windows.LoginWindowController;
import windows.Main;

/**
 * Created by DELL on 31.01.2017.
 * @author Daniel Sandrutski
 */

public class FuntionClient {
    static public WriteServer WS;
    static public LoginWindowController LWC;

    static public void FillingClientTable(String DataString)
    {
        /*DefaultTableModel model = (DefaultTableModel) jClientTable.getModel();
        model.setRowCount(0);

        Gson gson = new Gson();
        Client[] clients = gson.fromJson(DataString, Client[].class);
        for (Client client : clients) {
            model.addRow(new Object[]{client.getId(), client.getFIO(), client.getType(), client.getPhone(), client.getMail(), client.getStatus(), client.getNotes()});
        }*/
    }

    static public void SendLoginToServer(String login, String password){
        Message message = new Message("Login", new String[] {login, password}, "");
        Gson gson = new Gson();
        WS.WriteLine(gson.toJson(Crypt.encodeServerMessage(gson.toJson(message))));
    }

    static public void Login(String answer)
    {
        if(answer.equals("Yes"))
        {
            Platform.runLater(()-> LWC.STAGE.close());
        }
        else if(answer.equals("No"))
        {
            Platform.runLater(()-> LWC.loadPane.setVisible(false));
            Platform.runLater(()-> Dialogs.ShowError("Не правильный логин или пароль!!!"));
        }
    }
}
