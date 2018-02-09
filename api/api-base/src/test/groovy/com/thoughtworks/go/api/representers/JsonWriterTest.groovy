/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.spark.mocks.TestRequestContext
import org.junit.jupiter.api.Test

import static org.assertj.core.api.AssertionsForClassTypes.assertThat

class JsonWriterTest {

  @Test
  void "should add property"() {
    def jsonWriter = new JsonWriter(new TestRequestContext())
    jsonWriter.add("foo", "bar")

    assertThat(jsonWriter.getAsMap()).isEqualTo([foo: "bar"])
  }

  @Test
  void "should add property if not null"() {
    def jsonWriter = new JsonWriter(new TestRequestContext())
    jsonWriter.addIfNotNull("foo", "not-null-value")

    assertThat(jsonWriter.getAsMap()).isEqualTo([foo: "not-null-value"])
  }

  @Test
  void "should not add property if null"() {
    def jsonWriter = new JsonWriter(new TestRequestContext())
    jsonWriter.addIfNotNull("foo", null)

    assertThat(jsonWriter.getAsMap()).isEqualTo([:])
  }

  @Test
  void "should add Optional property if present"() {
    def jsonWriter = new JsonWriter(new TestRequestContext())
    jsonWriter.addOptional("foo", Optional.of("some-value"))

    assertThat(jsonWriter.getAsMap()).isEqualTo([foo: "some-value"])
  }

  @Test
  void "should not add Optional property if empty"() {
    def jsonWriter = new JsonWriter(new TestRequestContext())
    jsonWriter.addOptional("foo", Optional.empty())

    assertThat(jsonWriter.getAsMap()).isEqualTo([:])
  }

  @Test
  void "should add an embedded list"() {
    def jsonWriter = new JsonWriter(new TestRequestContext())
    jsonWriter.addEmbedded("stages", [
      [name: "stage1"],
      [name: "stage2"],
    ])

    jsonWriter.addEmbedded("foo", [
      [name: "bar1"],
      [name: "bar2"],
    ])

    assertThat(jsonWriter.getAsMap()).isEqualTo([_embedded: [
      stages: [
        [name: "stage1"],
        [name: "stage2"]
      ],
      foo   : [
        [name: "bar1"],
        [name: "bar2"]
      ]
    ]])
  }

  @Test
  void "should add link without named params"() {
    def jsonWriter = new JsonWriter(new TestRequestContext())
    jsonWriter.addLink("self", "/api/foo/bar")

    assertThat(jsonWriter.getAsMap()).isEqualTo([_links: [
      self: [href: "http://test.host/go/api/foo/bar"]
    ]])
  }

  @Test
  void "should add doc link"() {
    def jsonWriter = new JsonWriter(new TestRequestContext())
    jsonWriter.addDocLink("https://api.gocd.org/current/#agents")

    assertThat(jsonWriter.getAsMap()).isEqualTo([_links: [
      doc: [href: "https://api.gocd.org/current/#agents"]
    ]])
  }


}
