/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.api.representers

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.thoughtworks.go.api.base.JsonOutputWriter
import com.thoughtworks.go.spark.mocks.TestRequestContext
import org.apache.poi.util.LocaleUtil
import org.junit.jupiter.api.Test

import static org.assertj.core.api.AssertionsForClassTypes.assertThat
import static org.assertj.core.api.AssertionsForClassTypes.fail

class JsonOutputWriterTest {
  @Test
  void 'should output simple properties'() {
    def result = new StringWriter()

    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer.add("key1", "value1")
      writer.add("key2", "value2")
      writer.addIfNotNull("key3", "value3")
      writer.addIfNotNull("key4", (String) null)
    }

    assertThat(fromJSON(result.toString())).isEqualTo([key1: 'value1', key2: 'value2', key3: 'value3'])
  }

  @Test
  void 'should output time'() {
    def result = new StringWriter()

    def calendar = Calendar.getInstance(LocaleUtil.TIMEZONE_UTC)
    calendar.set(2000, Calendar.JANUARY, 2, 10, 11, 12)

    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer.add("key1", calendar.getTime())
      writer.addIfNotNull("key2", calendar.getTime())
      writer.addIfNotNull("key3", (Date) null)
    }

    assertThat(fromJSON(result.toString())).isEqualTo([key1: '2000-01-02T10:11:12Z', key2: '2000-01-02T10:11:12Z'])
  }

  @Test
  void 'should output primitive types'() {
    def result = new StringWriter()

    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer
        .add("key1", Integer.valueOf(123))
        .add("key2", false)
        .add("key3", Long.valueOf(1234567891234))
    }

    def outputAsMap = fromJSON(result.toString())
    assertThat(outputAsMap['key1']).isEqualTo(123)
    assertThat(outputAsMap['key2']).isEqualTo(false)
    assertThat(outputAsMap['key3']).isEqualTo(1234567891234L)

    assertThat(result.toString()).containsPattern("\"key1\" *: *123")
    assertThat(result.toString()).containsPattern("\"key2\" *: *false")
    assertThat(result.toString()).containsPattern("\"key3\" *: *1234567891234")
  }

  @Test
  void 'should output a child object'() {
    def result = new StringWriter()

    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer
        .addChild("child1") { child -> child.add("key1", "value1") }
        .addChild("child2") { child -> child.add("key2", "value2") }
    }

    assertThat(fromJSON(result.toString())).isEqualTo([
      child1: [
        key1: 'value1'
      ],
      child2: [
        key2: 'value2'
      ]
    ])
  }

  @Test
  void 'should be able to nest child objects'() {
    def result = new StringWriter()

    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer
        .addChild("parent") { child ->
        child.add("outerkey1", "outervalue1")

        child.addChild("innerchild1") { innerChild ->
          innerChild.add("innerkey1", "innervalue1")
        }

        child.add("outerkey2", "outervalue2")
      }
    }

    assertThat(fromJSON(result.toString())).isEqualTo([
      parent: [
        outerkey1  : 'outervalue1',
        innerchild1: [
          innerkey1: 'innervalue1'
        ],
        outerkey2  : 'outervalue2'
      ]
    ])
  }

  @Test
  void 'should be able to add simple child lists'() {
    def result1 = new StringWriter()
    new JsonOutputWriter(result1, new TestRequestContext()).forTopLevelObject { writer ->
      writer.addChildList('parent1', ['value1', 'value2', 'value3'])
    }
    assertThat(fromJSON(result1.toString())).isEqualTo([parent1: ['value1', 'value2', 'value3']])

    def result2 = new StringWriter()
    new JsonOutputWriter(result2, new TestRequestContext()).forTopLevelObject { writer ->
      writer
        .addChildList('parent1') { listWriter ->
        listWriter.value('value1')
        listWriter.value('value2')
        listWriter.value('value3')
      }
    }
    assertThat(fromJSON(result2.toString())).isEqualTo([parent1: ['value1', 'value2', 'value3']])
  }

  @Test
  void 'should be able to add child lists with embedded child objects'() {
    def result = new StringWriter()

    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer
        .addChildList('parent1') { listWriter ->
        listWriter.addChild { listChildWriter ->
          listChildWriter.add('key1', 'value1')
        }
        listWriter.addChild { listChildWriter ->
          listChildWriter.add('key1', 'value2')
        }
      }
    }

    assertThat(fromJSON(result.toString())).isEqualTo([
      parent1: [
        [key1: 'value1'],
        [key1: 'value2']
      ]
    ])
  }

  @Test
  void 'should be able to add links'() {
    def result = new StringWriter()

    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer
        .addLinks { linksWriter ->
        linksWriter.addLink("key1", "/href1")
        linksWriter.addLink("key2", "/href2")
      }
    }

    assertThat(fromJSON(result.toString())).isEqualTo([
      _links: [
        key1: [href: "http://test.host/go/href1"],
        key2: [href: "http://test.host/go/href2"]
      ]
    ])
  }

  @Test
  void 'should be able to decide between a top-level object and a top-level list'() {
    def result = new StringWriter()
    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer.add("key", "value")
    }
    assertThat(fromJSON(result.toString())).isEqualTo([key: "value"])

    result = new StringWriter()
    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelArray { writer ->
      writer.value("value1")
      writer.value("value2")
      writer.value("value3")
    }
    assertThat(fromJSONArray(result.toString())).isEqualTo(["value1", "value2", "value3"])

    result = new StringWriter()
    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelArray { writer ->
      writer.addChild { childWriter -> childWriter.add("key", "value1") }
      writer.addChild { childWriter -> childWriter.add("key", "value2") }
      writer.addChild { childWriter -> childWriter.add("key", "value3") }
    }
    assertThat(fromJSONArray(result.toString())).isEqualTo([
      [key: "value1"],
      [key: "value2"],
      [key: "value3"]
    ])
  }

  @Test
  void 'should not output valid JSON if an exception is thrown during processing'() {
    Object someObjectWhichWillThrowOnToString = new Object() {
      @Override
      String toString() {
        throw new RuntimeException("THROWS!")
      }
    }

    assertInvalidJSONOutput { outputWriter ->
      new JsonOutputWriter(outputWriter, new TestRequestContext()).forTopLevelObject { writer ->
        writer.add("key", someObjectWhichWillThrowOnToString.toString())
      }
    }

    assertInvalidJSONOutput { outputWriter ->
      new JsonOutputWriter(outputWriter, new TestRequestContext()).forTopLevelObject { writer ->
        writer.addChild("child1") { childWriter -> childWriter.add("key", someObjectWhichWillThrowOnToString.toString()) }
      }
    }

    assertInvalidJSONOutput { outputWriter ->
      new JsonOutputWriter(outputWriter, new TestRequestContext()).forTopLevelArray { writer ->
        writer.addChild { childWriter -> childWriter.add("key", "value1") }
        writer.addChild { childWriter -> childWriter.add("key", someObjectWhichWillThrowOnToString.toString()) }
        writer.addChild { childWriter -> childWriter.add("key", "value3") }
      }
    }

    assertInvalidJSONOutput { outputWriter ->
      new JsonOutputWriter(outputWriter, new TestRequestContext()).forTopLevelArray { writer ->
        writer.value("value1")
        writer.value(someObjectWhichWillThrowOnToString.toString())
        writer.value("value3")
      }
    }
  }

  @Test
  void 'should output date in milliseconds'() {
    def result = new StringWriter()
    def date = new Date()

    new JsonOutputWriter(result, new TestRequestContext()).forTopLevelObject { writer ->
      writer.addInMillis("timestamp", date)
    }

    assertThat(fromJSON(result.toString())).isEqualTo([
      timestamp: date.getTime()
    ])
  }

  def assertInvalidJSONOutput(Closure closure) {
    def result = new StringWriter()

    try {
      closure.call(result)
      fail("This should have failed!")
    } catch (RuntimeException ignored) {

      try {
        OBJECT_MAPPER.readValue(result.toString(), Object.class)
        fail("This should have been an invalid JSON: " + result.toString())
      } catch (JsonParseException | JsonMappingException e) {
        assertThat(e.getMessage()).contains("Failed due to an exception.")
      }

    }
  }

  def OBJECT_MAPPER = new ObjectMapper()

  Object fromJSON(String jsonString) {
    OBJECT_MAPPER.readValue(jsonString, Map.class)
  }

  Object fromJSONArray(String jsonString) {
    OBJECT_MAPPER.readValue(jsonString, List.class)
  }
}
