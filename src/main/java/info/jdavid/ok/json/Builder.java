package info.jdavid.ok.json;

import java.io.IOException;
import java.util.Map;

import com.squareup.moshi.JsonWriter;
import okio.Buffer;
import okio.BufferedSink;

@SuppressWarnings("WeakerAccess")
public class Builder {

  /**
   * Validates the given map as a valid representation of a json object.
   * @param map the map to inspect.
   * @return true if the map is a valid representation of a json object, false if it isn't.
   */
  public static boolean isValidObject(final Map<? extends CharSequence, ?> map) {
    try {
      return map != null && walk(map);
    }
    catch (final ClassCastException e) {
      return false;
    }
  }

  /**
   * Converts the given map representation of a json object to its string representation.
   * @param map the json object.
   * @return either a string or null if the map doesn't represent a valid json object.
   */
  public static String build(final Map<? extends CharSequence, ?> map) {
    final Buffer buffer = new Buffer();
    build(buffer, map);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  public static void build(final BufferedSink sink, final Map<? extends CharSequence, ?> map) {
    if (map == null) return;
    final JsonWriter writer;
    try {
      writer = JsonWriter.of(sink);
    }
    catch (final NullPointerException e) {
      return;
    }
    try {
      writer.beginObject();
      walk(writer, map);
      writer.endObject();
    }
    catch (final IOException e) {
      Logger.log(e);
    }
    finally {
      try {
        writer.close();
      }
      catch (final IOException ignore) {}
    }
  }

  /**
   * Validates the given list (iterable) as a valid representation of a json array.
   * @param list the list (iterable) to inspect.
   * @return true if the list is a valid representation of a json array, false if it isn't.
   */
  public static boolean isValidArray(final Iterable<?> list) {
    try {
      return list != null && walk(list);
    }
    catch (final ClassCastException e) {
      return false;
    }
  }

  /**
   * Converts the given list (iterable) representation of a json array to its string representation.
   * @param list the json array.
   * @return either a string or null if the list doesn't represent a valid json array.
   */
  public static String build(final Iterable<?> list) {
    final Buffer buffer = new Buffer();
    build(buffer, list);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param list the list (iterable) representation of the json array.
   */
  public static void build(final BufferedSink sink, final Iterable<?> list) {
    if (list == null) return;
    final JsonWriter writer;
    try {
      writer = JsonWriter.of(sink);
    }
    catch (final NullPointerException e) {
      return;
    }
    try {
      writer.beginArray();
      walk(writer, list);
      writer.endArray();
    }
    catch (final IOException e) {
      Logger.log(e);
    }
    finally {
      try {
        writer.close();
      }
      catch (final IOException ignore) {}
    }
  }

  private Builder() {}

  private static void walk(final JsonWriter writer, final Map<? extends CharSequence, ?> map) {
    for (final Map.Entry<? extends CharSequence, ?> entry: map.entrySet()) {
      try {
        final Object value = entry.getValue();
        if (value == null) {
          writer.name(entry.getKey().toString());
          writer.nullValue();
        }
        else if (value instanceof String) {
          writer.name(entry.getKey().toString());
          writer.value((String)value);
        }
        else if (value instanceof Number) {
          writer.name(entry.getKey().toString());
          writer.value(((Number)value));
        }
        else if (value instanceof Boolean) {
          writer.name(entry.getKey().toString());
          writer.value((Boolean)value);
        }
        else if (value instanceof CharSequence) {
          writer.name(entry.getKey().toString());
          writer.value(value.toString());
        }
        else if (value instanceof Map) {
          writer.name(entry.getKey().toString());
          writer.beginObject();
          //noinspection unchecked
          walk(writer, (Map)value);
          writer.endObject();
        }
        else if (value instanceof Iterable) {
          writer.name(entry.getKey().toString());
          writer.beginArray();
          //noinspection unchecked
          walk(writer, (Iterable)value);
          writer.endArray();
        }
      }
      catch (final IOException e) {
        Logger.log(e);
      }
    }
  }

  private static void walk(final JsonWriter writer, final Iterable<?> list) {
    for (final Object value: list) {
      try {
        if (value == null) {
          writer.nullValue();
        }
        else if (value instanceof String) {
          writer.value((String)value);
        }
        else if (value instanceof Number) {
          writer.value(((Number)value));
        }
        else if (value instanceof Boolean) {
          writer.value((Boolean)value);
        }
        else if (value instanceof CharSequence) {
          writer.value(value.toString());
        }
        else if (value instanceof Map) {
          writer.beginObject();
          //noinspection unchecked
          walk(writer, (Map)value);
          writer.endObject();
        }
        else if (value instanceof Iterable) {
          writer.beginArray();
          //noinspection unchecked
          walk(writer, (Iterable)value);
          writer.endArray();
        }
      }
      catch (final IOException e) {
        Logger.log(e);
      }
    }
  }

  private static boolean walk(final Map<? extends CharSequence, ?> map) {
    for (final Map.Entry<? extends CharSequence, ?> entry: map.entrySet()) {
      //noinspection ConstantConditions
      if (!(entry.getKey() instanceof CharSequence)) return false;
      final Object value = entry.getValue();
      if (value == null ||
          value instanceof String ||
          value instanceof Number ||
          value instanceof Boolean ||
          value instanceof CharSequence) {
        continue;
      }
      //noinspection unchecked
      if (value instanceof Map && walk((Map)value)) {
        continue;
      }
      //noinspection unchecked
      if (value instanceof Iterable && walk((Iterable)value)) {
        continue;
      }
      return false;
    }
    return true;
  }

  private static boolean walk(final Iterable<?> list) {
    for (final Object value: list) {
      if (value == null ||
          value instanceof String ||
          value instanceof Number ||
          value instanceof Boolean ||
          value instanceof CharSequence) {
        continue;
      }
      //noinspection unchecked
      if (value instanceof Map && walk((Map)value)) {
        continue;
      }
      //noinspection unchecked
      if (value instanceof Iterable && walk((Iterable)value)) {
        continue;
      }
      return false;
    }
    return true;
  }

}
