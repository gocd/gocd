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

describe ApiV3::Plugin::PluggableInstanceSettingsRepresenter do
  it 'should describe plugin settings' do
    package_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('package_view_template')
    package_metadata = com.thoughtworks.go.plugin.domain.common.PackageMaterialMetadata.new(true, false, true, 'Url', 1)
    configs = com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', package_metadata)
    package_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([configs], package_view)

    actual_json = ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(package_settings).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to eq({
                                configurations: [
                                  {
                                    key: 'url',
                                    metadata: {
                                      secure: false,
                                      required: true,
                                      part_of_identity: true,
                                      display_name: 'Url',
                                      display_order: 1
                                    }
                                  }
                                ],
                                view: {
                                  template: 'package_view_template'
                                }
                              }
                           )

  end
end
