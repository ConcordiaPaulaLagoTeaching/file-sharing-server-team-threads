package ca.concordia;

import ca.concordia.server.FileServer;

public class Main {
    public static void main(String[] args) {
        System.out.printf("Hello and welcome!");

        try {
            FileServer server = new FileServer(12345, "filesystem.dat", 10 * 128);
            // Start the file server
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start the file server: " + e.getMessage());
        }
    }
}