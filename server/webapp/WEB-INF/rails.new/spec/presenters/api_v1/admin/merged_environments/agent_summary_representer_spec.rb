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

describe ApiV1::Admin::MergedEnvironments::AgentSummaryRepresenter do

  it 'renders an agent summary' do
    presenter = ApiV1::Admin::MergedEnvironments::AgentSummaryRepresenter.new({agent: EnvironmentAgentConfig.new('agent-uuid'), environment: get_environment_config})
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :find, :doc)

    expect(actual_json).to have_link(:self).with_url('http://test.host/api/agents/agent-uuid')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/agents/:uuid')
    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#agents')

    actual_json.delete(:_links)
    expect(actual_json).to eq(get_agent_summary)
  end

  def get_environment_config
    env = EnvironmentConfigMother.environment('dev')
    env.setOrigins(com.thoughtworks.go.config.remote.FileConfigOrigin.new)
    env
  end

  def get_agent_summary
    {
      uuid: 'agent-uuid',
      origin: {
        type: 'local',
        file: {
          _links: {
            self: {
              href: 'http://test.host/admin/config_xml'
            },
            doc: {
              href: 'https://api.gocd.org/#get-configuration'
            }
          },
          name: 'cruise-config.xml'
        }
      }
    }
  end
end