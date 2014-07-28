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
    @uuid = agent_view_model.getUuid() unless agent_view_model.getUuid() == nil
    @agent_name = agent_view_model.getHostname() unless agent_view_model.getHostname() == nil
    @ip_address = agent_view_model.getIpAddress() unless agent_view_model.getIpAddress() == nil
    @sandbox = agent_view_model.getLocation() unless agent_view_model.getLocation() == nil
    @status = agent_view_model.getStatusForDisplay() unless agent_view_model.getStatusForDisplay() == nil
    @build_locator = agent_view_model.buildLocator() unless agent_view_model.buildLocator() == nil
    @os = agent_view_model.getOperatingSystem() unless agent_view_model.getOperatingSystem() == nil
    @free_space = agent_view_model.freeDiskSpace().toString() unless agent_view_model.freeDiskSpace() == nil
    @resources  = agent_view_model.getResources() unless agent_view_model.getResources() == nil
    @environments = agent_view_model.getEnvironments() unless agent_view_model.getEnvironments() == nil
  end
end