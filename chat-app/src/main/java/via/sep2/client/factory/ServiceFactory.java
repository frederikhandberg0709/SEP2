package via.sep2.client.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import via.sep2.client.command.CommandManager;
import via.sep2.client.service.AuthService;
import via.sep2.client.service.ChatService;

public class ServiceFactory {
    private static final Logger logger = Logger.getLogger(ServiceFactory.class.getName());
    private static ServiceFactory instance;
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    private ServiceFactory() {
    }

    public static ServiceFactory getInstance() {
        if (instance == null) {
            synchronized (ServiceFactory.class) {
                if (instance == null) {
                    instance = new ServiceFactory();
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) services.computeIfAbsent(serviceClass, this::createService);
    }

    private Object createService(Class<?> serviceClass) {
        logger.info("Creating service: " + serviceClass.getSimpleName());

        if (serviceClass == AuthService.class) {
            return new AuthService();
        } else if (serviceClass == ChatService.class) {
            return new ChatService();
        } else if (serviceClass == CommandManager.class) {
            return new CommandManager();
        }

        throw new IllegalArgumentException("Unknown service type: " + serviceClass);
    }

    // For testing allow injection of mock services
    public <T> void registerService(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
        logger.info("Registered mock service: " + serviceClass.getSimpleName());
    }

    public void clearServices() {
        services.values().forEach(service -> {
            if (service instanceof CommandManager) {
                ((CommandManager) service).shutdown();
            }
        });
        services.clear();
        logger.info("All services cleared");
    }
}
