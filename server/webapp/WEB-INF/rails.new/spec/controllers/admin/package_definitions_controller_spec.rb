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

require 'rails_helper'

describe Admin::PackageDefinitionsController do
  include ConfigSaveStubbing
  include MockRegistryModule

  before :each do
    allow(controller).to receive(:populate_config_validity)
    @go_config_service = stub_service(:go_config_service)
    allow(controller).to receive(:package_definition_service).with(no_args).and_return(@package_definition_service= double('Package Definition Service'))
  end

  describe "action" do
    before(:each) do
      @cruise_config = BasicCruiseConfig.new()
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      allow(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
      repository1 = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
      repository2 = PackageRepositoryMother.create("repo2", "repo2-name", "invalid-pluginid", "version1.0", Configuration.new)
      @pkg = PackageDefinitionMother.create("pkg3", "package3-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "p3v2"), ConfigurationPropertyMother.create("k3", true, "secure")].to_java(ConfigurationProperty)), repository1)
      repo2_pkg = PackageDefinitionMother.create("pkg4", "package4-name", Configuration.new, repository2)
      repository1.setPackages(Packages.new([@pkg].to_java(PackageDefinition)))
      repository2.setPackages(Packages.new([repo2_pkg].to_java(PackageDefinition)))
      @package_repositories = PackageRepositories.new([repository1, repository2].to_java(PackageRepository))
      @cruise_config.setPackageRepositories(@package_repositories)

      @package_metadata_store = double(PackageMetadataStore)
      allow(PackageMetadataStore).to receive(:getInstance).and_return(@package_metadata_store)
      @package_configurations = PackageConfigurations.new()
      @package_configurations.add(PackageConfiguration.new("k2").with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
      @package_configurations.add(PackageConfiguration.new("k3").with(PackageConfiguration::DISPLAY_NAME, "Key 3").with(PackageConfiguration::SECURE, true))
    end

    describe "show" do
      it "should show package definition details" do
        expect(@package_metadata_store).to receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :show, params: { :repo_id => "repo1", :package_id => "pkg3" }

        expect(assigns[:package_configuration].properties.size).to eq(1)
        expect(assigns[:package_configuration].properties[0].display_name).to eq("Key 2")
        expect(assigns[:package_configuration].properties[0].value).to eq("p3v2")
        expect(controller).to render_template("show")
        assert_template layout: false
      end

      it "should render error for invalid plugin" do
        expect(@package_metadata_store).to receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :show, params: { :repo_id => "repo2", :package_id => "pkg4" }

        expect(assigns[:errors]).to eq("Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin.")
      end

      it "should render error if repository not found" do
        get :show, params: { :repo_id => "deleted-repo", :package_id => "pkg4" }

        expect(assigns[:errors]).to eq("Could not find the repository with id 'deleted-repo'. It might have been deleted.")
      end
    end

    describe "show_for_new_pipeline_wizard" do
      it "should show package definition details" do
        expect(@package_metadata_store).to receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :show_for_new_pipeline_wizard, params: { :repo_id => "repo1", :package_id => "pkg3" }

        expect(assigns[:package_configuration].properties.size).to eq(1)
        expect(assigns[:package_configuration].properties[0].display_name).to eq("Key 2")
        expect(assigns[:package_configuration].properties[0].value).to eq("p3v2")
        expect(controller).to render_template("show_for_new_pipeline_wizard")
        assert_template layout: false
      end

      it "should render error for invalid plugin" do
        expect(@package_metadata_store).to receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :show_for_new_pipeline_wizard, params: { :repo_id => "repo2", :package_id => "pkg4" }

        expect(assigns[:errors]).to eq("Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin.")
      end

      it "should render error if repository not found" do
        get :show_for_new_pipeline_wizard, params: { :repo_id => "deleted-repo", :package_id => "pkg4" }

        expect(assigns[:errors]).to eq("Could not find the repository with id 'deleted-repo'. It might have been deleted.")
      end
    end

    describe "show_wih_repository_list" do
      it "should show package definition details along with package repository listing" do
        expect(@package_metadata_store).to receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :show_with_repository_list, params: { :repo_id => "repo1", :package_id => "pkg3" }

        expect(assigns[:package_configuration].properties.size).to eq(1)
        expect(assigns[:package_configuration].properties[0].display_name).to eq("Key 2")
        expect(assigns[:package_configuration].properties[0].value).to eq("p3v2")
        expect(assigns[:package_repositories]).to eq(@package_repositories)
        expect(assigns[:package_to_pipeline_map]).to eq(@cruise_config.getGroups().getPackageUsageInPipelines());
        expect(controller).to render_template("show_with_repository_list")
        assert_template layout: "admin"
      end

      it "should render error for invalid plugin" do
        expect(@package_metadata_store).to receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :show_with_repository_list, params: { :repo_id => "repo2", :package_id => "pkg4" }

        expect(assigns[:errors]).to eq("Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin.")
      end

      it "should render 404 when repo is missing" do
        get :show_with_repository_list, params: { :repo_id => "repo3", :package_id => "pkg4" }
        expect(response.response_code).to eq(404)
        expect(assigns[:message]).to eq("Could not find package id 'pkg4' under repository with id 'repo3'. It might have been deleted.")
        expect(assigns[:status]).to eq(404)
      end

      it "should render 404 when package is missing" do
        get :show_with_repository_list, params: { :repo_id => "repo2", :package_id => "pkg5" }
        expect(response.response_code).to eq(404)
        expect(assigns[:message]).to eq("Could not find package id 'pkg5' under repository with id 'repo2'. It might have been deleted.")
        expect(assigns[:status]).to eq(404)
      end

      it "should render package configuration with just name when plugin is missing" do
        expect(@package_metadata_store).to receive(:getMetadata).with("invalid-pluginid").and_return(nil)
        get :show_with_repository_list, params: { :repo_id => "repo2", :package_id => "pkg4" }
        expect(assigns[:errors]).to eq("Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin.")
        assigns[:package_configuration].name == "yum"
        expect(assigns[:package_configuration].properties.size).to eq(0)
      end
    end

    describe "pipelines_used_in" do
      it "should show pipelines used in for a given package" do

        packageOne = PackageMaterialConfig.new("package-id-one");
        packageTwo = PackageMaterialConfig.new("package-id-two");

        p1 = PipelineConfig.new(CaseInsensitiveString.new("pipeline1"), MaterialConfigs.new([packageOne, packageTwo].to_java(PackageMaterialConfig)), [StageConfig.new].to_java(StageConfig));
        p2 = PipelineConfig.new(CaseInsensitiveString.new("pipeline2"), MaterialConfigs.new([packageTwo].to_java(PackageMaterialConfig)), [StageConfig.new].to_java(StageConfig));

        groupOne = BasicPipelineConfigs.new([p1].to_java(PipelineConfig));
        groupTwo = BasicPipelineConfigs.new([p2].to_java(PipelineConfig));

        @cruise_config.getGroups.add(groupOne)
        @cruise_config.getGroups.add(groupTwo)


        get :pipelines_used_in, params: { :repo_id => "repo-id", :package_id => "package-id-one" }

        expect(assigns[:pipelines_with_group].get(0).first).to eq(p1);
        expect(assigns[:pipelines_with_group].get(0).last).to eq(groupOne);
        expect(controller).to render_template("pipelines_used_in")
        assert_template layout: false
      end
    end


    describe "new" do
      it "should render template for new package definition" do
        expect(@package_metadata_store).to receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :new, params: { :repo_id => "repo1" }

        expect(assigns[:package_configuration].properties.size).to eq(2)
        expect(assigns[:package_configuration].properties[0].display_name).to eq("Key 2")
        expect(assigns[:package_configuration].properties[0].value).to eq(nil)
        expect(assigns[:package_configuration].properties[1].display_name).to eq("Key 3")
        expect(assigns[:package_configuration].properties[1].value).to eq(nil)
        expect(response).to render_template "new"
        assert_template layout: false
      end

      it "should render error for invalid plugin" do
        expect(@package_metadata_store).to receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :new, params: { :repo_id => "repo2" }

        expect(assigns[:errors]).to eq("Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin.")
      end

      it "should render error if repository not found" do
        get :new, params: { :repo_id => "deleted-repo" }

        expect(assigns[:errors]).to eq("Could not find the repository with id 'deleted-repo'. It might have been deleted.")
      end
    end

    describe "new_for_new_pipeline_wizard" do
      it "should render template for new package definition" do
        expect(@package_metadata_store).to receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :new_for_new_pipeline_wizard, params: { :repo_id => "repo1" }

        expect(assigns[:package_configuration].properties.size).to eq(2)
        expect(assigns[:package_configuration].properties[0].display_name).to eq("Key 2")
        expect(assigns[:package_configuration].properties[0].value).to eq(nil)
        expect(assigns[:package_configuration].properties[1].display_name).to eq("Key 3")
        expect(assigns[:package_configuration].properties[1].value).to eq(nil)
        expect(response).to render_template "new_for_new_pipeline_wizard"
        assert_template layout: false
      end

      it "should render error for invalid plugin" do
        expect(@package_metadata_store).to receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :new_for_new_pipeline_wizard, params: { :repo_id => "repo2" }

        expect(assigns[:errors]).to eq("Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin.")
      end

      it "should render error if repository not found" do
        get :new_for_new_pipeline_wizard, params: { :repo_id => "deleted-repo" }

        expect(assigns[:errors]).to eq("Could not find the repository with id 'deleted-repo'. It might have been deleted.")
      end
    end

    describe "destroy" do
      it "should delete given package" do
        stub_save_for_success
        expect(stub_service(:flash_message_service)).to receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-uuid")
        expect(@package_metadata_store).to receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        delete :destroy, params: { :repo_id => "repo1", :package_id => "pkg3", :config_md5 => "1234abcd" }

        expect(@cruise_config.getPackageRepositories.find("repo1").getPackages.size).to eq(0)
        assert_update_command ::ConfigUpdate::SaveAsGroupAdmin, ConfigUpdate::CheckIsGroupAdmin
        assert_template layout: false
      end
    end

    describe "check_connection" do
      before(:each) do
        @result = double(HttpLocalizedOperationResult)
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)
      end

      it "should check connection for given package definition" do
        configuration = Configuration.new([ConfigurationPropertyMother.create("key1", false, "value1"), ConfigurationPropertyMother.create("key2", false, "value2")].to_java(ConfigurationProperty))
        expected_pkg = PackageDefinitionMother.create(nil, "pkg-name", configuration, @package_repositories.find("repo1"))
        expect(@go_config_service).to receive(:getCurrentConfig).and_return(@cruise_config)
        expect(@result).to receive(:isSuccessful).and_return(true)
        expect(@result).to receive(:message).with(anything).and_return("Connection OK from plugin.")
        expect(@package_definition_service).to receive(:check_connection).with(expected_pkg, @result)

        get :check_connection, params: { :material => {:package_definition => {:repositoryId => "repo1", :name => "pkg-name", :configuration => {"0" => configuration_for("key1", "value1"), "1" => configuration_for("key2", "value2")}}} }

        json = JSON.parse(response.body)
        expect(json["success"]).to eq("Connection OK from plugin.")
        expect(json["error"]).to eq(nil)
      end

      it "should give error if check connection fails for given package definition" do
        pkg_params = {"package_definition" => {"repositoryId" => 'repository_id'}}
        repositories = double("repositories")
        package_repository = double('package_repository')
        package_definition = double('package_definition')
        expect(package_repository).to receive(:findOrCreatePackageDefinition).with(pkg_params).and_return(package_definition)
        expect(repositories).to receive(:find).with('repository_id').and_return(package_repository)
        cruise_config = double("cruise config")
        expect(cruise_config).to receive(:getPackageRepositories).and_return(repositories)
        expect(@go_config_service).to receive(:getCurrentConfig).and_return(cruise_config)
        expect(@package_definition_service).to receive(:check_connection).with(package_definition, @result) do |p, r|
          # we don't really care about the error itself. Just the fact that an error occurred. Hence the PACKAGE_CHECK_FAILED error being used here. (Sachin)
          allow(r).to receive(:message).and_return("Package check Failed. Reason(s): foo")
          allow(r).to receive(:isSuccessful).and_return(false)
        end

        get :check_connection, params: { :material => pkg_params }

        json = JSON.parse(response.body)
        expect(json["success"]).to eq(nil)
        expect(json["error"]).to eq("Package check Failed. Reason(s): foo")
      end

    end
  end

  private

  def configuration_for name, value
    {:configurationKey => {:name => name}, :configurationValue => {:value => value}}
  end

end
