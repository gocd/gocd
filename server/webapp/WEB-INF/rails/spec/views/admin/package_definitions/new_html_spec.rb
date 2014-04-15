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

require File.join(File.dirname(__FILE__), "..", "..", "..", "spec_helper")

describe "new.html.erb" do

  it "should render new pkg def page" do
    metadata = PackageConfigurations.new
    metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
    metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::SECURE, true).with(PackageConfiguration::REQUIRED, false).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))

    package_definition = PackageDefinition.new
    p1 = ConfigurationProperty.new(ConfigurationKey.new("key1"), ConfigurationValue.new("value1"))
    p2 = ConfigurationProperty.new(ConfigurationKey.new("key2"), EncryptedConfigurationValue.new("value2"))

    configuration = Configuration.new([p1, p2].to_java(com.thoughtworks.go.domain.config.ConfigurationProperty))
    package_definition.setConfiguration(configuration)
    assigns[:package_configuration] = PackageViewModel.new metadata, package_definition

    render "admin/package_definitions/new.html"

    response.body.should have_tag(".information", "The new package will be available to be used as material in all pipelines. Other admins might be able to edit this package.")

    response.body.should have_tag(".new_form_item_block label", "Package Name*")
    response.body.should have_tag(".new_form_item_block input[type='text'][name='material[package_definition[name]]']")

    response.body.should have_tag(".new_form_item input[type='hidden'][name='material[package_definition[configuration][0][configurationKey][name]]'][value='key1']")
    response.body.should have_tag(".new_form_item label", "Key 1*")
    response.body.should have_tag(".new_form_item input[type='text'][name='material[package_definition[configuration][0][configurationValue][value]]'][value='value1']")

    response.body.should have_tag(".new_form_item input[type='hidden'][name='material[package_definition[configuration][1][configurationKey][name]]'][value='key2']")
    response.body.should have_tag(".new_form_item label", "Key 2")
    response.body.should have_tag(".new_form_item input[type='password'][name='material[package_definition[configuration][1][configurationValue][value]]'][value='value2']")
  end
end