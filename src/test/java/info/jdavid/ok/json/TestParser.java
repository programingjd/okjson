package info.jdavid.ok.json;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;
import okio.Buffer;
import okio.BufferedSource;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParser {

  @BeforeClass
  public static void setUp() {
    // Use the parser once to get rid of the static initializer penalty.
    // This is done so that the first test elapsed time doesn't get artificially high.
    try {
      Parser.parse("[{}]");
    }
    catch (Exception ignore) {}
  }

  @Test
  public void testParseFromSource() {
    assertNull(Parser.parse((BufferedSource)null));
    final Buffer buffer = new Buffer();
    buffer.writeUtf8("{\n  \"a\": [\n    1\n  ]\n}");
    final Map<String, ?> map = Parser.parse(buffer);
    assertNotNull(map);
    assertEquals(1, map.size());
    final List<?> list = (List<?>)map.get("a");
    assertNotNull(map);
    assertEquals(1, list.size());
    assertEquals(1, list.get(0));
  }

  @Test
  public void testNumber1() {
    final Number n = Parser.stringToNumber("9");
    assertTrue(n instanceof Integer);
    assertEquals(9, n.longValue());
  }

  @Test
  public void testNumber2() {
    final Number n = Parser.stringToNumber("-235");
    assertTrue(n instanceof Integer);
    assertEquals(-235, n.longValue());
  }

  @Test
  public void testNumber3() {
    final Number n = Parser.stringToNumber("456.1");
    assertTrue(n instanceof Double);
    assertEquals(456.1, (Double)n, 0);
  }

  @Test
  public void testNumber4() {
    final Number n = Parser.stringToNumber("-3.E-5");
    assertTrue(n instanceof Double);
    assertEquals(-3E-5, (Double)n, 0);
  }

  @Test
  public void testNumber5() {
    final Number n = Parser.stringToNumber("1234567890123");
    assertTrue(n instanceof Long);
    assertEquals(1234567890123L, (Long)n, 0);
  }

  @Test
  public void testNumber6() {
    final Number n = Parser.stringToNumber("-12345678901");
    assertTrue(n instanceof Long);
    assertEquals(-12345678901L, (Long)n, 0);
  }

  @Test
  public void testNumber7() {
    final Number n = Parser.stringToNumber("-2147483649");
    assertTrue(n instanceof Long);
    assertEquals(-2147483649L, (Long)n, 0);
  }

  @Test
  public void testNumber8() {
    final Number n = Parser.stringToNumber("2147483647");
    assertTrue(n instanceof Integer);
    assertEquals(2147483647, (Integer)n, 0);
  }

  @Test
  public void testNumber9() {
    final Number n = Parser.stringToNumber("-2000000000");
    assertTrue(n instanceof Integer);
    assertEquals(-2000000000, (Integer)n, 0);
  }

  @Test
  public void testNumber10() {
    final Number n = Parser.stringToNumber("-2800000000");
    assertTrue(n instanceof Long);
    assertEquals(-2800000000L, (Long)n, 0);
  }


  @Test
  public void testNumber11() {
    final Number n = Parser.stringToNumber("-1000000000");
    assertTrue(n instanceof Integer);
    assertEquals(-1000000000, n.longValue(), 0);
  }

  @Test
  public void testNumber12() {
    final Number n = Parser.stringToNumber("-3000000000");
    assertTrue(n instanceof Long);
    assertEquals(-3000000000L, (Long)n, 0);
  }

  @Test
  public void testNumber13() {
    final Number n = Parser.stringToNumber("3000000000");
    assertTrue(n instanceof Long);
    assertEquals(3000000000L, (Long)n, 0);
  }

  @Test
  public void testNumber14() {
    final Number n = Parser.stringToNumber("1000000000");
    assertTrue(n instanceof Integer);
    assertEquals(1000000000, n.longValue(), 0);
  }

  @Test
  public void testNumber15() {
    final Number n = Parser.stringToNumber("2000000000");
    assertTrue(n instanceof Integer);
    assertEquals(2000000000, n.longValue(), 0);
  }

  @Test
  public void testNumber16() {
    final Number n = Parser.stringToNumber("2800000000");
    assertTrue(n instanceof Long);
    assertEquals(2800000000L, (Long)n, 0);
  }

  @Test
  public void testParseEmpty() {
    final String str = "";
    final Object parsed = Parser.parse(str);
    assertNull(parsed);
  }

  @Test
  public void testParseInvalidJson() {
    final String str = "\"key\":\"value\"";
    final Object parsed = Parser.parse(str);
    assertNull(parsed);
  }

  @Test
  public void testNestedArray() {
    final String str = "[ [], [ \"a\", \"b\" ] ]";
    final Object parsed = Parser.parse(str);
    assertTrue(parsed instanceof List);
    final List array = (List)parsed;
    assertEquals(2, array.size());
    final Object first = array.get(0);
    assertTrue(first instanceof List);
    final List empty = (List)first;
    assertEquals(0, empty.size());
    final Object second = array.get(1);
    assertTrue(second instanceof List);
    final List ab = (List)second;
    assertEquals(2, ab.size());
    assertEquals("a", ab.get(0));
    assertEquals("b", ab.get(1));
  }

  @Test
  public void testArray() {
    final Object parsed = Parser.parse("[ \"a\", 3, 2.5, true, null, {}, [] ]");
    assertTrue(parsed instanceof List);
    final List array = (List)parsed;
    assertEquals(7, array.size());
    assertEquals("a", array.get(0));
    assertEquals(3, array.get(1));
    assertEquals(2.5, array.get(2));
    assertEquals(true, array.get(3));
    assertNull(array.get(4));
    assertTrue(array.get(5) instanceof Map);
    assertEquals(0, ((Map)array.get(5)).size());
    assertTrue(array.get(6) instanceof List);
    assertEquals(0, ((List)array.get(6)).size());
  }


  @SuppressWarnings("unchecked")
  @Test
  public void testParseJsonObject0() {
    final Object parsed = Parser.parse("{}");
    assertTrue(parsed instanceof Map);
    //noinspection unchecked
    final Map<String, ?> map = (Map<String, ?>)parsed;
    assertEquals(0, map.size());
    assertSame(map, (Map<String, ?>)Parser.parse("{\n}"));
    assertSame(map, (Map<String, ?>)Parser.parse("{ }"));
    assertSame(map, (Map<String, ?>)Parser.parse("{\t\n }"));
  }

  @Test
  public void testParseJsonArray0() {
    final Object parsed = Parser.parse("[]");
    assertTrue(parsed instanceof List);
    //noinspection unchecked
    final List<?> list = (List<?>)parsed;
    assertEquals(0, list.size());
    assertSame(list, (List<?>)Parser.parse("[\n]"));
    assertSame(list, (List<?>)Parser.parse("[ ]"));
    assertSame(list, (List<?>)Parser.parse("[\t\n ]"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseJsonObject1() {
    final Object parsed = Parser.parse("{\"root\":{}}");
    assertTrue(parsed instanceof Map);
    //noinspection unchecked
    final Map<String, ?> map = (Map<String, ?>)parsed;
    assertEquals(1, map.size());
    final Object rootValue = map.get("root");
    assertNotNull(rootValue);
    assertTrue(rootValue instanceof Map);
    //noinspection unchecked
    final Map<String, ?> rootMap = (Map<String, ?>)rootValue;
    assertEquals(0, rootMap.size());
    assertSame(map, (Map<String, ?>)Parser.parse("{\n  \"root\": {}\n}"));
    assertSame(map, (Map<String, ?>)Parser.parse("{\n\t\"root\": {}\n}"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseJsonArray1() {
    final Object parsed = Parser.parse("{\"root\":[]}");
    assertTrue(parsed instanceof Map);
    //noinspection unchecked
    final Map<String, ?> map = (Map<String, ?>)parsed;
    assertEquals(1, map.size());
    final Object rootValue = map.get("root");
    assertNotNull(rootValue);
    assertTrue(rootValue instanceof List);
    //noinspection unchecked
    final List<?> rootList = (List<?>)rootValue;
    assertEquals(0, rootList.size());
    assertSame(map, (Map<String, ?>)Parser.parse(Builder.build(map, true)));
    assertSame(map, (Map<String, ?>)Parser.parse(Builder.build(map, "\t")));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testParseJsonObject2() {
    final Object parsed = Parser.parse(
      "{\"a\":[1,2,3,4],\"b\":{\"b1\":\"text\",\"b2\":{}},\"c\":2.55,\"d\":true}"
    );
    assertTrue(parsed instanceof Map);
    //noinspection unchecked
    final Map<String, ?> map = (Map<String, ?>)parsed;
    assertEquals(4, map.size());
    final Object aValue = map.get("a");
    assertNotNull(aValue);
    assertTrue(aValue instanceof List);
    //noinspection unchecked
    final List<?> aList = (List<?>)aValue;
    assertEquals(4, aList.size());
    assertEquals(1, aList.get(0));
    assertEquals(2, aList.get(1));
    assertEquals(3, aList.get(2));
    assertEquals(4, aList.get(3));
    final Object bValue = map.get("b");
    assertNotNull(bValue);
    assertTrue(bValue instanceof Map);
    //noinspection unchecked
    final Map<String, ?> bMap = (Map<String, ?>)bValue;
    assertEquals(2, bMap.size());
    assertEquals("text", bMap.get("b1"));
    final Object b2Value = bMap.get("b2");
    assertNotNull(b2Value);
    assertTrue(b2Value instanceof Map);
    //noinspection unchecked
    final Map<String, ?> b2Map = (Map<String, ?>)b2Value;
    assertEquals(0, b2Map.size());
    assertEquals(2.55, map.get("c"));
    assertEquals(true, map.get("d"));
    assertSame(map, (Map<String, ?>)Parser.parse(Builder.build(map, true)));
    assertSame(map, (Map<String, ?>)Parser.parse(Builder.build(map, "\t")));
  }

  @SuppressWarnings("unchecked")
  private void assertSame(final Map<String, ?> expected, final Map<String, ?> actual) {
    if (expected.size() != actual.size()) throw new AssertionFailedError();
    final Iterator<?> entryIterator1 = expected.entrySet().iterator();
    final Iterator<?> entryIterator2 = expected.entrySet().iterator();
    while (entryIterator1.hasNext()) {
      final Map.Entry<String, ?> entry1 = (Map.Entry<String, ?>)entryIterator1.next();
      final Map.Entry<String, ?> entry2 = (Map.Entry<String, ?>)entryIterator2.next();
      assertEquals(entry1.getKey(), entry2.getKey());
      final Object value1 = entry1.getValue();
      final Object value2 = entry2.getValue();
      assertSame(value1, value2);
    }
  }

  private void assertSame(final List<?> expected, final List<?> actual) {
    assertEquals(expected.size(), actual.size());
    final Iterator<?> listIterator1 = expected.iterator();
    final Iterator<?> listIterator2 = actual.iterator();
    while (listIterator1.hasNext()) {
      assertSame(listIterator1.next(), listIterator2.next());
    }
  }

  @SuppressWarnings("unchecked")
  private void assertSame(final Object expected, final Object actual) {
    if (expected instanceof Map) {
      assertSame((Map<String, ?>)expected, (Map<String, ?>)actual);
    }
    else if (expected instanceof List) {
      assertSame((List<?>)expected, (List<?>)actual);

    }
    else {
      assertEquals(expected, actual);
    }
  }

}
