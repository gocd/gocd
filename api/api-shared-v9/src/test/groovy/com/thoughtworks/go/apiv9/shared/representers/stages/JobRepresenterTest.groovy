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

package com.thoughtworks.go.apiv9.shared.representers.stages

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv9.admin.shared.representers.stages.JobRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException
import com.thoughtworks.go.helper.JobConfigMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.*

class JobRepresenterTest {
  @Nested
  class Serialize {
    @Test
    void 'should render job with hal representation' () {
      def actualJson = toObjectString({JobRepresenter.toJSON(it, JobConfigMother.jobConfig())})

      assertThatJson(actualJson).isEqualTo(jobHash)
    }

    @Test
    void 'should serialize run_instance_count with value set to 10' () {
      def jobConfig = new JobConfig(new CaseInsensitiveString("job"))
      jobConfig.setRunInstanceCount(10)

      def actualJson = toObject({JobRepresenter.toJSON(it, jobConfig)})

      assertThatJson(actualJson['run_instance_count']).isEqualTo(10)
    }

    @Test
    void 'should serialize run_instance_count with value `all` when runOnAllAgents is true' () {
      def jobConfig =  new JobConfig(new CaseInsensitiveString("job"))
      jobConfig.setRunOnAllAgents(true)

      def actualJson = toObject({JobRepresenter.toJSON(it, jobConfig)})

      assertEquals(actualJson['run_instance_count'],"all")
    }

    @Test
    void 'should serialize run_instance_count with value `nil` when runOnAllAgents is false and eunInstanceCount is unset' () {
      def jobConfig =  new JobConfig(new CaseInsensitiveString("job"))
      def actualJson = toObject({JobRepresenter.toJSON(it, jobConfig)})

      assertThatJson(actualJson['run_instance_count']).isEqualTo(null)
    }

    @Test
    void 'should serialize timeout with value set to `10` to `10`' () {
      def jobConfig =  new JobConfig(new CaseInsensitiveString("job"))
      jobConfig.setTimeout('10')

      def actualJson = toObject({JobRepresenter.toJSON(it, jobConfig)})

      assertThatJson(actualJson['timeout']).isEqualTo(10)
    }

    @Test
    void 'should serialize timeout with value `0` to `never`' () {
      def jobConfig =  new JobConfig(new CaseInsensitiveString("job"))
      jobConfig.setTimeout('0')

      def actualJson = toObject({JobRepresenter.toJSON(it, jobConfig)})

      assertEquals(actualJson['timeout'],'never')
    }

    @Test
    void 'should serialize timeout with value `nil` to `default`' () {
      def jobConfig =  new JobConfig(new CaseInsensitiveString("job"))
      jobConfig.setTimeout(null)

      def actualJson = toObject({JobRepresenter.toJSON(it, jobConfig)})

      assertThatJson(actualJson['timeout']).isEqualTo(null)
    }

    @Test
    void 'should serialize job elastic_profile_id' () {
      def jobConfig =  new JobConfig(new CaseInsensitiveString("job"))
      jobConfig.setElasticProfileId('docker.unit-test')

      def actualJson = toObject({JobRepresenter.toJSON(it, jobConfig)})

      assertEquals('docker.unit-test', actualJson['elastic_profile_id'])

    }

    def jobHash =
    [
      name:                  'defaultJob',
      run_instance_count:    3,
      timeout:               100,
      environment_variables: [
        [ secure: true, name: 'MULTIPLE_LINES', encrypted_value: JobConfigMother.jobConfig().variables.get(0).encryptedValue ],
        [ secure: false, name: 'COMPLEX', value: 'This has very <complex> data' ]
      ],
      resources:             ["Linux" ,"Java"],
      tasks:                 [
        [ type: 'ant', attributes: [working_directory: 'working-directory', build_file: 'build-file', target: 'target', run_if: []] ]
      ],
      tabs:                  [
        [ name: 'coverage', path: 'Jcoverage/index.html' ],
        [ name: 'something', path: 'something/path.html' ]
      ],
      artifacts:             [
        [ source: 'target/dist.jar', destination: 'pkg', type: 'build' ],
        [ source: 'target/reports/**/*Test.xml', destination: 'reports', type: 'test' ]
      ],
    ]
  }

  @Nested
  class Deserialize {

    @Test
    void 'should convert basic hash to Job'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        name   : 'some-job',
        timeout: '100'
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)

      assertEquals('some-job', actualJobConfig.name().toString())
      assertEquals('100', actualJobConfig.getTimeout())
    }

    @Test
    void 'should convert attribute run_instance_count with value `all` to Job'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        run_instance_count: 'all'
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)

      assertTrue(actualJobConfig.isRunOnAllAgents())
      assertNull(actualJobConfig.getRunInstanceCount())
    }

    @Test
    void 'should convert attribute run_instance_count with value `nil` to Job'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        run_instance_count: null
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)

      assertFalse(actualJobConfig.isRunOnAllAgents())
      assertNull(actualJobConfig.getRunInstanceCount())
    }

    @Test
    void 'should convert attribute run_instance_count with integer value `10` to Job'() {
      def jsonReader1 = GsonTransformer.instance.jsonReaderFrom([
        run_instance_count: 10
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader1)

      assertFalse(actualJobConfig.isRunOnAllAgents())
      assertEquals('10', actualJobConfig.getRunInstanceCount())

      def jsonReader2 = GsonTransformer.instance.jsonReaderFrom([
        run_instance_count: '10'
      ])
      def jobConfig = JobRepresenter.fromJSON(jsonReader2)
      assertFalse(jobConfig.isRunOnAllAgents())
      assertEquals('10', jobConfig.getRunInstanceCount())
    }

    @Test
    void 'should convert attribute timeout with value `never` to Job with timeout `0`'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        timeout: 'never'
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      assertEquals(actualJobConfig.getTimeout(), '0')
    }

    @Test
    void 'should convert attribute timeout with value `nil` to Job with timeout `nil`'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        timeout: null
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      assertNull(actualJobConfig.getTimeout())
    }

    @Test
    void 'should convert attribute timeout with integer value `10` to Job with timeout `10`'() {
      def jsonReader1 = GsonTransformer.instance.jsonReaderFrom([
        timeout: '10'
      ])
      def actualJobConfig1 = JobRepresenter.fromJSON(jsonReader1)
      assertEquals('10', actualJobConfig1.getTimeout())

      def jsonReader2 = GsonTransformer.instance.jsonReaderFrom([
        timeout: 20
      ])
      def actualJobConfig2 = JobRepresenter.fromJSON(jsonReader2)
      assertEquals('20', actualJobConfig2.getTimeout())
    }

    @Test
    void 'should convert basic hash with environment variable to Job'() {
      def environmentVariables = [
        [
          name: 'USERNAME',
          value: 'bob',
          secure: true
        ],
        [
          name: 'API_KEY',
          value: 'tOps3cret',
          secure: false
        ]
      ]
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        environment_variables: environmentVariables
      ])

      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      assertEquals('USERNAME', actualJobConfig.getVariables().get(0).name)
      assertEquals('API_KEY', actualJobConfig.getVariables().get(1).name)
    }

    @Test
    void 'should convert basic hash with resources to Job'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        resources: ['Java', 'Linux']
      ])

      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      def listOfResourceNames = actualJobConfig.resourceConfigs().name
      assertEquals('Java', listOfResourceNames.get(0))
      assertEquals('Linux', listOfResourceNames.get(1))
    }

    @Test
    void 'should convert basic hash with task to Job'() {
      def taskHash = [
        type: 'ant',
        attributes:
        [
          working_directory: 'working-directory',
          build_file: 'build-file',
          target: 'target'
        ]
      ]

      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        tasks: [taskHash]
      ])

      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      assertEquals(actualJobConfig.tasks().size(), 1)
      def task = actualJobConfig.tasks().first()
      assertEquals(task.getTaskType(), 'ant')
    }

    @Test
    void 'should raise exception when invalid task type is passed'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        tasks: [[type: 'invalid', attributes: [command: 'dont-care']]]
      ])

      def exception = assertThrows(UnprocessableEntityException.class, { JobRepresenter.fromJSON(jsonReader) })
      assertEquals("Invalid task type invalid. It has to be one of 'exec,ant,nant,rake,fetch,pluggable_task'.", exception.getMessage())
    }

    @Test
    void 'should convert basic hash with tabs to Job'() {
      def tabHash = [
        [
          name: 'coverage',
          path: 'Jcoverage/index.html'
        ],
        [
          name: 'something',
          path: 'something/path.html'
        ]
      ]

      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        tabs: tabHash
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      def listOfTabNames = actualJobConfig.getTabs().name
      assertEquals(listOfTabNames.get(0), 'coverage')
      assertEquals(listOfTabNames.get(1), 'something')
    }

    @Test
    void 'should convert basic hash with artifact to Job'() {
      def artifacts = [
        [
          source: 'target/dist.jar',
          destination: 'pkg',
          type: 'build'
        ],
        [
          source: 'test-dir',
          destination: 'testoutput',
          type: 'test'
        ]
      ]
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        artifacts: artifacts
      ])

      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      def destinations = actualJobConfig.artifactConfigs().collect { eachItem -> ((BuiltinArtifactConfig) eachItem).getDestination() }
      assertEquals(destinations.get(0), 'pkg')
      assertEquals(destinations.get(1), 'testoutput')

      def artifactTypes = actualJobConfig.artifactConfigs().collect { eachItem -> eachItem.getArtifactType().name() }

      assertEquals(artifactTypes.get(0), 'build')
      assertEquals(artifactTypes.get(1), 'test')
    }

    @Test
    void 'should raise exception when invalid artifact type is passed'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        artifacts: [[type: 'invalid']]
      ])

      def exception = assertThrows(UnprocessableEntityException.class, { JobRepresenter.fromJSON(jsonReader) })
      assertEquals("Invalid Artifact type: 'invalid'. It has to be one of build,test,external.", exception.getMessage())
    }

    @Test
    void 'should convert attribute elastic_profile_id to Job with elastic_profile_id'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        elastic_profile_id: 'docker.unit-test'
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      assertEquals(actualJobConfig.getElasticProfileId(), 'docker.unit-test')
    }

    @Test
    void 'should convert attribute elastic_profile_id to Job with `nil` elastic_profile_id'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        elastic_profile_id: null
      ])
      def actualJobConfig = JobRepresenter.fromJSON(jsonReader)
      assertNull(actualJobConfig.getElasticProfileId())
    }
  }


  @Test
  void 'should map errors' () {
    def jobConfig =  new JobConfig()
    jobConfig.setRunInstanceCount(-2)
    def plans = new ArtifactTypeConfigs()
    plans.add(new TestArtifactConfig(null, '../foo'))
    jobConfig.setArtifactConfigs(plans)
    jobConfig.setTasks(new Tasks(new FetchTask(new CaseInsensitiveString(''),
      new CaseInsensitiveString(''), new CaseInsensitiveString(''), null, null)))
    jobConfig.setTabs(new Tabs(new Tab('coverage#1',
      '/Jcoverage/index.html'), new Tab('coverage#1', '/Jcoverage/path.html')))
    jobConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "grp", new PipelineConfig(),
      new StageConfig(), jobConfig))

    def actualJson = toObjectString({ JobRepresenter.toJSON(it, jobConfig) })

    assertThatJson(actualJson).isEqualTo(jobHashWithErrors)
  }

    def jobHashWithErrors =
    [
      run_instance_count: -2,
      timeout: null,
      environment_variables: [],
      resources: [],
      tasks:
      [
        [
          type      : 'fetch',
          attributes:
            [
              pipeline        : '',
              artifact_origin : 'gocd',
              stage           : '',
              job             : '',
              is_source_a_file: false,
              source          : null,
              destination     : '',
              run_if          : []
            ],
          errors    :
            [
              job  : ['Job is a required field.'],
              stage: ['Stage is a required field.']
            ]
        ]
      ],
      tabs:
      [
        [
          errors:
          [
            name: ["Tab name 'coverage#1' is not unique.", "Tab name 'coverage#1' is invalid. This must be alphanumeric and can contain underscores and periods."]
          ],
          name: 'coverage#1',
          path: '/Jcoverage/index.html'
        ],
        [
          errors:
          [
            name:
            ["Tab name 'coverage#1' is not unique.", "Tab name 'coverage#1' is invalid. This must be alphanumeric and can contain underscores and periods."]
          ],
          name: 'coverage#1',
          path: '/Jcoverage/path.html'
        ]
      ],
      artifacts:
      [
        [
          errors:
          [
            source:
            ["Job 'null' has an artifact with an empty source"],
            destination:
            ["Invalid destination path. Destination path should match the pattern (([.]\\/)?[.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ])"]
          ],
          source: null,
          destination: "../foo",
          type: "test"
        ]],
      errors:
      [
        run_instance_count: ["'Run Instance Count' cannot be a negative number as it represents number of instances Go needs to spawn during runtime."],
        name: ["Name is a required field"]
      ]
    ]
  }
