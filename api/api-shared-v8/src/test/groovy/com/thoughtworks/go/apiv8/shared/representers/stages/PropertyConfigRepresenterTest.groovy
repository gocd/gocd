/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv8.shared.representers.stages

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv8.admin.shared.representers.stages.PropertyConfigRepresenter
import com.thoughtworks.go.config.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PropertyConfigRepresenterTest  {
  private ArtifactPropertyConfig artifactPropertyConfig

  def propertyHash =
  [
    name: 'foo',
    source: 'target/emma/coverage.xml',
    xpath: 'substring-before(//report/data/all/coverage[starts-with(@type,class)]/@value, %)'
  ]

  def propertyHashWithErrors =
    [
      name: '',
      source: 'target/emma/coverage.xml',
      xpath: 'substring-before(//report/data/all/coverage[starts-with(@type,class)]/@value, %)',
      errors:      [
        name:      ["Invalid property name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."]
      ]
    ]

  @BeforeEach
  void setUp() {
    artifactPropertyConfig = new ArtifactPropertyConfig("foo", "target/emma/coverage.xml", "substring-before(//report/data/all/coverage[starts-with(@type,class)]/@value, %)")
  }

  @Test
  void 'should serialize property'() {
    def actualJson = toObjectString({ PropertyConfigRepresenter.toJSON(it, artifactPropertyConfig) })

    assertThatJson(actualJson).isEqualTo(propertyHash)
  }

  @Test
  void 'should deserialize artifact property'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(propertyHash)
    def artifactProperty = PropertyConfigRepresenter.fromJSON(jsonReader)

    assertThatJson(artifactProperty).isEqualTo(artifactPropertyConfig)
  }

  @Test
  void 'should map errors'() {
    artifactPropertyConfig.setName('')
    artifactPropertyConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "g", new PipelineConfig(), new StageConfig(), new JobConfig()))
    def actualJson = toObjectString({ PropertyConfigRepresenter.toJSON(it, artifactPropertyConfig) })

    assertThatJson(actualJson).isEqualTo(propertyHashWithErrors)
  }
}
