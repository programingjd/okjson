package info.jdavid.ok.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBuilder {

  @BeforeClass
  public static void setUp() {
    // Use the parser once to get rid of the static initializer penalty.
    // This is done so that the first test elapsed time doesn't get artificially high.
    try {
      final List list = new ArrayList();
      //noinspection unchecked
      list.add(new HashMap());
      Builder.build(list);
    }
    catch (Exception ignore) {}
  }

  private static List<?> list(Object... items) {
    final List<Object> list = new ArrayList<Object>();
    Collections.addAll(list, items);
    return list;
  }

  private static final class KeyValue {
    public final String key;
    public final Object value;
    public KeyValue(final String key, final Object value) {
      this.key = key;
      this.value = value;
    }
  }

  private static KeyValue kv(final String key, final Object value) {
    return new KeyValue(key, value);
  }

  private static Map<String, ?> map(KeyValue... items) {
    final Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (final KeyValue item: items) {
      map.put(item.key, item.value);
    }
    return map;
  }

  @Test
  public void testBuildNullMap() {
    //noinspection unchecked
    final String built = Builder.build((Map)null);
    assertNull(built);
  }

  @Test
  public void testBuildNullList() {
    //noinspection unchecked
    final String built = Builder.build((List)null);
    assertNull(built);
  }

  @Test
  public void testBuildEmptyMap() {
    final Map<String, ?> map = Collections.<String, Object>emptyMap();
    assertTrue(Builder.isValidObject(map));
    final String built = Builder.build(map);
    assertEquals("{}", built);
  }

  @Test
  public void testBuildEmptyList() {
    final List<?> list = Collections.emptyList();
    assertTrue(Builder.isValidArray(list));
    final String built = Builder.build(list);
    assertEquals("[]", built);
  }

  @Test
  public void testBuildUnknownObjectKey() {
    final Map map = new HashMap();
    //noinspection unchecked
    map.put(3, "test");
    //noinspection unchecked
    assertFalse(Builder.isValidObject(map));
    try {
      //noinspection unchecked
      fail(Builder.build(map));
    }
    catch (final ClassCastException ignore) {}
  }

  @Test
  public void testBuildUnknownValue1() {
    assertFalse(Builder.isValidObject(null));
    final Map<String, ?> map = map(kv("k1", new Date()));
    assertFalse(Builder.isValidObject(map));
    assertEquals("{}", Builder.build(map));
  }

  @Test
  public void testBuildUnknownValue2() {
    assertFalse(Builder.isValidArray(null));
    final List<?> list = list(new Date());
    assertFalse(Builder.isValidArray(list));
    assertEquals("[]", Builder.build(list));
  }

  @Test
  public void testBuildNumbers1() {
    final Map<String, ?> map = map(kv("k1", 1), kv("k2", 2.3f), kv("k3", -2L), kv("k4", 1E-3));
    assertTrue(Builder.isValidObject(map));
    assertEquals("{\"k1\":1,\"k2\":2.3,\"k3\":-2,\"k4\":0.001}", Builder.build(map));
  }

  @Test
  public void testBuildNumbers2() {
    final List<?> list = list(.2, 3.1f, 0, 4L, 1E-2);
    assertTrue(Builder.isValidArray(list));
    assertEquals("[0.2,3.1,0,4,0.01]", Builder.build(list));
  }

  @Test
  public void testBuildStrings1() {
    final Map<String, ?> map = map(kv("key_a","value \"a\""), kv("a\tb", "\n"));
    assertTrue(Builder.isValidObject(map));
    assertEquals("{\"key_a\":\"value \\\"a\\\"\",\"a\\tb\":\"\\n\"}", Builder.build(map));
  }

  @Test
  public void testBuildStrings2() {
    final List<?> list = list("a", "abc", "\n", "\"quotes\"");
    assertTrue(Builder.isValidArray(list));
    assertEquals("[\"a\",\"abc\",\"\\n\",\"\\\"quotes\\\"\"]", Builder.build(list));
  }

  @Test
  public void testBuildBooleans() {
    final Map<String, ?> map = map(kv("a", true), kv("b", list(true, false, true)));
    assertTrue(Builder.isValidObject(map));
    assertTrue(Builder.isValidArray((List)map.get("b")));
    assertEquals("{\"a\":true,\"b\":[true,false,true]}", Builder.build(map));
  }

  @Test
  public void testMixed1() {
    final Map<String, ?> map = map(kv("a", "v"), kv("b", list("a", 2, true)),
                                   kv("c", map(kv("c1", true), kv("c2", list("val")))),
                                   kv("d", list(list(1,2,3), map())));
    assertTrue(Builder.isValidObject(map));
    assertEquals("{\"a\":\"v\",\"b\":[\"a\",2,true],\"c\":{\"c1\":true,\"c2\":[\"val\"]},\"d\":[[1,2,3],{}]}",
                 Builder.build(map));
  }

  @Test
  public void testMixed2() {
    final List<?> list = list("a","b",map(kv("k",3.5),kv("e",map())),list(map(),true),5,false);
    assertTrue(Builder.isValidArray(list));
    assertEquals("[\"a\",\"b\",{\"k\":3.5,\"e\":{}},[{},true],5,false]",
                 Builder.build(list));
  }

}
