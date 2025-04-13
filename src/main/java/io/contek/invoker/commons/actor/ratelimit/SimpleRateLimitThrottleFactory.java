package io.contek.invoker.commons.actor.ratelimit;

import io.contek.ursa.cache.LimiterManager;

import javax.annotation.concurrent.ThreadSafe;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

@ThreadSafe
public final class SimpleRateLimitThrottleFactory implements IRateLimitThrottleFactory {

  private final LimiterManager manager;
  private final List<IRateLimitQuotaInterceptor> interceptors;

  private SimpleRateLimitThrottleFactory(
      LimiterManager manager, List<IRateLimitQuotaInterceptor> interceptors) {
    this.manager = manager;
    this.interceptors = interceptors;
  }

  public static SimpleRateLimitThrottleFactory create(
      LimiterManager cache, List<IRateLimitQuotaInterceptor> interceptors) {
    return new SimpleRateLimitThrottleFactory(cache, Collections.unmodifiableList(interceptors));
  }

  @Override
  public IRateLimitThrottle create(InetAddress boundLocalAddress, String apiKeyId) {
    return new SimpleRateLimitThrottle(
        boundLocalAddress.getCanonicalHostName(), apiKeyId, manager, interceptors);
  }
}
