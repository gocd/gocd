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

describe ApiV1::Scms::PluggableScmRepresenter do

  before :each do
    @scm = SCM.new("1", PluginConfiguration.new("foo", "1"),
                  Configuration.new(
                      ConfigurationProperty.new(ConfigurationKey.new("username"), ConfigurationValue.new("user")),
                      ConfigurationProperty.new(ConfigurationKey.new("password"), EncryptedConfigurationValue.new("bar"))))
    @scm.setName('material')
  end

  it 'should render a pluggable scm material with hal representation' do
    presenter = ApiV1::Scms::PluggableScmRepresenter.new(@scm)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_link(:self).with_url(UrlBuilder.new.apiv1_admin_scm_api_url(material_name: @scm.get_name))
    expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/#scms')
    actual_json.delete(:_links)

    expect(actual_json).to eq(expected_json)
  end

  def expected_json
    {
        id: "1",
        name: "material",
        auto_update: true,
        plugin_configuration: {
            id: "foo",
            version:"1"
        },
        configuration: [
            {
                key: "username",
                value: "user"
            },
            {
                key: "password",
                encrypted_value: "bar"
            }
        ]
    }
  end
end