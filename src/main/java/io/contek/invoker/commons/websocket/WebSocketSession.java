package io.contek.invoker.commons.websocket;

import com.alibaba.fastjson2.JSON;
import io.contek.invoker.commons.MetricsRecorder;
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
      ws.send(casted.getRawText());
      return;
    }

    if (METRICS) {
      long startTime = System.nanoTime();
      ws.send(JSON.toJSONString(message));
      metrics.recordInvocation((System.nanoTime() - startTime) / 1000, true);
    } else {
      ws.send(JSON.toJSONString(message));
    }
  }

  void close() {
    ws.close(1000, null);
  }
}
