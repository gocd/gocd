##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

require 'spec_helper'

describe "admin/package_definitions/show_for_new_pipeline_wizard.html.erb" do

  it "should render package name and package configurations" do
    metadata = PackageConfigurations.new
    metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
    metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))

    p1 = ConfigurationProperty.new(ConfigurationKey.new("key1"), ConfigurationValue.new("value1"), nil, nil)
    p2 = ConfigurationProperty.new(ConfigurationKey.new("key2"), ConfigurationValue.new("value2"), nil, nil)
    package = PackageDefinition.new("go", "package-name", Configuration.new([p1, p2].to_java(ConfigurationProperty)))

    model = PackageViewModel.new metadata, package
    assign(:package_configuration, model)

    render

    expect(response.body).to have_selector(".new_form_item_block label", :text => "Package Name")
    expect(response.body).to have_selector(".new_form_item_block input[type='text'][disabled='disabled'][value='package-name']")
    expect(response.body).to have_selector(".new_form_item label", :text => "Key 1")
    expect(response.body).to have_selector(".new_form_item input[type='text'][disabled='disabled'][value='value1']")
    expect(response.body).to have_selector(".new_form_item label", :text => "Key 2")
    expect(response.body).to have_selector(".new_form_item input[type='text'][disabled='disabled'][value='value2']")

    expect(response.body).not_to have_selector(".error_message")
  end
end
