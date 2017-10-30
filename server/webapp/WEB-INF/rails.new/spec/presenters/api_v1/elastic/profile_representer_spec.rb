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

require 'rails_helper'

describe ApiV1::Elastic::ProfileRepresenter do
  it 'represents a profile' do
    profile = ElasticProfile.new('foo', 'docker', ConfigurationPropertyMother.create('foo', false, 'bar'))
    actual_json = ApiV1::Elastic::ProfileRepresenter.new(profile).to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:doc, :self, :find)

    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#elastic-agent-profiles')
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/elastic/profiles/foo')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/elastic/profiles/:profile_id')

    actual_json.delete(:_links)

    expect(actual_json).to eq({id: 'foo', plugin_id: 'docker', properties: [{key: 'foo', value: 'bar'}]})
  end

  it 'should convert a json to a profile' do
    profile = ApiV1::Elastic::ProfileRepresenter.new(ElasticProfile.new).from_hash({id: 'foo', plugin_id: 'docker', properties: [{key: 'foo', value: 'bar'}]})

    expect(profile.getId()).to eq('foo')
    expect(profile.getPluginId()).to eq('docker')
    expect(profile.getConfigurationAsMap(true)).to eq({'foo' => 'bar'})
  end
end
