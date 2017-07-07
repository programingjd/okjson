package info.jdavid.ok.json;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import okio.Buffer;
import okio.BufferedSink;


@SuppressWarnings({ "WeakerAccess", "unused", "SameParameterValue" })
public class Builder {

  private static final String DEFAULT_INDENTATION = "  ";
  private static final Pattern INDENTATION_PATTERN = Pattern.compile("^(?:\t*| *)$");

  /**
   * Validates the given map as a valid representation of a json object.
   * @param map the map to inspect.
   * @return true if the map is a valid representation of a json object, false if it isn't.
   */
  public static boolean isValidObject(@Nullable final Map<? extends CharSequence, ?> map) {
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
  public static @Nullable String build(@Nullable final Map<? extends CharSequence, ?> map) {
    final Buffer buffer = new Buffer();
    buildWithIndent(buffer, map, null);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given map representation of a json object to its string representation.
   * @param map the json object.
   * @param indent whether to indent (2 spaces) the result or not.
   * @return either a string or null if the map doesn't represent a valid json object.
   */
  public static @Nullable String build(@Nullable final Map<? extends CharSequence, ?> map,
                                       final boolean indent) {
    final Buffer buffer = new Buffer();
    buildWithIndent(buffer, map, indent ? DEFAULT_INDENTATION : null);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given map representation of a json object to its string representation.
   * @param map the json object.
   * @param indent the indentation string (tabs or spaces), which can be null.
   * @return either a string or null if the map doesn't represent a valid json object.
   */
  public static @Nullable String build(@Nullable final Map<? extends CharSequence, ?> map,
                                       @Nullable final String indent) {
    final Buffer buffer = new Buffer();
    build(buffer, map, indent);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Writes the given map representation of a json object to a {@link okio.BufferedSink}.
   * @param sink the target buffer.
   * @param map the map representation of the json object.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Map<? extends CharSequence, ?> map) {
    buildWithIndent(sink, map, null);
  }

  /**
   * Writes the given map representation of a json object to a {@link okio.BufferedSink}.
   * @param sink the target buffer.
   * @param map the map representation of the json object.
   * @param indent whether to indent (2 spaces) the result or not.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Map<? extends CharSequence, ?> map,
                           final boolean indent) {
    buildWithIndent(sink, map, indent ? DEFAULT_INDENTATION : null);
  }

  /**
   * Writes the given map representation of a json object to a {@link okio.BufferedSink}.
   * @param sink the target buffer.
   * @param map the map representation of the json object.
   * @param indent the indentation string (tabs or spaces), which can be null.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Map<? extends CharSequence, ?> map,
                           @Nullable final String indent) {
    if (indent != null && !INDENTATION_PATTERN.matcher(indent).matches()) {
      throw new IllegalArgumentException("Invalid indentation string.");
    }
    buildWithIndent(sink, map, indent == null || indent.length() == 0 ? null : indent);
  }

  private static void buildWithIndent(@Nullable final BufferedSink sink,
                                      @Nullable final Map<? extends CharSequence, ?> map,
                                      @Nullable final String indent) {
    if (map == null) return;
    if (sink == null) return;
    final JsonWriter writer = new JsonWriter(sink);
    if (indent != null) writer.setIndent(indent);
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
   * @return true if the iterable is a valid representation of a json array, false if it isn't.
   */
  public static boolean isValidArray(@Nullable final Iterable<?> list) {
    try {
      return list != null && walk(list);
    }
    catch (final ClassCastException e) {
      return false;
    }
  }

  /**
   * Validates the given list (iterator) as a valid representation of a json array.
   * @param iterator the iterator to inspect.
   * @return true if the iterator is a valid representation of a json array, false if it isn't.
   */
  public static boolean isValidArray(@Nullable final Iterator<?> iterator) {
    try {
      return iterator != null && walk(iterator);
    }
    catch (final ClassCastException e) {
      return false;
    }
  }

  /**
   * Validates the given list (enumeration) as a valid representation of a json array.
   * @param enumeration the enumeration to inspect.
   * @return true if the enumeration is a valid representation of a json array, false if it isn't.
   */
  public static boolean isValidArray(@Nullable final Enumeration<?> enumeration) {
    try {
      return enumeration != null && walk(enumeration);
    }
    catch (final ClassCastException e) {
      return false;
    }
  }

  /**
   * Converts the given list (iterable) representation of a json array to its string representation.
   * @param list the json array.
   * @return either a string or null if the iterable doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Iterable<?> list) {
    final Buffer buffer = new Buffer();
    buildWithIndent(buffer, list, null);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given list (iterator) representation of a json array to its string representation.
   * @param iterator the json array.
   * @return either a string or null if the iterator doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Iterator<?> iterator) {
    final Buffer buffer = new Buffer();
    buildWithIndent(buffer, iterator, null);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given list (enumeration) representation of a json array to its string representation.
   * @param enumeration the json array.
   * @return either a string or null if the enumeration doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Enumeration<?> enumeration) {
    final Buffer buffer = new Buffer();
    buildWithIndent(buffer, enumeration, null);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given list (iterable) representation of a json array to its string representation.
   * @param list the json array.
   * @param indent whether to indent (2 spaces) the result or not.
   * @return either a string or null if the iterable doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Iterable<?> list, final boolean indent) {
    final Buffer buffer = new Buffer();
    buildWithIndent(buffer, list, indent ? DEFAULT_INDENTATION : null);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given list (iterator) representation of a json array to its string representation.
   * @param iterator the json array.
   * @param indent whether to indent (2 spaces) the result or not.
   * @return either a string or null if the iterator doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Iterator<?> iterator, final boolean indent) {
    final Buffer buffer = new Buffer();
    buildWithIndent(buffer, iterator, indent ? DEFAULT_INDENTATION : null);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given list (enumeration) representation of a json array to its string representation.
   * @param enumeration the json array.
   * @param indent whether to indent (2 spaces) the result or not.
   * @return either a string or null if the enumeration doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Enumeration<?> enumeration, final boolean indent) {
    final Buffer buffer = new Buffer();
    buildWithIndent(buffer, enumeration, indent ? DEFAULT_INDENTATION : null);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given list (iterable) representation of a json array to its string representation.
   * @param list the json array.
   * @param indent the indentation string (tabs or spaces), which can be null.
   * @return either a string or null if the iterable doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Iterable<?> list,
                                       @Nullable final String indent) {
    final Buffer buffer = new Buffer();
    build(buffer, list, indent);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given list (iterator) representation of a json array to its string representation.
   * @param iterator the json array.
   * @param indent the indentation string (tabs or spaces), which can be null.
   * @return either a string or null if the iterator doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Iterator<?> iterator,
                                       @Nullable final String indent) {
    final Buffer buffer = new Buffer();
    build(buffer, iterator, indent);
    if (buffer.size() == 0) return null;
    try {
      return buffer.readUtf8();
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts the given list (enumeration) representation of a json array to its string representation.
   * @param enumeration the json array.
   * @param indent the indentation string (tabs or spaces), which can be null.
   * @return either a string or null if the enumeration doesn't represent a valid json array.
   */
  public static @Nullable String build(@Nullable final Enumeration<?> enumeration,
                                       @Nullable final String indent) {
    final Buffer buffer = new Buffer();
    build(buffer, enumeration, indent);
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
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Iterable<?> list) {
    buildWithIndent(sink, list, null);
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param iterator the list (iterator) representation of the json array.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Iterator<?> iterator) {
    buildWithIndent(sink, iterator, null);
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param enumeration the list (enumeration) representation of the json array.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Enumeration<?> enumeration) {
    buildWithIndent(sink, enumeration, null);
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param list the list (iterable) representation of the json array.
   * @param indent whether to indent (2 spaces) the result or not.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Iterable<?> list,
                           final boolean indent) {
    buildWithIndent(sink, list, indent ? DEFAULT_INDENTATION : null);
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param iterator the list (iterator) representation of the json array.
   * @param indent whether to indent (2 spaces) the result or not.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Iterator<?> iterator,
                           final boolean indent) {
    buildWithIndent(sink, iterator, indent ? DEFAULT_INDENTATION : null);
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param enumeration the list (enumeration) representation of the json array.
   * @param indent whether to indent (2 spaces) the result or not.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Enumeration<?> enumeration,
                           final boolean indent) {
    buildWithIndent(sink, enumeration, indent ? DEFAULT_INDENTATION : null);
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param list the list (iterable) representation of the json array.
   * @param indent the indentation string (tabs or spaces), which can be null.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Iterable<?> list,
                           @Nullable final String indent) {
    if (indent != null && !INDENTATION_PATTERN.matcher(indent).matches()) {
      throw new IllegalArgumentException("Invalid indentation string.");
    }
    buildWithIndent(sink, list, indent == null || indent.length() == 0 ? null : indent);
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param iterator the list (iterator) representation of the json array.
   * @param indent the indentation string (tabs or spaces), which can be null.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Iterator<?> iterator,
                           @Nullable final String indent) {
    if (indent != null && !INDENTATION_PATTERN.matcher(indent).matches()) {
      throw new IllegalArgumentException("Invalid indentation string.");
    }
    buildWithIndent(sink, iterator, indent == null || indent.length() == 0 ? null : indent);
  }

  /**
   * Writes the string representation of a given json array (represented by a list) to a
   * {@link okio.BufferedSource}.
   * @param sink the target buffer.
   * @param enumeration the list (enumeration) representation of the json array.
   * @param indent the indentation string (tabs or spaces), which can be null.
   */
  public static void build(@Nullable final BufferedSink sink,
                           @Nullable final Enumeration<?> enumeration,
                           @Nullable final String indent) {
    if (indent != null && !INDENTATION_PATTERN.matcher(indent).matches()) {
      throw new IllegalArgumentException("Invalid indentation string.");
    }
    buildWithIndent(sink, enumeration, indent == null || indent.length() == 0 ? null : indent);
  }

  @SuppressWarnings("Duplicates")
  private static void buildWithIndent(@Nullable final BufferedSink sink,
                                      @Nullable final Iterable<?> list,
                                      @Nullable final String indent) {
    if (list == null) return;
    if (sink == null) return;
    final JsonWriter writer = new JsonWriter(sink);
    if (indent != null) writer.setIndent(indent);
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

  @SuppressWarnings("Duplicates")
  private static void buildWithIndent(@Nullable final BufferedSink sink,
                                      @Nullable final Iterator<?> iterator,
                                      @Nullable final String indent) {
    if (iterator == null) return;
    if (sink == null) return;
    final JsonWriter writer = new JsonWriter(sink);
    if (indent != null) writer.setIndent(indent);
    try {
      writer.beginArray();
      walk(writer, iterator);
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

  @SuppressWarnings("Duplicates")
  private static void buildWithIndent(@Nullable final BufferedSink sink,
                                      @Nullable final Enumeration<?> enumeration,
                                      @Nullable final String indent) {
    if (enumeration == null) return;
    if (sink == null) return;
    final JsonWriter writer = new JsonWriter(sink);
    if (indent != null) writer.setIndent(indent);
    try {
      writer.beginArray();
      walk(writer, enumeration);
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
          walk(writer, (Map<String, ?>)value);
          writer.endObject();
        }
        else if (value instanceof Iterable) {
          writer.name(entry.getKey().toString());
          writer.beginArray();
          walk(writer, (Iterable<?>)value);
          writer.endArray();
        }
        else if (value instanceof Iterator) {
          writer.name(entry.getKey().toString());
          writer.beginArray();
          walk(writer, (Iterator<?>)value);
          writer.endArray();
        }
        else if (value instanceof Enumeration) {
          writer.name(entry.getKey().toString());
          writer.beginArray();
          walk(writer, (Enumeration<?>)value);
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
      value(writer, value);
    }
  }

  private static void walk(final JsonWriter writer, final Iterator<?> iterator) {
    while (iterator.hasNext()) {
      final Object value = iterator.next();
      value(writer, value);
    }
  }

  private static void walk(final JsonWriter writer, final Enumeration<?> enumeration) {
    while (enumeration.hasMoreElements()) {
      final Object value = enumeration.nextElement();
      value(writer, value);
    }
  }

  private static void value(final JsonWriter writer, @Nullable final Object value) {
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
        walk(writer, (Map<String, ?>)value);
        writer.endObject();
      }
      else if (value instanceof Iterable) {
        writer.beginArray();
        walk(writer, (Iterable<?>)value);
        writer.endArray();
      }
      else if (value instanceof Iterator) {
        writer.beginArray();
        walk(writer, (Iterator<?>)value);
        writer.endArray();
      }
      else if (value instanceof Enumeration) {
        writer.beginArray();
        walk(writer, (Enumeration<?>)value);
        writer.endArray();
      }
    }
    catch (final IOException e) {
      Logger.log(e);
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
      if (value instanceof Map && walk((Map<String, ?>)value)) {
        continue;
      }
      if (value instanceof Iterable && walk((Iterable<?>)value)) {
        continue;
      }
      if (value instanceof Iterator && walk((Iterator<?>)value)) {
        continue;
      }
      if (value instanceof Enumeration && walk((Enumeration<?>)value)) {
        continue;
      }
      return false;
    }
    return true;
  }

  private static boolean walk(final Iterable<?> list) {
    for (final Object value: list) {
      if (!value(value)) return false;
    }
    return true;
  }

  private static boolean walk(final Iterator<?> iterator) {
    while (iterator.hasNext()) {
      final Object value = iterator.next();
      if (!value(value)) return false;
    }
    return true;
  }

  private static boolean walk(final Enumeration<?> enumeration) {
    while (enumeration.hasMoreElements()) {
      final Object value = enumeration.nextElement();
      if (!value(value)) return false;
    }
    return true;
  }

  private static boolean value(@Nullable final Object value) {
    if (value == null ||
        value instanceof String ||
        value instanceof Number ||
        value instanceof Boolean ||
        value instanceof CharSequence) {
      return true;
    }
    //noinspection unchecked
    if (value instanceof Map && walk((Map<String, ?>)value)) {
      return true;
    }
    if (value instanceof Iterable && walk((Iterable<?>)value)) {
      return true;
    }
    if (value instanceof Iterator && walk((Iterator<?>)value)) {
      return true;
    }
    //noinspection RedundantIfStatement
    if (value instanceof Enumeration && walk((Enumeration<?>)value)) {
      return true;
    }
    return false;
  }

}
