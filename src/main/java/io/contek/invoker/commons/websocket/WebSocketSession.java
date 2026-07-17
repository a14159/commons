package io.contek.invoker.commons.websocket;

import com.alibaba.fastjson2.JSON;
import is.fm.util.MetricsRecorder;
import okhttp3.WebSocket;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class WebSocketSession {

  public static final boolean METRICS = false; // javac should remove the corresponding code

  private static final MetricsRecorder metrics = MetricsRecorder.getRecorder("e2eLatency#generateJsonAndPush");
  static {
    metrics.setPrintInterval(200);
    metrics.setPrintFullStats(true);
    metrics.setTimeMicro();
  }

  private final WebSocket ws;

  WebSocketSession(WebSocket ws) {
    this.ws = ws;
  }

  public void send(AnyWebSocketMessage message) {
    if (message instanceof IWebSocketRawTextMessage casted) {
      send(casted.getRawText());
      return;
    }

    if (METRICS) {
      long startTime = System.nanoTime();
      send(JSON.toJSONString(message));
      metrics.recordInvocation((System.nanoTime() - startTime) / 1000, true);
    } else {
      send(JSON.toJSONString(message));
    }
  }

  private void send(String message) {
    if (!ws.send(message)) {
      throw new WebSocketSessionInactiveException(
          "Failed to enqueue WebSocket message: session is closing or closed, or the outgoing queue is full.");
    }
  }

  void close() {
    ws.close(1000, null);
  }
}
