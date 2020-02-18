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
package com.thoughtworks.go.apiv10.shared.representers.stages

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.JobRepresenter
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.StageRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.helper.JobConfigMother
import com.thoughtworks.go.helper.StageConfigMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class StageRepresenterTest  {

  @Nested
  class Serialize {

    @Test
    void 'should render stage with hal representation'() {
      def actualJson = toObjectString({ StageRepresenter.toJSON(it, getStageConfig()) })

      assertThatJson(actualJson).isEqualTo(stageHash)
    }

    def stageHash =
    [
      name:                    'stage1',
      fetch_materials:         true,
      clean_working_directory: false,
      never_cleanup_artifacts: false,
      approval:                [
        type                 : 'success',
        allow_only_on_success: false,
        authorization        : [
          roles: [],
          users: []
        ]
      ],
      environment_variables:   [
        [
          secure:          true,
          name:            'MULTIPLE_LINES',
          encrypted_value: getStageConfig().variables.get(0).getEncryptedValue()
        ],
        [
          secure: false,
          name:   'COMPLEX',
          value:  'This has very <complex> data'
        ]
      ],
      jobs: getStageConfig().getJobs().collect { eachJob -> toObject({JobRepresenter.toJSON(it, eachJob)}) }
    ]
  }

  @Nested
  class Deserialize {

    @Test
    void 'should convert basic hash to StageConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        name                   : 'stage1',
        fetch_materials        : true,
        clean_working_directory: false,
        never_cleanup_artifacts: false
      ])
      def stageConfig = StageRepresenter.fromJSON(jsonReader)

      assertEquals('stage1', stageConfig.name().toString())
      assertTrue(stageConfig.isFetchMaterials())
    }

    @Test
    void 'should convert basic hash with Approval to StageConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        approval:
          [
            type: "manual",
            authorization:
            [
              roles:
              ["role1", "role2"],
              users:
              ["user1", "user2"]
            ]
          ]
      ])
      def stageConfig = StageRepresenter.fromJSON(jsonReader)

      def authConfig = new AuthConfig(new AdminRole(new CaseInsensitiveString("role1")), new AdminRole(new CaseInsensitiveString("role2")),
      new AdminUser(new CaseInsensitiveString("user1")), new AdminUser(new CaseInsensitiveString("user2")))
      def approval = new Approval(authConfig)

      assertEquals(approval, stageConfig.getApproval())
    }

    @Test
    void 'should convert basic hash with Environment variables to StageConfig'() {
      def environmentVariables = [
        environment_variables:
        [
          [
            secure: true,
            name: 'MULTIPLE_LINES',
            encrypted_value:
            getStageConfig().variables.get(0).getEncryptedValue()
          ],
          [
            secure: false,
            name: 'COMPLEX',
            value: 'This has very <complex> data'
          ]
        ]
      ]

      def jsonReader = GsonTransformer.instance.jsonReaderFrom(environmentVariables)
      def stageConfig = StageRepresenter.fromJSON(jsonReader)

      def listOfEnvVars = stageConfig.getVariables().name
      assertEquals("MULTIPLE_LINES", listOfEnvVars.get(0))
      assertEquals("COMPLEX", listOfEnvVars.get(1))
    }

    @Test
    void 'should convert basic hash with Jobs to StageConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        jobs: [jobHash]
      ])

      def stageConfig = StageRepresenter.fromJSON(jsonReader)
      assertEquals(JobConfigMother.jobConfig(), stageConfig.getJobs().first())
    }

    def jobHash =
    [
      name:                  'defaultJob',
      run_on_all_agents:     false,
      run_instance_count:    3,
      timeout:               100,
      environment_variables: [
        [secure: true, name: 'MULTIPLE_LINES', encrypted_value: JobConfigMother.jobConfig().variables.get(0).getEncryptedValue()],
        [secure: false, name: 'COMPLEX', value: 'This has very <complex> data']
      ],
      resources:             ['Linux', 'Java'],
      tasks:                 [
        [type: 'ant', attributes: [working_directory: 'working-directory', build_file: 'build-file', target: 'target']]
      ],
      tabs:                  [
        [name: 'coverage', path: 'Jcoverage/index.html'],
        [name: 'something', path: 'something/path.html']
      ],
      artifacts:             [
        [source: 'target/dist.jar', destination: 'pkg', type: 'build'],
        [source: 'target/reports/**/*Test.xml', destination: 'reports', type: 'test']
      ],

      properties:            [[name: 'coverage.class', source: 'target/emma/coverage.xml', xpath: "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"]]
    ]

  }

  @Test
  void 'should render errors'() {
    def stageConfig = StageConfigMother.stageConfigWithEnvironmentVariable("stage#1")
    stageConfig.getJobs().get(0).setTasks(new Tasks(new FetchTask(new CaseInsensitiveString(""), new CaseInsensitiveString(""), new CaseInsensitiveString(""), null, null)))
    stageConfig.addError('name', 'Invalid stage name')

    def actualJson = toObjectString({ StageRepresenter.toJSON(it, stageConfig) })
    assertThatJson(actualJson).isEqualTo(stageHashWithErrors(stageConfig))
  }


  static def stageHashWithErrors(stageConfig) {
    return [
      name: "stage#1",
      fetch_materials: true,
      clean_working_directory: false,
      never_cleanup_artifacts: false,
      approval:
      [
        type                 : "success",
        allow_only_on_success: false,
        authorization        :
        [
          roles:
          [],
          users:
          []
        ]
      ],
      environment_variables:
      [
        [
          secure: true,
          name: "MULTIPLE_LINES",
          encrypted_value:
          stageConfig.variables.get(0).getEncryptedValue()
        ],
        [
          secure: false,
          name: "COMPLEX",
          value: "This has very <complex> data"
        ]
      ],
      jobs:
      stageConfig.getJobs().collect {eachJob -> toObject({ JobRepresenter.toJSON(it, eachJob)})},
      errors:
      [
        name:
        ["Invalid stage name"]
      ]
    ]
  }

  static def getStageConfig() {
    return StageConfigMother.stageConfigWithEnvironmentVariable("stage1")
  }
}
