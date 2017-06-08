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

describe ApiV1::Security::AuthConfigRepresenter do
  it 'should serialize AuthConfig to json' do
    authConfig = SecurityAuthConfig.new('foo', 'docker', ConfigurationPropertyMother.create('foo', false, 'bar'))
    actual_json = ApiV1::Security::AuthConfigRepresenter.new(authConfig).to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:doc, :self, :find)

    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#authorization-configuration')
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/security/auth_configs/foo')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/security/auth_configs/:auth_config_id')

    actual_json.delete(:_links)

    expect(actual_json).to eq({id: 'foo', plugin_id: 'docker', properties: [{key: 'foo', value: 'bar'}]})
  end

  it 'Should convert a json to an authconfig' do
    authConfig = ApiV1::Security::AuthConfigRepresenter.new(SecurityAuthConfig.new).from_hash({id: 'foo', plugin_id: 'ldap', properties: [{key: 'foo', value: 'bar'}]})

    expect(authConfig.getId()).to eq('foo')
    expect(authConfig.getPluginId()).to eq('ldap')
    expect(authConfig.getConfigurationAsMap(true)).to eq({'foo' => 'bar'})
  end

  it 'Should accept a encrypted value' do
    authConfig = ApiV1::Security::AuthConfigRepresenter.new(SecurityAuthConfig.new).from_hash({id: 'foo', plugin_id: 'ldap', properties: [{key: 'foo', encrypted_value: GoCipher.new.encrypt('bar')}]})

    expect(authConfig.getId()).to eq('foo')
    expect(authConfig.getPluginId()).to eq('ldap')
    expect(authConfig.getConfigurationAsMap(true)).to eq({'foo' => 'bar'})
  end
end
