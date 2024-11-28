package org.example;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private static String host = "localhost";
    private static int port = 12345;
    private static PrintWriter out;
    private static BufferedReader in;
    private static Socket socket;
    private static String username;

    public static void main(String[] args) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            // Приветствие и ввод имени пользователя
            System.out.println(in.readLine());  // Приветствие от сервера
            System.out.print("Введите ваш никнейм: ");
            username = scanner.nextLine();
            out.println(username);

            // Поток для обработки ввода сообщений
            Thread inputThread = new Thread(() -> {
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
