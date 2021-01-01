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

import com.thoughtworks.go.config.EnvironmentAgentConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class AgentRepresenterTest {

  @Test
  void 'should serialize to JSON'() {
    EnvironmentAgentConfig config = new EnvironmentAgentConfig("agent1")

    def json = toObjectString({ AgentRepresenter.toJSON(it, config) })

    assertThatJson(json).isEqualTo([
      "_links": [
        "doc" : [
          "href": apiDocsUrl("#agents")
        ],
        "find": [
          "href": "/api/agents/:uuid"
        ],
        "self": [
          "href": "http://test.host/go/api/agents/agent1"
        ]
      ],
      "uuid"  : "agent1"
    ])
  }
}
