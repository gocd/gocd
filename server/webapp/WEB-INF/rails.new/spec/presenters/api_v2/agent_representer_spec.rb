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

describe ApiV2::AgentRepresenter do
  include AgentMother

  it 'renders an agent with hal representation' do
    presenter   = ApiV2::AgentRepresenter.new(idle_agent(
                                                hostname:         'agent01.example.com',
                                                location:         '/var/lib/go-server',
                                                uuid:             'some-uuid',
                                                space:            10.gigabytes,
                                                operating_system: 'Linux',
                                                ip_address:       '127.0.0.1',
                                                resources:        'linux,firefox',
                                                environments:     %w(uat load_test)
                                              ))
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :find, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/agents/some-uuid')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/agents/:uuid')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#agents')

    actual_json.delete(:_links)
    expect(actual_json).to eq(agent_hash)
  end

  it 'renders agent state correctly' do
    {
      idle_agent         => AgentRuntimeStatus::Idle,
      building_agent     => AgentRuntimeStatus::Building,
      lost_contact_agent => AgentRuntimeStatus::LostContact,
      missing_agent      => AgentRuntimeStatus::Missing,
      cancelled_agent    => AgentRuntimeStatus::Building,
    }.each do |agent, state|
      presenter   = ApiV2::AgentRepresenter.new(agent)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json[:agent_state]).to eq(state)
    end
  end

  it 'renders build state correctly' do
    {
      idle_agent         => AgentRuntimeStatus::Idle,
      building_agent     => AgentRuntimeStatus::Building,
      lost_contact_agent => AgentRuntimeStatus::Unknown,
      missing_agent      => AgentRuntimeStatus::Unknown,
      cancelled_agent    => AgentRuntimeStatus::Cancelled,
    }.each do |agent, state|
      presenter   = ApiV2::AgentRepresenter.new(agent)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json[:build_state]).to eq(state)
    end
  end

  it 'renders config state correctly' do
    {
      pending_agent      => AgentConfigStatus::Pending,
      building_agent     => AgentConfigStatus::Enabled,
      disabled_agent     => AgentConfigStatus::Disabled,
      lost_contact_agent => AgentConfigStatus::Enabled,
      missing_agent      => AgentConfigStatus::Enabled,
    }.each do |agent, state|
      presenter   = ApiV2::AgentRepresenter.new(agent)
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

      expect(actual_json[:agent_config_state]).to eq(state)
    end
  end

  it 'renders config errors during serialization' do
    presenter   = ApiV2::AgentRepresenter.new(agent_with_config_errors)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expected_errors = {
        ip_address: ["'IP' is an invalid IP address."],
        resources: [
        "Resource name 'foo%' is not valid. Valid names much match '^[-\\w\\s|.]*$'",
        "Resource name 'bar$' is not valid. Valid names much match '^[-\\w\\s|.]*$'"
      ]
    }
    expect(actual_json[:errors]).to eq(expected_errors)
  end

  it "should handle nil agent config" do
    presenter   = ApiV2::AgentRepresenter.new(nil)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to be_nil
  end

  def agent_hash
    {
      uuid:               'some-uuid',
      hostname:           'agent01.example.com',
      ip_address:         '127.0.0.1',
      agent_config_state: AgentConfigStatus::Enabled,
      agent_state:        AgentRuntimeStatus::Idle,
      build_state:        AgentRuntimeStatus::Idle,
      sandbox:            '/var/lib/go-server',
      operating_system:   'Linux',
      free_space:         10.gigabytes,
      resources:          %w(firefox linux),
      environments:       %w(load_test uat)
    }
  end
end
