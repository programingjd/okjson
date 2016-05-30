package info.jdavid.ok.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;

import okio.Buffer;
import okio.BufferedSource;

@SuppressWarnings("WeakerAccess")
public final class Parser {

  /**
   * Converts the given String to a
   * {@link okio.BufferedSource} and passes it to {@link #parse(BufferedSource)}.
   * @param s the json string to parse.
   * @param <T> List&lt;?&gt; or Map&lt;String, ?&gt;.
   * @return either a map representing a json object, or a list representing a json array,
   * or even null if the string is not valid json.
   */
  public static <T> T parse(final String s) {
    final Buffer buffer = new Buffer();
    try {
      return parse(buffer.writeUtf8(s));
    }
    finally {
      buffer.close();
    }
  }

  /**
   * Converts a json string to its object representation. The representation is using
   * {@link java.util.Map}s for json objects and {@link java.util.List}s for json arrays.
   * @param source the json string as an okio source.
   * @param <T> List&lt;?&gt; or Map&lt;String, ?&gt;.
   * @return the object representation of the json string, or null if the source is not valid json.
   */
  public static <T> T parse(final BufferedSource source) {
    final JsonReader reader;
    try {
      reader = JsonReader.of(source);
    }
    catch (final NullPointerException e) {
      return null;
    }
    try {
      final JsonReader.Token token = nextToken(reader);
      if (token == null) return null;
      switch (token) {
        case BEGIN_OBJECT: {
          try {
            reader.beginObject();
          }
          catch (final IOException e) {
            Logger.log(e);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            break;
          }
          //noinspection unchecked
          return (T)walk(reader, new HashMap());
        }
        case BEGIN_ARRAY: {
          try {
            reader.beginArray();
          }
          catch (final IOException e) {
            Logger.log(e);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            break;
          }
          //noinspection unchecked
          return (T)walkArray(reader, new ArrayList());
        }
      }
      return null;
    }
    finally {
      try {
        reader.close();
      }
      catch (final IOException ignore) {}
    }
  }

  private Parser() {}

  private static JsonReader.Token nextToken(final JsonReader reader) {
    if (reader == null) return null;
    try {
      return reader.peek();
    }
    catch (final IOException e) {
      Logger.log(e);
      return null;
    }
  }

  private static Number nextNumber(final JsonReader reader) throws IOException, JsonDataException {
    return stringToNumber(reader.nextString());

  }

  static Number stringToNumber(final String s) {
    if (s.indexOf('.') == -1) {
      // integer or long
      final int n = s.length();
      if (n < 10) {
        return Integer.valueOf(s);
      }
      else if (n > 11) {
        return Long.valueOf(s);
      }
      else {
        final boolean negative = s.indexOf('-') == 0;
        if (n == 10) {
          if (negative) return Integer.valueOf(s);
          final char first = s.charAt(0);
          if (first == '1') return Integer.valueOf(s);
          if (first > '2') return Long.valueOf(s);
          final Long longValue = Long.valueOf(s);
          if (longValue > Integer.MAX_VALUE) {
            return longValue;
          }
          else {
            return longValue.intValue();
          }
        }
        else { // n == 11
          if (!negative) return Long.valueOf(s);
          final char first = s.charAt(1);
          if (first == '1') return Integer.valueOf(s);
          if (first > '2') return Long.valueOf(s);
          final Long longValue = Long.valueOf(s);
          if (longValue < Integer.MIN_VALUE) {
            return longValue;
          }
          else {
            return longValue.intValue();
          }
        }
      }
    }
    else {
      return Double.valueOf(s);
    }
  }

  private static Map<String, ?> walk(final JsonReader reader, final Map<String, ?> map) {
    JsonReader.Token token;
    while ((token = nextToken(reader)) != null) {
      switch (token) {
        case NAME: {
          final String name;
          try {
            name = reader.nextName();
          }
          catch (final IOException e) {
            Logger.log(e);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            break;
          }
          walk(reader, map, name);
          break;
        }
        case END_OBJECT: {
          try {
            reader.endObject();
          }
          catch (final IOException e) {
            Logger.log(e);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            break;
          }
          return map;
        }
        default: {
          try {
            reader.skipValue();
          }
          catch (final IOException e) {
            Logger.log(e);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            break;
          }
        }
      }
    }
    return map;
  }

  private static void walk(final JsonReader reader, final Map<String, ?> map, final String name) {
    final JsonReader.Token token = nextToken(reader);
    if (token == null) return;
    switch (token) {
      case BEGIN_OBJECT: {
        try {
          reader.beginObject();
        }
        catch (final IOException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
          break;
        }
        catch (final JsonDataException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
          break;
        }
        //noinspection unchecked
        ((Map)map).put(name, walk(reader, new HashMap()));
        break;
      }
      case BEGIN_ARRAY: {
        try {
          reader.beginArray();
        }
        catch (final IOException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
          break;
        }
        catch (final JsonDataException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
          break;
        }
        //noinspection unchecked
        ((Map)map).put(name, walkArray(reader, new ArrayList()));
        break;
      }
      case NULL: {
        try {
          reader.nextNull();
        }
        catch (final IOException e) {
          Logger.log(e);
        }
        catch (final JsonDataException e) {
          Logger.log(e);
        }
        //noinspection unchecked
        ((Map)map).put(name, null);
        break;
      }
      case BOOLEAN: {
        try {
          //noinspection unchecked
          ((Map)map).put(name, reader.nextBoolean());
        }
        catch (final IOException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
        }
        catch (final JsonDataException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
        }
        break;
      }
      case NUMBER: {
        try {
          //noinspection unchecked
          ((Map)map).put(name, nextNumber(reader));
        }
        catch (final IOException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
        }
        catch (final JsonDataException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
        }
        break;
      }
      case STRING: {
        try {
          //noinspection unchecked
          ((Map)map).put(name, reader.nextString());
        }
        catch (final IOException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
        }
        catch (final JsonDataException e) {
          Logger.log(e);
          //noinspection unchecked
          ((Map)map).put(name, null);
        }
        break;
      }
    }
  }

  private static List<?> walkArray(final JsonReader reader, final List<?> list) {
    JsonReader.Token token;
    while ((token = nextToken(reader)) != null) {
      switch (token) {
        case END_ARRAY: {
          try {
            reader.endArray();
          }
          catch (final IOException e) {
            Logger.log(e);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            break;
          }
          return list;
        }
        case BEGIN_OBJECT: {
          try {
            reader.beginObject();
          }
          catch (final IOException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
            break;
          }
          //noinspection unchecked
          ((List)list).add(walk(reader, new HashMap()));
          break;
        }
        case BEGIN_ARRAY: {
          try {
            reader.beginArray();
          }
          catch (final IOException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
            break;
          }
          //noinspection unchecked
          ((List)list).add(walkArray(reader, new ArrayList()));
          break;
        }
        case NULL: {
          try {
            reader.nextNull();
          }
          catch (final IOException e) {
            Logger.log(e);
          }
          catch (final JsonDataException e) {
            Logger.log(e);
          }
          //noinspection unchecked
          ((List)list).add(null);
          break;
        }
        case BOOLEAN: {
          try {
            //noinspection unchecked
            ((List)list).add(reader.nextBoolean());
          }
          catch (final IOException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
          }
          break;
        }
        case NUMBER: {
          try {
            //noinspection unchecked
            ((List)list).add(nextNumber(reader));
          }
          catch (final IOException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
          }
          break;
        }
        case STRING: {
          try {
            //noinspection unchecked
            ((List)list).add(reader.nextString());
          }
          catch (final IOException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            //noinspection unchecked
            ((List)list).add(null);
          }
          break;
        }
        default: {
          try {
            reader.skipValue();
          }
          catch (final IOException e) {
            Logger.log(e);
            break;
          }
          catch (final JsonDataException e) {
            Logger.log(e);
            break;
          }
        }
      }
    }
    return list;
  }

}

