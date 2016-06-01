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

describe Admin::JobsController do
  include ConfigSaveStubbing
  include MockRegistryModule
  include TaskMother

  def add_resource(job_name, resource)
    @pipeline.getFirstStageConfig().getJobs().getJob(CaseInsensitiveString.new(job_name)).addResource(resource)
  end

  describe "routes" do
    it "should resolve new" do
      {:get => "/admin/pipelines/dev/stages/test.1/jobs/new"}.should route_to(:controller => "admin/jobs", :action => "new", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
      {:get => "/admin/templates/dev/stages/test.1/jobs/new"}.should route_to(:controller => "admin/jobs", :action => "new", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "templates")
      admin_job_new_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/test.1/jobs/new"
      admin_job_new_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "templates").should == "/admin/templates/foo.bar/stages/test.1/jobs/new"
    end

    it "should resolve create" do
      {:post => "/admin/pipelines/dev/stages/test.1/jobs"}.should route_to(:controller => "admin/jobs", :action => "create", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
      admin_job_create_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/test.1/jobs"
    end

    it "should resolve destroy" do
      {:delete => "/admin/pipelines/dev/stages/test.1/job/job.1"}.should route_to(:controller => "admin/jobs", :action => "destroy", :pipeline_name => "dev", :stage_name => "test.1", :job_name=> "job.1", :stage_parent => "pipelines")
    end

    it "should generate index" do
      {:get => "/admin/pipelines/dev/stages/test.1/jobs"}.should route_to(:controller => "admin/jobs", :action => "index", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
      admin_job_listing_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/test.1/jobs"
    end

    it "should generate destroy" do
      # Cannot have route_for for DELETE as route_for does not honor the :method => :delete attribute
      admin_job_delete_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :job_name => "job.1", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/test.1/job/job.1"
    end

    it "should generate route for tabs" do
      {:get => "/admin/pipelines/dev/stages/test.1/job/job.1/tabs"}.should route_to(:controller => "admin/jobs", :action => "edit", :pipeline_name => "dev", :stage_name => "test.1", :job_name=> "job.1", :stage_parent => "pipelines", :current_tab => "tabs")
      admin_job_edit_path(:pipeline_name => "foo.bar", :stage_name => "foo.bar", :job_name => "foo.bar", :current_tab => "tabs", :stage_parent => "templates").should == "/admin/templates/foo.bar/stages/foo.bar/job/foo.bar/tabs"
      admin_job_edit_path(:pipeline_name => "foo.bar", :stage_name => "foo.bar", :job_name => "foo.bar", :current_tab => "tabs", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/foo.bar/job/foo.bar/tabs"
    end
  end

  describe "action" do
    before(:each) do
      controller.stub(:populate_config_validity)

      @cruise_config = BasicCruiseConfig.new()
      @pipeline = PipelineConfigMother.createPipelineConfig("pipeline-name", "stage-name", ["job-1", "job-2", "job-3"].to_java(java.lang.String))
      @cruise_config.addPipeline("defaultGroup", @pipeline)

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @user = Username.new(CaseInsensitiveString.new("loser"))
      controller.stub(:current_user).and_return(@user)
      @result = HttpLocalizedOperationResult.new
      @load_result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(@result, @load_result)

      @go_config_service = double('Go Config Service')
      controller.stub(:go_config_service).and_return(@go_config_service)

      @pipeline_pause_service = double('Pipeline Pause Service')
      controller.stub(:pipeline_pause_service).and_return(@pipeline_pause_service)

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end


    describe "index" do

      it "should assign jobs" do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)

        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)

        get :index, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :stage_parent => "pipelines"

        assigns[:pipeline].should == @pipeline
        assigns[:stage].should == @pipeline.get(0)
        assigns[:jobs].should_not == nil
        assigns[:jobs].size.should == 3
        assigns[:jobs].get(0).name().should == CaseInsensitiveString.new("job-1")
        assigns[:jobs].get(1).name().should == CaseInsensitiveString.new("job-2")
        assigns[:jobs].get(2).name().should == CaseInsensitiveString.new("job-3")
        assert_template layout: "pipelines/stage"
      end
    end

    describe "new" do
      it "should render a new job" do
        controller.should_receive(:task_view_service).and_return(task_view_service = double("task_view_service"))
        task_view_service.should_receive(:getTaskViewModels).and_return(tvms = [TaskViewModel.new(AntTask.new(), "new", "erb"), TaskViewModel.new(NantTask.new(), "new", "erb")].to_java(TaskViewModel))
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
        add_resource("job-1", "windows")
        add_resource("job-1", "linux")
        add_resource("job-2", "windows-xp")
        add_resource("job-2", "solaris")
        @go_config_service.stub(:registry)

        get :new, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :stage_parent => "pipelines"

        assigns[:pipeline].should == @pipeline
        assigns[:stage].should == @pipeline.get(0)
        assigns[:autocomplete_resources].should == ['linux','solaris','windows','windows-xp'].to_json
        assigns[:task_view_models].should == tvms

        actual_job_assigned = assigns[:job]
        job_config_new = JobConfig.new(CaseInsensitiveString.new(""), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))
        actual_job_assigned.should == job_config_new
        actual_job_assigned.tasks().first.should == AntTask.new
        assert_template layout: false
      end
    end

    describe "delete" do
      before do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
      end

      it "should delete the given job" do
        stub_save_for_success
        stub_service(:flash_message_service).should_receive(:add).with(FlashMessageModel.new("Saved successfully.", "success")).and_return("random-uuid")

        delete :destroy, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :config_md5 => "1234abcd", :stage_parent => "pipelines"

        jobs = @pipeline.get(0).getJobs()
        jobs.size().should == 2
        jobs.get(0).name().should == CaseInsensitiveString.new("job-2")
        jobs.get(1).name().should == CaseInsensitiveString.new("job-3")
        assert_update_command ::ConfigUpdate::JobsNode, ::ConfigUpdate::JobsJobSubject, ::ConfigUpdate::RefsAsUpdatedRefs
        response.should redirect_to(admin_job_listing_url(:pipeline_name => "pipeline-name", :stage_name => "stage-name", :stage_parent => "pipelines", :fm => "random-uuid"))
      end

      it "should load jobs back when delete fails" do
        stub_save_for_validation_error do |result, *_|
          result.conflict(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end

        delete :destroy, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :config_md5 => "1234abcd", :stage_parent => "pipelines"

        jobs = @pipeline.get(0).getJobs()
        jobs.size().should == 2
        jobs.get(0).name().should == CaseInsensitiveString.new("job-2")
        jobs.get(1).name().should == CaseInsensitiveString.new("job-3")
        assert_template layout: "pipelines/stage"
      end
    end

    describe "edit" do
      before(:each) do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should load job" do
        add_resource("job-2", "anything")
        add_resource("job-2", "windows-xp ")
        add_resource("job-2", "solaris")
        add_resource("job-2", "fluff")
        add_resource("job-2", "Foo")

        get :edit, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "settings",:config_md5 => "1234abcd", :stage_parent => "pipelines"

        assigns[:jobs].should_not == nil
        assigns[:job].should == @pipeline.get(0).getJobs().get(0)
        assigns[:autocomplete_resources].should == ["Foo","anything","fluff","solaris","windows-xp"].to_json
        assert_template layout: "pipelines/job"
      end

        it "should render error page if job does not exist" do
          get :edit, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "does_not_exist", :current_tab => "settings",:config_md5 => "1234abcd", :stage_parent => "pipelines"
          assert_template "shared/config_error.html"
          assert_template layout: "layouts/application"
        end
    end

    describe "update" do
      before :each do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
      end

      it "should update job name and redirect to the new job" do
        stub_save_for_success

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1",
                    :current_tab => "settings",:config_md5 => "1234abcd", "job"=>{"name" => "renamed_job"}, :stage_parent => "pipelines"

        response.location.should =~ /admin\/pipelines\/pipeline-name\/stages\/stage-name\/job\/renamed_job\/settings?.*?fm=(.+)/
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end

      it "should not redirect when update fails" do
        stub_save_for_validation_error do |result, *_|
          result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]), HealthStateType.unauthorisedForPipeline("pipeline-name"))
        end
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1",
            :current_tab => "settings", :config_md5 => "1234abcd", "job"=>{"name" => "doesnt_matter"}, :stage_parent => "pipelines"

        response.location.should be_nil
        assert_template "settings"
        assert_template layout: false
        response.status.should == 401
      end

      it "should load resources for autocomplete even when update fails" do
        stub_save_for_validation_error do |result, *_|
          result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]), HealthStateType.unauthorisedForPipeline("pipeline-name"))
        end
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        add_resource("job-2","anything")

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1",
                    :current_tab => "settings",:config_md5 => "1234abcd", "job"=>{"name" => "doesnt_matter"}, :stage_parent => "pipelines"

        response.location.should be_nil
        assigns[:autocomplete_resources].should == ["anything"].to_json
      end

      it "should not load new config on save failure (validation / merge conflict)" do
        stub_save_for_validation_error do |result, *_|
          result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]), HealthStateType.unauthorisedForPipeline("pipeline-name"))
        end
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        add_resource("job-2","anything")

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1",
                    :current_tab => "settings",:config_md5 => "1234abcd", "job"=>{"name" => "doesnt_matter"}, :stage_parent => "pipelines"

        controller.instance_variable_get("@cruise_config").getMd5().should == "1234abcd"
        @go_config_service.should_not_receive(:loadForEdit)
      end

      it "should update environment variables" do
        stub_save_for_success

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "settings",:config_md5 => "1234abcd", "job"=>{"variables"=>[{:name=>"key", :valueForDisplay=>"value"}]}, :stage_parent => "pipelines"

        variable = assigns[:job].variables().get(0)
        variable.name.should == "key"
        variable.value.should == "value"
        variable.valueForDisplay.should == "value"
        response.location.should =~ /admin\/pipelines\/pipeline-name\/stages\/stage-name\/job\/job-1\/settings?.*?fm=(.+)/
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end

     it "should update custom tabs" do
        stub_save_for_success

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "tabs",:config_md5 => "1234abcd", "job"=>{"tabs"=>[{"name"=>"tab1", "path"=>"path1"}]}, :stage_parent => "pipelines"

        assigns[:job].getTabs().get(0).name.should == "tab1"
        assigns[:job].getTabs().get(0).path.should == "path1"
        response.location.should =~ /admin\/pipelines\/pipeline-name\/stages\/stage-name\/job\/job-1\/tabs?.*?fm=(.+)/
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
     end

      it "should clear environment variables" do
        stub_save_for_success

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "settings",:config_md5 => "1234abcd", "default_as_empty_list" => ["job>variables"], :stage_parent => "pipelines"

        assigns[:job].variables().isEmpty.should == true
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end

      it "should update resources" do
        stub_save_for_success

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "resources",:config_md5 => "1234abcd", "job"=> {"resources" => "a,  b  ,c,d"}, :stage_parent => "pipelines"

        assigns[:job].resources().exportToCsv().should == "a, b, c, d, "
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
      end

      it "should populate an empty artifactPlans when params is nil" do
        stub_save_for_success

        put :update, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job_name => "job-1", :current_tab => "artifacts",:config_md5 => "1234abcd", "default_as_empty_list" => ["job>artifactPlans"], :stage_parent => "pipelines"

        assigns[:job].artifactPlans().size().should == 0
        assert_update_command ::ConfigUpdate::JobNode, ::ConfigUpdate::NodeAsSubject, ::ConfigUpdate::RefsAsUpdatedRefs
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
        @pluggable_task_service = double('Pluggable_task_service')
        controller.stub(:pluggable_task_service).and_return(@pluggable_task_service)
      end

      it "should be able to create a job with a pluggable task" do
        @pluggable_task_service.stub(:validate)
        task_view_service = double('Task View Service')
        controller.stub(:task_view_service).and_return(task_view_service)
        @new_task = PluggableTask.new(PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("Url", false, nil)].to_java(ConfigurationProperty)))
        task_view_service.should_receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
        stub_save_for_success

        pipeline_name = "pipeline-name"
        job = {:name => "new_job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:foo => "bar"}}}
        post :create, :pipeline_name => pipeline_name, :stage_name => "stage-name", :job => job,  :config_md5 => "1234abcd", :stage_parent => "pipelines"

        @cruise_config.getAllErrors().size.should == 0
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAction, ::ConfigUpdate::RefsAsUpdatedRefs
        pipeline_config = @cruise_config.getPipelineConfigByName(CaseInsensitiveString.new(pipeline_name))
        pipeline_config.get(0).getJobs().last().name == JobConfig.new("new_job")
        pipeline_config.get(0).getJobs().last().getTasks().first().instance_of?(PluggableTask).should == true
      end

      it "should validate pluggable tasks before create" do
        task_view_service = double('Task View Service')
        controller.stub(:task_view_service).and_return(task_view_service)
        @pluggable_task_service.stub(:validate) do |task|
          task.getConfiguration().getProperty("key").addError("key", "some error")
        end
        @new_task = PluggableTask.new( PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("key", false, nil)].to_java(ConfigurationProperty)))
        task_view_service.should_receive(:taskInstanceFor).with("pluggableTask").and_return(@new_task)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        stub_save_for_validation_error do |result, cruise_config, pipeline|
          result.badRequest(LocalizedMessage.string("SAVE_FAILED"))
        end
        task_view_service.should_receive(:getTaskViewModelsWith).with(anything).and_return(Object.new)

        job = {:name => "job", :tasks => {:taskOptions => "pluggableTask", "pluggableTask" => {:key => "value"}}}
        post :create, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job => job,  :config_md5 => "1234abcd", :stage_parent => "pipelines"

        task_to_be_saved = assigns[:job].getTasks().first()
        task_to_be_saved.instance_of?(PluggableTask).should == true
        task_to_be_saved.getConfiguration().getProperty("key").errors().getAll().size().should > 0
        task_to_be_saved.getConfiguration().getProperty("key").errors().getAllOn("key").get(0).should == "some error"
        assert_template "new"
        assert_template layout: false
        response.status.should == 400
      end

      it "should create a new job" do
        stub_save_for_success

        post :create, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job => { :name => "new_job" },  :config_md5 => "1234abcd", :stage_parent => "pipelines"

        assigns[:node].last.should == JobConfig.new("new_job")
        assigns[:job].should == JobConfig.new("new_job")
        assert_update_command ::ConfigUpdate::JobsNode, ::ConfigUpdate::RefsAsUpdatedRefs
      end

      it "should show error message when config save fails for reasons other than validations" do
        execTask = ExecTask.new('ls', '', 'work')
        controller.should_receive(:task_view_service).twice.and_return(task_view_service = double("task_view_service"))
        task_view_service.should_receive(:taskInstanceFor).and_return(ExecTask.new)
        task_view_service.should_receive(:getTaskViewModelsWith).with(execTask).and_return(@tvms = [TaskViewModel.new(AntTask.new(), "new", "erb"), TaskViewModel.new(execTask, "new", "erb")].to_java(TaskViewModel))
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        stub_save_for_validation_error do |result, *_|
          result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]), HealthStateType.unauthorisedForPipeline("pipeline-name"))
        end

        post :create, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job => {:name => "new_job", :tasks => {:taskOptions => "exec", "exec" => {:command => "ls", :workingDirectory => 'work'}}},  :config_md5 => "1234abcd", :stage_parent => "pipelines"

        assert_template "new"
        assert_template layout: false
        response.status.should == 401
      end

      it "should load jobs page with resource when creation fails" do
        execTask = ExecTask.new('ls', '', 'work')
        controller.should_receive(:task_view_service).twice.and_return(task_view_service = double("task_view_service"))
        task_view_service.should_receive(:taskInstanceFor).and_return(ExecTask.new)
        task_view_service.should_receive(:getTaskViewModelsWith).with(execTask).and_return(@tvms = [TaskViewModel.new(AntTask.new(), "new", "erb"), TaskViewModel.new(execTask, "new", "erb")].to_java(TaskViewModel))
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)

        add_resource("job-2", "anything")

        stub_save_for_validation_error do |result, *_|
          result.badRequest(LocalizedMessage.string("SAVE_FAILED"))
        end

        post :create, :pipeline_name => "pipeline-name", :stage_name => "stage-name", :job => {:name => "new_job", :tasks => {:taskOptions => "exec", "exec" => {:command => "ls", :workingDirectory => 'work'}}},  :config_md5 => "1234abcd", :stage_parent => "pipelines"

        assigns[:autocomplete_resources].should == ["anything"].to_json
        assert_template "new"
        assert_template layout: false
        response.status.should == 400
      end
    end
  end
end
