#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'
require_relative 'stages_controller_examples'

describe Admin::StagesController do
  include MockRegistryModule
  include TaskMother

  before do
    allow(controller).to receive(:pipeline_pause_service).with(no_args).and_return(@pipeline_pause_service = double('Pipeline Pause Service'))
    allow(controller).to receive(:task_view_service).with(no_args).and_return(@task_view_service = double('task_view_service'))
    allow(controller).to receive(:set_current_user)
    allow(controller).to receive(:go_config_service).with(no_args).and_return(@go_config_service = double('@go_config_service'))
  end
  include ConfigSaveStubbing
  describe "routes" do
    it "should resolve index" do
      expect({:get => "/admin/pipelines/dev/stages"}).to route_to(:controller => "admin/stages", :action => "index", :pipeline_name => "dev", :stage_parent => "pipelines")
    end

    it "should resolve new" do
      expect({:get => "/admin/pipelines/dev/stages/new"}).to route_to(:controller => "admin/stages", :action => "new", :pipeline_name => "dev", :stage_parent => "pipelines")
    end

    it "should resolve create" do
      expect({:post => "/admin/pipelines/dev/stages"}).to route_to(:controller => "admin/stages", :action => "create", :pipeline_name => "dev", :stage_parent => "pipelines")
    end

    it "should resolve edit/settings" do
      expect({:get => "/admin/pipelines/dev/stages/test.foo/settings"}).to route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "test.foo", :current_tab => "settings")
      expect({:get => "/admin/templates/dev/stages/test.foo/settings"}).to route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "templates", :pipeline_name => "dev", :stage_name => "test.foo", :current_tab => "settings")
    end

    it "should generate delete" do
      expect(admin_stage_delete_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz.foo")
      expect({:delete => "/admin/pipelines/foo.bar/stages/baz.foo"}).to route_to(:controller => "admin/stages", :action => "destroy", :stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo")
    end

    it "should resolve edit/environment_variables" do
      expect({:get => "/admin/pipelines/dev/stages/baz.foo/environment_variables"}).to route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "baz.foo", :current_tab => "environment_variables")
    end

    it "should generate edit/settings" do
      expect(admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "settings")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/settings")
    end

    it "should generate edit/environment_variables" do
      expect(admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "environment_variables")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/environment_variables")
    end

    it "should resolve update/settings" do
      expect({:put => "/admin/pipelines/dev/stages/baz.foo/settings"}).to route_to(:controller => "admin/stages", :action => "update", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "baz.foo", :current_tab => "settings")
    end

    it "should generate update/settings" do
      expect(admin_stage_update_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "settings")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/settings")
    end

    it "should generate index" do
      expect(admin_stage_listing_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages")
      expect({:get => "/admin/pipelines/foo.bar/stages"}).to route_to(:controller => "admin/stages", :action => "index", :stage_parent=>"pipelines", :pipeline_name => "foo.bar")
    end

    it "should generate new" do
      expect(admin_stage_new_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/new")
    end

    it "should generate create" do
      expect(admin_stage_create_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages")
    end

    it "should generate edit" do
      expect(admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "my.stage", :current_tab => "settings")).to eq("/admin/pipelines/foo.bar/stages/my.stage/settings")
    end

    it "should generate increment_index" do
      expect(admin_stage_increment_index_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/index/increment")
    end

    it "should generate decrement_index" do
      expect(admin_stage_decrement_index_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/index/decrement")
    end

    it "should generate use template" do
      expect({:put => "/admin/pipelines/foo.bar/stages"}).to route_to(:controller => "admin/stages", :action => "use_template", :stage_parent=>"pipelines", :pipeline_name => "foo.bar")
      expect(admin_stage_use_template_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages")
    end

  end

  describe "action" do
    before(:each) do
      allow(controller).to receive(:populate_config_validity)

      @cruise_config = BasicCruiseConfig.new()
      cruise_config_mother = GoConfigMother.new
      @pipeline_template = PipelineTemplateConfig.new(CaseInsensitiveString.new("template-name"), [StageConfigMother.manualStage("template-stage-name")].to_java(StageConfig))
      @cruise_config.addTemplate(@pipeline_template)
      @pipeline = cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))
      @pipeline_group = @cruise_config.findGroup("defaultGroup")
      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @user = Username.new(CaseInsensitiveString.new("loser"))
      allow(controller).to receive(:current_user).and_return(@user)
      @result = HttpLocalizedOperationResult.new
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result)

      @go_config_service = double('Go Config Service')
      allow(controller).to receive(:go_config_service).and_return(@go_config_service)
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
    end

    describe "index" do

      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        allow(@go_config_service).to receive(:rolesForUser).and_return(nil)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should set current tab param" do
        get :index, params:{:pipeline_name => "pipeline-name", :stage_parent => "pipelines"}

        expect(controller.params[:current_tab]).to eq('stages')
        assert_template layout: "pipelines/details"
      end

      it "should populate stage_usage map with stages that are currently referenced in materials" do
        upstream_stage = @pipeline.getStage(CaseInsensitiveString.new("stage-name"))
        downstream = PipelineConfigMother.pipelineConfig("downstream", MaterialConfigsMother.dependencyMaterialConfig("pipeline-name", "stage-name"), JobConfigs.new)
        @cruise_config.addPipeline("defaultGroup", downstream)

        get :index, params:{:pipeline_name => "pipeline-name", :stage_parent => "pipelines"}

        expect(assigns[:stage_usage].contains(upstream_stage)).to eq(true)
      end

      it "should assign templates" do
        @cruise_config.addTemplate(PipelineTemplateConfig.new(CaseInsensitiveString.new("foo"), [StageConfigMother.stageWithTasks("stage_one")].to_java(StageConfig)))
        @template_config_service = double('Template Config Service')
        allow(controller).to receive(:template_config_service).and_return(@template_config_service)

        list_of_templates = [TemplatesViewModel.new(@cruise_config.getTemplateByName(CaseInsensitiveString.new("foo")), true, true), TemplatesViewModel.new(@cruise_config.getTemplateByName(CaseInsensitiveString.new("template-name")), true, true)]
        expect(@template_config_service).to receive(:getTemplateViewModels).with(anything).and_return(list_of_templates)

        get :index, params:{:pipeline_name => "pipeline-name", :stage_parent => "pipelines"}

        expect(assigns[:template_list]).to eq(list_of_templates)
      end

      it "should not bomb when there are no templates" do
        @cruise_config.getTemplates().removeTemplateNamed(CaseInsensitiveString.new("template-name"))

        get :index, params:{:pipeline_name => "pipeline-name", :stage_parent => "pipelines"}

        expect(assigns[:template_list]).to eq([])
      end
    end

    describe "new" do

      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
        expect(@task_view_service).to receive(:getTaskViewModels).and_return(@tvms = [TaskViewModel.new(AntTask.new(), "new"), TaskViewModel.new(NantTask.new(), "new")].to_java(TaskViewModel))
      end

      it "should load a blank exec task in a blank job" do
        allow(@go_config_service).to receive(:registry)

        get :new, params:{:pipeline_name => "pipeline-name", :stage_parent => "pipelines"}

        new_job = JobConfig.new(CaseInsensitiveString.new(""), ResourceConfigs.new, ArtifactTypeConfigs.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))
        new_stage = StageConfig.new(CaseInsensitiveString.new(""), JobConfigs.new([new_job].to_java(JobConfig)))
        actual_stage = assigns[:stage]
        expect(actual_stage).to eq(new_stage)
        expect(actual_stage.getJobs().get(0).tasks().first).to eq(AntTask.new)
        assigns[:task_view_models] = @tvms
        assert_template layout: false
      end
    end

    describe "create" do
      before :each do
        set_up_registry
        task_preference = com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference.new(TaskMother::ApiTaskForTest.new)
        PluggableTaskConfigStore.store().setPreferenceFor("curl.plugin", task_preference)
      end

      after :each do
        unload_all_from_registry
        PluggableTaskConfigStore.store().removePreferenceFor("curl.plugin")
      end

      before :each do
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
        @pluggable_task_service = double('Pluggable_task_service')
        allow(controller).to receive(:pluggable_task_service).and_return(@pluggable_task_service)
      end

      it "should be able to create a stage with a pluggable task" do
        allow(@pluggable_task_service).to receive(:validate)
        @new_task = PluggableTask.new( PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("Url", false, nil)].to_java(ConfigurationProperty)))
        expect(@task_view_service).to receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
        stub_save_for_success

        stage = {:name => "stage", :jobs => [{:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:foo => "bar"}}}]}
        pipeline_name = "pipeline-name"
        post :create, params:{:stage_parent => "pipelines", :pipeline_name => pipeline_name, :config_md5 => "1234abcd", :stage => stage}

        expect(@cruise_config.getAllErrors().size).to eq(0)
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
        pipeline_config = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new(pipeline_name))
        expect(pipeline_config.last().name()).to eq(CaseInsensitiveString.new("stage"))
        expect(pipeline_config.last().getJobs().first().getTasks().first().instance_of?(PluggableTask)).to eq(true)
      end

      it "should validate pluggable tasks before create" do
        allow(@pluggable_task_service).to receive(:validate) do |task|
          task.getConfiguration().getProperty("key").addError("key", "some error")
        end
        @new_task = PluggableTask.new( PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("key", false, nil)].to_java(ConfigurationProperty)))
        expect(@task_view_service).to receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
        stub_save_for_validation_error do |result, cruise_config, pipeline|
          result.badRequest("Save failed, see errors below")
        end
        expect(@task_view_service).to receive(:getTaskViewModelsWith).with(anything).and_return(Object.new)

        job = {:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:key => "value"}}}
        stage = {:name => "stage", :jobs => [job]}
        post :create, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => stage}

        task_to_be_saved = assigns[:pipeline].last().getJobs().first().getTasks().first()
        expect(task_to_be_saved.instance_of?(PluggableTask)).to eq(true)
        expect(task_to_be_saved.getConfiguration().getProperty("key").errors().getAll().size()).to be > 0
        expect(task_to_be_saved.getConfiguration().getProperty("key").errors().getAllOn("key").get(0)).to eq("some error")
        assert_template "new"
        assert_template layout: false
        expect(response.status).to eq(400)
      end

      it "should populate config_file_conflict when the md5 has already been changed" do
        stub_save_for_validation_error do |result, config, node|
          result.conflict('some message')
        end

        expect(@task_view_service).to receive(:taskInstanceFor).with("ant").and_return(AntTask.new())
        expect(@task_view_service).to receive(:getTaskViewModelsWith).with(AntTask.new()).and_return(tvms = [TaskViewModel.new(AntTask.new(), "new"), TaskViewModel.new(NantTask.new(), "new")].to_java(TaskViewModel))


        job = {:name => "job", :tasks => {:taskOptions => "ant", "ant" => {}}}
        post :create, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [job]}}

        expect(assigns[:config_file_conflict]).to eq(true)
        assert_save_arguments
      end

      it "should save a new stage" do
        stub_save_for_success
        expect(@task_view_service).to receive(:taskInstanceFor).with("ant").and_return(AntTask.new())


        job = {:name => "job", :tasks => {:taskOptions => "ant", "ant" => {}}}

        post :create, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [job]}}

        expect(@cruise_config.getAllErrors().size).to eq(0)
        expect(@pipeline.size()).to eq(2)
        expect(@pipeline.get(1).name()).to eq(CaseInsensitiveString.new("stage"))
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::PipelineOrTemplateNode, ::ConfigUpdate::RefsAsUpdatedRefs
        expect(response.body).to eq('Saved successfully')
        expect(URI.parse(response.location).path).to eq(admin_stage_listing_path)
      end

      it "should show error message when config save fails for reasons other than validations" do
        expect(@task_view_service).to receive(:taskInstanceFor).with("exec").and_return(ExecTask.new())
        expect(@task_view_service).to receive(:getTaskViewModelsWith).with(ExecTask.new('ls','','work')).and_return(tvms = [TaskViewModel.new(AntTask.new(), "new"), TaskViewModel.new(NantTask.new(), "new")].to_java(TaskViewModel))
        stub_save_for_validation_error do |result, config, node|
          result.forbidden('some message', HealthStateType.forbiddenForPipeline("pipeline-name"))
        end

        post :create, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [{:name => "123", :tasks => {:taskOptions => "exec", "exec" => {:command => "ls", :workingDirectory => 'work'}}}]}}

        assert_save_arguments
        expect(assigns[:task_view_models]).to eq(tvms)
        assert_template "new"
        assert_template layout: false
        expect(response.status).to eq(403)
      end

      it "should assign config_errors for display when save fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          @cruise_config.errors().add("base", "someError")
          result.badRequest('some message')
        end

        expect(@task_view_service).to receive(:taskInstanceFor).with("exec").and_return(ExecTask.new('ls', '', 'work'))
        expect(@task_view_service).to receive(:getTaskViewModelsWith).with(ExecTask.new('ls','','work')).and_return(tvms = [TaskViewModel.new(AntTask.new(), "new"), TaskViewModel.new(NantTask.new(), "new")].to_java(TaskViewModel))


        post :create, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [{:name => "123", :tasks => {:taskOptions => "exec", "exec" => {:command => "ls", :workingDirectory => 'work'}}}]}}

        expect(assigns[:errors].size).to eq(1)
        assert_save_arguments
        assert_template "new"
        assert_template layout: false
        expect(response.status).to eq(400)
      end

      it "should remove errors related to material before assigning config_errors " do
        stub_save_for_validation_error do |result, cruise_config, pipeline|
          cruise_config.errors().add("base", "someError")
          pipeline.addError("name", "bad-pipeline-name")
          pipeline.get(1).addError("name", "bad-stage-name")
          pipeline.get(1).getJobs().get(0).addError("name", "bad-job-name")
          result.badRequest('some message')
        end

        expect(@task_view_service).to receive(:getTaskViewModelsWith).with(anything).and_return(tvms = [].to_java(TaskViewModel))

        post :create, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :stage => {:name =>  "stage", :type => "cruise", :jobs => [{:name => "123"}]}}

        expect(assigns[:errors].size).to eq(2)
        expect(assigns[:errors][0]).to eq("someError")
        expect(assigns[:errors][1]).to eq("bad-pipeline-name")
        assert_save_arguments
        assert_template "new"
        assert_template layout: false
        expect(response.status).to eq(400)
      end
    end

    describe "edit" do
      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should render error page if stage does not exist" do
        get :edit, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "does_not_exist", :current_tab => "permissions"}
        assert_template "shared/config_error.html"
        assert_template layout: "layouts/application"
      end
    end

    describe "edit_permissions" do

      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should load stage, pipeline_group, autocomplete_users and autocomplete_roles" do
        allow(controller).to receive(:user_service).and_return(user_service = instance_double('com.thoughtworks.go.server.service.UserService'))

        expect(user_service).to receive(:rolesThatCanOperateOnStage).and_return(["role1", "role2", "role3"])
        expect(user_service).to receive(:usersThatCanOperateOnStage).and_return(["user1", "user2", "user3"])

        get :edit, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :current_tab => "permissions"}

        expect(assigns[:stage]).to eq(@pipeline.get(0))
        expect(assigns[:pipeline_group]).to eq(@pipeline_group)
        expect(assigns[:autocomplete_users]).to eq(["user1", "user2", "user3"].to_json)
        expect(assigns[:autocomplete_roles]).to eq(["role1", "role2", "role3"].to_json)
        assert_template "permissions"
        assert_template layout: "pipelines/stage"
      end
    end

    describe "edit_permissions_template" do

      before do
        @template_config_service = double('Template Config Service')
        allow(controller).to receive(:template_config_service).and_return(@template_config_service)
        expect(@template_config_service).to receive(:loadForEdit).with("template-name", @user, @result).and_return(ConfigForEdit.new(@pipeline_template, @cruise_config, @cruise_config))
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("template-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should assign all users and all roles when loading permissions autocomplete for a template" do
        allow(controller).to receive(:user_service).and_return(user_service = instance_double('com.thoughtworks.go.server.service.UserService'))
        expect(user_service).to receive(:allRoleNames).and_return(["role1", "role2", "role3", "role4"])
        expect(user_service).to receive(:allUsernames).and_return(["user1", "user2", "user3", "user4"])

        get :edit, params:{:stage_parent => "templates", :pipeline_name => "template-name", :stage_name => "template-stage-name", :current_tab => "permissions"}

        expect(assigns[:stage]).to eq(@pipeline_template.get(0))
        expect(assigns[:pipeline_group]).to eq(nil)
        expect(assigns[:autocomplete_users]).to eq(["user1", "user2", "user3", "user4"].to_json)
        expect(assigns[:autocomplete_roles]).to eq(["role1", "role2", "role3", "role4"].to_json)
        assert_template "permissions"
        assert_template layout: "templates/stage"
      end
    end

    describe "update" do

      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should update stage instance with form fields and save it" do
        stub_save_for_success

        put :update, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "permissions", :stage => {:approval => {:type => "manual"},:variables =>[{:name=>"key", :valueForDisplay=>"value"}]}}

        expect(assigns[:stage].getApproval().getType()).to eq("manual")
        environment_variable = assigns[:stage].variables().get(0)
        expect(environment_variable.name).to eq("key")
        expect(environment_variable.value).to eq("value")
        expect(environment_variable.valueForDisplay()).to eq("value")
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::StageNode, ConfigUpdate::NodeAsSubject
        expect(response.location).to match(/\/admin\/pipelines\/pipeline-name\/stages\/stage-name\/permissions\?fm=#{uuid_pattern}$/)
        expect(response.status).to eq(200)
        expect(response.body).to eq("Saved successfully")
      end

      it "should redirect to edit form for the new stage-name when name is changed" do
        stub_save_for_success

        put :update, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "permissions", :stage => {:name => "new-stage-name"}}

        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::StageNode, ConfigUpdate::NodeAsSubject
        expect(response.location).to match(/\/admin\/pipelines\/pipeline-name\/stages\/new-stage-name\/permissions\?fm=#{uuid_pattern}$/)
        expect(response.status).to eq(200)
        expect(response.body).to eq("Saved successfully")
      end

      it "should update environment fields as empty if params not set" do
        stage_config = @pipeline.getStage(CaseInsensitiveString.new("stage-name"))
        stage_config.variables().add("key1","value1")
        stage_config.variables().add("key2","value2")
        @pipeline.set(0,stage_config)
        stub_save_for_success

        put :update, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "settings", :stage => {:name => "g", :approval => {:type => "manual"}}, :default_as_empty_list => ["stage>variables"]}

        expect(assigns[:stage].variables().isEmpty()).to eq(true)
        assert_save_arguments
      end

      it "should update stage permissions" do
        stub_save_for_success

        put :update, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "settings", :stage => { :securityMode => "define", :operateUsers => [{ :name => "user1"}, {:name => "user2"}], :operateRoles => [{ :name => "role1"}, {:name => "role2"}]}}

        expect(assigns[:stage].getOperateUsers().get(0)).to eq(AdminUser.new(CaseInsensitiveString.new("user1")))
        expect(assigns[:stage].getOperateUsers().get(1)).to eq(AdminUser.new(CaseInsensitiveString.new("user2")))
        expect(assigns[:stage].getOperateRoles().get(0)).to eq(AdminRole.new(CaseInsensitiveString.new("role1")))
        expect(assigns[:stage].getOperateRoles().get(1)).to eq(AdminRole.new(CaseInsensitiveString.new("role2")))

        assert_save_arguments
      end

      it "should render the form again if save fails" do
        stub_save_for_validation_error do |result, _, _|
          result.conflict("modified already")
        end

        put :update, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :stage_name => "stage-name", :config_md5 => "1234abcd", :current_tab => "permissions", :stage => {:name => "new-stage-name"}}

        expect(response.location).to be_nil
        assert_template "permissions"
        assert_template layout: false
        expect(response.status).to eq(409)
      end
    end

    describe "delete" do

      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
        allow(@go_config_service).to receive(:rolesForUser).and_return(nil)
      end

      it "should delete the given stage" do
        stub_save_for_success

        @pipeline.add(StageConfigMother.oneBuildPlanWithResourcesAndMaterials('stage-to-delete'))
        delete :destroy, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-to-delete", :config_md5 => "1234abcd", :stage_parent => "pipelines"}
        expect(@pipeline.size()).to eq(1)
        expect(@pipeline.get(0).name()).to eq(CaseInsensitiveString.new("stage-name"))

        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::PipelineOrTemplateNode, ConfigUpdate::PipelineStageSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end
    end

    describe "increment and decrement for pipelines" do

      before do
        @stage_parent = "pipelines"
        allow(@go_config_service).to receive(:rolesForUser).and_return(nil)
      end

      it_should_behave_like :stages_controller
    end

    describe "increment and decrement for templates" do

      before :each do
        allow(@go_config_service).to receive(:rolesForUser).and_return(nil)
        @is_template = true
        @pipeline = PipelineTemplateConfig.new(CaseInsensitiveString.new("pipeline-name"), [].to_java(StageConfig))
        @cruise_config.addTemplate(@pipeline)
        @stage_parent = "templates"
      end

      it_should_behave_like :stages_controller
    end

    describe "use template action" do

      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        allow(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
        allow(@go_config_service).to receive(:rolesForUser).and_return(nil)
      end

      it "should use template" do
        stub_save_for_success
        @cruise_config.addTemplate(PipelineTemplateConfig.new(CaseInsensitiveString.new("foo-template"), [StageConfigMother.stageWithTasks("stage_one")].to_java(StageConfig)))

        put :use_template, params:{:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :pipeline => {com.thoughtworks.go.config.PipelineConfig::TEMPLATE_NAME => "foo-template", com.thoughtworks.go.config.PipelineConfig::CONFIGURATION_TYPE => com.thoughtworks.go.config.PipelineConfig::CONFIGURATION_TYPE_TEMPLATE}}

        expect(@pipeline.getTemplateName()).to eq(CaseInsensitiveString.new("foo-template"))

        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::PipelineNode, ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end
    end

    describe "config_change" do
      describe "security" do
        it 'should allow anyone, with security disabled' do
          disable_security
          expect(controller).to allow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end

        it 'should disallow anonymous users, with security enabled' do
          enable_security
          login_as_anonymous
          expect(controller).to disallow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end

        it 'should disallow normal users, with security enabled' do
          login_as_user
          expect(controller).to disallow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end

        it 'should allow admin users, with security enabled' do
          login_as_admin
          expect(controller).to allow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end

        it 'should disallow pipeline group admin users, with security enabled' do
          login_as_group_admin
          expect(controller).to disallow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end
      end

      it "should route to action" do
        login_as_admin

        expect(:get => "admin/config_change/between/md5_value_2/and/md5_value_1").to route_to({:controller => "admin/stages", :action => "config_change", :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        expect(controller.send(:admin_config_change_path, :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1")).to eq("/admin/config_change/between/md5_value_2/and/md5_value_1")
      end

      it "should generate the correct route" do
        login_as_admin

        expect(controller.send(:admin_config_change_path, :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1")).to eq("/admin/config_change/between/md5_value_2/and/md5_value_1")
      end

      it "should assign config changes for given md5" do
        login_as_admin

        result = HttpLocalizedOperationResult.new
        expect(@go_config_service).to receive(:configChangesFor).with("md5_value_2", "md5_value_1", result).and_return("changes_string")
        get :config_change, params:{:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"}
        expect(assigns(:changes)).to eq "changes_string"
      end

      it "should assign error message if getting config changes for given md5 fails" do
        login_as_admin

        result = HttpLocalizedOperationResult.new
        expect(@go_config_service).to receive(:configChangesFor).with("md5_value_2", "md5_value_1", result)
        expect(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:httpCode).and_return(400)
        allow(result).to receive(:message).and_return("no config version found")
        get :config_change, params:{:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"}
        expect(assigns(:config_change_error_message)).to eq "no config version found"
      end

      it "should assign message if config changes is nil because given md5 is the first revision in repo" do
        login_as_admin

        result = HttpLocalizedOperationResult.new
        expect(@go_config_service).to receive(:configChangesFor).with("md5_value_2", "md5_value_1", result).and_return(nil)
        expect(result).to receive(:isSuccessful).and_return(true)
        get :config_change, params:{:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"}
        expect(assigns(:config_change_error_message)).to eq "This is the first entry in the config versioning. Please refer config tab to view complete configuration during this run."
      end
    end
  end
end
