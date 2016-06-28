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
load File.join(File.dirname(__FILE__), 'fetch_task_controller_example.rb')

describe Admin::TasksController, "fetch task" do
  include TaskMother
  include FormUI
  include ConfigSaveStubbing

  before do
    @example_task = fetch_task_with_exec_on_cancel_task
    @task_type = fetch_task_with_exec_on_cancel_task.getTaskType()
    @updated_payload = {:pipelineName => 'other-pipeline', :stage => 'other-stage', :job => 'other-job', :src => 'new-src', :dest => 'new-dest', :isSourceAFile => '1', :hasCancelTask => "1", :onCancelConfig=> { :onCancelOption => 'exec', :execOnCancel => {:command => "echo", :args => "'failing'", :workingDirectory => "oncancel_working_dir"}}}
    @updated_task = fetch_task_with_exec_on_cancel_task('other-pipeline', 'other-stage', 'other-job', 'new-src', 'new-dest')

    @new_task = FetchTask.new

    @create_payload= {:pipelineName => 'pipeline', :stage => 'stage', :job => 'job', :src => 'src', :dest => 'dest', :isSourceAFile => '1', :hasCancelTask => "1", :onCancelConfig=> { :onCancelOption => 'exec', :execOnCancel => {:command => "echo", :args => "'failing'", :workingDirectory => "oncancel_working_dir"}}}
    @created_task= fetch_task_with_exec_on_cancel_task
  end


  it_should_behave_like :task_controller

  describe "auto-suggest" do
    before(:each) do
      controller.stub(:populate_config_validity)
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
    end

    describe "when looking at pipeline" do
      before do
        @parent_type = "pipelines"
        @pipeline_name = "pipeline.name"
        @pipeline_name_object = @pipeline_name
        @stage_name = "stage.three"
        @job_name = "dev"
        @modify_payload = {:pipelineName => 'parent-pipeline', :stage => 'parent-stage', :job => 'job.parent.1', :src => 'src-file', :dest => 'dest-dir', :isSourceAFile => '1', :hasCancelTask => "1", :onCancelConfig=> { :onCancelOption => 'exec', :execOnCancel => {:command => "echo", :args => "'failing'", :workingDirectory => "oncancel_working_dir"}}}
      end

      def form_load_expectation
        @go_config_service.should_receive(:loadForEdit).with(@pipeline_name, @user, @result).and_return(@pipeline_config_for_edit)
      end

      it_should_behave_like :fetch_task_controller

      def pipelines_json
        [
          {:pipeline => "", :stages => [{:stage => "stage.one", :jobs => [{:job => "dev"}]},
                                        {:stage => "stage.two", :jobs => [{:job => "dev"}]}]},
          {:pipeline => "parent-pipeline", :stages => [{:stage => "parent-stage", :jobs => [{:job => "job.parent.1"}]}]},
          {:pipeline => "pipeline.name", :stages => [{:stage => "stage.one", :jobs => [{:job => "dev"}]},
                                        {:stage => "stage.two", :jobs => [{:job => "dev"}]}]},
          {:pipeline => "gramp-pipeline/parent-pipeline", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1"}, {:job => "job.gramp.2"}]}]}
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
        @modify_payload = {:pipelineName => 'parent-pipeline', :stage => 'parent-stage', :job => 'job.parent.1', :src => 'src-file', :dest => 'dest-dir', :isSourceAFile => '1', :hasCancelTask => "1", :onCancelConfig=> { :onCancelOption => 'exec', :execOnCancel => {:command => "echo", :args => "'failing'", :workingDirectory => "oncancel_working_dir"}}}
      end

      def form_load_expectation
        template_config_for_edit = ConfigForEdit.new(@template, @cruise_config, @cruise_config)
        @template_config_service.should_receive(:loadForEdit).with(@pipeline_name, @user, @result).and_return(template_config_for_edit)
      end

      it_should_behave_like :fetch_task_controller

#      def pipelines_json
#        [{:pipeline => "gramp-pipeline", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1"}, {:job => "job.gramp.2"}]}]},
#         {:pipeline => "parent-pipeline", :stages => [{:stage => "parent-stage", :jobs => [{:job => "job.parent.1"}]}]},
#         {:pipeline => "pipeline.name", :stages => [{:stage => "stage.one", :jobs => [{:job => "dev"}]},
#                                                    {:stage => "stage.two", :jobs => [{:job => "dev"}]},
#                                                    {:stage => "stage.three", :jobs => [{:job => "dev"}]}]}].to_json
#      end

      def pipelines_json
        [
         {:pipeline => "gramp-pipeline", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1"}, {:job => "job.gramp.2"}]}]},
         {:pipeline => "parent-pipeline", :stages => [{:stage => "parent-stage", :jobs => [{:job => "job.parent.1"}]}]},
         {:pipeline => "pipeline.name", :stages => [{:stage => "stage.one", :jobs => [{:job => "dev"}]},
                                                    {:stage => "stage.three", :jobs => [{:job => "dev"}]},
                                                    {:stage => "stage.two", :jobs => [{:job => "dev"}]}]},
         {:pipeline => "gramp-pipeline/parent-pipeline", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1"}, {:job => "job.gramp.2"}]}]},
         {:pipeline => "parent-pipeline/pipeline.name", :stages => [{:stage => "parent-stage", :jobs => [{:job => "job.parent.1"}]}]},
         {:pipeline => "gramp-pipeline/parent-pipeline/pipeline.name", :stages => [{:stage => "gramp-stage", :jobs => [{:job => "job.gramp.1"}, {:job => "job.gramp.2"}]}]}
        ].to_json
      end
    end
  end

  def controller_specific_setup task_view_service
    task_view_service.stub(:taskInstanceFor).with("exec").and_return(exec_task_without_on_cancel)
  end
end
