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

describe PackageViewModel do
  it "should create model from metadata and package configuration" do
    package_configuration = PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1")
    config_property = ConfigurationProperty.new(ConfigurationKey.new("key1"), ConfigurationValue.new("value"))
    model = PackagePropertyModel.new(package_configuration, config_property)
    model.display_name.should == "Key 1"
    model.name.should == "key1"
    model.value.should == "value"
    model.is_mandatory.should == true
    model.is_secure.should == false
  end

  it "should create model from metadata when package property is nil" do
    package_configuration = PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1")
    model = PackagePropertyModel.new(package_configuration, nil)
    model.display_name.should == "Key 1"
    model.name.should == "key1"
    model.value.should == nil
    model.is_mandatory.should == true
    model.is_secure.should == false
  end

  it "should display key name if display name is not configured" do
    config = PackageConfiguration.new("key3_secure").with(PackageConfiguration::SECURE, true)
    property = ConfigurationProperty.new(ConfigurationKey.new("key3_secure"), nil, EncryptedConfigurationValue.new("secure"), nil)

    model = PackagePropertyModel.new config, property
    model.display_name.should == "key3_secure"
    model.value.should == "secure"
  end
end
