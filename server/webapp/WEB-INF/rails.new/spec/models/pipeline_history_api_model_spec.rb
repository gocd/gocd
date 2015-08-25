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

describe PipelineHistoryAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pagination_view_model = create_pagination_model
      @pipeline_history_view_model = create_pipeline_history_model
      pipeline_history_api_model = PipelineHistoryAPIModel.new(@pagination_view_model, @pipeline_history_view_model)

      pipeline_history_api_model.pagination.page_size.should == 10
      pipeline_history_api_model.pagination.offset.should == 1
      pipeline_history_api_model.pagination.total.should == 100

      pipeline_instance_api_model = pipeline_history_api_model.pipelines[0]
      pipeline_instance_api_model.id.should == 321
      pipeline_instance_api_model.name.should == 'pipeline name'
      pipeline_instance_api_model.counter.should == 123
      pipeline_instance_api_model.label.should == 'label'
      pipeline_instance_api_model.natural_order.should == 1
      pipeline_instance_api_model.can_run.should == true
      pipeline_instance_api_model.comment.should == 'pipeline comment'
      pipeline_instance_api_model.preparing_to_schedule.should == false

      build_cause_api_model = pipeline_instance_api_model.build_cause
      build_cause_api_model.trigger_message.should == 'message'
      build_cause_api_model.trigger_forced.should == true
      build_cause_api_model.approver.should == 'me'

      material_revision_api_model = build_cause_api_model.material_revisions[0]
      material_revision_api_model.changed.should == true

      material_api_model = material_revision_api_model.material
      material_api_model.id.should == 2
      material_api_model.fingerprint.should == 'fingerprint'
      material_api_model.type.should == 'git'
      material_api_model.description.should == 'URL: http://test.com Branch: master'

      modification_api_model = material_revision_api_model.modifications[0]
      modification_api_model.id.should == 321
      modification_api_model.revision.should == 'revision'
      modification_api_model.modified_time.should == 12345678
      modification_api_model.user_name.should == 'user name'
      modification_api_model.comment.should == 'comment'
      modification_api_model.email_address.should == 'test@test.com'

      stage_history_item_api_model = pipeline_instance_api_model.stages[0]
      stage_history_item_api_model.id.should == 456
      stage_history_item_api_model.name.should == 'stage name'
      stage_history_item_api_model.counter.should == '1'
      stage_history_item_api_model.scheduled.should == false
      stage_history_item_api_model.approval_type.should == 'manual'
      stage_history_item_api_model.approved_by.should == 'me'
      stage_history_item_api_model.result.should == 'passed'
      stage_history_item_api_model.rerun_of_counter.should == 1
      stage_history_item_api_model.operate_permission.should == 'yes'
      stage_history_item_api_model.can_run.should == true

      job_history_item_api_model = stage_history_item_api_model.jobs[0]
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
      @pipeline_history_view_model = create_empty_pipeline_history_model
      pipeline_history_api_model = PipelineHistoryAPIModel.new(@pagination_view_model, @pipeline_history_view_model)

      pipeline_history_api_model.pagination.page_size.should == nil
      pipeline_history_api_model.pagination.offset.should == nil
      pipeline_history_api_model.pagination.total.should == nil

      pipeline_instance_api_model = pipeline_history_api_model.pipelines[0]
      pipeline_instance_api_model.id.should == nil
      pipeline_instance_api_model.name.should == nil
      pipeline_instance_api_model.counter.should == nil
      pipeline_instance_api_model.label.should == nil
      pipeline_instance_api_model.natural_order.should == nil
      pipeline_instance_api_model.can_run.should == nil
      pipeline_instance_api_model.comment.should == nil
      pipeline_instance_api_model.preparing_to_schedule.should == nil

      build_cause_api_model = pipeline_instance_api_model.build_cause
      build_cause_api_model.trigger_message.should == nil
      build_cause_api_model.trigger_forced.should == nil
      build_cause_api_model.approver.should == nil

      material_revision_api_model = build_cause_api_model.material_revisions[0]
      material_revision_api_model.changed.should == nil

      material_api_model = material_revision_api_model.material
      material_api_model.id.should == nil
      material_api_model.fingerprint.should == nil
      material_api_model.type.should == nil
      material_api_model.description.should == nil

      modification_api_model = material_revision_api_model.modifications[0]
      modification_api_model.id.should == nil
      modification_api_model.revision.should == nil
      modification_api_model.modified_time.should == nil
      modification_api_model.user_name.should == nil
      modification_api_model.comment.should == nil
      modification_api_model.email_address.should == nil

      stage_history_item_api_model = pipeline_instance_api_model.stages[0]
      stage_history_item_api_model.id.should == nil
      stage_history_item_api_model.name.should == nil
      stage_history_item_api_model.counter.should == nil
      stage_history_item_api_model.scheduled.should == nil
      stage_history_item_api_model.approval_type.should == nil
      stage_history_item_api_model.approved_by.should == nil
      stage_history_item_api_model.result.should == nil
      stage_history_item_api_model.rerun_of_counter.should == nil
      stage_history_item_api_model.operate_permission.should == nil
      stage_history_item_api_model.can_run.should == nil

      job_history_item_api_model = stage_history_item_api_model.jobs[0]
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
