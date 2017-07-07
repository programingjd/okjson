// Heavily inspired by
// https://github.com/square/moshi/blob/master/moshi/src/main/java/com/squareup/moshi/JsonUtf8Writer.java
package info.jdavid.ok.json;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import okio.BufferedSink;
import okio.Sink;

import static info.jdavid.ok.json.JsonScope.DANGLING_NAME;
import static info.jdavid.ok.json.JsonScope.EMPTY_ARRAY;
import static info.jdavid.ok.json.JsonScope.EMPTY_DOCUMENT;
import static info.jdavid.ok.json.JsonScope.EMPTY_OBJECT;
import static info.jdavid.ok.json.JsonScope.NONEMPTY_ARRAY;
import static info.jdavid.ok.json.JsonScope.NONEMPTY_DOCUMENT;
import static info.jdavid.ok.json.JsonScope.NONEMPTY_OBJECT;


@SuppressWarnings("UnusedReturnValue")
class JsonWriter implements Closeable, Flushable {
  // The nesting stack. Using a manual array rather than an ArrayList saves 20%. This stack permits
  // up to 32 levels of nesting including the top-level document. Deeper nesting is prone to trigger
  // StackOverflowErrors.
  private int stackSize = 0;
  private final int[] scopes = new int[32];
  private final String[] pathNames = new String[32];
  private final int[] pathIndices = new int[32];

  /**
   * A string containing a full set of spaces for a single level of indentation, or null for no
   * pretty printing.
   */
  private String indent;

  /*
   * From RFC 7159, "All Unicode characters may be placed within the
   * quotation marks except for the characters that must be escaped:
   * quotation mark, reverse solidus, and the control characters
   * (U+0000 through U+001F)."
   *
   * We also escape '\u2028' and '\u2029', which JavaScript interprets as
   * newline characters. This prevents eval() from failing with a syntax
   * error. http://code.google.com/p/google-gson/issues/detail?id=341
 */
  private static final String[] REPLACEMENT_CHARS;
  static {
    REPLACEMENT_CHARS = new String[128];
    for (int i = 0; i <= 0x1f; i++) {
      REPLACEMENT_CHARS[i] = String.format("\\u%04x", (int) i);
    }
    REPLACEMENT_CHARS['"'] = "\\\"";
    REPLACEMENT_CHARS['\\'] = "\\\\";
    REPLACEMENT_CHARS['\t'] = "\\t";
    REPLACEMENT_CHARS['\b'] = "\\b";
    REPLACEMENT_CHARS['\n'] = "\\n";
    REPLACEMENT_CHARS['\r'] = "\\r";
    REPLACEMENT_CHARS['\f'] = "\\f";
  }

  /** The output data, containing at most one top-level array or object. */
  private final BufferedSink sink;

  /** The name/value separator; either ":" or ": ". */
  private String separator = ":";

  private String deferredName;

  JsonWriter(final BufferedSink sink) {
    this.sink = sink;
    pushScope(EMPTY_DOCUMENT);
  }

  /**
   * Sets the indentation string to be repeated for each level of indentation
   * in the encoded document. If {@code indent.isEmpty()} the encoded document
   * will be compact. Otherwise the encoded document will be more
   * human-readable.
   *
   * @param indent a string containing only whitespace.
   */
  void setIndent(final String indent) {
    this.indent = !indent.isEmpty() ? indent : null;
    this.separator = !indent.isEmpty() ? ": " : ":";
  }

  /**
   * Begins encoding a new array. Each call to this method must be paired with
   * a call to {@link #endArray}.
   *
   * @return this writer.
   */
  JsonWriter beginArray() throws IOException {
    writeDeferredName();
    return open(EMPTY_ARRAY, "[");
  }

  /**
   * Ends encoding the current array.
   *
   * @return this writer.
   */
  JsonWriter endArray() throws IOException {
    return close(EMPTY_ARRAY, NONEMPTY_ARRAY, "]");
  }

  /**
   * Begins encoding a new object. Each call to this method must be paired
   * with a call to {@link #endObject}.
   *
   * @return this writer.
   */
  JsonWriter beginObject() throws IOException {
    writeDeferredName();
    return open(EMPTY_OBJECT, "{");
  }

  /**
   * Ends encoding the current object.
   *
   * @return this writer.
   */
  JsonWriter endObject() throws IOException {
    return close(EMPTY_OBJECT, NONEMPTY_OBJECT, "}");
  }

  /**
   * Enters a new scope by appending any necessary whitespace and the given
   * bracket.
   */
  private JsonWriter open(final int empty, final String openBracket) throws IOException {
    beforeValue();
    pushScope(empty);
    pathIndices[stackSize - 1] = 0;
    sink.writeUtf8(openBracket);
    return this;
  }

  /**
   * Closes the current scope by appending any necessary whitespace and the
   * given bracket.
   */
  private JsonWriter close(final int empty, final int nonempty,
                           final String closeBracket) throws IOException {
    int context = peekScope();
    if (context != nonempty && context != empty) throw new IllegalStateException("Nesting problem.");
    if (deferredName != null) throw new IllegalStateException("Dangling name: " + deferredName);

    --stackSize;
    pathNames[stackSize] = null; // Free the last path name so that it can be garbage collected!
    ++pathIndices[stackSize - 1];
    if (context == nonempty) newline();
    sink.writeUtf8(closeBracket);
    return this;
  }

  /**
   * Encodes the property name.
   *
   * @param name the name of the forthcoming value. Must not be null.
   * @return this writer.
   */
  JsonWriter name(final String name) throws IOException {
    if (stackSize == 0) throw new IllegalStateException("JsonWriter is closed.");
    if (deferredName != null) throw new IllegalStateException("Nesting problem.");
    deferredName = name;
    pathNames[stackSize - 1] = name;
    return this;
  }

  private void writeDeferredName() throws IOException {
    if (deferredName != null) {
      beforeName();
      string(sink, deferredName);
      deferredName = null;
    }
  }

  /**
   * Encodes {@code value}.
   *
   * @param value the literal string value, or null to encode a null literal.
   * @return this writer.
   */
  JsonWriter value(final String value) throws IOException {
    writeDeferredName();
    beforeValue();
    string(sink, value);
    ++pathIndices[stackSize - 1];
    return this;
  }

  /**
   * Encodes {@code null}.
   *
   * @return this writer.
   */
  JsonWriter nullValue() throws IOException {
    if (deferredName != null) {
      writeDeferredName();
    }
    beforeValue();
    sink.writeUtf8("null");
    ++pathIndices[stackSize - 1];
    return this;
  }

  /**
   * Encodes {@code value}.
   *
   * @return this writer.
   */
  private JsonWriter value(final boolean value) throws IOException {
    writeDeferredName();
    beforeValue();
    sink.writeUtf8(value ? "true" : "false");
    ++pathIndices[stackSize - 1];
    return this;
  }

  /**
   * Encodes {@code value}.
   *
   * @return this writer.
   */
  JsonWriter value(final Boolean value) throws IOException {
    return value(value.booleanValue());
  }

  /**
   * Encodes {@code value}.
   *
   * @param value a finite value. May not be {@linkplain Double#isNaN() NaNs} or
   *     {@linkplain Double#isInfinite() infinities}.
   * @return this writer.
   */
  JsonWriter value(final Number value) throws IOException {
    final String string = value.toString();
    writeDeferredName();
    beforeValue();
    sink.writeUtf8(string);
    ++pathIndices[stackSize - 1];
    return this;
  }

  /**
   * Ensures all buffered data is written to the underlying {@link Sink}
   * and flushes that writer.
   */
  @Override public void flush() throws IOException {
    if (stackSize == 0) throw new IllegalStateException("JsonWriter is closed.");
    sink.flush();
  }

  /**
   * Flushes and closes this writer and the underlying {@link Sink}.
   *
   * @throws JsonDataException if the JSON document is incomplete.
   */
  @Override public void close() throws IOException {
    sink.close();

    final int size = stackSize;
    if (size > 1 || size == 1 && scopes[size - 1] != NONEMPTY_DOCUMENT) {
      throw new IOException("Incomplete document");
    }
    stackSize = 0;
  }

  /**
   * Writes {@code value} as a string literal to {@code sink}. This wraps the value in double quotes
   * and escapes those characters that require it.
   */
  static void string(final BufferedSink sink, final String value) throws IOException {
    //noinspection UnnecessaryLocalVariable
    final String[] replacements = REPLACEMENT_CHARS;
    sink.writeByte('"');
    int last = 0;
    final int length = value.length();
    for (int i=0; i<length; ++i) {
      final char c = value.charAt(i);
      final String replacement;
      if (c < 128) {
        replacement = replacements[c];
        if (replacement == null) continue;
      }
      else if (c == '\u2028') {
        replacement = "\\u2028";
      }
      else if (c == '\u2029') {
        replacement = "\\u2029";
      }
      else {
        continue;
      }
      if (last < i) {
        sink.writeUtf8(value, last, i);
      }
      sink.writeUtf8(replacement);
      last = i + 1;
    }
    if (last < length) {
      sink.writeUtf8(value, last, length);
    }
    sink.writeByte('"');
  }

  private void newline() throws IOException {
    if (indent == null) return;
    sink.writeByte('\n');
    final int size = stackSize;
    for (int i=1; i<size; ++i) {
      sink.writeUtf8(indent);
    }
  }

  /**
   * Inserts any necessary separators and whitespace before a name. Also
   * adjusts the stack to expect the name's value.
   */
  private void beforeName() throws IOException {
    int context = peekScope();
    if (context == NONEMPTY_OBJECT) { // first in object
      sink.writeByte(',');
    }
    else if (context != EMPTY_OBJECT) { // not in an object!
      throw new IllegalStateException("Nesting problem.");
    }
    newline();
    replaceTop(DANGLING_NAME);
  }

  /**
   * Inserts any necessary separators and whitespace before a literal value,
   * inline array, or inline object. Also adjusts the stack to expect either a
   * closing bracket or another element.
   */
  @SuppressWarnings("fallthrough")
  private void beforeValue() throws IOException {
    switch (peekScope()) {
      case NONEMPTY_DOCUMENT:
      case EMPTY_DOCUMENT: // first in document
        replaceTop(NONEMPTY_DOCUMENT);
        break;
      case EMPTY_ARRAY: // first in array
        replaceTop(NONEMPTY_ARRAY);
        newline();
        break;
      case NONEMPTY_ARRAY: // another in array
        sink.writeByte(',');
        newline();
        break;
      case DANGLING_NAME: // value for name
        sink.writeUtf8(separator);
        replaceTop(NONEMPTY_OBJECT);
        break;
      default:
        throw new IllegalStateException("Nesting problem.");
    }
  }

  /** Returns the scope on the top of the stack. */
  private int peekScope() {
    if (stackSize == 0) throw new IllegalStateException("JsonWriter is closed.");
    return scopes[stackSize - 1];
  }

  private void pushScope(final int newTop) {
    if (stackSize == scopes.length) {
      throw new JsonDataException("Nesting too deep at " + getPath() + ": circular reference?");
    }
    scopes[stackSize++] = newTop;
  }

  /** Replace the value on the top of the stack with the given value. */
  private void replaceTop(final int topOfStack) {
    scopes[stackSize - 1] = topOfStack;
  }

  /**
   * Returns a <a href="http://goessner.net/articles/JsonPath/">JsonPath</a> to
   * the current location in the JSON value.
   */
  private String getPath() {
    return JsonScope.getPath(stackSize, scopes, pathNames, pathIndices);
  }

}
