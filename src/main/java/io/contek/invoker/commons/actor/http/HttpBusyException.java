package io.contek.invoker.commons.actor.http;


import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class HttpBusyException extends AnyHttpException {

  public HttpBusyException(Exception cause) {
    super(null, cause);
  }
}
