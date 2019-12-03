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


shared_examples_for :fetch_task_controller do
  describe "form" do
    before do
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with(@pipeline_name).and_return(@pause_info)
      form_load_expectation
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      allow(@go_config_service).to receive(:artifactIdToPluginIdForFetchPluggableArtifact).and_return({})
    end

    it "should load auto-suggest data for new fetch-form" do
      get :new, params:{:current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :type => FetchTask.new.getTaskType(), :stage_parent => @parent_type}
      expect(assigns[:task]).to eq(FetchTaskAdapter.new(FetchTask.new))
      expect(assigns[:pipeline_json]).to eq(pipelines_json)
    end

    it "should load auto-suggest data for fetch-edit form" do
      get :edit, params:{:current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :type => FetchTask.new.getTaskType(), :stage_parent => @parent_type, :task_index => '0'}
      expect(assigns[:task]).to eq(fetch_task_with_exec_on_cancel_task)
      expect(assigns[:pipeline_json]).to eq(pipelines_json)
    end
  end

  describe "submission" do
    before (:each) do
      form_load_expectation
      allow(@go_config_service).to receive(:getCurrentConfig).and_return(@cruise_config)
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with(@pipeline_name).and_return(@pause_info)
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      allow(@go_config_service).to receive(:artifactIdToPluginIdForFetchPluggableArtifact).and_return({})
      @pipeline_config_service = stub_service(:pipeline_config_service)
      allow(controller).to receive(:pipeline_config_service).and_return(@pipeline_config_service)
    end

    it "should load auto-suggest(off updated config) data when updating fetch task" do
      stub_config_save_with_subject(fetch_task_with_exec_on_cancel_task("parent-pipeline", "parent-stage", "job.parent.1", "src-file", "dest-dir"))
      put :update, params:{:current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :config_md5 => "abcd1234", :type => fetch_task_with_exec_on_cancel_task.getTaskType(), :stage_parent => @parent_type, :task_index => '0',
          :task => @modify_payload}

      expect(assigns[:task]).to eq(fetch_task_with_exec_on_cancel_task("parent-pipeline", "parent-stage", "job.parent.1", "src-file", "dest-dir"))
      expect(assigns[:pipeline_json]).to eq(pipelines_json)
    end

    it "should load auto-suggest(off updated config) data when create fetch task" do
      if @parent_type == 'pipelines'
        @pipeline = PipelineConfigMother.createPipelineConfig(@pipeline_name, @stage_name, [@job_name].to_java(java.lang.String))
        allow(@pipeline_config_service).to receive(:getPipelineConfig).with("pipeline.name").and_return(@pipeline)
        allow(@pipeline_config_service).to receive(:updatePipelineConfig).and_return(HttpLocalizedOperationResult.new)

        post :create, params:{:current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :config_md5 => "abcd1234", :type => fetch_task_with_exec_on_cancel_task.getTaskType(), :stage_parent => @parent_type, :task => @modify_payload}

        expect(assigns[:pipeline_json]).to eq(pipelines_json)
      elsif @parent_type == 'templates'
        stub_config_save_with_subject(fetch_task_with_exec_on_cancel_task("parent-pipeline", "parent-stage", "job.parent.1", "src-file", "dest-dir"))
        post :create, params:{:current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :config_md5 => "abcd1234", :type => fetch_task_with_exec_on_cancel_task.getTaskType(), :stage_parent => @parent_type, :task => @modify_payload}

        expect(assigns[:task]).to eq(fetch_task_with_exec_on_cancel_task("parent-pipeline", "parent-stage", "job.parent.1", "src-file", "dest-dir"))
        expect(assigns[:pipeline_json]).to eq(pipelines_json)
      end
    end
  end
end
