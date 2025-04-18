package io.contek.invoker.commons.actor;

import io.contek.invoker.commons.actor.http.IHttpClient;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class RequestContext implements AutoCloseable {

  private final IHttpClient client;

  public RequestContext(IHttpClient client) {
    this.client = client;
  }

  public IHttpClient getClient() {
    return client;
  }

  @Override
  public void close() {
  }
}
