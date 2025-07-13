package via.sep2.client.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class EventBus {

    private static final Logger logger = Logger.getLogger(EventBus.class.getName());
    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService eventExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EventBus-Thread");
        t.setDaemon(true);
        return t;
    });

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.info("Subscribed listener for event type: " + eventType.getSimpleName());
    }

    public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            logger.info("Unsubscribed listener for event type: " + eventType.getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<EventListener<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null && !eventListeners.isEmpty()) {
            // Async event processing to avoid blocking
            eventExecutor.submit(() -> {
                for (EventListener<?> listener : eventListeners) {
                    try {
                        ((EventListener<T>) listener).onEvent(event);
                    } catch (Exception e) {
                        logger.severe("Error in event listener for " + event.getClass().getSimpleName() + ": "
                                + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            logger.fine("Published event: " + event.getClass().getSimpleName());
        }
    }

    public void shutdown() {
        eventExecutor.shutdown();
        logger.info("EventBus shutdown");
    }

    public int getListenerCount(Class<?> eventType) {
        List<EventListener<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }

    public void clearAllListeners() {
        listeners.clear();
        logger.info("All event listeners cleared");
    }
}
