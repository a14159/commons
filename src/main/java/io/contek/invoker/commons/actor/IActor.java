package io.contek.invoker.commons.actor;

import io.contek.invoker.security.ICredential;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Clock;

@ThreadSafe
public interface IActor {

  ICredential getCredential();

  RequestContext getRequestContext(String requestName)
      throws InterruptedException;

  Clock getClock();
}
