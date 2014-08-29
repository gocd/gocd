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

class AgentAPIModel
  attr_reader :uuid, :agent_name, :ip_address, :sandbox, :status, :build_locator, :os, :free_space, :resources, :environments

  def initialize(agent_view_model)
    @uuid = agent_view_model.getUuid()
    @agent_name = agent_view_model.getHostname()
    @ip_address = agent_view_model.getIpAddress()
    @sandbox = agent_view_model.getLocation()
    @status = agent_view_model.getStatusForDisplay()
    @build_locator = agent_view_model.buildLocator()
    @os = agent_view_model.getOperatingSystem()
    @free_space = agent_view_model.freeDiskSpace().to_s unless agent_view_model.freeDiskSpace() == nil
    @resources  = agent_view_model.getResources()
    @environments = agent_view_model.getEnvironments()
  end
end