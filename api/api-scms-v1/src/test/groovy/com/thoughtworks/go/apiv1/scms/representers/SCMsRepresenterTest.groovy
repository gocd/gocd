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
package com.thoughtworks.go.apiv1.scms.representers

import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.scm.SCM
import com.thoughtworks.go.domain.scm.SCMMother
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class SCMsRepresenterTest {
  @Test
  void 'should serialize list of scms to json'() {
    SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
      ConfigurationPropertyMother.create("key1", false, "value1"),
      ConfigurationPropertyMother.create("key2", true, "secret"),
    ))

    String actualJson = toObjectString({ SCMsRepresenter.toJSON(it, Arrays.asList(scm)) })

    assertThatJson(actualJson).isEqualTo([
      "_links"   : [
        "doc" : [
          "href": apiDocsUrl('#scms')
        ],
        "find": [
          "href": "http://test.host/go/api/admin/scms/:material_name"
        ],
        "self": [
          "href": "http://test.host/go/api/admin/scms"
        ]
      ],
      "_embedded": [
        "scms": [
          [
            "_links"         : [
              "doc" : [
                "href": apiDocsUrl('#scms')
              ],
              "self": [
                "href": "http://test.host/go/api/admin/scms/foobar"
              ],
              "find": [
                "href": "http://test.host/go/api/admin/scms/:material_name"
              ]
            ],
            "id"             : "1",
            "name"           : "foobar",
            "auto_update"    : true,
            "plugin_metadata": [
              "id"     : "plugin1",
              "version": "v1.0"
            ],
            "configuration"  : [
              [
                "key"  : "key1",
                "value": "value1"
              ],
              [
                "key"            : "key2",
                "encrypted_value": new GoCipher().encrypt("secret")
              ]
            ]
          ]
        ]
      ]
    ])
  }
}