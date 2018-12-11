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

import com.thoughtworks.go.api.util.GsonTransformer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.HaltException

import static org.assertj.core.api.AssertionsForClassTypes.assertThat
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType

class JsonReaderTest {

  @Nested
  class String {
    @Test
    void 'should read valid value'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["foo": "bar"])
      assertThat(reader.getString("foo")).isEqualTo("bar")
    }

    @Test
    void 'should read optional value when present'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["foo": "bar"])
      assertThat(reader.optString("foo").get()).isEqualTo("bar")
    }

    @Test
    void 'should read optional value when absent'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["xyz": "bar"])
      assertThat(reader.optString("foo").isPresent()).isFalse()
    }

    @Test
    void 'should blow up if reading wrong type'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["foo": ["bar": "baz"]])
      assertThatExceptionOfType(HaltException.class)
        .isThrownBy({ reader.getString("foo") })
      assertThatExceptionOfType(HaltException.class)
        .isThrownBy({ reader.optString("foo") })
    }
  }

  @Nested
  class JsonArray {

    @Test
    void 'should read optional value when present'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["foo": ["bar", "baz"]])
      def expectedArray = new com.google.gson.JsonArray()
      expectedArray.add("bar")
      expectedArray.add("baz")
      assertThat(reader.optJsonArray("foo").get()).isEqualTo(expectedArray)
    }

    @Test
    void 'should read optional value when absent'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["xyz": "bar"])
      assertThat(reader.optJsonArray("foo").isPresent()).isFalse()
    }

    @Test
    void 'should blow up if reading wrong type'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["foo": "bar"])
      assertThatExceptionOfType(HaltException.class)
        .isThrownBy({ reader.optJsonArray("foo") })
    }
  }

  @Nested
  class JsonObject {

    @Test
    void 'should read json object'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["foo": ["bar": "baz"]])
      def newReader = reader.readJsonObject("foo")

      assertThat(newReader.getString("bar")).isEqualTo("baz")
    }

    @Test
    void 'should optionally read json object'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["foo": ["bar": "baz"]])
      def newReader = reader.optJsonObject("other")

      assertThat(newReader.isPresent()).isFalse()
    }

    @Test
    void 'should blow up if reading wrong type'() {
      def reader = GsonTransformer.instance.jsonReaderFrom(["foo": "bar"])
      assertThatExceptionOfType(HaltException.class)
        .isThrownBy({ reader.optJsonObject("foo") })
      assertThatExceptionOfType(HaltException.class)
        .isThrownBy({ reader.readJsonObject("foo") })
    }
  }

  @Nested
  class GetBoolean {
    @Test
    void 'should retrieve boolean value'() {
      def reader = GsonTransformer.instance.jsonReaderFrom([
        "boolean_true" : true,
        "boolean_false": false,
        "string_true"  : "true",
        "string_false" : "false"
      ])

      assertThat(reader.getBoolean("boolean_true")).isTrue()
      assertThat(reader.getBoolean("boolean_false")).isFalse()
      assertThat(reader.getBoolean("string_true")).isTrue()
      assertThat(reader.getBoolean("string_false")).isFalse()
    }

    @Test
    void 'should error out when property is missing'() {
      def reader = GsonTransformer.instance.jsonReaderFrom([:])

      assertThatExceptionOfType(HaltException.class)
        .isThrownBy({reader.getBoolean("foo")})
    }
  }
}
