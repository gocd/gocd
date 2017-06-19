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

describe ApiV2::Plugin::PluginInfoRepresenter do
  it 'renders plugin_info with hal representation' do

    url_configuration = com.thoughtworks.go.server.ui.plugins.PluginConfiguration.new('url', HashMap.new({required: true, secure: false, display_name: 'Url', display_order: '1'}), nil)
    username_configuration = com.thoughtworks.go.server.ui.plugins.PluginConfiguration.new('username', HashMap.new({:required => :true, :secure => false}), 'package')
    view = com.thoughtworks.go.server.ui.plugins.PluginView.new('plugin_view_template')
    settings = PluggableInstanceSettings.new([url_configuration, username_configuration], view)

    plugin_info = PluginInfo.new('plugin_id', 'plugin_name', 'plugin_version', 'plugin_type', 'plugin_display_name', settings, nil)

    actual_json = ApiV2::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :find, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info/plugin_id')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_info/:id')
    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#plugin-info')

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
                                     metadata: {required: true, secure: false, display_name: 'Url', display_order: '1'}
                                   },
                                   {
                                     key:      'username',
                                     type:     'package',
                                     metadata: {required: :true, secure: false}
                                   }
                                 ],
                                 view: {
                                   template: 'plugin_view_template'
                                 }
                               }
                              })
  end

  it 'should render plugin_info in absence of pluggable_settings' do
    plugin_info = PluginInfo.new('plugin_id', 'plugin_name','plugin_version', 'plugin_type', nil, nil, nil)

    actual_json = ApiV2::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

    actual_json.delete(:_links)
    expect(actual_json).to eq({id:      'plugin_id',
                               name:    'plugin_name',
                               version: 'plugin_version',
                               type:    'plugin_type'
                              })
  end

  it 'should render image when available' do
    image = com.thoughtworks.go.plugin.access.common.models.Image.new('foo', Base64.strict_encode64('bar'))
    plugin_info = PluginInfo.new('plugin_id', 'plugin_name', 'plugin_version', 'plugin_type', nil, nil, image)
    actual_json = ApiV2::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_link(:image).with_url(image.toDataURI)
    actual_json.delete(:_links)
    expect(actual_json).to eq({id:      'plugin_id',
                               name:    'plugin_name',
                               version: 'plugin_version',
                               type:    'plugin_type',
                               image: {
                                 content_type: image.getContentType,
                                 data:         image.data
                               }
                              })
  end
end
