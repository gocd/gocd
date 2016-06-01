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


def vm_template_for task
  return vm_for task unless (task.instance_of? PluggableTask)
  PluggableTaskViewModelFactory.new().viewModelFor(task, "edit")
end

shared_examples_for :task_controller  do
  include ConfigSaveStubbing, TaskMother

  describe "routes" do
    describe "index" do
      it "should resolve templates as :stage_parent" do
        {:get => "/admin/templates/dev.foo/stages/test.bar/job/job-1.baz/tasks"}.should route_to(:controller => "admin/tasks", :action => "index", :stage_parent=>"templates", :pipeline_name=>"dev.foo", :stage_name=>"test.bar", :job_name=>"job-1.baz", :current_tab=>"tasks")
      end

      it "should resolve" do
        {:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks"}.should route_to(:controller => "admin/tasks", :action => "index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :stage_parent => "pipelines", :current_tab=>"tasks")
      end

      it "should generate" do
        admin_tasks_listing_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :stage_parent => "pipelines", :current_tab=>"tasks").should == "/admin/pipelines/foo.bar/stages/baz/job/quux/tasks"
      end
    end

    describe "increment_index" do
      it "should resolve" do
        {:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/task/1/index/increment"}.should route_to(:controller => "admin/tasks", :action => "increment_index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab=>"tasks")
      end

      it "should generate" do
        admin_task_increment_index_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => "1", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz/job/quux/task/1/index/increment"
      end
    end

    describe "decrement_index" do
      it "should resolve" do
        {:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/task/1/index/decrement"}.should route_to(:controller => "admin/tasks", :action => "decrement_index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab=>"tasks")
      end

      it "should generate" do
        admin_task_decrement_index_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => "1", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz/job/quux/task/1/index/decrement"
      end
    end

    describe "edit" do
      it "should resolve" do
        {:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/1/edit"}.should route_to(:controller => "admin/tasks", :action => "edit", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab=>"tasks")
      end

      it "should generate" do
        admin_task_edit_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 2, :type=> "#{@task_type}", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/2/edit"
      end

      it "should only accept numerical task_index(s)" do
        {:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello/edit"}.should route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello/edit")
        {:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/100abc200/edit"}.should route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/100abc200/edit")
      end
    end

    describe "delete" do
      it "should resolve" do
        {:delete => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/1"}.should route_to(:controller => "admin/tasks", :action => "destroy", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab=>"tasks")
      end

      it "should generate" do
        admin_task_delete_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 2, :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/2"
      end

      it "should only accept numerical task_index(s)" do
        {:delete => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello"}.should route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello")
      end
    end

    describe "update" do
      it "should resolve" do
        {:put => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/1"}.should route_to(:controller => "admin/tasks", :action => "update", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :type=> "#{@task_type}", :stage_parent => "pipelines", :current_tab=>"tasks")
      end

      it "should generate" do
        admin_task_update_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 1, :type => "#{@task_type}", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/1"
      end

    end

    describe "new" do
      it "should resolve" do
        {:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/new"}.should route_to(:controller => "admin/tasks", :action => "new", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :type  => "#{@task_type}", :stage_parent => "pipelines", :current_tab=>"tasks")
      end

      it "should generate" do
        admin_task_new_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :type => "#{@task_type}", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/new"
      end

    end

    describe "create" do
      it "should resolve" do
        {:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}"}.should route_to(:controller => "admin/tasks", :action => "create", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :type  => "#{@task_type}", :stage_parent => "pipelines", :current_tab=>"tasks")
      end

      it "should generate" do
        admin_task_create_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :type => "#{@task_type}", :stage_parent => "pipelines").should == "/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}"
      end

    end
  end

  describe "action implementation" do
    before(:each) do
      controller.stub(:populate_config_validity)
      @pipeline = PipelineConfigMother.createPipelineConfig("pipeline.name", "stage.name", ["job.1", "job.2", "job.3"].to_java(java.lang.String))
      @tasks = @pipeline.get(0).getJobs().get(0).getTasks()
      @tasks.add(@example_task)

      @go_config_service = stub_service(:go_config_service)
      @pipeline_pause_service = stub_service(:pipeline_pause_service)

      @user = current_user
      @result = stub_localized_result

      @template = PipelineTemplateConfig.new(CaseInsensitiveString.new("template.name"), [StageConfigMother.stageWithTasks("stage_one"), StageConfigMother.stageWithTasks("stage_two")].to_java(StageConfig))

      @cruise_config = BasicCruiseConfig.new()
      @cruise_config.addPipeline("my-groups", @pipeline)
      @cruise_config.addTemplate(@template)
      set(@cruise_config, "md5", "abcd1234")

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      @pluggable_task_service = stub_service(:pluggable_task_service)
      @config_store = double('config store')
      @controller.stub(:config_store).and_return(@config_store)
    end

    describe "destroy" do
      it "should delete a given task from a job" do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        stub_save_for_success
        delete :destroy, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "abcd1234", :stage_parent => 'pipelines', :current_tab=>"tasks"
        tasks = @pipeline.get(0).getJobs().get(0).getTasks()
        tasks.size().should == 0
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode, ::ConfigUpdate::JobTaskSubject
      end

      it "should delete a given task from a job in template" do
        stub_save_for_success
        delete :destroy, :pipeline_name => "template.name", :stage_name => "stage_one", :job_name => "job", :task_index => "1", :config_md5 => "abcd1234", :stage_parent => 'templates', :current_tab=>"tasks"
        tasks = @template.get(0).getJobs().get(0).getTasks()
        tasks.size().should == 1
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode, ::ConfigUpdate::JobTaskSubject
      end
    end

    describe "edit" do

      before do
        @task_view_service = stub_service(:task_view_service)
        @go_config_service.should_receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        @on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
      end

      it "should handle for a task" do
        @task_view_service.should_receive(:getOnCancelTaskViewModels).with(@example_task).and_return(@on_cancel_task_vms)
        @task_view_service.should_receive(:getViewModel).with(@example_task, "edit").and_return(vm_template_for(@example_task))

        get :edit, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "abcd1234", :type => @task_type, :stage_parent => "pipelines", :current_tab=>"tasks"

        assigns[:task].should == @example_task
        assigns[:on_cancel_task_vms].should == @on_cancel_task_vms
        response.status.should == 200
        assigns[:config_store].should == @config_store
        assert_template "admin/tasks/plugin/edit"
        assert_template layout: false
      end
    end

    describe "update" do
      before :each do
        @go_config_service.should_receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        @pluggable_task_service.should_receive(:validate) if(@new_task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask)
      end

      it "should update a given task" do
        stub_save_for_success

        put :update, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "abcd1234", :type => @task_type, :task => @updated_payload, :stage_parent => "pipelines", :current_tab=>"tasks"
        assigns[:task].should == @updated_task
        response.status.should == 200

        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::TaskNode, ::ConfigUpdate::NodeAsSubject
      end

      it "should update a given task with on cancel" do
        stub_save_for_success

        updated_payload_with_on_cancel = {:hasCancelTask => '1',
                                              :onCancelConfig => {:onCancelOption => 'ant', :antOnCancel => {:buildFile => "cancelFile", :target => "cancelTarget", :workingDirectory => "anotherWD"}}}
        @updated_payload.merge!(updated_payload_with_on_cancel)
        @updated_task.setCancelTask(ant_task("cancelFile","cancelTarget","anotherWD"))

        put :update, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "abcd1234", :type => @task_type, :task => @updated_payload, :stage_parent => "pipelines", :current_tab=>"tasks"

        assigns[:task].should == @updated_task
        response.status.should == 200

        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::TaskNode, ::ConfigUpdate::NodeAsSubject
      end

      it "should assign config_errors for display when update fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          @cruise_config.errors().add("base", "someError")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end
        task_view_service = stub_service(:task_view_service)
        task_view_service.should_receive(:getViewModel).with(@updated_task, 'edit').and_return(vm_template_for(@updated_task))
        controller_specific_setup task_view_service
        on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
        task_view_service.should_receive(:getOnCancelTaskViewModels).with(@updated_task).and_return(on_cancel_task_vms)

        put :update, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "1234abcd", :type => @task_type, :task => @updated_payload, :stage_parent => "pipelines", :current_tab=>"tasks"

        assigns[:errors].size.should == 1
        assigns[:on_cancel_task_vms].should == on_cancel_task_vms
        assert_save_arguments
        assert_template "admin/tasks/plugin/edit"
        assert_template layout: false
        response.status.should == 400
      end

    end

    describe "new" do
      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
        @task_view_service = stub_service(:task_view_service)
        @on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
      end

      it "should create a new task" do
        @task_view_service.should_receive(:getOnCancelTaskViewModels).with(@new_task).and_return(@on_cancel_task_vms)
        @task_view_service.should_receive(:taskInstanceFor).with(an_instance_of(String)).and_return(@new_task)
        @task_view_service.should_receive(:getViewModel).with(@new_task, "new").and_return(vm_template_for(@new_task))

        get :new, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :config_md5 => "abcd1234", :type => @task_type, :stage_parent => "pipelines", :current_tab=>"tasks"
        assigns[:task].should == @new_task
        assigns[:on_cancel_task_vms].should == @on_cancel_task_vms
        response.status.should == 200
        assigns[:config_store].should == @config_store
      end
    end

    describe "create" do
      before do
        @go_config_service.should_receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
        @task_view_service = stub_service(:task_view_service)
        @task_view_service.should_receive(:taskInstanceFor).with(@task_type).and_return(@new_task)
        controller_specific_setup(@task_view_service)
        @pluggable_task_service.should_receive(:validate) if(@new_task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask)
      end

      it "should create a task" do
        stub_save_for_success

        post :create, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :type => @task_type, :config_md5 => "abcd1234", :task => @create_payload, :stage_parent => "pipelines", :current_tab=>"tasks"

        @tasks.length.should == 2
        @tasks.last.should == @created_task
        assigns[:task].should == @created_task
        assigns[:config_store].should == nil

        response.status.should == 200
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode
      end

      it "should assign config_errors for display when update fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          @cruise_config.errors().add("base", "someError")
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end
        @task_view_service.should_receive(:getViewModel).with(@created_task, 'new').and_return(vm_template_for(@created_task))
        @on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
        @task_view_service.should_receive(:getOnCancelTaskViewModels).with(@created_task).and_return(@on_cancel_task_vms)

        post :create, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :type => @task_type, :config_md5 => "1234abcd", :task => @create_payload, :stage_parent => "pipelines", :current_tab=>"tasks"

        assigns[:errors].size.should == 1
        assigns[:on_cancel_task_vms].should == @on_cancel_task_vms
        assert_save_arguments
        assigns[:config_store].should_not == nil
        assigns[:config_store].should == @config_store
        assert_template "admin/tasks/plugin/new"
        assert_template layout: false
        response.status.should == 400
      end
    end
  end
end
