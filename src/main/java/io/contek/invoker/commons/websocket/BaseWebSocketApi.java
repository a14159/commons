package io.contek.invoker.commons.websocket;

import com.google.common.collect.ImmutableList;
import io.contek.invoker.commons.MetricsRecorder;
import io.contek.invoker.commons.actor.IActor;
import io.contek.invoker.commons.actor.RequestContext;
import io.contek.invoker.commons.actor.http.HttpBusyException;
import io.contek.invoker.commons.actor.http.HttpInterruptedException;
import io.contek.invoker.commons.actor.ratelimit.TypedPermitRequest;
import io.contek.invoker.security.ICredential;
import io.contek.ursa.AcquireTimeoutException;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

@ThreadSafe
public abstract class BaseWebSocketApi implements IWebSocketApi {

  private static final Logger log = LogManager.getLogger(BaseWebSocketApi.class);

  public static final boolean METRICS = false; // javac should remove the corresponding code

  private static int id = 0;

  private final int connectionId;

  private final IActor actor;
  private final IWebSocketMessageParser parser;
  private final IWebSocketAuthenticator authenticator;
  private final IWebSocketLiveKeeper liveKeeper;

  private final Handler handler = new Handler();
  private final ScheduledExecutorService scheduler = newSingleThreadScheduledExecutor();

  private final AtomicReference<WebSocketSession> sessionHolder = new AtomicReference<>();
  private final AtomicReference<ScheduledFuture<?>> scheduleHolder = new AtomicReference<>();
  private final WebSocketComponentManager components = new WebSocketComponentManager();

  private static final MetricsRecorder metrics = MetricsRecorder.getRecorder("e2eLatency#parseJson");
  static {
    metrics.setPrintInterval(1000);
    metrics.setPrintFullStats(true);
    metrics.setTimeMicro();
  }

  protected BaseWebSocketApi(
      IActor actor,
      IWebSocketMessageParser parser,
      IWebSocketAuthenticator authenticator,
      IWebSocketLiveKeeper liveKeeper) {
    this.actor = actor;
    this.parser = parser;
    this.authenticator = authenticator;
    this.liveKeeper = liveKeeper;
    this.connectionId = ++id;
  }

  public final boolean isActive() {
    synchronized (sessionHolder) {
      return scheduleHolder.get() != null;
    }
  }

  @Override
  public final void attach(IWebSocketComponent component) {
    synchronized (components) {
      parser.register(component);
      components.attach(component);
      activate();
    }
    log.info("Attaching subscription {} to ws connection #{}", component, connectionId);
  }

  protected final IWebSocketMessageParser getParser() {
    return parser;
  }

  protected final IWebSocketAuthenticator getAuthenticator() {
    return authenticator;
  }

  protected final IWebSocketLiveKeeper getLiveKeeper() {
    return liveKeeper;
  }

  protected abstract ImmutableList<TypedPermitRequest> getRequiredQuotas();

  protected abstract WebSocketCall createCall(ICredential credential);

  protected abstract void checkErrorMessage(AnyWebSocketMessage message)
      throws WebSocketRuntimeException;

  private void forwardMessage(String text) {
    if (METRICS) {
      long startTime = System.nanoTime();
      ParseResult result = parser.parse(text);
      metrics.recordInvocation((System.nanoTime() - startTime) / 1000, true);
      forwardMessage(result);
    } else {
      ParseResult result = parser.parse(text);
      forwardMessage(result);
    }
  }

  private void forwardMessage(ByteString bytes) {
    ParseResult result = parser.parse(bytes.toByteArray());
    forwardMessage(result);
  }

  private void forwardMessage(ParseResult result) {
    try {
      AnyWebSocketMessage message = result.getMessage();
      checkErrorMessage(message);
      synchronized (sessionHolder) {
        WebSocketSession session = sessionHolder.get();

        synchronized (liveKeeper) {
          liveKeeper.onMessage(message, session);
        }
        synchronized (authenticator) {
          if (!authenticator.isCompleted()) {
            authenticator.onMessage(message, session);
          }
        }
        synchronized (components) {
          components.onMessage(message, session);
        }
      }
    } catch (IllegalStateException | NullPointerException e) {
      log.error("Failed to handle message: {}.", result.getStringValue(), e);
      throw new WebSocketIllegalMessageException(e);
    }
  }

  private void connect() {
    synchronized (sessionHolder) {
      sessionHolder.updateAndGet(
          oldValue -> {
            if (oldValue != null) {
              return oldValue;
            }
            WebSocketCall call = createCall(actor.getCredential());
            try (RequestContext context =
                actor.getRequestContext(getClass().getSimpleName(), getRequiredQuotas())) {
              WebSocketSession session = call.submit(context.getClient(), handler);
              activate();
              return session;
            } catch (AcquireTimeoutException e) {
              throw new HttpBusyException(e);
            } catch (InterruptedException e) {
              throw new HttpInterruptedException(e);
            }
          });
    }
  }

  private void afterDisconnect() {
    synchronized (sessionHolder) {
      sessionHolder.set(null);
      synchronized (components) {
        components.afterDisconnect();
      }
      synchronized (liveKeeper) {
        liveKeeper.afterDisconnect();
      }
      synchronized (authenticator) {
        authenticator.afterDisconnect();
      }
    }
  }

  private void heartbeat() {
    try {
      synchronized (sessionHolder) {
        WebSocketSession session = sessionHolder.get();

        synchronized (components) {
          components.refresh();
          if (session == null) {
            if (!components.hasComponent()) {
              deactivate();
              return;
            }
            if (components.hasActiveComponent()) {
              connect();
            }
            return;
          }

          if (!components.hasActiveComponent()) {
            log.info("No active components. Closing session #{}.", connectionId);
            session.close();
            return;
          }

          synchronized (liveKeeper) {
            try {
              liveKeeper.onHeartbeat(session);
            } catch (WebSocketSessionInactiveException e) {
              log.warn("WebSocket session #{} is inactive {}", connectionId, e.getMessage());
              session.close();
            }
          }

          synchronized (authenticator) {
            if (authenticator.isPending()) {
              return;
            }
            if (!authenticator.isCompleted()) {
              authenticator.handshake(session);
              return;
            }
          }

          components.heartbeat(session);
        }
      }
    } catch (Throwable t) {
      log.error("Heartbeat failed for ws #{}", connectionId, t);
    }
  }

  private void activate() {
    synchronized (scheduleHolder) {
      scheduleHolder.updateAndGet(
          oldValue -> {
            if (oldValue != null && !oldValue.isDone()) {
              return oldValue;
            }
            log.debug("WS connection #{} is now active", connectionId);
            return scheduler.scheduleWithFixedDelay(this::heartbeat, 0, 1, TimeUnit.SECONDS);
          });
    }
  }

  private void deactivate() {
    synchronized (scheduleHolder) {
      scheduleHolder.updateAndGet(
          oldValue -> {
            if (oldValue == null) {
              return null;
            }
            if (!oldValue.isDone()) {
              oldValue.cancel(true);
            }
            return null;
          });
    }
    log.debug("WS connection #{} is now deactivated", connectionId);
  }

  @ThreadSafe
  private final class Handler extends WebSocketListener {

    @Override
    public void onClosed(WebSocket ws, int code, String reason) {
      log.info("Session #{} is closed: {} {}.", connectionId, code, reason);
      try {
        afterDisconnect();
      } catch (Throwable t) {
        log.error("Failed to handle closed session.", t);
      }
    }

    @Override
    public void onFailure(WebSocket ws, Throwable t, @Nullable Response response) {
      if (t instanceof SocketTimeoutException) {
        log.warn("Shutting down inactive session #{}: SocketTimeoutException {}", connectionId, t.getMessage());
      } else if (t instanceof EOFException) {
        log.warn("Server closed connection  #{} EOFException: {} ", connectionId, t.getMessage());
      } else if (t instanceof IOException) {
        log.warn("Connection #{} interrupted {} IOException", connectionId, t.getMessage());
      } else if (t instanceof WebSocketServerRestartException) {
        log.warn("Server requires restart {} for #{} WebSocketServerRestartException", connectionId, t.getMessage());
      } else if (t instanceof WebSocketSessionExpiredException) {
        log.warn("Session #{} expired WebSocketSessionExpiredException {}", connectionId, t.getMessage());
      } else if (t instanceof WebSocketSessionInactiveException) {
        log.warn("Session #{} is inactive WebSocketSessionInactiveException {}", connectionId, t.getMessage());
      } else if (t instanceof WebSocketIllegalSequenceException) {
        log.warn("Received out of order message for #{} WebSocketIllegalSequenceException: {}", connectionId, t.getMessage());
      } else if (t instanceof WebSocketIllegalStateException) {
        log.warn("Channel #{} has invalid state {} WebSocketIllegalStateException", connectionId, t.getMessage());
      } else {
        log.error("Encountered unknown error for ws #{}: {}", connectionId, response, t);
      }

      try {
        log.info("Closing connection #{}.", connectionId);
        ws.cancel();
        afterDisconnect();
        log.debug("Component states reset.");
      } catch (Throwable t2) {
        log.error("Failed to handle failure.", t2);
      }
    }

    @Override
    public void onMessage(WebSocket ws, String text) {
      forwardMessage(text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
      forwardMessage(bytes);
    }

    @Override
    public void onOpen(WebSocket ws, Response response) {
      log.info("WS {} connection #{} is open: {}.", actor.getCredential().isAnonymous() ? "public" : "private",connectionId, response);
    }
  }
}
