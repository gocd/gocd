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

shared_examples_for :task_controller do
  include ConfigSaveStubbing
  include TaskMother
  include MiscSpecExtensions

  describe "routes" do
    describe "index" do
      it "should resolve templates as :stage_parent" do
        expect({:get => "/admin/templates/dev.foo/stages/test.bar/job/job-1.baz/tasks"}).to route_to(:controller => "admin/tasks", :action => "index", :stage_parent => "templates", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :current_tab => "tasks")
      end

      it "should resolve" do
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks"}).to route_to(:controller => "admin/tasks", :action => "index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_tasks_listing_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :stage_parent => "pipelines", :current_tab => "tasks")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks")
      end
    end

    describe "increment_index" do
      it "should resolve" do
        expect({:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/task/1/index/increment"}).to route_to(:controller => "admin/tasks", :action => "increment_index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_increment_index_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => "1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/task/1/index/increment")
      end
    end

    describe "decrement_index" do
      it "should resolve" do
        expect({:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/task/1/index/decrement"}).to route_to(:controller => "admin/tasks", :action => "decrement_index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_decrement_index_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => "1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/task/1/index/decrement")
      end
    end

    describe "edit" do
      it "should resolve" do
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/1/edit"}).to route_to(:controller => "admin/tasks", :action => "edit", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_edit_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 2, :type => "#{@task_type}", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/2/edit")
      end

      it "should only accept numerical task_index(s)" do
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello/edit"}).to route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello/edit")
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/100abc200/edit"}).to route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/100abc200/edit")
      end
    end

    describe "delete" do
      it "should resolve" do
        expect({:delete => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/1"}).to route_to(:controller => "admin/tasks", :action => "destroy", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_delete_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 2, :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/2")
      end

      it "should only accept numerical task_index(s)" do
        expect({:delete => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello"}).to route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello")
      end
    end

    describe "update" do
      it "should resolve" do
        expect({:put => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/1"}).to route_to(:controller => "admin/tasks", :action => "update", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_update_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 1, :type => "#{@task_type}", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/1")
      end

    end

    describe "new" do
      it "should resolve" do
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/new"}).to route_to(:controller => "admin/tasks", :action => "new", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_new_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :type => "#{@task_type}", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/new")
      end

    end

    describe "create" do
      it "should resolve" do
        expect({:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}"}).to route_to(:controller => "admin/tasks", :action => "create", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_create_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :type => "#{@task_type}", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}")
      end

    end
  end
  describe "action implementation" do
    before(:each) do
      allow(controller).to receive(:populate_config_validity)
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
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      allow(@go_config_service).to receive(:artifactIdToPluginIdForFetchPluggableArtifact).and_return({})
      allow(@go_config_service).to receive(:pipelineConfigNamed).with(an_instance_of(CaseInsensitiveString)).and_return(@pipeline)
      allow(@go_config_service).to receive(:getCurrentConfig).and_return(@cruise_config)
      @pluggable_task_service = stub_service(:pluggable_task_service)
      @config_store = double('config store')
      allow(@controller).to receive(:config_store).and_return(@config_store)
    end

    describe "destroy" do
      it "should delete a given task from a job" do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        stub_save_for_success
        delete :destroy, params: {:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "abcd1234", :stage_parent => 'pipelines', :current_tab => "tasks"}
        tasks = @pipeline.get(0).getJobs().get(0).getTasks()
        expect(tasks.size()).to eq(0)
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode, ::ConfigUpdate::JobTaskSubject
      end

      it "should delete a given task from a job in template" do
        stub_save_for_success
        delete :destroy, params: {:pipeline_name => "template.name", :stage_name => "stage_one", :job_name => "job", :task_index => "1", :config_md5 => "abcd1234", :stage_parent => 'templates', :current_tab => "tasks"}
        tasks = @template.get(0).getJobs().get(0).getTasks()
        expect(tasks.size()).to eq(1)
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode, ::ConfigUpdate::JobTaskSubject
      end
    end

    describe "edit" do

      before do
        @task_view_service = stub_service(:task_view_service)
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
        expect(@go_config_service).to receive(:doesPipelineExist).and_return(true)
        expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        @on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
      end

      it "should handle for a task" do
        expect(@task_view_service).to receive(:getOnCancelTaskViewModels).with(@example_task).and_return(@on_cancel_task_vms)
        expect(@task_view_service).to receive(:getViewModel).with(@example_task, "edit").and_return(vm_template_for(@example_task))

        get :edit, params: {:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "abcd1234", :type => @task_type, :stage_parent => "pipelines", :current_tab => "tasks"}

        expect(assigns[:task]).to eq(@example_task)
        expect(assigns[:on_cancel_task_vms]).to eq(@on_cancel_task_vms)
        expect(response.status).to eq(200)
        expect(assigns[:config_store]).to eq(@config_store)
        assert_template "admin/tasks/plugin/edit"
        assert_template layout: false
      end
    end

    describe "update" do
      before :each do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
        expect(@go_config_service).to receive(:doesPipelineExist).and_return(true)
        expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        expect(@pluggable_task_service).to receive(:validate) if (@new_task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask)
      end

      it "should update a given task" do
        stub_config_save_with_subject(@subject)

        put :update, params: {:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "abcd1234", :type => @task_type, :task => @updated_payload, :stage_parent => "pipelines", :current_tab => "tasks"}

        expect(assigns[:task]).to eq(expected_task(@updated_task))
        expect(response.status).to eq(200)

        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode, ::ConfigUpdate::JobTaskSubject
      end

      it "should update a given task with on cancel" do
        @subject.setCancelTask(ant_task("cancelFile", "cancelTarget", "anotherWD"))
        stub_config_save_with_subject(@subject)

        updated_payload_with_on_cancel = {:hasCancelTask => '1',
                                          :onCancelConfig => {:onCancelOption => 'ant', :antOnCancel => {:buildFile => "cancelFile", :target => "cancelTarget", :workingDirectory => "anotherWD"}}}
        @updated_payload.merge!(updated_payload_with_on_cancel)
        @updated_task.setCancelTask(ant_task("cancelFile", "cancelTarget", "anotherWD"))

        put :update, params: {:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "abcd1234", :type => @task_type, :task => @updated_payload, :stage_parent => "pipelines", :current_tab => "tasks"}

        expect(assigns[:task]).to eq(expected_task(@updated_task))
        expect(response.status).to eq(200)

        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode, ::ConfigUpdate::JobTaskSubject
      end

      it "should assign config_errors for display when update fails due to validation errors" do
        stub_save_for_validation_error_with_subject(@subject) do |result, config, node|
          result.badRequest('some message')
          config.errors().add("base", "someError")

        end
        task_view_service = stub_service(:task_view_service)
        expect(task_view_service).to receive(:getViewModel).with(@updated_task, 'edit').and_return(vm_template_for(@updated_task))
        controller_specific_setup task_view_service
        on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
        expect(task_view_service).to receive(:getOnCancelTaskViewModels).with(@updated_task).and_return(on_cancel_task_vms)
        allow(task_view_service).to receive(:taskInstanceFor).with(@updated_task.getTaskType).and_return(@updated_task)

        put :update, params: {:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "1234abcd", :type => @task_type, :task => @updated_payload, :stage_parent => "pipelines", :current_tab => "tasks"}

        expect(assigns[:errors].size).to eq(1)
        expect(assigns[:on_cancel_task_vms]).to eq(on_cancel_task_vms)
        assert_save_arguments
        assert_template "admin/tasks/plugin/edit"
        assert_template layout: false
        expect(response.status).to eq(400)
      end

    end

    describe "new" do
      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
        expect(@go_config_service).to receive(:doesPipelineExist).and_return(true)
        expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
        @task_view_service = stub_service(:task_view_service)
        @on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
      end

      it "should create a new task" do
        expect(@task_view_service).to receive(:getOnCancelTaskViewModels).with(@new_task).and_return(@on_cancel_task_vms)
        expect(@task_view_service).to receive(:taskInstanceFor).with(an_instance_of(String)).and_return(@new_task)
        expect(@task_view_service).to receive(:getViewModel).with(@new_task, "new").and_return(vm_template_for(@new_task))

        get :new, params: {:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :config_md5 => "abcd1234", :type => @task_type, :stage_parent => "pipelines", :current_tab => "tasks"}
        expect(assigns[:task]).to eq(@new_task)
        expect(assigns[:on_cancel_task_vms]).to eq(@on_cancel_task_vms)
        expect(response.status).to eq(200)
        expect(assigns[:config_store]).to eq(@config_store)
      end
    end

    describe "create" do
      before do
        expect(@go_config_service).to receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
        expect(@go_config_service).to receive(:doesPipelineExist).and_return(true)
        expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
        @task_view_service = stub_service(:task_view_service)
        expect(@task_view_service).to receive(:taskInstanceFor).with(@task_type).and_return(@new_task)
        controller_specific_setup(@task_view_service)
        expect(@pluggable_task_service).to receive(:validate) if (@new_task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask)
      end

      it "should create a task" do
        stub_config_save_with_subject(@created_task)

        post :create, params: {:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :type => @task_type, :config_md5 => "abcd1234", :task => @create_payload, :stage_parent => "pipelines", :current_tab => "tasks"}

        expect(@tasks.length).to eq(2)
        expect(assigns[:task]).to eq(@created_task)
        expect(assigns[:config_store]).to eq(nil)

        expect(response.status).to eq(200)
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode
      end

      it "should assign config_errors for display when update fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          result.badRequest('some message')
          config.errors().add("base", "someError")
        end

        expect(@task_view_service).to receive(:getViewModel).with(@created_task, 'new').and_return(vm_template_for(@created_task))
        @on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
        expect(@task_view_service).to receive(:getOnCancelTaskViewModels).with(@created_task).and_return(@on_cancel_task_vms)

        post :create, params: {:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :type => @task_type, :config_md5 => "1234abcd", :task => @create_payload, :stage_parent => "pipelines", :current_tab => "tasks"}

        expect(assigns[:errors].size).to eq(1)
        expect(assigns[:on_cancel_task_vms]).to eq(@on_cancel_task_vms)
        assert_save_arguments
        expect(assigns[:config_store]).not_to eq(nil)
        expect(assigns[:config_store]).to eq(@config_store)
        assert_template "admin/tasks/plugin/new"
        assert_template layout: false
        expect(response.status).to eq(400)
      end
    end
  end

  def vm_template_for task
    return vm_for task unless (task.instance_of? PluggableTask)
    PluggableTaskViewModelFactory.new().viewModelFor(task, "edit")
  end

  def expected_task(task)
    return task unless task.instance_of?(FetchTaskAdapter)
    task.getAppropriateTask
  end
end
