package io.contek.invoker.commons.rest;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.ByteString;
import okio.Options;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;

@Immutable
public final class RestResponse {

  private static final Options BYTE_ORDER_MARKS = Options.of(
      ByteString.decodeHex("efbbbf"),
      ByteString.decodeHex("feff"),
      ByteString.decodeHex("fffe0000"),
      ByteString.decodeHex("fffe"),
      ByteString.decodeHex("0000feff"));

  private final int code;
  private final byte[] bytes;
  private final Charset charset;

  RestResponse(int code, @Nullable ResponseBody body) throws IOException {
    this.code = code;
    if (body == null) {
      bytes = null;
      charset = UTF_8;
      return;
    }

    MediaType contentType = body.contentType();
    BufferedSource source = body.source();
    // Match ResponseBody.string(): a BOM overrides the declared charset and is consumed.
    charset = switch (source.select(BYTE_ORDER_MARKS)) {
      case 0 -> UTF_8;
      case 1 -> UTF_16BE;
      case 2 -> Charset.forName("UTF-32LE");
      case 3 -> UTF_16LE;
      case 4 -> Charset.forName("UTF-32BE");
      default -> contentType == null ? UTF_8 : contentType.charset(UTF_8);
    };
    bytes = source.readByteArray();
  }

  public int getCode() {
    return code;
  }

  @Nullable
  public String getStringValue() {
    return bytes == null ? null : new String(bytes, charset);
  }

  @Nullable
  public <T> T getAs(Class<T> type) throws RestParsingException {
    try {
      // Keep UTF-8 JSON as bytes; decode the full body only for text access or other charsets.
      return UTF_8.equals(charset)
          ? JSON.parseObject(bytes, type)
          : JSON.parseObject(getStringValue(), type);
    } catch (JSONException e) {
      throw new RestParsingException(code, this, type, e);
    }
  }
}
