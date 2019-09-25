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

describe Admin::JobsController do
  include ConfigSaveStubbing
  include MockRegistryModule
  include TaskMother

  def add_resource(job_name, resource)
    @pipeline.getFirstStageConfig().getJobs().getJob(CaseInsensitiveString.new(job_name)).addResourceConfig(resource)
  end

  before :each do
    allow(controller).to receive(:populate_config_validity)
    @go_config_service = double('Go Config Service')
    allow(controller).to receive(:go_config_service).and_return(@go_config_service)

    @pipeline_pause_service = double('Pipeline Pause Service')
    allow(controller).to receive(:pipeline_pause_service).and_return(@pipeline_pause_service)
    @task_view_service = double('task_view_service')
    allow(controller).to receive(:task_view_service).and_return(@task_view_service)
    @pluggable_task_service = double('Pluggable_task_service')
    allow(controller).to receive(:pluggable_task_service).and_return(@pluggable_task_service)
  end

  describe "routes" do
    it "should resolve new" do
      expect({:get => "/admin/pipelines/dev/stages/test.1/jobs/new"}).to route_to(:controller => "admin/jobs", :action => "new", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
      expect({:get => "/admin/templates/dev/stages/test.1/jobs/new"}).to route_to(:controller => "admin/jobs", :action => "new", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "templates")
      expect(admin_job_new_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/test.1/jobs/new")
      expect(admin_job_new_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "templates")).to eq("/admin/templates/foo.bar/stages/test.1/jobs/new")
    end

    it "should resolve create" do
      expect({:post => "/admin/pipelines/dev/stages/test.1/jobs"}).to route_to(:controller => "admin/jobs", :action => "create", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
      expect(admin_job_create_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/test.1/jobs")
    end

    it "should resolve destroy" do
      expect({:delete => "/admin/pipelines/dev/stages/test.1/job/job.1"}).to route_to(:controller => "admin/jobs", :action => "destroy", :pipeline_name => "dev", :stage_name => "test.1", :job_name=> "job.1", :stage_parent => "pipelines")
    end

    it "should generate index" do
      expect({:get => "/admin/pipelines/dev/stages/test.1/jobs"}).to route_to(:controller => "admin/jobs", :action => "index", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
      expect(admin_job_listing_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/test.1/jobs")
    end

    it "should generate destroy" do
      # Cannot have route_for for DELETE as route_for does not honor the :method => :delete attribute
      expect(admin_job_delete_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :job_name => "job.1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/test.1/job/job.1")
    end

    it "should generate route for tabs" do
      expect({:get => "/admin/pipelines/dev/stages/test.1/job/job.1/tabs"}).to route_to(:controller => "admin/jobs", :action => "edit", :pipeline_name => "dev", :stage_name => "test.1", :job_name=> "job.1", :stage_parent => "pipelines", :current_tab => "tabs")
      expect(admin_job_edit_path(:pipeline_name => "foo.bar", :stage_name => "foo.bar", :job_name => "foo.bar", :current_tab => "tabs", :stage_parent => "templates")).to eq("/admin/templates/foo.bar/stages/foo.bar/job/foo.bar/tabs")
      expect(admin_job_edit_path(:pipeline_name => "foo.bar", :stage_name => "foo.bar", :job_name => "foo.bar", :current_tab => "tabs", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/foo.bar/job/foo.bar/tabs")
    end
  end

  describe "action" do
    before(:each) do
      @cruise_config = BasicCruiseConfig.new()
      @pipeline = PipelineConfigMother.createPipelineConfig("pipeline-name", "stage-name", ["job-1", "job-2", "job-3"].to_java(java.lang.String))
      @cruise_config.addPipeline("defaultGroup", @pipeline)

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @user = Username.new(CaseInsensitiveString.new("loser"))
      allow(controller).to receive(:current_user).and_return(@user)
      @result = HttpLocalizedOperationResult.new
      @load_result = HttpLocalizedOperationResult.new
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(@result, @load_result)

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end


    describe "index" do

      it "should assign jobs" do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)

        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)

        get :index, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :stage_parent => "pipelines"}

        expect(assigns[:pipeline]).to eq(@pipeline)
        expect(assigns[:stage]).to eq(@pipeline.get(0))
        expect(assigns[:jobs]).not_to eq(nil)
        expect(assigns[:jobs].size).to eq(3)
        expect(assigns[:jobs].get(0).name()).to eq(CaseInsensitiveString.new("job-1"))
        expect(assigns[:jobs].get(1).name()).to eq(CaseInsensitiveString.new("job-2"))
        expect(assigns[:jobs].get(2).name()).to eq(CaseInsensitiveString.new("job-3"))
        assert_template layout: "pipelines/stage"
      end
    end

    describe "new" do
      it "should render a new job" do
        expect(@task_view_service).to receive(:getTaskViewModels).and_return(tvms = [TaskViewModel.new(AntTask.new(), "new"), TaskViewModel.new(NantTask.new(), "new")].to_java(TaskViewModel))
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        add_resource("job-1", "windows")
        add_resource("job-1", "linux")
        add_resource("job-2", "windows-xp")
        add_resource("job-2", "solaris")
        allow(@go_config_service).to receive(:registry)

        get :new, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :stage_parent => "pipelines"}

        expect(assigns[:pipeline]).to eq(@pipeline)
        expect(assigns[:stage]).to eq(@pipeline.get(0))
        expect(assigns[:autocomplete_resources]).to eq(['linux','solaris','windows','windows-xp'].to_json)
        expect(assigns[:task_view_models]).to eq(tvms)

        actual_job_assigned = assigns[:job]
        job_config_new = JobConfig.new(CaseInsensitiveString.new(""), ResourceConfigs.new, ArtifactTypeConfigs.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))
        expect(actual_job_assigned).to eq(job_config_new)
        expect(actual_job_assigned.tasks().first).to eq(AntTask.new)
        assert_template layout: false
      end
    end

    describe "delete" do
      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
      end

      it "should delete the given job" do
        stub_save_for_success
        expect(stub_service(:flash_message_service)).to receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-uuid")

        delete :destroy, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :config_md5 => "1234abcd", :stage_parent => "pipelines"}

        jobs = @pipeline.get(0).getJobs()
        expect(jobs.size()).to eq(2)
        expect(jobs.get(0).name()).to eq(CaseInsensitiveString.new("job-2"))
        expect(jobs.get(1).name()).to eq(CaseInsensitiveString.new("job-3"))
        assert_update_command ::ConfigUpdate::JobsNode, ::ConfigUpdate::JobsJobSubject, ::ConfigUpdate::RefsAsUpdatedRefs
        expect(response).to redirect_to(admin_job_listing_url(:pipeline_name => "pipeline-name", :stage_name => "stage-name", :stage_parent => "pipelines", :fm => "random-uuid"))
      end

      it "should load jobs back when delete fails" do
        stub_save_for_validation_error do |result, *_|
          result.conflict('some message')
        end

        delete :destroy, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :config_md5 => "1234abcd", :stage_parent => "pipelines"}

        jobs = @pipeline.get(0).getJobs()
        expect(jobs.size()).to eq(2)
        expect(jobs.get(0).name()).to eq(CaseInsensitiveString.new("job-2"))
        expect(jobs.get(1).name()).to eq(CaseInsensitiveString.new("job-3"))
        assert_template layout: "pipelines/stage"
      end
    end

    describe "edit" do
      before(:each) do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should load job" do
        add_resource("job-2", "anything")
        add_resource("job-2", "windows-xp ")
        add_resource("job-2", "solaris")
        add_resource("job-2", "fluff")
        add_resource("job-2", "Foo")

        get :edit, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "settings",:config_md5 => "1234abcd", :stage_parent => "pipelines"}

        expect(assigns[:jobs]).not_to eq(nil)
        expect(assigns[:job]).to eq(@pipeline.get(0).getJobs().get(0))
        expect(assigns[:autocomplete_resources]).to eq(["Foo","anything","fluff","solaris","windows-xp"].to_json)
        assert_template layout: "pipelines/job"
      end

        it "should render error page if job does not exist" do
          get :edit, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "does_not_exist", :current_tab => "settings",:config_md5 => "1234abcd", :stage_parent => "pipelines"}
          assert_template "shared/config_error.html"
          assert_template layout: "layouts/application"
        end
    end

    describe "update" do
      before :each do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
      end

      it "should update job name and redirect to the new job" do
        stub_save_for_success

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1",
                    :current_tab => "settings",:config_md5 => "1234abcd", "job"=>{"name" => "renamed_job"}, :stage_parent => "pipelines"}

        expect(response.location).to match(/admin\/pipelines\/pipeline-name\/stages\/stage-name\/job\/renamed_job\/settings?.*?fm=(.+)/)
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end

      it "should not redirect when update fails" do
        stub_save_for_validation_error do |result, *_|
          result.forbidden('some message', HealthStateType.forbiddenForPipeline("pipeline-name"))
        end
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1",
            :current_tab => "settings", :config_md5 => "1234abcd", "job"=>{"name" => "doesnt_matter"}, :stage_parent => "pipelines"}

        expect(response.location).to be_nil
        assert_template "settings"
        assert_template layout: false
        expect(response.status).to eq(403)
      end

      it "should load resources for autocomplete even when update fails" do
        stub_save_for_validation_error do |result, *_|
          result.forbidden('some message', HealthStateType.forbiddenForPipeline("pipeline-name"))
        end
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        add_resource("job-2","anything")

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1",
                    :current_tab => "settings",:config_md5 => "1234abcd", "job"=>{"name" => "doesnt_matter"}, :stage_parent => "pipelines"}

        expect(response.location).to be_nil
        expect(assigns[:autocomplete_resources]).to eq(["anything"].to_json)
      end

      it "should not load new config on save failure (validation / merge conflict)" do
        stub_save_for_validation_error do |result, *_|
          result.forbidden('some message', HealthStateType.forbiddenForPipeline("pipeline-name"))
        end
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        add_resource("job-2","anything")

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1",
                    :current_tab => "settings",:config_md5 => "1234abcd", "job"=>{"name" => "doesnt_matter"}, :stage_parent => "pipelines"}

        expect(controller.instance_variable_get("@cruise_config").getMd5()).to eq("1234abcd")
        expect(@go_config_service).not_to receive(:loadForEdit)
      end

      it "should update environment variables" do
        stub_save_for_success

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "settings",:config_md5 => "1234abcd", "job"=>{"variables"=>[{:name=>"key", :valueForDisplay=>"value"}]}, :stage_parent => "pipelines"}

        variable = assigns[:job].variables().get(0)
        expect(variable.name).to eq("key")
        expect(variable.value).to eq("value")
        expect(variable.valueForDisplay).to eq("value")
        expect(response.location).to match(/admin\/pipelines\/pipeline-name\/stages\/stage-name\/job\/job-1\/settings?.*?fm=(.+)/)
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end

     it "should update custom tabs" do
        stub_save_for_success

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "tabs",:config_md5 => "1234abcd", "job"=>{"tabs"=>[{"name"=>"tab1", "path"=>"path1"}]}, :stage_parent => "pipelines"}

        expect(assigns[:job].getTabs().get(0).name).to eq("tab1")
        expect(assigns[:job].getTabs().get(0).path).to eq("path1")
        expect(response.location).to match(/admin\/pipelines\/pipeline-name\/stages\/stage-name\/job\/job-1\/tabs?.*?fm=(.+)/)
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
     end

      it "should clear environment variables" do
        stub_save_for_success

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "settings",:config_md5 => "1234abcd", "default_as_empty_list" => ["job>variables"], :stage_parent => "pipelines"}

        expect(assigns[:job].variables().isEmpty).to eq(true)
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end

      it "should update resources" do
        stub_save_for_success

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "resources",:config_md5 => "1234abcd", "job"=> {"resources" => "a,  b  ,c,d"}, :stage_parent => "pipelines"}

        expect(assigns[:job].resourceConfigs().exportToCsv()).to eq("a, b, c, d, ")
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end

      it "should populate an empty artifactConfigs when params is nil" do
        stub_save_for_success

        put :update, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "artifacts",:config_md5 => "1234abcd", "default_as_empty_list" => ["job>artifactConfigs"], :stage_parent => "pipelines"}

        expect(assigns[:job].artifactConfigs().size()).to eq(0)
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
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

      it "should be able to create a job with a pluggable task" do
        allow(@pluggable_task_service).to receive(:validate)
        @new_task = PluggableTask.new(PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("Url", false, nil)].to_java(ConfigurationProperty)))
        expect(@task_view_service).to receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
        stub_save_for_success

        pipeline_name = "pipeline-name"
        job = {:name => "new_job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:foo => "bar"}}}
        post :create, params:{:pipeline_name => pipeline_name, :stage_name => "stage-name", :job => job,  :config_md5 => "1234abcd", :stage_parent => "pipelines"}

        expect(@cruise_config.getAllErrors().size).to eq(0)
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
        pipeline_config = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new(pipeline_name))
        pipeline_config.get(0).getJobs().last().name == JobConfig.new("new_job")
        expect(pipeline_config.get(0).getJobs().last().getTasks().first().instance_of?(PluggableTask)).to eq(true)
      end

      it "should validate pluggable tasks before create" do
        allow(@pluggable_task_service).to receive(:validate) do |task|
          task.getConfiguration().getProperty("key").addError("key", "some error")
        end
        @new_task = PluggableTask.new( PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("key", false, nil)].to_java(ConfigurationProperty)))
        expect(@task_view_service).to receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        stub_save_for_validation_error do |result, cruise_config, pipeline|
          result.badRequest("Save failed, see errors below")
        end
        expect(@task_view_service).to receive(:getTaskViewModelsWith).with(anything).and_return(Object.new)

        job = {:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:key => "value"}}}
        post :create, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job => job,  :config_md5 => "1234abcd", :stage_parent => "pipelines"}

        task_to_be_saved = assigns[:job].getTasks().first()
        expect(task_to_be_saved.instance_of?(PluggableTask)).to eq(true)
        expect(task_to_be_saved.getConfiguration().getProperty("key").errors().getAll().size()).to be > 0
        expect(task_to_be_saved.getConfiguration().getProperty("key").errors().getAllOn("key").get(0)).to eq("some error")
        assert_template "new"
        assert_template layout: false
        expect(response.status).to eq(400)
      end

      it "should create a new job" do
        stub_save_for_success

        post :create, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job => { :name => "new_job" },  :config_md5 => "1234abcd", :stage_parent => "pipelines"}

        expect(assigns[:node].last).to eq(JobConfig.new("new_job"))
        expect(assigns[:job]).to eq(JobConfig.new("new_job"))
        assert_update_command ::ConfigUpdate::JobsNode, ::ConfigUpdate::RefsAsUpdatedRefs
      end

      it "should show error message when config save fails for reasons other than validations" do
        execTask = ExecTask.new('ls', '', 'work')
        expect(@task_view_service).to receive(:taskInstanceFor).and_return(ExecTask.new)
        expect(@task_view_service).to receive(:getTaskViewModelsWith).with(execTask).and_return(@tvms = [TaskViewModel.new(AntTask.new(), "new"), TaskViewModel.new(execTask, "new")].to_java(TaskViewModel))
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        stub_save_for_validation_error do |result, *_|
          result.forbidden('some message', HealthStateType.forbiddenForPipeline("pipeline-name"))
        end

        post :create, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job => {:name => "new_job", :tasks => {:taskOptions => "exec", "exec" => {:command => "ls", :workingDirectory => 'work'}}},  :config_md5 => "1234abcd", :stage_parent => "pipelines"}

        assert_template "new"
        assert_template layout: false
        expect(response.status).to eq(403)
      end

      it "should load jobs page with resource when creation fails" do
        execTask = ExecTask.new('ls', '', 'work')
        expect(@task_view_service).to receive(:taskInstanceFor).and_return(ExecTask.new)
        expect(@task_view_service).to receive(:getTaskViewModelsWith).with(execTask).and_return(@tvms = [TaskViewModel.new(AntTask.new(), "new"), TaskViewModel.new(execTask, "new")].to_java(TaskViewModel))
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)

        add_resource("job-2", "anything")

        stub_save_for_validation_error do |result, *_|
          result.badRequest("Save failed, see errors below")
        end

        post :create, params:{:pipeline_name => "pipeline-name", :stage_name => "stage-name", :job => {:name => "new_job", :tasks => {:taskOptions => "exec", "exec" => {:command => "ls", :workingDirectory => 'work'}}},  :config_md5 => "1234abcd", :stage_parent => "pipelines"}

        expect(assigns[:autocomplete_resources]).to eq(["anything"].to_json)
        assert_template "new"
        assert_template layout: false
        expect(response.status).to eq(400)
      end
    end
  end
end
