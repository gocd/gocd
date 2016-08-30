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
  class AgentsRepresenter < BaseRepresenter

    def initialize(agents_environments_map, security_service, current_user)
      @security_service = security_service
      @current_user = current_user
      super(agents_environments_map)
    end

    link :self do |opts|
      opts[:url_builder].apiv4_agents_url
    end

    link :doc do
      'https://api.go.cd/#agents'
    end

    collection :agents, embedded: true, exec_context: :decorator, decorator: AgentRepresenter

    def agents
      represented.map() {|agent, environments| {agent: agent, environments: environments, security_service: @security_service, current_user: @current_user}}
    end
  end
end
