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

package com.thoughtworks.go.api

import com.google.gson.JsonParseException
import com.thoughtworks.go.api.util.GsonTransformer
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

import java.time.Instant
import java.time.temporal.ChronoUnit

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType

class GsonTransformerTest {
  private GsonTransformer gsonTransformer = GsonTransformer.instance

  @Test
  void shouldSerializeDateInParticularFormat() {
    def date = Date.from(
      Instant.ofEpochSecond(0)
        .plus(12, ChronoUnit.HOURS)
        .plus(13, ChronoUnit.MINUTES)
        .plus(14, ChronoUnit.SECONDS)
        .plus(1, ChronoUnit.DAYS)
        .plus(30, ChronoUnit.DAYS)
        .plus(365, ChronoUnit.DAYS)
    )

    JSONAssert.assertEquals(gsonTransformer.render([x: date]), '{"x": "1971-02-01T12:13:14Z"}', true)
  }

  @Test
  void shouldNotEscapeHtml() {
    JSONAssert.assertEquals(gsonTransformer.render([x: "<html>"]), '{"x": "<html>"}', true)
  }

  @Test
  void shouldSerializeNulls() {
    JSONAssert.assertEquals(gsonTransformer.render([x: null]), '{"x": null}', true)
  }

  @Test
  void shouldThrowJsonParseExceptionWhenReadingBadData() {
    assertThatExceptionOfType(JsonParseException.class)
    .isThrownBy({gsonTransformer.jsonReaderFrom("bad-data")})
  }
}
