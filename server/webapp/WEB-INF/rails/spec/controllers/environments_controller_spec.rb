##########################GO-LICENSE-START################################
# Copyright 2018 ThoughtWorks, Inc.
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
    before :each do
      allow(controller).to receive(:current_user).and_return('user_foo')
      @entity_hashing_service = double("Entity Hashing Service")
      allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service = double('environment_config_service', :isEnvironmentFeatureEnabled => true))
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))
      allow(@security_service).to receive(:isUserAdmin).and_return(false)
      allow(@environment_config_service).to receive(:listAllMergedEnvironments).and_return([EnvironmentViewModel.new(BasicEnvironmentConfig.new(CaseInsensitiveString.new('environment-1'))), EnvironmentViewModel.new(BasicEnvironmentConfig.new(CaseInsensitiveString.new('environment-2')))])
    end

    it "should set current tab" do
         user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
         allow(controller).to receive(:current_user).and_return(user)
         allow(@environment_config_service).to receive(:environmentNames).and_return(["environment-1", "environment-2"])
         get :index
         expect(assigns[:current_tab_name]).to eq("environments")
    end

    it "should show add environment only if the user is a Go admin" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(user)
      allow(@environment_config_service).to receive(:environmentNames).and_return(["environment-1", "environment-2"])
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))

      expect(@security_service).to receive(:isUserAdmin).with(user).and_return(true)

      get :index

      expect(assigns[:show_add_environments]).to eq(true)
    end

    it "should not show add environment link when the user is not a Go admin" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(user)
      allow(@environment_config_service).to receive(:environmentNames).and_return(["environment-1", "environment-2"])
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))

      expect(@security_service).to receive(:isUserAdmin).with(user).and_return(false)

      get :index

      expect(assigns[:show_add_environments]).to eq(false)
    end

    it "should load pipeline model instance for pipelines in a environment" do
      expect(@environment_config_service).to receive(:environmentNames).and_return(["environment-1", "environment-2"])

      expect(@security_service).to receive(:isUserAdmin).with(controller.current_user).and_return(false)

      get :index

      expect(assigns[:environments]).to eq(["environment-1", "environment-2"])
    end

    it "should match /environments defaulting to html format" do
      expect(:get => "/environments").to route_to({:controller => "environments", :action => 'index', :format => :html})
    end

    it "should match /new" do
      expect(:get => "/environments/new").to route_to({:controller => "environments", :action => 'new', :no_layout => true})
    end

    it "should match /create" do
      expect(:post => "/environments/create").to route_to({:controller => "environments", :action => 'create', :no_layout => true})
    end

    it "should match /update" do
      expect(:put => "/environments/foo").to route_to({:no_layout=>true, :controller => "environments", :action => 'update', :name => 'foo'})
      expect(:put => "/environments/foo.bar.baz").to route_to({:no_layout=>true, :controller => "environments", :action => 'update', :name => 'foo.bar.baz'})
    end

    it "should match /edit/pipelines" do
      expect(:get => "/environments/foo/edit/pipelines").to route_to({:controller => "environments", :action => 'edit_pipelines', :name => 'foo', :no_layout => true})
      expect(:get => "/environments/foo.bar.baz/edit/pipelines").to route_to({:controller => "environments", :action => 'edit_pipelines', :name => 'foo.bar.baz', :no_layout => true})
    end

    it "should match /edit/agents" do
      expect(:get => "/environments/foo/edit/agents").to route_to({:controller => "environments", :action => 'edit_agents', :name => 'foo', :no_layout => true})
      expect(:get => "/environments/foo.bar.baz/edit/agents").to route_to({:controller => "environments", :action => 'edit_agents', :name => 'foo.bar.baz', :no_layout => true})
    end

    it "should match /edit/variables" do
      expect(:get => "/environments/foo/edit/variables").to route_to({:controller => "environments", :action => 'edit_variables', :name => 'foo', :no_layout => true})
      expect(:get => "/environments/foo.bar.baz/edit/variables").to route_to({:controller => "environments", :action => 'edit_variables', :name => 'foo.bar.baz', :no_layout => true})
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

      post :create, params:{:no_layout => true, :environment => {:name => environment_name, :variables => [{"name" => "SHELL", "valueForDisplay" => "/bin/zsh"}]}}

      expect(@env_name).to eq(CaseInsensitiveString.new(environment_name))
      expect(@environment_variables).to eq([{"name" => "SHELL", "value" => "/bin/zsh"}])

      expect(result.isSuccessful).to eq(true)
      expect(response.status).to eq(302)
      flash_guid = $1 if response.location =~ /environments\?.*?fm=(.+)/
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

      post :create, params:{:no_layout => true, :environment => {:name => environment_name, :pipelines => [{:name => "first_pipeline"}, {:name => "second_pipeline"}], :agents => [{:uuid => "agent_1_uuid"}]}}

      expect(response.status).to eq(302)
      flash_guid = $1 if response.location =~ /environments\?.*?fm=(.+)/
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
      @config_helper.using_cruise_config_dao(Spring.bean("goConfigDao"))
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
        result.conflict('Failed to add environment.')
      end

      post :create, params:{:no_layout => true, :environment => {:name => environment_name, :pipelines => []}}

      expect(response.body).to match(/Failed to add environment./)
      expect(response.status).to eq(409)
    end

    it "should return error message if environment name is blank" do
      post :create, params:{:no_layout => true, :environment => {:pipelines => []}}

      expect(response.status).to eq(400)
      expect(response.body).to match(/Environment name is required/)
    end

    it "should retain pipeline selection on error" do
      current_user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      allow(controller).to receive(:current_user).and_return(current_user)
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(current_user).and_return([EnvironmentPipelineModel.new("first", nil)])


      post :create, params:{:no_layout => true, :environment => {'pipelines' => [{'name' => "first"}]}}

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
        result.badRequest("Failed to update environment 'foo-environment'")
      end

      put :update, params:{:no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => @environment_name, :cruise_config_md5 => md5}

      expect(response.status).to eq(400)
      expect(response.body).to match(/Failed to update environment 'foo-environment'/)
    end

    it "should return error message if environment name is blank" do
      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(@environment_name, an_instance_of(HttpLocalizedOperationResult)).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment,"md5"))
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment)
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(@user).and_return([])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(@user,@environment).and_return([])

      put :update, params:{:no_layout => true, :environment => {:name => ""}, :name => @environment_name, :cruise_config_md5 => md5}

      expect(response.status).to eq(400)
      expect(response.body).to match(/Environment name is required/)
    end

    it "should return error occured while loading the environment for edit" do
      result = HttpLocalizedOperationResult.new()

      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(anything, anything) do |name, result|
        result.unprocessableEntity("Failed to update environment 'foo-environment'")
      end

      put :update, params:{:no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => @environment_name, :cruise_config_md5 => md5}

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
        result.setMessage("Updated environment 'foo_env'.")
      end

      put :update, params:{:no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => @environment_name, :cruise_config_md5 => md5}

      expect(response).to be_success
      expect(response.location).to match(/^\/environments\?.*?fm=/)
      flash_guid = $1 if response.location =~ /environments\?.*?fm=(.+)/
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
        result.setMessage("some message")
      end


      put :update, params:{:no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => "foo_env", :cruise_config_md5 => 'md5'}

      expect(response).to be_success
      expect(response.location).to match(/^\/environments\?.*?fm=/)
      flash_guid = $1 if response.location =~ /environments\?.*?fm=(.+)/
      flash = controller.flash_message_service.get(flash_guid)
      assert_flash_message_and_class(flash, "some message", "success")
      expect(response.body).to eq("some message")
    end
  end

  describe "edit" do
    render_views
    before(:each) do
      @environment_name = "foo-environment"
      @environment = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment.setVariables(EnvironmentVariablesConfigMother.env(["name_foo", "name_baz"].to_java(java.lang.String), ["value_bar", "value_quux"].to_java(java.lang.String)))
      @entity_hashing_service = double("Entity Hashing Service")
      allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service = double('environment_config_service', :isEnvironmentFeatureEnabled => true))
      allow(controller).to receive(:agent_service).and_return(@agent_service = double('agent_service'))
      allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new
      @config_helper.onSetUp()
      @config_helper.using_cruise_config_dao(Spring.bean('goConfigDao'))
      allow(controller).to receive(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
      @config_helper.addAdmins(["user_foo"].to_java(:string))
      @config_helper.addEnvironments([@environment_name])
      @config_helper.addEnvironmentVariablesToEnvironment(@environment_name, "name_foo", "value_bar")
      @config_helper.addEnvironmentVariablesToEnvironment(@environment_name, "name_baz", "value_quux")

      allow(controller).to receive(:cruise_config_md5).and_return("foo_bar_baz")
      allow(@entity_hashing_service).to receive(:md5ForEntity).and_return("md5")
      setup_base_urls
    end

    after(:each) do
      @config_helper.onTearDown()
    end

    it "should load existing variables for environment variables edit" do
      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(@environment_name, an_instance_of(HttpLocalizedOperationResult)).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment,"md5"))
      expect(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment)

      get :edit_variables, params:{:name => "foo-environment", :no_layout => true}

      expect(assigns[:environment]).to_not be_nil
      expect(response.body).to have_selector("form input[type='text'][name='environment[variables][][name]'][value='name_foo']")
      expect(response.body).to have_selector("form input[type='text'][name='environment[variables][][valueForDisplay]'][value='value_bar']")
      expect(response.body).to have_selector("form input[type='text'][name='environment[variables][][name]'][value='name_baz']")
      expect(response.body).to have_selector("form input[type='text'][name='environment[variables][][valueForDisplay]'][value='value_quux']")
    end

    it "should load existing pipelines as checked for pipeline edit" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @config_helper.addPipelineWithGroup("foo-group", "foo", "dev", ["unit"].to_java(:string))
      @config_helper.addPipelineToEnvironment(@environment_name, "foo")

      @config_helper.addPipelineWithGroup("bar-group", "bar", "dev", ["unit"].to_java(:string))
      @config_helper.addEnvironments(["another_env"].to_java(:string))
      @config_helper.addPipelineToEnvironment("another_env", "bar")

      @config_helper.addPipelineWithGroup("baz-group", "baz", "dev", ["unit"].to_java(:string))
      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(@environment_name, an_instance_of(HttpLocalizedOperationResult)).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment,"md5"))
      expect(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment)
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(user).and_return([EnvironmentPipelineModel.new("foo", @environment_name), EnvironmentPipelineModel.new("bar", "another_env"), EnvironmentPipelineModel.new("baz", nil)])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(anything,anything).and_return([])
      expect(@agent_service).to receive(:registeredAgents).and_return(AgentsViewModel.new)

      get :edit_pipelines, params:{:name => "foo-environment", :no_layout => true}

      expect(assigns[:environment]).to_not be_nil

      expect(response.body).to have_selector("input[type='checkbox'][name='environment[pipelines][][name]'][value='foo'][checked='checked']")
      expect(response.body).to have_selector("label", "bar (another_env)")
      expect(response.body).to have_selector("form input[type='checkbox'][name='environment[pipelines][][name]'][value='baz']")
    end

    it "should load existing agents as checked for agent edit" do
      @environment.getLocalAgents().add(com.thoughtworks.go.config.EnvironmentAgentConfig.new('in-env'))
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @config_helper.addAgent("in-env", "in-env")
      @config_helper.addAgentToEnvironment(@environment_name, "in-env")
      @config_helper.addAgent("out-of-env", "out-env")
      in_env = AgentViewModel.new(AgentInstanceMother.localInstance(SystemEnvironment.new, "in-env", "in-env"), "foo-environment")
      out_env = AgentViewModel.new(AgentInstanceMother.localInstance(SystemEnvironment.new, "out-env", "out-of-env"))
      agents_view_model = AgentsViewModel.new(in_env, out_env)
      expect(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with(@environment_name, an_instance_of(HttpLocalizedOperationResult)).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment,"md5"))
      expect(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment)
      expect(@environment_config_service).to receive(:getAllLocalPipelinesForUser).with(user).and_return([EnvironmentPipelineModel.new("foo", @environment_name), EnvironmentPipelineModel.new("bar", "another_env"), EnvironmentPipelineModel.new("baz", nil)])
      expect(@environment_config_service).to receive(:getAllRemotePipelinesForUserInEnvironment).with(anything,anything).and_return([])
      expect(@agent_service).to receive(:registeredAgents).and_return(agents_view_model)
      get :edit_agents, params:{:name => "foo-environment", :no_layout => true}

      expect(assigns[:environment]).to_not be_nil


      expect(response.body).to have_selector("form input[type='checkbox'][name='environment[agents][][uuid]'][value='in-env'][checked='true']")
      expect(response.body).to have_selector("form input[type='checkbox'][name='environment[agents][][uuid]'][value='out-env']")
    end

    it "should fail agent_edit for a non existing environment" do
      allow(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with('some-non-existent-environment', an_instance_of(HttpLocalizedOperationResult)) do |env, result|
        result.badRequest(LocalizedMessage::string("RESOURCE_NOT_FOUND", 'Environment', 'some-non-existent-environment'))
      end

      get :edit_agents, params:{:name => "some-non-existent-environment", :no_layout => true}

      expect(assigns[:environment]).to be_nil

      expect(response.body).to eq("Environment 'some-non-existent-environment' not found.\n")
    end

    it "should fail pipeline_edit for a non existing environment" do
      allow(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with('some-non-existent-environment', an_instance_of(HttpLocalizedOperationResult)) do |env, result|
        result.badRequest(LocalizedMessage::string("RESOURCE_NOT_FOUND", 'Environment', 'some-non-existent-environment'))
      end

      get :edit_pipelines, params:{:name => "some-non-existent-environment", :no_layout => true}

      expect(assigns[:environment]).to be_nil

      expect(response.body).to eq("Environment 'some-non-existent-environment' not found.\n")
    end

    it "should fail variable_edit for a non existing environment" do
      allow(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).with('some-non-existent-environment', an_instance_of(HttpLocalizedOperationResult)) do |env, result|
        result.badRequest(LocalizedMessage::string("RESOURCE_NOT_FOUND", 'Environment', 'some-non-existent-environment'))
      end

      get :edit_variables, params:{:name => "some-non-existent-environment", :no_layout => true}

      expect(assigns[:environment]).to be_nil

      expect(response.body).to eq("Environment 'some-non-existent-environment' not found.\n")
    end
  end

  def assert_flash_message_and_class(flash, message, class_name)
    expect(flash.to_s).to eq(message)
    expect(flash.flashClass()).to eq(class_name)
  end

end
