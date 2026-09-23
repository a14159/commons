package io.contek.invoker.commons.actor.http;

import javax.annotation.concurrent.Immutable;

/**
 * @deprecated Use {@link HttpClientFactory}. This compatibility wrapper shares its cached clients.
 */
@Deprecated
@Immutable
public final class SimpleHttpClientFactory implements IHttpClientFactory {

  public static boolean USE_LOGGING = false;

  public static boolean NO_NAGLE_KEEP_ALIVE = true;

  private SimpleHttpClientFactory() {}

  public static SimpleHttpClientFactory getInstance() {
    return InstanceHolder.INSTANCE;
  }

  @Override
  public IHttpClient create(IHttpContext context) {
    return HttpClientFactory.getInstance().create(context, USE_LOGGING, NO_NAGLE_KEEP_ALIVE);
  }

  @Immutable
  private static final class InstanceHolder {

    private static final SimpleHttpClientFactory INSTANCE = new SimpleHttpClientFactory();
  }
}
