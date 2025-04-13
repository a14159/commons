package io.contek.invoker.commons.actor.ratelimit;

import io.contek.ursa.AcquireTimeoutException;
import io.contek.ursa.IPermitSession;
import io.contek.ursa.cache.LimiterManager;
import io.contek.ursa.cache.PermitRequest;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.List;

@ThreadSafe
public final class SimpleRateLimitThrottle implements IRateLimitThrottle {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final String boundLocalAddress;
  private final String apiKeyId;

  private final LimiterManager manager;
  private final List<IRateLimitQuotaInterceptor> interceptors;

  SimpleRateLimitThrottle(
      String boundLocalAddress,
      @Nullable String apiKeyId,
      LimiterManager manager,
      List<IRateLimitQuotaInterceptor> interceptors) {
    this.boundLocalAddress = boundLocalAddress;
    this.apiKeyId = apiKeyId;
    this.manager = manager;
    this.interceptors = interceptors;
  }

  @Override
  public IPermitSession acquire(String requestName, List<TypedPermitRequest> quota)
      throws AcquireTimeoutException, InterruptedException {
    for (IRateLimitQuotaInterceptor interceptor : interceptors) {
      quota = interceptor.apply(requestName, quota);
    }
    List<PermitRequest> requests =
        quota.stream().map(this::toPermitRequest).toList();
    return manager.acquire(requests);
  }

  private PermitRequest toPermitRequest(TypedPermitRequest quota) {
    if (quota.getPermits() <= 0) {
      throw new IllegalArgumentException("No permits");
    }

    return switch (quota.getType()) {
      case IP -> PermitRequest.newBuilder()
              .setName(quota.getName())
              .setKey(boundLocalAddress)
              .setPermits(quota.getPermits())
              .setTimeout(TIMEOUT)
              .build();
      case API_KEY -> {
        if (apiKeyId == null) {
          throw new IllegalArgumentException();
        }
        yield PermitRequest.newBuilder()
                .setName(quota.getName())
                .setKey(apiKeyId)
                .setPermits(quota.getPermits())
                .setTimeout(TIMEOUT)
                .build();
      }
    };
  }
}
