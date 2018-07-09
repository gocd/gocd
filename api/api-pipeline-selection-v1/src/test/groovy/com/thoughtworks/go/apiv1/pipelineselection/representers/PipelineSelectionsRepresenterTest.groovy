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

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.api.util.MessageJson
import com.thoughtworks.go.config.BasicPipelineConfigs
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigs
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.HaltException

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.api.util.HaltApiMessages.propertyIsNotAJsonBoolean
import static com.thoughtworks.go.api.util.HaltApiMessages.propertyIsNotAJsonStringArray

class PipelineSelectionsRepresenterTest {

  @Nested
  class Serialize {
    @Test
    void 'should serialize'() {
      def group1 = new BasicPipelineConfigs(group: "grp1")
      def group2 = new BasicPipelineConfigs(group: "grp2")

      group1.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline3")))
      group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline1")))
      group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline2")))

      List<PipelineConfigs> pipelineConfigs = [group1, group2]

      def actualJson = toObjectString({
        PipelineSelectionsRepresenter.toJSON(it, new PipelineSelectionResponse(['build-linux', 'build-windows'], true, pipelineConfigs))
      })

      JsonFluentAssert.assertThatJson(actualJson).isEqualTo([
        pipelines : [
          grp1: ["pipeline3"],
          grp2: ["pipeline1", "pipeline2"]
        ],
        selections: ['build-linux', 'build-windows'],
        blacklist : true
      ])
    }

    @Test
    void 'should not serialize empty pipeline groups'() {
      def group1 = new BasicPipelineConfigs(group: "grp1")
      def group2 = new BasicPipelineConfigs(group: "grp2")

      group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline1")))
      group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline2")))

      List<PipelineConfigs> pipelineConfigs = [group1, group2]

      def actualJson = toObjectString({
        PipelineSelectionsRepresenter.toJSON(it, new PipelineSelectionResponse(['build-linux', 'build-windows'], true, pipelineConfigs))
      })

      JsonFluentAssert.assertThatJson(actualJson).isEqualTo([
        pipelines : [
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
      Assertions.assertThat(response.selections()).isEqualTo(["build-linux", "build-windows"])
      Assertions.assertThat(response.blacklist()).isTrue()
    }

    @Test
    void 'should blow up on bad blacklist type'() {
      def jsonObject = new JsonObject()
      jsonObject.add("blacklist", new JsonObject())

      Assertions.assertThatCode({
        PipelineSelectionsRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom([blacklist: [:]]))
      }).isInstanceOf(HaltException)
        .hasFieldOrPropertyWithValue("statusCode", 422)
        .hasFieldOrPropertyWithValue("body", MessageJson.create(propertyIsNotAJsonBoolean("blacklist", jsonObject)))
    }

    @Test
    void 'should blow up on bad selection type'() {
      def jsonObject = new JsonObject()
      def array = new JsonArray()
      array.add(new JsonObject())

      jsonObject.add("selections", array)
      Assertions.assertThatCode({
        PipelineSelectionsRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom([selections: [[:]]]))
      }).isInstanceOf(HaltException)
        .hasFieldOrPropertyWithValue("statusCode", 422)
        .hasFieldOrPropertyWithValue("body", MessageJson.create(propertyIsNotAJsonStringArray("selections", jsonObject)))
    }
  }
}

