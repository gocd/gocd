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
package com.thoughtworks.go.apiv2.materials.representers.materials


import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.PackageMaterialConfig
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PackageMaterialRepresenterTest {

  @Test
  void "should represent a package material"() {
    PackageMaterialConfig packageMaterialConfig = MaterialConfigsMother.packageMaterialConfig()
    def actualJson = toObjectString(MaterialsRepresenter.toJSON(packageMaterialConfig))

    assertThatJson(actualJson).isEqualTo([
      type       : 'package',
      fingerprint: packageMaterialConfig.fingerprint,
      attributes :
        [
          ref: "p-id"
        ]
    ])
  }

  @Test
  void "should render errors"() {
    def package_config = new PackageMaterialConfig(new CaseInsensitiveString(''), '', null)
    def material_configs = new MaterialConfigs(package_config)
    material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))

    def actualJson = toObjectString(MaterialsRepresenter.toJSON(material_configs.first()))

    assertThatJson(actualJson).isEqualTo([
      type       : "package",
      fingerprint: package_config.fingerprint,
      attributes : [
        ref: ""
      ],
      errors     : [
        ref: ["Please select a repository and package"]
      ]
    ])
  }

}
