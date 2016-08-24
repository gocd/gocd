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

describe Admin::PackageRepositoriesController do
  include MockRegistryModule, ConfigSaveStubbing

  describe :routes do
    it "should resolve route to the new package-repositories page" do
      expect({:get => "/admin/package_repositories/new"}).to route_to(:controller => "admin/package_repositories", :action => "new")
      expect(package_repositories_new_path).to eq("/admin/package_repositories/new")
    end

    it "should resolve route to the list package-repositories page" do
      expect({:get => "/admin/package_repositories/list"}).to route_to(:controller => "admin/package_repositories", :action => "list")
      expect(package_repositories_list_path).to eq("/admin/package_repositories/list")
    end

    it "should resolve route to the create package-repositories page" do
      expect({:post => "/admin/package_repositories"}).to route_to(:controller => "admin/package_repositories", :action => "create")
      expect(package_repositories_create_path).to eq("/admin/package_repositories")
    end

    it "should resolve route to the edit package-repositories page" do
      expect({:get => "/admin/package_repositories/abcd-1234/edit"}).to route_to(:controller => "admin/package_repositories", :action => "edit", :id => "abcd-1234")
      expect(package_repositories_edit_path(:id => "abcd-1234")).to eq("/admin/package_repositories/abcd-1234/edit")
    end

    it "should resolve route to the update package-repositories page" do
      expect({:put => "/admin/package_repositories/abcd-1234"}).to route_to(:controller => "admin/package_repositories", :action => "update", :id => "abcd-1234")
      expect(package_repositories_update_path(:id => "abcd-1234")).to eq("/admin/package_repositories/abcd-1234")
    end

    it "should resolve route to plugin config" do
      expect({:get => "/admin/package_repositories/abcd-1234/config"}).to route_to(:controller => "admin/package_repositories", :action => "plugin_config", :plugin => "abcd-1234")
      expect(package_repositories_plugin_config_path(:plugin => "abcd-1234")).to eq("/admin/package_repositories/abcd-1234/config")
    end

    it "should resolve route to plugin config for repo" do
      expect({:get => "/admin/package_repositories/repoid/pluginid/config"}).to route_to(:controller => "admin/package_repositories", :action => "plugin_config_for_repo", :plugin => "pluginid", :id => "repoid")
      expect(package_repositories_plugin_config_for_repo_path(:plugin => "pluginid", :id => "repoid")).to eq("/admin/package_repositories/repoid/pluginid/config")
    end

    it "should resolve route to check connection for repo" do
      expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      expect({:post => "admin/package_repositories/check_connection"}).to route_to(:controller => "admin/package_repositories", :action => "check_connection")
      expect(package_repositories_check_connection_path).to eq("/admin/package_repositories/check_connection")
    end

    it "should resolve route to deletion of repo" do
      expect({:delete => "/admin/package_repositories/repo"}).to route_to(:controller => "admin/package_repositories", :action => "destroy", :id => "repo")
      expect(package_repositories_delete_path(:id => "repo")).to eq("/admin/package_repositories/repo")
    end

    it "should allow dots in the name of a plugin in the route for a plugin's configuration" do
      expect({:get => "/admin/package_repositories/plugin.id.with.dots/config"}).to route_to(:controller => "admin/package_repositories", :action => "plugin_config", :plugin => "plugin.id.with.dots")
      expect(package_repositories_plugin_config_path(:plugin => "plugin.id.with.dots")).to eq("/admin/package_repositories/plugin.id.with.dots/config")
    end

    it "should allow dots in the name of a plugin in the route for a plugin config for a repository" do
      expect({:get => "/admin/package_repositories/repoid/plugin.id.with.dots/config"}).to route_to(:controller => "admin/package_repositories", :action => "plugin_config_for_repo", :plugin => "plugin.id.with.dots", :id => "repoid")
      expect(package_repositories_plugin_config_for_repo_path(:plugin => "plugin.id.with.dots", :id => "repoid")).to eq("/admin/package_repositories/repoid/plugin.id.with.dots/config")
    end
  end

  describe :actions do

    before :each do
      config_validity = double('config validity')
      config_validity.should_receive(:isValid).and_return(true)
      @go_config_service = double('go config service')
      controller.stub(:go_config_service).and_return(@go_config_service)
      @go_config_service.should_receive(:checkConfigFileValid).and_return(config_validity)
      @go_config_service.stub(:registry)

      @cloner = double('cloner')
      controller.stub(:get_cloner_instance).and_return(@cloner)
    end

    describe "new" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= double('Package Repository Service'))
        @cruise_config = BasicCruiseConfig.new
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @cloner.should_receive(:deepClone).at_least(1).times.with(@cruise_config).and_return(@cruise_config)
        @user = current_user
      end

      it "should render form for addition of package repository" do
        package_repositories = PackageRepositories.new
        @cruise_config.setPackageRepositories(package_repositories)

        get :new

        expect(assigns[:tab_name]).to eq("package-repositories")
        expect(assigns[:package_repository]).to_not be_nil
        expect(assigns[:package_repositories]).to eq(package_repositories)
        expect(assigns[:package_to_pipeline_map]).to eq(@cruise_config.getGroups().getPackageUsageInPipelines())
        assert_template layout: "admin"
      end
    end

    describe "list" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= double('Package Repository Service'))
        @cruise_config = BasicCruiseConfig.new
        @cloner.should_receive(:deepClone).at_least(1).times.with(@cruise_config).and_return(@cruise_config)
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @user = current_user
      end

      it "should render page for package repository list" do
        package_repositories = PackageRepositories.new
        @cruise_config.setPackageRepositories(package_repositories)

        get :list

        expect(assigns[:tab_name]).to eq("package-repositories")
        expect(assigns[:package_repository]).to_not be_nil
        expect(assigns[:package_repositories]).to eq(package_repositories)
        expect(assigns[:package_to_pipeline_map]).to eq(@cruise_config.getGroups().getPackageUsageInPipelines())
        assert_template layout: "admin"
      end
    end

    describe "config" do
      before(:each) do
        @cruise_config = BasicCruiseConfig.new
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @cloner.should_receive(:deepClone).at_least(1).times.with(@cruise_config).and_return(@cruise_config)

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

        expect(assigns[:repository_configuration]).to_not be_nil
        expect(assigns[:repository_configuration].properties[0].display_name).to eq("Key 1")
        expect(assigns[:repository_configuration].properties[0].value).to eq(nil)
        expect(assigns[:plugin_id]).to eq("pluginid")
        expect(assigns[:isNewRepo]).to eq(true)
        assert_template layout: false
      end

      it "should get the configuration properties with values for a given repo-id associated with package material plugin" do
        get :plugin_config_for_repo, :id => "repo1", :plugin => "pluginid"

        expect(assigns[:repository_configuration]).to_not be_nil
        expect(assigns[:repository_configuration].properties.size).to eq(1)
        expect(assigns[:repository_configuration].properties[0].display_name).to eq("Key 1")
        expect(assigns[:repository_configuration].properties[0].value).to eq("v1")
        expect(assigns[:plugin_id]).to eq("pluginid")
        expect(assigns[:isNewRepo]).to eq(false)
        assert_template layout: false
      end
    end

    describe "create" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= double('Package Repository Service'))
        @cruise_config = BasicCruiseConfig.new
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @cloner.should_receive(:deepClone).at_least(1).times.with(@cruise_config).and_return(@cruise_config)

        @user = current_user
      end

      it "should save package repository form" do
        package_repository = PackageRepository.new
        package_repository.setId("repo-id")
        PackageRepository.stub(:new).and_return(package_repository)
        @package_repository_service.should_receive(:savePackageRepositoryToConfig).with(package_repository, "1234abcd", @user).and_return(ConfigUpdateAjaxResponse::success("repo-id", 200,  "success"))

        post :create, :config_md5 => "1234abcd", :package_repository => {:name => "name", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}

        expect(response.body).to eq("{\"fieldErrors\":{},\"globalErrors\":[],\"message\":\"success\",\"isSuccessful\":true,\"subjectIdentifier\":\"repo-id\",\"redirectUrl\":\"/admin/package_repositories/repo-id/edit\"}")
        expect(flash[:success]).to eq("success")
        expect(response.response_code).to eq(200)
        expect(response.location).to eq("/admin/package_repositories/repo-id/edit")
        expect(response.headers["Go-Config-Error"]).to eq(nil)
      end
      it "should not add flash message when create fails" do
        package_repository = PackageRepository.new
        PackageRepository.stub(:new).and_return(package_repository)
        @package_repository_service.should_receive(:savePackageRepositoryToConfig).with(package_repository, "1234abcd", @user).and_return(ConfigUpdateAjaxResponse::failure(nil, 500, "failed", nil, nil));

        post :create, :config_md5 => "1234abcd", :package_repository => {:name => "name", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}

        expect(flash[:success]).to eq(nil)
        expect(response.response_code).to eq(500)
        expect(response.headers["Go-Config-Error"]).to eq("failed")
        expect(response.location).to eq(nil)
      end
    end

    describe "edit" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= double('Package Repository Service'))
        @cruise_config = BasicCruiseConfig.new
        @cloner.should_receive(:deepClone).at_least(1).times.with(@cruise_config).and_return(@cruise_config)

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

        expect(assigns[:package_repository]).to eq(@repository1)
        expect(assigns[:repository_configuration]).to_not be_nil
        expect(assigns[:repository_configuration].properties.size).to eq(1)
        expect(assigns[:repository_configuration].properties[0].display_name).to eq("Key 1")
        expect(assigns[:repository_configuration].properties[0].value).to eq("v1")
        expect(assigns[:package_repositories]).to eq(@cruise_config.getPackageRepositories())
        expect(assigns[:tab_name]).to eq("package-repositories")
        expect(assigns[:package_to_pipeline_map]).to eq(@cruise_config.getGroups().getPackageUsageInPipelines())
        assert_template layout: "admin"
      end

      it "should render error if plugin is missing package repository" do
        get :edit, :id => "with-missing-plugin"

        expect(assigns[:package_repository]).to eq(@repository2)
        expect(assigns[:repository_configuration]).to_not be_nil
        expect(assigns[:repository_configuration].properties.size).to eq(0)
        expect(assigns[:errors].size).to eq(1)
        expect(assigns[:errors]).to include("Associated plugin 'missing' not found. Please contact the Go admin to install the plugin.")
        expect(assigns[:package_repositories]).to eq(@cruise_config.getPackageRepositories())
        expect(assigns[:tab_name]).to eq("package-repositories")
      end

      it "should render 404 page when repo is missing" do
        get :edit, :id => "missing-repo-id"

        expect(response.response_code).to eq(404)
        expect(assigns[:message]).to eq("Could not find the repository with id 'missing-repo-id'. It might have been deleted.")
        expect(assigns[:status]).to eq(404)
      end
    end

    describe "update" do
      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= double('Package Repository Service'))
        @cruise_config = BasicCruiseConfig.new
        @cloner.should_receive(:deepClone).at_least(1).times.with(@cruise_config).and_return(@cruise_config)
        @go_config_service.should_receive(:getConfigForEditing).and_return(@cruise_config)
        @user = current_user
      end

      it "should update package repository form" do
        package_repository = PackageRepository.new
        PackageRepository.stub(:new).and_return(package_repository)
        @package_repository_service.should_receive(:savePackageRepositoryToConfig).with(package_repository, "1234abcd", @user).and_return(ConfigUpdateAjaxResponse::success("id", 200, "success"))

        post :update, :config_md5 => "1234abcd", :id => "id", :package_repository => {:name => "name", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}

        expect(response.body).to eq("{\"fieldErrors\":{},\"globalErrors\":[],\"message\":\"success\",\"isSuccessful\":true,\"subjectIdentifier\":\"id\",\"redirectUrl\":\"/admin/package_repositories/id/edit\"}")
        expect(flash[:success]).to eq("success")
        expect(response.response_code).to eq(200)
        expect(response.location).to eq("/admin/package_repositories/id/edit")
        expect(response.headers["Go-Config-Error"]).to eq(nil)
      end

      it "should not add flash message when update fails" do
        package_repository = PackageRepository.new
        PackageRepository.stub(:new).and_return(package_repository)
        fieldErrors = LinkedHashMap.new
        fieldErrors.put("field1", Arrays.asList(["error 1"].to_java(java.lang.String)))
        fieldErrors.put("field2", Arrays.asList(["error 2"].to_java(java.lang.String)))
        ajax_response = ConfigUpdateAjaxResponse::failure("id", 500, "failed", fieldErrors, Arrays.asList(["global1", "global2"].to_java(java.lang.String)))

        @package_repository_service.should_receive(:savePackageRepositoryToConfig).with(package_repository, "1234abcd", @user).and_return(ajax_response)

        post :update, :config_md5 => "1234abcd", :id => "id", :package_repository => {:name => "name", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}

        expect(flash[:notice]).to eq(nil)
        expect(response.body).to eq("{\"fieldErrors\":{\"field1\":[\"error 1\"],\"field2\":[\"error 2\"]},\"globalErrors\":[\"global1\",\"global2\"],\"message\":\"failed\",\"isSuccessful\":false,\"subjectIdentifier\":\"id\"}")
        expect(flash[:success]).to eq(nil)
        expect(response.response_code).to eq(500)
        expect(response.headers["Go-Config-Error"]).to eq("failed")
        expect(response.location).to eq(nil)
      end
    end

    describe "check connection" do

      before(:each) do
        controller.stub(:package_repository_service).with().and_return(@package_repository_service= double('Package Repository Service'))
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
        expect(json["success"]).to eq("Connection OK from plugin.")
        expect(json["error"]).to eq(nil)
      end

      it "should show error when check connection fails for given package repository" do
        package_repository = PackageRepositoryMother.create("repo-id", "name", "yum", nil, Configuration.new([ConfigurationPropertyMother.create("key", false, "value")].to_java(ConfigurationProperty)))
        @result.should_receive(:isSuccessful).and_return(false)
        @result.should_receive(:message).twice.with(anything).and_return("Connection To Repo Failed. Bad Url")
        @package_repository_service.should_receive(:checkConnection).with(package_repository, @result)

        get :check_connection, :package_repository => {:name => "name", :repoId => "repo-id", :pluginConfiguration => {:id => "yum"}, :configuration => {"0" => {:configurationKey => {:name => "key"}, :configurationValue => {:value => "value"}}}}

        json = JSON.parse(response.body)
        expect(json["success"]).to eq(nil)
        expect(json["error"]).to eq("Connection To Repo Failed. Bad Url")
      end
    end

    describe :destroy do

      before :each do
        @cruise_config = double('cruise config')
        @cloner.should_receive(:deepClone).at_least(1).times.with(@cruise_config).and_return(@cruise_config)
        @go_config_service.should_receive(:getConfigForEditing).at_least(1).times.and_return(@cruise_config)
        @config_md5 = "1234abcd"

        @update_response = double('update_response')
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

        expect(response).to redirect_to package_repositories_list_path(:fm => 'random-uuid')
      end

      it "should render error when repository can not be deleted" do
        repository_id = 'some_repository_id'
        @update_response.should_receive(:getCruiseConfig).twice.and_return(@cruise_config)
        @update_response.should_receive(:getNode).and_return(@cruise_config)
        @update_response.should_receive(:getSubject).and_return(@cruise_config)
        @update_response.should_receive(:configAfterUpdate).and_return(@cruise_config)
        plugin_configuration = double(PluginConfiguration)
        plugin_configuration.should_receive(:getId).and_return(repository_id)
        package_repository = double(PackageRepository)
        package_repository.should_receive(:getPluginConfiguration).and_return(plugin_configuration)
        package_repositories = double(PackageRepositories)
        package_repositories.should_receive(:find).with(repository_id).and_return(package_repository)
        @cruise_config.should_receive(:getPackageRepositories).twice.and_return(package_repositories)
        pipeline_groups = double(PipelineGroups)
        pipeline_groups.should_receive(:getPackageUsageInPipelines).and_return(nil)
        @cruise_config.should_receive(:getGroups).and_return(pipeline_groups)
        @cruise_config.should_receive(:getAllErrorsExceptFor).and_return([])
        @go_config_service.should_receive(:updateConfigFromUI).with(anything, @config_md5, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |action, md5, user, r|
          r.badRequest(LocalizedMessage.string("SAVE_FAILED"))
        end.and_return(@update_response)

        delete :destroy, :id => repository_id, :config_md5 => @config_md5

        expect(assigns[:tab_name]).to eq("package-repositories")
        assert_template "edit"
        assert_template layout: "admin"
        expect(response.status).to eq(400)
        assert_template layout: "admin"
      end
    end
  end
end
