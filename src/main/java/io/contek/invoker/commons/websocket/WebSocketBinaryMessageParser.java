package io.contek.invoker.commons.websocket;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.concurrent.ThreadSafe;


@ThreadSafe
public abstract class WebSocketBinaryMessageParser implements IWebSocketMessageParser {

  private static final Logger log = LogManager.getLogger(WebSocketBinaryMessageParser.class);

  @Override
  public final ParseResult parse(String text) {
    try {
      AnyWebSocketMessage message = fromText(text);
      return new ParseResult(text, message);
    } catch (Throwable t) {
      log.error("Failed to parse text message: {}.", text, t);
      throw new WebSocketIllegalMessageException(t);
    }
  }

  @Override
  public final ParseResult parse(byte[] bytes) {
    try {
      AnyWebSocketMessage message = fromBytes(bytes);
      return new ParseResult("binary", message);
    } catch (Throwable t) {
      log.error("Failed to decode binary message: size {}.", bytes.length, t);
      throw new WebSocketIllegalMessageException(t);
    }
  }

  protected AnyWebSocketMessage fromText(String text) {
    throw new UnsupportedOperationException();
  }

  protected AnyWebSocketMessage fromBytes(byte[] bytes) {
    throw new UnsupportedOperationException();
  }
}
