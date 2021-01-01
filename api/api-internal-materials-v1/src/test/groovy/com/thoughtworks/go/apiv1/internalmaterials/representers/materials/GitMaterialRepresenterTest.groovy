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
package com.thoughtworks.go.apiv1.internalmaterials.representers.materials


import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class GitMaterialRepresenterTest implements MaterialRepresenterTrait<GitMaterialConfig> {

  GitMaterialConfig existingMaterial() {
    return MaterialConfigsMother.gitMaterialConfig()
  }

  @Test
  void "should serialize material without name"() {
    def gitMaterialConfig = git("http://funk.com/blank")
    def actualJson = toObjectString(MaterialsRepresenter.toJSON(gitMaterialConfig))

    assertThatJson(actualJson).isEqualTo([
      type       : 'git',
      fingerprint: gitMaterialConfig.fingerprint,
      attributes : [
        url        : "http://funk.com/blank",
        name       : null,
        auto_update: true,
        branch     : "master",
      ]
    ])
  }

  @Test
  void "should serialize material with blank branch"() {
    def gitMaterialConfig = git("http://funk.com/blank", "")
    def actualJson = toObjectString(MaterialsRepresenter.toJSON(gitMaterialConfig))

    assertThatJson(actualJson).isEqualTo([
      type       : 'git',
      fingerprint: gitMaterialConfig.fingerprint,
      attributes : [
        url        : "http://funk.com/blank",
        name       : null,
        auto_update: true,
        branch     : "master",
      ]
    ])
  }

  def materialHash() {
    [
      type       : 'git',
      fingerprint: existingMaterial().fingerprint,
      attributes : [
        url        : "http://user:******@funk.com/blank",
        branch     : 'branch',
        name       : 'AwesomeGitMaterial',
        auto_update: false,
      ]
    ]
  }

}
