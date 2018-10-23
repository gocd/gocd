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

package com.thoughtworks.go.apiv2.configrepos.representers

import com.thoughtworks.go.config.PartialConfigParseResult
import com.thoughtworks.go.config.remote.PartialConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PartialConfigParseResultRepresenterTest {
  @Test
  void 'toJSON() with successful result'() {
    PartialConfigParseResult result = new PartialConfigParseResult("abc", new PartialConfig())
    String json = toObjectString({ w -> PartialConfigParseResultRepresenter.toJSON(w, result) })
    assertThatJson(json).isEqualTo([
      revision: "abc",
      success : true,
      error   : null
    ])
  }

  @Test
  void 'toJSON() with failing result'() {
    PartialConfigParseResult result = new PartialConfigParseResult("abc", new RuntimeException("bang!"))
    String json = toObjectString({ w -> PartialConfigParseResultRepresenter.toJSON(w, result) })
    assertThatJson(json).isEqualTo([
      revision: "abc",
      success : false,
      error   : "bang!"
    ])
  }
}