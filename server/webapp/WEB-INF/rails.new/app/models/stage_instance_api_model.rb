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
  attr_reader :id, :name, :counter, :scheduled, :approval_type, :approved_by, :result, :rerun_of_counter, :operate_permission, :can_run, :pipeline_name, :pipeline_counter, :jobs

  def initialize(stage_instance_model)
    @id = stage_instance_model.getId() unless stage_instance_model.getId() == nil
    @name = stage_instance_model.getName() unless stage_instance_model.getName() == nil
    @counter = stage_instance_model.getCounter() unless stage_instance_model.getCounter() == nil
    @scheduled = stage_instance_model.isScheduled() unless stage_instance_model.isScheduled() == nil
    @approval_type = stage_instance_model.getApprovalType() unless stage_instance_model.getApprovalType() == nil
    @approved_by = stage_instance_model.getApprovedBy() unless stage_instance_model.getApprovedBy() == nil
    @result = stage_instance_model.getResult().to_s unless stage_instance_model.getResult() == nil
    @rerun_of_counter = stage_instance_model.getRerunOfCounter() unless stage_instance_model.getRerunOfCounter() == nil
    @operate_permission = stage_instance_model.hasOperatePermission() unless stage_instance_model.hasOperatePermission() == nil
    @can_run = stage_instance_model.getCanRun() unless stage_instance_model.getCanRun() == nil
    @pipeline_name = stage_instance_model.getPipelineName() unless stage_instance_model.getPipelineName() == nil
    @pipeline_counter = stage_instance_model.getPipelineCounter() unless stage_instance_model.getPipelineCounter() == nil
    @jobs = []
    stage_instance_model.getBuildHistory().each do |job_instance_model|
      @jobs << JobInstanceAPIModel.new(job_instance_model)
    end
  end
end