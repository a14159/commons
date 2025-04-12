package io.contek.invoker.commons.websocket;

import io.contek.invoker.util.TinyBitSet;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static io.contek.invoker.commons.websocket.ConsumerState.TERMINATED;

@NotThreadSafe
final class WebSocketComponentManager {

  private final List<IWebSocketComponent> active = new ArrayList<>(16);
  private final List<IWebSocketComponent> idle = new ArrayList<>(16);


  private static final class ComponentFilterPredicate implements Predicate<IWebSocketComponent> {
    private int idx = 0;
    private long bitSet;

    public void init(long bitSet) {
      idx = 0;
      this.bitSet = bitSet;
    }

    @Override
    public boolean test(IWebSocketComponent iWebSocketComponent) {
      return TinyBitSet.isSet(bitSet, idx++);
    }
  }

  private final ComponentFilterPredicate removePredicate = new ComponentFilterPredicate();

  void attach(IWebSocketComponent component) {
    switch (component.getState()) {
      case ACTIVE -> active.add(component);
      case IDLE -> idle.add(component);
      default -> throw new IllegalStateException(component.getState().name());
    }
    if (active.size() + idle.size() > 63)
      throw new IllegalStateException("Too many components added to a ComponentManager");
  }

  void refresh() {
    long toRemoveIdle = 0;
    for (int i = 0; i < idle.size(); i++) {
      IWebSocketComponent next = idle.get(i);
      switch (next.getState()) {
        case ACTIVE:
          active.add(next);
        case TERMINATED:
          toRemoveIdle = TinyBitSet.set(toRemoveIdle, i);
      }
    }
    synchronized (removePredicate) {
      removePredicate.init(toRemoveIdle);
      idle.removeIf(removePredicate);
    }
    long toRemoveActive = 0;
    for (int i = 0; i < active.size(); i++) {
      IWebSocketComponent next = active.get(i);
      switch (next.getState()) {
        case IDLE:
          idle.add(next);
        case TERMINATED:
          toRemoveActive = TinyBitSet.set(toRemoveActive, i);
      }
    }
    synchronized (removePredicate) {
      removePredicate.init(toRemoveActive);
      active.removeIf(removePredicate);
    }
  }

  void heartbeat(WebSocketSession session) {
      // noinspection ALL
      for (int i = 0, activeSize = active.size(); i < activeSize; i++) {
          IWebSocketComponent c = active.get(i);
          c.heartbeat(session);
      }
  }

  boolean hasActiveComponent() {
    return !active.isEmpty();
  }

  boolean hasComponent() {
    return !active.isEmpty() || !idle.isEmpty();
  }

  void onMessage(AnyWebSocketMessage message, WebSocketSession session) {
      // noinspection ALL
      for (int i = 0, activeSize = active.size(); i < activeSize; i++) {
          IWebSocketComponent component = active.get(i);
          component.onMessage(message, session);
      }
  }

  void afterDisconnect() {
    active.forEach(IWebSocketListener::afterDisconnect);
    active.removeIf(component -> component.getState() == TERMINATED);

    idle.forEach(IWebSocketListener::afterDisconnect);
    idle.removeIf(component -> component.getState() == TERMINATED);
  }
}
