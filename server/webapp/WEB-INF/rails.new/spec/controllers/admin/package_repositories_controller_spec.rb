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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe Admin::PackageRepositoriesController do
  include MockRegistryModule, ConfigSaveStubbing

  describe :routes do
    it "should resolve route to the new package-repositories page" do
      {:get => "/admin/package_repositories/new"}.should route_to(:controller => "admin/package_repositories", :action => "new")
      package_repositories_new_path.should == "/admin/package_repositories/new"
    end

    it "should resolve route to the list package-repositories page" do
      {:get => "/admin/package_repositories/list"}.should route_to(:controller => "admin/package_repositories", :action => "list")
      package_repositories_list_path.should == "/admin/package_repositories/list"
    end

    it "should resolve route to the create package-repositories page" do
      {:post => "/admin/package_repositories"}.should route_to(:controller => "admin/package_repositories", :action => "create")
      package_repositories_create_path.should == "/admin/package_repositories"
    end

    it "should resolve route to the edit package-repositories page" do
      {:get => "/admin/package_repositories/abcd-1234/edit"}.should route_to(:controller => "admin/package_repositories", :action => "edit", :id => "abcd-1234")
      package_repositories_edit_path(:id => "abcd-1234").should == "/admin/package_repositories/abcd-1234/edit"
    end

    it "should resolve route to the update package-repositories page" do
      {:put => "/admin/package_repositories/abcd-1234"}.should route_to(:controller => "admin/package_repositories", :action => "update", :id => "abcd-1234")
      package_repositories_update_path(:id => "abcd-1234").should == "/admin/package_repositories/abcd-1234"
    end

    it "should resolve route to plugin config" do
      {:get => "/admin/package_repositories/abcd-1234/config"}.should route_to(:controller => "admin/package_repositories", :action => "plugin_config", :plugin => "abcd-1234")
      package_repositories_plugin_config_path(:plugin => "abcd-1234").should == "/admin/package_repositories/abcd-1234/config"
    end

    it "should resolve route to plugin config for repo" do
      {:get => "/admin/package_repositories/repoid/pluginid/config"}.should route_to(:controller => "admin/package_repositories", :action => "plugin_config_for_repo", :plugin => "pluginid", :id => "repoid")
      package_repositories_plugin_config_for_repo_path(:plugin => "pluginid", :id => "repoid").should == "/admin/package_repositories/repoid/pluginid/config"
    end

    it "should resolve route to check connection for repo" do
      {:get => "/admin/package_repositories/check_connection?id=foo"}.should route_to(:controller => "admin/package_repositories", :action => "check_connection", :id => "foo")
      package_repositories_check_connection_path.should == "/admin/package_repositories/check_connection"
    end

    it "should resolve route to deletion of repo" do
      {:delete => "/admin/package_repositories/repo"}.should route_to(:controller => "admin/package_repositories", :action => "destroy", :id => "repo")
      package_repositories_delete_path(:id => "repo").should == "/admin/package_repositories/repo"
    end
  end

  describe :actions do

    before :each do
      config_validity = mock('config validity')
      config_validity.should_receive(:isValid).and_return(true)
      @go_config_service = mock('go config service')
      controller.stub(:go_config_service).and_return(@go_config_service)
      @go_config_service.should_receive(:checkConfigFileValid).and_return(config_validity)
      @go_config_service.stub(:registry)
      controller.stub(:populate_health_messages)

      @cloner = mock('cloner')
      controller.stub!(:get_cloner_instance).and_return(@cloner)
    end

    describe "new" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= mock('Package Repository Service'))
        @cruise_config = CruiseConfig.new
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @cloner.should_receive(:deepClone).any_number_of_times.with(@cruise_config).and_return(@cruise_config)
        @user = current_user
      end

      it "should render form for addition of package repository" do
        package_repositories = PackageRepositories.new
        @cruise_config.setPackageRepositories(package_repositories)
        get :new
        assigns[:tab_name].should == "package-repositories"
        assigns[:package_repository].should_not be_nil
        assigns[:package_repositories].should == package_repositories
        assigns[:package_to_pipeline_map].should == @cruise_config.getGroups().getPackageUsageInPipelines();
      end
    end

    describe "list" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= mock('Package Repository Service'))
        @cruise_config = CruiseConfig.new
        @cloner.should_receive(:deepClone).any_number_of_times.with(@cruise_config).and_return(@cruise_config)
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @user = current_user
      end

      it "should render page for package repository list" do
        package_repositories = PackageRepositories.new
        @cruise_config.setPackageRepositories(package_repositories)
        get :list
        assigns[:tab_name].should == "package-repositories"
        assigns[:package_repository].should_not be_nil
        assigns[:package_repositories].should == package_repositories
        assigns[:package_to_pipeline_map].should == @cruise_config.getGroups().getPackageUsageInPipelines();
      end
    end

    describe "config" do
      before(:each) do
        @cruise_config = CruiseConfig.new
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @cloner.should_receive(:deepClone).any_number_of_times.with(@cruise_config).and_return(@cruise_config)


        repository1 = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
        repos = PackageRepositories.new
        repos.add(repository1)
        @cruise_config.setPackageRepositories(repos)
        repo_metadata_store = RepositoryMetadataStore.getInstance()
        configurations = PackageConfigurations.new()
        configurations.add(PackageConfiguration.new("k1").with(PackageConfiguration::DISPLAY_NAME, "Key 1").with(PackageConfiguration::DISPLAY_ORDER, 0))
        repo_metadata_store.addMetadataFor("pluginid", configurations)
      end

      it "should get the configuration properties for a given plugin-id" do
        get :plugin_config, :plugin => "pluginid"

        assigns[:repository_configuration].should_not be_nil
        assigns[:repository_configuration].properties[0].display_name.should == "Key 1"
        assigns[:repository_configuration].properties[0].value.should == nil
        assigns[:plugin_id].should == "pluginid"
        assigns[:isNewRepo].should == true
      end

      it "should get the configuration properties with values for a given repo-id associated with package material plugin" do
        get :plugin_config_for_repo, :id => "repo1", :plugin => "pluginid"

        assigns[:repository_configuration].should_not be_nil
        assigns[:repository_configuration].properties.size.should == 1
        assigns[:repository_configuration].properties[0].display_name.should == "Key 1"
        assigns[:repository_configuration].properties[0].value.should == "v1"
        assigns[:plugin_id].should == "pluginid"
        assigns[:isNewRepo].should == false
      end
    end

    describe "create" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= mock('Package Repository Service'))
        @cruise_config = CruiseConfig.new
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @cloner.should_receive(:deepClone).any_number_of_times.with(@cruise_config).and_return(@cruise_config)

        @user = current_user
      end

      it "should save package repository form" do
        package_repository = PackageRepository.new
        package_repository.setId("repo-id")
        PackageRepository.stub(:new).and_return(package_repository)
        @package_repository_service.should_receive(:savePackageRepositoryToConfig).with(package_repository, "1234abcd", @user).and_return(ConfigUpdateAjaxResponse::success("repo-id", 200,  "success"))
        post :create, :config_md5 => "1234abcd", :package_repository => {:name => "name", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}
        response.body.should == "{\"fieldErrors\":{},\"globalErrors\":[],\"message\":\"success\",\"isSuccessful\":true,\"subjectIdentifier\":\"repo-id\",\"redirectUrl\":\"/admin/package_repositories/repo-id/edit\"}"
        flash[:success].should == "success"
        response.response_code.should == 200
        response.location.should == "/admin/package_repositories/repo-id/edit"
        response.headers["Go-Config-Error"].should == nil
      end
      it "should not add flash message when create fails" do
        package_repository = PackageRepository.new
        PackageRepository.stub(:new).and_return(package_repository)
        @package_repository_service.should_receive(:savePackageRepositoryToConfig).with(package_repository, "1234abcd", @user).and_return(ConfigUpdateAjaxResponse::failure(nil, 500, "failed", nil, nil));
        post :create, :config_md5 => "1234abcd", :package_repository => {:name => "name", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}
        flash[:success].should == nil
        response.response_code.should == 500
        response.headers["Go-Config-Error"].should == "failed"
        response.location.should == nil
      end
    end

    describe "edit" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= mock('Package Repository Service'))
        @cruise_config = CruiseConfig.new
        @cloner.should_receive(:deepClone).any_number_of_times.with(@cruise_config).and_return(@cruise_config)

        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @user = current_user
        @repository1 = PackageRepositoryMother.create("abcd-1234", "repo1-name", "pluginid", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
        @repository2 = PackageRepositoryMother.create("with-missing-plugin", "repo2-name", "missing", "version1.0", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
        @cruise_config.setPackageRepositories(PackageRepositories.new([@repository1, @repository2].to_java(PackageRepository)))
        repo_metadata_store = RepositoryMetadataStore.getInstance()
        configurations = PackageConfigurations.new()
        configurations.add(PackageConfiguration.new("k1").with(PackageConfiguration::DISPLAY_NAME, "Key 1").with(PackageConfiguration::DISPLAY_ORDER, 0))
        repo_metadata_store.addMetadataFor("pluginid", configurations)

      end

      it "should render form for editing  package repository" do
        get :edit, :id => "abcd-1234"
        assigns[:package_repository].should == @repository1
        assigns[:repository_configuration].should_not be_nil
        assigns[:repository_configuration].properties.size.should == 1
        assigns[:repository_configuration].properties[0].display_name.should == "Key 1"
        assigns[:repository_configuration].properties[0].value.should == "v1"
        assigns[:package_repositories].should == @cruise_config.getPackageRepositories()
        assigns[:tab_name].should == "package-repositories"
        assigns[:package_to_pipeline_map].should == @cruise_config.getGroups().getPackageUsageInPipelines();
      end

      it "should render error if plugin is missing package repository" do
        get :edit, :id => "with-missing-plugin"
        assigns[:package_repository].should == @repository2
        assigns[:repository_configuration].should_not be_nil
        assigns[:repository_configuration].properties.size.should == 0
        assigns[:errors].size.should == 1
        assigns[:errors].should include("Associated plugin 'missing' not found. Please contact the Go admin to install the plugin.")
        assigns[:package_repositories].should == @cruise_config.getPackageRepositories()
        assigns[:tab_name].should == "package-repositories"
      end

      it "should render 404 page when repo is missing" do
        get :edit, :id => "missing-repo-id"
        response.response_code.should == 404
        assigns[:message].should == "Could not find the repository with id 'missing-repo-id'. It might have been deleted."
        assigns[:status].should == 404
      end
    end

    describe "update" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= mock('Package Repository Service'))
        @cruise_config = CruiseConfig.new
        @cloner.should_receive(:deepClone).any_number_of_times.with(@cruise_config).and_return(@cruise_config)
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @user = current_user
      end

      it "should update package repository form" do
        package_repository = PackageRepository.new
        PackageRepository.stub(:new).and_return(package_repository)
        @package_repository_service.should_receive(:savePackageRepositoryToConfig).with(package_repository, "1234abcd", @user).and_return(ConfigUpdateAjaxResponse::success("id", 200, "success"))
        post :update, :config_md5 => "1234abcd", :id => "id", :package_repository => {:name => "name", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}
        response.body.should == "{\"fieldErrors\":{},\"globalErrors\":[],\"message\":\"success\",\"isSuccessful\":true,\"subjectIdentifier\":\"id\",\"redirectUrl\":\"/admin/package_repositories/id/edit\"}"
        flash[:success].should == "success"
        response.response_code.should == 200
        response.location.should == "/admin/package_repositories/id/edit"
        response.headers["Go-Config-Error"].should == nil
      end

      it "should not add flash message when update fails" do
        package_repository = PackageRepository.new
        PackageRepository.stub(:new).and_return(package_repository)
        fieldErrors = HashMap.new
        fieldErrors.put("field1", Arrays.asList(["error 1"].to_java(java.lang.String)))
        fieldErrors.put("field2", Arrays.asList(["error 2"].to_java(java.lang.String)))
        ajax_response = ConfigUpdateAjaxResponse::failure("id", 500, "failed", fieldErrors, Arrays.asList(["global1", "global2"].to_java(java.lang.String)))

        @package_repository_service.should_receive(:savePackageRepositoryToConfig).with(package_repository, "1234abcd", @user).and_return(ajax_response)
        post :update, :config_md5 => "1234abcd", :id => "id", :package_repository => {:name => "name", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}
        flash[:notice].should == nil
        response.body.should == "{\"fieldErrors\":{\"field2\":[\"error 2\"],\"field1\":[\"error 1\"]},\"globalErrors\":[\"global1\",\"global2\"],\"message\":\"failed\",\"isSuccessful\":false,\"subjectIdentifier\":\"id\"}"
        flash[:success].should == nil
        response.response_code.should == 500
        response.headers["Go-Config-Error"].should == "failed"
        response.location.should == nil
      end
    end

    describe "check connection" do

      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= mock('Package Repository Service'))
        @result = HttpLocalizedOperationResult.new
        HttpLocalizedOperationResult.stub(:new).and_return(@result)
      end

      it "should check connection for given package repository" do
        package_repository = PackageRepositoryMother.create("repo-id", "name", "yum", nil, Configuration.new([ConfigurationPropertyMother.create("key", false, "value")].to_java(ConfigurationProperty)))
        @result.should_receive(:isSuccessful).and_return(true)
        @result.should_receive(:message).with(anything).and_return("Connection OK from plugin.")
        @package_repository_service.should_receive(:checkConnection).with(package_repository, @result)

        get :check_connection, :package_repository => {:name => "name", :repoId => "repo-id", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}

        json = JSON.parse(response.body)
        json["success"].should == "Connection OK from plugin."
        json["error"].should == nil
      end

      it "should show error when check connection fails for given package repository" do
        package_repository = PackageRepositoryMother.create("repo-id", "name", "yum", nil, Configuration.new([ConfigurationPropertyMother.create("key", false, "value")].to_java(ConfigurationProperty)))
        @result.should_receive(:isSuccessful).and_return(false)
        @result.should_receive(:message).twice.with(anything).and_return("Connection To Repo Failed. Bad Url")
        @package_repository_service.should_receive(:checkConnection).with(package_repository, @result)

        get :check_connection, :package_repository => {:name => "name", :repoId => "repo-id", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}

        json = JSON.parse(response.body)
        json["success"].should == nil
        json["error"].should == "Connection To Repo Failed. Bad Url"
      end
    end

    describe :destroy do

      before :each do
        @cruise_config = mock('cruise config')
        @cloner.should_receive(:deepClone).any_number_of_times.with(@cruise_config).and_return(@cruise_config)
        @go_config_service.should_receive(:getConfigForEditing).any_number_of_times.and_return(@cruise_config)
        @go_config_service.should_receive(:getCurrentConfig).any_number_of_times.and_return(@cruise_config)
        @config_md5 = "1234abcd"

        @update_response = mock('update_response')
      end

      it "should delete repository successfully" do
        @update_response.should_receive(:getCruiseConfig).and_return(@cruise_config)
        @update_response.should_receive(:getNode).and_return(@cruise_config)
        @update_response.should_receive(:getSubject).and_return(@cruise_config)
        @update_response.should_receive(:configAfterUpdate).and_return(@cruise_config)
        @update_response.should_receive(:wasMerged).and_return(false)
        @go_config_service.should_receive(:updateConfigFromUI).with(anything, @config_md5, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)).and_return(@update_response)
        stub_service(:flash_message_service).should_receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-uuid")

        delete :destroy, :id => "repo-id", :config_md5 => @config_md5

        response.should redirect_to package_repositories_list_path(:fm => 'random-uuid')
      end

      it "should render error when repository can not be deleted" do
        repository_id = 'some_repository_id'
        @update_response.should_receive(:getCruiseConfig).twice.and_return(@cruise_config)
        @update_response.should_receive(:getNode).and_return(@cruise_config)
        @update_response.should_receive(:getSubject).and_return(@cruise_config)
        @update_response.should_receive(:configAfterUpdate).and_return(@cruise_config)
        plugin_configuration = mock(PluginConfiguration)
        plugin_configuration.should_receive(:getId).and_return(repository_id)
        package_repository = mock(PackageRepository)
        package_repository.should_receive(:getPluginConfiguration).and_return(plugin_configuration)
        package_repositories = mock(PackageRepositories)
        package_repositories.should_receive(:find).with(repository_id).and_return(package_repository)
        @cruise_config.should_receive(:getPackageRepositories).twice.and_return(package_repositories)
        pipeline_groups = mock(PipelineGroups)
        pipeline_groups.should_receive(:getPackageUsageInPipelines).and_return(nil)
        @cruise_config.should_receive(:getGroups).and_return(pipeline_groups)
        @cruise_config.should_receive(:getAllErrorsExceptFor).and_return([])
        @go_config_service.should_receive(:updateConfigFromUI).with(anything, @config_md5, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |action, md5, user, r|
          r.badRequest(LocalizedMessage.string("SAVE_FAILED"))
        end.and_return(@update_response)

        delete :destroy, :id => repository_id, :config_md5 => @config_md5

        assigns[:tab_name].should == "package-repositories"
        assert_template "edit"
        assert_template layout: "admin"
        response.status.should == 400
      end
    end
  end
end
