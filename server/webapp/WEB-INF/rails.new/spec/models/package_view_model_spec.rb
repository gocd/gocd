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
  before(:each) do
    @metadata = PackageConfigurations.new
    @metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
    @metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
    @metadata.addConfiguration(PackageConfiguration.new("key3_secure").with(PackageConfiguration::SECURE, true).with(PackageConfiguration::DISPLAY_NAME, "Key 3 Secure"))

    p1 = ConfigurationProperty.new(ConfigurationKey.new("key1"), ConfigurationValue.new("value1"), nil, nil)
    p2 = ConfigurationProperty.new(ConfigurationKey.new("key2"), ConfigurationValue.new("value2"), nil, nil)
    p3_secure = ConfigurationProperty.new(ConfigurationKey.new("key3_secure"), nil, EncryptedConfigurationValue.new("secure"), nil)
    @package = PackageDefinition.new("go", "package-name", Configuration.new([p1, p2, p3_secure].to_java(ConfigurationProperty)))
  end

  it "should create package view model from metadata and package configuration" do
    model = PackageViewModel.new @metadata, @package
    model.name.should == "package-name"
    model.properties.size.should == 3
    model.properties[0].display_name.should == "Key 1"
    model.properties[0].value.should == "value1"
    model.properties[1].display_name.should == "Key 2"
    model.properties[1].value.should == "value2"
    model.properties[2].display_name.should == "Key 3 Secure"
    model.properties[2].value.should == "secure"
  end

  it "should create package view model from metadata" do
    model = PackageViewModel.new @metadata, PackageDefinition.new
    model.name.should == nil
    model.properties.size.should == 3
    model.properties[0].display_name.should == "Key 1"
    model.properties[0].value.should == nil
    model.properties[1].display_name.should == "Key 2"
    model.properties[1].value.should == nil
    model.properties[2].display_name.should == "Key 3 Secure"
    model.properties[2].value.should == nil
  end

  it "should filter secure properties" do
    model = PackageViewModel.new @metadata, @package
    model.name.should == "package-name"
    model.properties.size.should == 3
    model.filterSecureProperties!
    model.properties.size.should == 2
    model.properties[0].display_name.should == "Key 1"
    model.properties[0].value.should == "value1"
    model.properties[1].display_name.should == "Key 2"
    model.properties[1].value.should == "value2"
  end

  it "should create empty package view model with just name if metadata is nil (plugin not found scenario)" do
    model = PackageViewModel.new nil, @package
    model.name.should == @package.name
    model.properties.size.should == 0
  end
end
