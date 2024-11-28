package org.example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static String host;
    private static int port;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();

    public static void main(String[] args) {
        loadConfig(); //загрузка конфигурации из файла.
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host))) { //загрузка хоста.
            logger.info("Сервер запущен на порту " + port + " и хосте " + host);
            while (true) {
                Socket clientSocket = serverSocket.accept(); //блок, пока клиент не подключится.
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            logger.error("Ошибка при запуске сервера", e);
        }
    }

    private static void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Извините, не удалось найти файл конфигурации.");
                return;
            }
            properties.load(input);
            host = properties.getProperty("server.host", "localhost");
            port = Integer.parseInt(properties.getProperty("server.port", "12345")); //класс загрузки настроек.
        } catch (IOException ex) {
            logger.error("Ошибка при загрузке конфигурации", ex);
        }
    }

    public static class ClientHandler implements Runnable {
        private Socket socket; //соединение с клиентом
        private PrintWriter out; //отправка сообщений
        private BufferedReader in; //чтение сообщения клиента
        private String username; //ники клиента
        private boolean isRunning = true; //работает ли клиен??
        private Queue<String> messageQueue = new LinkedList<>(); //очередь сообщений

        public ClientHandler(Socket socket) {
            this.socket = socket;
        } //прием сообщений от сокета.

        @Override
        public void run() { //точка входа потока
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //входной поток сообщений.
                out = new PrintWriter(socket.getOutputStream(), true); //поток отправляется сразу.

                out.println("Добро пожаловать на сервер!");
                out.println("Введите ваш никнейм:");
                username = in.readLine();
                logger.info("{} подключился", username);

                sendUserListToAllClients();

                // Инициализация и отправка начального меню
                out.println("Привет, " + username + "! Выберите тип сообщения:");
                displayMenu(out);

                String action;
                while ((action = in.readLine()) != null) {
                    switch (action) {
                        case "1":
                            handlePrivateMessage();
                            break;
                        case "2":
                            handleBroadcastMessage();
                            break;
                        case "3":
                            handleExit();
                            break;
                        case "4":
                            handleUserListRequest();
                            break;
                        case "5":
                            handleRefreshMessages();
                            break;
                        default:
                            out.println("Неверный ввод. Попробуйте снова.");
                            break;
                    }

                    if (isRunning) {
                        displayMenu(out);  // отправляем меню снова только в случае, если сессия продолжается
                    }
                }
            } catch (IOException e) {
                logger.error("Ошибка в обработке клиента {}", username, e);
            } finally {
                closeConnection();
            }
        }

        private void handlePrivateMessage() throws IOException {
            out.println("Введите имя получателя:");
            String recipient = in.readLine();
            out.println("Введите сообщение:");
            String message = in.readLine();
            sendPrivateMessage(recipient, message);
        }

        private void handleBroadcastMessage() throws IOException {
            out.println("Введите сообщение:");
            String message = in.readLine();
            sendBroadcastMessage(message);
        }

        private void handleExit() throws IOException {
            isRunning = false;
            socket.close();
            clientHandlers.remove(this);
            sendUserListToAllClients();
            logger.info("{} отключился", username);
        }

        private void handleUserListRequest() {
            logger.info("{} запросил список пользователей.", username);
            sendUserListToClient();
        }

        private void handleRefreshMessages() {
            logger.info("{} запросил обновление сообщений.", username);
            refreshMessages();
        }

        private void sendPrivateMessage(String recipient, String message) {
            boolean recipientFound = false;
            for (ClientHandler client : clientHandlers) {
                if (client.username.equals(recipient)) {
                    recipientFound = true;
                    client.addMessageToQueue("Личное сообщение от " + username + ": " + message);
                    out.println("Ваше сообщение отправлено пользователю " + recipient);
                    logger.info("Личное сообщение от {} для {}: {}", username, recipient, message);
                    break;
                }
            }
            if (!recipientFound) {
                out.println("Пользователь с ником " + recipient + " не найден.");
            }
        }

        private void sendBroadcastMessage(String message) {
            for (ClientHandler client : clientHandlers) {
                client.addMessageToQueue("Широковещательное сообщение от " + username + ": " + message);
            }
            logger.info("Широковещательное сообщение от {}: {}", username, message);
        }

        private void sendUserListToAllClients() {
            StringBuilder userList = new StringBuilder("Список пользователей: ");
            for (ClientHandler client : clientHandlers) {
                userList.append(client.username).append(", ");
            }

            if (userList.length() > 0) {
                userList.setLength(userList.length() - 2);
            }

            for (ClientHandler client : clientHandlers) {
                client.addMessageToQueue(userList.toString());
            }

            logger.info("Обновленный список пользователей отправлен всем клиентам: {}", userList.toString());
        }

        private void sendUserListToClient() {
            StringBuilder userList = new StringBuilder("Список пользователей: ");
            for (ClientHandler client : clientHandlers) {
                userList.append(client.username).append(", ");
            }

            if (userList.length() > 0) { //добавление пользователя в строчку.
                userList.setLength(userList.length() - 2); //удаление знака.
            }

            out.println(userList.toString());
            logger.info("Отправлен список пользователей клиенту {}", username);
        }

        private void displayMenu(PrintWriter out) {
            out.println("1 - Личное сообщение");
            out.println("2 - Широковещательное сообщение");
            out.println("3 - Выход");
            out.println("4 - Запрос списка пользователей");
            out.println("5 - Обновить сообщения");
        }

        private void refreshMessages() {
            while (!messageQueue.isEmpty()) { //если очередь пуста, происходит отправка сообщений клиента через сокетю
                out.println(messageQueue.poll()); //если нет сообщений от все равно срабатывает, а потом отключается.
            }
        }

        private void addMessageToQueue(String message) {
            messageQueue.add(message);
        } //добавление сообщения в очередь.

        private void closeConnection() { //закрытие сервера с клиентом.
            try {
                if (socket != null && !socket.isClosed()) { //если сокет инициализирован и только потом был закрыт
                    // или он уже закрыт клиентом.
                    socket.close();
                    logger.info("Соединение с {} закрыто", username);
                }
            } catch (IOException e) {
                logger.error("Ошибка при закрытии соединения с {}", username, e);
            }
        }
    }
}