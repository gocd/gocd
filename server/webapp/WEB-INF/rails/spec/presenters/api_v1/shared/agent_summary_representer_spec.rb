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


require 'rails_helper'

describe ApiV1::Shared::AgentSummaryRepresenter do

  it 'renders an agent summary' do
    presenter = ApiV1::Shared::AgentSummaryRepresenter.new(EnvironmentAgentConfig.new('agent-uuid'))
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :find, :doc)

    expect(actual_json).to have_link(:self).with_url('http://test.host/go/api/agents/agent-uuid')
    expect(actual_json).to have_link(:find).with_url('http://test.host/go/api/agents/:uuid')
    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#agents')

    actual_json.delete(:_links)
    expect(actual_json).to eq({uuid: 'agent-uuid'})
  end

end
