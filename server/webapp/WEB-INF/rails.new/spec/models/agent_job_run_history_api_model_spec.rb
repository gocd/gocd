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

describe AgentJobRunHistoryAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pagination_view_model = create_pagination_model
      @job_run_history_view_model = create_agent_job_run_history_model
      agent_job_run_history_api_model = AgentJobRunHistoryAPIModel.new(@pagination_view_model, @job_run_history_view_model)

      agent_job_run_history_api_model.pagination.page_size.should == 10
      agent_job_run_history_api_model.pagination.offset.should == 1
      agent_job_run_history_api_model.pagination.total.should == 100

      job_instance_api_model = agent_job_run_history_api_model.jobs[0]
      job_instance_api_model.id.should == 543
      job_instance_api_model.name.should == 'job name'
      job_instance_api_model.state.should == 'state'
      job_instance_api_model.result.should == 'result'
      job_instance_api_model.scheduled_date.should == 12345678
      job_instance_api_model.rerun.should == false
      job_instance_api_model.original_job_id.should == 0
      job_instance_api_model.agent_uuid.should == 'uuid'
      job_instance_api_model.pipeline_name.should == 'pipeline'
      job_instance_api_model.pipeline_counter.should == 123
      job_instance_api_model.stage_name.should == 'stage'
      job_instance_api_model.stage_counter.should == '1'

      job_state_transition_api_model = job_instance_api_model.job_state_transitions[0]
      job_state_transition_api_model.id.should == 987
      job_state_transition_api_model.state.should == 'building'
      job_state_transition_api_model.state_change_time.should == 12345678
    end

    it "should handle empty data" do
      @pagination_view_model = create_empty_pagination_model
      @job_run_history_view_model = create_empty_agent_job_run_history_model
      agent_job_run_history_api_model = AgentJobRunHistoryAPIModel.new(@pagination_view_model, @job_run_history_view_model)

      agent_job_run_history_api_model.pagination.page_size.should == nil
      agent_job_run_history_api_model.pagination.offset.should == nil
      agent_job_run_history_api_model.pagination.total.should == nil

      job_instance_api_model = agent_job_run_history_api_model.jobs[0]
      job_instance_api_model.id.should == nil
      job_instance_api_model.name.should == nil
      job_instance_api_model.state.should == nil
      job_instance_api_model.result.should == nil
      job_instance_api_model.scheduled_date.should == nil
      job_instance_api_model.rerun.should == nil
      job_instance_api_model.original_job_id.should == nil
      job_instance_api_model.agent_uuid.should == nil
      job_instance_api_model.pipeline_name.should == nil
      job_instance_api_model.pipeline_counter.should == nil
      job_instance_api_model.stage_name.should == nil
      job_instance_api_model.stage_counter.should == nil

      job_state_transition_api_model = job_instance_api_model.job_state_transitions[0]
      job_state_transition_api_model.id.should == nil
      job_state_transition_api_model.state.should == nil
      job_state_transition_api_model.state_change_time.should == nil
    end
  end
end
