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
package com.thoughtworks.go.apiv11.shared.representers.materials

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv11.admin.shared.representers.materials.MaterialsRepresenter
import com.thoughtworks.go.apiv11.admin.shared.representers.stages.ConfigHelperOptions
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import static com.thoughtworks.go.helper.MaterialConfigsMother.hg
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.util.command.HgUrlArgument
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.mockito.Mockito.mock

class HgMaterialRepresenterTest implements MaterialRepresenterTrait {

  static def existingMaterial() {
    return MaterialConfigsMother.hgMaterialConfigFull("http://domain/path")
  }

  def getOptions() {
    return new ConfigHelperOptions(mock(BasicCruiseConfig.class), new PasswordDeserializer())
  }

  def existingMaterialWithErrors() {
    def hgConfig = hg(new HgUrlArgument(''), null, null, null, true, null, false, '/dest/', new CaseInsensitiveString('!nV@l!d'))
    def materialConfigs = new MaterialConfigs(hgConfig);
    materialConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), new PipelineConfig()))
    return materialConfigs.get(0)
  }


  @Nested
  class Credentials {
    @Test
    void "should deserialize material with credentials in attributes"() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        type      : 'hg',
        attributes:
          [
            url     : "http://funk.com/blank",
            branch  : "master",
            username: "user",
            password: "password"
          ]
      ])

      def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
      def expected = hg("http://funk.com/blank", null)
      expected.setUserName("user")
      expected.setPassword("password")
      expected.setBranchAttribute("master")

      assertEquals(expected.isAutoUpdate(), deserializedObject.isAutoUpdate())
      assertNull(deserializedObject.getName())
      assertEquals(expected, deserializedObject)
    }

    @Test
    void "should deserialize material with encrypted password in attributes"() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        type      : 'hg',
        attributes:
          [
            url               : "http://funk.com/blank",
            branch            : "master",
            username          : "user",
            encrypted_password: new GoCipher().encrypt("password")
          ]
      ])

      def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())
      def expected = hg("http://funk.com/blank", null)
      expected.setUserName("user")
      expected.setPassword("password")
      expected.setBranchAttribute("master")

      assertEquals(expected.isAutoUpdate(), deserializedObject.isAutoUpdate())
      assertNull(deserializedObject.getName())
      assertEquals(expected, deserializedObject)
    }

    @Test
    void "should add error if failed to deserialize encrypted password"() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        type      : 'hg',
        attributes:
          [
            url               : "http://funk.com/blank",
            branch            : "master",
            username          : "user",
            encrypted_password: "not-decryptable-by-gocd"
          ]
      ])

      def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())

      assertThat(deserializedObject.errors().on("encryptedPassword"))
        .isEqualTo("Encrypted value for password is invalid. This usually happens when the cipher text is invalid.")
    }
  }

  @Test
  void "should deserialize material with null url"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : 'hg',
      attributes:
        [
          url     : null,
          branch  : "master",
          username: "user",
          password: "password"
        ]
    ])

    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())

    assertThat(((HgMaterialConfig) deserializedObject).getUrl()).isNull()
  }

  @Test
  void "should deserialize material without url attribute"() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      type      : 'hg',
      attributes:
        [
          branch  : "master",
          username: "user",
          password: "password"
        ]
    ])

    def deserializedObject = MaterialsRepresenter.fromJSON(jsonReader, getOptions())

    assertThat(((HgMaterialConfig) deserializedObject).getUrl()).isNull()
  }

  def materialHash =
    [
      type      : 'hg',
      attributes: [
        url          : "http://domain/path",
        destination  : "dest-folder",
        filter       : [
          ignore: ['**/*.html', '**/foobar/']
        ],
        invert_filter: false,
        name         : "hg-material",
        auto_update  : true
      ]
    ]

  def expectedMaterialHashWithErrors =
    [
      type      : "hg",
      attributes: [
        url          : "",
        destination  : "/dest/",
        filter       : null,
        invert_filter: false,
        name         : "!nV@l!d",
        auto_update  : true
      ],
      errors    : [
        name       : ["Invalid material name '!nV@l!d'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."],
        destination: ["Dest folder '/dest/' is not valid. It must be a sub-directory of the working folder."],
        url        : ["URL cannot be blank"]
      ]
    ]
}
