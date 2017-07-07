package info.jdavid.ok.json;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;


class JsonReader implements Closeable {

  // The nesting stack. Using a manual array rather than an ArrayList saves 20%. This stack permits
  // up to 32 levels of nesting including the top-level document. Deeper nesting is prone to trigger
  // StackOverflowErrors.
  private int stackSize = 0;
  private final int[] scopes = new int[32];
  private final String[] pathNames = new String[32];
  private final int[] pathIndices = new int[32];

  private static final long MIN_INCOMPLETE_INTEGER = Long.MIN_VALUE / 10;

  private static final ByteString SINGLE_QUOTE_OR_SLASH = ByteString.encodeUtf8("'\\");
  private static final ByteString DOUBLE_QUOTE_OR_SLASH = ByteString.encodeUtf8("\"\\");
  private static final ByteString UNQUOTED_STRING_TERMINALS
    = ByteString.encodeUtf8("{}[]:, \n\t\r\f/\\;#=");
  private static final ByteString LINEFEED_OR_CARRIAGE_RETURN = ByteString.encodeUtf8("\n\r");

  private static final int PEEKED_NONE = 0;
  private static final int PEEKED_BEGIN_OBJECT = 1;
  private static final int PEEKED_END_OBJECT = 2;
  private static final int PEEKED_BEGIN_ARRAY = 3;
  private static final int PEEKED_END_ARRAY = 4;
  private static final int PEEKED_TRUE = 5;
  private static final int PEEKED_FALSE = 6;
  private static final int PEEKED_NULL = 7;
  private static final int PEEKED_SINGLE_QUOTED = 8;
  private static final int PEEKED_DOUBLE_QUOTED = 9;
  private static final int PEEKED_UNQUOTED = 10;
  /** When this is returned, the string value is stored in peekedString. */
  private static final int PEEKED_BUFFERED = 11;
  private static final int PEEKED_SINGLE_QUOTED_NAME = 12;
  private static final int PEEKED_DOUBLE_QUOTED_NAME = 13;
  private static final int PEEKED_UNQUOTED_NAME = 14;
  private static final int PEEKED_BUFFERED_NAME = 15;
  /** When this is returned, the integer value is stored in peekedLong. */
  private static final int PEEKED_LONG = 16;
  private static final int PEEKED_NUMBER = 17;
  private static final int PEEKED_EOF = 18;

  /* State machine when parsing numbers */
  private static final int NUMBER_CHAR_NONE = 0;
  private static final int NUMBER_CHAR_SIGN = 1;
  private static final int NUMBER_CHAR_DIGIT = 2;
  private static final int NUMBER_CHAR_DECIMAL = 3;
  private static final int NUMBER_CHAR_FRACTION_DIGIT = 4;
  private static final int NUMBER_CHAR_EXP_E = 5;
  private static final int NUMBER_CHAR_EXP_SIGN = 6;
  private static final int NUMBER_CHAR_EXP_DIGIT = 7;

  /** The input JSON. */
  private final BufferedSource source;
  private final Buffer buffer;

  private int peeked = PEEKED_NONE;

  /**
   * A peeked value that was composed entirely of digits with an optional
   * leading dash. Positive values may not have a leading 0.
   */
  private long peekedLong;

  /**
   * The number of characters in a peeked number literal. Increment 'pos' by
   * this after reading a number.
   */
  private int peekedNumberLength;

  /**
   * A peeked string that should be parsed on the next double, long or string.
   * This is populated before a numeric value is parsed and used if that parsing
   * fails.
   */
  private @Nullable String peekedString;

  JsonReader(final BufferedSource source) {
    this.source = source;
    this.buffer = source.buffer();
    pushScope(JsonScope.EMPTY_DOCUMENT);
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the beginning of a new
   * array.
   */
  public void beginArray() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    if (p == PEEKED_BEGIN_ARRAY) {
      pushScope(JsonScope.EMPTY_ARRAY);
      pathIndices[stackSize - 1] = 0;
      peeked = PEEKED_NONE;
    }
    else {
      throw new JsonDataException("Expected BEGIN_ARRAY but was " + peek() + " at path " + getPath());
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the
   * end of the current array.
   */
  public void endArray() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    if (p == PEEKED_END_ARRAY) {
      stackSize--;
      ++pathIndices[stackSize - 1];
      peeked = PEEKED_NONE;
    }
    else {
      throw new JsonDataException("Expected END_ARRAY but was " + peek() + " at path " + getPath());
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the beginning of a new
   * object.
   */
  public void beginObject() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    if (p == PEEKED_BEGIN_OBJECT) {
      pushScope(JsonScope.EMPTY_OBJECT);
      peeked = PEEKED_NONE;
    }
    else {
      throw new JsonDataException("Expected BEGIN_OBJECT but was " + peek() + " at path " + getPath());
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the end of the current
   * object.
   */
  public void endObject() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    if (p == PEEKED_END_OBJECT) {
      --stackSize;
      pathNames[stackSize] = null; // Free the last path name so that it can be garbage collected!
      ++pathIndices[stackSize - 1];
      peeked = PEEKED_NONE;
    }
    else {
      throw new JsonDataException("Expected END_OBJECT but was " + peek() + " at path " + getPath());
    }
  }

  /**
   * Returns true if the current array or object has another element.
   */
  public boolean hasNext() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    return p != PEEKED_END_OBJECT && p != PEEKED_END_ARRAY;
  }

  /**
   * Returns the type of the next token without consuming it.
   */
  public Token peek() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    switch (p) {
      case PEEKED_BEGIN_OBJECT:
        return Token.BEGIN_OBJECT;
      case PEEKED_END_OBJECT:
        return Token.END_OBJECT;
      case PEEKED_BEGIN_ARRAY:
        return Token.BEGIN_ARRAY;
      case PEEKED_END_ARRAY:
        return Token.END_ARRAY;
      case PEEKED_SINGLE_QUOTED_NAME:
      case PEEKED_DOUBLE_QUOTED_NAME:
      case PEEKED_UNQUOTED_NAME:
      case PEEKED_BUFFERED_NAME:
        return Token.NAME;
      case PEEKED_TRUE:
      case PEEKED_FALSE:
        return Token.BOOLEAN;
      case PEEKED_NULL:
        return Token.NULL;
      case PEEKED_SINGLE_QUOTED:
      case PEEKED_DOUBLE_QUOTED:
      case PEEKED_UNQUOTED:
      case PEEKED_BUFFERED:
        return Token.STRING;
      case PEEKED_LONG:
      case PEEKED_NUMBER:
        return Token.NUMBER;
      case PEEKED_EOF:
        return Token.END_DOCUMENT;
      default:
        throw new AssertionError();
    }
  }

  private int doPeek() throws IOException {
    int peekStack = scopes[stackSize - 1];
    if (peekStack == JsonScope.EMPTY_ARRAY) {
      scopes[stackSize - 1] = JsonScope.NONEMPTY_ARRAY;
    }
    else if (peekStack == JsonScope.NONEMPTY_ARRAY) {
      // Look for a comma before the next element.
      final int c = nextNonWhitespace(true);
      buffer.readByte(); // consume ']' or ','.
      switch (c) {
        case ']':
          return peeked = PEEKED_END_ARRAY;
        case ';': case ',':
          break;
        default:
          throw syntaxError("Unterminated array");
      }
    }
    else if (peekStack == JsonScope.EMPTY_OBJECT || peekStack == JsonScope.NONEMPTY_OBJECT) {
      scopes[stackSize - 1] = JsonScope.DANGLING_NAME;
      // Look for a comma before the next element.
      if (peekStack == JsonScope.NONEMPTY_OBJECT) {
        final int c = nextNonWhitespace(true);
        buffer.readByte(); // Consume '}' or ','.
        switch (c) {
          case '}':
            return peeked = PEEKED_END_OBJECT;
          case ';': case ',':
            break;
          default:
            throw syntaxError("Unterminated object");
        }
      }
      final int c = nextNonWhitespace(true);
      switch (c) {
        case '"':
          buffer.readByte(); // consume the '\"'.
          return peeked = PEEKED_DOUBLE_QUOTED_NAME;
        case '\'':
          buffer.readByte(); // consume the '\''.
          return peeked = PEEKED_SINGLE_QUOTED_NAME;
        case '}':
          if (peekStack != JsonScope.NONEMPTY_OBJECT) {
            buffer.readByte(); // consume the '}'.
            return peeked = PEEKED_END_OBJECT;
          }
          throw syntaxError("Expected name");
        default:
          if (isLiteral((char)c)) return peeked = PEEKED_UNQUOTED_NAME;
          throw syntaxError("Expected name");
      }
    } else if (peekStack == JsonScope.DANGLING_NAME) {
      scopes[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
      // Look for a colon before the value.
      final int c = nextNonWhitespace(true);
      buffer.readByte(); // Consume ':'.
      switch (c) {
        case ':':
          break;
        case '=':
          if (source.request(1) && buffer.getByte(0) == '>') {
            buffer.readByte(); // Consume '>'.
          }
          break;
        default:
          throw syntaxError("Expected ':'");
      }
    }
    else if (peekStack == JsonScope.EMPTY_DOCUMENT) {
      scopes[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT;
    }
    else if (peekStack == JsonScope.NONEMPTY_DOCUMENT) {
      final int c = nextNonWhitespace(false);
      if (c == -1) return peeked = PEEKED_EOF;
    }
    else if (peekStack == JsonScope.CLOSED) {
      throw new IllegalStateException("JsonReader is closed");
    }

    final int c = nextNonWhitespace(true);
    switch (c) {
      case ']':
        if (peekStack == JsonScope.EMPTY_ARRAY) {
          buffer.readByte(); // Consume ']'.
          return peeked = PEEKED_END_ARRAY;
        }
        // fall-through to handle ",]"
      case ';': case ',':
        // In lenient mode, a 0-length literal in an array means 'null'.
        if (peekStack == JsonScope.EMPTY_ARRAY || peekStack == JsonScope.NONEMPTY_ARRAY) {
          return peeked = PEEKED_NULL;
        }
        throw syntaxError("Unexpected value");
      case '\'':
        buffer.readByte(); // Consume '\''.
        return peeked = PEEKED_SINGLE_QUOTED;
      case '"':
        buffer.readByte(); // Consume '\"'.
        return peeked = PEEKED_DOUBLE_QUOTED;
      case '[':
        buffer.readByte(); // Consume '['.
        return peeked = PEEKED_BEGIN_ARRAY;
      case '{':
        buffer.readByte(); // Consume '{'.
        return peeked = PEEKED_BEGIN_OBJECT;
      default:
    }

    int result = peekKeyword();
    if (result != PEEKED_NONE) return result;
    result = peekNumber();
    if (result != PEEKED_NONE) return result;

    if (!isLiteral(buffer.getByte(0))) throw syntaxError("Expected value");

    return peeked = PEEKED_UNQUOTED;
  }

  private int peekKeyword() throws IOException {
    // Figure out which keyword we're matching against by its first character.
    byte c = buffer.getByte(0);
    final String keyword;
    final String keywordUpper;
    int peeking;
    if (c == 't' || c == 'T') {
      keyword = "true";
      keywordUpper = "TRUE";
      peeking = PEEKED_TRUE;
    }
    else if (c == 'f' || c == 'F') {
      keyword = "false";
      keywordUpper = "FALSE";
      peeking = PEEKED_FALSE;
    }
    else if (c == 'n' || c == 'N') {
      keyword = "null";
      keywordUpper = "NULL";
      peeking = PEEKED_NULL;
    }
    else {
      return PEEKED_NONE;
    }

    // Confirm that chars [1..length) match the keyword.
    final int length = keyword.length();
    for (int i=1; i<length; ++i) {
      if (!source.request(i + 1)) return PEEKED_NONE;
      c = buffer.getByte(i);
      if (c != keyword.charAt(i) && c != keywordUpper.charAt(i)) return PEEKED_NONE;
    }

    // Don't match trues, falsey or nullsoft!
    if (source.request(length + 1) && isLiteral(buffer.getByte(length))) return PEEKED_NONE;

    // We've found the keyword followed either by EOF or by a non-literal character.
    buffer.skip(length);
    return peeked = peeking;
  }

  private int peekNumber() throws IOException {
    long value = 0L; // Negative to accommodate Long.MIN_VALUE more easily.
    boolean negative = false;
    boolean fitsInLong = true;
    int last = NUMBER_CHAR_NONE;

    int i = 0;

    charactersOfNumber:
    for (; true; ++i) {
      if (!source.request(i + 1)) break;
      final byte c = buffer.getByte(i);
      switch (c) {
        case '-':
          if (last == NUMBER_CHAR_NONE) {
            negative = true;
            last = NUMBER_CHAR_SIGN;
            continue;
          }
          else if (last == NUMBER_CHAR_EXP_E) {
            last = NUMBER_CHAR_EXP_SIGN;
            continue;
          }
          return PEEKED_NONE;
        case '+':
          if (last == NUMBER_CHAR_EXP_E) {
            last = NUMBER_CHAR_EXP_SIGN;
            continue;
          }
          return PEEKED_NONE;
        case 'e': case 'E':
          if (last == NUMBER_CHAR_DIGIT || last == NUMBER_CHAR_FRACTION_DIGIT) {
            last = NUMBER_CHAR_EXP_E;
            continue;
          }
          return PEEKED_NONE;
        case '.':
          if (last == NUMBER_CHAR_DIGIT) {
            last = NUMBER_CHAR_DECIMAL;
            continue;
          }
          return PEEKED_NONE;
        default:
          if (c < '0' || c > '9') {
            if (!isLiteral(c)) {
              break charactersOfNumber;
            }
            return PEEKED_NONE;
          }
          if (last == NUMBER_CHAR_SIGN || last == NUMBER_CHAR_NONE) {
            value = -(c - '0');
            last = NUMBER_CHAR_DIGIT;
          }
          else if (last == NUMBER_CHAR_DIGIT) {
            if (value == 0) return PEEKED_NONE; // Leading '0' prefix is not allowed, since it could be octal.
            final long newValue = value * 10 - (c - '0');
            fitsInLong &= value > MIN_INCOMPLETE_INTEGER ||
                          (value == MIN_INCOMPLETE_INTEGER && newValue < value);
            value = newValue;
          }
          else if (last == NUMBER_CHAR_DECIMAL) {
            last = NUMBER_CHAR_FRACTION_DIGIT;
          }
          else if (last == NUMBER_CHAR_EXP_E || last == NUMBER_CHAR_EXP_SIGN) {
            last = NUMBER_CHAR_EXP_DIGIT;
          }
      }
    }

    // We've read a complete number. Decide if it's a PEEKED_LONG or a PEEKED_NUMBER.
    if (last == NUMBER_CHAR_DIGIT && fitsInLong && (value != Long.MIN_VALUE || negative) &&
        (value != 0 || !negative)) {
      peekedLong = negative ? value : -value;
      buffer.skip(i);
      return peeked = PEEKED_LONG;
    }
    else if (last == NUMBER_CHAR_DIGIT || last == NUMBER_CHAR_FRACTION_DIGIT ||
             last == NUMBER_CHAR_EXP_DIGIT) {
      peekedNumberLength = i;
      return peeked = PEEKED_NUMBER;
    }
    return PEEKED_NONE;
  }

  private boolean isLiteral(final int c) throws IOException {
    switch (c) {
      case '/':
      case '\\':
      case ';':
      case '#':
      case '=':
      case '{':
      case '}':
      case '[':
      case ']':
      case ':':
      case ',':
      case ' ':
      case '\t':
      case '\f':
      case '\r':
      case '\n':
        return false;
      default:
        return true;
    }
  }

  /**
   * Returns the next token, a {@linkplain Token#NAME property name}, and consumes it.
   *
   * @throws JsonDataException if the next token in the stream is not a property name.
   */
  public String nextName() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    final String result;
    if (p == PEEKED_UNQUOTED_NAME) {
      result = nextUnquotedValue();
    }
    else if (p == PEEKED_DOUBLE_QUOTED_NAME) {
      result = nextQuotedValue(DOUBLE_QUOTE_OR_SLASH);
    }
    else if (p == PEEKED_SINGLE_QUOTED_NAME) {
      result = nextQuotedValue(SINGLE_QUOTE_OR_SLASH);
    }
    else if (p == PEEKED_BUFFERED_NAME) {
      result = peekedString;
    }
    else {
      throw new JsonDataException("Expected a name but was " + peek() + " at path " + getPath());
    }
    peeked = PEEKED_NONE;
    pathNames[stackSize - 1] = result;
    return result;
  }

  /**
   * If the next token is a {@linkplain Token#NAME property name} that's in {@code options}, this
   * consumes it and returns its index. Otherwise this returns -1 and no name is consumed.
   */
  public int selectName(final Options options) throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    if (p < PEEKED_SINGLE_QUOTED_NAME || p > PEEKED_BUFFERED_NAME) return -1;
    if (p == PEEKED_BUFFERED_NAME) return findName(peekedString, options);

    int result = source.select(options.doubleQuoteSuffix);
    if (result != -1) {
      peeked = PEEKED_NONE;
      pathNames[stackSize - 1] = options.strings[result];
      return result;
    }

    // The next name may be unnecessary escaped. Save the last recorded path name, so that we
    // can restore the peek state in case we fail to find a match.
    final String lastPathName = pathNames[stackSize - 1];

    final String nextName = nextName();
    result = findName(nextName, options);

    if (result == -1) {
      peeked = PEEKED_BUFFERED_NAME;
      peekedString = nextName;
      // We can't push the path further, make it seem like nothing happened.
      pathNames[stackSize - 1] = lastPathName;
    }

    return result;
  }

  /**
   * If {@code name} is in {@code options} this consumes it and returns it's index.
   * Otherwise this returns -1 and no name is consumed.
   */
  private int findName(final String name, final Options options) {
    final int size = options.strings.length;
    for (int i=0; i<size; ++i) {
      if (name.equals(options.strings[i])) {
        peeked = PEEKED_NONE;
        pathNames[stackSize - 1] = name;
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the {@linkplain Token#STRING string} value of the next token, consuming it. If the next
   * token is a number, this method will return its string form.
   *
   * @throws JsonDataException if the next token is not a string or if this reader is closed.
   */
  public String nextString() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    final String result;
    if (p == PEEKED_UNQUOTED) {
      result = nextUnquotedValue();
    }
    else if (p == PEEKED_DOUBLE_QUOTED) {
      result = nextQuotedValue(DOUBLE_QUOTE_OR_SLASH);
    }
    else if (p == PEEKED_SINGLE_QUOTED) {
      result = nextQuotedValue(SINGLE_QUOTE_OR_SLASH);
    }
    else if (p == PEEKED_BUFFERED) {
      result = peekedString;
      peekedString = null;
    }
    else if (p == PEEKED_LONG) {
      result = Long.toString(peekedLong);
    }
    else if (p == PEEKED_NUMBER) {
      result = buffer.readUtf8(peekedNumberLength);
    }
    else {
      throw new JsonDataException("Expected a string but was " + peek() + " at path " + getPath());
    }
    peeked = PEEKED_NONE;
    ++pathIndices[stackSize - 1];
    return result;
  }

  /**
   * If the next token is a {@linkplain Token#STRING string} that's in {@code options}, this
   * consumes it and returns its index. Otherwise this returns -1 and no string is consumed.
   */
  public int selectString(final Options options) throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    if (p < PEEKED_SINGLE_QUOTED || p > PEEKED_BUFFERED) return -1;
    if (p == PEEKED_BUFFERED) return findString(peekedString, options);

    int result = source.select(options.doubleQuoteSuffix);
    if (result != -1) {
      peeked = PEEKED_NONE;
      ++pathIndices[stackSize - 1];
      return result;
    }

    final String nextString = nextString();
    result = findString(nextString, options);

    if (result == -1) {
      peeked = PEEKED_BUFFERED;
      peekedString = nextString;
      --pathIndices[stackSize - 1];
    }

    return result;
  }

  /**
   * If {@code string} is in {@code options} this consumes it and returns it's index.
   * Otherwise this returns -1 and no string is consumed.
   */
  private int findString(final String string, final Options options) {
    final int size = options.strings.length;
    for (int i=0; i<size; ++i) {
      if (string.equals(options.strings[i])) {
        peeked = PEEKED_NONE;
        ++pathIndices[stackSize - 1];
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the {@linkplain Token#BOOLEAN boolean} value of the next token, consuming it.
   *
   * @throws JsonDataException if the next token is not a boolean or if this reader is closed.
   */
  public boolean nextBoolean() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    if (p == PEEKED_TRUE) {
      peeked = PEEKED_NONE;
      ++pathIndices[stackSize - 1];
      return true;
    }
    else if (p == PEEKED_FALSE) {
      peeked = PEEKED_NONE;
      ++pathIndices[stackSize - 1];
      return false;
    }
    throw new JsonDataException("Expected a boolean but was " + peek() + " at path " + getPath());
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is a literal null. Returns
   * null.
   *
   * @throws JsonDataException if the next token is not null or if this reader is closed.
   */
  public @Nullable <T> T nextNull() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();
    if (p == PEEKED_NULL) {
      peeked = PEEKED_NONE;
      ++pathIndices[stackSize - 1];
      return null;
    }
    else {
      throw new JsonDataException("Expected null but was " + peek() + " at path " + getPath());
    }
  }

  /**
   * Returns the {@linkplain Token#NUMBER double} value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as a double using {@link
   * Double#parseDouble(String)}.
   *
   * @throws JsonDataException if the next token is not a literal value, or if the next literal
   *     value cannot be parsed as a double, or is non-finite.
   */
  public double nextDouble() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();

    if (p == PEEKED_LONG) {
      peeked = PEEKED_NONE;
      ++pathIndices[stackSize - 1];
      return (double)peekedLong;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = buffer.readUtf8(peekedNumberLength);
    }
    else if (p == PEEKED_DOUBLE_QUOTED) {
      peekedString = nextQuotedValue(DOUBLE_QUOTE_OR_SLASH);
    }
    else if (p == PEEKED_SINGLE_QUOTED) {
      peekedString = nextQuotedValue(SINGLE_QUOTE_OR_SLASH);
    }
    else if (p == PEEKED_UNQUOTED) {
      peekedString = nextUnquotedValue();
    }
    else if (p != PEEKED_BUFFERED) {
      throw new JsonDataException("Expected a double but was " + peek() + " at path " + getPath());
    }

    peeked = PEEKED_BUFFERED;
    final double result;
    try {
      result = Double.parseDouble(peekedString);
    }
    catch (final NumberFormatException e) {
      throw new JsonDataException("Expected a double but was " + peekedString
                                  + " at path " + getPath());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    ++pathIndices[stackSize - 1];
    return result;
  }

  /**
   * Returns the {@linkplain Token#NUMBER long} value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as a long. If the next token's numeric
   * value cannot be exactly represented by a Java {@code long}, this method throws.
   *
   * @throws JsonDataException if the next token is not a literal value, if the next literal value
   *     cannot be parsed as a number, or exactly represented as a long.
   */
  public long nextLong() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();

    if (p == PEEKED_LONG) {
      peeked = PEEKED_NONE;
      ++pathIndices[stackSize - 1];
      return peekedLong;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = buffer.readUtf8(peekedNumberLength);
    }
    else if (p == PEEKED_DOUBLE_QUOTED || p == PEEKED_SINGLE_QUOTED) {
      peekedString = p == PEEKED_DOUBLE_QUOTED ?
                     nextQuotedValue(DOUBLE_QUOTE_OR_SLASH) :
                     nextQuotedValue(SINGLE_QUOTE_OR_SLASH);
      try {
        final long result = Long.parseLong(peekedString);
        peeked = PEEKED_NONE;
        ++pathIndices[stackSize - 1];
        return result;
      }
      catch (final NumberFormatException ignored) {
        // Fall back to parse as a double below.
      }
    }
    else if (p != PEEKED_BUFFERED) {
      throw new JsonDataException("Expected a long but was " + peek() + " at path " + getPath());
    }

    peeked = PEEKED_BUFFERED;
    final long result;
    try {
      final BigDecimal asDecimal = new BigDecimal(peekedString);
      result = asDecimal.longValueExact();
    }
    catch (final NumberFormatException e) {
      throw new JsonDataException("Expected a long but was " + peekedString + " at path " + getPath());
    }
    catch (final ArithmeticException e) {
      throw new JsonDataException("Expected a long but was " + peekedString + " at path " + getPath());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    ++pathIndices[stackSize - 1];
    return result;
  }

  /**
   * Returns the string up to but not including {@code quote}, unescaping any character escape
   * sequences encountered along the way. The opening quote should have already been read. This
   * consumes the closing quote, but does not include it in the returned string.
   *
   * @throws IOException if any unicode escape sequences are malformed.
   */
  private String nextQuotedValue(final ByteString runTerminator) throws IOException {
    StringBuilder builder = null;
    while (true) {
      final long index = source.indexOfElement(runTerminator);
      if (index == -1L) throw syntaxError("Unterminated string");

      // If we've got an escape character, we're going to need a string builder.
      if (buffer.getByte(index) == '\\') {
        if (builder == null) builder = new StringBuilder();
        builder.append(buffer.readUtf8(index));
        buffer.readByte(); // '\'
        builder.append(readEscapeCharacter());
        continue;
      }

      // If it isn't the escape character, it's the quote. Return the string.
      if (builder == null) {
        final String result = buffer.readUtf8(index);
        buffer.readByte(); // Consume the quote character.
        return result;
      }
      else {
        builder.append(buffer.readUtf8(index));
        buffer.readByte(); // Consume the quote character.
        return builder.toString();
      }
    }
  }

  /** Returns an unquoted value as a string. */
  private String nextUnquotedValue() throws IOException {
    final long i = source.indexOfElement(UNQUOTED_STRING_TERMINALS);
    return i != -1 ? buffer.readUtf8(i) : buffer.readUtf8();
  }

  private void skipQuotedValue(final ByteString runTerminator) throws IOException {
    while (true) {
      final long index = source.indexOfElement(runTerminator);
      if (index == -1L) throw syntaxError("Unterminated string");

      if (buffer.getByte(index) == '\\') {
        buffer.skip(index + 1);
        readEscapeCharacter();
      }
      else {
        buffer.skip(index + 1);
        return;
      }
    }
  }

  private void skipUnquotedValue() throws IOException {
    final long i = source.indexOfElement(UNQUOTED_STRING_TERMINALS);
    buffer.skip(i != -1L ? i : buffer.size());
  }

  /**
   * Returns the {@linkplain Token#NUMBER int} value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as an int. If the next token's numeric
   * value cannot be exactly represented by a Java {@code int}, this method throws.
   *
   * @throws JsonDataException if the next token is not a literal value, if the next literal value
   *     cannot be parsed as a number, or exactly represented as an int.
   */
  public int nextInt() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) p = doPeek();

    int result;
    if (p == PEEKED_LONG) {
      result = (int) peekedLong;
      if (peekedLong != result) { // Make sure no precision was lost casting to 'int'.
        throw new JsonDataException("Expected an int but was " + peekedLong + " at path " + getPath());
      }
      peeked = PEEKED_NONE;
      ++pathIndices[stackSize - 1];
      return result;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = buffer.readUtf8(peekedNumberLength);
    }
    else if (p == PEEKED_DOUBLE_QUOTED || p == PEEKED_SINGLE_QUOTED) {
      peekedString = p == PEEKED_DOUBLE_QUOTED ?
                     nextQuotedValue(DOUBLE_QUOTE_OR_SLASH) :
                     nextQuotedValue(SINGLE_QUOTE_OR_SLASH);
      try {
        result = Integer.parseInt(peekedString);
        peeked = PEEKED_NONE;
        ++pathIndices[stackSize - 1];
        return result;
      }
      catch (final NumberFormatException ignored) {
        // Fall back to parse as a double below.
      }
    }
    else if (p != PEEKED_BUFFERED) {
      throw new JsonDataException("Expected an int but was " + peek() + " at path " + getPath());
    }

    peeked = PEEKED_BUFFERED;
    double asDouble;
    try {
      asDouble = Double.parseDouble(peekedString);
    }
    catch (final NumberFormatException e) {
      throw new JsonDataException("Expected an int but was " + peekedString + " at path " + getPath());
    }
    result = (int)asDouble;
    if (result != asDouble) { // Make sure no precision was lost casting to 'int'.
      throw new JsonDataException("Expected an int but was " + peekedString + " at path " + getPath());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    ++pathIndices[stackSize - 1];
    return result;
  }

  @Override public void close() throws IOException {
    peeked = PEEKED_NONE;
    scopes[0] = JsonScope.CLOSED;
    stackSize = 1;
    buffer.clear();
    source.close();
  }

  /**
   * Skips the next value recursively. If it is an object or array, all nested elements are skipped.
   * This method is intended for use when the JSON token stream contains unrecognized or unhandled
   * values.
   *
   * <p>This throws a {@link JsonDataException}.
   */
  public void skipValue() throws IOException {
    throw new JsonDataException("Cannot skip unexpected " + peek() + " at " + getPath());
  }

  /**
   * Returns the next character in the stream that is neither whitespace nor a
   * part of a comment. When this returns, the returned character is always at
   * {@code buffer[pos-1]}; this means the caller can always push back the
   * returned character by decrementing {@code pos}.
   */
  private int nextNonWhitespace(boolean throwOnEof) throws IOException {
    /*
     * This code uses ugly local variables 'p' and 'l' representing the 'pos'
     * and 'limit' fields respectively. Using locals rather than fields saves
     * a few field reads for each whitespace character in a pretty-printed
     * document, resulting in a 5% speedup. We need to flush 'p' to its field
     * before any (potentially indirect) call to fillBuffer() and reread both
     * 'p' and 'l' after any (potentially indirect) call to the same method.
     */
    int p = 0;
    while (source.request(p + 1)) {
      int c = buffer.getByte(p++);
      if (c == '\n' || c == ' ' || c == '\r' || c == '\t') continue;

      buffer.skip(p - 1);
      if (c == '/') {
        if (!source.request(2)) return c;

        final byte peek = buffer.getByte(1);
        switch (peek) {
          case '*':
            // skip a /* c-style comment */
            buffer.readByte(); // '/'
            buffer.readByte(); // '*'
            if (!skipTo("*/")) throw syntaxError("Unterminated comment");
            buffer.readByte(); // '*'
            buffer.readByte(); // '/'
            p = 0;
            continue;
          case '/':
            // skip a // end-of-line comment
            buffer.readByte(); // '/'
            buffer.readByte(); // '/'
            skipToEndOfLine();
            p = 0;
            continue;
          default:
            return c;
        }
      }
      else if (c == '#') {
        // Skip a # hash end-of-line comment. The JSON RFC doesn't specify this behaviour, but it's
        // required to parse existing documents.
        skipToEndOfLine();
        p = 0;
      }
      else {
        return c;
      }
    }
    if (throwOnEof) throw new EOFException("End of input");
    return -1;
  }

  /**
   * Advances the position until after the next newline character. If the line
   * is terminated by "\r\n", the '\n' must be consumed as whitespace by the
   * caller.
   */
  private void skipToEndOfLine() throws IOException {
    final long index = source.indexOfElement(LINEFEED_OR_CARRIAGE_RETURN);
    buffer.skip(index != -1 ? index + 1 : buffer.size());
  }

  /**
   * @param toFind a string to search for. Must not contain a newline.
   */
  private boolean skipTo(String toFind) throws IOException {
    outer:
    for (; source.request(toFind.length());) {
      for (int c=0; c<toFind.length(); ++c) {
        if (buffer.getByte(c) != toFind.charAt(c)) {
          buffer.readByte();
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  @Override public String toString() {
    return "JsonReader(" + source + ")";
  }

  /**
   * Unescapes the character identified by the character or characters that immediately follow a
   * backslash. The backslash '\' should have already been read. This supports both unicode escapes
   * "u000A" and two-character escapes "\n".
   *
   * @throws IOException if any unicode escape sequences are malformed.
   */
  private char readEscapeCharacter() throws IOException {
    if (!source.request(1)) throw syntaxError("Unterminated escape sequence");

    final byte escaped = buffer.readByte();
    switch (escaped) {
      case 'u':
        if (!source.request(4)) {
          throw new EOFException("Unterminated escape sequence at path " + getPath());
        }
        // Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4), 16);
        char result = 0;
        for (int i=0, end=i+4; i<end; ++i) {
          final byte c = buffer.getByte(i);
          result <<= 4;
          if (c >= '0' && c <= '9') {
            result += (c - '0');
          }
          else if (c >= 'a' && c <= 'f') {
            result += (c - 'a' + 10);
          }
          else if (c >= 'A' && c <= 'F') {
            result += (c - 'A' + 10);
          }
          else {
            throw syntaxError("\\u" + buffer.readUtf8(4));
          }
        }
        buffer.skip(4);
        return result;
      case 't':
        return '\t';
      case 'b':
        return '\b';
      case 'n':
        return '\n';
      case 'r':
        return '\r';
      case 'f':
        return '\f';
      case '\n':
      case '\'':
      case '"':
      case '\\':
      case '/':
        return (char)escaped;
      default:
        return (char)escaped;
    }
  }

  /**
   * Changes the reader to treat the next name as a string value. This is useful for map adapters so
   * that arbitrary type adapters can use {@link #nextString} to read a name value.
   */
  void promoteNameToValue() throws IOException {
    if (hasNext()) {
      peekedString = nextName();
      peeked = PEEKED_BUFFERED;
    }
  }

  final void pushScope(final int newTop) {
    if (stackSize == scopes.length) {
      throw new JsonDataException("Nesting too deep at " + getPath());
    }
    scopes[stackSize++] = newTop;
  }

  /**
   * Throws a new IO exception with the given message and a context snippet
   * with this reader's content.
   */
  final JsonEncodingException syntaxError(final String message) throws JsonEncodingException {
    throw new JsonEncodingException(message + " at path " + getPath());
  }

  final JsonDataException typeMismatch(@Nullable final Object value, final Object expected) {
    if (value == null) {
      return new JsonDataException("Expected " + expected + " but was null at path " + getPath());
    }
    else {
      return new JsonDataException("Expected " + expected + " but was " + value + ", a " +
                                   value.getClass().getName() + ", at path " + getPath());
    }
  }


  /**
   * Returns the value of the next token, consuming it. The result may be a string, number, boolean,
   * null, map, or list, according to the JSON structure.
   *
   * @throws JsonDataException if the next token is not a literal value, if a JSON object has a
   * duplicate key.
   */
  public final @Nullable Object readJsonValue() throws IOException {
    switch (peek()) {
      case BEGIN_ARRAY:
        List<Object> list = new ArrayList<Object>();
        beginArray();
        while (hasNext()) {
          list.add(readJsonValue());
        }
        endArray();
        return list;
      case BEGIN_OBJECT:
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        beginObject();
        while (hasNext()) {
          String name = nextName();
          Object value = readJsonValue();
          Object replaced = map.put(name, value);
          if (replaced != null) {
            throw new JsonDataException("Map key '" + name + "' has multiple values at path "
                                        + getPath() + ": " + replaced + " and " + value);
          }
        }
        endObject();
        return map;
      case STRING:
        return nextString();
      case NUMBER:
        return nextDouble();
      case BOOLEAN:
        return nextBoolean();
      case NULL:
        return nextNull();
      default:
        throw new IllegalStateException(
          "Expected a value but was " + peek() + " at path " + getPath());
    }
  }

  /**
   * Returns a <a href="http://goessner.net/articles/JsonPath/">JsonPath</a> to
   * the current location in the JSON value.
   */
  public final String getPath() {
    return JsonScope.getPath(stackSize, scopes, pathNames, pathIndices);
  }


  /**
   * A set of strings to be chosen with {@link #selectName} or {@link #selectString}. This prepares
   * the encoded values of the strings so they can be read directly from the input source.
   */
  public static final class Options {
    final String[] strings;
    final okio.Options doubleQuoteSuffix;

    private Options(String[] strings, okio.Options doubleQuoteSuffix) {
      this.strings = strings;
      this.doubleQuoteSuffix = doubleQuoteSuffix;
    }

    public static Options of(String... strings) {
      try {
        ByteString[] result = new ByteString[strings.length];
        Buffer buffer = new Buffer();
        for (int i = 0; i < strings.length; i++) {
          JsonWriter.string(buffer, strings[i]);
          buffer.readByte(); // Skip the leading double quote (but leave the trailing one).
          result[i] = buffer.readByteString();
        }
        return new Options(strings.clone(), okio.Options.of(result));
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * A structure, name, or value type in a JSON-encoded string.
   */
  public enum Token {

    /**
     * The opening of a JSON array. Written using {@link JsonWriter#beginArray}
     * and read using {@link JsonReader#beginArray}.
     */
    BEGIN_ARRAY,

    /**
     * The closing of a JSON array. Written using {@link JsonWriter#endArray}
     * and read using {@link JsonReader#endArray}.
     */
    END_ARRAY,

    /**
     * The opening of a JSON object. Written using {@link JsonWriter#beginObject}
     * and read using {@link JsonReader#beginObject}.
     */
    BEGIN_OBJECT,

    /**
     * The closing of a JSON object. Written using {@link JsonWriter#endObject}
     * and read using {@link JsonReader#endObject}.
     */
    END_OBJECT,

    /**
     * A JSON property name. Within objects, tokens alternate between names and
     * their values. Written using {@link JsonWriter#name} and read using {@link
     * JsonReader#nextName}
     */
    NAME,

    /**
     * A JSON string.
     */
    STRING,

    /**
     * A JSON number represented in this API by a Java {@code double}, {@code
     * long}, or {@code int}.
     */
    NUMBER,

    /**
     * A JSON {@code true} or {@code false}.
     */
    BOOLEAN,

    /**
     * A JSON {@code null}.
     */
    NULL,

    /**
     * The end of the JSON stream. This sentinel value is returned by {@link
     * JsonReader#peek()} to signal that the JSON-encoded value has no more
     * tokens.
     */
    END_DOCUMENT
  }

}
