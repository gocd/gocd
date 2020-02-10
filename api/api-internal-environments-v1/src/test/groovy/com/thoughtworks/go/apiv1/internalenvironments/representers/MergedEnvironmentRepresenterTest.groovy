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
package com.thoughtworks.go.apiv1.internalenvironments.representers

import com.thoughtworks.go.apiv1.internalenvironments.representers.configorigin.ConfigRepoOriginRepresenter
import com.thoughtworks.go.apiv1.internalenvironments.representers.configorigin.ConfigXmlOriginRepresenter
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.helper.EnvironmentConfigMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class MergedEnvironmentRepresenterTest {
  @Test
  void 'should render merged environment with hal representation'() {
    def environmentName = "env"
    def env = EnvironmentConfigMother.environment(environmentName)
    def remoteEnv = EnvironmentConfigMother.remote(environmentName)
    def mergeEnvironmentConfig = new MergeEnvironmentConfig(env, remoteEnv)

    def actualJson = toObjectString({
      MergedEnvironmentRepresenter.toJSON(it, mergeEnvironmentConfig)
    })

    def agentsJSON = mergeEnvironmentConfig.agents.collect { agent ->
      toObject({
        EnvironmentAgentRepresenter.toJSON(it, agent, mergeEnvironmentConfig)
      })
    }

    def pipelinesJSON = mergeEnvironmentConfig.pipelines.collect { pipeline ->
      toObject({
        EnvironmentPipelineRepresenter.toJSON(it, pipeline, mergeEnvironmentConfig)
      })
    }

    def environmentVariablesJSON = mergeEnvironmentConfig.variables.collect { envVar ->
      toObject({
        EnvironmentEnvironmentVariableRepresenter.toJSON(it, envVar, mergeEnvironmentConfig)
      })
    }

    def expected = [
      "name"                 : environmentName,
      origins                : [
        toObject({ ConfigXmlOriginRepresenter.toJSON(it, null) }),
        toObject({ ConfigRepoOriginRepresenter.toJSON(it, remoteEnv.getOrigin() as RepoConfigOrigin) })
      ],
      "agents"               : agentsJSON,
      "environment_variables": environmentVariablesJSON,
      "pipelines"            : pipelinesJSON
    ]

    assertThatJson(actualJson).isEqualTo(expected)
  }
}
