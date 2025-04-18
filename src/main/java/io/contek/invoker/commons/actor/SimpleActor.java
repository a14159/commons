package io.contek.invoker.commons.actor;

import io.contek.invoker.commons.actor.http.IHttpClient;
import io.contek.invoker.security.ICredential;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Clock;

@ThreadSafe
public final class SimpleActor implements IActor {

  private final ICredential credential;
  private final IHttpClient httpClient;

  public SimpleActor(
      ICredential credential, IHttpClient httpClient) {
    this.credential = credential;
    this.httpClient = httpClient;
  }

  @Override
  public ICredential getCredential() {
    return credential;
  }

  @Override
  public RequestContext getRequestContext(String requestName)
      throws InterruptedException {
    return new RequestContext(httpClient);
  }

  @Override
  public Clock getClock() {
    return Clock.systemUTC();
  }
}
