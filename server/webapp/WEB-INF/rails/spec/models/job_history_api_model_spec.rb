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

describe JobHistoryAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pagination_view_model = create_pagination_model
      @job_history_view_model = [create_job_model]
      job_history_api_model = JobHistoryAPIModel.new(@pagination_view_model, @job_history_view_model)

      expect(job_history_api_model.pagination.page_size).to eq(10)
      expect(job_history_api_model.pagination.offset).to eq(1)
      expect(job_history_api_model.pagination.total).to eq(100)

      job_instance_api_model = job_history_api_model.jobs[0]
      expect(job_instance_api_model.id).to eq(543)
      expect(job_instance_api_model.name).to eq('job name')
      expect(job_instance_api_model.state).to eq('state')
      expect(job_instance_api_model.result).to eq('result')
      expect(job_instance_api_model.scheduled_date).to eq(12345678)
      expect(job_instance_api_model.rerun).to eq(false)
      expect(job_instance_api_model.original_job_id).to eq(0)
      expect(job_instance_api_model.agent_uuid).to eq('uuid')
      expect(job_instance_api_model.pipeline_name).to eq('pipeline')
      expect(job_instance_api_model.pipeline_counter).to eq(123)
      expect(job_instance_api_model.stage_name).to eq('stage')
      expect(job_instance_api_model.stage_counter).to eq('1')

      job_state_transition_api_model = job_instance_api_model.job_state_transitions[0]
      expect(job_state_transition_api_model.id).to eq(987)
      expect(job_state_transition_api_model.state).to eq('building')
      expect(job_state_transition_api_model.state_change_time).to eq(12345678)
    end

    it "should handle empty data" do
      @pagination_view_model = create_empty_pagination_model
      @job_history_view_model = [create_empty_job_model]
      job_history_api_model = JobHistoryAPIModel.new(@pagination_view_model, @job_history_view_model)

      expect(job_history_api_model.pagination.page_size).to eq(nil)
      expect(job_history_api_model.pagination.offset).to eq(nil)
      expect(job_history_api_model.pagination.total).to eq(nil)

      job_instance_api_model = job_history_api_model.jobs[0]
      expect(job_instance_api_model.id).to eq(nil)
      expect(job_instance_api_model.name).to eq(nil)
      expect(job_instance_api_model.state).to eq(nil)
      expect(job_instance_api_model.result).to eq(nil)
      expect(job_instance_api_model.scheduled_date).to eq(nil)
      expect(job_instance_api_model.rerun).to eq(nil)
      expect(job_instance_api_model.original_job_id).to eq(nil)
      expect(job_instance_api_model.agent_uuid).to eq(nil)
      expect(job_instance_api_model.pipeline_name).to eq(nil)
      expect(job_instance_api_model.pipeline_counter).to eq(nil)
      expect(job_instance_api_model.stage_name).to eq(nil)
      expect(job_instance_api_model.stage_counter).to eq(nil)

      job_state_transition_api_model = job_instance_api_model.job_state_transitions[0]
      expect(job_state_transition_api_model.id).to eq(nil)
      expect(job_state_transition_api_model.state).to eq(nil)
      expect(job_state_transition_api_model.state_change_time).to eq(nil)
    end
  end
end
