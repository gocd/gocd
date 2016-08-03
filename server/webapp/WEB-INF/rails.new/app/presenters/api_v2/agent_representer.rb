##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
##########################################################################

module ApiV2
  class AgentRepresenter < ApiV2::BaseRepresenter
    java_import com.thoughtworks.go.domain.AgentRuntimeStatus

    alias_method :agent, :represented

    error_representer({
                          'ipAddress' => 'ip_address'
                      })

    link :self do |opts|
      opts[:url_builder].apiv2_agent_url(agent.getUuid())
    end

    link :doc do |opts|
      'https://api.go.cd/#agents'
    end

    link :find do |opts|
      opts[:url_builder].apiv2_agent_url(':uuid')
    end

    property :getUuid, as: :uuid
    property :getHostname, as: :hostname
    property :getIpAddress, as: :ip_address
    property :getLocation, as: :sandbox
    property :getOperatingSystem, as: :operating_system
    property :free_space, exec_context: :decorator
    property :agent_config_state, exec_context: :decorator
    property :agent_state, exec_context: :decorator
    property :build_state, exec_context: :decorator
    property :resources, exec_context: :decorator
    property :environments, exec_context: :decorator
    property :errors, exec_context: :decorator, decorator: ApiV2::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }

    def agent_config_state
      agent.getAgentConfigStatus()
    end

    def agent_state
      runtime_status.agentState
    end

    def build_state
      runtime_status.buildState
    end

    def resources #because you know - java.util.ArrayList
      agent.getResources().to_a
    end

    def environments #because you know - java.util.ArrayList
      agent.getEnvironments().to_a
    end

    def free_space
      if agent.freeDiskSpace() && !agent.freeDiskSpace().isNullDiskspace()
        agent.freeDiskSpace().space
      else
        'unknown'
      end
    end

    private
    def runtime_status
      @runtime_status ||= agent.getRuntimeStatus()
    end
  end
end
