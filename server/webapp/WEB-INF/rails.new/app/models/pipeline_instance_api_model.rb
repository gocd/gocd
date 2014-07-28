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
  attr_reader :id, :name, :counter, :label, :natural_order, :can_run, :preparing_to_schedule, :currently_locked, :lockable, :can_unlock, :build_cause, :stages

  def initialize(pipeline_instance_model)
    @id = pipeline_instance_model.getId() unless pipeline_instance_model.getId() == nil
    @name = pipeline_instance_model.getName() unless pipeline_instance_model.getName() == nil
    @counter = pipeline_instance_model.getCounter() unless pipeline_instance_model.getCounter() == nil
    @label = pipeline_instance_model.getLabel() unless pipeline_instance_model.getLabel() == nil
    @natural_order = pipeline_instance_model.getNaturalOrder() unless pipeline_instance_model.getNaturalOrder() == nil
    @can_run = pipeline_instance_model.getCanRun() unless pipeline_instance_model.getCanRun() == nil
    @preparing_to_schedule = pipeline_instance_model.isPreparingToSchedule() unless pipeline_instance_model.isPreparingToSchedule() == nil
    @currently_locked = pipeline_instance_model.isCurrentlyLocked() unless pipeline_instance_model.isCurrentlyLocked() == nil
    @lockable = pipeline_instance_model.isLockable() unless pipeline_instance_model.isLockable() == nil
    @can_unlock = pipeline_instance_model.canUnlock() unless pipeline_instance_model.canUnlock() == nil
    @build_cause = BuildCauseAPIModel.new(pipeline_instance_model.getBuildCause())
    @stages = []
    pipeline_instance_model.getStageHistory().each do |stage_instance_model|
      @stages << StageInstanceAPIModel.new(stage_instance_model)
    end
  end
end