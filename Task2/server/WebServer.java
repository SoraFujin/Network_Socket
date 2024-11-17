package Task2.server;

import java.net.*;
import java.nio.file.Files;
import java.io.*;

public class WebServer {
    private static final int port = 5698;
    private static final String BASE_DIRECTORY = ".";

    private static Socket clientSocket;
    private static ServerSocket server;

    private static InputStream input;
    private static OutputStream out;

    private static BufferedReader reader;
    private static InputStreamReader streamReader;
    private static PrintWriter writer;

    public static void main(String[] args) {
        startServer(port);
        clientConnection(clientSocket, server);
        handleClient(clientSocket);
    }

    public static void startServer(int port) {
        try {
            // Starts server and listens on port 5698 and waits connection
            server = new ServerSocket(port);
            System.out.println("Server connected to port: \"" + port + "\"");
            System.out.println("Server Started");
            System.out.println("Waiting for clinet");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clientConnection(Socket clientSocket, ServerSocket server) {
        try {
            while (true) {
                clientSocket = server.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void handleClient(Socket clientSocket) {
        try {
            input = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            streamReader = new InputStreamReader(input);

            reader = new BufferedReader(streamReader);
            writer = new PrintWriter(out, true);

            String requestLine = reader.readLine();
            System.out.println("Request: " + requestLine);

            if (requestLine == null || requestLine.isEmpty()) {
                clientSocket.close();
                return;
            }

            String[] token = requestLine.split(" ");
            if (token.length < 2 || !token[0].equals("GET")) {
                sendErrorResponse(out, 404, "Not Found");            
                return;
            }

            String requestedFile = token[1];
            if(requestedFile.equals("/")) {
                requestedFile = "/main_en.html";
            }
            String filePath = BASE_DIRECTORY + requestedFile;
            File file = new File(filePath);

            if(!file.exists() || file.isDirectory()) {
                sendErrorResponse(out, 404, "Not Found");
                return;
            }

            String contentType = getContentType(filePath);
            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Type: " + contentType);
            writer.println("Content-Length: " + file.length());
            writer.println("Connection: close");
            writer.println();

            // Send file content
            Files.copy(file.toPath(), out);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendErrorResponse(OutputStream output, int statusCode, String statusMessage)
            throws IOException {
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
