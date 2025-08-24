package io.contek.invoker.commons.actor;

import io.contek.invoker.commons.actor.http.IHttpClient;
import io.contek.invoker.security.ICredential;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Clock;

@ThreadSafe
public final class SimpleActor implements IActor {

  private final ICredential credential;
  private final RequestContext requestContext;

  public SimpleActor(ICredential credential, IHttpClient httpClient) {
    this.credential = credential;
    this.requestContext = new RequestContext(httpClient);
  }

  @Override
  public ICredential getCredential() {
    return credential;
  }

  @Override
  public RequestContext getRequestContext(String requestName) {
    return requestContext;
  }

  @Override
  public Clock getClock() {
    return Clock.systemUTC();
  }
}
