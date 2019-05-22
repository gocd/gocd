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

package com.thoughtworks.go.apiv1.materials.representers

import com.thoughtworks.go.api.base.JsonUtils
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class MaterialConfigsRepresenterTest {

  @Test
  void "should serialize MaterialConfigs"() {
    def gitMaterialConfig = new GitMaterialConfig()
    gitMaterialConfig.url = "http://example.com/git-repo"
    gitMaterialConfig.branch = "master"
    gitMaterialConfig.shallowClone = true

    def hgMaterialConfig = new HgMaterialConfig()
    hgMaterialConfig.url = "http://example.com/hg-repo"
    hgMaterialConfig.branchAttribute = "test"

    def materialConfigs = new MaterialConfigs(gitMaterialConfig, hgMaterialConfig)

    def actualJSON = JsonUtils.toObjectString({ MaterialConfigsRepresenter.toJSON(it, materialConfigs) })

    assertThatJson(actualJSON).isEqualTo([
      _links: [
        doc : [href: apiDocsUrl('#materials')],
        self: [href: 'http://test.host/go/api/config/materials']
      ],
      _embedded: [
        materials: [
          [
            fingerprint: materialConfigs[0].fingerprint,
            type: materialConfigs[0].typeForDisplay,
            description: materialConfigs[0].longDescription
          ],
          [
            fingerprint: materialConfigs[1].getFingerprint(),
            type: materialConfigs[1].typeForDisplay,
            description: materialConfigs[1].longDescription
          ],
        ]
      ]
    ])

  }
}
