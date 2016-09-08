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

describe ApiV4::AgentsRepresenter do
  include AgentInstanceFactory

  before :each do
    @security_service = double('security_service')
    @current_user = Username.new(CaseInsensitiveString.new('user'))
  end

  it 'renders all agents with hal representation' do
    idle_agent  = idle_agent(
      hostname:         'Agent01',
      location:         '/var/lib/go-server',
      uuid:             'some-uuid',
      space:            10.gigabytes,
      operating_system: 'Linux',
      ip_address:       '127.0.0.1',
      resources:        'linux,firefox'
    )
    presenter   = ApiV4::AgentsRepresenter.new({ idle_agent => %w(uat load_test) }, @security_service, @current_user)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/agents')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#agents')
    actual_json.delete(:_links)
    actual_json.fetch(:_embedded).should == { :agents => [ApiV4::AgentRepresenter.new({agent: idle_agent, environments: %w(uat load_test), security_service: @security_service, current_user: @current_user}).to_hash(url_builder: UrlBuilder.new)] }
  end

end
