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
load File.join(File.dirname(__FILE__), 'task_controller_examples.rb')

describe Admin::TasksController do
  include TaskMother
  include FormUI
  include ConfigSaveStubbing

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
    @example_task = plugin_task
    @example_task.configuration.addNewConfiguration("Url", false)

    @task_type = plugin_task.getTaskType()
    @updated_payload = {:Url => "http://foo/bar"}
    @updated_task = plugin_task("curl.plugin", [ConfigurationPropertyMother.create("Url", false, "http://foo/bar")])

    @new_task = PluggableTask.new("", PluginConfiguration.new("curl.plugin", "1.0"), Configuration.new([ConfigurationPropertyMother.create("Url", false, nil)].to_java(ConfigurationProperty)))

    @create_payload= {:Url => "http://foo"}
    @created_task= plugin_task("curl.plugin", [ConfigurationPropertyMother.create("Url", false, "http://foo")])

  end

  it_should_behave_like :task_controller

  describe "actions" do
    before do
      controller.stub(:populate_config_validity)
      @pipeline = PipelineConfigMother.createPipelineConfig("pipeline.name", "stage.name", ["job.1", "job.2", "job.3"].to_java(java.lang.String))
      @tasks = @pipeline.get(0).getJobs().get(0).getTasks()
      @tasks.add(@example_task)

      @go_config_service = stub_service(:go_config_service)
      @pipeline_pause_service = stub_service(:pipeline_pause_service)

      @user = current_user
      @result = stub_localized_result

      @cruise_config = BasicCruiseConfig.new()
      @cruise_config.addPipeline("my-groups", @pipeline)
      set(@cruise_config, "md5", "abcd1234")

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      @go_config_service.should_receive(:getCurrentConfig).and_return(@cruise_config)

      @go_config_service.should_receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      @task_view_service = stub_service(:task_view_service)
      @pluggable_task_service = stub_service(:pluggable_task_service)
    end

    describe "create" do
      it "should perform plugin validation before creating a pluggable task" do
        @task_view_service.should_receive(:taskInstanceFor).with(@task_type).and_return(@new_task)

        @pluggable_task_service.should_receive(:validate) do |task|
          task.getConfiguration().getProperty("Url").addError("Url", "error message")
        end

        stub_save_for_validation_error do |result, config, node|
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end
        @task_view_service.should_receive(:getViewModel).with(@created_task, 'new').and_return(vm_template_for(@created_task))
        @on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task)].to_java(TaskViewModel))
        @task_view_service.should_receive(:getOnCancelTaskViewModels).with(@created_task).and_return(@on_cancel_task_vms)

        post :create, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :type => @task_type, :config_md5 => "1234abcd", :task => @create_payload, :stage_parent => "pipelines", :current_tab => "tasks"

        assigns[:task].getConfiguration().getProperty("Url").errors().getAll().size().should == 1
        assigns[:task].getConfiguration().getProperty("Url").errors().getAll().should include("error message")
        assert_save_arguments
        assert_template "admin/tasks/plugin/new"
        assert_template layout: false
        response.status.should == 400
      end
    end

    describe "update" do
      it "should perform plugin validation before updating a pluggable task" do
        @pluggable_task_service.should_receive(:validate) do |task|
          task.getConfiguration().getProperty("Url").addError("Url", "error message")
        end

        stub_save_for_validation_error do |result, config, node|
          result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
        end
        task_view_service = stub_service(:task_view_service)
        task_view_service.should_receive(:getViewModel).with(@updated_task, 'edit').and_return(vm_template_for(@updated_task))
        on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task)].to_java(TaskViewModel))
        task_view_service.should_receive(:getOnCancelTaskViewModels).with(@updated_task).and_return(on_cancel_task_vms)

        put :update, :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "1234abcd", :type => @task_type, :task => @updated_payload, :stage_parent => "pipelines", :current_tab => "tasks"

        assigns[:task].getConfiguration().getProperty("Url").errors().getAll().size().should == 1
        assigns[:task].getConfiguration().getProperty("Url").errors().getAll().should include("error message")
        assert_save_arguments
        assert_template "admin/tasks/plugin/edit"
        assert_template layout: false
        response.status.should == 400
      end
    end
  end
end
