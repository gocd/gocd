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

describe "config.html.erb" do
  describe "config.html" do
    it "should render repo configurations for existing repo" do
      metadata = PackageConfigurations.new
      metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
      metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::REQUIRED, false).with(PackageConfiguration::SECURE, true).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
      repository = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("key1", false, "v1"), ConfigurationPropertyMother.create("key2", true, "v2")].to_java(ConfigurationProperty)))

      config = RepoViewModel.new metadata, repository, nil

      render :partial => "admin/package_repositories/config.html", :locals => {:scope => {:repository_configuration => config, :plugin_id => "yum", :isNewRepo => false}}

      expect(response.body).to have_selector("fieldset legend span", :text => "YUM Repository Configuration")
      expect(response.body).to have_selector(".field label", :text => "Key 1*")
      expect(response.body).to have_selector(".field input[type='hidden'][name='package_repository[configuration][0][configurationKey][name]'][value='key1']")
      expect(response.body).to have_selector(".field input[type='text'][name='package_repository[configuration][0][configurationValue][value]'][value='v1']")

      expect(response.body).to have_selector(".field label", :text => "Key 2")
      expect(response.body).to have_selector(".field input[type='hidden'][name='package_repository[configuration][1][configurationKey][name]'][value='key2']")
      expect(response.body).to have_selector(".field input[type='password'][readonly='readonly'][name='package_repository[configuration][1][configurationValue][value]'][value='2AmGllf3Wbc=']")
      expect(response.body).to have_selector(".field input[type='hidden'][name='package_repository[configuration][1][encryptedValue][value]'][value='2AmGllf3Wbc=']")
      expect(response.body).to have_selector(".field input[type='checkbox'][id='checkbox_field_1'][name='package_repository[configuration][1][isChanged]'][value='1']")
      expect(response.body).to have_selector(".field label[for='checkbox_field_1']")
      expect(response.body).to have_selector(".field label[for='checkbox_field_1'] span", :text=>'Change Key 2')

      expect(response.body).to have_selector("button.submit span", :text => "CHECK CONNECTION")
      expect(response.body).to have_selector("span#repository_connection_message")
    end

    it "should render repo configurations for new repo" do
      metadata = PackageConfigurations.new
      metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
      metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::REQUIRED, false).with(PackageConfiguration::SECURE, true).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
      repository = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("key1", false, "v1"), ConfigurationPropertyMother.create("key2", true, "v2")].to_java(ConfigurationProperty)))

      config = RepoViewModel.new metadata, repository, nil

      render :partial => "admin/package_repositories/config.html", :locals => {:scope => {:repository_configuration => config, :plugin_id => "yum", :isNewRepo => true}}

      expect(response.body).to have_selector("fieldset legend span", :text => "YUM Repository Configuration")
      expect(response.body).to have_selector(".field label", :text => "Key 1*")
      expect(response.body).to have_selector(".field input[type='hidden'][name='package_repository[configuration][0][configurationKey][name]'][value='key1']")
      expect(response.body).to have_selector(".field input[type='text'][name='package_repository[configuration][0][configurationValue][value]'][value='v1']")

      expect(response.body).to have_selector(".field label", :text => "Key 2")
      expect(response.body).to have_selector(".field input[type='hidden'][name='package_repository[configuration][1][configurationKey][name]'][value='key2']")
      expect(response.body).to have_selector(".field input[type='hidden'][name='package_repository[configuration][1][encryptedValue][value]'][value='2AmGllf3Wbc=']")
      expect(response.body).to have_selector(".field input[type='password'][name='package_repository[configuration][1][configurationValue][value]'][value='2AmGllf3Wbc=']")
      expect(response.body).to have_selector(".field input[type='hidden'][name='package_repository[configuration][1][isChanged]'][value='1']")

      expect(response.body).not_to have_selector(".field input[type='checkbox'][id='checkbox_field_1'][name='package_repository[configuration][1][isChanged]'][value='1']")
      expect(response.body).not_to have_selector(".field label[for='checkbox_field_1']")

      expect(response.body).to have_selector("button.submit span", :text => "CHECK CONNECTION")
      expect(response.body).to have_selector("span#repository_connection_message")
    end
  end
end
