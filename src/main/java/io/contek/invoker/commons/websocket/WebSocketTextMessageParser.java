package io.contek.invoker.commons.websocket;


import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.concurrent.ThreadSafe;


@ThreadSafe
public abstract class WebSocketTextMessageParser implements IWebSocketMessageParser {

  private static final Logger log = LogManager.getLogger(WebSocketTextMessageParser.class);

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
    throw new UnsupportedOperationException();
  }

  protected abstract AnyWebSocketMessage fromText(String text);
}
