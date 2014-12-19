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

class PipelineInstanceAPIModel
  attr_reader :id, :name, :counter, :label, :natural_order, :can_run, :preparing_to_schedule, :currently_locked, :lockable, :can_unlock, :build_cause, :stages, :comment

  def initialize(pipeline_instance_model)
    @id = pipeline_instance_model.getId()
    @name = pipeline_instance_model.getName()
    @counter = pipeline_instance_model.getCounter()
    @label = pipeline_instance_model.getLabel()
    @natural_order = pipeline_instance_model.getNaturalOrder()
    @can_run = pipeline_instance_model.getCanRun()
    @preparing_to_schedule = pipeline_instance_model.isPreparingToSchedule()
    @build_cause = BuildCauseAPIModel.new(pipeline_instance_model.getBuildCause())
    @stages = pipeline_instance_model.getStageHistory().collect do |stage_instance_model|
      StageHistoryItemAPIModel.new(stage_instance_model)
    end
    @comment = pipeline_instance_model.getComment()
  end
end