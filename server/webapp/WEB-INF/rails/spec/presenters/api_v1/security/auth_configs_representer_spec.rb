#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe ApiV1::Security::AuthConfigsRepresenter do
  it 'Should serialize to json' do
    auth_config = SecurityAuthConfig.new('foo', 'docker', ConfigurationPropertyMother.create('foo', false, 'bar'))
    actual_json = ApiV1::Security::AuthConfigsRepresenter.new([auth_config]).to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:doc, :self, :find)

    expect(actual_json).to have_link(:doc).with_url(CurrentGoCDVersion.api_docs_url '#authorization-configuration')
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/security/auth_configs')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/security/auth_configs/:auth_config_id')

    actual_json.delete(:_links)

    expect(actual_json.fetch(:_embedded)).to eq(auth_configs: [ApiV1::Security::AuthConfigRepresenter.new(auth_config).to_hash(url_builder: UrlBuilder.new)])
  end
end
