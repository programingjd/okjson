package info.jdavid.ok.json;

import javax.annotation.Nullable;


final class JsonDataException extends RuntimeException {

  public JsonDataException() {}

  public JsonDataException(final @Nullable String message) {
    super(message);
  }

  public JsonDataException(final @Nullable Throwable cause) {
    super(cause);
  }

  public JsonDataException(final @Nullable String message, final @Nullable Throwable cause) {
    super(message, cause);
  }

}
