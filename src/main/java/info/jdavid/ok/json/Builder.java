package info.jdavid.ok.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.squareup.moshi.JsonWriter;
import okio.Buffer;
import okio.BufferedSink;

public class Builder {

  /**
   * Validates the given map as a valid representation of a json object.
   * @param map the map to inspect.
   * @return true if the map is a valid representation of a json object, false if it isn't.
   */
  public static boolean isValidObject(final Map<String, ?> map) {
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
  public static String build(final Map<String, ?> map) {
    final Buffer buffer = new Buffer();
    build(buffer, map);
    if (buffer.size() == 0) return null;
    return buffer.readUtf8();
  }

  /**
   * Writes the string representation of a given json object (represented by a map) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param map the map representation of the json object.
   */
  public static void build(final BufferedSink sink, final Map<String, ?> map) {
    if (map == null) return;
    final JsonWriter writer;
    try {
      writer = new JsonWriter(sink);
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
  }

  /**
   * Validates the given list as a valid representation of a json array.
   * @param list the list to inspect.
   * @return true if the list is a valid representation of a json array, false if it isn't.
   */
  public static boolean isValidArray(final List<?> list) {
    try {
      return list != null && walk(list);
    }
    catch (final ClassCastException e) {
      return false;
    }
  }

  /**
   * Converts the given list representation of a json array to its string representation.
   * @param list the json array.
   * @return either a string or null if the list doesn't represent a valid json array.
   */
  public static String build(final List<?> list) {
    final Buffer buffer = new Buffer();
    build(buffer, list);
    if (buffer.size() == 0) return null;
    return buffer.readUtf8();
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param list the list representation of the json array.
   */
  public static void build(final BufferedSink sink, final List<?> list) {
    if (list == null) return;
    final JsonWriter writer;
    try {
      writer = new JsonWriter(sink);
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
  }

  private Builder() {}

  private static void walk(final JsonWriter writer, final Map<String, ?> map) {
    for (final Map.Entry<String, ?> entry: map.entrySet()) {
      try {
        final Object value = entry.getValue();
        if (value == null) {
          writer.name(entry.getKey());
          writer.nullValue();
        }
        if (value instanceof String) {
          writer.name(entry.getKey());
          writer.value((String)value);
        }
        else if (value instanceof Number) {
          writer.name(entry.getKey());
          writer.value(((Number)value));
        }
        else if (value instanceof Boolean) {
          writer.name(entry.getKey());
          writer.value((Boolean)value);
        }
        else if (value instanceof Map) {
          writer.name(entry.getKey());
          writer.beginObject();
          //noinspection unchecked
          walk(writer, (Map)value);
          writer.endObject();
        }
        else if (value instanceof List) {
          writer.name(entry.getKey());
          writer.beginArray();
          //noinspection unchecked
          walk(writer, (List)value);
          writer.endArray();
        }
      }
      catch (final IOException e) {
        Logger.log(e);
      }
    }
  }

  private static void walk(final JsonWriter writer, final List<?> list) {
    for (final Object value: list) {
      try {
        if (value == null) {
          writer.nullValue();
        }
        if (value instanceof String) {
          writer.value((String)value);
        }
        else if (value instanceof Number) {
          writer.value(((Number)value));
        }
        else if (value instanceof Boolean) {
          writer.value((Boolean)value);
        }
        else if (value instanceof Map) {
          writer.beginObject();
          //noinspection unchecked
          walk(writer, (Map)value);
          writer.endObject();
        }
        else if (value instanceof List) {
          writer.beginArray();
          //noinspection unchecked
          walk(writer, (List)value);
          writer.endArray();
        }
      }
      catch (final IOException e) {
        Logger.log(e);
      }
    }
  }

  private static boolean walk(final Map<String, ?> map) {
    for (final Map.Entry<String, ?> entry: map.entrySet()) {
      String key = entry.getKey();
      if (key == null) return false;
      final Object value = entry.getValue();
      if (value == null ||
          value instanceof String ||
          value instanceof Number ||
          value instanceof Boolean) {
        continue;
      }
      //noinspection unchecked
      if (value instanceof Map && walk((Map)value)) {
        continue;
      }
      //noinspection unchecked
      if (value instanceof List && walk((List)value)) {
        continue;
      }
      return false;
    }
    return true;
  }

  private static boolean walk(final List<?> list) {
    for (final Object value: list) {
      if (value == null ||
          value instanceof String ||
          value instanceof Number ||
          value instanceof Boolean) {
        continue;
      }
      //noinspection unchecked
      if (value instanceof Map && walk((Map)value)) {
        continue;
      }
      //noinspection unchecked
      if (value instanceof List && walk((List)value)) {
        continue;
      }
      return false;
    }
    return true;
  }

}
