package implementationclasses;

import classresourse.Message;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by DELL on 25.01.2017.
 * @author Daniel Sandrutski
 */

public class JavaServer implements Runnable{
    private final ServerSocket ss; 
    private Thread serverThread;
    public static ListView LIST;

    BlockingQueue<SocketProcessor> q = new LinkedBlockingQueue<>();
    
    /**
     * Конструктор объекта сервера
     */
    public JavaServer(int port) throws IOException {
        ss = new ServerSocket(port);
    }
    
    
    /**
     * Главный цикл прослушивания/ожидания коннекта.
     */
    public void run() {
        serverThread = Thread.currentThread(); 
        while (true) {
            Socket s = getNewConn(); 
            if (serverThread.isInterrupted()) { 
                
                break;
            } else if (s != null){ 
                try {
                    final SocketProcessor processor = new SocketProcessor(s); 
                    final Thread thread = new Thread(processor); 
                    thread.setDaemon(true); 
                    thread.start(); 
                    q.offer(processor); 
                } 
                catch (IOException ignored) {}
            }
        }
    }
    
    /**
     * Ожидает новое подключение.
     */
    private Socket getNewConn() {
        Socket s = null;
        try {
            s = ss.accept();
        } catch (IOException e) {
            shutdownServer(); 
        }
        return s;
    }
    
    /**
     * метод "глушения" сервера
     */
    public synchronized void shutdownServer() {
        q.forEach(SocketProcessor::close);
        if (!ss.isClosed()) {
            try {
                ss.close();
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * Вложенный класс асинхронной обработки одного коннекта.
     */
    public class SocketProcessor implements Runnable{
        Socket s;
        BufferedReader br; 
        BufferedWriter bw;
        public String name = "";
        public String ip = "";
        public String id = "";
 
        /**
         * Сохраняем сокет, пробуем создать читателя и писателя. Если не получается - вылетаем без создания объекта
         */
        SocketProcessor(Socket socketParam) throws IOException {
            s = socketParam;
            br = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8") );
            
        }
        
        public String getName()
        {
            return this.name;
        }
 
        /**
         * Главный цикл чтения сообщений/рассылки
         */
        @Override
        @SuppressWarnings("StringEquality")
        public void run() {
            ConnectToMySQL connector = new ConnectToMySQL();
            ServerFunctions functions = new ServerFunctions();
            Gson gson = new Gson();
            String line = null;
            Message message;

            // Принимаем сеансовый ключ
            try {
                line = br.readLine();
            } catch (IOException e) {
                close(); // если не получилось - закрываем сокет.
            }
            if(line != null){
                message = gson.fromJson(Crypt.decryptRSA(gson.fromJson(line, byte[].class)), Message.class);
                Crypt.getDESKey(gson.fromJson(message.getTextArguments()[0], byte[].class)); // Устанавливаем сеансовый ключ
            }
            // Отправляем сообщение о принятии сеансововго ключа

            byte[] mess = Crypt.encodeServerMessage("IOK");

            send(gson.toJson(mess));

            try {
                line = br.readLine();
            } catch (IOException e) {
                close(); // если не получилось - закрываем сокет.
            }
            if(line != null){
                message = gson.fromJson(Crypt.decodeServerMessage(gson.fromJson(line, byte[].class)), Message.class);
                ip = message.getTextArguments()[0];
                name = message.getTextArguments()[0] + ":" + Integer.toString((int)Math.floor(Math.random() * 1000)) + Integer.toString((int)Math.floor(Math.random() * 1000));
                Platform.runLater(()->list());
            }

            Platform.runLater(()->connector.History("System", " Был подключен новый клиент. IP-адрес: " + name + "."));
                
            while (!s.isClosed()) { 
                line = null;
                try {
                    line = br.readLine(); // Принимаем сообщение
                } catch (IOException e) {
                    Platform.runLater(()->connector.History("System", " Был отключен клиент. IP-адрес: " + name + "."));
                    Platform.runLater(()->list());
                    close(); // если не получилось - закрываем сокет.
                }
                
                if(line == null) return;

                message = gson.fromJson(Crypt.decodeServerMessage(gson.fromJson(line, byte[].class)), Message.class);

                switch (message.getKeyword())
                {
                    case "FillingClientTable": 
                    {
                        functions.FillingClientTable(connector, message.getTextArguments(), name, this);
                        break;
                    }
                    
                    case "Login":
                    {
                        try
                        {
                            ResultSet rs = connector.Select("SELECT * FROM users WHERE Login = '" + message.getTextArguments()[0] + "' AND Password = '" + message.getTextArguments()[1] + "'");
                            rs.last();
                            int size = rs.getRow();
                            if(size == 0)
                            {
                                Message outMessage = new Message("Login", new String[] {"No"}, "");
                                send(gson.toJson(Crypt.encodeServerMessage(gson.toJson(outMessage))));
                                break;
                            }
                            else
                            {
                                Message outMessage = new Message("Login", new String[] {"Yes"}, "");
                                send(gson.toJson(Crypt.encodeServerMessage(gson.toJson(outMessage))));
                                String fio = rs.getString("FIO");
                                Platform.runLater(() -> connector.History("System", name + "Авторизация прошла успешно: " + fio + "."));
                                name += " " + rs.getString("FIO");
                                id = rs.getString("Login");
                                Platform.runLater(()->list());
                            }
                        }
                        catch (SQLException sqlE) 
                        {
                            Platform.runLater(() -> connector.History("System", "Error: " +sqlE.toString()));
                        }
                        break;
                    }
                    default:
                        Platform.runLater(() -> connector.History("System", "Ошибка определения функции."));
                        break;
                }
            }
        }
        
        public void UpdateTable()
        {
            q.forEach((sp) -> sp.send("Update"));
        }
 
        /**
         * Метод посылает в сокет полученную строку
         * @param line строка на отсылку
         */
        public synchronized void send(String line) {
            try {
                bw.write(line); 
                bw.write("\n"); 
                bw.flush(); 
            } catch (IOException e) {
                close(); 
            }
        }

        public synchronized void list() {
            LIST.getItems().clear();
            for (SocketProcessor sp: q) {
                LIST.getItems().add(sp.getName());
            }
        }
 
        /**
         * метод аккуратно закрывает сокет и убирает его со списка активных сокетов
         */
        public synchronized void close() {
            q.remove(this); 
            if (!s.isClosed()) {
                try {
                    s.close(); 
                } catch (IOException ignored) {}
            }
        }
 
        /**
         * Финализатор
         * @throws Throwable
         */
        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            super.finalize();
            close();
        }
    }
}
