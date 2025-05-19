package io.contek.invoker.commons.actor;

import io.contek.invoker.commons.actor.http.IHttpClient;
import io.contek.invoker.commons.actor.http.IHttpClientFactory;
import io.contek.invoker.commons.actor.http.IHttpContext;
import io.contek.invoker.security.ApiKey;
import io.contek.invoker.security.ICredential;
import io.contek.invoker.security.ICredentialFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class SimpleActorFactory implements IActorFactory {

  private final ICredentialFactory credentialFactory;
  private final IHttpClientFactory httpClientFactory;

  private SimpleActorFactory(
      ICredentialFactory credentialFactory,
      IHttpClientFactory httpClientFactory) {
    this.credentialFactory = credentialFactory;
    this.httpClientFactory = httpClientFactory;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public IActor create(@Nullable ApiKey apiKey, IHttpContext context) {
    ICredential credential =
        apiKey == null ? ICredential.anonymous() : credentialFactory.create(apiKey);
    IHttpClient httpClient = httpClientFactory.create(context);
    return new SimpleActor(credential, httpClient);
  }

  @NotThreadSafe
  public static final class Builder {

    private ICredentialFactory credentialFactory;
    private IHttpClientFactory httpClientFactory;

    public Builder setCredentialFactory(ICredentialFactory credentialFactory) {
      this.credentialFactory = credentialFactory;
      return this;
    }

    public Builder setHttpClientFactory(IHttpClientFactory httpClientFactory) {
      this.httpClientFactory = httpClientFactory;
      return this;
    }

    public SimpleActorFactory build() {
      if (credentialFactory == null) {
        throw new IllegalArgumentException("No credential factory specified");
      }
      if (httpClientFactory == null) {
        throw new IllegalArgumentException("No http client factory specified");
      }

      return new SimpleActorFactory(credentialFactory, httpClientFactory);
    }

    private Builder() {}
  }
}
