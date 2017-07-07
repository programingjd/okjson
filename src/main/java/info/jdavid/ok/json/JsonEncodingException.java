package info.jdavid.ok.json;

import java.io.IOException;
import javax.annotation.Nullable;


@SuppressWarnings("WeakerAccess")
public final class JsonEncodingException extends IOException {

  JsonEncodingException(final @Nullable String message) {
    super(message);
  }

}
