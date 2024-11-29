package Task2.server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class WebServer {
    // Constants for port number and directory paths for web files and resources
    private static final int PORT = 5698;
    private static final String BASE_DIRECTORY = "./Task2/web_files/html/";
    private static final String CSS_DIRECTORY = "./Task2/web_files/css/";
    private static final String IMAGES_DIRECTORY = "./Task2/images/";

    private static ServerSocket serverSocket;  // Server socket to listen for incoming connections
    private static Socket clientSocket;        // Socket for individual client connections
    
    public static void main(String[] args) {
        try {
            // Initialize the server socket and bind it to the specified port
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server connected to port: \"" + PORT + "\"");
            System.out.println("Server Started");
            System.out.println("Waiting for client...");
            
            // The server runs in an infinite loop to accept multiple client connections
            while (true) {
                try {
                    // Accept a client connection and create a socket for communication
                    clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                    // Handle the client's request
                    handleClient(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        
    }

    // Method to handle the client's request
    private static void handleClient(Socket clientSocket) throws IOException {
        // Initialize input and output streams to communicate with the client
        InputStream input = clientSocket.getInputStream();
        OutputStream output = clientSocket.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        PrintWriter writer = new PrintWriter(output, true);

        // Read the first line of the HTTP request (request line)
        String requestLine = reader.readLine();
        System.out.println("Request: " + requestLine);

        // If the request line is null or empty, do nothing and return
        if (requestLine == null || requestLine.isEmpty()) {
            return;
        }

        // Parse the request line (e.g., GET /index.html HTTP/1.1)
        String[] tokens = requestLine.split(" ");
        // If the request is not a GET request or is malformed, send a 400 Bad Request response
        if (tokens.length < 2 || !tokens[0].equals("GET")) {
            sendErrorResponse(output, clientSocket, 400, "Bad Request");
            return;
        }

        // Determine the file requested by the client
        String requestedFile = tokens[1];
        if (requestedFile.equals("/") || requestedFile.equals("/en") || requestedFile.equals("/index.html")) {
            requestedFile = "/main_en.html";  // Default page if no specific page is requested
        } else if (requestedFile.equals("/ar")) {
            requestedFile = "/main_ar.html";  // Arabic version of the page
        }

        // Build the full file path based on the requested file
        String filePath = BASE_DIRECTORY + requestedFile;
        System.out.println("File path processing: " + filePath);
        // If the requested file is in the /css/ or /images/ directories, update the file path accordingly
        if (requestedFile.startsWith("/css/")) {
            filePath = CSS_DIRECTORY + requestedFile.substring(4);
        } else if (requestedFile.startsWith("/images/")) {
            filePath = IMAGES_DIRECTORY + requestedFile.substring(8);
        }

        // If the requested file is for supporting materials, handle it differently
        if (requestedFile.startsWith("/requested_material")) {
            processSupportingMaterialRequest(reader, writer, requestedFile);
            return; 
        }

        // If the requested file exists, serve it to the client; otherwise, send a 404 Not Found response
        System.out.println("File Path: " + filePath);
        System.out.println("Content-Type: " + getContentType(filePath));
        File file = new File(filePath);

        if (!file.exists() || file.isDirectory()) {
            if (requestedFile.equals("/supporting_material_en.html")
                    || requestedFile.equals("/supporting_material_ar.html")) {
                processSupportingMaterialRequest(reader, writer, requestedFile); 
            } else {
                sendErrorResponse(output, clientSocket, 404, "Not Found");
            }
            return;
        }

        // Determine the content type based on the file extension
        String contentType = getContentType(filePath);
        // Send a 200 OK response header
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + file.length());
        writer.println("Connection: close");
        writer.println();

        // Copy the file content to the output stream to send it to the client
        Files.copy(file.toPath(), output);
        output.flush();
    }

    // Method to process requests for supporting material (images or videos)
    private static void processSupportingMaterialRequest(BufferedReader reader, PrintWriter writer,
            String requestedFile) throws IOException {
        // Extract query parameters (type and material) from the URL
        String queryString = requestedFile.contains("?") ? requestedFile.split("\\?")[1] : "";
        System.out.println("Query String: " + queryString);

        String type = "";
        String material = "";

        if (!queryString.isEmpty()) {
            String[] params = queryString.split("&");
            // Parse the query parameters
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    System.out.println("Key: " + key + " Value: " + value);
                    if ("type".equals(key)) {
                        type = value;
                    } else if ("material".equals(key)) {
                        material = value;
                    }
                }
            }
        }

        System.out.println("Type: " + type);
        System.out.println("Material: " + material);

        // Redirect the user based on the type of material requested
        if ("image".equals(type)) {
            writer.println("HTTP/1.1 307 Temporary Redirect");
            writer.println("Location: https://www.google.com/search?tbm=isch&q=" + material);
        } else if ("video".equals(type)) {
            writer.println("HTTP/1.1 307 Temporary Redirect");
            writer.println("Location: https://www.youtube.com/results?search_query=" + material);
        } else {
            sendErrorResponse(writer, clientSocket, 400, "Invalid Request Type");
            return;
        }

        // End the response and close the connection
        writer.println("Connection: close");
        writer.println(); 
        writer.flush();
    }

    // Method to send an error response (e.g., 404 Not Found)
    private static void sendErrorResponse(PrintWriter writer, Socket clientSocket, int statusCode, String message) {
        writer.println("HTTP/1.1 404 Not Found" + statusCode + " " + message);
        writer.println("Content-Type: text/html");
        writer.println("Connection: close");
        writer.println();
        writer.println("<html><body><h1>" + message + "</h1></body></html>");
        writer.flush();
    }

    // Overloaded method to send error response using output stream
    private static void sendErrorResponse(OutputStream output, Socket clientSocket, int statusCode,
            String statusMessage) throws IOException {
        PrintWriter writer = new PrintWriter(output, true);
        writer.println("HTTP/1.1 404 Not Found" + statusCode + " " + statusMessage);
        writer.println("Content-Type: text/html");
        writer.println("Connection: close");
        writer.println();
        writer.println("<html><head><title>Error 404</title></head><body>");
        writer.println("<h1 style='color: red;'>The file is not found</h1>");
        writer.println("<h1 style='color: red;'> Error 404</h1>");
        writer.println("<p>Client IP: " + clientSocket.getInetAddress() + "</p>");
        writer.println("<p>Client Port: " + clientSocket.getPort() + "</p>");
        writer.println("</body></html>");
        writer.flush();
    }

    // Method to determine the content type based on file extension
    private static String getContentType(String filePath) {
        if (filePath.endsWith(".html"))
            return "text/html";
        if (filePath.endsWith(".css"))
            return "text/css";
        if (filePath.endsWith(".png"))
            return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg"))
            return "image/jpeg";
        if (filePath.endsWith(".mp4"))
            return "video/mp4";
        return "application/octet-stream";  // Default content type
    }
}
