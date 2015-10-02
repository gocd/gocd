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

describe RepoViewModel do
  before (:each) do
    @metadata = PackageConfigurations.new
    @metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
    @metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::REQUIRED, false).with(PackageConfiguration::SECURE, true).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
  end

  it "should create repo view model from metadata when repo is nil" do

    model = RepoViewModel.new @metadata, nil, "yum"
    model.properties.size.should == 2
    model.properties[0].name.should == "key1"
    model.properties[0].display_name.should == "Key 1"
    model.properties[0].value.should == nil
    model.properties[0].is_mandatory.should == true
    model.properties[0].is_secure.should == false
    model.properties[1].name.should == "key2"
    model.properties[1].display_name.should == "Key 2"
    model.properties[1].value.should == nil
    model.properties[1].is_mandatory.should == false
    model.properties[1].is_secure.should == true
    model.errors.isEmpty().should == true
    end

  it "should create repo view model from metadata when repo is provided" do
    secure_property = ConfigurationPropertyMother.create("key2", true, "v2")
    repo = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("key1", false, "v1"), secure_property].to_java(ConfigurationProperty)))
    model = RepoViewModel.new @metadata, repo, "yum"
    model.properties.size.should == 2
    model.properties[0].display_name.should == "Key 1"
    model.properties[0].value.should == "v1"
    model.properties[1].display_name.should == "Key 2"
    model.properties[1].value.should == secure_property.getEncryptedValue().getValue()
  end

  it "should create repo view model from metadata when repo doesn't have all configuration " do
    repo = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("key1", false, "v1")].to_java(ConfigurationProperty)))
    model = RepoViewModel.new @metadata, repo, "yum"
    model.properties.size.should == 2
    model.properties[0].display_name.should == "Key 1"
    model.properties[0].value.should == "v1"
    model.properties[1].display_name.should == "Key 2"
    model.properties[1].value.should == nil
  end

  it "should add error to model if plugin is missing" do
    repo = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("key1", false, "v1")].to_java(ConfigurationProperty)))
    model = RepoViewModel.new nil, repo, "yum"
    model.properties.size.should == 0
    model.errors.isEmpty().should == false
    model.errors.on("pluginId").should == "Associated plugin 'yum' not found. Please contact the Go admin to install the plugin."
  end
end
