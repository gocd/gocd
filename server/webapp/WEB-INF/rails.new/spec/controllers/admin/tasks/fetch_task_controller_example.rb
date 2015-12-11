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
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with(@pipeline_name).and_return(@pause_info)
      form_load_expectation
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    it "should load auto-suggest data for new fetch-form" do
      get :new, :current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :type => FetchTask.new.getTaskType(), :stage_parent => @parent_type
      assigns[:task].should == FetchTask.new
      assigns[:pipeline_json].should == pipelines_json
    end

    it "should load auto-suggest data for fetch-edit form" do
      get :edit, :current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :type => FetchTask.new.getTaskType(), :stage_parent => @parent_type, :task_index => '0'
      assigns[:task].should == fetch_task_with_exec_on_cancel_task
      assigns[:pipeline_json].should == pipelines_json
    end
  end

  describe "submission" do
    before do
      form_load_expectation
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with(@pipeline_name).and_return(@pause_info)
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    it "should load auto-suggest(off updated config) data when updating fetch task" do
      stub_save_for_success
      put :update, :current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :config_md5 => "abcd1234", :type => fetch_task_with_exec_on_cancel_task.getTaskType(), :stage_parent => @parent_type, :task_index => '0',
          :task => @modify_payload

      assigns[:task].should == fetch_task_with_exec_on_cancel_task("parent-pipeline", "parent-stage", "job.parent.1", "src-file", "dest-dir")
      assigns[:pipeline_json].should == pipelines_json
    end

    it "should load auto-suggest(off updated config) data when create fetch task" do
      stub_save_for_success
      post :create, :current_tab=>"tasks", :pipeline_name => @pipeline_name, :stage_name => @stage_name, :job_name => @job_name, :config_md5 => "abcd1234", :type => fetch_task_with_exec_on_cancel_task.getTaskType(), :stage_parent => @parent_type, :task => @modify_payload

      assigns[:task].should == fetch_task_with_exec_on_cancel_task("parent-pipeline", "parent-stage", "job.parent.1", "src-file", "dest-dir")
      assigns[:pipeline_json].should == pipelines_json
    end
  end
end