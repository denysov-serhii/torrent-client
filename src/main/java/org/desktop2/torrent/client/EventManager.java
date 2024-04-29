package org.desktop2.torrent.client;

import java.util.*;
import java.util.function.Consumer;
import lombok.NonNull;

public class EventManager {
  private static final Map<Class<? extends Event>, Set<Consumer<Event>>> subscribers =
      new WeakHashMap<>();

  @SuppressWarnings("unchecked")
  public static <T extends Event> void register(Class<T> eventType, Consumer<T> subscriber) {
    subscribers.putIfAbsent(eventType, new HashSet<>());
    subscribers.get(eventType).add((Consumer<Event>) subscriber);
  }

  public static <T extends Event> void unregister(Class<T> eventType, Consumer<T> subscriber) {
    subscribers.putIfAbsent(eventType, new HashSet<>());
    subscribers.get(eventType).remove(subscriber);
  }

  public static void notify(@NonNull Event event) {
    subscribers.putIfAbsent(event.getClass(), new HashSet<>());
    subscribers.get(event.getClass()).forEach(sub -> sub.accept(event));
  }
}
