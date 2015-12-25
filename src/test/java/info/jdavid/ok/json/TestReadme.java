package info.jdavid.ok.json;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TestReadme {

  @Test
  public void testJava() {
    final String obj = "{\"key1\":1,\"key2\":\"abc\",\"key3\":[1,2,3]}";
    final Map<String, ?> map = Parser.parse(obj);
    assert Integer.valueOf(1).equals(map.get("key1"));
    assert "abc".equals(map.get("key2"));
    assert map.get("key3") instanceof List;

    final String obj2 = Builder.build(map);
    assert obj.equals(obj2);


    final String arr = "[\"a\",1.5,{\"key\":\"value\"}]";
    final List<?> list = Parser.parse(arr);
    assert 3 == list.size();
    assert "a".equals(list.get(0));
    assert Double.valueOf(1.5).equals(list.get(1));
    assert list.get(2) instanceof Map;

    final String arr2 = Builder.build(list);
    assert arr.equals(arr2);
  }

}
