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
require_relative 'task_controller_examples'
require_relative 'fetch_task_controller_example'

describe Admin::TasksController, "fetch task" do
  include TaskMother
  include FormUI
  include ConfigSaveStubbing

  before do
    @example_task = fetch_external_task
    @task_type = @example_task.getTaskType()

    @updated_task = fetch_external_task('other-pipeline', 'other-stage', 'other-job', 'installers')
    @updated_payload = {:pipelineName => 'other-pipeline', :stage => 'other-stage', :job => 'other-job', :artifactId => 'docker', :pluginId => 'cd.go.artifact.docker',
                        :configuration => {:DummyField => 'Some values'},
                        :hasCancelTask => "1",
                        :onCancelConfig => {
                          :onCancelOption => 'exec',
                          :execOnCancel => {:command => "echo", :args => "'failing'", :workingDirectory => "oncancel_working_dir"}
                        }
    }

    @updated_task_adapter = fetch_external_task('other-pipeline', 'other-stage', 'other-job', 'installers')
    @subject = @updated_task.getAppropriateTask
    @new_task = FetchTaskAdapter.new(FetchPluggableArtifactTask.new)

    @create_payload = {:selectedTaskType => 'external', :pipelineName => 'pipeline', :stage => 'stage', :job => 'job', :artifactId => 'docker', :pluginId => 'cd.go.artifact.docker',
                       :configuration => {:DummyField => 'FooBar'},
                       :hasCancelTask => "1",
                       :onCancelConfig => {
                         :onCancelOption => 'exec',
                         :execOnCancel => {:command => "echo", :args => "'failing'", :workingDirectory => "oncancel_working_dir"}
                       }
    }
    @created_task = @new_task

    @entity_hashing_service = stub_service(:entity_hashing_service)
    allow(@entity_hashing_service).to receive(:md5ForEntity).and_return('pipeline-md5')
  end

  it_should_behave_like :task_controller

  describe "auto-suggest" do
    before(:each) do
      allow(controller).to receive(:populate_config_validity)
      @gramp_pipeline = PipelineConfigMother.createPipelineConfig("gramp-pipeline", "gramp-stage", ["job.gramp.1", "job.gramp.2"].to_java(java.lang.String))
      @parent_pipeline = PipelineConfigMother.createPipelineConfig("parent-pipeline", "parent-stage", ["job.parent.1"].to_java(java.lang.String))
      @parent_pipeline.addMaterialConfig(DependencyMaterialConfig.new(CaseInsensitiveString.new("gramp-pipeline"), CaseInsensitiveString.new("gramp-stage")))
      @pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline.name", ["stage.one", "stage.two", "stage.three"].to_java(java.lang.String))
      @pipeline.addMaterialConfig(DependencyMaterialConfig.new(CaseInsensitiveString.new("parent-pipeline"), CaseInsensitiveString.new("parent-stage")))
      @tasks = @pipeline.getStage(CaseInsensitiveString.new("stage.three")).getJobs().get(0).getTasks()
      @tasks.add(fetch_task_with_exec_on_cancel_task)

      @go_config_service = stub_service(:go_config_service)
      @pipeline_pause_service = stub_service(:pipeline_pause_service)

      @user = current_user
      @result = stub_localized_result

      stage_one = StageConfigMother.stageWithTasks("stage_one")
      tasks = stage_one.jobConfigByConfigName(CaseInsensitiveString.new("job")).getTasks()
      tasks.clear
      tasks.add(fetch_task_with_exec_on_cancel_task)
      @template = PipelineTemplateConfig.new(CaseInsensitiveString.new("template.name"), [stage_one].to_java(StageConfig))

      @cruise_config = BasicCruiseConfig.new()
      @cruise_config.addPipeline("my-groups", @pipeline)
      @cruise_config.addPipeline("my-groups", @parent_pipeline)
      @cruise_config.addPipeline("old-groups", @gramp_pipeline)
      @cruise_config.addTemplate(@template)
      set(@cruise_config, "md5", "abcd1234")

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")

      allow(@go_config_service).to receive(:pipelineConfigNamed).with(an_instance_of(CaseInsensitiveString)).and_return(@pipeline_config_for_edit)
      allow(@go_config_service).to receive(:templateConfigNamed).with(an_instance_of(CaseInsensitiveString)).and_return(@template)
    end

    describe "when looking at pipeline" do
      before do
        @parent_type = "pipelines"
        @pipeline_name = "pipeline.name"
        @pipeline_name_object = @pipeline_name
        @stage_name = "stage.three"
        @job_name = "dev"
        @modify_payload = {:pipelineName => 'parent-pipeline', :stage => 'parent-stage', :job => 'job.parent.1', :src => 'src-file', :dest => 'dest-dir', :isSourceAFile => '1', :hasCancelTask => "1", :onCancelConfig => {:onCancelOption => 'exec', :execOnCancel => {:command => "echo", :args => "'failing'", :workingDirectory => "oncancel_working_dir"}}}
      end

      def form_load_expectation
        expect(@go_config_service).to receive(:loadForEdit).with(@pipeline_name, @user, @result).and_return(@pipeline_config_for_edit)
        expect(@go_config_service).to receive(:canEditPipeline).and_return(true)
        expect(@go_config_service).to receive(:isPipelineDefinedInConfigRepository).and_return(false)
      end

      it_should_behave_like :fetch_task_controller

      def pipelines_json
        [
          {:pipeline => "", :stages => [{:stage => "stage.one", :jobs => [{:job => "dev", "artifacts": {}}]},
                                        {:stage => "stage.two", :jobs => [{:job => "dev", "artifacts": {}}]}]},
          {:pipeline => "parent-pipeline", :stages => [{:stage => "parent-stage", :jobs => [{:job => "job.parent.1", "artifacts": {}}]}]},
          {:pipeline => "pipeline.name", :stages => [{:stage => "stage.one", :jobs => [{:job => "dev", "artifacts": {}}]},
                                                     {:stage => "stage.two", :jobs => [{:job => "dev", "artifacts": {}}]}]},
          {:pipeline => "gramp-pipeline/parent-pipeline", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1", "artifacts": {}}, {:job => "job.gramp.2", "artifacts": {}}]}]}
        ].to_json
      end
    end

    describe "when looking at template" do
      before do
        @parent_type = "templates"
        @pipeline_name = "template.name"
        @pipeline_name_object = CaseInsensitiveString.new(@pipeline_name)
        @stage_name = "stage_one"
        @job_name = "job"

        @template_config_service = stub_service(:template_config_service)
        @modify_payload = {:pipelineName => 'parent-pipeline', :stage => 'parent-stage', :job => 'job.parent.1', :src => 'src-file', :dest => 'dest-dir', :isSourceAFile => '1', :hasCancelTask => "1", :onCancelConfig => {:onCancelOption => 'exec', :execOnCancel => {:command => "echo", :args => "'failing'", :workingDirectory => "oncancel_working_dir"}}}
      end

      def form_load_expectation
        template_config_for_edit = ConfigForEdit.new(@template, @cruise_config, @cruise_config)
        expect(@template_config_service).to receive(:loadForEdit).with(@pipeline_name, @user, @result).and_return(template_config_for_edit)
      end

      it_should_behave_like :fetch_task_controller

      def pipelines_json
        [
          {:pipeline => "gramp-pipeline", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1", "artifacts": {}}, {:job => "job.gramp.2", "artifacts": {}}]}]},
          {:pipeline => "parent-pipeline", :stages => [{:stage => "parent-stage", :jobs => [{:job => "job.parent.1", "artifacts": {}}]}]},
          {:pipeline => "pipeline.name", :stages => [{:stage => "stage.one", :jobs => [{:job => "dev", "artifacts": {}}]},
                                                     {:stage => "stage.three", :jobs => [{:job => "dev", "artifacts": {}}]},
                                                     {:stage => "stage.two", :jobs => [{:job => "dev", "artifacts": {}}]}]},
          {:pipeline => "gramp-pipeline/parent-pipeline", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1", "artifacts": {}}, {:job => "job.gramp.2", "artifacts": {}}]}]},
          {:pipeline => "parent-pipeline/pipeline.name", :stages => [{:stage => "parent-stage", :jobs => [{:job => "job.parent.1", "artifacts": {}}]}]},
          {:pipeline => "gramp-pipeline/parent-pipeline/pipeline.name", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1", "artifacts": {}}, {:job => "job.gramp.2", "artifacts": {}}]}]}
        ].to_json
      end
    end
  end

  def controller_specific_setup task_view_service
    allow(task_view_service).to receive(:taskInstanceFor).with("fetch").and_return(@example_task)
    allow(task_view_service).to receive(:taskInstanceFor).with("exec").and_return(@example_task.cancelTask)
  end
end
