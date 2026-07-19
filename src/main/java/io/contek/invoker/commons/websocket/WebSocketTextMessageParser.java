package io.contek.invoker.commons.websocket;


import okio.ByteString;

import javax.annotation.concurrent.ThreadSafe;


@ThreadSafe
public abstract class WebSocketTextMessageParser implements IWebSocketMessageParser {

  @Override
  public final AnyWebSocketMessage parse(String text) {
    try {
      return fromText(text);
    } catch (WebSocketRuntimeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new WebSocketIllegalMessageException(
          "Failed to parse text message: " + text + ".", e);
    }
  }

  @Override
  public final AnyWebSocketMessage parse(ByteString bytes) {
    throw new UnsupportedOperationException();
  }

  protected abstract AnyWebSocketMessage fromText(String text);
}
