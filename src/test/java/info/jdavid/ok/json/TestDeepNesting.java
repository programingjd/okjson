package info.jdavid.ok.json;

import java.io.IOException;

import okio.Buffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestDeepNesting {

  @Test
  public void deeperNestingArrays() throws IOException {
    final Buffer buffer = new Buffer();
    final JsonWriter writer = new JsonWriter(buffer);
    for (int i=0; i<32; ++i) {
      writer.beginArray();
    }
    for (int i=0; i<32; ++i) {
      writer.endArray();
    }
    writer.flush();
    assertEquals("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]",
                 buffer.readUtf8());
  }

  @Test
  public void deeperNestingObjects() throws IOException {
    final Buffer buffer = new Buffer();
    final JsonWriter writer = new JsonWriter(buffer);
    for (int i=0; i<32; ++i) {
      writer.beginObject();
      writer.name("a");
    }
    writer.value(true);
    for (int i=0; i<32; ++i) {
      writer.endObject();
    }
    assertEquals("{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":" +
                 "{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":" +
                 "{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":" +
                 "true}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}", buffer.readUtf8());
  }

  @Test
  public void deeplyNestedArrays() throws IOException {
    final Buffer buffer = new Buffer();
    buffer.writeUtf8("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]");
    final JsonReader reader = new JsonReader(buffer);

    for (int i=0; i<32; ++i) {
      reader.beginArray();
    }
    for (int i=0; i<32; ++i) {
      reader.endArray();
    }
    assertEquals(JsonReader.Token.END_DOCUMENT, reader.peek());
  }

  @Test
  public void deeplyNestedObjects() throws IOException {
    final String template = "{\"a\":%s}";
    String json = "true";
    for (int i=0; i<32; ++i) {
      json = String.format(template, json);
    }

    final Buffer buffer = new Buffer();
    buffer.writeUtf8(json);
    final JsonReader reader = new JsonReader(buffer);
    for (int i=0; i<32; ++i) {
      reader.beginObject();
      assertEquals("a", reader.nextName());
    }
    assertEquals(true, reader.nextBoolean());
    for (int i=0; i<32; ++i) {
      reader.endObject();
    }
    assertEquals(JsonReader.Token.END_DOCUMENT, reader.peek());
  }

}
