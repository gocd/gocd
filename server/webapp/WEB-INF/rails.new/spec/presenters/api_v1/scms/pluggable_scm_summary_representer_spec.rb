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

describe ApiV1::Scms::PluggableScmSummaryRepresenter do

  before :each do
    @scm = SCM.new("1", PluginConfiguration.new("foo", "1"),
                   Configuration.new(
                       ConfigurationProperty.new(ConfigurationKey.new("username"), ConfigurationValue.new("user")),
                       ConfigurationProperty.new(ConfigurationKey.new("password"), EncryptedConfigurationValue.new("bar"))))
    @scm.setName('material')
  end

  it 'should render some details of pluggable scm material with hal representation' do
    actual_json = ApiV1::Scms::PluggableScmSummaryRepresenter.new(@scm).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_link(:self).with_url(UrlBuilder.new.apiv1_admin_scm_url(material_name: @scm.get_name))
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#scms')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/scms/:material_name')
    actual_json.delete(:_links)
    expect(actual_json).to eq(expected_partial_representation)
  end

  def expected_partial_representation
    {
        id: "1",
        name: "material",
        plugin_metadata: {
            id: "foo",
            version:"1"
        }
    }
  end
end
