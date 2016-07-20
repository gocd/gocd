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

describe ApiV2::Config::StageRepresenter do
  describe :serialize do
    it 'should render stage with hal representation' do
      presenter   = ApiV2::Config::StageRepresenter.new(get_stage_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to eq(stage_hash)
    end

    def stage_hash
      {
        name:                    'stage1',
        fetch_materials:         true,
        clean_working_directory: false,
        never_cleanup_artifacts: false,
        approval:                {
          type:          'success',
          authorization: {
            roles: [],
            users: []
          }
        },
        environment_variables:   [
                                   {
                                     secure:          true,
                                     name:            'MULTIPLE_LINES',
                                     encrypted_value: get_stage_config.variables.get(0).getEncryptedValue
                                   },
                                   {
                                     secure: false,
                                     name:   'COMPLEX',
                                     value:  'This has very <complex> data'
                                   }
                                 ],
        jobs:                    get_stage_config.getJobs().collect { |j| ApiV2::Config::JobRepresenter.new(j).to_hash(url_builder: UrlBuilder.new) }
      }
    end

  end

  describe :deserialize do
    it 'should convert basic hash to StageConfig' do
      stage_config = StageConfig.new

      ApiV2::Config::StageRepresenter.new(stage_config).from_hash({name:                    'stage1',
                                                                   fetch_materials:         true,
                                                                   clean_working_directory: false,
                                                                   never_cleanup_artifacts: false, })
      expect(stage_config.name.to_s).to eq('stage1')
      expect(stage_config.isFetchMaterials).to eq(true)
    end

    it 'should convert basic hash with Approval to StageConfig' do
      stage_config = StageConfig.new

      ApiV2::Config::StageRepresenter.new(stage_config).from_hash({approval: {
                                                                    type:          "manual",
                                                                    authorization: {
                                                                      roles: ["role1", "role2"],
                                                                      users: ["user1", "user2"]
                                                                    }}})
      admins      = [
        AdminRole.new(CaseInsensitiveString.new("role1")), AdminRole.new(CaseInsensitiveString.new("role2")),
        AdminUser.new(CaseInsensitiveString.new("user1")), AdminUser.new(CaseInsensitiveString.new("user2"))].to_java(com.thoughtworks.go.domain.config.Admin)
      auth_config = AuthConfig.new(admins)

      approval = Approval.new(auth_config)
      expect(stage_config.getApproval).to eq(approval)
    end

    it 'should convert basic hash with Environment variables to StageConfig' do
      stage_config         = StageConfig.new
      environment_variables={environment_variables: [
                                                      {
                                                        secure:          true,
                                                        name:            'MULTIPLE_LINES',
                                                        encrypted_value: get_stage_config.variables.get(0).getEncryptedValue
                                                      },
                                                      {
                                                        secure: false,
                                                        name:   'COMPLEX',
                                                        value:  'This has very <complex> data'
                                                      }
                                                    ]}
      ApiV2::Config::StageRepresenter.new(stage_config).from_hash(environment_variables)
      expect(stage_config.variables.map(&:name)).to eq(%w(MULTIPLE_LINES COMPLEX))
    end

    it 'should convert basic hash with Jobs to StageConfig' do
      stage_config = StageConfig.new

      ApiV2::Config::StageRepresenter.new(stage_config).from_hash({jobs: [job_hash]})
      expect(stage_config.getJobs.first).to eq(com.thoughtworks.go.helper.JobConfigMother.jobConfig())
    end

    def job_hash
      {
        name:                  'defaultJob',
        run_on_all_agents:     false,
        run_instance_count:    3,
        timeout:               100,
        environment_variables: [
                                 {secure: true, name: 'MULTIPLE_LINES', encrypted_value: com.thoughtworks.go.helper.JobConfigMother.jobConfig().variables.get(0).getEncryptedValue},
                                 {secure: false, name: 'COMPLEX', value: 'This has very <complex> data'}
                               ],
        resources:             %w(Linux Java),
        tasks:                 [
                                 {type: 'ant', attributes: {working_directory: 'working-directory', build_file: 'build-file', target: 'target'}}
                               ],
        tabs:                  [
                                 {name: 'coverage', path: 'Jcoverage/index.html'},
                                 {name: 'something', path: 'something/path.html'}
                               ],
        artifacts:             [
                                 {source: 'target/dist.jar', destination: 'pkg', type: 'build'},
                                 {source: 'target/reports/**/*Test.xml', destination: 'reports', type: 'test'}
                               ],

        properties:            [{name: 'coverage.class', source: 'target/emma/coverage.xml', xpath: "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"}]
      }
    end

  end

  it "should render errors" do
    stage_config = StageConfigMother.stageConfigWithEnvironmentVariable("stage#1")
    stage_config.getJobs().get(0).setTasks(com.thoughtworks.go.config.Tasks.new(FetchTask.new(CaseInsensitiveString.new(""),CaseInsensitiveString.new(""), CaseInsensitiveString.new(""),nil, nil )))
    stage_config.addError('name','Invalid stage name')
    presenter = ApiV2::Config::StageRepresenter.new(stage_config)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq(stage_hash_with_errors(stage_config))
  end


  def stage_hash_with_errors(stage_config)
    {
      name:                    "stage#1",
      fetch_materials:         true,
      clean_working_directory: false,
      never_cleanup_artifacts: false,
      approval:                {
        type:          "success",
        authorization: {
          roles: [],
          users: []
        }
      },
      environment_variables:   [
                                 {
                                   secure:          true,
                                   name:            "MULTIPLE_LINES",
                                   encrypted_value: stage_config.variables.get(0).getEncryptedValue
                                 },
                                 {
                                   secure: false,
                                   name:   "COMPLEX",
                                   value:  "This has very <complex> data"
                                 }
                               ],
      jobs:                    stage_config.getJobs().collect { |j| ApiV2::Config::JobRepresenter.new(j).to_hash(url_builder: UrlBuilder.new) },
      errors:                  {
        name: ["Invalid stage name"]
      }
    }
  end

  def get_stage_config
    StageConfigMother.stageConfigWithEnvironmentVariable("stage1")
  end
end
