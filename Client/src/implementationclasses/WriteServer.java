package implementationclasses;

import classresourse.Message;
import javafx.application.Platform;
import windows.ConnectWindowController;
import windows.Dialogs;
import com.google.gson.Gson;

import java.net.*;
import java.io.*;
import java.util.Objects;

/**
 * Created by DELL on 29.01.2017.
 * @author Daniel Sandrutski
 */

public class WriteServer {

    private Socket s;  // это будет сокет для сервера
    private BufferedReader socketReader; // буферизированный читатель с сервера
    private BufferedWriter socketWriter; // буферизированный писатель на сервер

    public WriteServer(String host, int port, ConnectWindowController CWC){
        try
        {
            s = new Socket(host, port); // создаем сокет
            // создаем читателя и писателя в сокет с дефолной кодировкой UTF-8
            socketReader = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            socketWriter = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));
            Gson gson = new Gson();
            Message message;

            // Создание сеансового ключа и отправка его серверу
            // Генерируем сеансовый ключ
            byte[] tempkey = Crypt.generateKeyAES128();
            message = new Message("SessionKey", new String[] {gson.toJson(tempkey)}, ""); // Отпарвляем сеансовый ключ
            WriteLine(gson.toJson(Crypt.encryptKeyRSA(gson.toJson(message))));
            // Принимаем подтверждающее сообщение
            String line = null;
            try {
                line = socketReader.readLine();
            } catch (IOException e) {
                if ("Socket closed".equals(e.getMessage())) {
                }
                Platform.runLater(()-> {
                    Dialogs.ShowError("Потеряно соединение с сервером!!! Приложение будет закрыто.");
                    System.exit(0);
                });
            }

            byte[] byteLine = gson.fromJson(line, byte[].class);

            if(!Objects.equals(Crypt.decodeServerMessage(byteLine), "IOK")){
                Platform.runLater(()-> {
                    Dialogs.ShowError("Ошибка передачи данных к серверу!!!");
                    Platform.runLater(()->CWC.loadPane.setVisible(false));
                    Close();
                    return;
                });
            }


            String clientIP = null;
            // Формируем IP клиента
            if(Objects.equals(host, "127.0.0.1")){ // Если localhost
                clientIP = "localhost";
            } else if(host.indexOf("192") == 0) { // Если используется локальный IP, то находим локальный IP и отправляем его
                InetAddress iaLocalAddress;
                try {
                    iaLocalAddress =
                            InetAddress.getLocalHost();

                    byte[] ip;
                    ip = iaLocalAddress.getAddress();

                    clientIP = (0xff & (int)ip[0]) + "." +
                            (0xff & (int)ip[1]) + "." +
                            (0xff & (int)ip[2]) + "." +
                            (0xff & (int)ip[3]);
                } catch(UnknownHostException ex) {
                    System.out.println(ex.toString());
                }
            } else {
                clientIP = getCurrentIP(); // Иначе внешний IP, находим его
            }
            // Отправляем ip адрес пользователя серверу
            message = new Message("ClientIP", new String[] {clientIP}, "");
            WriteLine(gson.toJson(Crypt.encodeServerMessage(gson.toJson(message))));

            CWC.STAGE.close();

            new Thread(new Receiver()).start();// создаем и запускаем нить асинхронного чтения из сокета
        } catch (IOException e) { // если объект не создан...
            Platform.runLater(()-> {
                Dialogs.ShowError("Ошибка подключения к серверу!!! Ошибка возможна, если указан не верный IP-адрес или серврер в данный момент не работает!");
                CWC.loadPane.setVisible(false);
            });
            //Platform.runLater(()-> CWC.loadPane.setVisible(false));
            return;
        }
    }

    public void WriteLine(String userString) {
        try {
            socketWriter.write(userString); //пишем строку пользователя
            socketWriter.write("\n"); //добавляем "новою строку", дабы readLine() сервера сработал
            socketWriter.flush(); // отправляем
        } catch (IOException e) {
            Platform.runLater(()->{
                Dialogs.ShowError("Ошибка соединения с сервером!!! Возможно работа сервера была нарушена! Презапустите приложение и попробуйте снова!");
                System.exit(0);
            });

        }

    }

    public void Close() {
        if (!s.isClosed()) { // проверяем, что сокет не закрыт...
            try {
                s.close(); // закрываем...
                System.exit(0); // выходим!
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
    }

    private class Receiver implements Runnable{
        /**
         * run() вызовется после запуска нити из конструктора клиента.
         */
        @Override
        public void run() {
            while (!s.isClosed()) {
                String line = null;
                try {
                    line = socketReader.readLine();
                } catch (IOException e) {
                    if ("Socket closed".equals(e.getMessage())) {
                        break;
                    }
                    Platform.runLater(()-> {
                        Dialogs.ShowError("Потеряно соединение с сервером!!! Приложение будет закрыто.");
                        System.exit(0);
                    });
                    return;
                }
                if (line == null) {
                    Platform.runLater(()-> {
                        Dialogs.ShowError("Серврер препвал соединение!!! Приложение будет закрыто.");
                        System.exit(0);
                    });
                } else {
                    Gson gson = new Gson();
                    byte[] byteLine = gson.fromJson(line, byte[].class);
                    Message message = gson.fromJson(Crypt.decodeServerMessage(byteLine), Message.class);

                    switch (message.getKeyword())
                    {
                        case "FillingClientTable":
                        {
                            break;
                        }
                        case "Login":
                        {
                            FuntionClient.Login(message.getTextArguments()[0]);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static String getCurrentIP() {
        String result = null;
        try {
            BufferedReader reader = null;
            try {
                URL url = new URL("https://myip.by/");
                InputStream inputStream;
                inputStream = url.openStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder allText = new StringBuilder();
                char[] buff = new char[1024];

                int count;
                while ((count = reader.read(buff)) != -1) {
                    allText.append(buff, 0, count);
                }
                Integer indStart = allText.indexOf("<div id=\"ip\" class=\"bodytext headline\">");
                Integer indEnd = allText.indexOf("</div>", indStart);

                String ipAddress = allText.substring(indStart + 39, indEnd);
                if (ipAddress.split("\\.").length == 4) {
                    result = ipAddress;
                }
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                result = "Ошибка доступа";
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}