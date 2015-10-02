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

describe StageHistoryAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pagination_view_model = create_pagination_model
      @stage_view_model = [create_stage_model]
      stage_history_api_model = StageHistoryAPIModel.new(@pagination_view_model, @stage_view_model)

      stage_history_api_model.pagination.page_size.should == 10
      stage_history_api_model.pagination.offset.should == 1
      stage_history_api_model.pagination.total.should == 100

      stage_instance_api_model = stage_history_api_model.stages[0]
      stage_instance_api_model.id.should == 456
      stage_instance_api_model.name.should == 'stage name'
      stage_instance_api_model.counter.should == '1'
      stage_instance_api_model.approval_type.should == 'manual'
      stage_instance_api_model.approved_by.should == 'me'
      stage_instance_api_model.result.should == 'passed'
      stage_instance_api_model.rerun_of_counter.should == 1
      stage_instance_api_model.pipeline_name.should == 'pipeline'
      stage_instance_api_model.pipeline_counter.should == 1

      job_history_item_api_model = stage_instance_api_model.jobs[0]
      job_history_item_api_model.id.should == 543
      job_history_item_api_model.name.should == 'job name'
      job_history_item_api_model.state.should == 'state'
      job_history_item_api_model.result.should == 'result'
      job_history_item_api_model.scheduled_date.should == 12345678
      job_history_item_api_model.rerun.should == nil
      job_history_item_api_model.original_job_id.should == nil
      job_history_item_api_model.agent_uuid.should == nil
      job_history_item_api_model.pipeline_name.should == nil
      job_history_item_api_model.pipeline_counter.should == nil
      job_history_item_api_model.stage_name.should == nil
      job_history_item_api_model.stage_counter.should == nil
    end

    it "should handle empty data" do
      @pagination_view_model = create_empty_pagination_model
      @stage_view_model = [create_empty_stage_model]
      stage_history_api_model = StageHistoryAPIModel.new(@pagination_view_model, @stage_view_model)

      stage_history_api_model.pagination.page_size.should == nil
      stage_history_api_model.pagination.offset.should == nil
      stage_history_api_model.pagination.total.should == nil

      stage_instance_api_model = stage_history_api_model.stages[0]
      stage_instance_api_model.id.should == nil
      stage_instance_api_model.name.should == nil
      stage_instance_api_model.counter.should == nil
      stage_instance_api_model.approval_type.should == nil
      stage_instance_api_model.approved_by.should == nil
      stage_instance_api_model.result.should == nil
      stage_instance_api_model.rerun_of_counter.should == nil
      stage_instance_api_model.pipeline_name.should == nil
      stage_instance_api_model.pipeline_counter.should == nil

      job_history_item_api_model = stage_instance_api_model.jobs[0]
      job_history_item_api_model.id.should == nil
      job_history_item_api_model.name.should == nil
      job_history_item_api_model.state.should == nil
      job_history_item_api_model.result.should == nil
      job_history_item_api_model.scheduled_date.should == nil
      job_history_item_api_model.rerun.should == nil
      job_history_item_api_model.original_job_id.should == nil
      job_history_item_api_model.agent_uuid.should == nil
      job_history_item_api_model.pipeline_name.should == nil
      job_history_item_api_model.pipeline_counter.should == nil
      job_history_item_api_model.stage_name.should == nil
      job_history_item_api_model.stage_counter.should == nil
    end
  end
end
