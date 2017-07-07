package info.jdavid.ok.json;

import javax.annotation.Nullable;


@SuppressWarnings("WeakerAccess")
public final class JsonDataException extends RuntimeException {

  JsonDataException(final @Nullable String message) {
    super(message);
  }

}
