#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe RepoViewModel do
  before (:each) do
    @metadata = PackageConfigurations.new
    @metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
    @metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::REQUIRED, false).with(PackageConfiguration::SECURE, true).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
  end

  it "should create repo view model from metadata when repo is nil" do

    model = RepoViewModel.new @metadata, nil, "yum"
    expect(model.properties.size).to eq(2)
    expect(model.properties[0].name).to eq("key1")
    expect(model.properties[0].display_name).to eq("Key 1")
    expect(model.properties[0].value).to eq(nil)
    expect(model.properties[0].is_mandatory).to eq(true)
    expect(model.properties[0].is_secure).to eq(false)
    expect(model.properties[1].name).to eq("key2")
    expect(model.properties[1].display_name).to eq("Key 2")
    expect(model.properties[1].value).to eq(nil)
    expect(model.properties[1].is_mandatory).to eq(false)
    expect(model.properties[1].is_secure).to eq(true)
    expect(model.errors.isEmpty()).to eq(true)
    end

  it "should create repo view model from metadata when repo is provided" do
    secure_property = ConfigurationPropertyMother.create("key2", true, "v2")
    repo = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("key1", false, "v1"), secure_property].to_java(ConfigurationProperty)))
    model = RepoViewModel.new @metadata, repo, "yum"
    expect(model.properties.size).to eq(2)
    expect(model.properties[0].display_name).to eq("Key 1")
    expect(model.properties[0].value).to eq("v1")
    expect(model.properties[1].display_name).to eq("Key 2")
    expect(model.properties[1].value).to eq(secure_property.getEncryptedValue())
  end

  it "should create repo view model from metadata when repo doesn't have all configuration " do
    repo = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("key1", false, "v1")].to_java(ConfigurationProperty)))
    model = RepoViewModel.new @metadata, repo, "yum"
    expect(model.properties.size).to eq(2)
    expect(model.properties[0].display_name).to eq("Key 1")
    expect(model.properties[0].value).to eq("v1")
    expect(model.properties[1].display_name).to eq("Key 2")
    expect(model.properties[1].value).to eq(nil)
  end

  it "should add error to model if plugin is missing" do
    repo = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("key1", false, "v1")].to_java(ConfigurationProperty)))
    model = RepoViewModel.new nil, repo, "yum"
    expect(model.properties.size).to eq(0)
    expect(model.errors.isEmpty()).to eq(false)
    expect(model.errors.on("pluginId")).to eq("Plugin 'yum' not found.")
  end
end
