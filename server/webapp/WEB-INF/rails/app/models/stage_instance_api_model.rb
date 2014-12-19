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

class StageInstanceAPIModel
  attr_reader :id, :name, :counter, :approval_type, :approved_by, :result, :rerun_of_counter, :pipeline_name, :pipeline_counter, :jobs

  def initialize(stage_instance_model)
    @id = stage_instance_model.getId()
    @name = stage_instance_model.getName()
    @counter = stage_instance_model.getCounter()
    @approval_type = stage_instance_model.getApprovalType()
    @approved_by = stage_instance_model.getApprovedBy()
    @result = stage_instance_model.getResult().to_s unless stage_instance_model.getResult() == nil
    @rerun_of_counter = stage_instance_model.getRerunOfCounter()
    @pipeline_name = stage_instance_model.getPipelineName() unless stage_instance_model.getIdentifier() == nil
    @pipeline_counter = stage_instance_model.getPipelineCounter() unless stage_instance_model.getIdentifier() == nil
    @jobs = stage_instance_model.getBuildHistory().collect do |job_instance_model|
      JobHistoryItemAPIModel.new(job_instance_model)
    end
  end
end