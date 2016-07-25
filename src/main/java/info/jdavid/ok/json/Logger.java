package info.jdavid.ok.json;

import android.util.Log;

enum Logger {
  INSTANCE;

  private final LogHandler logger;

  Logger() {
    boolean android;
    try {
      Class.forName("android.util.LogPrinter");
      android = true;
    }
    catch (final ClassNotFoundException e) {
      android = false;
    }
    if (android) {
      logger = new LogHandler() {
        @Override public void log(final Exception e) {
          Log.e("JSON Parser", e.getMessage(), e);
        }
      };
    }
    else {
      logger = new LogHandler() {
        @Override public void log(final Exception e) { e.printStackTrace(); }
      };
    }
  }

  private interface LogHandler {
    void log(final Exception e);
  }

  public void exception(final Exception e) {
    logger.log(e);
  }

  public static void log(final Exception e) {
    INSTANCE.exception(e);
  }

}
