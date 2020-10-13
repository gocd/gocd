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
package com.thoughtworks.go.apiv12.shared.representers.configorigin

import com.thoughtworks.go.apiv12.admin.shared.representers.configorigin.ConfigXmlOriginRepresenter
import com.thoughtworks.go.config.remote.FileConfigOrigin
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ConfigXmlOriginRepresenterTest {

  @Test
  void  'should render local config origin'() {
    def actualJson = toObjectString({ ConfigXmlOriginRepresenter.toJSON(it, getConfigXmlOrigin()) })
    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  def getConfigXmlOrigin() {
    return new FileConfigOrigin()
  }

  def expectedJson =
  [
    type: 'gocd',
    _links: [
      self: [
        href: 'http://test.host/go/admin/config_xml'
      ],
      doc: [
        href: apiDocsUrl('#get-configuration')
      ]
    ]
  ]
}
