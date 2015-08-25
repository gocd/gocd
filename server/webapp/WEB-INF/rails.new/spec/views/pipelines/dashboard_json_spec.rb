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

describe "/pipelines/dashboard.json.erb" do
  include PipelineModelMother
  include PipelinesHelper

  context 'in JSON of one pipeline group with one pipeline and one active instance,' do
    let(:group_name) { 'first' }
    let(:pipeline_name) { 'pip1' }
    let(:pipeline_label) { '1' }
    let(:pipeline_counter) { 5 }

    before {
      group = PipelineGroupModel.new(group_name)
      pipeline = pipeline_model(pipeline_name, pipeline_label)
      group.add(pipeline)

      @json = render_json_for group
    }

    it 'should have only one group with one pipeline' do
      expect(@json.length).to eq(1)

      the_group = @json[0]
      expect(the_group["name"]).to eq(group_name)
      expect(the_group["pipelines"].length).to eq(1)
    end

    it 'should have one instance in the pipeline' do
      first_group_first_pipeline = @json[0]["pipelines"][0]

      assert_pipeline_details first_group_first_pipeline, pipeline_name, 1
    end

    it 'should have the details of the instance' do
      the_instance = @json[0]["pipelines"][0]["instances"][0]

      assert_instance_details(the_instance, pipeline_name, pipeline_label, pipeline_counter)
    end

    it 'should have one stage in the instance' do
      stage_name = "cruise"

      the_instance = @json[0]["pipelines"][0]["instances"][0]
      expect(the_instance["stages"].length).to eq(1)

      the_stage = @json[0]["pipelines"][0]["instances"][0]["stages"][0]
      expect(the_stage["name"]).to eq(stage_name)
      expect(the_stage["status"]).to eq("Passed")
      expect(the_stage["details_path"]).to eq("/pipelines/#{pipeline_name}/#{pipeline_counter}/#{stage_name}/10")
    end
  end

  context 'in JSON of multiple pipeline groups and pipelines,' do
    let(:group1_name) { "g1" }
    let(:group1_pipeline1_name) { "g1_p1" }
    let(:group1_pipeline2_name) { "g1_p2" }

    let(:group2_name) { "g2" }
    let(:group2_pipeline1_name) { "g2_p1" }
    let(:group2_pipeline2_name) { "g2_p2" }
    let(:group2_pipeline3_name) { "g2_p3" }

    let(:group3_name) { "g3" }
    let(:group3_pipeline1_name) { "g3_p1" }

    before {
      group1 = PipelineGroupModel.new(group1_name)
      group1.add(pipeline_model(group1_pipeline1_name, "#{group1_pipeline1_name}_label"))
      group1.add(pipeline_model(group1_pipeline2_name, "#{group1_pipeline2_name}_label"))

      group2 = PipelineGroupModel.new(group2_name)
      group2.add(pipeline_model(group2_pipeline1_name, "#{group2_pipeline1_name}_label"))
      group2.add(pipeline_model(group2_pipeline2_name, "#{group2_pipeline2_name}_label"))
      group2.add(pipeline_model(group2_pipeline3_name, "#{group2_pipeline3_name}_label"))

      group3 = PipelineGroupModel.new(group3_name)
      group3.add(pipeline_model(group3_pipeline1_name, "#{group3_pipeline1_name}_label"))

      @json = render_json_for group1, group2, group3
    }

    it 'should have multiple groups with the right number of pipelines' do
      expect(@json.length).to eq(3)

      the_group1 = @json[0]
      expect(the_group1["name"]).to eq(group1_name)
      expect(the_group1["pipelines"].length).to eq(2)

      the_group2 = @json[1]
      expect(the_group2["name"]).to eq(group2_name)
      expect(the_group2["pipelines"].length).to eq(3)

      the_group3 = @json[2]
      expect(the_group3["name"]).to eq(group3_name)
      expect(the_group3["pipelines"].length).to eq(1)
    end

    it 'should have pipeline details for all the pipelines' do
      assert_pipeline_details @json[0]["pipelines"][0], group1_pipeline1_name, 1
      assert_pipeline_details @json[0]["pipelines"][1], group1_pipeline2_name, 1

      assert_pipeline_details @json[1]["pipelines"][0], group2_pipeline1_name, 1
      assert_pipeline_details @json[1]["pipelines"][1], group2_pipeline2_name, 1
      assert_pipeline_details @json[1]["pipelines"][2], group2_pipeline3_name, 1

      assert_pipeline_details @json[2]["pipelines"][0], group3_pipeline1_name, 1
    end

    it 'should have instance details for all the pipelines' do
      assert_instance_details @json[0]["pipelines"][0]["instances"][0], group1_pipeline1_name, "#{group1_pipeline1_name}_label", 5
      assert_instance_details @json[0]["pipelines"][1]["instances"][0], group1_pipeline2_name, "#{group1_pipeline2_name}_label", 5

      assert_instance_details @json[1]["pipelines"][0]["instances"][0], group2_pipeline1_name, "#{group2_pipeline1_name}_label", 5
      assert_instance_details @json[1]["pipelines"][1]["instances"][0], group2_pipeline2_name, "#{group2_pipeline2_name}_label", 5
      assert_instance_details @json[1]["pipelines"][2]["instances"][0], group2_pipeline3_name, "#{group2_pipeline3_name}_label", 5

      assert_instance_details @json[2]["pipelines"][0]["instances"][0], group3_pipeline1_name, "#{group3_pipeline1_name}_label", 5
    end
  end

  context 'in JSON of multiple instances and stages,' do
    let(:group_name) { "g1" }
    let(:pipeline_name) { "g1_p1" }
    let(:pipeline_label) { "g1_p1_label" }
    let(:pipeline_counter) { 2 }

    before {
      instance1 = pipeline_instance_model_empty pipeline_name, "stage1", "stage2"
      instance2 = pipeline_instance_model(
          {:name => pipeline_name, :label => pipeline_label, :counter => pipeline_counter,
           :stages => [
               {:name => "p1_stage1", :counter => "10", :approved_by => "Anonymous"},
               {:name => "p1_stage2", :counter => "12", :approved_by => "SomeOne"}]})

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model_with_instances([instance1, instance2], pipeline_name))

      @json = render_json_for group
    }

    it 'should have no instance details for the instance with no history' do
      expect(@json[0]["pipelines"][0]["instances"].length).to eq(2)

      no_history_instance = @json[0]["pipelines"][0]["instances"][0]
      expect(no_history_instance["has_history"]).to eq("false")
      expect(no_history_instance.keys).to eq(["has_history"])
    end

    it 'should have instance details for instance with history' do
      expect(@json[0]["pipelines"][0]["instances"].length).to eq(2)

      the_instance = @json[0]["pipelines"][0]["instances"][1]
      assert_instance_details the_instance, pipeline_name, pipeline_label, pipeline_counter, "p1_stage2", "Passed"
    end

    it 'should have details for all stages of the instance with history' do
      the_instance = @json[0]["pipelines"][0]["instances"][1]

      expect(the_instance["stages"].length).to eq(2)

      the_first_stage = the_instance["stages"][0]
      expect(the_first_stage["name"]).to eq("p1_stage1")
      expect(the_first_stage["status"]).to eq("Passed")
      expect(the_first_stage["details_path"]).to eq("/pipelines/#{pipeline_name}/#{pipeline_counter}/p1_stage1/10")

      the_second_stage = the_instance["stages"][1]
      expect(the_second_stage["name"]).to eq("p1_stage2")
      expect(the_second_stage["status"]).to eq("Passed")
      expect(the_second_stage["details_path"]).to eq("/pipelines/#{pipeline_name}/#{pipeline_counter}/p1_stage2/12")
    end
  end

  context 'in JSON where some operations are not possible,' do
    let(:group_name) { 'first' }
    let(:pipeline_name) { 'pip1' }
    let(:pipeline_label) { '1' }
    let(:pipeline_counter) { 2 }

    it 'should have no operations when user has no operate permission' do
      can_operate = false

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model(pipeline_name, pipeline_label, false, true, nil, can_operate))
      groups_json = render_json_for group

      operations = groups_json[0]["pipelines"][0]["available_operations"]
      expect(operations.length).to eq(0)
    end

    it 'should have trigger, trigger-with-options and pause operations when pipeline is NOT paused, and user has operate permissions' do
      can_operate = true
      pause_cause = nil

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model(pipeline_name, pipeline_label, false, true, pause_cause, can_operate))
      groups_json = render_json_for group

      operations = groups_json[0]["pipelines"][0]["available_operations"]
      expect(operations.length).to eq(3)

      expect(operations[0]["operation"]).to eq("trigger")
      expect(operations[0]["operation_path"]).to eq("/api/pipelines/pip1/schedule")

      expect(operations[1]["operation"]).to eq("trigger_with_options")
      expect(operations[1]["operation_path"]).to eq("/pipelines/show_for_trigger")

      expect(operations[2]["operation"]).to eq("pause")
      expect(operations[2]["operation_path"]).to eq("/api/pipelines/pip1/pause")
    end

    it 'should have trigger, trigger-with-options and unpause operations when pipeline IS paused, and user has operate permissions' do
      can_operate = true
      pause_cause = "Wait for it"

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model(pipeline_name, pipeline_label, false, true, pause_cause, can_operate))
      groups_json = render_json_for group

      operations = groups_json[0]["pipelines"][0]["available_operations"]
      expect(operations.length).to eq(3)

      expect(operations[0]["operation"]).to eq("trigger")
      expect(operations[0]["operation_path"]).to eq("/api/pipelines/pip1/schedule")

      expect(operations[1]["operation"]).to eq("trigger_with_options")
      expect(operations[1]["operation_path"]).to eq("/pipelines/show_for_trigger")

      expect(operations[2]["operation"]).to eq("unpause")
      expect(operations[2]["operation_path"]).to eq("/api/pipelines/pip1/unpause")
    end

    it 'should have can_administer set to true when user has administration permission on pipeline' do
      can_administer = true

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model(pipeline_name, pipeline_label, false, true, nil, true))
      group.getPipelineModels[0].updateAdministrability(can_administer)
      groups_json = render_json_for group

      the_pipeline = groups_json[0]["pipelines"][0]
      expect(the_pipeline["can_administer"]).to eq(true)
    end

    it 'should have can_administer set to false when user does not have administration permission on pipeline' do
      can_administer = false

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model(pipeline_name, pipeline_label, false, true, nil, true))
      group.getPipelineModels[0].updateAdministrability(can_administer)
      groups_json = render_json_for group

      the_pipeline = groups_json[0]["pipelines"][0]
      expect(the_pipeline["can_administer"]).to eq(false)
    end

    it 'should have unlock operation when pipeline is locked, and user has permission to unlock it' do
      is_locked = true
      can_unlock = true

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model_with_lock_status(can_unlock, is_locked, pipeline_name, pipeline_label, pipeline_counter))
      groups_json = render_json_for group

      the_pipeline = groups_json[0]["pipelines"][0]
      unlock_operation = the_pipeline["available_operations"].find {|op| op["operation"] == "unlock"}

      expect(the_pipeline["is_locked"]).to eq(true)
      expect(unlock_operation).to_not be_nil
      expect(unlock_operation["operation_path"]).to eq("/api/pipelines/#{pipeline_name}/releaseLock")
    end

    it 'should NOT have unlock operation when pipeline is locked, and user does NOT have permission to unlock it' do
      is_locked = true
      can_unlock = false

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model_with_lock_status(can_unlock, is_locked, pipeline_name, pipeline_label, pipeline_counter))
      groups_json = render_json_for group

      the_pipeline = groups_json[0]["pipelines"][0]
      unlock_operation = the_pipeline["available_operations"].find {|op| op["operation"] == "unlock"}

      expect(the_pipeline["is_locked"]).to eq(true)
      expect(unlock_operation).to be_nil
    end

    it 'should NOT have unlock operation when pipeline is NOT locked' do
      is_locked = false
      can_unlock = true

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model_with_lock_status(can_unlock, is_locked, pipeline_name, pipeline_label, pipeline_counter))
      groups_json = render_json_for group

      the_pipeline = groups_json[0]["pipelines"][0]
      unlock_operation = the_pipeline["available_operations"].find {|op| op["operation"] == "unlock"}

      expect(the_pipeline["is_locked"]).to eq(false)
      expect(unlock_operation).to be_nil
    end
  end

  context 'in JSON, information about pause,' do
    let(:group_name) { 'first' }
    let(:pipeline_name) { 'pip1' }
    let(:pipeline_label) { '1' }

    it 'should have no pause message and paused_by when pipeline is not paused' do
      can_operate = true
      pause_cause = nil

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model(pipeline_name, pipeline_label, false, true, pause_cause, can_operate))
      groups_json = render_json_for group

      pause_info = groups_json[0]["pipelines"][0]["pause_info"]
      expect(pause_info["is_paused"]).to eq(false)
      expect(pause_info.include? "paused_by").to be_false
      expect(pause_info.include? "message").to be_false
    end

    it 'should have pause message and paused_by when pipeline is paused' do
      can_operate = true
      pause_cause = "Wait for it"

      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_model(pipeline_name, pipeline_label, false, true, pause_cause, can_operate))
      groups_json = render_json_for group

      pause_info = groups_json[0]["pipelines"][0]["pause_info"]
      expect(pause_info["is_paused"]).to eq(true)
      expect(pause_info["paused_by"]).to eq("raghu")
      expect(pause_info["message"]).to eq(pause_cause)
    end
  end

  context 'in JSON, information about previous stage status,' do
    let(:group_name) { 'first' }
    let(:pipeline_name) { 'pip1' }
    let(:pipeline_label) { '1' }
    let(:pipeline_counter) { 4 }

    it 'should have no previous run information when no stage is active in current pipeline' do
      group = PipelineGroupModel.new(group_name)
      pipeline_model = pipeline_model(pipeline_name, pipeline_label)
      group.add(pipeline_model)

      expect(pipeline_model.getLatestPipelineInstance().isAnyStageActive()).to be_false

      groups_json = render_json_for group

      expect(groups_json[0]["pipelines"][0].include? "previous_instance").to be_false
    end

    it 'should have no previous run information when a stage is active but it has no previous stage' do
      running_instance = pipeline_instance_model(
          {:name => pipeline_name, :label => pipeline_label, :counter => pipeline_counter,
           :stages => [
               {:name => "p1_stage1", :counter => "10", :approved_by => "Anonymous", :job_state => JobState::Completed, :job_result => JobResult::Passed},
               {:name => "p1_stage2", :counter => "12", :approved_by => "SomeOne", :job_state => JobState::Building, :job_result => JobResult::Unknown}]})

      pipeline_with_active_instance = pipeline_model_with_instances([running_instance], pipeline_name)
      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_with_active_instance)

      expect(pipeline_with_active_instance.getLatestPipelineInstance().isAnyStageActive()).to be_true
      expect(pipeline_with_active_instance.getLatestPipelineInstance().activeStage().hasPreviousStage()).to be_false

      groups_json = render_json_for group

      expect(groups_json[0]["pipelines"][0].include? "previous_instance").to be_false
    end

    it 'should have previous run information when a stage is active and has previous stage' do
      previous_stage = stage_model_with_result "p1_stage2", "11", StageResult::Failed, JobState::Completed, JobResult::Failed
      previous_stage.getIdentifier().setPipelineLabel("SOME_PIPELINE_LABEL")

      running_instance = pipeline_instance_model(
          {:name => pipeline_name, :label => pipeline_label, :counter => pipeline_counter,
           :stages => [
               {:name => "p1_stage1", :counter => "10", :approved_by => "Anonymous", :job_state => JobState::Completed, :job_result => JobResult::Passed},
               {:name => "p1_stage2", :counter => "12", :approved_by => "SomeOne", :job_state => JobState::Building, :job_result => JobResult::Unknown}]})

      running_instance.getStageHistory().last().setPreviousStage(previous_stage)

      pipeline_with_active_instance = pipeline_model_with_instances([running_instance], pipeline_name)
      group = PipelineGroupModel.new(group_name)
      group.add(pipeline_with_active_instance)

      expect(running_instance.isAnyStageActive()).to be_true
      expect(running_instance.activeStage().hasPreviousStage()).to be_true

      groups_json = render_json_for group

      previous_instance = groups_json[0]["pipelines"][0]["previous_instance"]
      expect(previous_instance["result"]).to eq("Failed")
      expect(previous_instance["details_path"]).to eq("/pipelines/cruise/10/p1_stage2/11")
      expect(previous_instance["pipeline_label"]).to eq("SOME_PIPELINE_LABEL")
    end
  end
end

def render_json_for(*groups)
  assign(:pipeline_groups, groups)
  render
  JSON.parse(response.body)
end

def assert_pipeline_details the_pipeline, name, expected_number_of_instances
  expect(the_pipeline["name"]).to eq(name)
  expect(the_pipeline["instances"].length).to eq(expected_number_of_instances)
  expect(the_pipeline["settings_path"]).to eq("/admin/pipelines/#{name}/general")
end

def assert_instance_details the_instance, pipeline_name, pipeline_label, pipeline_counter, latest_stage_name = "cruise", latest_stage_state = "Passed"
  expect(the_instance["has_history"]).to eq("true")
  expect(the_instance["label"]).to eq(pipeline_label)
  expect(is_time_within_minutes(6, the_instance["scheduled_time"])).to be_true
  expect(the_instance["history_path"]).to eq("/tab/pipeline/history/#{pipeline_name}")
  expect(the_instance["vsm_path"]).to eq("/pipelines/value_stream_map/#{pipeline_name}/#{pipeline_counter}")
  expect(the_instance["compare_path"]).to eq("/compare/#{pipeline_name}/#{pipeline_counter - 1}/with/#{pipeline_counter}")
  expect(the_instance["build_cause_path"]).to eq("/pipelines/#{pipeline_name}/#{pipeline_counter}/build_cause")
  expect(the_instance["triggered_by"]).to eq("Anonymous")
  expect(the_instance["latest_stage_name"]).to eq(latest_stage_name)
  expect(the_instance["latest_stage_state"]).to eq(latest_stage_state)
end

def is_time_within_minutes expected_number_of_minutes_time_is_within, time_in_milliseconds_since_epoch
  time_now = Time.now.to_i * 1000
  lower_bound = time_now - (expected_number_of_minutes_time_is_within * 60 * 1000 / 2)
  upper_bound = time_now + (expected_number_of_minutes_time_is_within * 60 * 1000 / 2)

  lower_bound <= time_in_milliseconds_since_epoch and time_in_milliseconds_since_epoch <= upper_bound
end

def pipeline_model_with_lock_status(can_unlock, is_locked, pipeline_name, pipeline_label, pipeline_counter)
  latest_instance = pipeline_instance_model(
      {:name => pipeline_name, :label => pipeline_label, :counter => pipeline_counter,
       :stages => [
           {:name => "p1_stage1", :counter => "10", :approved_by => "Anonymous"},
           {:name => "p1_stage2", :counter => "12", :approved_by => "SomeOne"}]})
  earlier_instance = pipeline_instance_model(
      {:name => pipeline_name, :label => pipeline_label, :counter => pipeline_counter + 1,
       :stages => [
           {:name => "p1_stage1", :counter => "11", :approved_by => "Anonymous"},
           {:name => "p1_stage2", :counter => "13", :approved_by => "SomeOne"}]})

  latest_instance.setCurrentlyLocked(is_locked)
  latest_instance.setCanUnlock(can_unlock)
  pipeline_model_with_instances([latest_instance, earlier_instance], pipeline_name, true, nil, true)
end
