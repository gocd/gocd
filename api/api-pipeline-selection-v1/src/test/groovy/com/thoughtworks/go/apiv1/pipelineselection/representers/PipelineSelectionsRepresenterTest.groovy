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

package com.thoughtworks.go.apiv1.pipelineselection.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.api.util.MessageJson
import com.thoughtworks.go.config.BasicPipelineConfigs
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.server.domain.user.PipelineSelections
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.HaltException

import static com.thoughtworks.go.api.util.HaltApiMessages.propertyIsNotAJsonBoolean
import static com.thoughtworks.go.api.util.HaltApiMessages.propertyIsNotAJsonStringArray

class PipelineSelectionsRepresenterTest {

  @Nested
  class Serialize {
    @Test
    void 'should serialize'() {
      def selections = new PipelineSelections(["build-linux", "build-windows"], new Date(), 10, true)

      def group1 = new BasicPipelineConfigs(group: "grp1")
      def group2 = new BasicPipelineConfigs(group: "grp2")

      group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline1")))
      group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline2")))

      List<PipelineConfigs> pipelineConfigs = [group1, group2]
      def actualJson = PipelineSelectionsRepresenter.toJSON(new PipelineSelectionResponse(selections, pipelineConfigs), null)
      JsonFluentAssert.assertThatJson(actualJson).isEqualTo([
        pipelines : [
          grp1: [],
          grp2: ["pipeline1", "pipeline2"]
        ],
        selections: ['build-linux', 'build-windows'],
        blacklist : true
      ])
    }
  }

  @Nested
  class Deserialize {
    @Test
    void 'should deserialize'() {
      def json = [
        selections: ['build-linux', 'build-windows'],
        blacklist : true
      ]

      PipelineSelectionResponse response = PipelineSelectionsRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom(json))
      Assertions.assertThat(response.selectedPipelines.pipelineList()).isEqualTo(["build-linux", "build-windows"])
      Assertions.assertThat(response.selectedPipelines.isBlacklist()).isTrue()
    }

    @Test
    void 'should blow up on bad blacklist type'() {
      Assertions.assertThatCode({
        PipelineSelectionsRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom([blacklist: [:]]))
      }).isInstanceOf(HaltException)
        .hasFieldOrPropertyWithValue("statusCode", 422)
        .hasFieldOrPropertyWithValue("body", MessageJson.create(propertyIsNotAJsonBoolean("blacklist")))
    }

    @Test
    void 'should blow up on bad selection type'() {
      Assertions.assertThatCode({
        PipelineSelectionsRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom([selections: [[:]]]))
      }).isInstanceOf(HaltException)
        .hasFieldOrPropertyWithValue("statusCode", 422)
        .hasFieldOrPropertyWithValue("body", MessageJson.create(propertyIsNotAJsonStringArray("selections")))
    }
  }
}

