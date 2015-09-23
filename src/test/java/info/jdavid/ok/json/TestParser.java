package info.jdavid.ok.json;

import java.util.List;
import java.util.Map;

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
  public void testParseJsonObject0() {
    final String str = "{}";
    final Object parsed = Parser.parse(str);
    assertTrue(parsed instanceof Map);
    //noinspection unchecked
    final Map<String, ?> map = (Map<String, ?>)parsed;
    assertEquals(0, map.size());
  }

  @Test
  public void testParseJsonArray0() {
    final String str = "[]";
    final Object parsed = Parser.parse(str);
    assertTrue(parsed instanceof List);
    //noinspection unchecked
    final List<?> list = (List<?>)parsed;
    assertEquals(0, list.size());
  }

  @Test
  public void testParseJsonObject1() {
    final String str = "{\"root\":{}}";
    final Object parsed = Parser.parse(str);
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
  }

  @Test
  public void testParseJsonArray1() {
    final String str = "{\"root\":[]}";
    final Object parsed = Parser.parse(str);
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
  }

  @Test
  public void testParseJsonObject2() {
    final String str = "{\"a\":[1,2,3,4],\"b\":{\"b1\":\"text\",\"b2\":{}},\"c\":2.55,\"d\":true}";
    final Object parsed = Parser.parse(str);
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
  }

}
