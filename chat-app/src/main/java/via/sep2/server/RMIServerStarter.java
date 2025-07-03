package via.sep2.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

import via.sep2.server.rmi.ChatServerImpl;
import via.sep2.shared.interfaces.ChatServerInterface;

public class RMIServerStarter {

    private static final Logger logger = Logger.getLogger(RMIServerStarter.class.getName());
    private static final String SERVER_NAME = "ChatServer";
    private static final int RMI_PORT = 1099;

    public static void main(String[] args) {
        try {
            startServer();
        } catch (Exception e) {
            logger.severe("Failed to start RMI server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void startServer() throws Exception {
        logger.info("Starting RMI Chat Server...");

        // Create and export the server object
        ChatServerInterface server = new ChatServerImpl();

        // Create or locate the RMI registry
        Registry registry = getOrCreateRegistry();

        // Bind the server to the registry
        registry.rebind(SERVER_NAME, server);

        logger.info("Chat server bound to registry as '" + SERVER_NAME + "'");
        logger.info("Server is ready and waiting for client connections...");
        logger.info("Server URL: rmi://localhost:" + RMI_PORT + "/" + SERVER_NAME);

        setupShutdownHook(registry);

        // Keep the server running
        try {
            Thread.currentThread().join(); // Wait indefinitely
        } catch (InterruptedException e) {
            logger.info("Server interrupted, shutting down...");
        }
    }

    private static Registry getOrCreateRegistry() throws Exception {
        try {
            // Try to create a new registry
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            logger.info("Created RMI registry on port " + RMI_PORT);
            return registry;
        } catch (Exception e) {
            // If creation fails, try to locate existing registry
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);
            logger.info("Located existing RMI registry on port " + RMI_PORT);
            return registry;
        }
    }

    private static void setupShutdownHook(Registry registry) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Chat Server...");
            try {
                registry.unbind(SERVER_NAME);
                logger.info("Server unbound from registry");
            } catch (Exception e) {
                logger.warning("Error during shutdown: " + e.getMessage());
            }
        }));
    }
}
