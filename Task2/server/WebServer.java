package Task2.server;

import java.net.*;
import java.nio.file.Files;
import java.io.*;

public class WebServer {
    private static final int PORT = 5698;
    private static final String BASE_DIRECTORY = "./Task2/web_files"; 

    private static Socket clientSocket;
    private static ServerSocket server;

    public static void main(String[] args) {
        startServer(PORT);

        while (true) {
            try {
                clientSocket = server.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                handleClient(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void startServer(int port) {
        try {
            // Starts server and listens on the specified port
            server = new ServerSocket(port);
            System.out.println("Server connected to port: \"" + port + "\"");
            System.out.println("Server Started");
            System.out.println("Waiting for client...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handleClient(Socket clientSocket) {
        try (
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            PrintWriter writer = new PrintWriter(output, true)
        ) {
            // Read the request line
            String requestLine = reader.readLine();
            System.out.println("Request: " + requestLine);

            if (requestLine == null || requestLine.isEmpty()) {
                clientSocket.close();
                return;
            }

            // Parse the requested file
            String[] tokens = requestLine.split(" ");
            if (tokens.length < 2 || !tokens[0].equals("GET")) {
                sendErrorResponse(output, 400, "Bad Request");
                return;
            }

            String requestedFile = tokens[1];
            if (requestedFile.equals("/")) {
                requestedFile = "/html/main_en.html"; // Default page
            }

            String filePath = BASE_DIRECTORY + requestedFile;

            // Check if file exists and serve it
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory()) {
                sendErrorResponse(output, 404, "Not Found");
                return;
            }

            // Send HTTP response headers
            String contentType = getContentType(filePath);
            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Type: " + contentType);
            writer.println("Content-Length: " + file.length());
            writer.println("Connection: close");
            writer.println();

            // Send file content
            Files.copy(file.toPath(), output);
            output.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendErrorResponse(OutputStream output, int statusCode, String statusMessage) throws IOException {
        PrintWriter writer = new PrintWriter(output, true);
        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: text/html");
        writer.println("Connection: close");
        writer.println();
        writer.println("<html><head><title>Error</title></head><body>");
        writer.println("<h1>" + statusCode + " " + statusMessage + "</h1>");
        writer.println("</body></html>");
        writer.flush();
    }

    private static String getContentType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html";
        if (filePath.endsWith(".css")) return "text/css";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
