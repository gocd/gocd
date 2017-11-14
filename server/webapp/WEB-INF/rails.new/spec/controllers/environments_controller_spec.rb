##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe EnvironmentsController do
  describe "index, create and update" do
    before do
      allow(controller).to receive(:current_user).and_return('user_foo')
      @environment_service = double("Environment Service")
      @entity_hashing_service = double("Entity Hashing Service")
      allow(controller).to receive(:environment_service).and_return(@environment_service)
      allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service = double('environment_config_service', :isEnvironmentFeatureEnabled => true))
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))
      allow(@security_service).to receive(:isUserAdmin).and_return(false)
    end

    it "should set current tab" do
         user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
         allow(controller).to receive(:current_user).and_return(user)
         allow(@environment_service).to receive(:getEnvironments).with(user).and_return(["environment-1", "environment-2"])
         get :index
         expect(assigns[:current_tab_name]).to eq("environments")
    end

    it "should show add environment only if the user is a Go admin" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(user)
      allow(@environment_service).to receive(:getEnvironments).with(user).and_return(["environment-1", "environment-2"])
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))

      expect(@security_service).to receive(:isUserAdmin).with(user).and_return(true)

      get :index

      expect(assigns[:show_add_environments]).to eq(true)
    end

    it "should not show add environment link when the user is not a Go admin" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(user)
      allow(@environment_service).to receive(:getEnvironments).with(user).and_return(["environment-1", "environment-2"])
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))

      expect(@security_service).to receive(:isUserAdmin).with(user).and_return(false)

      get :index

      expect(assigns[:show_add_environments]).to eq(false)
    end

    it "should load pipeline model instance for pipelines in a environment" do
      expect(@environment_service).to receive(:getEnvironments).with(controller.current_user).and_return(["environment-1", "environment-2"])

      expect(@security_service).to receive(:isUserAdmin).with(controller.current_user).and_return(false)

      get :index

      expect(assigns[:environments]).to eq(["environment-1", "environment-2"])
    end

    it "should create a new environment" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(user)
      environment_name = "foo-environment"
      result = ""
      @env_name = @pipelines = @agents = @user = @environment_variables = ""
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(user).and_return([EnvironmentPipelineModel.new("foo", nil), EnvironmentPipelineModel.new("bar", nil)])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(anything,anything).and_return([])

      expect(@environment_config_service).to receive(:createEnvironment) do |environment_config, user, operation_result|
        @env_name = environment_config.name()
        @environment_variables = environment_config.getVariables().map do |evc|
          {"name" => evc.name(), "value" => evc.getValue()}
        end
        result = operation_result
      end

      post :create, params: { :no_layout => true, :environment => {:name => environment_name, :variables => [{"name" => "SHELL", "valueForDisplay" => "/bin/zsh"}]} }

      expect(@env_name).to eq(CaseInsensitiveString.new(environment_name))
      expect(@environment_variables).to eq([{"name" => "SHELL", "value" => "/bin/zsh"}])

      expect(result.isSuccessful).to eq(true)
      expect(response.status).to eq(302)
      flash_guid = $1 if response.location =~ /environments\/foo-environment\/show\?.*?fm=(.+)/
      assert_flash_message_and_class(controller.flash_message_service.get(flash_guid), "Added environment 'foo-environment'", 'success')
    end

    it "should create a new environment with pipeline selections" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(user)
      environment_name = "foo-environment"
      createEnvironmentCalled = false
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(user).and_return([EnvironmentPipelineModel.new("foo", nil), EnvironmentPipelineModel.new("bar", nil)])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(anything,anything).and_return([])
      expect(@environment_config_service).to receive(:createEnvironment) do |env_config, user, result|
        expect(env_config.name()).to eq(CaseInsensitiveString.new(environment_name))
        expect(env_config.getPipelineNames().to_a).to eq([CaseInsensitiveString.new("first_pipeline"), CaseInsensitiveString.new("second_pipeline")])
        expect(env_config.getAgents().map(&:getUuid)).to eq(["agent_1_uuid"])
        expect(user).to eq(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
        createEnvironmentCalled = true
      end

      post :create, params: { :no_layout => true, :environment => {:name => environment_name, :pipelines => [{:name => "first_pipeline"}, {:name => "second_pipeline"}], :agents => [{:uuid => "agent_1_uuid"}]} }

      expect(response.status).to eq(302)
      flash_guid = $1 if response.location =~ /environments\/foo-environment\/show\?.*?fm=(.+)/
      assert_flash_message_and_class(controller.flash_message_service.get(flash_guid), "Added environment 'foo-environment'", "success")
      expect(createEnvironmentCalled).to eq(true)
    end
  end

  describe "create environment shows errors" do
    render_views

    before :each do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @entity_hashing_service = double("Entity Hashing Service")
      allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service = double('environment_config_service', :isEnvironmentFeatureEnabled => true))
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))
      @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new
      @config_helper.onSetUp()
      @config_helper.using_cruise_config_dao(controller.go_config_dao)
      allow(controller).to receive(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
      allow(@security_service).to receive(:canViewAdminPage).with(user).and_return(true)
      allow(@security_service).to receive(:isUserAdmin).with(user).and_return(true)
      setup_base_urls
      pipelines = [EnvironmentPipelineModel.new("first", nil)]
      allow(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(current_user).and_return(pipelines)
      allow(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(@user).and_return([])
      allow(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(anything,anything).and_return([])
      allow(@environment_config_service).to receive(:isEnvironmentFeatureEnabled).and_return(true)
    end

    after :each do
      @config_helper.onTearDown()
    end

    it "should return error message and the response status code of the passed in result" do
      environment_name = "foo-environment"
      @config_helper.addEnvironments([environment_name])
      result = HttpLocalizedOperationResult.new()

      expect(@environment_config_service).to receive(:createEnvironment).with(anything, anything, result) do |new_config, user, result|
        result.conflict(LocalizedMessage.string("RESOURCE_ALREADY_EXISTS", "environment", @environment_name))
      end

      post :create, params: { :no_layout => true, :environment => {:name => environment_name, :pipelines => []} }

      expect(response.body).to match(/Failed to add environment./)
      expect(response.status).to eq(409)
    end

    it "should return error message if environment name is blank" do
      post :create, params: { :no_layout => true, :environment => {:pipelines => []} }

      expect(response.status).to eq(400)
      expect(response.body).to match(/Environment name is required/)
    end

    it "should retain pipeline selection on error" do
      current_user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(current_user)
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(current_user).and_return([EnvironmentPipelineModel.new("first", nil)])


      post :create, params: { :no_layout => true, :environment => {'pipelines' => [{'name' => "first"}]} }

      expect(response.status).to eq(400)
      expect(response.body).to have_selector("input[type='checkbox'][name='environment[pipelines][][name]'][id='pipeline_first'][checked='checked']")
    end
  end

  describe "update environment shows errors" do
    render_views

    before :each do
      @entity_hashing_service = double("Entity Hashing Service")
      allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service = double('environment_config_service', :isEnvironmentFeatureEnabled => true))
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))
      @user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(@user)
      allow(@security_service).to receive(:canViewAdminPage).with(@user).and_return(true)
      @environment_name = "foo-environment"
      allow(@entity_hashing_service).to receive(:md5ForEntity).and_return('md5')
      @environment = BasicEnvironmentConfig.new(CaseInsensitiveString.new("environment_name"))
    end

    def md5
      'md5'
    end

    it "should return error message and the response status code of the passed in result" do
      result = HttpLocalizedOperationResult.new()
      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(@environment_name, an_instance_of(HttpLocalizedOperationResult)).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment,"md5"))
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).and_return(@environment)
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(@user).and_return([])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(@user,@environment).and_return([])

      expect(@environment_config_service).to receive(:updateEnvironment).with(anything,anything,anything,'md5',result) do |old_config, new_config, user, md5, result|
        result.badRequest(LocalizedMessage.string("ENV_UPDATE_FAILED", @environment_name))
      end

      put :update, params: { :no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => @environment_name, :cruise_config_md5 => md5 }

      expect(response.status).to eq(400)
      expect(response.body).to match(/Failed to update environment 'foo-environment'/)
    end

    it "should return error message if environment name is blank" do
      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(@environment_name, an_instance_of(HttpLocalizedOperationResult)).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment,"md5"))
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment)
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(@user).and_return([])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(@user,@environment).and_return([])

      put :update, params: { :no_layout => true, :environment => {:name => ""}, :name => @environment_name, :cruise_config_md5 => md5 }

      expect(response.status).to eq(400)
      expect(response.body).to match(/Environment name is required/)
    end

    it "should return error occured while loading the environment for edit" do
      result = HttpLocalizedOperationResult.new()

      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(anything, anything) do |name, result|
        result.unprocessableEntity(LocalizedMessage.string("ENV_UPDATE_FAILED", @environment_name))
      end

      put :update, params: { :no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => @environment_name, :cruise_config_md5 => md5 }

      expect(response.status).to eq(422)
      expect(response.body).to match(/Failed to update environment 'foo-environment'/)
    end

    it "should successfully update environment" do
      result = HttpLocalizedOperationResult.new()
      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(@environment_name, an_instance_of(HttpLocalizedOperationResult)).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment,"md5"))
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).and_return(@environment)
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(@user).and_return([])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(@user,@environment).and_return([])
      expect(@environment_config_service).to receive(:updateEnvironment).with(anything,anything,anything,'md5',result) do |old_config, new_config, user, md5, result|
        result.setMessage(LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS",["foo_env"].to_java(java.lang.String)))
      end

      put :update, params: { :no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => @environment_name, :cruise_config_md5 => md5 }

      expect(response).to be_success
      expect(response.location).to match(/^\/environments\/foo_env\/show\?.*?fm=/)
      flash_guid = $1 if response.location =~ /environments\/foo_env\/show\?.*?fm=(.+)/
      flash = controller.flash_message_service.get(flash_guid)
      assert_flash_message_and_class(flash, "Updated environment 'foo_env'.", "success")
      expect(response.body).to eq("Updated environment 'foo_env'.")
    end
  end

  describe "update environment showing success messages" do
    before :each do
      @entity_hashing_service = double("Entity Hashing Service")
      allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service = double('environment_config_service', :isEnvironmentFeatureEnabled => true))
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))
    end

    it "should show that modified environment is merged with latest configuration" do
      allow(@entity_hashing_service).to receive(:md5ForEntity).and_return('md5')
      result = HttpLocalizedOperationResult.new()
      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(anything, anything).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(BasicEnvironmentConfig.new(), "md5"))
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(any_args).and_return([])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(anything, anything).and_return([])

      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with("foo_env").and_return(BasicEnvironmentConfig.new(CaseInsensitiveString.new("foo_env")))

      expect(@environment_config_service).to receive(:updateEnvironment).with(anything, anything, anything, 'md5', result) do |old_config, new_config, user, md5, result|
        result.setMessage(LocalizedMessage.composite([LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS", ["foo_env"].to_java(java.lang.String)), LocalizedMessage.string("CONFIG_MERGED")].to_java(com.thoughtworks.go.i18n.Localizable)))
      end


      put :update, params: { :no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => "foo_env", :cruise_config_md5 => 'md5' }

      expect(response).to be_success
      expect(response.location).to match(/^\/environments\/foo_env\/show\?.*?fm=/)
      flash_guid = $1 if response.location =~ /environments\/foo_env\/show\?.*?fm=(.+)/
      flash = controller.flash_message_service.get(flash_guid)
      assert_flash_message_and_class(flash, "Updated environment 'foo_env'. The configuration was modified by someone else, but your changes were merged successfully.", "success")
      expect(response.body).to eq("Updated environment 'foo_env'. The configuration was modified by someone else, but your changes were merged successfully.")
    end
  end

  describe "edit" do
    render_views

    before(:each) do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new
      @config_helper.onSetUp()
      @config_helper.using_cruise_config_dao(controller.go_config_dao)
      allow(controller).to receive(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
      @config_helper.addAdmins(["user_foo"].to_java(:string))
      @environment_name = "foo-environment"
      @config_helper.addEnvironments([@environment_name])
      @config_helper.addEnvironmentVariablesToEnvironment(@environment_name, "name_foo", "value_bar")
      @config_helper.addEnvironmentVariablesToEnvironment(@environment_name, "name_baz", "value_quux")

      allow(controller).to receive(:cruise_config_md5).and_return("foo_bar_baz")
      setup_base_urls
    end

    after(:each) do
      @config_helper.onTearDown()
    end

    it "should load existing variables for environment variables edit" do
      get :edit_variables, params: { :name => "foo-environment", :no_layout => true }

      expect(assigns[:environment]).to_not be_nil
      expect(response.body).to have_selector("form input[type='text'][name='environment[variables][][name]'][value='name_foo']")
      expect(response.body).to have_selector("form input[type='text'][name='environment[variables][][valueForDisplay]'][value='value_bar']")
      expect(response.body).to have_selector("form input[type='text'][name='environment[variables][][name]'][value='name_baz']")
      expect(response.body).to have_selector("form input[type='text'][name='environment[variables][][valueForDisplay]'][value='value_quux']")
    end

    it "should load existing pipelines as checked for pipeline edit" do
      @config_helper.addPipelineWithGroup("foo-group", "foo", "dev", ["unit"].to_java(:string))
      @config_helper.addPipelineToEnvironment(@environment_name, "foo")

      @config_helper.addPipelineWithGroup("bar-group", "bar", "dev", ["unit"].to_java(:string))
      @config_helper.addEnvironments(["another_env"].to_java(:string))
      @config_helper.addPipelineToEnvironment("another_env", "bar")

      @config_helper.addPipelineWithGroup("baz-group", "baz", "dev", ["unit"].to_java(:string))

      get :edit_pipelines, params: { :name => "foo-environment", :no_layout => true }

      expect(assigns[:environment]).to_not be_nil

      expect(response.body).to have_selector("form input[type='checkbox'][name='environment[pipelines][][name]'][value='foo'][checked='checked']")
      expect(response.body).to have_selector("label", "bar (another_env)")
      expect(response.body).to have_selector("form input[type='checkbox'][name='environment[pipelines][][name]'][value='baz']")
    end

    it "should load existing pipelines as checked for pipeline edit" do
      @config_helper.addAgent("in-env", "in-env")
      @config_helper.addAgentToEnvironment(@environment_name, "in-env")
      @config_helper.addAgent("out-of-env", "out-env")

      get :edit_agents, params: { :name => "foo-environment", :no_layout => true }

      expect(assigns[:environment]).to_not be_nil

      expect(response.body).to have_selector("form input[type='checkbox'][name='environment[agents][][uuid]'][value='in-env'][checked='true']")
      expect(response.body).to have_selector("form input[type='checkbox'][name='environment[agents][][uuid]'][value='out-env']")
    end

    it "should fail agent_edit for a non existing environment" do
      get :edit_agents, params: { :name => "some-non-existent-environment", :no_layout => true }

      expect(assigns[:environment]).to be_nil

      expect(response.body).to eq("Environment 'some-non-existent-environment' not found.\n")
    end

    it "should fail pipeline_edit for a non existing environment" do
      get :edit_pipelines, params: { :name => "some-non-existent-environment", :no_layout => true }

      expect(assigns[:environment]).to be_nil

      expect(response.body).to eq("Environment 'some-non-existent-environment' not found.\n")
    end

    it "should fail variable_edit for a non existing environment" do
      get :edit_variables, params: { :name => "some-non-existent-environment", :no_layout => true }

      expect(assigns[:environment]).to be_nil

      expect(response.body).to eq("Environment 'some-non-existent-environment' not found.\n")
    end
  end

  describe "show" do
    render_views

    before do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new
      @config_helper.using_cruise_config_dao(controller.go_config_dao)
      allow(controller).to receive(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
      @config_helper.addAdmins(["user_foo"].to_java(:string))
      @environment_name = "foo-env"
      @config_helper.addEnvironments([@environment_name])
      @config_helper.addEnvironmentVariablesToEnvironment(@environment_name, "name_foo", "value_bar")
      @config_helper.addEnvironmentVariablesToEnvironment(@environment_name, "name_baz", "value_quux")
      @config_helper.add_pipeline_with_group("grp", "foo_pipeline", "dev", ["builds"].to_java(:string))
      @config_helper.add_pipeline_with_group("grp", "bar_pipeline", "dev", ["builds"].to_java(:string))
      @config_helper.add_pipeline_with_group("grp", "baz_pipeline", "dev", ["builds"].to_java(:string))
      @config_helper.add_pipeline_with_group("grp", "quux_pipeline", "dev", ["builds"].to_java(:string))
      @config_helper.addPipelineToEnvironment("foo-env", "foo_pipeline")
      @config_helper.addPipelineToEnvironment("foo-env", "bar_pipeline")
      @config_helper.addEnvironments(["bar-env"])
      @config_helper.addPipelineToEnvironment("bar-env", "quux_pipeline")
      @config_helper.addAgent("host-1", "uuid-1")
      Spring.bean('agentDao').associateCookie(AgentIdentifier.new("host-1", "192.168.1.2", "uuid-1"), "cookie-1")
      ri = AgentRuntimeInfo.fromServer(AgentConfig.new("uuid-1", "host-1", "192.168.1.2"), true, "/foo/bar", 100, "linux", false)
      ri.setCookie("cookie-1")
      Spring.bean('agentService').updateRuntimeInfo(ri)
      @config_helper.addAgent("host-2", "uuid-2")
      Spring.bean('agentDao').associateCookie(AgentIdentifier.new("host-2", "192.168.1.3", "uuid-2"), "cookie-2")
      ri = AgentRuntimeInfo.fromServer(AgentConfig.new("uuid-2", "host-2", "192.168.1.3"), true, "/bar/baz", 100, "linux", false)
      ri.setCookie("cookie-2")
      Spring.bean('agentService').updateRuntimeInfo(ri)
      @config_helper.addAgent("host-3", "uuid-3")
      Spring.bean('agentDao').associateCookie(AgentIdentifier.new("host-3", "192.168.1.4", "uuid-3"), "cookie-3")
      ri = AgentRuntimeInfo.fromServer(AgentConfig.new("uuid-3", "host-3", "192.168.1.4"), true, "/baz/quux", 100, "linux", false)
      ri.setCookie("cookie-3")
      Spring.bean('agentService').updateRuntimeInfo(ri)
      @config_helper.addAgent("host-4", "uuid-4")
      Spring.bean('agentDao').associateCookie(AgentIdentifier.new("host-4", "192.168.1.5", "uuid-4"), "cookie-4")
      ri = AgentRuntimeInfo.fromServer(AgentConfig.new("uuid-4", "host-4", "192.168.1.5"), true, "/quux/bang", 100, "linux", false)
      ri.setCookie("cookie-4")
      Spring.bean('agentService').updateRuntimeInfo(ri)
      @config_helper.addAgentToEnvironment("foo-env", "uuid-1")
      @config_helper.addAgentToEnvironment("foo-env", "uuid-2")
      @config_helper.addAgentToEnvironment("bar-env", "uuid-3")

      @config_helper.addEnvironmentVariablesToEnvironment("bar-env", "name_hi", "value_bye")
      setup_base_urls
    end

    it "should show pipelines and environment variables" do
      get :show, params: { :name => "foo-env" }

      expect(response).to be_success

      expect(response.body).to have_selector(".added_pipelines li", :text => "foo_pipeline")
      expect(response.body).to have_selector(".added_pipelines li", :text => "bar_pipeline")
      expect(response.body).to_not have_selector("li", :text => "baz_pipeline")
      expect(response.body).to_not have_selector("li", :text => "quux_pipeline")

      expect(response.body).to have_selector(".added_agents li[title='/foo/bar']", :text => "host-1 (192.168.1.2)")
      expect(response.body).to have_selector(".added_agents li[title='/bar/baz']", :text => "host-2 (192.168.1.3)")
      expect(response.body).to_not have_selector("li", :text => /host-3/)
      expect(response.body).to_not have_selector("li", :text => /host-4/)

      expect(response.body).to have_selector(".added_environment_variables li", :text => "name_foo = value_bar")
      expect(response.body).to have_selector(".added_environment_variables li", :text => "name_baz = value_quux")
      expect(response.body).to_not have_selector("li", :text => "name_hi = value_bye")
    end
  end

  def assert_flash_message_and_class(flash, message, class_name)
    expect(flash.to_s).to eq(message)
    expect(flash.flashClass()).to eq(class_name)
  end

end
