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
package com.thoughtworks.go.apiv2.compare.representers.material

import com.thoughtworks.go.apiv2.compare.representers.MaterialRepresenter
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig
import com.thoughtworks.go.domain.config.PluginConfiguration
import com.thoughtworks.go.domain.scm.SCM
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PluggableScmMaterialRepresenterTest {

  @Test
  void "should represent a pluggable scm material"() {
    def pluggableSCMMaterial = MaterialConfigsMother.pluggableSCMMaterialConfig()
    def actualJson = toObjectString({ MaterialRepresenter.toJSON(it, pluggableSCMMaterial) })

    assertThatJson(actualJson).isEqualTo(pluggableScmMaterialHash)
  }

  @Test
  void "should render errors"() {
    SCM scmConfig = mock(SCM.class)
    when(scmConfig.getName()).thenReturn("scm-name")
    when(scmConfig.getConfigForDisplay()).thenReturn("k1:v1")
    when(scmConfig.getPluginConfiguration()).thenReturn(mock(PluginConfiguration.class))

    def pluggableScmMaterial = new PluggableSCMMaterialConfig(new CaseInsensitiveString(''), scmConfig, '/dest', null, false)
    def materialConfigs = new MaterialConfigs(pluggableScmMaterial)
    materialConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))

    def actualJson = toObjectString({ MaterialRepresenter.toJSON(it, materialConfigs.first()) })
    assertThatJson(actualJson).isEqualTo(expectedMaterialHashWithErrors)
  }

  def pluggableScmMaterialHash =
    [
      type      : 'plugin',
      attributes: [
        ref         : "scm-id",
        filter      : [
          ignore: ["**/*.html", "**/foobar/"]
        ],
        destination : 'des-folder',
        display_type: "SCM",
        description : "WARNING! Plugin missing. []",
      ]
    ]

  def expectedMaterialHashWithErrors =
    [
      type      : "plugin",
      attributes: [
        ref         : null,
        filter      : null,
        destination : "/dest",
        display_type: "SCM",
        description : "k1:v1",
      ],
      errors    : [
        destination: ["Dest folder '/dest' is not valid. It must be a sub-directory of the working folder."],
        ref        : ["Please select a SCM"]
      ]
    ]

}
