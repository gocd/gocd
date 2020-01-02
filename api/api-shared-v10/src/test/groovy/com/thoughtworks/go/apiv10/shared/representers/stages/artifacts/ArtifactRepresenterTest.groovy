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
package com.thoughtworks.go.apiv10.shared.representers.stages.artifacts

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.artifacts.ArtifactRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.validation.FilePathTypeValidator
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ArtifactRepresenterTest {

  def externalArtifactHash =
    [
      type           : "external",
      artifact_id    : "docker-image-stable",
      store_id       : "dockerhub",
      "configuration": [
        [
          key  : "image",
          value: "alpine"
        ]
      ]
    ]

  def testArtifactWithErrors =
  [source: null, destination: '../foo', type: 'test',
    errors: [
      destination: ['Invalid destination path. Destination path should match the pattern (([.]\\/)?[.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ])'],
      source:      ["Job 'null' has an artifact with an empty source"]
    ]
  ]

  def buildArtifactHash =
  [
    source:      'target/dist.jar',
    destination: 'pkg',
    type:        'build'
  ]

  def buildArtifactHashWithErrors =
  [
    source:      null,
    destination: '../foo',
    type:        'build',
    errors:      [
      source:      ["Job 'null' has an artifact with an empty source"],
      destination: ['Invalid destination path. Destination path should match the pattern '+ FilePathTypeValidator.PATH_PATTERN]
    ]
  ]

  def testArtifactHash =
  [
    source:      'target/reports/**/*Test.xml',
    destination: 'reports',
    type:        'test'
  ]

  def testArtifactHashWithErrors =
  [
    source:      null,
    destination: '../foo',
    type:        'test',
    errors:      [
      source:      ["Job 'null' has an artifact with an empty source"],
      destination: ['Invalid destination path. Destination path should match the pattern '+ FilePathTypeValidator.PATH_PATTERN]
    ]
  ]

  @Test
  void 'should serialize build artifact'() {
    def buildArtifactConfig = new BuildArtifactConfig('target/dist.jar', 'pkg')

    def actualJson = toObjectString({ ArtifactRepresenter.toJSON(it, buildArtifactConfig)})

    assertThatJson(actualJson).isEqualTo(buildArtifactHash)
  }

  @Test
  void 'should serialize test artifact'() {
    def config = new TestArtifactConfig()
    config.setSource('target/reports/**/*Test.xml')
    config.setDestination('reports')
    def actualJson = toObjectString({ArtifactRepresenter.toJSON(it, config)})

    assertThatJson(actualJson).isEqualTo(testArtifactHash)
  }

  @Test
  void 'should serialize external artifact'() {
    def config = new PluggableArtifactConfig("docker-image-stable", "dockerhub", ConfigurationPropertyMother.create("image", false, "alpine"))

    def actualJson = toObjectString({ ArtifactRepresenter.toJSON(it, config) })

    assertThatJson(actualJson).isEqualTo(externalArtifactHash)
  }

  @Test
  void 'should deserialize test artifact'() {
    def expected = new TestArtifactConfig()
    expected.setSource('target/reports/**/*Test.xml')
    expected.setDestination('reports')

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(testArtifactHash)
    def actualArtifactConfig = ArtifactRepresenter.fromJSON(jsonReader)

    assertThatJson(actualArtifactConfig).isEqualTo(expected)
  }

  @Test
  void 'should deserialize external artifact'() {
    def config = new PluggableArtifactConfig("docker-image-stable", "dockerhub", ConfigurationPropertyMother.create("image", false, "alpine"))
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(externalArtifactHash)
    def actualArtifactConfig = ArtifactRepresenter.fromJSON(jsonReader)

    assertThatJson(actualArtifactConfig).isEqualTo(config)

  }

  @Test
  void 'should map errors'() {
    def plan = new TestArtifactConfig(null, '../foo')

    plan.validateTree(PipelineConfigSaveValidationContext.forChain(true, "g", new PipelineConfig(), new StageConfig(), new JobConfig()))
    def actualJson = toObjectString({ArtifactRepresenter.toJSON(it, plan)})

    assertThatJson(actualJson).isEqualTo(testArtifactWithErrors)
  }
}
