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

module ApiV1
  class AgentRepresenter < ApiV1::BaseRepresenter
    alias_method :agent, :represented

    link :self do |opts|
      opts[:url_builder].apiv1_agent_url(agent.getUuid())
    end

    link :doc do |opts|
      'http://api.go.cd/#agents'
    end

    link :find do |opts|
      opts[:url_builder].apiv1_agent_url(':uuid')
    end

    property :getUuid, as: :uuid
    property :getHostname, as: :hostname
    property :getIpAddress, as: :ip_address
    property :isEnabled, as: :enabled
    property :getLocation, as: :sandbox
    property :getStatusForDisplay, as: :status
    property :getOperatingSystem, as: :operating_system
    property :free_space, exec_context: :decorator
    property :getResources, as: :resources
    property :getEnvironments, as: :environments

    def free_space
      if agent.freeDiskSpace() && !agent.freeDiskSpace().isNullDiskspace()
        agent.freeDiskSpace().space
      else
        'unknown'
      end
    end
  end
end
