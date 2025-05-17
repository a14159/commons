package io.contek.invoker.commons.actor.http;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class ParsedHttpException extends AnyHttpException {

  private final Object parsedEntity;

  public ParsedHttpException(int code, Object parsedEntity, String message) {
    super(code, message);
    this.parsedEntity = parsedEntity;
  }

  public Object getParsedEntity() {
    return parsedEntity;
  }

  public <T> T getParsedEntityAs(Class<T> type) {
    T result = tryGetParsedEntityAs(type);
    if (result == null) {
      throw new ClassCastException(
          "Expected error type: " + type + ", actual type: " + parsedEntity.getClass());
    }
    return result;
  }

  @Nullable
  public <T> T tryGetParsedEntityAs(Class<T> type) {
    if (type.isAssignableFrom(parsedEntity.getClass())) {
      return type.cast(parsedEntity);
    }
    return null;
  }
}
