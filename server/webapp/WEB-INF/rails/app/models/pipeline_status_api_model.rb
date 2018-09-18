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

class PipelineStatusAPIModel
  attr_reader :paused, :pausedCause, :pausedBy, :locked, :schedulable

  def initialize(pipeline_status)
    @paused = pipeline_status.isPaused()
    @pausedCause = pipeline_status.pausedCause()
    @pausedBy = pipeline_status.pausedBy()
    @locked = pipeline_status.isLocked()
    @schedulable = pipeline_status.isSchedulable()
  end
end