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

class StageAPIModel
  attr_reader :id, :name, :counter, :approval_type, :approved_by, :result, :rerun_of_counter, :pipeline_name, :pipeline_counter, :jobs

  def initialize(stage_instance_model)
    @id = stage_instance_model.getId()
    @name = stage_instance_model.getName()
    @counter = stage_instance_model.getCounter()
    @approval_type = stage_instance_model.getApprovalType()
    @approved_by = stage_instance_model.getApprovedBy()
    @result = stage_instance_model.getResult().to_s unless stage_instance_model.getResult() == nil
    @rerun_of_counter = stage_instance_model.getRerunOfCounter()
    @fetch_materials = stage_instance_model.shouldFetchMaterials()
    @clean_working_directory = stage_instance_model.shouldCleanWorkingDir()
    @artifacts_deleted = stage_instance_model.isArtifactsDeleted()
    @pipeline_name = stage_instance_model.getIdentifier().getPipelineName() unless stage_instance_model.getIdentifier() == nil
    @pipeline_counter = stage_instance_model.getIdentifier().getPipelineCounter() unless stage_instance_model.getIdentifier() == nil
    @jobs = stage_instance_model.getJobInstances().collect do |job_instance_model|
      job_instance_api_model = JobInstanceAPIModel.new(job_instance_model)
      job_instance_api_model.clear_pipeline_and_stage_details
      job_instance_api_model
    end
  end
end