##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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

describe ApiV1::Admin::MergedEnvironments::MergedEnvironmentConfigRepresenter do

  describe :serialize do
    it 'renders an environment with hal representation' do
      environment = get_environment_config
      presenter = ApiV1::Admin::MergedEnvironments::MergedEnvironmentConfigRepresenter.new(environment)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :find, :doc)
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/environments/:environment_name/merged')
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/environments/dev/merged')
      expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#merged-environment-config')

      actual_json.delete(:_links)
      expect(actual_json).to eq({
                                  name: 'dev',
                                  origins: [ApiV1::Shared::ConfigOrigin::ConfigXmlOriginRepresenter.new(com.thoughtworks.go.config.remote.FileConfigOrigin.new).to_hash(url_builder: UrlBuilder.new)],
                                  pipelines: [ApiV1::Admin::MergedEnvironments::PipelineConfigSummaryRepresenter.new({pipeline: com.thoughtworks.go.config.EnvironmentPipelineConfig.new('dev-pipeline'), environment: environment}).to_hash(url_builder: UrlBuilder.new)],
                                  agents: [ApiV1::Admin::MergedEnvironments::AgentSummaryRepresenter.new({agent: EnvironmentAgentConfig.new('dev-agent'), environment: environment}).to_hash(url_builder: UrlBuilder.new),
                                           ApiV1::Admin::MergedEnvironments::AgentSummaryRepresenter.new({agent: EnvironmentAgentConfig.new('omnipresent-agent'), environment: environment}).to_hash(url_builder: UrlBuilder.new)],
                                  environment_variables: [
                                    ApiV1::Admin::MergedEnvironments::EnvironmentVariableRepresenter.new({env_var: EnvironmentVariableConfig.new('username', 'admin'), environment: environment}).to_hash(url_builder: UrlBuilder.new)
                                  ]
                                })
    end


    def get_environment_config
      env = EnvironmentConfigMother.environment('dev').tap { |c| c.addEnvironmentVariable('username', 'admin') }
      env.setOrigins(com.thoughtworks.go.config.remote.FileConfigOrigin.new)
      env
    end

  end
end