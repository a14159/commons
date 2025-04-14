package io.contek.invoker.commons.actor;

import io.contek.invoker.commons.actor.http.IHttpClient;
import io.contek.invoker.commons.actor.ratelimit.TypedPermitRequest;
import io.contek.invoker.security.ICredential;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Clock;
import java.util.List;

@ThreadSafe
public final class SimpleActorNoThrottle implements IActor {

  private final ICredential credential;
  private final IHttpClient httpClient;

  public SimpleActorNoThrottle(ICredential credential, IHttpClient httpClient) {
    this.credential = credential;
    this.httpClient = httpClient;
  }

  @Override
  public ICredential getCredential() {
    return credential;
  }

  @Override
  public RequestContext getRequestContext(String requestName, List<TypedPermitRequest> ignored) {
    return new RequestContext(httpClient, null);
  }

  @Override
  public Clock getClock() {
    return Clock.systemUTC();
  }
}
