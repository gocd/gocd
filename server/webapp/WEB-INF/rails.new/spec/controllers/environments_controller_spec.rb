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

require File.expand_path(File.dirname(__FILE__) + '/../spec_helper')

def assert_flash_message_and_class(flash, message, class_name)
  flash.to_s.should == message
  flash.flashClass().should == class_name
end

describe EnvironmentsController do
  describe "index, create and update" do
    before do
      @environment_service = Object.new
      controller.stub!(:current_user).and_return('user_foo')
      controller.stub!(:set_locale)
      controller.stub(:licensed_agent_limit)
      controller.stub(:populate_config_validity)
    end

    it "should set current tab" do
         user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
         controller.stub!(:current_user).and_return(user)
         controller.stub!(:environment_config_service).and_return(stub('environment_config_service', :isEnvironmentFeatureEnabled => true))
         @environment_service.stub!(:getEnvironments).with(user).and_return(["environment-1", "environment-2"])
         get :index
         assigns[:current_tab_name].should == "environments"
    end

    it "should show add environment only if the user is a Go admin" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      controller.stub!(:current_user).and_return(user)
      controller.stub!(:environment_config_service).and_return(stub('environment_config_service', :isEnvironmentFeatureEnabled => true))
      @environment_service.stub!(:getEnvironments).with(user).and_return(["environment-1", "environment-2"])
      controller.stub!(:security_service).and_return(@security_service = Object.new)

      @security_service.should_receive(:isUserAdmin).with(user).and_return(true)

      get :index

      assigns[:show_add_environments].should == true
    end

    it "should not show add environment link when the user is not a Go admin" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      controller.stub!(:current_user).and_return(user)
      controller.stub!(:environment_config_service).and_return(stub('environment_config_service', :isEnvironmentFeatureEnabled => true))
      @environment_service.stub!(:getEnvironments).with(user).and_return(["environment-1", "environment-2"])
      controller.stub!(:security_service).and_return(@security_service = Object.new)

      @security_service.should_receive(:isUserAdmin).with(user).and_return(false)

      get :index

      assigns[:show_add_environments].should == false
    end

    it "should load pipeline model instance for pipelines in a environment" do
      controller.stub!(:environment_service).and_return(@environment_service)
      controller.stub!(:environment_config_service).and_return(stub('environment_config_service', :isEnvironmentFeatureEnabled => true))
      @environment_service.should_receive(:getEnvironments).with(controller.current_user).and_return(["environment-1", "environment-2"])
      controller.stub!(:security_service).and_return(@security_service = Object.new)
      @security_service.should_receive(:isUserAdmin).with(controller.current_user).and_return(false)
      get :index
      assigns[:environments].should == ["environment-1", "environment-2"]
    end

    it "should match /environments defaulting to html format" do
      params_from(:get, "/environments").should == {:controller => "environments", :action => 'index', :format => "html"}
    end

    it "should match /environments defaulting to json format" do
      params_from(:post, "/environments.json").should == {:controller => "environments", :action => 'index', :format => "json"}
    end

    it "should match /new" do
      params_from(:get, "/environments/new").should == {:controller => "environments", :action => 'new', :no_layout => true}
    end

    it "should match /create" do
      params_from(:post, "/environments/create").should == {:controller => "environments", :action => 'create', :no_layout => true}
    end

    it "should match /update" do
      params_from(:put, "/environments/foo").should == {:no_layout=>true, :controller => "environments", :action => 'update', :name => 'foo'}
      params_from(:put, "/environments/foo.bar.baz").should == {:no_layout=>true, :controller => "environments", :action => 'update', :name => 'foo.bar.baz'}
    end

    it "should match /show" do
      params_from(:get, "/environments/foo/show").should == {:controller => "environments", :action => 'show', :name => 'foo'}
      params_from(:get, "/environments/foo.bar.baz/show").should == {:controller => "environments", :action => 'show', :name => 'foo.bar.baz'}
    end

    it "should match /edit/pipelines" do
      params_from(:get, "/environments/foo/edit/pipelines").should == {:controller => "environments", :action => 'edit_pipelines', :name => 'foo', :no_layout => true}
      params_from(:get, "/environments/foo.bar.baz/edit/pipelines").should == {:controller => "environments", :action => 'edit_pipelines', :name => 'foo.bar.baz', :no_layout => true}
    end

    it "should match /edit/agents" do
      params_from(:get, "/environments/foo/edit/agents").should == {:controller => "environments", :action => 'edit_agents', :name => 'foo', :no_layout => true}
      params_from(:get, "/environments/foo.bar.baz/edit/agents").should == {:controller => "environments", :action => 'edit_agents', :name => 'foo.bar.baz', :no_layout => true}
    end

    it "should match /edit/variables" do
      params_from(:get, "/environments/foo/edit/variables").should == {:controller => "environments", :action => 'edit_variables', :name => 'foo', :no_layout => true}
      params_from(:get, "/environments/foo.bar.baz/edit/variables").should == {:controller => "environments", :action => 'edit_variables', :name => 'foo.bar.baz', :no_layout => true}
    end

    it "should create a new environment" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      controller.stub!(:current_user).and_return(user)
      controller.stub!(:environment_config_service).and_return(@environment_service)
      environment_name = "foo-environment"
      result = ""
      @env_name = @pipelines = @agents = @user = @environment_variables = ""
      @environment_service.should_receive(:getAllPipelinesForUser).with(user).and_return([EnvironmentPipelineModel.new("foo", nil), EnvironmentPipelineModel.new("bar", nil)])

      @environment_service.should_receive(:createEnvironment) do |environment_config, user, operation_result|
        @env_name = environment_config.name()
        @environment_variables = environment_config.getVariables().map do |evc|
          {"name" => evc.name(), "value" => evc.getValue()}
        end
        result = operation_result
      end

      post :create, :no_layout => true, :environment => {:name => environment_name, :variables => [{"name" => "SHELL", "valueForDisplay" => "/bin/zsh"}]}

      @env_name.should == CaseInsensitiveString.new(environment_name)
      @environment_variables.should == [{"name" => "SHELL", "value" => "/bin/zsh"}]

      result.isSuccessful.should == true
      response.status.should == "302 Found"
      flash_guid = $1 if response.location =~ /environments\/foo-environment\/show\?.*?fm=(.+)/
      assert_flash_message_and_class(controller.flash_message_service.get(flash_guid), "Added environment 'foo-environment'", 'success')
    end

    it "should create a new environment with pipeline selections" do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      controller.stub!(:current_user).and_return(user)
      controller.stub!(:environment_config_service).and_return(@environment_service)
      environment_name = "foo-environment"
      createEnvironmentCalled = false
      @environment_service.should_receive(:getAllPipelinesForUser).with(user).and_return([EnvironmentPipelineModel.new("foo", nil), EnvironmentPipelineModel.new("bar", nil)])
      @environment_service.should_receive(:createEnvironment) do |env_config, user, result|
        env_config.name().should == CaseInsensitiveString.new(environment_name)
        env_config.getPipelineNames().to_a.should == [CaseInsensitiveString.new("first_pipeline"), CaseInsensitiveString.new("second_pipeline")]
        env_config.getAgents().map(&:getUuid).should == ["agent_1_uuid"]
        user.should == com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
        createEnvironmentCalled = true
      end

      post :create, :no_layout => true, :environment => {:name => environment_name, :pipelines => [{:name => "first_pipeline"}, {:name => "second_pipeline"}], :agents => [{:uuid => "agent_1_uuid"}]}

      response.status.should == "302 Found"
      flash_guid = $1 if response.location =~ /environments\/foo-environment\/show\?.*?fm=(.+)/
      assert_flash_message_and_class(controller.flash_message_service.get(flash_guid), "Added environment 'foo-environment'", "success")
      createEnvironmentCalled.should == true
    end
  end

  describe "create environment shows errors" do
    integrate_views

    before :each do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @environment_service = Object.new
      @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new
      @config_helper.onSetUp()
      @config_helper.using_cruise_config_dao(controller.go_config_file_dao)
      controller.stub!(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
      controller.stub!(:security_service).and_return(@security_service = Object.new)
      @security_service.should_receive(:canViewAdminPage).with(user).and_return(true)
      @security_service.stub(:isUserAdmin).with(user).and_return(true)
      setup_base_urls
    end

    after :each do
      @config_helper.onTearDown()
    end

    it "should return error message and the response status code of the passed in result" do
      environment_name = "foo-environment"
      @config_helper.addEnvironments([environment_name])

      post :create, :no_layout => true, :environment => {:name => environment_name, :pipelines => []}

      response.status.should == "409 Conflict"
      response.body.should =~ /Failed to add environment. Environment 'foo-environment' already exists./
    end

    it "should return error message if environment name is blank" do
      post :create, :no_layout => true, :environment => {:pipelines => []}

      response.status.should == "400 Bad Request"
      response.body.should =~ /Environment name is required/
    end

    it "should retain pipeline selection on error" do
      controller.stub!(:environment_config_service).and_return(@environment_service)
      current_user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      controller.stub!(:current_user).and_return(current_user)

      pipelines = [EnvironmentPipelineModel.new("first", nil)]
      @environment_service.should_receive(:getAllPipelinesForUser).with(current_user).and_return(pipelines)
      @environment_service.stub!(:isEnvironmentFeatureEnabled).and_return(true)

      post :create, :no_layout => true, :environment => {'pipelines' => [{'name' => "first"}]}

      response.status.should == "400 Bad Request"

      response.body.should have_tag("input[type='checkbox'][name='environment[pipelines][][name]'][id='pipeline_first'][checked='checked']")
    end
  end

  describe "update environment shows errors" do
    integrate_views

    before :each do
      @user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @environment_service = Object.new
      @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new
      @config_helper.onSetUp()
      @config_helper.using_cruise_config_dao(controller.go_config_file_dao)
      controller.stub!(:current_user).and_return(@user)
      controller.stub!(:security_service).and_return(@security_service = Object.new)
      @security_service.stub(:canViewAdminPage).with(@user).and_return(true)
      @environment_name = "foo-environment"
      @config_helper.addEnvironments([@environment_name])
      @config_helper.addAdmins(["user_foo"].to_java(:string))
      setup_base_urls
    end

    after :each do
      @config_helper.onTearDown()
    end

    def md5
      Spring.bean("goConfigFileDao").md5OfConfigFile()
    end

    it "should return error message and the response status code of the passed in result" do
      put :update, :no_layout => true, :environment => {:pipelines => [{:name => "foo"}]}, :name => @environment_name, :cruise_config_md5 => md5

      response.status.should == "400 Bad Request"
      response.body.should =~ /Failed to update environment 'foo-environment'. Environment 'foo-environment' refers to an unknown pipeline 'foo'./
    end

    it "should return error message if environment name is blank" do
      put :update, :no_layout => true, :environment => {:name => ""}, :name => @environment_name, :cruise_config_md5 => md5

      response.status.should == "400 Bad Request"
      response.body.should =~ /Environment name is required/
    end

    it "should return error when pipeline added in a different environment is added" do
      @config_helper.addPipelineWithGroup("foo-group", "foo-pipeline", "dev", ["unit"].to_java(:string))
      @config_helper.addPipelineWithGroup("bar-group", "bar-pipeline", "dev", ["unit"].to_java(:string))
      @config_helper.addEnvironments(["another_env"].to_java(:string))
      @config_helper.addPipelineToEnvironment("another_env", "bar-pipeline")

      put :update, :no_layout => true, :environment => {'pipelines' => [{'name' => "foo-pipeline"}, {'name' => "bar-pipeline"}]}, :name => @environment_name, :cruise_config_md5 => md5

      response.status.should == "400 Bad Request"

      response.body.should == "Failed to update environment 'foo-environment'. Duplicate unique value [bar-pipeline] declared for identity constraint \"uniqueEnvironmentPipelineName\" of element \"environments\".\n"
    end

    it "should return error when invalid agent uuid is added" do
      put :update, :no_layout => true, :environment => {'agents' => [{'uuid' => "invalid-one"}]}, :name => @environment_name, :cruise_config_md5 => md5

      response.status.should == "400 Bad Request"

      response.body.should == "Failed to update environment 'foo-environment'. Environment 'foo-environment' has an invalid agent uuid 'invalid-one'\n"
    end

    it "should return error when updating a non-exitant environment" do
      put :update, :no_layout => true, :environment => {'agents' => [{'uuid' => "something"}]}, :name => "an_env_that_does_not_exist", :cruise_config_md5 => md5

      response.status.should == "400 Bad Request"

      response.body.should == "Environment named 'an_env_that_does_not_exist' not found.\n"
    end

    it "should successfully update environment" do
      @config_helper.addPipelineWithGroup("bar-group", "bar", "dev", ["unit"].to_java(:string))
      @config_helper.addAgent("agent.com", "uuid-1")

      put :update, :no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => @environment_name, 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => @environment_name, :cruise_config_md5 => md5

      response.status.should == "200 OK"
      response.location.should =~ /^\/environments\/foo-environment\/show\?.*?fm=/
      flash_guid = $1 if response.location =~ /environments\/foo-environment\/show\?.*?fm=(.+)/
      flash = controller.flash_message_service.get(flash_guid)
      assert_flash_message_and_class(flash, "Updated environment 'foo-environment'.", "success")
      response.body.should == "Updated environment 'foo-environment'."
    end
  end

  describe "update environment showing success messages" do

    it "should show that modified environment is merged with latest configuration" do

      environment_service = mock("Environment Config Service")
      controller.stub!(:environment_config_service).and_return(environment_service)
      result = HttpLocalizedOperationResult.new()
      result.setMessage(LocalizedMessage.composite([LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS",["foo_env"].to_java(java.lang.String)),LocalizedMessage.string("CONFIG_MERGED")].to_java(com.thoughtworks.go.i18n.Localizable)))
      environment_service.should_receive(:updateEnvironment).with(any_args,any_args,any_args,any_args).and_return(result)
      environment_service.should_receive(:forEdit).with(any_args,any_args).and_return(com.thoughtworks.go.domain.ConfigElementForEdit.new(EnvironmentConfig.new(),"md5"))
      environment_service.should_receive(:getAllPipelinesForUser).with(any_args).and_return([])


      put :update, :no_layout => true,
          :environment => {'agents' => [{'uuid' => "uuid-1"}], 'name' => 'foo_env', 'pipelines' => [{'name' => 'bar'}], 'variables' => [{'name' => "var_name", 'value' => "var_value"}]},
          :name => "environment_name", :cruise_config_md5 => 'md5'

      response.status.should == "200 OK"
      response.location.should =~ /^\/environments\/foo_env\/show\?.*?fm=/
      flash_guid = $1 if response.location =~ /environments\/foo_env\/show\?.*?fm=(.+)/
      flash = controller.flash_message_service.get(flash_guid)
      assert_flash_message_and_class(flash, "Updated environment 'foo_env'. The configuration was modified by someone else, but your changes were merged successfully.", "success")
      response.body.should == "Updated environment 'foo_env'. The configuration was modified by someone else, but your changes were merged successfully."
    end
  end


  describe :edit do
    integrate_views

    before(:each) do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new
      @config_helper.onSetUp()
      @config_helper.using_cruise_config_dao(controller.go_config_file_dao)
      controller.stub!(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
      controller.stub!(:security_service).and_return(@security_service = Object.new)
      @security_service.stub(:canViewAdminPage).with(user).and_return(true)
      @config_helper.addAdmins(["user_foo"].to_java(:string))
      @environment_name = "foo-environment"
      @config_helper.addEnvironments([@environment_name])
      @config_helper.addEnvironmentVariablesToEnvironment(@environment_name, "name_foo", "value_bar")
      @config_helper.addEnvironmentVariablesToEnvironment(@environment_name, "name_baz", "value_quux")

      controller.stub(:cruise_config_md5).and_return("foo_bar_baz")
      setup_base_urls
    end

    after(:each) do
      @config_helper.onTearDown()
    end

    def assert_has_config_md5 response
      response.body.should have_tag("form input[type='hidden'][name='cruise_config_md5'][value='foo_bar_baz']")
    end

    it "should load existing variables for environment variables edit" do
      get :edit_variables, :name => "foo-environment", :no_layout => true
      assigns[:environment].should_not be_nil
      response.body.should have_tag("form input[type='text'][name='environment[variables][][name]'][value='name_foo']")
      response.body.should have_tag("form input[type='text'][name='environment[variables][][valueForDisplay]'][value='value_bar']")
      response.body.should have_tag("form input[type='text'][name='environment[variables][][name]'][value='name_baz']")
      response.body.should have_tag("form input[type='text'][name='environment[variables][][valueForDisplay]'][value='value_quux']")
      assert_has_config_md5 response
    end

    it "should load existing pipelines as checked for pipeline edit" do
      @config_helper.addPipelineWithGroup("foo-group", "foo", "dev", ["unit"].to_java(:string))
      @config_helper.addPipelineToEnvironment(@environment_name, "foo")

      @config_helper.addPipelineWithGroup("bar-group", "bar", "dev", ["unit"].to_java(:string))
      @config_helper.addEnvironments(["another_env"].to_java(:string))
      @config_helper.addPipelineToEnvironment("another_env", "bar")

      @config_helper.addPipelineWithGroup("baz-group", "baz", "dev", ["unit"].to_java(:string))

      get :edit_pipelines, :name => "foo-environment", :no_layout => true

      assigns[:environment].should_not be_nil

      response.body.should have_tag("form input[type='checkbox'][name='environment[pipelines][][name]'][value='foo'][checked='checked']")
      response.body.should have_tag("label", "bar (another_env)")
      response.body.should have_tag("form input[type='checkbox'][name='environment[pipelines][][name]'][value='baz']")
      assert_has_config_md5 response
    end

    it "should load existing pipelines as checked for pipeline edit" do
      @config_helper.addAgent("in-env", "in-env")
      @config_helper.addAgentToEnvironment(@environment_name, "in-env")
      @config_helper.addAgent("out-of-env", "out-env")
      @security_service.stub!(:hasOperatePermissionForAgents).and_return(true)

      get :edit_agents, :name => "foo-environment", :no_layout => true

      assigns[:environment].should_not be_nil

      response.body.should have_tag("form input[type='checkbox'][name='environment[agents][][uuid]'][value='in-env'][checked='true']")
      response.body.should have_tag("form input[type='checkbox'][name='environment[agents][][uuid]'][value='out-env']")
      assert_has_config_md5 response
    end

    it "should fail agent_edit for a non existing environment" do
      get :edit_agents, :name => "some-non-existent-environment", :no_layout => true

      assigns[:environment].should be_nil

      response.body.should == "Environment named 'some-non-existent-environment' not found.\n"
    end

    it "should fail pipeline_edit for a non existing environment" do
      get :edit_pipelines, :name => "some-non-existent-environment", :no_layout => true

      assigns[:environment].should be_nil

      response.body.should == "Environment named 'some-non-existent-environment' not found.\n"
    end

    it "should fail variable_edit for a non existing environment" do
      get :edit_variables, :name => "some-non-existent-environment", :no_layout => true

      assigns[:environment].should be_nil

      response.body.should == "Environment named 'some-non-existent-environment' not found.\n"
    end
  end

  describe :show do
    integrate_views

    before do
      user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo'))
      @config_helper = com.thoughtworks.go.util.GoConfigFileHelper.new
      @config_helper.using_cruise_config_dao(controller.go_config_file_dao)
      controller.stub!(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
      controller.stub!(:security_service).and_return(@security_service = Object.new)
      @security_service.stub(:canViewAdminPage).with(user).and_return(true)
      @security_service.stub(:isUserAdmin).with(user).and_return(true)
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
      ri = AgentRuntimeInfo.fromServer(AgentConfig.new("uuid-1", "host-1", "192.168.1.2"), true, "/foo/bar", 100, "linux")
      ri.setCookie("cookie-1")
      Spring.bean('agentService').updateRuntimeInfo(ri)
      @config_helper.addAgent("host-2", "uuid-2")
      Spring.bean('agentDao').associateCookie(AgentIdentifier.new("host-2", "192.168.1.3", "uuid-2"), "cookie-2")
      ri = AgentRuntimeInfo.fromServer(AgentConfig.new("uuid-2", "host-2", "192.168.1.3"), true, "/bar/baz", 100, "linux")
      ri.setCookie("cookie-2")
      Spring.bean('agentService').updateRuntimeInfo(ri)
      @config_helper.addAgent("host-3", "uuid-3")
      Spring.bean('agentDao').associateCookie(AgentIdentifier.new("host-3", "192.168.1.4", "uuid-3"), "cookie-3")
      ri = AgentRuntimeInfo.fromServer(AgentConfig.new("uuid-3", "host-3", "192.168.1.4"), true, "/baz/quux", 100, "linux")
      ri.setCookie("cookie-3")
      Spring.bean('agentService').updateRuntimeInfo(ri)
      @config_helper.addAgent("host-4", "uuid-4")
      Spring.bean('agentDao').associateCookie(AgentIdentifier.new("host-4", "192.168.1.5", "uuid-4"), "cookie-4")
      ri = AgentRuntimeInfo.fromServer(AgentConfig.new("uuid-4", "host-4", "192.168.1.5"), true, "/quux/bang", 100, "linux")
      ri.setCookie("cookie-4")
      Spring.bean('agentService').updateRuntimeInfo(ri)
      @config_helper.addAgentToEnvironment("foo-env", "uuid-1")
      @config_helper.addAgentToEnvironment("foo-env", "uuid-2")
      @config_helper.addAgentToEnvironment("bar-env", "uuid-3")

      @config_helper.addEnvironmentVariablesToEnvironment("bar-env", "name_hi", "value_bye")
      setup_base_urls
    end

    it "should show pipelines and environment variables" do
      get :show, :name => "foo-env"

      response.status.should == "200 OK"

      response.body.should have_tag(".added_pipelines li", "foo_pipeline")
      response.body.should have_tag(".added_pipelines li", "bar_pipeline")
      response.body.should_not have_tag("li", "baz_pipeline")
      response.body.should_not have_tag("li", "quux_pipeline")

      response.body.should have_tag(".added_agents li[title='/foo/bar']", "host-1 (192.168.1.2)")
      response.body.should have_tag(".added_agents li[title='/bar/baz']", "host-2 (192.168.1.3)")
      response.body.should_not have_tag("li", /host-3/)
      response.body.should_not have_tag("li", /host-4/)

      response.body.should have_tag(".added_environment_variables li", "name_foo = value_bar")
      response.body.should have_tag(".added_environment_variables li", "name_baz = value_quux")
      response.body.should_not have_tag("li", "name_hi = value_bye")
    end
  end
end
