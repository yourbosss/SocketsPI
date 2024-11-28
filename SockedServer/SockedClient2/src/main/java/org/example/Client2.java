package org.example;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client2 {
    private static final Logger logger = LoggerFactory.getLogger(Client2.class);
    private static String host = "localhost"; //адрес сервера.
    private static int port = 12345;
    private static PrintWriter out; //поток вывода для отправки сообщений на сервер.
    private static BufferedReader in; //поток для чтения.
    private static Socket socket; //cокет, через который осуществляется подключение клиента к серверу.
    private static String username;

    public static void main(String[] args) {
        try {
            socket = new Socket(host, port); //создается сокет, по котору читается все.
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            // приветствие и ввод имени пользователя
            System.out.println(in.readLine());  // приветствие от сервера
            System.out.print("Введите ваш никнейм: ");
            username = scanner.nextLine();
            out.println(username);

            // Ппток для обработки ввода сообщений
            Thread inputThread = new Thread(() -> { //постоянно слушает ввод с клавиатуры. Когда пользователь вводит строку, она немедленно отправляется на сервер.
                while (true) {
                    String command = scanner.nextLine();
                    out.println(command);
                }
            });
            inputThread.start();

            // Поток для вывода сообщений от сервера
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println(serverMessage);
            }

        } catch (IOException e) {
            logger.error("Ошибка клиента: ", e);
        }
    }
}
