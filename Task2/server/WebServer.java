package Task2.server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class WebServer {
    private static final int PORT = 5698;
    private static final String BASE_DIRECTORY = "./Task2/web_files/html/";
    private static final String CSS_DIRECTORY = "./Task2/web_files/css/";
    private static final String IMAGES_DIRECTORY = "./Task2/images/";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server connected to port: \"" + PORT + "\"");
            System.out.println("Server Started");
            System.out.println("Waiting for client...");

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                    handleClient(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) throws IOException {
        InputStream input = clientSocket.getInputStream();
        OutputStream output = clientSocket.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        PrintWriter writer = new PrintWriter(output, true);

        String requestLine = reader.readLine();
        System.out.println("Request: " + requestLine);

        if (requestLine == null || requestLine.isEmpty()) {
            return;
        }

        String[] tokens = requestLine.split(" ");
        if (tokens.length < 2 || !tokens[0].equals("GET")) {
            sendErrorResponse(output, clientSocket, 400, "Bad Request");
            return;
        }

        String requestedFile = tokens[1];
        if (requestedFile.equals("/")) {
            requestedFile = "/main_en.html"; 
        } else if (requestedFile.equals("/ar")) {
            requestedFile = "/main_ar.html"; 
        }

        String filePath = BASE_DIRECTORY + requestedFile;
        if (requestedFile.startsWith("/css/")) {
            filePath = CSS_DIRECTORY + requestedFile.substring(4); 
        } else if (requestedFile.startsWith("/images/")) {
            filePath = IMAGES_DIRECTORY + requestedFile.substring(8); 
        }

        System.out.println("File Path: " + filePath);
        System.out.println("Content-Type: " + getContentType(filePath));
        File file = new File(filePath);

        if (!file.exists() || file.isDirectory()) {
            if (requestedFile.equals("/supporting_material_en.html")
                    || requestedFile.equals("/supporting_material_ar.html")) {
                processSupportingMaterialRequest(reader, writer);
            } else {
                sendErrorResponse(output, clientSocket, 404, "Not Found");
            }
            return;
        }

        String contentType = getContentType(filePath);
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + file.length());
        writer.println("Connection: close");
        writer.println();

        Files.copy(file.toPath(), output);
        output.flush();
    }

    private static void processSupportingMaterialRequest(BufferedReader reader, PrintWriter writer) {
        try {
            String fileName = reader.readLine();
            if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
                writer.println("HTTP/1.1 307 Temporary Redirect");
                writer.println("Location: https://www.google.com/search?tbm=isch&q=" + fileName);
            } else if (fileName.endsWith(".mp4")) {
                writer.println("HTTP/1.1 307 Temporary Redirect");
                writer.println("Location: https://www.youtube.com/results?search_query=" + fileName);
            } else {
                sendErrorResponse(writer, 404, "File Not Found");
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendErrorResponse(OutputStream output, Socket clientSocket, int statusCode,
            String statusMessage)
            throws IOException {
        PrintWriter writer = new PrintWriter(output, true);
        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: text/html");
        writer.println("Connection: close");
        writer.println();
        writer.println("<html><head><title>Error</title></head><body>");
        writer.println("<h1 style='color: red;'>The file is not found</h1>");
        writer.println("<p>Client IP: " + clientSocket.getInetAddress() + "</p>");
        writer.println("<p>Client Port: " + clientSocket.getPort() + "</p>");
        writer.println("</body></html>");
        writer.flush();
    }

    private static void sendErrorResponse(PrintWriter writer, int statusCode, String statusMessage) {
        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: text/html");
        writer.println("Connection: close");
        writer.println();
        writer.println("<html><body><h1>" + statusCode + " " + statusMessage + "</h1></body></html>");
        writer.flush();
    }

    private static String getContentType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html";
        if (filePath.endsWith(".css")) return "text/css";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }
}
