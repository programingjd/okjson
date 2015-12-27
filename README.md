![jcenter](https://img.shields.io/badge/_jcenter_-_2.7.0-6688ff.png?style=flat) &#x2003; ![jcenter](https://img.shields.io/badge/_Tests_-_40/40-green.png?style=flat)
# okjson
A JSON library for the jvm and android, built on top of [okio](https://github.com/square/okio) and [moshi](https://github.com/square/moshi/).
 
It transforms JSON objects to `Maps` and JSON arrays to `Lists`.

## Download ##

The maven artifacts are on [Bintray](https://bintray.com/programingjd/maven/info.jdavid.ok.json/view)
and [jcenter](https://bintray.com/search?query=info.jdavid.ok.json).

[Download](https://bintray.com/artifact/download/programingjd/maven/info/jdavid/ok/json/okjson/2.7.0/okjson-2.7.0.jar) the latest jar.

__Maven__

Include [those settings](https://bintray.com/repo/downloadMavenRepoSettingsFile/downloadSettings?repoPath=%2Fbintray%2Fjcenter)
 to be able to resolve jcenter artifacts.
```
<dependency>
  <groupId>info.jdavid.ok.json</groupId>
  <artifactId>okjson</artifactId>
  <version>2.7.0</version>
</dependency>
```
__Gradle__

Add jcenter to the list of maven repositories.
```
repositories {
  jcenter()
}
```
```
dependencies {
  compile 'info.jdavid.ok.json:okjson:2.7.0'
}
```

## Usage ##


__java__
```java
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
```
__groovy__
```groovy
String obj = '{"key1":1,"key2":"abc","key3":[1,2,3]}'
Map map = Parser.parse(obj)
assert 1 == map.key1
assert 'abc' == map.key2
assert map.key3 instanceof List
assert 3 == map.key3.size()
assert 1 == map.key3[0]
assert 2 == map.key3[1]
assert 3 == map.key3[2]

String obj2 = Builder.build(map)
assert obj == obj2
String obj3 = Builder.build([key1: 1, key2: 'abc', key3: [1, 2, 3]])
assert obj == obj3


String arr = '["a",1.5,{"key":"value"}]'
List list = Parser.parse(arr);
assert 3 == list.size();
assert "a" == list[0]
assert 1.5 == list[1]
assert list[2] instanceof Map
assert 'value' == list[2].key

String arr2 = Builder.build(list);
assert arr == arr2
String arr3 = Builder.build(['a', 1.5, [key: 'value']])
assert arr == arr3
```