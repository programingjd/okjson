package info.jdavid.ok.json;

import java.io.IOException;
import javax.annotation.Nullable;

final class JsonEncodingException extends IOException {

  public JsonEncodingException(final @Nullable String message) {
    super(message);
  }

}
