package info.jdavid.ok.json;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;
import okio.Timeout;
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
    final String key;
    final Object value;
    KeyValue(final String key, final Object value) {
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
  public void testWrongUnsafeCast() {
    final Map<Object, Object> map = new HashMap<Object, Object>();
    map.put(Boolean.TRUE, "test");
    //noinspection unchecked
    assertFalse(Builder.isValidObject((Map)map));
  }

  @Test
  public void testInvalidIndentationString() {
    try {
      Builder.build(Collections.<String, Object>emptyMap(), "++");
      fail();
    }
    catch (final IllegalArgumentException ignore) {}
    try {
      Builder.build(Collections.emptyList(), "--");
      fail();
    }
    catch (final IllegalArgumentException ignore) {}
    try {
      Builder.build(Collections.emptyList().iterator(), "a");
      fail();
    }
    catch (final IllegalArgumentException ignore) {}
    try {
      Builder.build(Collections.enumeration(Collections.emptyList()), "___");
      fail();
    }
    catch (final IllegalArgumentException ignore) {}
  }

  @Test
  public void testThrowingSink() {
    final ThrowingSink sink1 = new ThrowingSink(true, false);
    Builder.build(sink1.sink(), Collections.<String, Object>emptyMap());
    assertEquals(0, sink1.buffer.size());
    Builder.build(sink1.sink(), Collections.<String, Object>emptyMap(), false);
    assertEquals(0, sink1.buffer.size());
    Builder.build(sink1.sink(), Collections.<String, Object>emptyMap(), "  ");
    assertEquals(0, sink1.buffer.size());
    Builder.build(sink1.sink(), Collections.emptyList());
    assertEquals(0, sink1.buffer.size());
    Builder.build(sink1.sink(), Collections.emptyList(), false);
    assertEquals(0, sink1.buffer.size());
    Builder.build(sink1.sink(), Collections.emptyList(), "  ");
    assertEquals(0, sink1.buffer.size());
    Builder.build(sink1.sink(), Collections.emptyList().iterator());
    assertEquals(0, sink1.buffer.size());
    Builder.build(sink1.sink(), Collections.emptyList().iterator(), false);
    assertEquals(0, sink1.buffer.size());
    Builder.build(sink1.sink(), Collections.emptyList().iterator(), "  ");
    assertEquals(0, sink1.buffer.size());
    final ThrowingSink sink2 = new ThrowingSink(true, true);
    Builder.build(sink2.sink(), Collections.<String, Object>emptyMap());
    assertEquals(0, sink2.buffer.size());
    Builder.build(sink2.sink(), Collections.<String, Object>emptyMap(), true);
    assertEquals(0, sink2.buffer.size());
    Builder.build(sink2.sink(), Collections.<String, Object>emptyMap(), "\t");
    assertEquals(0, sink2.buffer.size());
    Builder.build(sink2.sink(), Collections.emptyList());
    assertEquals(0, sink2.buffer.size());
    Builder.build(sink2.sink(), Collections.emptyList(), true);
    assertEquals(0, sink2.buffer.size());
    Builder.build(sink2.sink(), Collections.emptyList(), "\t");
    assertEquals(0, sink2.buffer.size());
    Builder.build(sink2.sink(), Collections.emptyList().iterator());
    assertEquals(0, sink2.buffer.size());
    Builder.build(sink2.sink(), Collections.emptyList().iterator(), true);
    assertEquals(0, sink2.buffer.size());
    Builder.build(sink2.sink(), Collections.emptyList().iterator(), "\t");
    assertEquals(0, sink2.buffer.size());
    final ThrowingSink sink3 = new ThrowingSink(false, true);
    Builder.build(sink3.sink(), Collections.<String, Object>emptyMap());
    assertEquals("{}", sink3.buffer.readUtf8());
    Builder.build(sink3.sink(), Collections.emptyList());
    assertEquals("[]", sink3.buffer.readUtf8());
  }

  @Test
  public void testBuildNullMap() {
    //noinspection unchecked
    final String built = Builder.build((Map<String, ?>)null);
    assertNull(built);
    assertNull(Builder.build((Map<String, ?>)null, false));
    assertNull(Builder.build((Map<String, ?>)null, true));
    assertNull(Builder.build((Map<String, ?>)null, "  "));
  }

  @Test
  public void testBuildNullList() {
    final String built = Builder.build((Iterable<?>)null);
    assertNull(built);
    assertNull(Builder.build((Iterable<?>)null, false));
    assertNull(Builder.build((Iterable<?>)null, true));
    assertNull(Builder.build((Iterable<?>)null, "  "));
  }

  @Test
  public void testBuildMapToSink() {
    Builder.build(null, Collections.<String, Object>emptyMap());
    final Buffer buffer = new Buffer();
    Builder.build(buffer, (Map<String, ?>)null);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Map<String, ?>)null, false);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Map<String, ?>)null, true);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Map<String, ?>)null, "  ");
    assertEquals(0, buffer.size());
    Builder.build(buffer, Collections.<String, Object>emptyMap());
    assertEquals("{}", buffer.readUtf8());
  }

  @Test
  public void testBuildIterableToSink() {
    Builder.build(null, Collections.<String, Object>emptyMap());
    Builder.build(null, Collections.emptyList());
    Builder.build(null, Collections.emptyList().iterator());
    Builder.build(null, Collections.enumeration(Collections.emptyList()));
    Builder.build(null, Collections.<String, Object>emptyMap(), false);
    Builder.build(null, Collections.emptyList(), false);
    Builder.build(null, Collections.emptyList().iterator(), false);
    Builder.build(null, Collections.enumeration(Collections.emptyList()), false);
    Builder.build(null, Collections.<String, Object>emptyMap(), true);
    Builder.build(null, Collections.emptyList(), "  ");
    Builder.build(null, Collections.emptyList().iterator(), "  ");
    Builder.build(null, Collections.enumeration(Collections.emptyList()), "  ");
    final Buffer buffer = new Buffer();
    Builder.build(buffer, (Iterable<?>)null);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Iterable<?>)null, false);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Iterable<?>)null, true);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Iterable<?>)null, "  ");
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Iterator<?>)null, false);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Iterator<?>)null, true);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Iterator<?>)null, "  ");
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Enumeration<?>)null, false);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Enumeration<?>)null, true);
    assertEquals(0, buffer.size());
    Builder.build(buffer, (Enumeration<?>)null, "  ");
    assertEquals(0, buffer.size());
    Builder.build(buffer, Collections.emptyList());
    assertEquals("[]", buffer.readUtf8());
    Builder.build(buffer, Collections.emptyList().iterator());
    assertEquals("[]", buffer.readUtf8());
    Builder.build(buffer, Collections.enumeration(Collections.emptyList()));
    assertEquals("[]", buffer.readUtf8());
    Builder.build(buffer, Collections.emptyList().iterator(), false);
    assertEquals("[]", buffer.readUtf8());
    Builder.build(buffer, Collections.enumeration(Collections.emptyList()), false);
    assertEquals("[]", buffer.readUtf8());
    Builder.build(buffer, Collections.emptyList().iterator(), true);
    assertEquals("[]", buffer.readUtf8());
    Builder.build(buffer, Collections.enumeration(Collections.emptyList()), true);
    assertEquals("[]", buffer.readUtf8());
    Builder.build(buffer, Collections.emptyList().iterator(), "  ");
    assertEquals("[]", buffer.readUtf8());
    Builder.build(buffer, Collections.enumeration(Collections.emptyList()), "  ");
    assertEquals("[]", buffer.readUtf8());
  }

  @Test
  public void testBuildEmptyMap() {
    final Map<String, ?> map = Collections.<String, Object>emptyMap();
    assertTrue(Builder.isValidObject(map));
    assertEquals("{}", Builder.build(map));
    assertEquals("{}", Builder.build(map, true));
    assertEquals("{}", Builder.build(map, "\t"));
  }

  @Test
  public void testBuildEmptyList() {
    final List<?> list = Collections.emptyList();
    assertTrue(Builder.isValidArray(list));
    assertEquals("[]", Builder.build(list));
    assertEquals("[]", Builder.build(list, true));
    assertEquals("[]", Builder.build(list, "\t"));
  }

  @Test
  public void testBuildEmptySet() {
    final Set<?> set = Collections.emptySet();
    assertTrue(Builder.isValidArray(set));
    assertEquals("[]", Builder.build(set));
    assertEquals("[]", Builder.build(set, true));
    assertEquals("[]", Builder.build(set, "\t"));
  }

  @Test
  public void testBuildEmptyIterator() {
    final List<?> list = Collections.emptyList();
    assertTrue(Builder.isValidArray(list.iterator()));
    assertEquals("[]", Builder.build(list.iterator()));
    assertEquals("[]", Builder.build(list.iterator(), true));
    assertEquals("[]", Builder.build(list.iterator(), "\t"));
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @Test
  public void testBuildEmptyEnumeration() {
    final Vector<?> vector = new Vector<Object>();
    assertTrue(Builder.isValidArray(vector.elements()));
    assertEquals("[]", Builder.build(vector.elements()));
    assertEquals("[]", Builder.build(vector.elements(), true));
    assertEquals("[]", Builder.build(vector.elements(), "\t"));
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
    assertFalse(Builder.isValidArray((Iterable<?>)null));
    assertFalse(Builder.isValidArray((Iterator<?>)null));
    assertFalse(Builder.isValidArray((Enumeration<?>)null));
    final List<?> list = list(new Date());
    assertFalse(Builder.isValidArray(list));
    assertEquals("[]", Builder.build(list));
    assertEquals("[]", Builder.build(list, true));
    assertEquals("[]", Builder.build(list, "\t"));
    assertFalse(Builder.isValidArray(list.iterator()));
    assertEquals("[]", Builder.build(list.iterator()));
    assertEquals("[]", Builder.build(list.iterator(), true));
    assertEquals("[]", Builder.build(list.iterator(), "\t"));
    assertFalse(Builder.isValidArray(Collections.enumeration(list)));
    assertEquals("[]", Builder.build(Collections.enumeration(list)));
    assertEquals("[]", Builder.build(Collections.enumeration(list), true));
    assertEquals("[]", Builder.build(Collections.enumeration(list), "\t"));
  }

  @Test
  public void testBuildNumbers1() {
    final Map<String, ?> map = map(kv("k1", 1), kv("k2", 2.3f), kv("k3", -2L), kv("k4", 1E-3));
    assertTrue(Builder.isValidObject(map));
    assertEquals("{\"k1\":1,\"k2\":2.3,\"k3\":-2,\"k4\":0.001}", Builder.build(map));
    assertEquals("{\n  \"k1\": 1,\n  \"k2\": 2.3,\n  \"k3\": -2,\n  \"k4\": 0.001\n}",
                 Builder.build(map, true));
    assertEquals("{\n\t\"k1\": 1,\n\t\"k2\": 2.3,\n\t\"k3\": -2,\n\t\"k4\": 0.001\n}",
                 Builder.build(map, "\t"));
  }

  @Test
  public void testBuildNumbers2() {
    final List<?> list = list(.2, 3.1f, 0, 4L, 1E-2);
    assertTrue(Builder.isValidArray(list));
    assertEquals("[0.2,3.1,0,4,0.01]", Builder.build(list));
    assertEquals("[\n  0.2,\n  3.1,\n  0,\n  4,\n  0.01\n]", Builder.build(list, true));
    assertEquals("[\n\t0.2,\n\t3.1,\n\t0,\n\t4,\n\t0.01\n]", Builder.build(list, "\t"));
    assertTrue(Builder.isValidArray(list.iterator()));
    assertEquals("[0.2,3.1,0,4,0.01]", Builder.build(list.iterator()));
    assertEquals("[\n  0.2,\n  3.1,\n  0,\n  4,\n  0.01\n]",
                 Builder.build(list.iterator(), true));
    assertEquals("[\n\t0.2,\n\t3.1,\n\t0,\n\t4,\n\t0.01\n]",
                 Builder.build(list.iterator(), "\t"));
    assertTrue(Builder.isValidArray(Collections.enumeration(list)));
    assertEquals("[0.2,3.1,0,4,0.01]", Builder.build(Collections.enumeration(list)));
    assertEquals("[\n  0.2,\n  3.1,\n  0,\n  4,\n  0.01\n]",
                 Builder.build(Collections.enumeration(list), true));
    assertEquals("[\n\t0.2,\n\t3.1,\n\t0,\n\t4,\n\t0.01\n]",
                 Builder.build(Collections.enumeration(list), "\t"));
  }

  @Test
  public void testBuildNumbers3() {
    final long t1 = System.currentTimeMillis();
    try { Thread.sleep(Math.round(Math.random() * 100)); } catch (final InterruptedException ignore) {}
    final long t2 = System.currentTimeMillis();
    final List<?> list = list(t1, t2);
    assertTrue(Builder.isValidArray(list));
    final Object parsedList = Parser.parse(Builder.build(list));
    assertTrue(parsedList instanceof List);
    final List parsedList2 = (List)parsedList;
    assertEquals(2, parsedList2.size());
    assertEquals(t1, parsedList2.get(0));
    assertEquals(t2, parsedList2.get(1));
    assertTrue(Builder.isValidArray(list.iterator()));
    final Object parsedIterator = Parser.parse(Builder.build(list.iterator(), true));
    assertTrue(parsedIterator instanceof List);
    final List parsedIterator2 = (List)parsedList;
    assertEquals(2, parsedIterator2.size());
    assertEquals(t1, parsedIterator2.get(0));
    assertEquals(t2, parsedIterator2.get(1));
    assertTrue(Builder.isValidArray(Collections.enumeration(list)));
    final Object parsedEnumeration = Parser.parse(Builder.build(Collections.enumeration(list), "\t"));
    assertTrue(parsedEnumeration instanceof List);
    final List parsedEnumeration2 = (List)parsedList;
    assertEquals(2, parsedEnumeration2.size());
    assertEquals(t1, parsedEnumeration2.get(0));
    assertEquals(t2, parsedEnumeration2.get(1));
  }

  @Test
  public void testBuildNumbers4() {
    final long t1 = System.currentTimeMillis();
    try { Thread.sleep(Math.round(Math.random() * 100)); } catch (final InterruptedException ignore) {}
    final long t2 = System.currentTimeMillis();
    final Map<String, ?> map = map(kv("a", t1), kv("b", t2));
    assertTrue(Builder.isValidObject(map));
    final Object parsed = Parser.parse(Builder.build(map));
    assertTrue(parsed instanceof Map);
    // noinspection unchecked
    final Map<String, ?> parsedMap = (Map<String, ?>)parsed;
    assertEquals(2, parsedMap.size());
    assertEquals(t1, parsedMap.get("a"));
    assertEquals(t2, parsedMap.get("b"));
    final Object parsed2 = Parser.parse(Builder.build(map, true));
    assertTrue(parsed2 instanceof Map);
    // noinspection unchecked
    final Map<String, ?> parsedMap2 = (Map<String, ?>)parsed2;
    assertEquals(2, parsedMap2.size());
    assertEquals(t1, parsedMap2.get("a"));
    assertEquals(t2, parsedMap2.get("b"));
    final Object parsed3 = Parser.parse(Builder.build(map, "\t"));
    assertTrue(parsed3 instanceof Map);
    // noinspection unchecked
    final Map<String, ?> parsedMap3 = (Map<String, ?>)parsed3;
    assertEquals(2, parsedMap3.size());
    assertEquals(t1, parsedMap3.get("a"));
    assertEquals(t2, parsedMap3.get("b"));
  }

  @Test
  public void testBuildStrings1() {
    final Map<String, ?> map = map(kv("key_a","value \"a\""), kv("a\tb", "\n"));
    assertTrue(Builder.isValidObject(map));
    assertEquals("{\"key_a\":\"value \\\"a\\\"\",\"a\\tb\":\"\\n\"}", Builder.build(map));
    assertEquals("{\n  \"key_a\": \"value \\\"a\\\"\",\n  \"a\\tb\": \"\\n\"\n}",
                 Builder.build(map, true));
    assertEquals("{\n\t\"key_a\": \"value \\\"a\\\"\",\n\t\"a\\tb\": \"\\n\"\n}",
                 Builder.build(map, "\t"));
  }

  @Test
  public void testBuildStrings2() {
    final List<?> list = list("a", "abc", "\n", "\"quotes\"");
    assertTrue(Builder.isValidArray(list));
    assertEquals("[\"a\",\"abc\",\"\\n\",\"\\\"quotes\\\"\"]", Builder.build(list));
    assertEquals("[\n  \"a\",\n  \"abc\",\n  \"\\n\",\n  \"\\\"quotes\\\"\"\n]",
                 Builder.build(list, true));
    assertEquals("[\n\t\"a\",\n\t\"abc\",\n\t\"\\n\",\n\t\"\\\"quotes\\\"\"\n]",
                 Builder.build(list, "\t"));
    assertTrue(Builder.isValidArray(list.iterator()));
    assertEquals("[\"a\",\"abc\",\"\\n\",\"\\\"quotes\\\"\"]", Builder.build(list.iterator()));
    assertEquals("[\n  \"a\",\n  \"abc\",\n  \"\\n\",\n  \"\\\"quotes\\\"\"\n]",
                 Builder.build(list.iterator(), true));
    assertEquals("[\n\t\"a\",\n\t\"abc\",\n\t\"\\n\",\n\t\"\\\"quotes\\\"\"\n]",
                 Builder.build(list.iterator(), "\t"));
    assertTrue(Builder.isValidArray(Collections.enumeration(list)));
    assertEquals("[\"a\",\"abc\",\"\\n\",\"\\\"quotes\\\"\"]",
                 Builder.build(Collections.enumeration(list)));
    assertEquals("[\n  \"a\",\n  \"abc\",\n  \"\\n\",\n  \"\\\"quotes\\\"\"\n]",
                 Builder.build(Collections.enumeration(list), true));
    assertEquals("[\n\t\"a\",\n\t\"abc\",\n\t\"\\n\",\n\t\"\\\"quotes\\\"\"\n]",
                 Builder.build(Collections.enumeration(list), "\t"));
  }

  @Test
  public void testCharSequenceValues() {
    final Map<String, ?> map = map(
      kv("key_a", new StringBuilder("value \"a\"")),
      kv("a\tb", new StringBuilder("\n"))
    );
    assertTrue(Builder.isValidObject(map));
    assertEquals("{\"key_a\":\"value \\\"a\\\"\",\"a\\tb\":\"\\n\"}", Builder.build(map));
    assertEquals("{\n  \"key_a\": \"value \\\"a\\\"\",\n  \"a\\tb\": \"\\n\"\n}",
                 Builder.build(map, true));
    assertEquals("{\n\t\"key_a\": \"value \\\"a\\\"\",\n\t\"a\\tb\": \"\\n\"\n}",
                 Builder.build(map, "\t"));
  }

  @Test
  public void testCharSequenceKeys() {
    final Map<CharSequence, Object> map = new LinkedHashMap<CharSequence, Object>();
    map.put("key_a", "value \"a\"");
    map.put(new StringBuilder("a\tb"), new StringBuilder("\n"));
    assertTrue(Builder.isValidObject(map));
    assertEquals("{\"key_a\":\"value \\\"a\\\"\",\"a\\tb\":\"\\n\"}", Builder.build(map));
    assertEquals("{\n  \"key_a\": \"value \\\"a\\\"\",\n  \"a\\tb\": \"\\n\"\n}",
                 Builder.build(map, true));
    assertEquals("{\n\t\"key_a\": \"value \\\"a\\\"\",\n\t\"a\\tb\": \"\\n\"\n}",
                 Builder.build(map, "\t"));
  }

  @Test
  public void testBuildBooleans() {
    final Map<String, ?> map = map(kv("a", true), kv("b", list(true, false, true)));
    assertTrue(Builder.isValidObject(map));
    assertTrue(Builder.isValidArray((List)map.get("b")));
    assertEquals("{\"a\":true,\"b\":[true,false,true]}", Builder.build(map));
    assertEquals("{\n  \"a\": true,\n  \"b\": [\n    true,\n    false,\n    true\n  ]\n}",
                 Builder.build(map, true));
    assertEquals("{\n\t\"a\": true,\n\t\"b\": [\n\t\ttrue,\n\t\tfalse,\n\t\ttrue\n\t]\n}",
                 Builder.build(map, "\t"));
  }

  @Test
  public void testMixed1() {
    final Map<String, ?> map = map(kv("a", "v"), kv("b", list("a", 2, true)),
                                   kv("c", map(kv("c1", true), kv("c2", list("val")))),
                                   kv("d", list(list(1,2,3), map())));
    assertTrue(Builder.isValidObject(map));
    assertEquals("{\"a\":\"v\",\"b\":[\"a\",2,true],\"c\":{\"c1\":true,\"c2\":[\"val\"]},\"d\":[[1,2,3],{}]}",
                 Builder.build(map));
    assertEquals("{\n  \"a\": \"v\",\n  \"b\": [\n    \"a\",\n    2,\n    true\n  ],\n  \"c\": " +
                 "{\n    \"c1\": true,\n    \"c2\": [\n      \"val\"\n    ]\n  },\n  \"d\": " +
                 "[\n    [\n      1,\n      2,\n      3\n    ],\n    {}\n  ]\n}",
                 Builder.build(map, true));
    assertEquals("{\n\t\"a\": \"v\",\n\t\"b\": [\n\t\t\"a\",\n\t\t2,\n\t\ttrue\n\t],\n\t\"c\": " +
                 "{\n\t\t\"c1\": true,\n\t\t\"c2\": [\n\t\t\t\"val\"\n\t\t]\n\t},\n\t\"d\": " +
                 "[\n\t\t[\n\t\t\t1,\n\t\t\t2,\n\t\t\t3\n\t\t],\n\t\t{}\n\t]\n}",
                 Builder.build(map, "\t"));
  }

  @Test
  public void testMixed2() {
    final List<?> list = list("a","b",map(kv("k",3.5),kv("e",map())),list(map(),true),5,false);
    assertTrue(Builder.isValidArray(list));
    assertEquals("[\"a\",\"b\",{\"k\":3.5,\"e\":{}},[{},true],5,false]",
                 Builder.build(list));
    assertEquals("[\n  \"a\",\n  \"b\",\n  {\n    \"k\": 3.5,\n    \"e\": {}\n  },\n  " +
                 "[\n    {},\n    true\n  ],\n  5,\n  false\n]",
                 Builder.build(list, true));
    assertEquals("[\n\t\"a\",\n\t\"b\",\n\t{\n\t\t\"k\": 3.5,\n\t\t\"e\": {}\n\t},\n\t" +
                 "[\n\t\t{},\n\t\ttrue\n\t],\n\t5,\n\tfalse\n]",
                 Builder.build(list, "\t"));
  }


  private static class ThrowingSink implements InvocationHandler {

    public static BufferedSink proxy(final ThrowingSink sink) {
      final Buffer buffer = new Buffer();
      return (BufferedSink)Proxy.newProxyInstance(buffer.getClass().getClassLoader(),
                                                  buffer.getClass().getInterfaces(),
                                                  sink);
    }

    private final Buffer buffer = new Buffer();
    private final boolean onWrite;
    private final boolean onClose;

    public ThrowingSink(final boolean onWrite, final boolean onClose) {
      this.onWrite = onWrite;
      this.onClose = onClose;
    }

    @Override public Object invoke(final Object proxy, final Method method,
                                   final Object[] args) throws Throwable {
      if (onWrite && method.getName().contains("write")) throw new IOException();
      if (onClose && method.getName().equals("close")) throw new IOException();
      return method.invoke(buffer, args);
    }

    public BufferedSink sink() {
      return proxy(this);
    }

  }

}
