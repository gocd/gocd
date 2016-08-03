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

describe ApiV1::Plugin::PluginInfoRepresenter do
  it 'renders plugin_info with hal representation' do

    url_configuration = com.thoughtworks.go.server.ui.plugins.PluginConfiguration.new('url', HashMap.new({:required => :true, :secure => false}), nil)
    username_configuration = com.thoughtworks.go.server.ui.plugins.PluginConfiguration.new('username', HashMap.new({:required => :true, :secure => false}), 'package')
    view = com.thoughtworks.go.server.ui.plugins.PluginView.new('plugin_view_template')
    settings = PluggableInstanceSettings.new([url_configuration, username_configuration], view)

    plugin_info = PluginInfo.new('plugin_id', 'plugin_name', 'plugin_version', 'plugin_type', 'plugin_display_name', settings)

    actual_json = ApiV1::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :find, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info/plugin_id')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_info/:id')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#plugin-info')

    actual_json.delete(:_links)
    expect(actual_json).to eq({id:             'plugin_id',
                               name:           'plugin_name',
                               display_name:   'plugin_display_name',
                               version:        'plugin_version',
                               type:           'plugin_type',
                               pluggable_instance_settings: {
                                   configurations: [
                                       {
                                           key:      'url',
                                           metadata: {:required => :true, :secure => false}
                                       },
                                       {
                                           key:      'username',
                                           type:     'package',
                                           metadata: {:required => :true, :secure => false}
                                       }
                                   ],
                                   view: {
                                       template: 'plugin_view_template'
                                   }
                               }
                               })
  end

  it 'should render plugin_info in absence of pluggable_settings' do
    plugin_info = PluginInfo.new('plugin_id', 'plugin_name','plugin_version', 'plugin_type', nil, nil)

    actual_json = ApiV1::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

    actual_json.delete(:_links)
    expect(actual_json).to eq({id:      'plugin_id',
                               name:    'plugin_name',
                               version: 'plugin_version',
                               type:    'plugin_type'
                              })
  end
end
