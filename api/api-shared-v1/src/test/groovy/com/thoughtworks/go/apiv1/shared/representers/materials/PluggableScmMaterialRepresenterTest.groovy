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
package com.thoughtworks.go.apiv1.shared.representers.materials

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig
import com.thoughtworks.go.domain.scm.SCMMother
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.*
import static org.mockito.Mockito.mock

class PluggableScmMaterialRepresenterTest {

  def getOptions() {
    return new ConfigHelperOptions(mock(BasicCruiseConfig.class), mock(PasswordDeserializer.class))
  }

  void "should represent a pluggable scm material" () {
    def pluggableSCMMaterial = MaterialConfigsMother.pluggableSCMMaterialConfig()
    def actualJson = toObjectString({MaterialRepresenter.toJSON(it, pluggableSCMMaterial)})

    assertThatJson(actualJson).isEqualTo(pluggableScmMaterialHash)
  }

  @Test
  void "should deserialize"() {
    def scm = SCMMother.create("scm-id")
    def cruiseConfig = new BasicCruiseConfig()
    cruiseConfig.getSCMs().add(scm)

    def options = new ConfigHelperOptions(cruiseConfig, mock(PasswordDeserializer.class))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(pluggableScmMaterialHash)
    def pluggableScmMaterialConfig = (PluggableSCMMaterialConfig) MaterialRepresenter.fromJSON(jsonReader, options)

    assertEquals("scm-id", pluggableScmMaterialConfig.getScmId())
    assertEquals(scm, pluggableScmMaterialConfig.getSCMConfig())
    assertEquals("des-folder", pluggableScmMaterialConfig.getFolder())
    assertEquals("**/*.html,**/foobar/", pluggableScmMaterialConfig.filter().getStringForDisplay())
  }

  @Test
  void "should set scmId during deserialisation if matching package definition is not present in config" () {
    def options = new ConfigHelperOptions(new BasicCruiseConfig(), mock(PasswordDeserializer.class))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(pluggableScmMaterialHash)
    def pluggableScmMaterialConfig = (PluggableSCMMaterialConfig) MaterialRepresenter.fromJSON(jsonReader, options)

    assertEquals("scm-id", pluggableScmMaterialConfig.getScmId())
    assertNull(pluggableScmMaterialConfig.getSCMConfig())
  }

  @Test
  void "should deserialize pluggable scm material with nulls"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : "plugin",
      attributes:
        [
          ref        : "23a28171-3d5a-4912-9f36-d4e1536281b0",
          filter     : null,
          destination: null
        ]
    ])

    def options = new ConfigHelperOptions(new BasicCruiseConfig(), mock(PasswordDeserializer.class))
    def pluggableScmMaterialConfig = (PluggableSCMMaterialConfig) MaterialRepresenter.fromJSON(jsonReader, options)

    assertNull(pluggableScmMaterialConfig.getName())
    assertEquals("23a28171-3d5a-4912-9f36-d4e1536281b0", pluggableScmMaterialConfig.getScmId())
    assertNull(pluggableScmMaterialConfig.getFolder())
    assertTrue(pluggableScmMaterialConfig.filter().isEmpty())
  }

  @Test
  void "should render errors"() {
    def pluggableScmMaterial = new PluggableSCMMaterialConfig(new CaseInsensitiveString(''), null, '/dest', null, false)
    def materialConfigs = new MaterialConfigs(pluggableScmMaterial);
    materialConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))

    def actualJson = toObjectString({ MaterialRepresenter.toJSON(it, materialConfigs.first()) })
    assertThatJson(actualJson).isEqualTo(expectedMaterialHashWithErrors)
  }

  def pluggableScmMaterialHash =
  [
    type: 'plugin',
    attributes: [
      ref: "scm-id",
      filter: [
        ignore: ["**/*.html", "**/foobar/"]
      ],
      destination: 'des-folder'
    ]
  ]

  def expectedMaterialHashWithErrors =
  [
    type: "plugin",
    attributes: [
      ref: null,
      filter: null,
      destination: "/dest"
    ],
    errors: [
      destination: ["Dest folder '/dest' is not valid. It must be a sub-directory of the working folder."],
      ref: ["Please select a SCM"]
    ]
  ]

}
