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

require 'rails_helper'

describe Admin::TasksController do
  include MockRegistryModule
  include TaskMother
  include ReflectiveUtil
  include ConfigSaveStubbing

  describe "increment" do

    before(:each) do
      allow(controller).to receive(:populate_config_validity)
      @pipeline = PipelineConfigMother.createPipelineConfig("pipeline.name", "stage.name", ["job.1"].to_java(java.lang.String))
      @tasks = @pipeline.get(0).getJobs().get(0).getTasks()
      @tasks.add(ant_task)
      @tasks.add(nant_task)
      @tasks.add(rake_task)
      @tasks.add(exec_task)

      @go_config_service = stub_service(:go_config_service)
      @pipeline_pause_service = stub_service(:pipeline_pause_service)

      @user = current_user
      @result = stub_localized_result

      @template = PipelineTemplateConfig.new(CaseInsensitiveString.new("template.name"), [StageConfigMother.stageWithTasks("stage_one"), StageConfigMother.stageWithTasks("stage_two")].to_java(StageConfig))
      @cruise_config = BasicCruiseConfig.new()
      @cruise_config.addTemplate(@template)
      set(@cruise_config, "md5", "abcd1234")
      @cruise_config.addPipeline("my-group", @pipeline)

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end


    it "should increment a task's index" do
      stub_save_for_success

      post :increment_index, params: { :pipeline_name => "template.name", :stage_name => "stage_one", :job_name => "job", :task_index=> "0", :config_md5 => "abcd1234", :stage_parent => "templates", :current_tab => "tasks" }

      tasks = @template.get(0).getJobs().get(0).getTasks()

      expect(tasks.size()).to eq(2)
      expect(tasks.get(0)).to be_an(AntTask)
      expect(tasks.get(1)).to be_a(ExecTask)
      assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode
    end

    it "should decrement a task's index" do
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)

      stub_save_for_success

      post :decrement_index, params: { :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :task_index=> "1", :config_md5 => "abcd1234", :stage_parent => "pipelines", :current_tab => "tasks" }

      expect(@tasks.size()).to eq(4)
      expect(@tasks.get(0)).to eq(nant_task)
      expect(@tasks.get(1)).to eq(ant_task)
      assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ::ConfigUpdate::JobNode
    end
  end

  describe "index" do

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

      expect(@go_config_service).to receive(:loadForEdit).with("pipeline.name", @user, @result).and_return(@pipeline_config_for_edit)
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline.name").and_return(@pause_info)
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    it "should load tasks" do
      expect(controller).to receive(:task_view_service).and_return(task_view_service = double("task_view_service"))
      expect(task_view_service).to receive(:getTaskViewModels).and_return(tasks = [TaskViewModel.new(AntTask.new(), "new"), TaskViewModel.new(NantTask.new(), "new")].to_java(TaskViewModel))

      get :index, params: { :pipeline_name => "pipeline.name", :stage_name => "stage.name", :job_name => "job.1", :stage_parent => "pipelines", :current_tab=>"tasks" }

      expect(assigns[:pipeline]).to eq(@pipeline)
      expect(assigns[:stage]).to eq(@pipeline.get(0))
      expect(assigns[:job]).to eq(@pipeline.get(0).getJobs().get(0))
      expect(assigns[:tasks]).to eq(com.thoughtworks.go.config.Tasks.new([@example_task].to_java(Task)))
      expect(assigns[:task_view_models]).to eq(tasks)
      assert_template "index"
      assert_template layout: "pipelines/job"
    end
  end

  describe "config_store" do
    it "should return config store" do
      actual = controller.send(:config_store)
      expect(actual.instance_of?(com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore)).to eq(true)
    end
  end
end
