/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.dependencymaterialautocomplete.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.StageConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class SuggestionsRepresenterTest {

  @Test
  void 'should create json'() {
    List<PipelineConfig> suggestions = [
      pipeline("pipe-a", stage("stage-a1"), stage("stage-a2")),
      pipeline("pipe-b", stage("stage-b1")),
    ]

    String actualJson = toArrayString({ SuggestionsRepresenter.toJSON(it, suggestions) })

    assertThatJson(actualJson).isEqualTo([
      [
        "name"  : "pipe-a",
        "stages": ["stage-a1", "stage-a2"]
      ],
      [
        "name"  : "pipe-b",
        "stages": ["stage-b1"]
      ]
    ])
  }

  private CaseInsensitiveString ident(String name) {
    return new CaseInsensitiveString(name)
  }

  private PipelineConfig pipeline(String name, StageConfig... stages) {
    return new PipelineConfig(ident(name), null, stages)
  }

  private StageConfig stage(String name) {
    return new StageConfig(ident(name), null);
  }

}
