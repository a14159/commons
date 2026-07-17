package io.contek.invoker.commons.rest;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class RestResponse {

  private final int code;
  private final String stringValue;

  RestResponse(int code, @Nullable String stringValue) {
    this.code = code;
    this.stringValue = stringValue;
  }

  public int getCode() {
    return code;
  }

  @Nullable
  public String getStringValue() {
    return stringValue;
  }

  @Nullable
  public <T> T getAs(Class<T> type) throws RestParsingException {
    try {
      return stringValue == null ? null : JSON.parseObject(stringValue, type);
    } catch (JSONException e) {
      throw new RestParsingException(code, this, type, e);
    }
  }
}
