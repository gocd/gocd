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
include GoUtil, FormUI

describe "show_with_repository_list.html.erb" do
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
    packageOnePipelines.add(Pair.new(PipelineConfig.new,PipelineConfigs.new))
    packageThreePipelines = ArrayList.new
    packageThreePipelines.add(Pair.new(PipelineConfig.new,PipelineConfigs.new))
    @packageToPipelineMap.put("pid1",packageOnePipelines)
    @packageToPipelineMap.put("pid3",packageThreePipelines)

    #md5 setup
    assigns[:cruise_config] = @cruise_config = mock("cruise config")
    @cruise_config.should_receive(:canDeletePackageRepository).any_number_of_times.with(anything).and_return(true)
    @cruise_config.should_receive(:getMd5).any_number_of_times.and_return("abc")

    # metadata setup
    metadata = PackageConfigurations.new
    metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
    metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
    p1 = ConfigurationProperty.new(ConfigurationKey.new("key1"), ConfigurationValue.new("value1"), nil, nil)
    p2 = ConfigurationProperty.new(ConfigurationKey.new("key2"), ConfigurationValue.new("value2"), nil, nil)
    package = PackageDefinition.new("go", "package-name", Configuration.new([p1, p2].to_java(ConfigurationProperty)))
    model = PackageViewModel.new metadata, package
    assigns[:package_configuration] = model
    assigns[:package_repositories] = @repos
    assigns[:package_to_pipeline_map] = @packageToPipelineMap
  end

  describe "list.html" do
    it "should render package name and package configurations along with listing" do

      in_params(:repo_id => "id1",:package_id => "pid1")

      render "admin/package_definitions/show_with_repository_list.html"

      response.body.should have_tag(".field label", "Package Name")
      response.body.should have_tag(".field input[type='text'][value='package-name']")
      response.body.should have_tag(".field label", "Key 1")
      response.body.should have_tag(".field input[type='text'][value='value1']")
      response.body.should have_tag(".field label", "Key 2")
      response.body.should have_tag(".field input[type='text'][value='value2']")
      response.body.should_not have_tag(".error_message")
    end

    it "should render pipelines used link with delete button disabled when package is used by pipelines" do

      in_params(:repo_id => "id1",:package_id => "pid1")

      render "admin/package_definitions/show_with_repository_list.html"

      response.body.should have_tag("a[id='show_pipelines_used_in']","Show pipelines using this package")
      response.body.should have_tag("button[id='delete_package'][disabled='disabled'][title='This package is being used in one or more pipeline(s), cannot delete the package']","Delete")
    end

    it "should render delete button with prompt when package is not used by any pipeline" do

      in_params(:repo_id => "id1",:package_id => "pid2")

      render "admin/package_definitions/show_with_repository_list.html"

      response.body.should have_tag("div.information","No Pipelines currently use this package")

      response.body do
        with_tag("form[action='#{package_definition_delete_path(:repo_id => 'id1', :package_id => 'pid2')}'][id='delete_package_form'][method='post']") do
          with_tag("input[name='_method'][type='hidden'][value='delete']")
          with_tag("input[name='config_md5'][type='hidden'][value='abc']")
          with_tag("span[id='trigger_package_delete_pid2']") do
            with_tag("button[id='delete_button_pid2']")
            with_tag("div[id='warning_prompt']") do
              with_tag("p", "You are about to delete package pname4")
            end
          end
        end
      end
    end
  end
end