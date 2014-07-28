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
  attr_reader :id, :name, :state, :result, :scheduled_date, :rerun, :original_job_id, :agent_uuid, :pipeline_name, :pipeline_counter, :stage_name, :stage_counter

  def initialize(job_instance_model)
    @id = job_instance_model.getId() unless job_instance_model.getId() == nil
    @name = job_instance_model.getName() unless job_instance_model.getName() == nil
    @state = job_instance_model.getState().to_s unless job_instance_model.getState() == nil
    @result = job_instance_model.getResult().to_s unless job_instance_model.getResult() == nil
    @scheduled_date = job_instance_model.getScheduledDate().to_s unless job_instance_model.getScheduledDate() == nil
    @rerun = job_instance_model.isRerun() if (job_instance_model.respond_to? :isRerun) && (job_instance_model.isRerun() != nil)
    @original_job_id = job_instance_model.getOriginalJobId() if (job_instance_model.respond_to? :getOriginalJobId) && (job_instance_model.getOriginalJobId() != nil)
    @agent_uuid = job_instance_model.getAgentUuid() if (job_instance_model.respond_to? :getAgentUuid) && (job_instance_model.getAgentUuid() != nil)
    @pipeline_name = job_instance_model.getPipelineName() if (job_instance_model.respond_to? :getPipelineName) && (job_instance_model.getPipelineName() != nil)
    @pipeline_counter = job_instance_model.getPipelineCounter() if (job_instance_model.respond_to? :getPipelineCounter) && (job_instance_model.getPipelineCounter() != nil)
    @stage_name = job_instance_model.getStageName() if (job_instance_model.respond_to? :getStageName) && (job_instance_model.getStageName() != nil)
    @stage_counter = job_instance_model.getStageCounter() if (job_instance_model.respond_to? :getStageCounter) && (job_instance_model.getStageCounter() != nil)
  end
end