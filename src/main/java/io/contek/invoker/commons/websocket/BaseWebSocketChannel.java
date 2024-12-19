package io.contek.invoker.commons.websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static io.contek.invoker.commons.websocket.ConsumerState.*;
import static io.contek.invoker.commons.websocket.SubscriptionState.*;

@ThreadSafe
public abstract class BaseWebSocketChannel<
        Id extends BaseWebSocketChannelId<Message>, Message extends AnyWebSocketMessage, Data>
    implements IWebSocketComponent, IWebSocketChannel<Data> {

  private static final Logger log = LogManager.getLogger(BaseWebSocketChannel.class);

  private final Id id;

  private final AtomicReference<SubscriptionState> stateHolder =
      new AtomicReference<>(UNSUBSCRIBED);
  private final List<ISubscribingConsumer<Data>> consumers = new ArrayList<>();

  protected BaseWebSocketChannel(Id id) {
    this.id = id;
  }

  public final Id getId() {
    return id;
  }

  @Override
  public final void addConsumer(ISubscribingConsumer<Data> consumer) {
    synchronized (consumers) {
      synchronized (stateHolder) {
        SubscriptionState state = stateHolder.get();
        consumer.onStateChange(state);
      }
      consumers.add(consumer);
    }
  }

  @Override
  public final void heartbeat(WebSocketSession session) {
    synchronized (consumers) {
      ConsumerState childConsumerState = getChildConsumerState();

      synchronized (stateHolder) {
        SubscriptionState currentState = stateHolder.get();
        SubscriptionState newState = null;
        if (currentState == SUBSCRIBED && childConsumerState == IDLE) {
          log.debug("Unsubscribing channel {}.", id);
          newState = unsubscribe(session);
          if (newState == SUBSCRIBED || newState == SUBSCRIBING) {
            log.error("Channel {} has invalid state after unsubscribe: {}.", id, newState);
          }
        } else if (currentState == UNSUBSCRIBED && childConsumerState == ACTIVE) {
          log.debug("Subscribing channel {}.", id);
          newState = subscribe(session);
          if (newState == UNSUBSCRIBED || newState == UNSUBSCRIBING) {
            log.error("Channel {} has invalid state after subscribe: {}.", id, newState);
          }
        }

        if (newState != null) {
          setState(newState);
        }
      }
    }
  }

  @Override
  public final ConsumerState getState() {
    synchronized (consumers) {
      if (getChildConsumerState() == ACTIVE) {
        return ACTIVE;
      }
    }

    synchronized (stateHolder) {
      return stateHolder.get() != UNSUBSCRIBED ? ACTIVE : IDLE;
    }
  }

  @Override
  public final void onMessage(AnyWebSocketMessage message, WebSocketSession session) {
    Message casted = tryCast(message);
    if (casted != null) {
      Data data = getData(casted);
      synchronized (consumers) {
        // noinspection ALL
        for (int i = 0; i < consumers.size(); i++) {
          ISubscribingConsumer<Data> consumer = consumers.get(i);
          consumer.onNext(data);
        }
      }
    }

    SubscriptionState newState = getState(message);
    if (newState != null) {
      log.info("Channel {} is now {}.", id, newState);
      setState(newState);
    }
  }

  @Override
  public final void afterDisconnect() {
    reset();
    setState(UNSUBSCRIBED);
  }

  public abstract Class<Message> getMessageType();

  protected abstract Data getData(Message message);

  protected abstract SubscriptionState subscribe(WebSocketSession session);

  protected abstract SubscriptionState unsubscribe(WebSocketSession session);

  @Nullable
  protected abstract SubscriptionState getState(AnyWebSocketMessage message);

  protected abstract void reset();

  private final Predicate<ISubscribingConsumer<Data>> filterTerminated = consumer -> consumer.getState() == TERMINATED;

  private ConsumerState getChildConsumerState() {
    synchronized (consumers) {
      consumers.removeIf(filterTerminated);
      // noinspection ALL
      for (int i = 0; i < consumers.size(); i++) {
          ISubscribingConsumer<Data> consumer = consumers.get(i);
          if (consumer.getState() == ACTIVE) {
              return ACTIVE;
          }
      }
      return IDLE;
    }
  }

  @Nullable
  private Message tryCast(AnyWebSocketMessage message) {
    if (!getMessageType().isAssignableFrom(message.getClass())) {
      return null;
    }
    Message casted = getMessageType().cast(message);
    return id.accepts(casted) ? casted : null;
  }

  private void setState(SubscriptionState state) {
    synchronized (stateHolder) {
      synchronized (consumers) {
          // noinspection ALL
          for (int i = 0, consumersSize = consumers.size(); i < consumersSize; i++) {
              ISubscribingConsumer<Data> consumer = consumers.get(i);
              consumer.onStateChange(state);
          }
      }
      stateHolder.set(state);
    }
  }
}
