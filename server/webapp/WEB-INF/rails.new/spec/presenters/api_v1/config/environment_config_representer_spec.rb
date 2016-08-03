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

describe ApiV1::Config::EnvironmentConfigRepresenter do

  describe :serialize do
    it 'renders an environment with hal representation' do
      presenter = ApiV1::Config::EnvironmentConfigRepresenter.new(get_environment_config)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :find, :doc)
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/environments/:environment_name')
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/environments/dev')
      expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#environment-config')

      actual_json.delete(:_links)
      expect(actual_json).to eq({name: 'dev',
                                 pipelines: [ApiV1::Config::PipelineConfigSummaryRepresenter.new(com.thoughtworks.go.config.EnvironmentPipelineConfig.new('dev-pipeline')).to_hash(url_builder: UrlBuilder.new)],
                                 agents: [ApiV1::AgentSummaryRepresenter.new(EnvironmentAgentConfig.new('dev-agent')).to_hash(url_builder: UrlBuilder.new),
                                          ApiV1::AgentSummaryRepresenter.new(EnvironmentAgentConfig.new('omnipresent-agent')).to_hash(url_builder: UrlBuilder.new)],
                                 environment_variables: [{:secure => false, :name => 'username', :value => 'admin'}]})
    end


    def get_environment_config
      EnvironmentConfigMother.environment('dev').tap { |c| c.addEnvironmentVariable('username', 'admin') }
    end

  end

  describe :deserialize do
    it 'should convert from minimal json to EnvironmentConfig' do
      environment_config = BasicEnvironmentConfig.new

      ApiV1::Config::EnvironmentConfigRepresenter.new(environment_config).from_hash(environment_hash_basic)
      expect(environment_config.name.to_s).to eq('new-dev-environment')
      expect(environment_config.getPipelines.containsPipelineNamed(CaseInsensitiveString.new('pipeline1'))).to eq(true)
      expect(environment_config.getPipelines.containsPipelineNamed(CaseInsensitiveString.new('pipeline2'))).to eq(true)
      expect(environment_config.hasAgent('uuid1')).to eq(true)
      expect(environment_config.hasAgent('uuid2')).to eq(true)
      expect(environment_config.hasAgent('uuid2')).to eq(true)
      expect(environment_config.getSecureVariables.size()).to eq(1)
      expect(environment_config.getPlainTextVariables.size()).to eq(1)
      expect(environment_config.getVariables.size()).to eq(2)
      expect(environment_config.getPlainTextVariables.hasVariable('username')).to eq(true)
      expect(environment_config.getSecureVariables.hasVariable('password')).to eq(true)
    end

    def environment_hash_basic
      {
        name: 'new-dev-environment',
        agents: [{uuid: 'uuid1'}, {uuid: 'uuid2'}],
        pipelines: [{name: 'pipeline1'}, {name: 'pipeline2'}],
        environment_variables: [{name: 'username', value: 'admin', secure: false},
                                {name: 'password', value: 'paSsw0rd', secure: true}]
      }
    end

  end
end
