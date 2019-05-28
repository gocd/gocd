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

describe PipelineHistoryAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pagination_view_model = create_pagination_model
      @pipeline_history_view_model = create_pipeline_history_model
      pipeline_history_api_model = PipelineHistoryAPIModel.new(@pagination_view_model, @pipeline_history_view_model)

      expect(pipeline_history_api_model.pagination.page_size).to eq(10)
      expect(pipeline_history_api_model.pagination.offset).to eq(1)
      expect(pipeline_history_api_model.pagination.total).to eq(100)

      pipeline_instance_api_model = pipeline_history_api_model.pipelines[0]
      expect(pipeline_instance_api_model.id).to eq(321)
      expect(pipeline_instance_api_model.name).to eq('pipeline name')
      expect(pipeline_instance_api_model.counter).to eq(123)
      expect(pipeline_instance_api_model.label).to eq('label')
      expect(pipeline_instance_api_model.natural_order).to eq(1)
      expect(pipeline_instance_api_model.can_run).to eq(true)
      expect(pipeline_instance_api_model.comment).to eq('pipeline comment')
      expect(pipeline_instance_api_model.preparing_to_schedule).to eq(false)

      build_cause_api_model = pipeline_instance_api_model.build_cause
      expect(build_cause_api_model.trigger_message).to eq('message')
      expect(build_cause_api_model.trigger_forced).to eq(true)
      expect(build_cause_api_model.approver).to eq('me')

      material_revision_api_model = build_cause_api_model.material_revisions[0]
      expect(material_revision_api_model.changed).to eq(true)

      material_api_model = material_revision_api_model.material
      expect(material_api_model.id).to eq(2)
      expect(material_api_model.fingerprint).to eq('fingerprint')
      expect(material_api_model.type).to eq('git')
      expect(material_api_model.description).to eq('URL: http://test.com Branch: master')

      modification_api_model = material_revision_api_model.modifications[0]
      expect(modification_api_model.id).to eq(321)
      expect(modification_api_model.revision).to eq('revision')
      expect(modification_api_model.modified_time).to eq(12345678)
      expect(modification_api_model.user_name).to eq('user name')
      expect(modification_api_model.comment).to eq('comment')
      expect(modification_api_model.email_address).to eq('test@test.com')

      stage_history_item_api_model = pipeline_instance_api_model.stages[0]
      expect(stage_history_item_api_model.id).to eq(456)
      expect(stage_history_item_api_model.name).to eq('stage name')
      expect(stage_history_item_api_model.counter).to eq('1')
      expect(stage_history_item_api_model.scheduled).to eq(false)
      expect(stage_history_item_api_model.approval_type).to eq('manual')
      expect(stage_history_item_api_model.approved_by).to eq('me')
      expect(stage_history_item_api_model.result).to eq('passed')
      expect(stage_history_item_api_model.rerun_of_counter).to eq(1)
      expect(stage_history_item_api_model.operate_permission).to eq('yes')
      expect(stage_history_item_api_model.can_run).to eq(true)

      job_history_item_api_model = stage_history_item_api_model.jobs[0]
      expect(job_history_item_api_model.id).to eq(543)
      expect(job_history_item_api_model.name).to eq('job name')
      expect(job_history_item_api_model.state).to eq('state')
      expect(job_history_item_api_model.result).to eq('result')
      expect(job_history_item_api_model.scheduled_date).to eq(12345678)
      expect(job_history_item_api_model.rerun).to eq(nil)
      expect(job_history_item_api_model.original_job_id).to eq(nil)
      expect(job_history_item_api_model.agent_uuid).to eq(nil)
      expect(job_history_item_api_model.pipeline_name).to eq(nil)
      expect(job_history_item_api_model.pipeline_counter).to eq(nil)
      expect(job_history_item_api_model.stage_name).to eq(nil)
      expect(job_history_item_api_model.stage_counter).to eq(nil)
    end

    it "should handle empty data" do
      @pagination_view_model = create_empty_pagination_model
      @pipeline_history_view_model = create_empty_pipeline_history_model
      pipeline_history_api_model = PipelineHistoryAPIModel.new(@pagination_view_model, @pipeline_history_view_model)

      expect(pipeline_history_api_model.pagination.page_size).to eq(nil)
      expect(pipeline_history_api_model.pagination.offset).to eq(nil)
      expect(pipeline_history_api_model.pagination.total).to eq(nil)

      pipeline_instance_api_model = pipeline_history_api_model.pipelines[0]
      expect(pipeline_instance_api_model.id).to eq(nil)
      expect(pipeline_instance_api_model.name).to eq(nil)
      expect(pipeline_instance_api_model.counter).to eq(nil)
      expect(pipeline_instance_api_model.label).to eq(nil)
      expect(pipeline_instance_api_model.natural_order).to eq(nil)
      expect(pipeline_instance_api_model.can_run).to eq(nil)
      expect(pipeline_instance_api_model.comment).to eq(nil)
      expect(pipeline_instance_api_model.preparing_to_schedule).to eq(nil)

      build_cause_api_model = pipeline_instance_api_model.build_cause
      expect(build_cause_api_model.trigger_message).to eq(nil)
      expect(build_cause_api_model.trigger_forced).to eq(nil)
      expect(build_cause_api_model.approver).to eq(nil)

      material_revision_api_model = build_cause_api_model.material_revisions[0]
      expect(material_revision_api_model.changed).to eq(nil)

      material_api_model = material_revision_api_model.material
      expect(material_api_model.id).to eq(nil)
      expect(material_api_model.fingerprint).to eq(nil)
      expect(material_api_model.type).to eq(nil)
      expect(material_api_model.description).to eq(nil)

      modification_api_model = material_revision_api_model.modifications[0]
      expect(modification_api_model.id).to eq(nil)
      expect(modification_api_model.revision).to eq(nil)
      expect(modification_api_model.modified_time).to eq(nil)
      expect(modification_api_model.user_name).to eq(nil)
      expect(modification_api_model.comment).to eq(nil)
      expect(modification_api_model.email_address).to eq(nil)

      stage_history_item_api_model = pipeline_instance_api_model.stages[0]
      expect(stage_history_item_api_model.id).to eq(nil)
      expect(stage_history_item_api_model.name).to eq(nil)
      expect(stage_history_item_api_model.counter).to eq(nil)
      expect(stage_history_item_api_model.scheduled).to eq(nil)
      expect(stage_history_item_api_model.approval_type).to eq(nil)
      expect(stage_history_item_api_model.approved_by).to eq(nil)
      expect(stage_history_item_api_model.result).to eq(nil)
      expect(stage_history_item_api_model.rerun_of_counter).to eq(nil)
      expect(stage_history_item_api_model.operate_permission).to eq(nil)
      expect(stage_history_item_api_model.can_run).to eq(nil)

      job_history_item_api_model = stage_history_item_api_model.jobs[0]
      expect(job_history_item_api_model.id).to eq(nil)
      expect(job_history_item_api_model.name).to eq(nil)
      expect(job_history_item_api_model.state).to eq(nil)
      expect(job_history_item_api_model.result).to eq(nil)
      expect(job_history_item_api_model.scheduled_date).to eq(nil)
      expect(job_history_item_api_model.rerun).to eq(nil)
      expect(job_history_item_api_model.original_job_id).to eq(nil)
      expect(job_history_item_api_model.agent_uuid).to eq(nil)
      expect(job_history_item_api_model.pipeline_name).to eq(nil)
      expect(job_history_item_api_model.pipeline_counter).to eq(nil)
      expect(job_history_item_api_model.stage_name).to eq(nil)
      expect(job_history_item_api_model.stage_counter).to eq(nil)
    end
  end
end
