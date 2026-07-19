package io.contek.invoker.commons.websocket;


import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.concurrent.ThreadSafe;


@ThreadSafe
public abstract class WebSocketBinaryMessageParser implements IWebSocketMessageParser {

  private static final Logger log = LogManager.getLogger(WebSocketBinaryMessageParser.class);

  @Override
  public final AnyWebSocketMessage parse(String text) {
    try {
      return fromText(text);
    } catch (Throwable t) {
      log.error("Failed to parse text message: {}.", text, t);
      throw new WebSocketIllegalMessageException(t);
    }
  }

  @Override
  public final AnyWebSocketMessage parse(ByteString bytes) {
    try {
      return fromBytes(bytes);
    } catch (Throwable t) {
      log.error("Failed to decode binary message: size {}.", bytes.size(), t);
      throw new WebSocketIllegalMessageException(t);
    }
  }

  protected AnyWebSocketMessage fromText(String text) {
    throw new UnsupportedOperationException();
  }

  protected AnyWebSocketMessage fromBytes(ByteString bytes) {
    throw new UnsupportedOperationException();
  }
}
