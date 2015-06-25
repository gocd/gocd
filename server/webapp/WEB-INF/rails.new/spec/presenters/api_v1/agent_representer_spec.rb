##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

describe ApiV1::AgentRepresenter do
  include AgentMother

  it 'renders an agent with hal representation' do
    presenter   = ApiV1::AgentRepresenter.new(idle_agent(
                                                hostname:         'agent01.example.com',
                                                location:         '/var/lib/go-server',
                                                uuid:             'some-uuid',
                                                space:            10.gigabytes,
                                                operating_system: 'Linux',
                                                ip_address:       '127.0.0.1',
                                                resources:        'linux,firefox',
                                                environments:     ['uat', 'load_test']
                                              ))
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :find, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/agents/some-uuid')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/agents/:uuid')
    expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/#agents')

    actual_json.delete(:_links)
    expect(actual_json).to eq(agent_hash)
  end

  def agent_hash
    {
      uuid:             'some-uuid',
      hostname:         'agent01.example.com',
      ip_address:       '127.0.0.1',
      enabled:          true,
      sandbox:          '/var/lib/go-server',
      status:           'Idle',
      operating_system: 'Linux',
      free_space:       10.gigabytes,
      resources:        ['firefox', 'linux'],
      environments:     ['load_test', 'uat']
    }
  end

end
