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

class JobInstanceAPIModel
  attr_reader :id, :name, :state, :result, :scheduled_date, :rerun, :original_job_id, :agent_uuid, :pipeline_name, :pipeline_counter, :stage_name, :stage_counter, :job_state_transitions

  def initialize(job_instance_model)
    @id = job_instance_model.getId()
    @name = job_instance_model.getName()
    @state = job_instance_model.getState().to_s unless job_instance_model.getState() == nil
    @result = job_instance_model.getResult().to_s unless job_instance_model.getResult() == nil
    @scheduled_date = job_instance_model.getScheduledDate().getTime() unless job_instance_model.getScheduledDate() == nil
    @rerun = job_instance_model.isRerun()
    @original_job_id = job_instance_model.getOriginalJobId()
    @agent_uuid = job_instance_model.getAgentUuid()
    @pipeline_name = job_instance_model.getPipelineName()
    @pipeline_counter = job_instance_model.getPipelineCounter()
    @stage_name = job_instance_model.getStageName()
    @stage_counter = job_instance_model.getStageCounter()
    @job_state_transitions = job_instance_model.getTransitions().collect do |job_state_transition_model|
      JobStateTransitionAPIModel.new(job_state_transition_model)
    end
  end

  def clear_pipeline_and_stage_details
    @pipeline_name = nil
    @pipeline_counter = nil
    @stage_name = nil
    @stage_counter = nil
  end
end