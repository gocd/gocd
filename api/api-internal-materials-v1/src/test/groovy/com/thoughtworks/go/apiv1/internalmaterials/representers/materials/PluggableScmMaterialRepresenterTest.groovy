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
package com.thoughtworks.go.apiv1.internalmaterials.representers.materials


import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig
import com.thoughtworks.go.domain.scm.SCM
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PluggableScmMaterialRepresenterTest {

  @Test
  void "should represent a pluggable scm material"() {
    def pluggableSCMMaterial = MaterialConfigsMother.pluggableSCMMaterialConfig()
    def actualJson = toObjectString(MaterialsRepresenter.toJSON(pluggableSCMMaterial))

    assertThatJson(actualJson).isEqualTo([
      type       : 'plugin',
      fingerprint: pluggableSCMMaterial.fingerprint,
      attributes : [
        ref        : "scm-id",
        auto_update: true,
        filter     : [
          ignore: ["**/*.html", "**/foobar/"]
        ],
        destination: 'des-folder',
      ]
    ])
  }

  @Test
  void "should render errors"() {
    def pluggableScmMaterial = new PluggableSCMMaterialConfig(new CaseInsensitiveString(''), new SCM("id", "name"), '/dest', null)
    def materialConfigs = new MaterialConfigs(pluggableScmMaterial)
    materialConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))

    def actualJson = toObjectString(MaterialsRepresenter.toJSON(materialConfigs.first()))
    assertThatJson(actualJson).isEqualTo([
      type       : "plugin",
      fingerprint: pluggableScmMaterial.fingerprint,
      attributes : [
        ref        : "id",
        auto_update: true,
        filter     : null,
        destination: "/dest",
      ],
      errors     : [
        destination: ["Dest folder '/dest' is not valid. It must be a sub-directory of the working folder."],
        ref        : ["Could not find SCM for given scm-id: [id]."]
      ]
    ])
  }
}
