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


shared_examples_for :fetch_task_controller do
  describe "form" do
    before do
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with(@pipeline_name).and_return(@pause_info)
      form_load_expectation
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    it "should load auto-suggest data for new fetch-form" do
      get :new, params: { :current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :type => FetchTask.new.getTaskType(), :stage_parent => @parent_type }
      expect(assigns[:task]).to eq(FetchTask.new)
      expect(assigns[:pipeline_json]).to eq(pipelines_json)
    end

    it "should load auto-suggest data for fetch-edit form" do
      get :edit, params: { :current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :type => FetchTask.new.getTaskType(), :stage_parent => @parent_type, :task_index => '0' }
      expect(assigns[:task]).to eq(fetch_task_with_exec_on_cancel_task)
      expect(assigns[:pipeline_json]).to eq(pipelines_json)
    end
  end

  describe "submission" do
    before do
      form_load_expectation
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with(@pipeline_name).and_return(@pause_info)
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    it "should load auto-suggest(off updated config) data when updating fetch task" do
      stub_save_for_success
      put :update, params: { :current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :config_md5 => "abcd1234", :type => fetch_task_with_exec_on_cancel_task.getTaskType(), :stage_parent => @parent_type, :task_index => '0',
          :task => @modify_payload }

      expect(assigns[:task]).to eq(fetch_task_with_exec_on_cancel_task("parent-pipeline", "parent-stage", "job.parent.1", "src-file", "dest-dir"))
      expect(assigns[:pipeline_json]).to eq(pipelines_json)
    end

    it "should load auto-suggest(off updated config) data when create fetch task" do
      stub_save_for_success
      post :create, params: { :current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :config_md5 => "abcd1234", :type => fetch_task_with_exec_on_cancel_task.getTaskType(), :stage_parent => @parent_type, :task => @modify_payload }

      expect(assigns[:task]).to eq(fetch_task_with_exec_on_cancel_task("parent-pipeline", "parent-stage", "job.parent.1", "src-file", "dest-dir"))
      expect(assigns[:pipeline_json]).to eq(pipelines_json)
    end
  end
end