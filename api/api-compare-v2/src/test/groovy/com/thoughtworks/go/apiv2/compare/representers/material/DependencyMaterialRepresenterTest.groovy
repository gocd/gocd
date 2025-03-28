/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.apiv2.compare.representers.material


import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.helper.MaterialConfigsMother

class DependencyMaterialRepresenterTest implements MaterialRepresenterTrait {
  def existingMaterial() {
    MaterialConfigsMother.dependencyMaterialConfig()
  }

  def existingMaterialWithErrors() {
    def dependencyConfig = new DependencyMaterialConfig(new CaseInsensitiveString(''), new CaseInsensitiveString(''))
    def materialConfigs = new MaterialConfigs(dependencyConfig)
    def pipeline = new PipelineConfig(new CaseInsensitiveString("p"), materialConfigs)
    pipeline.setOrigins(new FileConfigOrigin())
    materialConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), pipeline))
    return materialConfigs.first()
  }

  def materialHash =
    [
      type      : 'dependency',
      attributes: [
        pipeline    : "pipeline-name",
        stage       : "stage-name",
        name        : null,
        auto_update : true,
        display_type: "Pipeline",
        description : "pipeline-name [ stage-name ]",
      ]
    ]

  def expectedMaterialHashWithErrors =
    [
      type      : "dependency",
      attributes:
        [
          pipeline    : "",
          stage       : "",
          name        : null,
          auto_update : true,
          display_type: "Pipeline",
          description : " [  ]",
        ],
      errors    : [
        pipeline: ["Pipeline with name '' does not exist, it is defined as a dependency for pipeline 'p' (cruise-config.xml)"]
      ]
    ]
}
