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

describe Admin::MaterialHelper do
  include Admin::MaterialHelper, RailsLocalizer

  describe "material options in new pipeline wizard" do
    it "should populate options correctly" do
      material_options_map = material_options
      material_options_map.size.should == 7
      material_options_map["Subversion"].should == SvnMaterialConfig::TYPE
      material_options_map["Git"].should == GitMaterialConfig::TYPE
      material_options_map["Mercurial"].should == HgMaterialConfig::TYPE
      material_options_map["Perforce"].should == P4MaterialConfig::TYPE
      material_options_map["Team Foundation Server"].should == TfsMaterialConfig::TYPE
      material_options_map["Pipeline"].should == DependencyMaterialConfig::TYPE
      material_options_map["Package"].should == PackageMaterialConfig::TYPE
    end
  end

  describe "dropdown options for package repositories" do
    before :each do
      @original_cruise_config = BasicCruiseConfig.new
      valid_plugin_id = "pluginid"
      invalid_plugin_id = "invalid-pluginid"
      repository1 = PackageRepositoryMother.create("repo1", "repo1-name", valid_plugin_id, "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
      repository2 = PackageRepositoryMother.create("repo2", "repo2-name", valid_plugin_id, "version2.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
      repository3 = PackageRepositoryMother.create("repo3", "repo3-name", invalid_plugin_id, "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
      pkg1 = PackageDefinitionMother.create("pkg1", "package1-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "v2")].to_java(ConfigurationProperty)), repository1)
      pkg3 = PackageDefinitionMother.create("pkg3", "package3-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "v2")].to_java(ConfigurationProperty)), repository1)
      pkg2 = PackageDefinitionMother.create("pkg2", "package2-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "v2")].to_java(ConfigurationProperty)), repository2)
      pkg4 = PackageDefinitionMother.create("pkg4", "package4-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "v2")].to_java(ConfigurationProperty)), repository2)
      repository1.setPackages(Packages.new([pkg1, pkg3].to_java(PackageDefinition)))
      repository2.setPackages(Packages.new([pkg2, pkg4].to_java(PackageDefinition)))
      @original_cruise_config.setPackageRepositories(PackageRepositories.new([repository1, repository2, repository3].to_java(PackageRepository)))

      metadata_store = double("RepositoryMetadataStore")
      RepositoryMetadataStore.stub(:getInstance).and_return(metadata_store)
      metadata_store.stub(:getMetadata).with(valid_plugin_id).and_return(PackageConfigurations.new)
      metadata_store.stub(:getMetadata).with(invalid_plugin_id).and_return(nil)

    end

    it "should get a map of repos with corresponding packages" do
      repository_packages_map_from_config.size.should == 3
      repository_packages_map_from_config.should == {"repo1" => {:name => "repo1-name", :plugin_id => "pluginid", :is_plugin_missing => false, :packages => [{:id => "pkg1", :name => "package1-name"},{:id => "pkg3", :name => "package3-name"}]},
                                                     "repo2" => {:name => "repo2-name", :plugin_id => "pluginid", :is_plugin_missing => false, :packages => [{:id => "pkg2", :name => "package2-name"},{:id => "pkg4", :name => "package4-name"}]},
                                                     "repo3" => {:name => "repo3-name", :plugin_id => "invalid-pluginid", :is_plugin_missing => true, :packages => []}
      }
    end

    it "should get all plugins with the select one for dropdown option" do
      metadataStore = double("RepositoryMetadataStore")
      RepositoryMetadataStore.stub(:getInstance).and_return(metadataStore)
      metadataStore.stub(:getPlugins).and_return(Arrays.asList(["P1", "P2"].to_java(java.lang.String)))

      package_material_plugins.size.should == 3
      package_material_plugins.should include(["[Select]", ""],"P1", "P2")
    end
  end
end
