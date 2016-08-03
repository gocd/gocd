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

describe ApiV1::VersionInfoRepresenter do
  before(:each) do
    @system_environment = double('system_environment')
    @installed_version  = GoVersion.new('1.2.3-1')
    @latest_version     = GoVersion.new('5.6.7-1')

    @system_environment.stub(:getUpdateServerUrl).and_return('https://update.example.com/some/path?foo=bar')
  end

  it 'renders a version_info with hal representation' do
    model = VersionInfo.new('go_server', @installed_version, @latest_version, nil)

    presenter   = ApiV1::VersionInfoRepresenter.new(model, @system_environment)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_link(:self).with_url('http://test.host/api/version_infos/stale')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#versionInfo')
    actual_json.delete(:_links)

    expect(actual_json).to eq({ component_name:    'go_server',
                                update_server_url: 'https://update.example.com/some/path?foo=bar&current_version=1.2.3-1',
                                installed_version: '1.2.3-1',
                                latest_version:    '5.6.7-1' })
  end

  it 'should handle latest version unavailable' do
    model = VersionInfo.new('go_server', @installed_version, nil, nil)

    presenter   = ApiV1::VersionInfoRepresenter.new(model, @system_environment)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    actual_json.delete(:_links)

    expect(actual_json).to eq({ component_name:    'go_server',
                                update_server_url: 'https://update.example.com/some/path?foo=bar&current_version=1.2.3-1',
                                installed_version: '1.2.3-1',
                                latest_version:    nil })
  end

  it 'should handle update server url without query params' do
    @system_environment.stub(:getUpdateServerUrl).and_return('https://update.example.com/some/path')

    model = VersionInfo.new('go_server', @installed_version, nil, nil)

    presenter   = ApiV1::VersionInfoRepresenter.new(model, @system_environment)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    actual_json.delete(:_links)

    expect(actual_json).to eq({ component_name:    'go_server',
                                update_server_url: 'https://update.example.com/some/path?current_version=1.2.3-1',
                                installed_version: '1.2.3-1',
                                latest_version:    nil })
  end
end
