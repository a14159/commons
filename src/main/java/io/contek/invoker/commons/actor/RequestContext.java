package io.contek.invoker.commons.actor;

import io.contek.invoker.commons.actor.http.IHttpClient;
import io.contek.ursa.IPermitSession;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class RequestContext implements AutoCloseable {

  private final IHttpClient client;
  private final IPermitSession permit;

  public RequestContext(IHttpClient client, @Nullable IPermitSession permit) {
    this.client = client;
    this.permit = permit;
  }

  public IHttpClient getClient() {
    return client;
  }

  @Override
  public void close() {
    if (permit != null)
      permit.close();
  }
}
