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

describe ApiV1::PluginRepresenter do
  it 'renders plugin with hal representation' do

    url_configuration = PluginConfigurationViewModel.new('url', HashMap.new({:required => :true, :secure => false}))
    username_configuration = PluginConfigurationViewModel.new('username', HashMap.new({:required => :true, :secure => false}), 'package')
    plugin = PluginViewModel.new('plugin_id', 'plugin_name', 'plugin_version', 'plugin_type', 'plugin_view_template', [url_configuration, username_configuration])

    actual_json = ApiV1::PluginRepresenter.new(plugin).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :find, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins/plugin_id')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugins/:id')
    expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/#plugins')

    actual_json.delete(:_links)
    expect(actual_json).to eq({id:             'plugin_id',
                               name:           'plugin_name',
                               version:        'plugin_version',
                               type:           'plugin_type',
                               viewTemplate:   'plugin_view_template',
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
                               ]})
  end

  it 'should render plugin in absence of configurations' do
    plugin = PluginViewModel.new('plugin_id', 'plugin_name', 'plugin_version', 'plugin_type')

    actual_json = ApiV1::PluginRepresenter.new(plugin).to_hash(url_builder: UrlBuilder.new)

    actual_json.delete(:_links)
    expect(actual_json).to eq({id:      'plugin_id',
                               name:    'plugin_name',
                               version: 'plugin_version',
                               type:    'plugin_type',
                              })
  end
end