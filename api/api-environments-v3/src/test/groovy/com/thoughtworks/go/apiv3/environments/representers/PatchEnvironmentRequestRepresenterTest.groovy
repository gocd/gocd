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
package com.thoughtworks.go.apiv3.environments.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class PatchEnvironmentRequestRepresenterTest {

  @Test
  void 'should de-serialize a patch request from JSON'() {
    def encryptedText = new GoCipher().encrypt("confidential")
    def patchRequestObject = [
      "pipelines"            : [
        "add"   : ["up42"],
        "remove": ["sample"]
      ],
      "environment_variables": [
        "add"   : [
          [
            "name" : "GO_SERVER_URL",
            "value": "https://ci.example.com/go"
          ],
          [
            "name"  : "GO_NO_SERVER_URL",
            "value" : "https://ci.example.com/go",
            "secure": true
          ],
          [
            "name"  : "Secured",
            "encrypted_value" : encryptedText,
            "secure": true
          ]
        ],
        "remove": ["URL"]
      ]
    ]

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(patchRequestObject)
    def patchEnvironmentRequest = PatchEnvironmentRequestRepresenter.fromJSON(jsonReader)

    assertThat(patchEnvironmentRequest.getPipelineToAdd().size(), is(1))
    assertThat(patchEnvironmentRequest.getPipelineToRemove().size(), is(1))
    assertThat(patchEnvironmentRequest.getEnvironmentVariablesToAdd().size(), is(3))
    assertThat(patchEnvironmentRequest.getEnvironmentVariablesToRemove().size(), is(1))

    assertThat(patchEnvironmentRequest.getPipelineToAdd().first(), equalTo("up42"))
    assertThat(patchEnvironmentRequest.getPipelineToRemove().first(), equalTo("sample"))

    def secureEnvVariable = patchEnvironmentRequest.getEnvironmentVariablesToAdd().get(1)
    assertThat(secureEnvVariable.getName(), equalTo("GO_NO_SERVER_URL"))
    assertThat(secureEnvVariable.getValue(), equalTo("https://ci.example.com/go"))
    assertThat(secureEnvVariable.isSecure(), equalTo(true))

    def encryptedEnvVariable = patchEnvironmentRequest.getEnvironmentVariablesToAdd().last()
    assertThat(encryptedEnvVariable.getName(), equalTo("Secured"))
    assertThat(encryptedEnvVariable.getValue(), equalTo("confidential"))
    assertThat(encryptedEnvVariable.isSecure(), equalTo(true))
  }
}