##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

require 'spec_helper'

describe ApiV3::Config::JobRepresenter do

  describe :serialize do
    it 'should render job with hal representation' do

      presenter   = ApiV3::Config::JobRepresenter.new(com.thoughtworks.go.helper.JobConfigMother.jobConfig())
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to eq(job_hash)
    end

    it 'should serialize run_instance_count with value set to 10' do
      job_config = JobConfig.new
      job_config.setRunInstanceCount(10)

      presenter   = ApiV3::Config::JobRepresenter.new(job_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json[:run_instance_count]).to be(10)
    end

    it 'should serialize run_instance_count with value `all` when runOnAllAgents is true' do
      job_config = JobConfig.new
      job_config.setRunOnAllAgents(true)

      presenter   = ApiV3::Config::JobRepresenter.new(job_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json[:run_instance_count]).to eq('all')
    end

    it 'should serialize run_instance_count with value `nil` when runOnAllAgents is false and eunInstanceCount is unset' do
      job_config = JobConfig.new

      presenter   = ApiV3::Config::JobRepresenter.new(job_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json[:run_instance_count]).to be(nil)
    end

    it 'should serialize timeout with value set to `10` to `10`' do
      job_config = JobConfig.new
      job_config.setTimeout('10')

      presenter   = ApiV3::Config::JobRepresenter.new(job_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json[:timeout]).to be(10)
    end

    it 'should serialize timeout with value `0` to `never`' do
      job_config = JobConfig.new
      job_config.setTimeout('0')

      presenter   = ApiV3::Config::JobRepresenter.new(job_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json[:timeout]).to eq('never')
    end

    it 'should serialize timeout with value `nil` to `default`' do
      job_config = JobConfig.new
      job_config.setTimeout(nil)

      presenter   = ApiV3::Config::JobRepresenter.new(job_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json[:timeout]).to eq(nil)
    end

    it 'should serialize job elastic_profile_id' do
      job_config = JobConfig.new
      job_config.setElasticProfileId('docker.unit-test')

      presenter   = ApiV3::Config::JobRepresenter.new(job_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json[:elastic_profile_id]).to eq('docker.unit-test')
    end

    def job_hash
      {
        name:                  'defaultJob',
        run_instance_count:    3,
        timeout:               100,
        environment_variables: [
                                 { secure: true, name: 'MULTIPLE_LINES', encrypted_value: com.thoughtworks.go.helper.JobConfigMother.jobConfig().variables.get(0).getEncryptedValue },
                                 { secure: false, name: 'COMPLEX', value: 'This has very <complex> data' }
                               ],
        resources:             %w(Linux Java),
        tasks:                 [
                                 { type: 'ant', attributes: {working_directory: 'working-directory', build_file: 'build-file', target: 'target', run_if: [], on_cancel: nil} }
                               ],
        tabs:                  [
                                 { name: 'coverage', path: 'Jcoverage/index.html' },
                                 { name: 'something', path: 'something/path.html' }
                               ],
        artifacts:             [
                                 { source: 'target/dist.jar', destination: 'pkg', type: 'build' },
                                 { source: 'target/reports/**/*Test.xml', destination: 'reports', type: 'test' }
                               ],

        properties:            [{ name: 'coverage.class', source: 'target/emma/coverage.xml', xpath: "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')" }]
      }
    end
  end

  describe :deserialize do
    it 'should convert basic hash to Job' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash({
                                                                name:    'some-job',
                                                                timeout: '100',
                                                              })
      expect(job_config.name.to_s).to eq('some-job')
      expect(job_config.timeout).to eq('100')
    end

    it 'should convert attribute run_instance_count with value `all` to Job' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash(run_instance_count: 'all')
      expect(job_config.run_on_all_agents).to eq(true)
      expect(job_config.run_instance_count).to eq(nil)
    end

    it 'should convert attribute run_instance_count with value `nil` to Job' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash(run_instance_count: nil)
      expect(job_config.run_on_all_agents).to eq(false)
      expect(job_config.run_instance_count).to eq(nil)
    end

    it 'should convert attribute run_instance_count with integer value `nil` to Job' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash(run_instance_count: '10')
      expect(job_config.run_on_all_agents).to eq(false)
      expect(job_config.run_instance_count).to eq('10')

      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash(run_instance_count: 10)
      expect(job_config.run_on_all_agents).to eq(false)
      expect(job_config.run_instance_count).to eq('10')
    end

    it 'should convert attribute timeout with value `never` to Job with timeout `0`' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash(timeout: 'never')
      expect(job_config.timeout).to eq('0')
    end

    it 'should convert attribute timeout with value `nil` to Job with timeout `nil`' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash(timeout: nil)
      expect(job_config.timeout).to eq(nil)
    end

    it 'should convert attribute timeout with integer value `10` to Job with timeout `10`' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash(timeout: '10')
      expect(job_config.timeout).to eq('10')

      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash(timeout: 10)
      expect(job_config.timeout).to eq('10')
    end

    it 'should convert basic hash with environment variable to Job' do
      job_config            = JobConfig.new
      environment_variables = [
        {
          name:   'USERNAME',
          value:  'bob',
          secure: true
        },
        {
          name:   'API_KEY',
          value:  'tOps3cret',
          secure: false
        }
      ]

      ApiV3::Config::JobRepresenter.new(job_config).from_hash({ environment_variables: environment_variables })
      expect(job_config.variables.map(&:name)).to eq(%w(USERNAME API_KEY))
    end

    it 'should convert basic hash with resources to Job' do
      job_config = JobConfig.new

      ApiV3::Config::JobRepresenter.new(job_config).from_hash({ resources: %w(java linux) })
      expect(job_config.resources.map(&:name)).to eq(%w(java linux))
    end

    it 'should convert basic hash with task to Job' do
      job_config = JobConfig.new
      task_hash  = { type: 'ant', attributes: { working_directory: 'working-directory', build_file: 'build-file', target: 'target' } }
      ApiV3::Config::JobRepresenter.new(job_config).from_hash({ tasks: [task_hash] })
      expect(job_config.tasks.size).to eq(1)
      expect(job_config.tasks.first.getTaskType).to eq('ant')
      expect(job_config.tasks.first.getBuildFile).to eq('build-file')
    end

    it 'should  raise exception when invalid task type is passed' do
      expect do
        job_config = JobConfig.new
        ApiV3::Config::JobRepresenter.new(job_config).from_hash({ tasks: [{ type: 'invalid' }] })
      end.to raise_error(ApiV3::UnprocessableEntity, /Invalid task type 'invalid'. It has to be one of /)
    end

    it 'should convert basic hash with tabs to Job' do
      job_config = JobConfig.new
      tab_hash   = [
        {
          name: 'coverage', path: 'Jcoverage/index.html' },
        { name: 'something', path: 'something/path.html' }
      ]
      ApiV3::Config::JobRepresenter.new(job_config).from_hash({ tabs: tab_hash })
      expect(job_config.getTabs.map(&:name)).to eq(%w(coverage something))
    end

    it 'should convert basic hash with artifact to Job' do
      job_config = JobConfig.new
      artifacts  = [
        { source: 'target/dist.jar', destination: 'pkg', type: 'build' },
        { source: nil, destination: 'testoutput', type: 'test' }
      ]

      ApiV3::Config::JobRepresenter.new(job_config).from_hash({ artifacts: artifacts })
      expect(job_config.artifactPlans.map(&:dest)).to eq(%w(pkg testoutput))
      expect(job_config.artifactPlans.map(&:getArtifactType).map(&:to_s)).to eq(%w(file unit))
    end

    it 'should raise exception when invalid artifact type is passed' do
      expect do
        job_config = JobConfig.new
        ApiV3::Config::JobRepresenter.new(job_config).from_hash({ artifacts: [{ type: 'invalid' }] })
      end.to raise_error(ApiV3::UnprocessableEntity, /Invalid Artifact type: 'invalid'. It has to be one of/)
    end

    it 'should convert basic hash with properties to Job' do
      job_config = JobConfig.new
      properties = [
        {
          name:   'coverage.class',
          source: 'target/emma/coverage.xml',
          xpath:  "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"
        }
      ]

      ApiV3::Config::JobRepresenter.new(job_config).from_hash({ properties: properties })
      expect(job_config.getProperties.map(&:name)).to eq(['coverage.class'])
    end

    it 'should convert attribute elastic_profile_id to Job with elastic_profile_id' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash({elastic_profile_id: 'docker.unit-test'})

      expect(job_config.getElasticProfileId).to eq('docker.unit-test')
    end

    it 'should convert attribute elastic_profile_id to Job with `nil` elastic_profile_id' do
      job_config = JobConfig.new
      ApiV3::Config::JobRepresenter.new(job_config).from_hash({})

      expect(job_config.getElasticProfileId).to eq(nil)
    end
  end

  it 'should map errors' do
    job_config = JobConfig.new
    job_config.setRunInstanceCount(-2);
    plans      = ArtifactPlans.new
    plans.add(TestArtifactPlan.new(nil, '../foo'))
    job_config.setArtifactPlans(plans)
    job_config.setTasks(com.thoughtworks.go.config.Tasks.new(FetchTask.new(CaseInsensitiveString.new(''), CaseInsensitiveString.new(''), CaseInsensitiveString.new(''), nil, nil)))
    job_config.setTabs(com.thoughtworks.go.config.Tabs.new(com.thoughtworks.go.config.Tab.new('coverage#1', '/Jcoverage/index.html'), com.thoughtworks.go.config.Tab.new('coverage#1', '/Jcoverage/path.html')))
    job_config.validateTree(PipelineConfigSaveValidationContext.forChain(true, "grp", PipelineConfig.new, StageConfig.new, job_config))
    presenter   = ApiV3::Config::JobRepresenter.new(job_config)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq(job_hash_with_errors)
  end

  def job_hash_with_errors
    {
      name:                  nil,
      run_instance_count:    -2,
      timeout:               nil,
      environment_variables: [],
      resources:             [],
      tasks:                 [
                               {
                                 type:       'fetch',
                                 attributes: {pipeline: '', stage: '', job: '', is_source_a_file: false, source: nil, destination: '', run_if: [], on_cancel: nil},
                                 errors:     {
                                   job:    ['Job is a required field.'],
                                   source: ['Should provide either srcdir or srcfile'],
                                   stage:  ['Stage is a required field.']
                                 }
                               }
                             ],
      tabs:                  [
                               {
                                 errors: {
                                   name: ["Tab name 'coverage#1' is not unique.",
                                          "Tab name 'coverage#1' is invalid. This must be alphanumeric and can contain underscores and periods."
                                         ]
                                 },
                                 name:   'coverage#1',
                                 path:   '/Jcoverage/index.html'
                               },
                               {
                                 errors: {
                                   name: ["Tab name 'coverage#1' is not unique.",
                                          "Tab name 'coverage#1' is invalid. This must be alphanumeric and can contain underscores and periods."
                                         ]
                                 },
                                 name:   'coverage#1',
                                 path:   '/Jcoverage/path.html'
                               }
                             ],
      artifacts:             [
                               {
                                 errors:      {
                                   source:      ["Job 'null' has an artifact with an empty source"],
                                   destination: ["Invalid destination path. Destination path should match the pattern (([.]\\/)?[.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ])"]
                                 },
                                 source:      nil,
                                 destination: "../foo",
                                 type:        "test"}],
      properties:            nil,
      errors:                {
        run_instance_count: ["'Run Instance Count' cannot be a negative number as it represents number of instances Go needs to spawn during runtime."],
        name:               ["Name is a required field"]
      }
    }
  end

end
