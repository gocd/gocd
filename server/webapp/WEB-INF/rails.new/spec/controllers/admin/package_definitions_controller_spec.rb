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

describe Admin::PackageDefinitionsController do
  include ConfigSaveStubbing
  include MockRegistryModule
  describe "routes" do
    it "should resolve route to the new package_definitions page" do
      {:get => "/admin/package_definitions/repoid/new"}.should route_to(:controller => "admin/package_definitions", :action => "new", :repo_id => "repoid")
      package_definitions_new_path(:repo_id => "repoid").should == "/admin/package_definitions/repoid/new"
    end

    it "should resolve route to the new package_definitions page" do
      {:get => "/admin/package_definitions/repoid/new_for_new_pipeline_wizard"}.should route_to(:controller => "admin/package_definitions", :action => "new_for_new_pipeline_wizard", :repo_id => "repoid")
      package_definitions_new_for_new_pipeline_wizard_path(:repo_id => "repoid").should == "/admin/package_definitions/repoid/new_for_new_pipeline_wizard"
    end

    it "should route to package_config action" do
      {:get => "/admin/package_definitions/repoid/packageid"}.should route_to(:controller => "admin/package_definitions", :action => "show", :repo_id => "repoid", :package_id => "packageid")
      package_definitions_show_path(:repo_id => "repoid", :package_id => "packageid").should == "/admin/package_definitions/repoid/packageid"
    end

    it "should route to package_config action" do
      {:get => "/admin/package_definitions/repoid/packageid/for_new_pipeline_wizard"}.should route_to(:controller => "admin/package_definitions", :action => "show_for_new_pipeline_wizard", :repo_id => "repoid", :package_id => "packageid")
      package_definitions_show_for_new_pipeline_wizard_path(:repo_id => "repoid", :package_id => "packageid").should == "/admin/package_definitions/repoid/packageid/for_new_pipeline_wizard"
    end

    it "should route to package_config action with repository listing" do
      {:get => "/admin/package_definitions/repoid/packageid/with_repository_list"}.should route_to(:controller => "admin/package_definitions", :action => "show_with_repository_list", :repo_id => "repoid", :package_id => "packageid")
      package_definitions_show_with_repository_list_path(:repo_id => "repoid", :package_id => "packageid").should == "/admin/package_definitions/repoid/packageid/with_repository_list"
    end

    it "should route to pipeline used in" do
      {:get => "/admin/package_definitions/repoid/packageid/pipelines_used_in"}.should route_to(:controller => "admin/package_definitions", :action => "pipelines_used_in", :repo_id => "repoid", :package_id => "packageid")
      pipelines_used_in_path(:repo_id => "repoid", :package_id => "packageid").should == "/admin/package_definitions/repoid/packageid/pipelines_used_in"
    end

    it "should route to delete package" do
      {:delete => "/admin/package_definitions/repoid/packageid"}.should route_to(:controller => "admin/package_definitions", :action => "destroy", :repo_id => "repoid", :package_id => "packageid")
      package_definition_delete_path(:repo_id => "repoid", :package_id => "packageid").should == "/admin/package_definitions/repoid/packageid"
    end

    it "should resolve route to check connection for repo" do
      expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      expect({:post => "admin/package_definitions/check_connection"}).to route_to(:controller => "admin/package_definitions", :action => "check_connection")
      expect(package_definition_check_connection_path).to eq("/admin/package_definitions/check_connection")
    end

  end

  describe "action" do
    before(:each) do
      controller.stub(:populate_config_validity)
      @cruise_config = BasicCruiseConfig.new()
      @go_config_service = stub_service(:go_config_service)
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      @go_config_service.stub(:getConfigForEditing).and_return(@cruise_config)
      repository1 = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
      repository2 = PackageRepositoryMother.create("repo2", "repo2-name", "invalid-pluginid", "version1.0", Configuration.new)
      @pkg = PackageDefinitionMother.create("pkg3", "package3-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "p3v2"), ConfigurationPropertyMother.create("k3", true, "secure")].to_java(ConfigurationProperty)), repository1)
      repo2_pkg = PackageDefinitionMother.create("pkg4", "package4-name", Configuration.new, repository2)
      repository1.setPackages(Packages.new([@pkg].to_java(PackageDefinition)))
      repository2.setPackages(Packages.new([repo2_pkg].to_java(PackageDefinition)))
      @package_repositories = PackageRepositories.new([repository1, repository2].to_java(PackageRepository))
      @cruise_config.setPackageRepositories(@package_repositories)

      @package_metadata_store = double(PackageMetadataStore)
      PackageMetadataStore.stub(:getInstance).and_return(@package_metadata_store)
      @package_configurations = PackageConfigurations.new()
      @package_configurations.add(PackageConfiguration.new("k2").with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
      @package_configurations.add(PackageConfiguration.new("k3").with(PackageConfiguration::DISPLAY_NAME, "Key 3").with(PackageConfiguration::SECURE, true))
    end

    describe :show do
      it "should show package definition details" do
        @package_metadata_store.should_receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :show, :repo_id => "repo1", :package_id => "pkg3"

        assigns[:package_configuration].properties.size.should == 1
        assigns[:package_configuration].properties[0].display_name.should == "Key 2"
        assigns[:package_configuration].properties[0].value.should == "p3v2"
        controller.should render_template("show")
        assert_template layout: false
      end

      it "should render error for invalid plugin" do
        @package_metadata_store.should_receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :show, :repo_id => "repo2", :package_id => "pkg4"

        assigns[:errors].should == "Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin."
      end

      it "should render error if repository not found" do
        get :show, :repo_id => "deleted-repo", :package_id => "pkg4"

        assigns[:errors].should == "Could not find the repository with id 'deleted-repo'. It might have been deleted."
      end
    end

    describe :show_for_new_pipeline_wizard do
      it "should show package definition details" do
        @package_metadata_store.should_receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :show_for_new_pipeline_wizard, :repo_id => "repo1", :package_id => "pkg3"

        assigns[:package_configuration].properties.size.should == 1
        assigns[:package_configuration].properties[0].display_name.should == "Key 2"
        assigns[:package_configuration].properties[0].value.should == "p3v2"
        controller.should render_template("show_for_new_pipeline_wizard")
        assert_template layout: false
      end

      it "should render error for invalid plugin" do
        @package_metadata_store.should_receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :show_for_new_pipeline_wizard, :repo_id => "repo2", :package_id => "pkg4"

        assigns[:errors].should == "Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin."
      end

      it "should render error if repository not found" do
        get :show_for_new_pipeline_wizard, :repo_id => "deleted-repo", :package_id => "pkg4"

        assigns[:errors].should == "Could not find the repository with id 'deleted-repo'. It might have been deleted."
      end
    end

    describe :show_wih_repository_list do
      it "should show package definition details along with package repository listing" do
        @package_metadata_store.should_receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :show_with_repository_list, :repo_id => "repo1", :package_id => "pkg3"

        assigns[:package_configuration].properties.size.should == 1
        assigns[:package_configuration].properties[0].display_name.should == "Key 2"
        assigns[:package_configuration].properties[0].value.should == "p3v2"
        assigns[:package_repositories].should == @package_repositories
        assigns[:package_to_pipeline_map].should == @cruise_config.getGroups().getPackageUsageInPipelines();
        controller.should render_template("show_with_repository_list")
        assert_template layout: "admin"
      end

      it "should render error for invalid plugin" do
        @package_metadata_store.should_receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :show_with_repository_list, :repo_id => "repo2", :package_id => "pkg4"

        assigns[:errors].should == "Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin."
      end

      it "should render 404 when repo is missing" do
        get :show_with_repository_list, :repo_id => "repo3", :package_id => "pkg4"
        response.response_code.should == 404
        assigns[:message].should == "Could not find package id 'pkg4' under repository with id 'repo3'. It might have been deleted."
        assigns[:status].should == 404
      end

      it "should render 404 when package is missing" do
        get :show_with_repository_list, :repo_id => "repo2", :package_id => "pkg5"
        response.response_code.should == 404
        assigns[:message].should == "Could not find package id 'pkg5' under repository with id 'repo2'. It might have been deleted."
        assigns[:status].should == 404
      end

      it "should render package configuration with just name when plugin is missing" do
        @package_metadata_store.should_receive(:getMetadata).with("invalid-pluginid").and_return(nil)
        get :show_with_repository_list, :repo_id => "repo2", :package_id => "pkg4"
        assigns[:errors].should == "Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin."
        assigns[:package_configuration].name == "yum"
        assigns[:package_configuration].properties.size.should == 0
      end
    end

    describe :pipelines_used_in do
      it "should show pipelines used in for a given package" do

        packageOne = PackageMaterialConfig.new("package-id-one");
        packageTwo = PackageMaterialConfig.new("package-id-two");

        p1 = PipelineConfig.new(CaseInsensitiveString.new("pipeline1"), MaterialConfigs.new([packageOne, packageTwo].to_java(PackageMaterialConfig)), [StageConfig.new].to_java(StageConfig));
        p2 = PipelineConfig.new(CaseInsensitiveString.new("pipeline2"), MaterialConfigs.new([packageTwo].to_java(PackageMaterialConfig)), [StageConfig.new].to_java(StageConfig));

        groupOne = BasicPipelineConfigs.new([p1].to_java(PipelineConfig));
        groupTwo = BasicPipelineConfigs.new([p2].to_java(PipelineConfig));

        @cruise_config.getGroups.add(groupOne)
        @cruise_config.getGroups.add(groupTwo)


        get :pipelines_used_in, :repo_id => "repo-id", :package_id => "package-id-one"

        assigns[:pipelines_with_group].get(0).first.should == p1;
        assigns[:pipelines_with_group].get(0).last.should == groupOne;
        controller.should render_template("pipelines_used_in")
        assert_template layout: false
      end
    end


    describe :new do
      it "should render template for new package definition" do
        @package_metadata_store.should_receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :new, :repo_id => "repo1"

        assigns[:package_configuration].properties.size.should == 2
        assigns[:package_configuration].properties[0].display_name.should == "Key 2"
        assigns[:package_configuration].properties[0].value.should == nil
        assigns[:package_configuration].properties[1].display_name.should == "Key 3"
        assigns[:package_configuration].properties[1].value.should == nil
        response.should render_template "new"
        assert_template layout: false
      end

      it "should render error for invalid plugin" do
        @package_metadata_store.should_receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :new, :repo_id => "repo2"

        assigns[:errors].should == "Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin."
      end

      it "should render error if repository not found" do
        get :new, :repo_id => "deleted-repo"

        assigns[:errors].should == "Could not find the repository with id 'deleted-repo'. It might have been deleted."
      end
    end

    describe :new_for_new_pipeline_wizard do
      it "should render template for new package definition" do
        @package_metadata_store.should_receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        get :new_for_new_pipeline_wizard, :repo_id => "repo1"

        assigns[:package_configuration].properties.size.should == 2
        assigns[:package_configuration].properties[0].display_name.should == "Key 2"
        assigns[:package_configuration].properties[0].value.should == nil
        assigns[:package_configuration].properties[1].display_name.should == "Key 3"
        assigns[:package_configuration].properties[1].value.should == nil
        response.should render_template "new_for_new_pipeline_wizard"
        assert_template layout: false
      end

      it "should render error for invalid plugin" do
        @package_metadata_store.should_receive(:getMetadata).with("invalid-pluginid").and_return(nil)

        get :new_for_new_pipeline_wizard, :repo_id => "repo2"

        assigns[:errors].should == "Associated plugin 'invalid-pluginid' not found. Please contact the Go admin to install the plugin."
      end

      it "should render error if repository not found" do
        get :new_for_new_pipeline_wizard, :repo_id => "deleted-repo"

        assigns[:errors].should == "Could not find the repository with id 'deleted-repo'. It might have been deleted."
      end
    end

    describe :destroy do
      it "should delete given package" do
        stub_save_for_success
        stub_service(:flash_message_service).should_receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-uuid")
        @package_metadata_store.should_receive(:getMetadata).with("pluginid").and_return(@package_configurations)

        delete :destroy, :repo_id => "repo1", :package_id => "pkg3", :config_md5 => "1234abcd"

        @cruise_config.getPackageRepositories.find("repo1").getPackages.size.should ==0
        assert_update_command ::ConfigUpdate::SaveAsGroupAdmin, ConfigUpdate::CheckIsGroupAdmin
        assert_template layout: false
      end
    end

    describe :check_connection do
      before(:each) do
        controller.stub(:package_definition_service).with().and_return(@package_definition_service= double('Package Definition Service'))
        @result = HttpLocalizedOperationResult.new
        HttpLocalizedOperationResult.stub(:new).and_return(@result)
      end

      it "should check connection for given package definition" do
        configuration = Configuration.new([ConfigurationPropertyMother.create("key1", false, "value1"), ConfigurationPropertyMother.create("key2", false, "value2")].to_java(ConfigurationProperty))
        expected_pkg = PackageDefinitionMother.create(nil, "pkg-name", configuration, @package_repositories.find("repo1"))
        @go_config_service.should_receive(:getCurrentConfig).and_return(@cruise_config)
        @result.should_receive(:isSuccessful).and_return(true)
        @result.should_receive(:message).with(anything).and_return("Connection OK from plugin.")
        @package_definition_service.should_receive(:check_connection).with(expected_pkg, @result)

        get :check_connection, :material => {:package_definition => {:repositoryId => "repo1", :name => "pkg-name", :configuration => {"0" => configuration_for("key1", "value1"), "1" => configuration_for("key2", "value2")}}}

        json = JSON.parse(response.body)
        json["success"].should == "Connection OK from plugin."
        json["error"].should == nil
      end

      it "should give error if check connection fails for given package definition" do
        pkg_params = {"package_definition" => {"repositoryId" => 'repository_id'}}
        repositories = double("repositories")
        package_repository = double('package_repository')
        package_definition = double('package_definition')
        package_repository.should_receive(:findOrCreatePackageDefinition).with(pkg_params).and_return(package_definition)
        repositories.should_receive(:find).with('repository_id').and_return(package_repository)
        cruise_config = double("cruise config")
        cruise_config.should_receive(:getPackageRepositories).and_return(repositories)
        @go_config_service.should_receive(:getCurrentConfig).and_return(cruise_config)
        @package_definition_service.should_receive(:check_connection).with(package_definition, an_instance_of(HttpLocalizedOperationResult)) do |p, r|
          # we don't really care about the error itself. Just the fact that an error occurred. Hence the PACKAGE_CHECK_FAILED error being used here. (Sachin)
          r.badRequest(LocalizedMessage.string("PACKAGE_CHECK_FAILED", "foo"))
        end

        get :check_connection, :material => pkg_params

        json = JSON.parse(response.body)
        json["success"].should == nil
        json["error"].should == "Package check Failed. Reason(s): foo"
      end

    end
  end

  private

  def configuration_for name, value
    {:configurationKey => {:name => name}, :configurationValue => {:value => value}}
  end

end
