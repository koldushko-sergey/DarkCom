package implementationclasses;

import windows.Dialogs;
import java.sql.*;
import java.text.SimpleDateFormat;
import javafx.application.Platform;

public class ConnectToMySQL {

    static private final String URL = "jdbc:mysql://localhost:3306/darkcom?zeroDateTimeBehavior=convertToNull";
    static private final String USER = "root";
    static private final String PASSWORD = "root";

    static public void Test()
    {
        try 
        {
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            con.createStatement();
        }
        catch (SQLException sqlE) 
        {
            Platform.runLater(() -> {
                Dialogs.ShowError("Тест на корректное подключение к базе данных провален!!! Приложение будет закрыто!!!");
                System.exit(0);
            });
        }
    }
    
    public void Update(String query)
    {
        try
        {
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = con.createStatement();
            stmt.executeUpdate(query);
        }
        catch (SQLException sqlE)
        {
            Platform.runLater(()->History("System", "Ошибка соединения с БД: " + sqlE));
        }
    }
    
    public ResultSet Select(String query)
    {
        try 
        {
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = con.createStatement();
            return stmt.executeQuery(query); 
        } 
        catch (SQLException sqlE) 
        {
            Platform.runLater(()->History("System", "Ошибка соединения с БД: " + sqlE));
            return null;
        }
    }

    public void History(String user, String message){
        try
        {
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = con.createStatement();
            java.util.Date date = new java.util.Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            stmt.executeUpdate("INSERT INTO History (Date, User, Text) VALUES ('" + format.format(date) + "', '" + user + "', '" + message + "')");
        }
        catch (SQLException sqlE)
        {
            Platform.runLater(()->Dialogs.ShowError("Ошибка соединения с базой данных!!!"));
        }
    }
}
