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
include GoUtil, FormUI

describe "admin/package_definitions/show_with_repository_list.html.erb" do
  before(:each) do

    # package repo setup
    repository1 = PackageRepositoryMother.create("id1", "name1", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
    repository1.setPackages(Packages.new([PackageDefinition.new("pid1", "pname1", nil), PackageDefinition.new("pid2", "pname2", nil)].to_java(PackageDefinition)))
    repository2 = PackageRepositoryMother.create("id2", "name2", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
    repository2.setPackages(Packages.new([PackageDefinition.new("pid3", "pname3", nil), PackageDefinition.new("pid4", "pname4", nil)].to_java(PackageDefinition)))
    @repos = PackageRepositories.new
    @repos.add(repository1)
    @repos.add(repository2)

    # package to pipeline usage map setup
    @packageToPipelineMap = HashMap.new
    packageOnePipelines = ArrayList.new
    packageOnePipelines.add(Pair.new(PipelineConfig.new,BasicPipelineConfigs.new))
    packageThreePipelines = ArrayList.new
    packageThreePipelines.add(Pair.new(PipelineConfig.new,BasicPipelineConfigs.new))
    @packageToPipelineMap.put("pid1",packageOnePipelines)
    @packageToPipelineMap.put("pid3",packageThreePipelines)

    #md5 setup
    assign(:cruise_config, @cruise_config = double("cruise config"))
    @cruise_config.should_receive(:canDeletePackageRepository).at_least(:once).with(anything).and_return(true)
    @cruise_config.should_receive(:getMd5).at_least(:once).and_return("abc")

    # metadata setup
    metadata = PackageConfigurations.new
    metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
    metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
    p1 = ConfigurationProperty.new(ConfigurationKey.new("key1"), ConfigurationValue.new("value1"), nil, nil)
    p2 = ConfigurationProperty.new(ConfigurationKey.new("key2"), ConfigurationValue.new("value2"), nil, nil)
    package = PackageDefinition.new("go", "package-name", Configuration.new([p1, p2].to_java(ConfigurationProperty)))
    model = PackageViewModel.new metadata, package
    assign(:package_configuration, model)
    assign(:package_repositories, @repos)
    assign(:package_to_pipeline_map, @packageToPipelineMap)
  end

  describe "list.html" do
    it "should render package name and package configurations along with listing" do

      in_params(:repo_id => "id1",:package_id => "pid1")

      render

      expect(response.body).to have_selector(".field label", :text => "Package Name")
      expect(response.body).to have_selector(".field input[type='text'][value='package-name']")
      expect(response.body).to have_selector(".field label", :text => "Key 1")
      expect(response.body).to have_selector(".field input[type='text'][value='value1']")
      expect(response.body).to have_selector(".field label", :text => "Key 2")
      expect(response.body).to have_selector(".field input[type='text'][value='value2']")

      expect(response.body).not_to have_selector(".error_message")
    end

    it "should render pipelines used link with delete button disabled when package is used by pipelines" do

      in_params(:repo_id => "id1",:package_id => "pid1")

      render

      expect(response.body).to have_selector("a[id='show_pipelines_used_in']", :text => "Show pipelines using this package")
      expect(response.body).to have_selector("button[id='delete_package'][disabled='disabled'][title='This package is being used in one or more pipeline(s), cannot delete the package']", :text => "Delete")
    end

    it "should render delete button with prompt when package is not used by any pipeline" do
      in_params(:repo_id => "id1",:package_id => "pid2")

      render

      expect(response.body).to have_selector("div.information", :text => "No Pipelines currently use this package")

      Capybara.string(response.body).find("form[action='#{package_definition_delete_path(:repo_id => 'id1', :package_id => 'pid2')}'][id='delete_package_form'][method='post']").tap do |form|
        expect(form).to have_selector("input[name='_method'][type='hidden'][value='delete']")
        expect(form).to have_selector("input[name='config_md5'][type='hidden'][value='abc']")
        form.find("span[id='trigger_package_delete_pid2']") do |span|
          span.find("button[id='delete_button_pid2']") do |button|
            button.find("div[id='warning_prompt']") do |div|
              expect(div).to have_selector("p", :text => "You are about to delete package pname4")
            end
          end
        end
      end
    end
  end
end
