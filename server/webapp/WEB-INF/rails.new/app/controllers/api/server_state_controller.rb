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
class Api::ServerStateController < Api::ApiController

  def status
    if system_environment.isServerActive
      render text: 'active'
      return
    end
    render text: 'passive'
  end

  def to_active
    system_environment.switchToActiveState
    render text: 'active'
  end

  def to_passive
    system_environment.switchToPassiveState
    render text: 'passive'
  end

end