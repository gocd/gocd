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

class JobStateTransitionAPIModel
  attr_reader :id, :state, :state_change_time

  def initialize(job_state_transition_model)
    @id = job_state_transition_model.getId()
    @state = job_state_transition_model.getCurrentState().to_s unless job_state_transition_model.getCurrentState() == nil
    @state_change_time = job_state_transition_model.getStateChangeTime().getTime() unless job_state_transition_model.getStateChangeTime() == nil
  end
end