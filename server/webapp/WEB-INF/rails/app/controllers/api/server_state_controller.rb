##########################GO-LICENSE-START################################
# Copyright 2015 ThoughtWorks, Inc.
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

class Api::ServerStateController < Api::ApiController

  def status
    render json: state_hash
  end

  def to_active
    system_environment.switchToActiveState
    render json: state_hash
  end

  def to_passive
    system_environment.switchToPassiveState
    render json: state_hash
  end

  private

  def state_hash
    if system_environment.isServerActive
      {state: 'active'}
    else
      {state: 'passive'}
    end
  end

end