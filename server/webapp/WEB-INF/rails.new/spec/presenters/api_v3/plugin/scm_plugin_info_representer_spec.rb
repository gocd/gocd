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

describe ApiV3::Plugin::SCMPluginInfoRepresenter do
  it 'should describe an authentication plugin' do
    vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
    about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
    descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

    task_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
    metadata = com.thoughtworks.go.plugin.domain.common.MetadataWithPartOfIdentity.new(true, false, true)
    scm_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], task_view)

    plugin_info = com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo.new(descriptor, 'Foo task', scm_settings, nil)
    actual_json = ApiV3::Plugin::SCMPluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:scm_settings_doc)
    expect(actual_json).to have_link(:scm_settings_doc).with_url('https://api.gocd.org/#scms')
    actual_json.delete(:_links)

    expect(actual_json).to eq({
                                display_name: 'Foo task',
                                scm_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(scm_settings).to_hash(url_builder: UrlBuilder.new),
                              })

  end
end
