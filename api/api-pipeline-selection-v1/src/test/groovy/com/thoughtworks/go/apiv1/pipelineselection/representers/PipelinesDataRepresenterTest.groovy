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
package com.thoughtworks.go.apiv1.pipelineselection.representers

import com.thoughtworks.go.config.BasicPipelineConfigs
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigs
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PipelinesDataRepresenterTest {
  @Nested
  class Serialize {
    @Test
    void 'should serialize'() {
      def group1 = new BasicPipelineConfigs(group: "grp1")
      def group2 = new BasicPipelineConfigs(group: "grp2")

      group1.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline3")))
      group1.add(new PipelineConfig(name: new CaseInsensitiveString("build-linux")))
      group1.add(new PipelineConfig(name: new CaseInsensitiveString("build-windows")))
      group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline1")))
      group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline2")))

      List<PipelineConfigs> pipelineConfigs = [group1, group2]

      String actualJson = PipelinesDataRepresenter.toJSON(new PipelinesDataResponse(pipelineConfigs))

      JsonFluentAssert.assertThatJson(actualJson).isEqualTo([
        pipelines: [
          grp1: ["pipeline3", "build-linux", "build-windows"],
          grp2: ["pipeline1", "pipeline2"]
        ]])
    }
  }
}
