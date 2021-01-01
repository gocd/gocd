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
package com.thoughtworks.go.apiv10.shared.representers.materials

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv10.admin.shared.representers.materials.MaterialsRepresenter
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.ConfigHelperOptions
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.PackageMaterialConfig
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.mockito.Mockito.mock

class PackageMaterialRepresenterTest {

  def getOptions() {
    return new ConfigHelperOptions(mock(BasicCruiseConfig.class), mock(PasswordDeserializer.class))
  }

  @Test
  void "should represent a package material"() {
    def actualJson = toObjectString({ MaterialsRepresenter.toJSON(it, MaterialConfigsMother.packageMaterialConfig()) })

    assertThatJson(actualJson).isEqualTo(packageMaterialHash())
  }

  @Test
  void "should deserialize"() {
    def goConfig = new BasicCruiseConfig()
    def repo = PackageRepositoryMother.create("repoid")
    goConfig.getPackageRepositories().add(repo)

    def options = new ConfigHelperOptions(goConfig, mock(PasswordDeserializer.class))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(packageMaterialHash('package-name'))
    def packageMaterialConfig = (PackageMaterialConfig) MaterialsRepresenter.fromJSON(jsonReader, options)

    assertEquals('package-name', packageMaterialConfig.getPackageId())
    assertEquals(repo.findPackage('package-name'), packageMaterialConfig.getPackageDefinition())
  }

  @Test
  void "should set packageId during deserialisation if matching package definition is not present in config"() {
    def options = new ConfigHelperOptions(new BasicCruiseConfig(), mock(PasswordDeserializer.class))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(packageMaterialHash('package-name'))
    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, options)

    assertEquals("package-name", ((PackageMaterialConfig) deserializedObject).getPackageId())
    assertNull(((PackageMaterialConfig)deserializedObject).getPackageDefinition())
  }

  @Test
  void "should render errors"() {
    def package_config = new PackageMaterialConfig(new CaseInsensitiveString(''), '', null)
    def material_configs = new MaterialConfigs(package_config);
    material_configs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))

    def actualJson = toObjectString({ MaterialsRepresenter.toJSON(it, material_configs.first()) })

    assertThatJson(actualJson).isEqualTo(expectedMaterialHashWithErrors)
  }


  def packageMaterialHash(packageId = "p-id") {
    return [
      type: 'package',
      attributes:
      [
        ref:
        packageId
      ]
    ]
  }


  def expectedMaterialHashWithErrors =
  [
    type: "package",
    attributes: [
      ref: ""
    ],
    errors: [
      ref: ["Please select a repository and package"]
    ]
  ]
}
