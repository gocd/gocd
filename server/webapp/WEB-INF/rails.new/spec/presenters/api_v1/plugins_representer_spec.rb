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

describe ApiV1::PluginsRepresenter do
  it 'renders all plugins with hal representation' do
    plugin = PluginViewModel.new('plugin_id', 'plugin_version', 'plugin_type')

    actual_json = ApiV1::PluginsRepresenter.new([plugin]).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugins')
    expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/#plugins')
    actual_json.delete(:_links)
    actual_json.fetch(:_embedded).should == { :plugins => [ApiV1::PluginRepresenter.new(plugin).to_hash(url_builder: UrlBuilder.new)] }
  end
end