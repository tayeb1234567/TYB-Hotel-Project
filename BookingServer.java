import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class BookingServer {

    private static Connection bookingConn;
    private static Connection logConn;

    public static void main(String[] args) throws IOException {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Initialize DB connections
            bookingConn = DriverManager.getConnection("jdbc:sqlite:bookings.db");
            logConn = DriverManager.getConnection("jdbc:sqlite:logs.db");

            // Create tables if not exist
            try (Statement stmt = bookingConn.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS bookings (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT," +
                        "hotel TEXT," +
                        "details TEXT," +
                        "image TEXT)"
                );
            }
            try (Statement stmt = logConn.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "timestamp TEXT," +
                        "message TEXT)"
                );
            }

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/login", new LoginHandler());
        server.createContext("/book", new BookingHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);  // creates a default executor
        server.start();

        System.out.println("Server started at http://localhost:8000");

        // Add shutdown hook to close DB connections
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (bookingConn != null && !bookingConn.isClosed()) bookingConn.close();
                if (logConn != null && !logConn.isClosed()) logConn.close();
                System.out.println("Database connections closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));
    }

    private static void logMessage(String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        try (PreparedStatement pstmt = logConn.prepareStatement("INSERT INTO logs (timestamp, message) VALUES (?, ?)")) {
            pstmt.setString(1, timestamp);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to write log to database.");
            e.printStackTrace();
        }
    }

    private static void saveBookingToFile(String message) {
        File file = new File("saves.txt");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write("[" + timestamp + "] " + message + "\n");
            System.out.println("Booking saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save booking to file.");
            e.printStackTrace();
        }
    }

    // Handler for /login
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("&"));

            Map<String, String> params = parseUrlEncoded(body);

            String username = params.get("username");
            String password = params.get("password");

            logMessage("Login attempt: " + body);

            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                // TODO: Implement real user/pass check here
                String response = "success";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                String response = "fail";
                exchange.sendResponseHeaders(403, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

    // Handler for /book
    static class BookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("&"));

            Map<String, String> params = parseUrlEncoded(body);

            String username = params.getOrDefault("username", "unknown");
            String hotel = params.getOrDefault("hotel", "defaultHotel");
            String details = params.getOrDefault("details", "");
            String image = params.getOrDefault("image", "");

            String logEntry = "Booking from " + username + ": " + body;
            logMessage(logEntry);
            saveBookingToFile(logEntry);

            try (PreparedStatement pstmt = bookingConn.prepareStatement(
                    "INSERT INTO bookings (username, hotel, details, image) VALUES (?, ?, ?, ?)")) {
                pstmt.setString(1, username);
                pstmt.setString(2, hotel);
                pstmt.setString(3, details);
                pstmt.setString(4, image);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                String response = "Failed to save booking";
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String response = "Booking saved successfully!";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    // Handler to serve static files (html, css, js, etc.)
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            File file = new File("." + path).getCanonicalFile();

            if (!file.exists() || !file.isFile()) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String contentType = guessContentType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType);

            byte[] bytes = readFileFully(file);
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String guessContentType(String filename) {
            if (filename.endsWith(".html")) return "text/html; charset=utf-8";
            if (filename.endsWith(".css")) return "text/css";
            if (filename.endsWith(".js")) return "application/javascript";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
            if (filename.endsWith(".gif")) return "image/gif";
            return "application/octet-stream";
        }

        private byte[] readFileFully(File file) throws IOException {
            try (InputStream is = new FileInputStream(file);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                return baos.toByteArray();
            }
        }
    }

    // Utility to parse URL-encoded form data (e.g. key=value&key2=value2)
    private static Map<String, String> parseUrlEncoded(String input) {
        Map<String, String> map = new HashMap<>();
        if (input == null || input.isEmpty()) return map;

        String[] pairs = input.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name());
                    map.put(key, value);
                } else {
                    String key = URLDecoder.decode(pair, StandardCharsets.UTF_8.name());
                    map.put(key, "");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return map;
    }
}
