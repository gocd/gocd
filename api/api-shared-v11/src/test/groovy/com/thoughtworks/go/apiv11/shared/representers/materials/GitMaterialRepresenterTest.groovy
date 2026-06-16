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
package com.thoughtworks.go.apiv11.shared.representers.materials

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv11.admin.shared.representers.materials.MaterialsRepresenter
import com.thoughtworks.go.apiv11.admin.shared.representers.stages.ConfigHelperOptions
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.util.command.UrlArgument
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.config.CaseInsensitiveString.cis
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.mockito.Mockito.mock

class GitMaterialRepresenterTest implements MaterialRepresenterTrait {

  MaterialConfig existingMaterial() {
    MaterialConfigsMother.gitMaterialConfig()
  }

  ConfigHelperOptions getOptions() {
    return new ConfigHelperOptions(mock(BasicCruiseConfig.class), mock(PasswordDeserializer.class))
  }

  MaterialConfig existingMaterialWithErrors() {
    def gitConfig = git(new UrlArgument(''), null, null, '', '', true, null, false, '', cis('!nV@l!d'), false)
    def dupGitMaterial = git(new UrlArgument(''), null, null, '', '', true, null, false, '', cis('!nV@l!d'), false)
    def materialConfigs = new MaterialConfigs(gitConfig)
    materialConfigs.add(dupGitMaterial)

    materialConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))
    return materialConfigs.getFirst()
  }

  @Nested
  class Credentials {
    @Test
    void "should serialize material with credentials without name"() {
      def actualJson = toObjectString({
        MaterialsRepresenter.toJSON(it, git("http://user:pass@funk.com/blank"))
      })

      assertThatJson(actualJson).isEqualTo(gitMaterialHashWithCredentials)
    }

    @Test
    void "should serialize material with credentials with blank branch"() {
      def actualJson = toObjectString({
        MaterialsRepresenter.toJSON(it, git("http://user:pass@funk.com/blank", ""))
      })

      assertThatJson(actualJson).isEqualTo(gitMaterialHashWithCredentials)
    }

    @Test
    void "should deserialize material with credentials as attributes"() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        type      : 'git',
        attributes:
          [
            url     : "http://funk.com/blank",
            branch  : "master",
            username: "user",
            password: "password"
          ]
      ])

      def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
      def expected = git("http://funk.com/blank")
      expected.setUserName("user")
      expected.setPassword("password")

      assertEquals(expected.isAutoUpdate(), deserializedObject.isAutoUpdate())
      assertNull(deserializedObject.getName())
      assertEquals(expected, deserializedObject)
    }
  }

  @Test
  void "should deserialize material without name"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : 'git',
      attributes:
        [
          url               : "http://funk.com/blank",
          branch            : "master",
          auto_update       : true,
          name              : null,
          username          : 'user',
          encrypted_password: new GoCipher().encrypt("password")
        ]
    ])

    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
    def expected = git("http://funk.com/blank")
    expected.setUserName("user")
    expected.setPassword("password")

    assertEquals(expected.isAutoUpdate(), deserializedObject.isAutoUpdate())
    assertNull(deserializedObject.getName())
    assertEquals(expected, deserializedObject)
  }

  @Test
  void "should deserialize material without invert_filter"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : 'git',
      attributes:
        [
          url               : "http://funk.com/blank",
          branch            : "master",
          auto_update       : true,
          name              : null,
          invert_filter     : null,
          username          : 'user',
          encrypted_password: new GoCipher().encrypt("password")
        ]
    ])
    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
    def expected = git("http://funk.com/blank")
    expected.setUserName("user")
    expected.setPassword("password")

    assertEquals(expected.isInvertFilter(), deserializedObject.isInvertFilter())
    assertEquals(expected, deserializedObject)
  }

  @Test
  void "should deserialize material with invert_filter"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : 'git',
      attributes:
        [
          url               : "http://funk.com/blank",
          branch            : "master",
          auto_update       : true,
          name              : null,
          invert_filter     : true,
          username          : 'user',
          encrypted_password: new GoCipher().encrypt("password")
        ]
    ])
    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
    def expected = git("http://funk.com/blank")
    expected.setUserName("user")
    expected.setPassword("password")
    expected.setInvertFilter(true)

    assertEquals(expected.isInvertFilter(), deserializedObject.isInvertFilter())
    assertEquals(expected, deserializedObject)
  }

  @Test
  void "should deserialize material with blank branch"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : 'git',
      attributes:
        [
          url               : "http://funk.com/blank",
          branch            : "",
          auto_update       : true,
          name              : null,
          username          : 'user',
          encrypted_password: new GoCipher().encrypt("password")
        ]
    ])
    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
    assertEquals("master", ((GitMaterialConfig) deserializedObject).getBranch().toString())
  }

  @Test
  void "should deserialize material with null url"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : 'git',
      attributes:
        [
          url               : null,
          branch            : "master",
          auto_update       : true,
          name              : null,
          username          : 'user',
          encrypted_password: new GoCipher().encrypt("password")
        ]
    ])
    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
    assertThat(((GitMaterialConfig) deserializedObject).getUrl()).isNull()
  }

  @Test
  void "should deserialize material without url attribute"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : 'git',
      attributes:
        [
          branch            : "master",
          auto_update       : true,
          name              : null,
          username          : 'user',
          encrypted_password: new GoCipher().encrypt("password")
        ]
    ])
    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
    assertThat(((GitMaterialConfig) deserializedObject).getUrl()).isNull()
  }

  def materialHash =
    [
      type      : 'git',
      attributes: [
        url             : "http://funk.com/blank",
        username        : "user",
        destination     : "destination",
        filter          : [
          ignore: ['**/*.html', '**/foobar/']
        ],
        invert_filter   : false,
        branch          : 'branch',
        submodule_folder: 'sub_module_folder',
        shallow_clone   : true,
        name            : 'AwesomeGitMaterial',
        auto_update     : false,
      ]
    ]

  def gitMaterialHashWithCredentials =
    [
      type      : 'git',
      attributes: [
        url             : "http://user:******@funk.com/blank",
        destination     : null,
        filter          : null,
        invert_filter   : false,
        name            : null,
        auto_update     : true,
        branch          : "master",
        submodule_folder: null,
        shallow_clone   : false
      ]
    ]

  def expectedMaterialHashWithErrors =
    [
      type      : "git",
      attributes: [
        url             : "",
        destination     : "",
        filter          : null,
        invert_filter   : false,
        name            : "!nV@l!d",
        auto_update     : true,
        branch          : "master",
        submodule_folder: "",
        shallow_clone   : false
      ],
      errors    : [
        name       : ["You have defined multiple materials called '!nV@l!d'. Material names are case-insensitive and must be unique. Note that for dependency materials the default materialName is the name of the upstream pipeline. You can override this by setting the materialName explicitly for the upstream pipeline.", "Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
        destination: ["Destination directory is required when a pipeline has multiple SCM materials."],
        url        : ["URL cannot be blank"]
      ]
    ]
}
