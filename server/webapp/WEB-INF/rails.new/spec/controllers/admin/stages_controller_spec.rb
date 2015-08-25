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
load File.join(File.dirname(__FILE__), 'stages_controller_examples.rb')

describe Admin::StagesController do
  include MockRegistryModule
  include TaskMother

  before do
    controller.stub(:populate_health_messages)
    controller.stub(:pipeline_pause_service).with().and_return(@pipeline_pause_service = double('Pipeline Pause Service'))
    controller.stub(:set_current_user)
  end
  include ConfigSaveStubbing
  describe "routes" do
    it "should resolve index" do
      {:get => "/admin/pipelines/dev/stages"}.should route_to(:controller => "admin/stages", :action => "index", :pipeline_name => "dev", :stage_parent => "pipelines")
    end

    it "should resolve new" do
      {:get => "/admin/pipelines/dev/stages/new"}.should route_to(:controller => "admin/stages", :action => "new", :pipeline_name => "dev", :stage_parent => "pipelines")
    end

    it "should resolve create" do
      {:post => "/admin/pipelines/dev/stages"}.should route_to(:controller => "admin/stages", :action => "create", :pipeline_name => "dev", :stage_parent => "pipelines")
    end

    it "should resolve edit/settings" do
      {:get => "/admin/pipelines/dev/stages/test.foo/settings"}.should route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "test.foo", :current_tab => "settings")
      {:get => "/admin/templates/dev/stages/test.foo/settings"}.should route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "templates", :pipeline_name => "dev", :stage_name => "test.foo", :current_tab => "settings")
    end

    it "should generate delete" do
      admin_stage_delete_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz.foo"
      {:delete => "/admin/pipelines/foo.bar/stages/baz.foo"}.should route_to(:controller => "admin/stages", :action => "destroy", :stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo")
    end

    it "should resolve edit/environment_variables" do
      {:get => "/admin/pipelines/dev/stages/baz.foo/environment_variables"}.should route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "baz.foo", :current_tab => "environment_variables")
    end

    it "should generate edit/settings" do
      admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "settings").should == "/admin/pipelines/foo.bar/stages/baz.foo/settings"
    end

    it "should generate edit/environment_variables" do
      admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "environment_variables").should == "/admin/pipelines/foo.bar/stages/baz.foo/environment_variables"
    end

    it "should resolve update/settings" do
      {:put => "/admin/pipelines/dev/stages/baz.foo/settings"}.should route_to(:controller => "admin/stages", :action => "update", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "baz.foo", :current_tab => "settings")
    end

    it "should generate update/settings" do
      admin_stage_update_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "settings").should == "/admin/pipelines/foo.bar/stages/baz.foo/settings"
    end

    it "should generate index" do
      admin_stage_listing_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages"
      {:get => "/admin/pipelines/foo.bar/stages"}.should route_to(:controller => "admin/stages", :action => "index", :stage_parent=>"pipelines", :pipeline_name => "foo.bar")
    end

    it "should generate new" do
      admin_stage_new_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/new"
    end

    it "should generate create" do
      admin_stage_create_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages"
    end

    it "should generate edit" do
      admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "my.stage", :current_tab => "settings").should == "/admin/pipelines/foo.bar/stages/my.stage/settings"
    end

    it "should generate increment_index" do
      admin_stage_increment_index_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz.foo/index/increment"
    end

    it "should generate decrement_index" do
      admin_stage_decrement_index_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz.foo/index/decrement"
    end

    it "should generate use template" do
      {:put => "/admin/pipelines/foo.bar/stages"}.should route_to(:controller => "admin/stages", :action => "use_template", :stage_parent=>"pipelines", :pipeline_name => "foo.bar")
      admin_stage_use_template_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages"
    end

  end

  describe "action" do
    before(:each) do
      controller.stub(:populate_config_validity)

      @cruise_config = BasicCruiseConfig.new()
      cruise_config_mother = GoConfigMother.new
      @pipeline_template = PipelineTemplateConfig.new(CaseInsensitiveString.new("template-name"), [StageConfigMother.manualStage("template-stage-name")].to_java(StageConfig))
      @cruise_config.addTemplate(@pipeline_template)
      @pipeline = cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))
      @pipeline_group = @cruise_config.findGroup("defaultGroup")
      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @user = Username.new(CaseInsensitiveString.new("loser"))
      controller.stub(:current_user).and_return(@user)
      @result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(@result)

      @go_config_service = double('Go Config Service')
      controller.stub(:go_config_service).and_return(@go_config_service)
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
    end

    describe "index" do

      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should set current tab param" do
        get :index, :pipeline_name => "pipeline-name", :stage_parent => "pipelines"

        controller.params[:current_tab].should == 'stages'
        assert_template layout: "pipelines/details"
      end

      it "should populate stage_usage map with stages that are currently referenced in materials" do
        upstream_stage = @pipeline.getStage(CaseInsensitiveString.new("stage-name"))
        downstream = PipelineConfigMother.pipelineConfig("downstream", MaterialConfigsMother.dependencyMaterialConfig("pipeline-name", "stage-name"), JobConfigs.new)
        @cruise_config.addPipeline("defaultGroup", downstream)

        get :index, :pipeline_name => "pipeline-name", :stage_parent => "pipelines"

        assigns[:stage_usage].contains(upstream_stage).should == true
      end

      it "should assign templates" do
        @cruise_config.addTemplate(PipelineTemplateConfig.new(CaseInsensitiveString.new("foo"), [StageConfigMother.stageWithTasks("stage_one")].to_java(StageConfig)))

        get :index, :pipeline_name => "pipeline-name", :stage_parent => "pipelines"

        assigns[:template_list][0].should == CaseInsensitiveString.new("foo")
        assigns[:template_list][1].should == CaseInsensitiveString.new("template-name")
      end

      it "should not bomb when there are no templates" do
        @cruise_config.getTemplates().removeTemplateNamed(CaseInsensitiveString.new("template-name"))

        get :index, :pipeline_name => "pipeline-name", :stage_parent => "pipelines"

        assigns[:template_list].should == []
      end
    end

    describe "new" do

      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
        controller.should_receive(:task_view_service).and_return(task_view_service = double("task_view_service"))
        task_view_service.should_receive(:getTaskViewModels).and_return(@tvms = [TaskViewModel.new(AntTask.new(), "new", "erb"), TaskViewModel.new(NantTask.new(), "new", "erb")].to_java(TaskViewModel))
      end

      it "should load a blank exec task in a blank job" do
        @go_config_service.stub(:registry)

        get :new, :pipeline_name => "pipeline-name", :stage_parent => "pipelines"

        new_job = JobConfig.new(CaseInsensitiveString.new(""), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))
        new_stage = StageConfig.new(CaseInsensitiveString.new(""), JobConfigs.new([new_job].to_java(JobConfig)))
        actual_stage = assigns[:stage]
        actual_stage.should == new_stage
        actual_stage.getJobs().get(0).tasks().first.should == AntTask.new
        assigns[:task_view_models] = @tvms
        assert_template layout: false
      end
    end

    describe "create" do
      before :all do
        set_up_registry
        task_preference = com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference.new(TaskMother::ApiTaskForTest.new)
        PluggableTaskConfigStore.store().setPreferenceFor("curl.plugin", task_preference)
      end

      after :all do
        unload_all_from_registry
        PluggableTaskConfigStore.store().removePreferenceFor("curl.plugin")
      end

      before :each do
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
        @pluggable_task_service = double('Pluggable_task_service')
        controller.stub(:pluggable_task_service).and_return(@pluggable_task_service)
      end

      it "should be able to create a stage with a pluggable task" do
        @pluggable_task_service.stub(:validate)
        task_view_service = double('Task View Service')
        controller.stub(:task_view_service).and_return(task_view_service)
        @new_task = PluggableTask.new("", PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("Url", false, nil)].to_java(ConfigurationProperty)))
        task_view_service.should_receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
        stub_save_for_success

        stage = {:name => "stage", :jobs => [{:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:foo => "bar"}}}]}
        pipeline_name = "pipeline-name"
        post :create, :stage_parent => "pipelines", :pipeline_name => pipeline_name, :config_md5 => "1234abcd", :stage => stage

        @cruise_config.getAllErrors().size.should == 0
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
        pipeline_config = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new(pipeline_name))
        pipeline_config.last().name().should == CaseInsensitiveString.new("stage")
        pipeline_config.last().getJobs().first().getTasks().first().instance_of?(PluggableTask).should == true
      end

      it "should validate pluggable tasks before create" do
        task_view_service = double('Task View Service')
        controller.stub(:task_view_service).and_return(task_view_service)
        @pluggable_task_service.stub(:validate) do |task|
          task.getConfiguration().getProperty("key").addError("key", "some error")
        end
        @new_task = PluggableTask.new("", PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("key", false, nil)].to_java(ConfigurationProperty)))
        task_view_service.should_receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
        stub_save_for_validation_error do |result, cruise_config, pipeline|
          result.badRequest(LocalizedMessage.string("SAVE_FAILED"))
        end
        task_view_service.should_receive(:getTaskViewModelsWith).with(anything).and_return(Object.new)

        job = {:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:key => "value"}}}
        stage = {:name => "stage", :jobs => [job]}
        post :create, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => stage

        task_to_be_saved = assigns[:pipeline].last().getJobs().first().getTasks().first()
        task_to_be_saved.instance_of?(PluggableTask).should == true
        task_to_be_saved.getConfiguration().getProperty("key").errors().getAll().size().should > 0
        task_to_be_saved.getConfiguration().getProperty("key").errors().getAllOn("key").get(0).should == "some error"
        assert_template "new"
        assert_template layout: false
        response.status.should == 400
      end

      it "should populate config_file_conflict when the md5 has already been changed" do
        stub_save_for_validation_error do |result, config, node|
          result.conflict(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end

        job = {:name => "job", :tasks => {:taskOptions => "ant", "ant" => {}}}
        post :create, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [job]}

        assigns[:config_file_conflict].should == true
        assert_save_arguments
      end

      it "should save a new stage" do
        stub_save_for_success

        job = {:name => "job", :tasks => {:taskOptions => "ant", "ant" => {}}}

        post :create, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [job]}

        @cruise_config.getAllErrors().size.should == 0
        @pipeline.size().should == 2
        @pipeline.get(1).name().should == CaseInsensitiveString.new("stage")
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::PipelineOrTemplateNode, ::ConfigUpdate::RefsAsUpdatedRefs
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_stage_listing_path
      end

      it "should show error message when config save fails for reasons other than validations" do
        controller.should_receive(:task_view_service).twice.and_return(task_view_service = double("task_view_service"))
        task_view_service.should_receive(:taskInstanceFor).with("exec").and_return(ExecTask.new())
        task_view_service.should_receive(:getTaskViewModelsWith).with(ExecTask.new('ls','','work')).and_return(tvms = [TaskViewModel.new(AntTask.new(), "new", "erb"), TaskViewModel.new(NantTask.new(), "new", "erb")].to_java(TaskViewModel))
        stub_save_for_validation_error do |result, config, node|
          result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]), HealthStateType.unauthorisedForPipeline("pipeline-name"))
        end

        post :create, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [{:name => "123", :tasks => {:taskOptions => "exec", "exec" => {:command => "ls", :workingDirectory => 'work'}}}]}

        assert_save_arguments
        assigns[:task_view_models].should == tvms
        assert_template "new"
        assert_template layout: false
        response.status.should == 401
      end

      it "should assign config_errors for display when save fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          @cruise_config.errors().add("base", "someError")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end

        post :create, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [{:name => "123", :tasks => {:taskOptions => "exec", "exec" => {:command => "ls", :workingDirectory => 'work'}}}]}

        assigns[:errors].size.should == 1
        assert_save_arguments
        assert_template "new"
        assert_template layout: false
        response.status.should == 400
      end

      it "should remove errors related to material before assigning config_errors " do
        stub_save_for_validation_error do |result, cruise_config, pipeline|
          cruise_config.errors().add("base", "someError")
          pipeline.addError("name", "bad-pipeline-name")
          pipeline.get(1).addError("name", "bad-stage-name")
          pipeline.get(1).getJobs().get(0).addError("name", "bad-job-name")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end

        post :create, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [{:name => "123"}]}

        assigns[:errors].size.should == 2
        assigns[:errors][0].should == "someError"
        assigns[:errors][1].should == "bad-pipeline-name"
        assert_save_arguments
        assert_template "new"
        assert_template layout: false
        response.status.should == 400
      end
    end

    describe "edit" do
      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should render error page if stage does not exist" do
        get :edit, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "does_not_exist", :current_tab => "permissions"
        assert_template "shared/config_error.html"
        assert_template layout: "layouts/application"
      end
    end

    describe "edit_permissions" do

      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should load stage, pipeline_group, autocomplete_users and autocomplete_roles" do
        controller.stub(:user_service).and_return(user_service = Object.new)

        user_service.should_receive(:rolesThatCanOperateOnStage).and_return(["role1", "role2", "role3"])
        user_service.should_receive(:usersThatCanOperateOnStage).and_return(["user1", "user2", "user3"])

        get :edit, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :current_tab => "permissions"

        assigns[:stage].should == @pipeline.get(0)
        assigns[:pipeline_group].should == @pipeline_group
        assigns[:autocomplete_users].should == ["user1", "user2", "user3"].to_json
        assigns[:autocomplete_roles].should == ["role1", "role2", "role3"].to_json
        assert_template "permissions"
        assert_template layout: "pipelines/stage"
      end
    end

    describe "edit_permissions_template" do

      before do
        @template_config_service = double('Template Config Service')
        controller.stub(:template_config_service).and_return(@template_config_service)
        @template_config_service.should_receive(:loadForEdit).with("template-name", @user, @result).and_return(ConfigForEdit.new(@pipeline_template, @cruise_config, @cruise_config))
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("template-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should assign all users and all roles when loading permissions autocomplete for a template" do
        controller.stub(:user_service).and_return(user_service = Object.new)
        user_service.should_receive(:allRoleNames).and_return(["role1", "role2", "role3", "role4"])
        user_service.should_receive(:allUsernames).and_return(["user1", "user2", "user3", "user4"])

        get :edit, :stage_parent => "templates", :pipeline_name => "template-name", :stage_name => "template-stage-name", :current_tab => "permissions"

        assigns[:stage].should == @pipeline_template.get(0)
        assigns[:pipeline_group].should == nil
        assigns[:autocomplete_users].should == ["user1", "user2", "user3", "user4"].to_json
        assigns[:autocomplete_roles].should == ["role1", "role2", "role3", "role4"].to_json
        assert_template "permissions"
        assert_template layout: "templates/stage"
      end
    end

    describe "update" do

      before do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should update stage instance with form fields and save it" do
        stub_save_for_success

        put :update, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "permissions", :stage => {:approval => {:type => "manual"},:variables =>[{:name=>"key", :valueForDisplay=>"value"}]}

        assigns[:stage].getApproval().getType().should == "manual"
        environment_variable = assigns[:stage].variables().get(0)
        environment_variable.name.should == "key"
        environment_variable.value.should == "value"
        environment_variable.valueForDisplay().should == "value"
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::StageNode, ConfigUpdate::NodeAsSubject
        response.location.should =~ /\/admin\/pipelines\/pipeline-name\/stages\/stage-name\/permissions\?fm=#{uuid_pattern}$/
        response.status.should == 200
        response.body.should == "Saved successfully"
      end

      it "should redirect to edit form for the new stage-name when name is changed" do
        stub_save_for_success

        put :update, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "permissions", :stage => {:name => "new-stage-name"}

        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::StageNode, ConfigUpdate::NodeAsSubject
        response.location.should =~ /\/admin\/pipelines\/pipeline-name\/stages\/new-stage-name\/permissions\?fm=#{uuid_pattern}$/
        response.status.should == 200
        response.body.should == "Saved successfully"
      end

      it "should update environment fields as empty if params not set" do
        stage_config = @pipeline.getStage(CaseInsensitiveString.new("stage-name"))
        stage_config.variables().add("key1","value1")
        stage_config.variables().add("key2","value2")
        @pipeline.set(0,stage_config)
        stub_save_for_success

        put :update, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "settings", :stage => {:name => "g", :approval => {:type => "manual"}}, :default_as_empty_list => ["stage>variables"]

        assigns[:stage].variables().isEmpty().should == true
        assert_save_arguments
      end

      it "should update stage permissions" do
        stub_save_for_success

        put :update, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "settings", :stage => { :securityMode => "define", :operateUsers => [{ :name => "user1"}, {:name => "user2"}], :operateRoles => [{ :name => "role1"}, {:name => "role2"}]}

        assigns[:stage].getOperateUsers().get(0).should == AdminUser.new(CaseInsensitiveString.new("user1"))
        assigns[:stage].getOperateUsers().get(1).should == AdminUser.new(CaseInsensitiveString.new("user2"))
        assigns[:stage].getOperateRoles().get(0).should == AdminRole.new(CaseInsensitiveString.new("role1"))
        assigns[:stage].getOperateRoles().get(1).should == AdminRole.new(CaseInsensitiveString.new("role2"))

        assert_save_arguments
      end

      it "should render the form again if save fails" do
        stub_save_for_validation_error do |result, _, _|
          result.conflict(LocalizedMessage.modifiedBy("loser", Time.now.to_s))
        end

        put :update, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "permissions", :stage => {:name => "new-stage-name"}

        response.location.should be_nil
        assert_template "permissions"
        assert_template layout: false
        response.status.should == 409
      end
    end

    describe "delete" do

      before do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should delete the given stage" do
        stub_save_for_success

        @pipeline.add(StageConfigMother.oneBuildPlanWithResourcesAndMaterials('stage-to-delete'))
        delete :destroy, :pipeline_name => "pipeline-name", :stage_name => "stage-to-delete", :config_md5 => "1234abcd", :stage_parent => "pipelines"
        @pipeline.size().should == 1
        @pipeline.get(0).name().should == CaseInsensitiveString.new("stage-name")

        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::PipelineOrTemplateNode, ConfigUpdate::PipelineStageSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end
    end

    describe "increment and decrement for pipelines" do

      before do
        @stage_parent = "pipelines"
      end

      it_should_behave_like :stages_controller
    end

    describe "increment and decrement for templates" do

      before :each do
        @is_template = true
        @pipeline = PipelineTemplateConfig.new(CaseInsensitiveString.new("pipeline-name"), [].to_java(StageConfig))
        @cruise_config.addTemplate(@pipeline)
        @stage_parent = "templates"
      end

      it_should_behave_like :stages_controller
    end

    describe "use template action" do

      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.stub(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should use template" do
        stub_save_for_success
        @cruise_config.addTemplate(PipelineTemplateConfig.new(CaseInsensitiveString.new("foo-template"), [StageConfigMother.stageWithTasks("stage_one")].to_java(StageConfig)))

        put :use_template, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :pipeline => {com.thoughtworks.go.config.PipelineConfig::TEMPLATE_NAME => "foo-template", com.thoughtworks.go.config.PipelineConfig::CONFIGURATION_TYPE => com.thoughtworks.go.config.PipelineConfig::CONFIGURATION_TYPE_TEMPLATE}

        @pipeline.getTemplateName().should == CaseInsensitiveString.new("foo-template")

        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::PipelineNode, ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end
    end
  end
end
