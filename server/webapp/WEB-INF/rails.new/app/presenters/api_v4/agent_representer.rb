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

module ApiV4
  class AgentRepresenter < ApiV4::BaseRepresenter

    error_representer({
                        'ipAddress'       => 'ip_address',
                        'elasticAgentId'  => 'elastic_agent_id',
                        'elasticPluginId' => 'elastic_plugin_id'
                      })

    link :self do |opts|
      opts[:url_builder].apiv4_agent_url(agent.getUuid())
    end

    link :doc do |opts|
      'https://api.go.cd/#agents'
    end

    link :find do |opts|
      opts[:url_builder].apiv4_agent_url(':uuid')
    end

    property :uuid, exec_context: :decorator
    property :hostname, exec_context: :decorator
    property :ip_address, exec_context: :decorator
    property :elastic_agent_id, exec_context: :decorator, skip_nil: true
    property :elastic_plugin_id, exec_context: :decorator, skip_nil: true
    property :location, as: :sandbox, exec_context: :decorator
    property :operating_system, exec_context: :decorator
    property :free_space, exec_context: :decorator
    property :agent_config_state, exec_context: :decorator
    property :agent_state, exec_context: :decorator
    property :resources, exec_context: :decorator, skip_nil: true
    property :environments, exec_context: :decorator
    property :build_state, exec_context: :decorator
    property :build_details, exec_context: :decorator, skip_nil: true, skip_parse: true, decorator: ApiV4::BuildDetailsRepresenter

    delegate :uuid, :hostname, :ip_address, :location, :operating_system, to: :agent

    def elastic_agent_id
      agent.elasticAgentMetadata.elasticAgentId if agent.isElastic
    end

    def elastic_plugin_id
      agent.elasticAgentMetadata.elasticPluginId if agent.isElastic
    end

    def agent_config_state
      agent.getAgentConfigStatus()
    end

    def agent_state
      runtime_status.agentState
    end

    def build_state
      runtime_status.buildState
    end

    def resources
      agent.getResources().resourceNames.to_a.sort unless agent.isElastic
    end

    def environments
      represented[:environments].sort
    end

    def free_space
      if agent.freeDiskSpace() && !agent.freeDiskSpace().isNullDiskspace()
        agent.freeDiskSpace().space
      else
        'unknown'
      end
    end

    def build_details
      build_info if agent.getBuildingInfo().isBuilding()
    end

    protected

    def error_object
      agent.errors
    end

    private
    def agent
      represented[:agent]
    end

    def current_user
      represented[:current_user]
    end

    def security_service
      represented[:security_service]
    end

    def build_info
      agent.getBuildingInfo() if security_service.hasViewOrOperatePermissionForPipeline(current_user, agent.getBuildingInfo().getPipelineName())
    end

    def runtime_status
      @runtime_status ||= agent.getRuntimeStatus()
    end
  end
end
