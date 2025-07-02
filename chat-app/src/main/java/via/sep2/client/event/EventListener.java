package via.sep2.client.event;

@FunctionalInterface
public interface EventListener<T> {
    void onEvent(T event);
}
