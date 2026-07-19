package io.contek.invoker.commons.websocket;

import okio.ByteString;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface IWebSocketMessageParser {

  AnyWebSocketMessage parse(String text);

  AnyWebSocketMessage parse(ByteString bytes);

  void register(IWebSocketComponent component);
}
