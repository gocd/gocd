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

package com.thoughtworks.go.apiv8.admin.templateconfig.representers

import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate
import static com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplateWithParams
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ParametersRepresenterTest {
  @Test
  void 'should convert into json'() {
    def templateName = "template-name"
    def template = createTemplateWithParams(templateName, "param1", "params2")

    def actualJson = toObjectString({ ParametersRepresenter.toJSON(it, templateName, template.referredParams()) })

    def expectedJson = [
      "_links"    : [
        "self": [
          "href": "http://test.host/go/api/admin/templates/template-name"
        ],
        "doc" : [
          "href": apiDocsUrl("#template-config")
        ],
        "find": [
          "href": "http://test.host/go/api/admin/templates/:template_name"
        ]
      ],
      "name"      : "template-name",
      "parameters": [
        "params2",
        "param1"
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should render empty json if no parameters given'() {
    def templateName = "template-name"
    def template = createTemplate(templateName)

    def actualJson = toObjectString({ ParametersRepresenter.toJSON(it, templateName, template.referredParams()) })

    def expectedJson = [
      "_links"    : [
        "self": [
          "href": "http://test.host/go/api/admin/templates/template-name"
        ],
        "doc" : [
          "href": apiDocsUrl("#template-config")
        ],
        "find": [
          "href": "http://test.host/go/api/admin/templates/:template_name"
        ]
      ],
      "name"      : "template-name",
      "parameters": []
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
