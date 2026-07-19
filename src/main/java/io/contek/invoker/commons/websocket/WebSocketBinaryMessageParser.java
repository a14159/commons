package io.contek.invoker.commons.websocket;


import okio.ByteString;

import javax.annotation.concurrent.ThreadSafe;


@ThreadSafe
public abstract class WebSocketBinaryMessageParser implements IWebSocketMessageParser {

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
    try {
      return fromBytes(bytes);
    } catch (WebSocketRuntimeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new WebSocketIllegalMessageException(
          "Failed to decode binary message: size " + bytes.size() + ".", e);
    }
  }

  protected AnyWebSocketMessage fromText(String text) {
    throw new UnsupportedOperationException();
  }

  protected AnyWebSocketMessage fromBytes(ByteString bytes) {
    throw new UnsupportedOperationException();
  }
}
