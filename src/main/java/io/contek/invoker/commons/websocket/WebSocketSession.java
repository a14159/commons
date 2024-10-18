package io.contek.invoker.commons.websocket;

import com.alibaba.fastjson2.JSON;
import okhttp3.WebSocket;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class WebSocketSession {

  private final WebSocket ws;

  WebSocketSession(WebSocket ws) {
    this.ws = ws;
  }

  public void send(AnyWebSocketMessage message) {
    if (message instanceof IWebSocketRawTextMessage casted) {
      ws.send(casted.getRawText());
      return;
    }

    ws.send(JSON.toJSONString(message));
  }

  void close() {
    ws.close(1000, null);
  }
}
