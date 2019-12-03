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

describe Admin::TasksController do
  include ConfigSaveStubbing
  include TaskMother
  include FormUI

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
    @on_cancel_task = plugin_task
    @on_cancel_task.configuration.addNewConfiguration("Url", false)
    @on_cancel_task_type = plugin_task.getTaskType()

    @example_task = ant_task
    @task_type = ant_task.getTaskType()
    on_cancel_task_config_for_update = {:onCancelOption => "pluggable_task_curl_plugin", :pluggable_task_curl_pluginOnCancel => {:Url => "http://a"}}
    @updated_payload = {:buildFile => "newB", :target => "newT", :workingDirectory => "newWD", :hasCancelTask => "1", :onCancelConfig => on_cancel_task_config_for_update}
    @updated_task = ant_task("newB", "newT", "newWD")
    @subject = @updated_task
    @updated_on_cancel_task= plugin_task
    @updated_on_cancel_task.configuration.addNewConfiguration("Url", false)
    @updated_on_cancel_task.configuration.getProperty("Url").setConfigurationValue(ConfigurationValue.new("http://a"))
    @updated_task.setCancelTask(@updated_on_cancel_task)

    @new_task = AntTask.new
    on_cancel_task_config_for_create = {:onCancelOption => "pluggable_task_curl_plugin", :pluggable_task_curl_pluginOnCancel => {:Url => "http://b"}}
    @create_payload= {:buildFile => 'build.xml', :target => "compile", :workingDirectory => "dir", :hasCancelTask => "1", :onCancelConfig => on_cancel_task_config_for_create}
    @created_task= ant_task("build.xml", "compile", "dir")
    @created_on_cancel_task= plugin_task
    @created_on_cancel_task.configuration.addNewConfiguration("Url", false)
    @created_on_cancel_task.configuration.getProperty("Url").setConfigurationValue(ConfigurationValue.new("http://b"))
    @created_task.setCancelTask(@created_on_cancel_task)

  end

  describe "actions" do
    before do
      allow(controller).to receive(:populate_config_validity)
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
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      allow(@go_config_service).to receive(:artifactIdToPluginIdForFetchPluggableArtifact).and_return({})

      expect(@go_config_service).to receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
      expect(@go_config_service).to receive(:doesPipelineExist).and_return(true)
      expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      allow(@go_config_service).to receive(:pipelineConfigNamed).with(an_instance_of(CaseInsensitiveString)).and_return(@pipeline)
      @task_view_service = stub_service(:task_view_service)
      @pluggable_task_service = stub_service(:pluggable_task_service)
    end

    describe "update" do
      it "should perform plugin validation before updating a pluggable task" do
        expect(@task_view_service).to receive(:taskInstanceFor).with(@task_type).and_return(@new_task)
        expect(@task_view_service).to receive(:taskInstanceFor).with(@on_cancel_task_type).and_return(@on_cancel_task)

        expect(@pluggable_task_service).to receive(:validate) do |task|
          task.getConfiguration().getProperty("Url").addError("Url", "error message")
        end

        stub_save_for_validation_error do |result, config, node|
          result.badRequest('some message')
        end
        expect(@task_view_service).to receive(:getViewModel).with(@updated_task, 'edit').and_return(vm_template_for(@updated_task))
        on_cancel_task_vms = java.util.Arrays.asList([vm_template_for(exec_task('rm')), vm_template_for(ant_task), vm_template_for(nant_task), vm_template_for(rake_task), vm_template_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel))
        expect(@task_view_service).to receive(:getOnCancelTaskViewModels).with(@updated_task).and_return(on_cancel_task_vms)

        put :update, params:{:pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index => "0", :config_md5 => "1234abcd", :type => @task_type, :task => @updated_payload, :stage_parent => "pipelines", :current_tab => "tasks"}

        expect(assigns[:task].cancelTask().getConfiguration().getProperty("Url").errors().getAll().size()).to eq(1)
        expect(assigns[:task].cancelTask().getConfiguration().getProperty("Url").errors().getAll()).to include("error message")
        assert_save_arguments
        assert_template "admin/tasks/plugin/edit"
        assert_template layout: false
        expect(response.status).to eq(400)
      end
    end
  end

  def vm_template_for task
    return vm_for task unless (task.instance_of? PluggableTask)

    plugin_manager = double(PluginManager.class)
    expect(plugin_manager).to receive(:doOn).and_return(TaskViewStub.new)
    PluggableTaskViewModelFactory.new(plugin_manager).viewModelFor(task, "edit")
  end
end
