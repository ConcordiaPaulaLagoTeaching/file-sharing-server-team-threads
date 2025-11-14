package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private FileSystemManager fs_manager;
    private int port;
    
    public FileServer(int port, String file_system_name, int total_size) throws Exception {
        this.fs_manager = new FileSystemManager(file_system_name, 10, 20, 128);
        this.port = port;
    }

    public void start(){
        try (ServerSocket server_socket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port + "...");

            while (true) {
                Socket client_socket = server_socket.accept();
                System.out.println("New client connected: " + client_socket);
                
                //one thread for one client- thread applied on handle one client method
                new Thread(() -> handle_client(client_socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }

    //handle one client
    private void handle_client(Socket client_socket) {
        System.out.println("Handling client in new thread: " + client_socket);
        
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            PrintWriter writer = new PrintWriter(client_socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client " + client_socket + ": " + line);
                
                if (line.equalsIgnoreCase("QUIT")) {
                    writer.println("SUCCESS: Disconnecting.");
                    break;
                }
                
                String response = process_command(line);
                writer.println(response);
            }
        } catch (Exception e) {
            System.err.println("Error from client " + client_socket + ": " + e.getMessage());
        } finally {
            try {
                client_socket.close();
                System.out.println("Client disconnected: " + client_socket);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    //method to handle commands in each client, seperated
    private String process_command(String command_line) {
        try {
            String[] parts = command_line.split(" ", 3);
            String command = parts[0].toUpperCase();

            switch (command) {
                case "CREATE":
                    if (parts.length < 2) return "ERROR: CREATE needs filename";
                    fs_manager.create_file(parts[1]);
                    return "SUCCESS: File '" + parts[1] + "' created";

                case "DELETE":
                    if (parts.length < 2) return "ERROR: DELETE needs filename";
                    fs_manager.delete_file(parts[1]);
                    return "SUCCESS: File '" + parts[1] + "' deleted";

                case "READ":
                    if (parts.length < 2) return "ERROR: READ needs filename";
                    byte[] content = fs_manager.read_file(parts[1]);
                    return "SUCCESS: Content: " + new String(content);

                case "WRITE":
                    if (parts.length < 3) return "ERROR: WRITE needs filename and content";
                    fs_manager.write_file(parts[1], parts[2].getBytes());
                    return "SUCCESS: Written to '" + parts[1] + "'";

                case "LIST":
                    String[] files = fs_manager.list_files();
                    if (files.length == 0) return "SUCCESS: No files";
                    return "SUCCESS: Files: " + String.join(", ", files);

                default:
                    return "ERROR: Unknown command";
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}